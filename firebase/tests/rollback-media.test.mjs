import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { convertLegacySheetToSparkV2, digest } from '../migration/spark-converter.mjs';
import {
  createBackupRecord,
  createInMemoryStore,
  rollbackSheetPreservingPostCutover,
} from '../migration/spark-migration.mjs';

function fixture() {
  const legacyDoc = { id: 'avatar-test', ownerId: 'owner', name: 'Before', photoUri: 'content://original' };
  const converted = convertLegacySheetToSparkV2('avatar-test', legacyDoc, null, { timestamp: 1000 });
  const backupRecord = createBackupRecord({ charId: 'avatar-test', legacyDoc, converted, cutoverTimestamp: 1000 });
  const docs = { 'characters/avatar-test': converted.root };
  for (const [key, data] of Object.entries(converted.fields)) docs[`characters/avatar-test/fields/${key}`] = data;
  return { store: createInMemoryStore(docs), backupRecord };
}

async function rollback(context, options = {}) {
  return rollbackSheetPreservingPostCutover({ ...context, charId: 'avatar-test', ...options });
}

test('rollback maps a post-cutover preset to legacy photoUri and retains unrelated edits with ISO timestamps', async () => {
  const context = fixture();
  await context.store.commitBatch([
    { type: 'set', path: 'characters/avatar-test/fields/avatarPreset', data: { value: 'preset:scholar', revision: 2 } },
    { type: 'set', path: 'characters/avatar-test/fields/name', data: { value: 'After', revision: 2 } },
    { type: 'set', path: 'characters/avatar-test/operations/edit', data: {
      id: 'edit', type: 'PATCH_FIELDS', outcome: 'COMMITTED', createdAt: '2026-11-16T10:00:00Z',
      targets: [{ kind: 'FIELD', key: 'avatarPreset', nextRevision: 2 }, { kind: 'FIELD', key: 'name', nextRevision: 2 }],
    } },
  ]);
  const result = await rollback(context);
  assert.equal(result.restoredDoc.photoUri, 'preset:scholar');
  assert.equal(result.restoredDoc.name, 'After');
});

test('rollback retains explicit preset removal even when original media was local-only', async () => {
  const context = fixture();
  await context.store.commitBatch([
    { type: 'set', path: 'characters/avatar-test/fields/avatarPreset', data: { value: '', revision: 2 } },
    { type: 'set', path: 'characters/avatar-test/operations/clear', data: {
      id: 'clear', type: 'PATCH_FIELDS', outcome: 'COMMITTED', createdAt: 0,
      targets: [{ kind: 'FIELD', key: 'avatarPreset', nextRevision: 2 }],
    } },
  ]);
  assert.equal((await rollback(context)).restoredDoc.photoUri, '');
});

test('rollback preserves changed binary thumbnails and untouched local-only originals', async () => {
  const context = fixture();
  assert.equal((await rollback(context)).restoredDoc.photoUri, 'content://original');
  await context.store.commitBatch([{ type: 'set', path: 'characters/avatar-test/media/avatar', data: {
    bytesBase64: 'YWJj', mimeType: 'image/png', updatedAt: '2026-11-16T10:00:00Z',
  } }]);
  assert.equal((await rollback(context)).restoredDoc.photoUri, 'data:image/png;base64,YWJj');
});

test('rollback retains thumbnail deletion, replacement, and media precedence over a preset', async () => {
  const context = fixture();
  context.backupRecord.convertedSnapshot.media = { dataUri: 'data:image/png;base64,b2xk' };
  context.backupRecord.convertedChecksum = digest(context.backupRecord.convertedSnapshot);
  // The migrated thumbnail is now absent: removal must clear the full original too.
  assert.equal((await rollback(context)).restoredDoc.photoUri, '');
  await context.store.commitBatch([
    { type: 'set', path: 'characters/avatar-test/media/avatar', data: { dataUri: 'data:image/png;base64,bmV3' } },
    { type: 'set', path: 'characters/avatar-test/fields/avatarPreset', data: { value: 'preset:scout', revision: 2 } },
  ]);
  assert.equal((await rollback(context)).restoredDoc.photoUri, 'data:image/png;base64,bmV3');
});

test('rollback rejects a tampered converted snapshot before writing', async () => {
  const context = fixture();
  context.backupRecord.convertedSnapshot.media = { dataUri: 'tampered' };
  const before = context.store.snapshot();
  await assert.rejects(() => rollback(context, { dryRun: false }), /checksum/i);
  assert.deepEqual(context.store.snapshot(), before);
});

test('rollback refuses missing or already rolled-back roots', async () => {
  const context = fixture();
  await context.store.commitBatch([{ type: 'delete', path: 'characters/avatar-test' }]);
  await assert.rejects(() => rollback(context, { dryRun: false }), /protocolVersion|Spark/i);
  await context.store.commitBatch([{ type: 'set', path: 'characters/avatar-test', data: context.backupRecord.legacyDoc }]);
  await assert.rejects(() => rollback(context, { dryRun: false }), /protocolVersion|Spark/i);
});

test('audit export failure leaves the Spark root untouched', async () => {
  const context = fixture();
  const dir = await fs.mkdtemp(path.join(os.tmpdir(), 'sdo-rollback-audit-'));
  try {
    const before = context.store.snapshot();
    await assert.rejects(() => rollback(context, { dryRun: false, auditOutPath: dir }));
    assert.deepEqual(context.store.snapshot(), before);
  } finally {
    await fs.rm(dir, { recursive: true, force: true });
  }
});

test('failed atomic rollback commit retains live state and supports a fresh retry', async () => {
  const context = fixture();
  const before = context.store.snapshot();
  const failingStore = { ...context.store, async commitBatch() { throw new Error('commit failed'); } };
  await assert.rejects(() => rollback({ ...context, store: failingStore }, { dryRun: false }), /commit failed/);
  assert.deepEqual(context.store.snapshot(), before);
  assert.equal((await rollback(context, { dryRun: false })).status, 'ROLLED_BACK');
  await assert.rejects(() => rollback(context, { dryRun: false }), /Spark/i);
});
