import fs from 'node:fs/promises';
import path from 'node:path';
import {
  canonical,
  compareParity,
  convertLegacyInviteToSparkV2,
  convertLegacyMemberToSparkV2,
  convertLegacySheetToSparkV2,
  digest,
  reconstructItemFromSparkV2,
  reconstructLegacyFromSparkV2,
} from './spark-converter.mjs';

export const MAX_TARGETS_PER_OP = 3;

export function createInMemoryStore(initialDocs = {}) {
  const docs = new Map();
  for (const [docPath, data] of Object.entries(initialDocs)) {
    docs.set(docPath, structuredClone(data));
  }

  return {
    async getDoc(docPath) {
      const value = docs.get(docPath);
      return value === undefined ? null : structuredClone(value);
    },

    async listCollection(collectionPath) {
      const prefix = collectionPath.endsWith('/') ? collectionPath : `${collectionPath}/`;
      const results = [];
      for (const [docPath, data] of docs.entries()) {
        if (!docPath.startsWith(prefix)) continue;
        const remainder = docPath.slice(prefix.length);
        if (!remainder || remainder.includes('/')) continue;
        results.push({ id: remainder, path: docPath, data: structuredClone(data) });
      }
      results.sort((a, b) => a.id.localeCompare(b.id));
      return results;
    },

    async commitBatch(writes) {
      if (!Array.isArray(writes)) throw new Error('commitBatch requires an array of writes.');
      for (const w of writes) {
        if (w.type === 'set') {
          docs.set(w.path, structuredClone(w.data));
        } else if (w.type === 'delete') {
          docs.delete(w.path);
        } else {
          throw new Error(`Unsupported write type: ${w.type}`);
        }
      }
      return { written: writes.length };
    },

    snapshot() {
      return Object.fromEntries(
        [...docs.entries()].sort(([a], [b]) => a.localeCompare(b)).map(([k, v]) => [k, structuredClone(v)])
      );
    },
  };
}

export async function readSparkV2Sheet(store, charId) {
  const root = await store.getDoc(`characters/${charId}`);
  const fieldDocs = await store.listCollection(`characters/${charId}/fields`);
  const itemDocs = await store.listCollection(`characters/${charId}/items`);
  const media = await store.getDoc(`characters/${charId}/media/avatar`);
  const opDocs = await store.listCollection(`characters/${charId}/operations`);

  const fields = {};
  for (const doc of fieldDocs) {
    fields[doc.id] = doc.data;
  }

  const items = {};
  const itemFields = {};
  for (const doc of itemDocs) {
    items[doc.id] = doc.data;
    const subFields = await store.listCollection(`characters/${charId}/items/${doc.id}/fields`);
    itemFields[doc.id] = {};
    for (const fDoc of subFields) {
      itemFields[doc.id][fDoc.id] = fDoc.data;
    }
  }

  const operations = opDocs.map(d => d.data);
  return { root, fields, items, itemFields, media, operations };
}

