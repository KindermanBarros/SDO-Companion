#!/usr/bin/env node
import { createRequire } from 'node:module';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import {
  migrateAllSheets,
  migrateCampaignMembersAndInvites,
  migrateSheet,
  readAndVerifyBackupBundle,
  rollbackSheetPreservingPostCutover,
  writeBackupBundle,
} from './spark-migration.mjs';
import { convertLegacySheetToSparkV2 } from './spark-converter.mjs';

const require = createRequire(import.meta.url);

export function parseCliArgs(argv) {
  const args = {
    command: 'inventory',
    project: 'sdo-companion',
    sheet: null,
    all: false,
    dryRun: true,
    execute: false,
    confirmProd: false,
    backup: null,
    out: null,
    auditOut: null,
    emulatorHost: process.env.FIRESTORE_EMULATOR_HOST || null,
  };

  const positional = [];
  for (let i = 0; i < argv.length; i++) {
    const token = argv[i];
    if (token === '--execute') {
      args.execute = true;
      args.dryRun = false;
    } else if (token === '--dry-run') {
      args.dryRun = true;
      args.execute = false;
    } else if (token === '--confirm-prod') {
      args.confirmProd = true;
    } else if (token === '--all') {
      args.all = true;
    } else if (token === '--project' && argv[i + 1]) {
      args.project = argv[++i];
    } else if (token === '--sheet' && argv[i + 1]) {
      args.sheet = argv[++i];
    } else if (token === '--backup' && argv[i + 1]) {
      args.backup = argv[++i];
    } else if (token === '--out' && argv[i + 1]) {
      args.out = argv[++i];
    } else if (token === '--audit-out' && argv[i + 1]) {
      args.auditOut = argv[++i];
    } else if (token === '--emulator' && argv[i + 1]) {
      args.emulatorHost = argv[++i];
    } else if (!token.startsWith('--')) {
      positional.push(token);
    }
  }
  if (positional[0]) args.command = positional[0];
  return args;
}

function decodeFirestoreValue(value) {
  if (!value || typeof value !== 'object') return value;
  if ('mapValue' in value) {
    return Object.fromEntries(
      Object.entries(value.mapValue.fields ?? {}).map(([k, v]) => [k, decodeFirestoreValue(v)])
    );
  }
  if ('arrayValue' in value) {
    return (value.arrayValue.values ?? []).map(decodeFirestoreValue);
  }
  if ('integerValue' in value) return Number(value.integerValue);
  if ('doubleValue' in value) return Number(value.doubleValue);
  if ('booleanValue' in value) return Boolean(value.booleanValue);
  if ('nullValue' in value) return null;
  if ('timestampValue' in value) return value.timestampValue;
  if ('stringValue' in value) return value.stringValue;
  return Object.values(value)[0];
}

function encodeFirestoreValue(value) {
  if (value === null || value === undefined) return { nullValue: null };
  if (typeof value === 'boolean') return { booleanValue: value };
  if (typeof value === 'number') {
    return Number.isInteger(value) ? { integerValue: String(value) } : { doubleValue: value };
  }
  if (typeof value === 'string') return { stringValue: value };
  if (Array.isArray(value)) {
    return { arrayValue: { values: value.map(encodeFirestoreValue) } };
  }
  if (typeof value === 'object') {
    const fields = {};
    for (const [k, v] of Object.entries(value)) {
      if (v !== undefined) fields[k] = encodeFirestoreValue(v);
    }
    return { mapValue: { fields } };
  }
  return { stringValue: String(value) };
}

export async function createFirestoreRestStore({ project, emulatorHost }) {
  const base = '../node_modules/firebase-tools/lib/';
  const { Client } = require(base + 'apiv2');

  let urlPrefix = 'https://firestore.googleapis.com';
  let headers = {};
  if (emulatorHost) {
    const normalized = emulatorHost.startsWith('http') ? emulatorHost : `http://${emulatorHost}`;
    urlPrefix = normalized;
    headers = { Authorization: 'Bearer owner' };
  } else {
    const auth = require(base + 'auth');
    const options = { project, nonInteractive: true };
    const account = auth.getGlobalDefaultAccount();
    if (account) auth.setActiveAccount(options, account);
    await require(base + 'requireAuth').requireAuth(options);
  }

  const client = new Client({ urlPrefix, apiVersion: 'v1' });
  const docBase = `/projects/${project}/databases/(default)/documents`;

  return {
    async getDoc(docPath) {
      try {
        const res = await client.request({
          method: 'GET',
          path: `${docBase}/${docPath}`,
          headers,
        });
        return decodeFirestoreValue({ mapValue: { fields: res.body.fields ?? {} } });
      } catch (err) {
        if (err?.status === 404) return null;
        throw err;
      }
    },

    async listCollection(collectionPath) {
      const results = [];
      let pageToken;
      do {
        const res = await client.request({
          method: 'GET',
          path: `${docBase}/${collectionPath}`,
          headers,
          queryParams: { pageSize: 100, ...(pageToken ? { pageToken } : {}) },
        });
        for (const doc of res.body.documents ?? []) {
          const id = doc.name.split('/').at(-1);
          results.push({
            id,
            path: `${collectionPath}/${id}`,
            data: decodeFirestoreValue({ mapValue: { fields: doc.fields ?? {} } }),
          });
        }
        pageToken = res.body.nextPageToken;
      } while (pageToken);
      return results;
    },

    async commitBatch(writes) {
      const firestoreWrites = writes.map(w => {
        const name = `projects/${project}/databases/(default)/documents/${w.path}`;
        if (w.type === 'delete') return { delete: name };
        return {
          update: {
            name,
            fields: encodeFirestoreValue(w.data).mapValue.fields,
          },
        };
      });
      await client.request({
        method: 'POST',
        path: `/projects/${project}/databases/(default)/documents:commit`,
        headers,
        body: { writes: firestoreWrites },
      });
      return { written: writes.length };
    },
  };
}