export function buildMigrationStagingBatches(charId, converted, options = {}) {
  const actorUid = options.actorUid ?? converted.root.updatedBy ?? converted.root.ownerId;
  const createdAt = options.timestamp ?? converted.root.updatedAt ?? Date.now();
  const opPrefix = options.opPrefix ?? `mig_${charId}`;

  const stagedEntries = [];

  for (const fieldKey of Object.keys(converted.fields).sort()) {
    const doc = converted.fields[fieldKey];
    stagedEntries.push({
      target: {
        kind: 'FIELD',
        itemId: '',
        key: fieldKey,
        op: 'CREATE',
        expectedRevision: 0,
        nextRevision: doc.revision,
        baseValue: null,
        desiredValue: structuredClone(doc.value),
      },
      path: `characters/${charId}/fields/${fieldKey}`,
      data: structuredClone(doc),
    });
  }

  // Stage all item docs BEFORE their itemField docs so item exists with deleted=false.
  for (const itemId of Object.keys(converted.items).sort()) {
    const doc = converted.items[itemId];
    stagedEntries.push({
      target: {
        kind: 'ITEM',
        itemId,
        key: '',
        op: 'CREATE',
        expectedRevision: 0,
        nextRevision: doc.revision,
      },
      path: `characters/${charId}/items/${itemId}`,
      data: structuredClone(doc),
    });
  }

  for (const itemId of Object.keys(converted.itemFields).sort()) {
    const fMap = converted.itemFields[itemId];
    for (const fieldKey of Object.keys(fMap).sort()) {
      const doc = fMap[fieldKey];
      stagedEntries.push({
        target: {
          kind: 'ITEM_FIELD',
          itemId,
          key: fieldKey,
          op: 'CREATE',
          expectedRevision: 0,
          nextRevision: doc.revision,
          baseValue: null,
          desiredValue: structuredClone(doc.value),
        },
        path: `characters/${charId}/items/${itemId}/fields/${fieldKey}`,
        data: structuredClone(doc),
      });
    }
  }

  const stagingBatches = [];
  for (let i = 0; i < stagedEntries.length; i += MAX_TARGETS_PER_OP) {
    const chunk = stagedEntries.slice(i, i + MAX_TARGETS_PER_OP);
    const batchIndex = stagingBatches.length + 1;
    const opId = `${opPrefix}_s${String(batchIndex).padStart(4, '0')}`;
    const targets = chunk.map(e => e.target);
    const payloadHash = digest(chunk.map(e => ({ path: e.path, data: e.data })));
    const receipt = {
      id: opId,
      actorUid,
      type: 'MIGRATE_V2',
      outcome: 'COMMITTED',
      schemaVersion: 2,
      createdAt,
      targets,
      preconditions: {
        payloadHash,
        campaignId: converted.root.campaignId,
        lockType: converted.root.lockType,
      },
    };
    const writes = chunk.map(e => ({
      type: 'set',
      path: e.path,
      data: { ...e.data, lastOperationId: opId },
    }));
    writes.push({
      type: 'set',
      path: `characters/${charId}/operations/${opId}`,
      data: receipt,
    });
    stagingBatches.push({ opId, payloadHash, receipt, writes });
  }

  if (converted.media) {
    stagingBatches.push({
      opId: null,
      payloadHash: digest(converted.media),
      receipt: null,
      writes: [
        {
          type: 'set',
          path: `characters/${charId}/media/avatar`,
          data: structuredClone(converted.media),
        },
      ],
    });
  }

  const activationOpId = `${opPrefix}_activate`;
  const activationRoot = {
    ...structuredClone(converted.root),
    lastOperationId: activationOpId,
  };
  const activationTarget = {
    kind: 'SHEET',
    itemId: '',
    key: '',
    op: 'CREATE',
    expectedRevision: 0,
    nextRevision: activationRoot.revision,
    baseValue: null,
    desiredValue: {
      protocolVersion: activationRoot.protocolVersion,
      schemaVersion: activationRoot.schemaVersion,
      campaignId: activationRoot.campaignId,
      lockType: activationRoot.lockType,
      deleted: activationRoot.deleted,
    },
  };
  const activationReceipt = {
    id: activationOpId,
    actorUid,
    type: 'MIGRATE_V2',
    outcome: 'COMMITTED',
    schemaVersion: 2,
    createdAt,
    targets: [activationTarget],
    preconditions: {
      payloadHash: digest(activationRoot),
      campaignId: activationRoot.campaignId,
      lockType: activationRoot.lockType,
    },
  };

  return {
    stagingBatches,
    activationBatch: {
      opId: activationOpId,
      receipt: activationReceipt,
      root: activationRoot,
      writes: [
        { type: 'set', path: `characters/${charId}`, data: activationRoot },
        { type: 'set', path: `characters/${charId}/operations/${activationOpId}`, data: activationReceipt },
      ],
    },
  };
}

export function createBackupRecord({
  charId,
  legacyDoc,
  converted,
  cutoverOperationIds = [],
  cutoverTimestamp = Date.now(),
}) {
  const convertedSnapshot = {
    root: converted.root,
    fields: converted.fields,
    items: converted.items,
    itemFields: converted.itemFields,
    media: converted.media,
  };
  return {
    version: 1,
    charId,
    createdAt: new Date(cutoverTimestamp).toISOString(),
    cutoverTimestamp,
    cutoverRevision: converted.root.revision,
    cutoverOperationIds: [...cutoverOperationIds],
    legacyDoc: structuredClone(legacyDoc),
    legacyChecksum: digest(legacyDoc),
    convertedSnapshot: structuredClone(convertedSnapshot),
    convertedChecksum: digest(convertedSnapshot),
    legacyMediaInventory: structuredClone(converted.legacyMediaInventory),
  };
}

export async function writeBackupBundle(filePath, records) {
  const payload = {
    version: 1,
    createdAt: new Date().toISOString(),
    recordCount: records.length,
    records,
  };
  const bundle = {
    ...payload,
    bundleChecksum: digest(payload),
  };
  await fs.mkdir(path.dirname(filePath), { recursive: true });
  await fs.writeFile(filePath, JSON.stringify(bundle, null, 2), 'utf8');
  return bundle;
}

export async function readAndVerifyBackupBundle(filePath) {
  const raw = await fs.readFile(filePath, 'utf8');
  const bundle = JSON.parse(raw);
  const { bundleChecksum, ...payload } = bundle;
  if (!bundleChecksum || digest(payload) !== bundleChecksum) {
    throw new Error(`Backup bundle checksum verification failed for ${filePath}.`);
  }
  for (const record of bundle.records ?? []) {
    if (digest(record.legacyDoc) !== record.legacyChecksum) {
      throw new Error(`Backup record checksum mismatch for sheet ${record.charId}.`);
    }
  }
  return bundle;
}

function buildSubcollectionDigest(sheet) {
  return digest({
    fields: Object.fromEntries(
      Object.entries(sheet.fields ?? {}).map(([k, v]) => [k, { key: v.key, value: v.value, revision: v.revision }])
    ),
    items: Object.fromEntries(
      Object.entries(sheet.items ?? {}).map(([k, v]) => [k, { id: v.id, kind: v.kind, deleted: v.deleted, revision: v.revision }])
    ),
    itemFields: Object.fromEntries(
      Object.entries(sheet.itemFields ?? {}).map(([itemId, fMap]) => [
        itemId,
        Object.fromEntries(
          Object.entries(fMap).map(([k, v]) => [k, { key: v.key, value: v.value, revision: v.revision }])
        ),
      ])
    ),
    media: sheet.media ?? null,
  });
}