export async function runCli(argv, storeOverride = null) {
  const args = parseCliArgs(argv);

  // Safety guard: never auto-execute writes against production without explicit --execute AND --confirm-prod.
  if (!args.dryRun && !args.emulatorHost && !args.confirmProd) {
    throw new Error(
      'Refusing to execute writes against non-emulator Firestore without explicit --execute --confirm-prod flags. Use --dry-run (default) first.'
    );
  }

  const store = storeOverride ?? (await createFirestoreRestStore({
    project: args.project,
    emulatorHost: args.emulatorHost,
  }));

  if (args.command === 'inventory') {
    const chars = await store.listCollection('characters');
    const campaigns = new Map();
    const report = { discovered: 0, verified: 0, failed: 0, mediaInventory: [] };
    for (const c of chars) {
      report.discovered++;
      try {
        const cid = c.data.campaignId && c.data.campaignId !== 'default' ? c.data.campaignId : '';
        if (cid && !campaigns.has(cid)) {
          campaigns.set(cid, await store.getDoc(`campaigns/${cid}`));
        }
        const converted = convertLegacySheetToSparkV2(c.id, c.data, cid ? campaigns.get(cid) : null);
        report.verified++;
        if (converted.legacyMediaInventory.hasMedia) {
          const { originalPhotoUri: _omitted, ...meta } = converted.legacyMediaInventory;
          report.mediaInventory.push(meta);
        }
      } catch (err) {
        report.failed++;
        report.mediaInventory.push({ charId: c.id, error: err.message });
      }
    }
    return report;
  }

  if (args.command === 'backup') {
    const outFile = args.out || path.resolve(process.cwd(), `spark-backup-${Date.now()}.json`);
    const result = await migrateAllSheets({ store, dryRun: true });
    const bundle = await writeBackupBundle(outFile, result.backupRecords);
    return {
      status: 'BACKUP_WRITTEN',
      path: outFile,
      recordCount: bundle.recordCount,
      bundleChecksum: bundle.bundleChecksum,
    };
  }

  if (args.command === 'migrate') {
    if (args.sheet) {
      const sheetRes = await migrateSheet({
        store,
        charId: args.sheet,
        dryRun: args.dryRun,
        backupFilePath: args.backup,
      });
      return sheetRes;
    }
    if (!args.all) {
      throw new Error('Specify --sheet <charId> or --all for migrate.');
    }
    const sheetsRes = await migrateAllSheets({
      store,
      dryRun: args.dryRun,
      backupFilePath: args.backup,
    });
    const campaignRes = await migrateCampaignMembersAndInvites({
      store,
      dryRun: args.dryRun,
    });
    return { sheets: sheetsRes, campaignLinks: campaignRes };
  }

  if (args.command === 'rollback') {
    if (!args.sheet || !args.backup) {
      throw new Error('Rollback requires --sheet <charId> and --backup <backupBundlePath>.');
    }
    const bundle = await readAndVerifyBackupBundle(args.backup);
    const backupRecord = (bundle.records ?? []).find(r => r.charId === args.sheet);
    if (!backupRecord) {
      throw new Error(`No backup record found for ${args.sheet} in ${args.backup}.`);
    }
    return rollbackSheetPreservingPostCutover({
      store,
      charId: args.sheet,
      backupRecord,
      dryRun: args.dryRun,
      auditOutPath: args.auditOut,
    });
  }

  throw new Error(`Unknown command: ${args.command}`);
}

const isMain = process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href;
if (isMain) {
  runCli(process.argv.slice(2))
    .then(output => {
      console.log(JSON.stringify(output, null, 2));
    })
    .catch(err => {
      console.error(err.message);
      process.exitCode = 1;
    });
}