export async function migrateSheet({
  store,
  charId,
  campaignDoc = null,
  dryRun = true,
  actorUid,
  timestamp = Date.now(),
  backupFilePath = null,
}) {
  const manifestPath = `characters/${charId}/migration/v2`;
  const existingRoot = await store.getDoc(`characters/${charId}`);
  if (!existingRoot) {
    throw new Error(`Legacy character ${charId} not found.`);
  }

  const existingManifest = await store.getDoc(manifestPath);

  if (existingRoot.protocolVersion === 2 && existingRoot.schemaVersion === 2) {
    if (existingManifest?.status !== 'VERIFIED') {
      throw new Error(
        `Sheet ${charId} has protocolVersion=2 without a VERIFIED migration manifest (status=${existingManifest?.status ?? 'MISSING'}).`
      );
    }
    const liveSheet = await readSparkV2Sheet(store, charId);
    const reconstructed = reconstructLegacyFromSparkV2(liveSheet);
    if (existingManifest.legacyDoc) {
      const parity = compareParity(existingManifest.legacyDoc, reconstructed, {
        legacyMediaInventory: existingManifest.legacyMediaInventory,
      });
      if (!parity.ok) {
        throw new Error(`Existing migrated sheet ${charId} failed subcollection parity check: ${JSON.stringify(parity.mismatches)}`);
      }
    } else if (
      existingManifest.subcollectionChecksum &&
      buildSubcollectionDigest(liveSheet) !== existingManifest.subcollectionChecksum
    ) {
      throw new Error(`Existing migrated sheet ${charId} failed subcollection checksum verification.`);
    }
    return {
      status: 'ALREADY_MIGRATED',
      charId,
      dryRun,
      batchCount: 0,
      targetCount: 0,
      warnings: [],
    };
  }

  const legacyDoc = existingRoot;
  let resolvedCampaign = campaignDoc;
  const campaignId = legacyDoc.campaignId && legacyDoc.campaignId !== 'default' ? legacyDoc.campaignId : '';
  if (campaignId && !resolvedCampaign) {
    resolvedCampaign = await store.getDoc(`campaigns/${campaignId}`);
  }

  const converted = convertLegacySheetToSparkV2(charId, legacyDoc, resolvedCampaign, {
    actorUid,
    timestamp,
  });

  const { stagingBatches, activationBatch } = buildMigrationStagingBatches(charId, converted, {
    actorUid,
    timestamp,
  });
  const allBatches = [...stagingBatches, activationBatch];
  const cutoverOperationIds = allBatches.filter(b => b.opId).map(b => b.opId);
  const targetCount = allBatches.reduce((sum, b) => sum + (b.receipt?.targets.length ?? 0), 0);

  for (const batch of allBatches) {
    if (batch.receipt && batch.receipt.targets.length > MAX_TARGETS_PER_OP) {
      throw new Error(`Batch ${batch.opId} exceeds max targets per op (${MAX_TARGETS_PER_OP}).`);
    }
  }

  const backupRecord = createBackupRecord({
    charId,
    legacyDoc,
    converted,
    cutoverOperationIds,
    cutoverTimestamp: timestamp,
  });

  if (dryRun) {
    const reconstructed = reconstructLegacyFromSparkV2(converted);
    const parity = compareParity(legacyDoc, reconstructed, {
      legacyMediaInventory: converted.legacyMediaInventory,
    });
    if (!parity.ok) {
      throw new Error(`Dry-run parity failed for ${charId}: ${JSON.stringify(parity.mismatches)}`);
    }
    return {
      status: 'DRY_RUN_VERIFIED',
      charId,
      dryRun: true,
      batchCount: allBatches.length,
      targetCount,
      backupRecord,
      parity,
      warnings: converted.warnings,
    };
  }

  if (backupFilePath) {
    await writeBackupBundle(backupFilePath, [backupRecord]);
  }

  // Phase 1: Write STAGING manifest (root protocolVersion remains untouched/legacy!).
  await store.commitBatch([
    {
      type: 'set',
      path: manifestPath,
      data: {
        charId,
        status: 'STAGING',
        legacyChecksum: backupRecord.legacyChecksum,
        startedAt: timestamp,
        totalStagingBatches: stagingBatches.length,
      },
    },
  ]);

  // Execute staging batches (idempotent on resume: skip batches whose receipt already matches).
  for (const batch of stagingBatches) {
    if (batch.opId) {
      const existingOp = await store.getDoc(`characters/${charId}/operations/${batch.opId}`);
      if (existingOp && existingOp.preconditions?.payloadHash === batch.payloadHash) {
        continue;
      }
    }
    await store.commitBatch(batch.writes);
  }

  // Verify staged subcollection parity BEFORE activating root protocolVersion=2!
  const staged = await readSparkV2Sheet(store, charId);
  const stagedParity = compareParity(
    legacyDoc,
    reconstructLegacyFromSparkV2({ ...staged, root: activationBatch.root }),
    { legacyMediaInventory: converted.legacyMediaInventory }
  );
  if (!stagedParity.ok) {
    throw new Error(`Staged subcollection parity failed for ${charId} before activation: ${JSON.stringify(stagedParity.mismatches)}`);
  }

  const subcollectionChecksum = buildSubcollectionDigest(staged);

  // Phase 2: Atomic activation — write root (protocolVersion=2), activation receipt, and VERIFIED manifest together.
  await store.commitBatch([
    ...activationBatch.writes,
    {
      type: 'set',
      path: manifestPath,
      data: {
        charId,
        status: 'VERIFIED',
        legacyChecksum: backupRecord.legacyChecksum,
        subcollectionChecksum,
        legacyMediaInventory: converted.legacyMediaInventory,
        cutoverOperationIds,
        startedAt: existingManifest?.startedAt ?? timestamp,
        verifiedAt: timestamp,
      },
    },
  ]);

  const readBack = await readSparkV2Sheet(store, charId);
  const reconstructed = reconstructLegacyFromSparkV2(readBack);
  const parity = compareParity(legacyDoc, reconstructed, {
    legacyMediaInventory: converted.legacyMediaInventory,
  });
  if (!parity.ok) {
    throw new Error(`Post-activation parity failed for ${charId}: ${JSON.stringify(parity.mismatches)}`);
  }

  return {
    status: 'MIGRATED_VERIFIED',
    charId,
    dryRun: false,
    batchCount: allBatches.length,
    targetCount,
    backupRecord,
    parity,
    warnings: converted.warnings,
  };
}

export async function migrateAllSheets({
  store,
  dryRun = true,
  backupFilePath = null,
  actorUid,
  timestamp = Date.now(),
}) {
  const charDocs = await store.listCollection('characters');
  const campaigns = new Map();
  const results = [];
  const backupRecords = [];

  for (const entry of charDocs) {
    const campaignId = entry.data.campaignId && entry.data.campaignId !== 'default' ? entry.data.campaignId : '';
    if (campaignId && !campaigns.has(campaignId)) {
      campaigns.set(campaignId, await store.getDoc(`campaigns/${campaignId}`));
    }
    const res = await migrateSheet({
      store,
      charId: entry.id,
      campaignDoc: campaignId ? campaigns.get(campaignId) : null,
      dryRun,
      actorUid,
      timestamp,
    });
    results.push(res);
    if (res.backupRecord) backupRecords.push(res.backupRecord);
  }

  let backupBundle = null;
  if (!dryRun && backupFilePath && backupRecords.length > 0) {
    backupBundle = await writeBackupBundle(backupFilePath, backupRecords);
  }

  return {
    dryRun,
    discovered: charDocs.length,
    verified: results.filter(r => r.status === 'DRY_RUN_VERIFIED' || r.status === 'MIGRATED_VERIFIED').length,
    alreadyMigrated: results.filter(r => r.status === 'ALREADY_MIGRATED').length,
    results,
    backupRecords,
    backupBundle,
  };
}

export async function migrateCampaignMembersAndInvites({ store, dryRun = true }) {
  const legacyMembers = await store.listCollection('campaignMembers');
  const legacyInvites = await store.listCollection('campaignInvites');
  const convertedMembers = legacyMembers.map(m => convertLegacyMemberToSparkV2(m.id, m.data));
  const convertedInvites = legacyInvites.map(i => convertLegacyInviteToSparkV2(i.id, i.data));

  if (!dryRun) {
    const writes = [
      ...convertedMembers.map(m => ({ type: 'set', path: m.path, data: m.data })),
      ...convertedInvites.map(i => ({ type: 'set', path: i.path, data: i.data })),
    ];
    for (let i = 0; i < writes.length; i += MAX_TARGETS_PER_OP) {
      await store.commitBatch(writes.slice(i, i + MAX_TARGETS_PER_OP));
    }
  }

  return {
    dryRun,
    membersCount: convertedMembers.length,
    invitesCount: convertedInvites.length,
    members: convertedMembers,
    invites: convertedInvites,
  };
}

function upsertByKey(list, item, getKey) {
  const arr = Array.isArray(list) ? [...list] : [];
  const key = getKey(item);
  const idx = arr.findIndex(existing => getKey(existing) === key);
  if (idx >= 0) {
    arr[idx] = item;
  } else {
    arr.push(item);
  }
  return arr;
}

function removeByKey(list, key, getKey) {
  const arr = Array.isArray(list) ? list : [];
  return arr.filter(existing => getKey(existing) !== key);
}

function buildSingleItemFromLive(itemId, itemDoc, fieldMap) {
  const reconstructed = reconstructItemFromSparkV2(itemId, itemDoc, fieldMap);
  if (!reconstructed) return null;

  switch (itemDoc.kind) {
    case 'ATTRIBUTE':
      return { collection: 'attributes', keyFn: a => a.acronym, key: reconstructed.acronym, value: reconstructed };
    case 'INVENTORY':
      return { collection: 'inventory', keyFn: it => it.id, key: reconstructed.id, value: reconstructed };
    case 'ITEM_STATE': {
      const id = typeof reconstructed.id === 'string' ? reconstructed.id : reconstructed.id?.value;
      return {
        collection: 'itemStates',
        payloadKey: 'canonicalItemsPayload',
        keyFn: it => (typeof it.id === 'string' ? it.id : it.id?.value),
        key: id,
        value: reconstructed,
      };
    }
    case 'POWER':
      return { collection: 'powers', keyFn: p => p.id, key: reconstructed.id, value: reconstructed };
    case 'MYSTIC':
      return { collection: 'mysticAbilities', keyFn: m => m.id, key: reconstructed.id, value: reconstructed };
    case 'ABILITY':
      return {
        collection: 'abilities',
        payloadKey: 'canonicalAbilitiesPayload',
        keyFn: a => a.id,
        key: reconstructed.id,
        value: reconstructed,
      };
    case 'CONDITION': {
      const id = typeof reconstructed.instanceId === 'string'
        ? reconstructed.instanceId
        : reconstructed.instanceId?.value ?? reconstructed.id;
      return {
        collection: 'conditionInstances',
        payloadKey: 'canonicalConditionsPayload',
        keyFn: c => (typeof c.instanceId === 'string' ? c.instanceId : c.instanceId?.value ?? c.id),
        key: id,
        value: reconstructed,
      };
    }
    case 'LEGACY_CONDITION':
      return { collection: 'conditions', keyFn: c => c.id, key: reconstructed.id, value: reconstructed };
    case 'NOTE':
      if (reconstructed.isLegacyNotes) {
        return { collection: 'notes', key: 'legacy_notes', value: reconstructed.text };
      }
      return { collection: 'personalNotes', keyFn: n => n.id, key: reconstructed.id, value: reconstructed };
    default:
      return null;
  }
}

export async function rollbackSheetPreservingPostCutover({
  store,
  charId,
  backupRecord,
  dryRun = true,
  auditOutPath = null,
}) {
  if (!backupRecord || backupRecord.charId !== charId) {
    throw new Error(`Invalid backupRecord for sheet ${charId}.`);
  }
  if (!backupRecord.legacyChecksum || digest(backupRecord.legacyDoc) !== backupRecord.legacyChecksum) {
    throw new Error(`Backup checksum verification failed for sheet ${charId}; refusing rollback.`);
  }
  if (!backupRecord.convertedSnapshot || !backupRecord.convertedChecksum ||
      digest(backupRecord.convertedSnapshot) !== backupRecord.convertedChecksum) {
    throw new Error(`Converted backup checksum verification failed for sheet ${charId}; refusing rollback.`);
  }

  const cutoverRevision = backupRecord.cutoverRevision ?? 1;
  const cutoverOpIds = new Set(backupRecord.cutoverOperationIds ?? []);

  const liveSheet = await readSparkV2Sheet(store, charId);
  if (liveSheet.root?.protocolVersion !== 2 || liveSheet.root?.schemaVersion !== 2) {
    throw new Error(`Sheet ${charId} is not an active Spark v2 sheet; refusing rollback.`);
  }
  const postCutoverOps = liveSheet.operations
    .filter(op => {
      if (!op || op.outcome !== 'COMMITTED') return false;
      if (cutoverOpIds.has(op.id) || op.type === 'MIGRATE_V2') return false;
      const targets = Array.isArray(op.targets) ? op.targets : [];
      if (targets.length === 0) return false;
      // These receipts belong to the v2 collection. Device clocks and Firestore
      // timestamp representations cannot decide whether their edits survive.
      return true;
    })
    .sort((a, b) => {
      const revA = Math.max(0, ...(a.targets ?? []).map(t => t.nextRevision ?? 0));
      const revB = Math.max(0, ...(b.targets ?? []).map(t => t.nextRevision ?? 0));
      if (revA !== revB) return revA - revB;
      return String(a.id).localeCompare(String(b.id));
    });

  const restored = structuredClone(backupRecord.legacyDoc);
  const preservedTargets = [];
  const touchedSheet = new Set();
  const touchedFields = new Set();
  const touchedItems = new Set();

  for (const op of postCutoverOps) {
    for (const target of op.targets ?? []) {
      preservedTargets.push({ opId: op.id, type: op.type, actorUid: op.actorUid, ...target });
      if (target.kind === 'SHEET') touchedSheet.add(op.type);
      else if (target.kind === 'FIELD' && target.key) touchedFields.add(target.key);
      else if ((target.kind === 'ITEM' || target.kind === 'ITEM_FIELD') && target.itemId) touchedItems.add(target.itemId);
    }
  }

  if (touchedSheet.size > 0 && liveSheet.root) {
    restored.ownerId = liveSheet.root.ownerId;
    restored.campaignId = liveSheet.root.campaignId;
    restored.deleted = Boolean(liveSheet.root.deleted);
    restored.lockType = liveSheet.root.lockType;
    restored.isLocked = Boolean(liveSheet.root.isLocked);
    restored.lockedBy = liveSheet.root.lockedBy ?? '';
    restored.lockedAt = liveSheet.root.lockedAt ?? null;
    restored.appliedDeliveryIds = Array.isArray(liveSheet.root.appliedDeliveryIds)
      ? [...liveSheet.root.appliedDeliveryIds]
      : [];
  }

  for (const fieldKey of touchedFields) {
    const liveField = liveSheet.fields[fieldKey];
    if (liveField && liveField.value !== undefined) {
      if (fieldKey === 'itemCreationDraft' && typeof liveField.value === 'string' && liveField.value.trim().startsWith('{')) {
        try {
          restored.itemCreationDraft = JSON.parse(liveField.value);
          continue;
        } catch {
          // keep string
        }
      }
      restored[fieldKey] = structuredClone(liveField.value);
    }
  }

  for (const itemId of touchedItems) {
    const liveItem = liveSheet.items[itemId];
    if (!liveItem) continue;
    const built = buildSingleItemFromLive(itemId, liveItem, liveSheet.itemFields[itemId]);
    if (!built) continue;

    if (built.collection === 'notes') {
      restored.notes = liveItem.deleted ? '' : built.value;
      continue;
    }

    if (built.payloadKey && !Array.isArray(restored[built.collection]) && typeof restored[built.payloadKey] === 'string' && restored[built.payloadKey].trim() !== '') {
      try {
        restored[built.collection] = JSON.parse(restored[built.payloadKey]);
      } catch {
        restored[built.collection] = [];
      }
    }

    const updatedList = liveItem.deleted
      ? removeByKey(restored[built.collection], built.key, built.keyFn)
      : upsertByKey(restored[built.collection], built.value, built.keyFn);

    if (built.payloadKey && built.payloadKey in backupRecord.legacyDoc) {
      restored[built.payloadKey] = JSON.stringify(updatedList);
      if (built.collection in backupRecord.legacyDoc) {
        restored[built.collection] = updatedList;
      }
    } else {
      restored[built.collection] = updatedList;
    }
  }

  const mediaChanged = digest(liveSheet.media) !== digest(backupRecord.convertedSnapshot.media ?? null);
  const backupPreset = backupRecord.convertedSnapshot.fields?.avatarPreset;
  const livePreset = liveSheet.fields.avatarPreset;
  const presetChanged = touchedFields.has('avatarPreset') ||
    digest({ value: livePreset?.value, revision: livePreset?.revision }) !==
      digest({ value: backupPreset?.value, revision: backupPreset?.revision });
  if (mediaChanged || presetChanged) {
    // Match the client: thumbnail first, then the preset (including explicit removal).
    restored.photoUri = liveSheet.media?.dataUri ||
      (liveSheet.media?.bytesBase64 && liveSheet.media?.mimeType
        ? `data:${liveSheet.media.mimeType};base64,${liveSheet.media.bytesBase64}`
        : liveSheet.fields.avatarPreset?.value ?? '');
  }

  if (postCutoverOps.length > 0 && liveSheet.root) {
    restored.updatedAt = liveSheet.root.updatedAt ?? restored.updatedAt;
    restored.lastEditedBy = liveSheet.root.updatedBy ?? restored.lastEditedBy;
  }

  const allKeys = new Set([...Object.keys(backupRecord.legacyDoc), ...Object.keys(restored)]);
  const changes = [];
  for (const key of [...allKeys].sort()) {
    const beforeVal = backupRecord.legacyDoc[key];
    const afterVal = restored[key];
    if (digest({ value: beforeVal }) !== digest({ value: afterVal })) {
      changes.push({
        field: key,
        beforeBackup: structuredClone(beforeVal),
        afterRollback: structuredClone(afterVal),
      });
    }
  }

  const restoredChecksum = digest(restored);
  const auditCore = {
    version: 1,
    charId,
    dryRun,
    backupChecksumVerified: true,
    backupChecksum: backupRecord.legacyChecksum,
    cutoverRevision,
    postCutoverOpCount: postCutoverOps.length,
    postCutoverOps: postCutoverOps.map(op => ({
      id: op.id,
      type: op.type,
      actorUid: op.actorUid,
      createdAt: op.createdAt,
      targets: op.targets,
    })),
    preservedTargets,
    changes,
    restoredChecksum,
  };
  const auditReport = {
    ...auditCore,
    auditChecksum: digest(canonical(auditCore)),
  };

  if (auditOutPath) {
    await fs.mkdir(path.dirname(auditOutPath), { recursive: true });
    await fs.writeFile(auditOutPath, JSON.stringify({ ...auditReport, restoredDoc: restored }, null, 2), 'utf8');
  }

  if (!dryRun) {
    await store.commitBatch([{ type: 'set', path: `characters/${charId}`, data: restored }]);
  }

  return {
    status: dryRun ? 'DRY_RUN_ROLLBACK_READY' : 'ROLLED_BACK',
    ...auditReport,
    restoredDoc: restored,
  };
}
