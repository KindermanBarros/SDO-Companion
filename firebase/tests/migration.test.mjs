import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import {
  ITEM_ID_RE,
  MAX_THUMBNAIL_BYTES,
  SKILL_PROP_KEY_RE,
  compareParity,
  convertLegacyInviteToSparkV2,
  convertLegacyMemberToSparkV2,
  convertLegacySheetToSparkV2,
  digest,
  reconstructLegacyFromSparkV2,
} from '../migration/spark-converter.mjs';
import {
  MAX_TARGETS_PER_OP,
  createInMemoryStore,
  migrateAllSheets,
  migrateCampaignMembersAndInvites,
  migrateSheet,
  readAndVerifyBackupBundle,
  readSparkV2Sheet,
  rollbackSheetPreservingPostCutover,
  writeBackupBundle,
} from '../migration/spark-migration.mjs';
import { runCli } from '../migration/migrate-cli.mjs';

function makePngDataUri(width = 64, height = 64, extraPaddingBytes = 0) {
  // Minimal valid PNG signature + IHDR chunk (width, height) + optional padding payload
  const buf = Buffer.alloc(33 + extraPaddingBytes, 0);
  Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]).copy(buf, 0);
  buf.writeUInt32BE(13, 8);
  buf.write('IHDR', 12, 'ascii');
  buf.writeUInt32BE(width, 16);
  buf.writeUInt32BE(height, 20);
  buf[24] = 8;
  buf[25] = 6;
  return `data:image/png;base64,${buf.toString('base64')}`;
}

function buildRichLegacyCharacter(overrides = {}) {
  const canonicalAbilities = [
    {
      id: 'abil-1',
      name: 'Golpe Arcano',
      kind: 'SPELL',
      favorite: true,
      available: true,
      effect: 'Causa 2d6 de dano arcano.',
      cost: { type: 'ARCANE', amount: 2 },
      revision: 2,
    },
  ];
  const canonicalItems = [
    {
      id: { value: 'istate-1' },
      definition: { id: 'cat-potion', version: 1 },
      scope: 'CAMPAIGN',
      stack: { quantity: 3 },
      consumable: { kind: 'POTION', doses: 2 },
    },
  ];
  const canonicalConditions = [
    {
      instanceId: { value: 'cond-1' },
      payload: { kind: 'BLEEDING', intensity: 2, duration: { kind: 'ROUNDS', value: 3 } },
      source: { type: 'Narrative', id: 'op-0' },
      appliedAt: 1700000000000,
    },
  ];

  return {
    id: 'char-rich-1',
    ownerId: 'user-owner-1',
    campaignId: 'default',
    name: 'Kaelen Vane',
    race: 'Humano',
    subRace: 'Norte',
    raceAttribute: 'VIG',
    occupation: 'Explorador',
    height: '1.82m',
    age: '29',
    sex: 'M',
    size: 'Médio',
    avatarPreset: 'preset_ranger_01',
    photoUri: makePngDataUri(128, 128, 120),
    level: 5,
    creationStatus: 'COMPLETED',
    creationStep: 8,
    creationCompletedAt: 1700000001000,
    creationRulesVersion: 2,
    itemSchemaVersion: 3,
    canonicalSchemaVersion: 2,
    migrationReviewPayload: JSON.stringify([{ field: 'legacy', reason: 'checked' }]),
    canonicalAbilitiesPayload: JSON.stringify(canonicalAbilities),
    canonicalBodyPayload: JSON.stringify({ regions: [{ region: 'Torso', failures: 0 }], organs: [] }),
    canonicalConditionsPayload: JSON.stringify(canonicalConditions),
    activeModifiersPayload: JSON.stringify([{ modifierId: 'mod-1', amount: 1 }]),
    canonicalItemsPayload: JSON.stringify(canonicalItems),
    scopedItemCatalogPayload: JSON.stringify([{ reference: { id: 'cat-potion' }, name: 'Poção Menor' }]),
    canonicalProgressionPayload: JSON.stringify({ currentLevel: 5, pointsSpent: 12 }),
    progressionLifeBonus: 4,
    progressionSanityBonus: 2,
    progressionArcaneBonus: 3,
    progressionEnergyBonus: 1,
    progressionHistory: [{ id: 'prog-1', previousLevel: 4, newLevel: 5, rewards: [], appliedAt: 1700000000500 }],
    money: 450,
    life: { current: 18, maximum: 22, adjustment: 1 },
    sanity: { current: 14, maximum: 15, adjustment: 0 },
    arcane: { current: 6, maximum: 8, adjustment: 0 },
    energy: { current: 7, maximum: 9, adjustment: 0 },
    destiny: { current: 3, maximum: 5, adjustment: 0 },
    exhaustion: { current: 1, maximum: 10, adjustment: 0 },
    corruption: { current: 5, maximum: 100, adjustment: 0 },
    protectionAdjustments: { fisica: 1, mental: 0 },
    positiveTraits: ['Corajoso', 'Observador'],
    negativeTraits: ['Teimoso'],
    pathName: 'Trilha das Cinzas',
    pathMotto: 'Nada se perde.',
    pathKeywords: ['Cinza', 'Vigilância', 'Lâmina'],
    pathPillars: ['Disciplina', 'Memória', 'Equilíbrio'],
    learnedKnowledges: [{ id: 'know-1', name: 'Sobrevivência Urbana', value: 2 }],
    arcaneKnowledges: [{ id: 'ak-1', name: 'Runas Antigas', value: 1 }],
    battleTechniques: [{ id: 'bt-1', name: 'Aparar', value: 2 }],
    containerCapacity: 10,
    agilityLimit: '3',
    story: 'Veterano da fronteira norte.',
    itemCreationDraft: { name: 'Faca de Cinza', category: 'Arma', step: 2 },
    attributes: [
      {
        name: 'Força',
        acronym: 'FOR',
        value: 3,
        modifier: 0,
        skills: [
          { name: 'Atletismo', value: 2, modifier: 0 },
          { name: 'Artes Marciais', value: 1, modifier: 1 },
        ],
      },
      {
        name: 'Percepção',
        acronym: 'PER',
        value: 4,
        modifier: 1,
        skills: [{ name: 'Percepção', value: 3, modifier: 0 }],
      },
    ],
    inventory: [
      {
        id: 'old-dagger',
        name: 'Adaga de Ferro',
        state: 'E',
        quantity: 1,
        load: 1,
        durabilityCurrent: 8,
        durabilityMax: 10,
        itemCondition: 'NORMAL',
        gemIds: ['gem-ruby'],
        enhancementChargesCurrent: 2,
        enhancementChargesMax: 3,
      },
    ],
    powers: [
      {
        id: 'pow-1',
        name: 'Golpe Preciso',
        active: true,
        favorite: false,
        available: true,
        effect: '+2 no acerto',
      },
    ],
    mysticAbilities: [
      {
        id: 'mys-1',
        name: 'Eco de Cinza',
        type: 'ASH',
        favorite: true,
        available: true,
        ashPurity: 'REFINED',
      },
    ],
    conditions: [
      {
        id: 'lcond-1',
        name: 'Fadiga Leve',
        intensity: '1',
        duration: '1 hora',
        origin: 'Marcha',
        summary: '-1 em testes físicos',
      },
    ],
    personalNotes: [
      { id: 'note-1', title: 'Pistas', text: 'Investigar o farol.' },
    ],
    lockType: 'NONE',
    isLocked: false,
    lockedBy: '',
    lockedAt: null,
    deleted: false,
    appliedDeliveryIds: ['deliv-100'],
    updatedAt: 1700000002000,
    lastEditedBy: 'user-owner-1',
    dirty: true,
    lastSyncedAt: 1699999999000,
    fieldUpdatedAt: { name: 1700000002000 },
    ...overrides,
  };
}

test('converts standalone rich legacy sheet to Spark v2 and proves 100% lossless round-trip parity', () => {
  const legacy = buildRichLegacyCharacter();
  const converted = convertLegacySheetToSparkV2(legacy.id, legacy, null);

  assert.equal(converted.root.protocolVersion, 2);
  assert.equal(converted.root.schemaVersion, 2);
  assert.equal(converted.root.campaignId, '', 'default campaignId must normalize to empty string');
  assert.equal(converted.root.deleted, false);
  assert.deepEqual(converted.root.appliedDeliveryIds, ['deliv-100']);

  // Validate item IDs and granular skill/item property keys conform strictly to Spark v2 rules regex
  for (const itemId of Object.keys(converted.items)) {
    assert.match(itemId, ITEM_ID_RE);
  }
  const perSkills = Object.keys(converted.itemFields.attr_PER).filter(k => k.startsWith('skill_'));
  assert.equal(perSkills.length, 3);
  for (const k of perSkills) {
    assert.match(k, SKILL_PROP_KEY_RE);
  }
  assert.equal(converted.itemFields.attr_PER.skill_Percepcao_name.value, 'Percepção');
  assert.equal(converted.itemFields.attr_PER.skill_Percepcao_value.value, 3);
  assert.equal(converted.itemFields.attr_PER.skill_Percepcao_modifier.value, 0);

  // Granular inventory & ability fields
  assert.equal(converted.itemFields['inv_old-dagger'].name.value, 'Adaga de Ferro');
  assert.equal(converted.itemFields['inv_old-dagger'].quantity.value, 1);
  assert.equal(converted.itemFields['inv_old-dagger'].durabilityCurrent.value, 8);
  assert.equal(converted.itemFields['pow_pow-1'].name.value, 'Golpe Preciso');
  assert.equal(converted.itemFields['pow_pow-1'].active.value, true);
  assert.equal(converted.itemFields['abil_abil-1'].cost.value.amount, 2);

  // Thumbnail <= 64 KiB emitted in media/avatar
  assert.ok(converted.media);
  assert.equal(converted.media.mimeType, 'image/png');
  assert.equal(converted.media.width, 128);
  assert.equal(converted.media.height, 128);
  assert.equal(converted.legacyMediaInventory.migratedToThumbnail, true);
  assert.equal(converted.legacyMediaInventory.requiresLocalPreservation, false);

  const reconstructed = reconstructLegacyFromSparkV2(converted);
  const parity = compareParity(legacy, reconstructed, {
    legacyMediaInventory: converted.legacyMediaInventory,
  });
  assert.equal(parity.ok, true, JSON.stringify(parity.mismatches));
});

test('handles campaign-linked, deleted tombstone, and locked (PLAYER, HISTORIAN, legacy isLocked) sheets', () => {
  const campaign = { id: 'camp-42', ownerId: 'gm-1', state: 'ACTIVE' };

  // Campaign-linked without campaignDoc must fail verification
  const linked = buildRichLegacyCharacter({ id: 'char-camp', campaignId: 'camp-42' });
  assert.throws(() => convertLegacySheetToSparkV2('char-camp', linked, null), /Campaign link cannot be verified/);

  // Campaign-linked with valid campaignDoc + tombstone + PLAYER lock
  const tombstoneLocked = buildRichLegacyCharacter({
    id: 'char-tomb',
    campaignId: 'camp-42',
    deleted: true,
    lockType: 'PLAYER',
    isLocked: true,
    lockedBy: 'user-owner-1',
    lockedAt: 1700000010000,
  });
  const convertedTomb = convertLegacySheetToSparkV2('char-tomb', tombstoneLocked, campaign);
  assert.equal(convertedTomb.root.campaignId, 'camp-42');
  assert.equal(convertedTomb.root.deleted, true);
  assert.equal(convertedTomb.root.lockType, 'PLAYER');
  assert.equal(convertedTomb.root.isLocked, true);
  assert.equal(convertedTomb.root.lockedBy, 'user-owner-1');
  assert.equal(convertedTomb.root.lockedAt, 1700000010000);

  const reconTomb = reconstructLegacyFromSparkV2(convertedTomb);
  const parityTomb = compareParity(tombstoneLocked, reconTomb);
  assert.equal(parityTomb.ok, true, JSON.stringify(parityTomb.mismatches));

  // Legacy isLocked=true with lockType='NONE' normalizes to HISTORIAN
  const legacyLock = buildRichLegacyCharacter({
    id: 'char-leglock',
    campaignId: 'camp-42',
    lockType: 'NONE',
    isLocked: true,
    lockedBy: 'gm-1',
    lockedAt: 1700000020000,
  });
  const convertedLegacyLock = convertLegacySheetToSparkV2('char-leglock', legacyLock, campaign);
  assert.equal(convertedLegacyLock.root.lockType, 'HISTORIAN');
  assert.equal(convertedLegacyLock.root.isLocked, true);
  assert.ok(convertedLegacyLock.warnings.some(w => w.includes('HISTORIAN')));
  const parityLegacyLock = compareParity(legacyLock, reconstructLegacyFromSparkV2(convertedLegacyLock));
  assert.equal(parityLegacyLock.ok, true, JSON.stringify(parityLegacyLock.mismatches));
});

test('excludes > 64 KiB data:image and local URIs from Firestore thumbnail while preserving full original in inventory/backup', () => {
  const hugeDataUri = makePngDataUri(256, 256, 60000); // > 65536 bytes base64 data URI
  assert.ok(Buffer.byteLength(hugeDataUri, 'utf8') > MAX_THUMBNAIL_BYTES);

  const hugeSheet = buildRichLegacyCharacter({ id: 'char-huge-photo', photoUri: hugeDataUri });
  const convertedHuge = convertLegacySheetToSparkV2('char-huge-photo', hugeSheet, null);

  assert.equal(convertedHuge.media, null, 'Oversized photo must NOT be emitted to media/avatar');
  assert.equal(convertedHuge.legacyMediaInventory.requiresLocalPreservation, true);
  assert.equal(convertedHuge.legacyMediaInventory.migratedToThumbnail, false);
  assert.equal(convertedHuge.legacyMediaInventory.reason, 'EXCEEDS_64KIB_LIMIT');
  assert.equal(convertedHuge.legacyMediaInventory.originalPhotoUri, hugeDataUri);

  const reconHuge = reconstructLegacyFromSparkV2(convertedHuge);
  const parityHuge = compareParity(hugeSheet, reconHuge, {
    legacyMediaInventory: convertedHuge.legacyMediaInventory,
  });
  assert.equal(parityHuge.ok, true, JSON.stringify(parityHuge.mismatches));

  // Local content:// URI
  const localSheet = buildRichLegacyCharacter({
    id: 'char-local-uri',
    photoUri: 'content://media/external/images/media/9912',
  });
  const convertedLocal = convertLegacySheetToSparkV2('char-local-uri', localSheet, null);
  assert.equal(convertedLocal.media, null);
  assert.equal(convertedLocal.legacyMediaInventory.kind, 'LOCAL_URI');
  assert.equal(convertedLocal.legacyMediaInventory.requiresLocalPreservation, true);
  assert.equal(convertedLocal.legacyMediaInventory.originalPhotoUri, localSheet.photoUri);
});

test('compareParity flags mismatches on field, ID, campaign link, lock, and tombstone', () => {
  const legacy = buildRichLegacyCharacter();
  const converted = convertLegacySheetToSparkV2(legacy.id, legacy, null);
  const broken = reconstructLegacyFromSparkV2(converted);
  broken.name = 'Tampered';
  broken.campaignId = 'wrong-camp';
  broken.deleted = true;
  broken.lockType = 'HISTORIAN';
  broken.isLocked = true;
  broken.inventory[0].id = 'wrong-id';

  const parity = compareParity(legacy, broken);
  assert.equal(parity.ok, false);
  const fields = new Set(parity.mismatches.map(m => m.field));
  assert.ok(fields.has('name'));
  assert.ok(fields.has('campaignId'));
  assert.ok(fields.has('deleted'));
  assert.ok(fields.has('lockState'));
  assert.ok(fields.has('inventory[old-dagger]'));
});

test('converts legacy campaignMembers and campaignInvites to Spark v2 subcollections', () => {
  const memberRes = convertLegacyMemberToSparkV2('camp-1::u-hist', {
    campaignId: 'camp-1',
    userId: 'u-hist',
    role: 'HISTORIAN',
    state: 'ACTIVE',
    characterIds: ['char-1'],
    joinedAt: 100,
    updatedAt: 200,
    joinedByInviteId: 'INV1',
  });
  assert.equal(memberRes.path, 'campaigns/camp-1/members/u-hist');
  assert.equal(memberRes.data.role, 'PLAYER');
  assert.ok(memberRes.warnings.length > 0);

  const pendingInvite = convertLegacyInviteToSparkV2('INV1', {
    campaignId: 'camp-1',
    code: 'INV1',
    campaignName: 'Crônicas',
    campaignDescription: 'Mesa 1',
    createdBy: 'gm-1',
    createdAt: 10,
    expiresAt: null,
    revokedAt: null,
    generation: 1,
  });
  assert.equal(pendingInvite.path, 'campaigns/camp-1/invites/INV1');
  assert.equal(pendingInvite.data.state, 'PENDING');
  assert.equal(pendingInvite.data.recipientUid, '');

  const revokedInvite = convertLegacyInviteToSparkV2('INV2', {
    campaignId: 'camp-1',
    code: 'INV2',
    createdBy: 'gm-1',
    createdAt: 10,
    revokedAt: 50,
  });
  assert.equal(revokedInvite.data.state, 'REVOKED');
});

test('migrates sheets in bounded batches (<= 3 targets/op), writes verified SHA-256 backup, and enforces dry-run/prod safety', async () => {
  const tmpDir = await fs.mkdtemp(path.join(os.tmpdir(), 'sdo-mig-test-'));
  try {
    const campaign = { id: 'camp-1', ownerId: 'gm-1', state: 'ACTIVE' };
    const sheet = buildRichLegacyCharacter({ id: 'char-mig-1', campaignId: 'camp-1', notes: 'Nota legada antiga' });
    const store = createInMemoryStore({
      'campaigns/camp-1': campaign,
      'characters/char-mig-1': sheet,
      'campaignMembers/camp-1::user-owner-1': {
        campaignId: 'camp-1',
        userId: 'user-owner-1',
        role: 'PLAYER',
        state: 'ACTIVE',
        characterIds: ['char-mig-1'],
        joinedAt: 1,
        updatedAt: 1,
        joinedByInviteId: 'INV1',
      },
      'campaignInvites/INV1': {
        campaignId: 'camp-1',
        code: 'INV1',
        createdBy: 'gm-1',
        createdAt: 1,
        revokedAt: null,
      },
    });

    // CLI refuses non-emulator execute without --confirm-prod
    await assert.rejects(
      () => runCli(['migrate', '--all', '--execute'], store),
      /--confirm-prod/
    );

    // Dry-run by default leaves store untouched
    const dryRes = await runCli(['migrate', '--all'], store);
    assert.equal(dryRes.sheets.dryRun, true);
    assert.equal(dryRes.sheets.verified, 1);
    assert.equal((await store.listCollection('characters/char-mig-1/operations')).length, 0);

    // Execute migration with backup bundle
    const backupPath = path.join(tmpDir, 'backup.json');
    const migRes = await migrateAllSheets({
      store,
      dryRun: false,
      backupFilePath: backupPath,
      actorUid: 'admin-mig',
      timestamp: 1700000050000,
    });
    assert.equal(migRes.verified, 1);
    assert.equal(migRes.results[0].status, 'MIGRATED_VERIFIED');

    // Verify backup bundle checksum
    const loadedBundle = await readAndVerifyBackupBundle(backupPath);
    assert.equal(loadedBundle.recordCount, 1);

    // Tamper check fails loudly
    const tampered = structuredClone(loadedBundle);
    tampered.records[0].legacyDoc.name = 'Corrupted';
    const tamperedPath = path.join(tmpDir, 'tampered.json');
    await fs.writeFile(tamperedPath, JSON.stringify(tampered), 'utf8');
    await assert.rejects(() => readAndVerifyBackupBundle(tamperedPath), /checksum/i);

    // Verify every operation receipt has <= 3 targets and item docs precede itemFields
    const live = await readSparkV2Sheet(store, 'char-mig-1');
    assert.ok(live.operations.length > 1);
    for (const op of live.operations) {
      assert.ok(op.targets.length >= 1 && op.targets.length <= MAX_TARGETS_PER_OP);
      assert.equal(op.outcome, 'COMMITTED');
      assert.equal(op.schemaVersion, 2);
    }
    assert.equal(live.root.protocolVersion, 2);
    assert.equal(live.fields.name.value, 'Kaelen Vane');
    assert.equal(live.itemFields.note_legacy_notes.text.value, 'Nota legada antiga');

    // Campaign members & invites migration
    const campMig = await migrateCampaignMembersAndInvites({ store, dryRun: false });
    assert.equal(campMig.membersCount, 1);
    assert.equal(campMig.invitesCount, 1);
    const migratedMember = await store.getDoc('campaigns/camp-1/members/user-owner-1');
    const migratedInvite = await store.getDoc('campaigns/camp-1/invites/INV1');
    assert.equal(migratedMember.role, 'PLAYER');
    assert.equal(migratedInvite.state, 'PENDING');
  } finally {
    await fs.rm(tmpDir, { recursive: true, force: true });
  }
});

test('rollbackSheetPreservingPostCutover retains ALL post-cutover commits, preserves >64KiB media, and exports audit diff', async () => {
  const tmpDir = await fs.mkdtemp(path.join(os.tmpdir(), 'sdo-rollback-test-'));
  try {
    const hugePhoto = makePngDataUri(256, 256, 60000);
    const legacy = buildRichLegacyCharacter({
      id: 'char-rb-1',
      photoUri: hugePhoto,
      money: 450,
      life: { current: 18, maximum: 22, adjustment: 1 },
    });
    const store = createInMemoryStore({
      'characters/char-rb-1': legacy,
    });

    const migRes = await migrateSheet({
      store,
      charId: 'char-rb-1',
      dryRun: false,
      actorUid: 'user-owner-1',
      timestamp: 1000,
    });
    const backupRecord = migRes.backupRecord;

    // Simulate 5 post-cutover commits after cutoverRevision=1
    await store.commitBatch([
      // 1. Scalar PATCH_FIELDS on name & story
      {
        type: 'set',
        path: 'characters/char-rb-1/fields/name',
        data: { key: 'name', value: 'Kaelen Pós-Corte', revision: 2, lastOperationId: 'op-post-1', updatedBy: 'user-owner-1', updatedAt: 2000 },
      },
      {
        type: 'set',
        path: 'characters/char-rb-1/fields/story',
        data: { key: 'story', value: 'Sobreviveu ao corte Spark.', revision: 2, lastOperationId: 'op-post-1', updatedBy: 'user-owner-1', updatedAt: 2000 },
      },
      {
        type: 'set',
        path: 'characters/char-rb-1/operations/op-post-1',
        data: {
          id: 'op-post-1',
          actorUid: 'user-owner-1',
          type: 'PATCH_FIELDS',
          outcome: 'COMMITTED',
          schemaVersion: 2,
          createdAt: 2000,
          targets: [
            { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 1, nextRevision: 2 },
            { kind: 'FIELD', itemId: '', key: 'story', op: 'SET', expectedRevision: 1, nextRevision: 2 },
          ],
          preconditions: { payloadHash: digest({ name: 'Kaelen Pós-Corte' }) },
        },
      },
      // 2. ADJUST_RESOURCE deltas on life (-5) and money (+100)
      {
        type: 'set',
        path: 'characters/char-rb-1/fields/life',
        data: { key: 'life', value: { current: 13, maximum: 22, adjustment: 1 }, revision: 2, lastOperationId: 'op-post-2', updatedBy: 'user-owner-1', updatedAt: 3000 },
      },
      {
        type: 'set',
        path: 'characters/char-rb-1/fields/money',
        data: { key: 'money', value: 550, revision: 2, lastOperationId: 'op-post-2', updatedBy: 'user-owner-1', updatedAt: 3000 },
      },
      {
        type: 'set',
        path: 'characters/char-rb-1/operations/op-post-2',
        data: {
          id: 'op-post-2',
          actorUid: 'user-owner-1',
          type: 'ADJUST_RESOURCE',
          outcome: 'COMMITTED',
          schemaVersion: 2,
          createdAt: 3000,
          targets: [
            { kind: 'FIELD', itemId: '', key: 'life', op: 'DELTA', expectedRevision: 1, nextRevision: 2, delta: -5 },
            { kind: 'FIELD', itemId: '', key: 'money', op: 'DELTA', expectedRevision: 1, nextRevision: 2, delta: 100 },
          ],
          preconditions: { payloadHash: digest({ delta: true }) },
        },
      },
      // 3. UPSERT_ITEM adding a brand-new inventory item with granular fields
      {
        type: 'set',
        path: 'characters/char-rb-1/items/inv_new-sword',
        data: { id: 'inv_new-sword', kind: 'INVENTORY', deleted: false, revision: 1, lastOperationId: 'op-post-3', updatedBy: 'user-owner-1', updatedAt: 4000 },
      },
      {
        type: 'set',
        path: 'characters/char-rb-1/items/inv_new-sword/fields/name',
        data: { key: 'name', value: 'Espada Lunar', revision: 1, lastOperationId: 'op-post-3', updatedBy: 'user-owner-1', updatedAt: 4000 },
      },
      {
        type: 'set',
        path: 'characters/char-rb-1/items/inv_new-sword/fields/load',
        data: { key: 'load', value: 2, revision: 1, lastOperationId: 'op-post-3', updatedBy: 'user-owner-1', updatedAt: 4000 },
      },
      {
        type: 'set',
        path: 'characters/char-rb-1/items/inv_new-sword/fields/quantity',
        data: { key: 'quantity', value: 1, revision: 1, lastOperationId: 'op-post-3', updatedBy: 'user-owner-1', updatedAt: 4000 },
      },
      {
        type: 'set',
        path: 'characters/char-rb-1/operations/op-post-3',
        data: {
          id: 'op-post-3',
          actorUid: 'user-owner-1',
          type: 'UPSERT_ITEM',
          outcome: 'COMMITTED',
          schemaVersion: 2,
          createdAt: 4000,
          targets: [
            { kind: 'ITEM', itemId: 'inv_new-sword', key: '', op: 'CREATE', expectedRevision: 0, nextRevision: 1 },
            { kind: 'ITEM_FIELD', itemId: 'inv_new-sword', key: 'name', op: 'CREATE', expectedRevision: 0, nextRevision: 1, baseValue: null, desiredValue: 'Espada Lunar' },
            { kind: 'ITEM_FIELD', itemId: 'inv_new-sword', key: 'quantity', op: 'CREATE', expectedRevision: 0, nextRevision: 1, baseValue: null, desiredValue: 1 },
          ],
          preconditions: { payloadHash: digest({ newSword: true }) },
        },
      },
      // 4. DELETE_ITEM tombstoning old-dagger
      {
        type: 'set',
        path: 'characters/char-rb-1/items/inv_old-dagger',
        data: { id: 'inv_old-dagger', kind: 'INVENTORY', deleted: true, revision: 2, lastOperationId: 'op-post-4', updatedBy: 'user-owner-1', updatedAt: 5000 },
      },
      {
        type: 'set',
        path: 'characters/char-rb-1/operations/op-post-4',
        data: {
          id: 'op-post-4',
          actorUid: 'user-owner-1',
          type: 'DELETE_ITEM',
          outcome: 'COMMITTED',
          schemaVersion: 2,
          createdAt: 5000,
          targets: [
            { kind: 'ITEM', itemId: 'inv_old-dagger', key: '', op: 'TOMBSTONE', expectedRevision: 1, nextRevision: 2 },
          ],
          preconditions: { payloadHash: digest({ tombstone: 'inv_old-dagger' }) },
        },
      },
      // 5. SET_LOCK locking sheet as HISTORIAN
      {
        type: 'set',
        path: 'characters/char-rb-1',
        data: { ...(await store.getDoc('characters/char-rb-1')), lockType: 'HISTORIAN', isLocked: true, lockedBy: 'gm-1', lockedAt: 6000, revision: 2, lastOperationId: 'op-post-5', updatedBy: 'gm-1', updatedAt: 6000 },
      },
      {
        type: 'set',
        path: 'characters/char-rb-1/operations/op-post-5',
        data: {
          id: 'op-post-5',
          actorUid: 'gm-1',
          type: 'SET_LOCK',
          outcome: 'COMMITTED',
          schemaVersion: 2,
          createdAt: 6000,
          targets: [
            { kind: 'SHEET', itemId: '', key: '', op: 'LOCK', expectedRevision: 1, nextRevision: 2 },
          ],
          preconditions: { payloadHash: digest({ lockType: 'HISTORIAN' }), lockType: 'HISTORIAN' },
        },
      },
    ]);

    // Corrupted backupRecord must refuse rollback
    const badBackup = structuredClone(backupRecord);
    badBackup.legacyDoc.money = 999999;
    await assert.rejects(
      () => rollbackSheetPreservingPostCutover({ store, charId: 'char-rb-1', backupRecord: badBackup }),
      /checksum/i
    );

    // Dry-run rollback inspects and produces diff without overwriting Spark root
    const dryRollback = await rollbackSheetPreservingPostCutover({
      store,
      charId: 'char-rb-1',
      backupRecord,
      dryRun: true,
    });
    assert.equal(dryRollback.status, 'DRY_RUN_ROLLBACK_READY');
    assert.equal(dryRollback.postCutoverOpCount, 5);
    assert.equal((await store.getDoc('characters/char-rb-1')).protocolVersion, 2);

    // Execute rollback with audit export
    const auditPath = path.join(tmpDir, 'audit-diff.json');
    const execRollback = await rollbackSheetPreservingPostCutover({
      store,
      charId: 'char-rb-1',
      backupRecord,
      dryRun: false,
      auditOutPath: auditPath,
    });
    assert.equal(execRollback.status, 'ROLLED_BACK');
    assert.equal(execRollback.postCutoverOpCount, 5);

    const restored = await store.getDoc('characters/char-rb-1');
    // Post-cutover edits ALL retained!
    assert.equal(restored.name, 'Kaelen Pós-Corte');
    assert.equal(restored.story, 'Sobreviveu ao corte Spark.');
    assert.equal(restored.life.current, 13);
    assert.equal(restored.money, 550);
    assert.deepEqual(restored.inventory.map(i => i.id), ['new-sword']);
    assert.equal(restored.inventory[0].name, 'Espada Lunar');
    assert.equal(restored.lockType, 'HISTORIAN');
    assert.equal(restored.isLocked, true);
    assert.equal(restored.lockedBy, 'gm-1');
    // Pre-cutover >64KiB photo preserved from backup!
    assert.equal(restored.photoUri, hugePhoto);
    // Audit diff exported on disk
    const auditDisk = JSON.parse(await fs.readFile(auditPath, 'utf8'));
    assert.equal(auditDisk.auditChecksum, execRollback.auditChecksum);
    assert.ok(auditDisk.changes.some(c => c.field === 'name'));
    assert.ok(auditDisk.changes.some(c => c.field === 'money'));
    assert.ok(auditDisk.changes.some(c => c.field === 'inventory'));
    // Immutable operations audit trail kept intact
    assert.ok((await store.listCollection('characters/char-rb-1/operations')).length >= 5);
  } finally {
    await fs.rm(tmpDir, { recursive: true, force: true });
  }
});

test('CLI inventory, backup, and rollback commands work end-to-end with dry-run default and prod guard', async () => {
  const tmpDir = await fs.mkdtemp(path.join(os.tmpdir(), 'sdo-cli-test-'));
  try {
    const smallPhoto = makePngDataUri(64, 64, 10);
    const largePhoto = makePngDataUri(256, 256, 60000);
    const store = createInMemoryStore({
      'characters/c-small': buildRichLegacyCharacter({ id: 'c-small', photoUri: smallPhoto }),
      'characters/c-large': buildRichLegacyCharacter({ id: 'c-large', photoUri: largePhoto, deleted: true }),
    });

    // Inventory
    const invReport = await runCli(['inventory'], store);
    assert.equal(invReport.discovered, 2);
    assert.equal(invReport.verified, 2);
    assert.equal(invReport.failed, 0);
    assert.equal(invReport.mediaInventory.length, 2);
    assert.equal(invReport.mediaInventory.find(m => m.charId === 'c-small').migratedToThumbnail, true);
    assert.equal(invReport.mediaInventory.find(m => m.charId === 'c-large').requiresLocalPreservation, true);

    // Backup
    const backupFile = path.join(tmpDir, 'bundle.json');
    const backupRes = await runCli(['backup', '--out', backupFile], store);
    assert.equal(backupRes.status, 'BACKUP_WRITTEN');
    assert.equal(backupRes.recordCount, 2);

    // Migrate single sheet via CLI --execute --confirm-prod
    const migRes = await runCli(['migrate', '--sheet', 'c-small', '--execute', '--confirm-prod'], store);
    assert.equal(migRes.status, 'MIGRATED_VERIFIED');

    // Post-cutover edit on attribute skill and delivery ID
    const rootDoc = await store.getDoc('characters/c-small');
    await store.commitBatch([
      {
        type: 'set',
        path: 'characters/c-small',
        data: { ...rootDoc, appliedDeliveryIds: ['deliv-100', 'deliv-200'], revision: 2, lastOperationId: 'op-deliv', updatedBy: 'gm-1', updatedAt: 9000 },
      },
      {
        type: 'set',
        path: 'characters/c-small/items/attr_FOR/fields/skill_Atletismo_value',
        data: { key: 'skill_Atletismo_value', value: 5, revision: 2, lastOperationId: 'op-deliv', updatedBy: 'user-owner-1', updatedAt: 9000 },
      },
      {
        type: 'set',
        path: 'characters/c-small/operations/op-deliv',
        data: {
          id: 'op-deliv',
          actorUid: 'gm-1',
          type: 'ACCEPT_DELIVERY',
          outcome: 'COMMITTED',
          schemaVersion: 2,
          createdAt: 9000,
          targets: [
            { kind: 'SHEET', itemId: '', key: '', op: 'DELIVERY', expectedRevision: 1, nextRevision: 2 },
            { kind: 'ITEM_FIELD', itemId: 'attr_FOR', key: 'skill_Atletismo_value', op: 'SET', expectedRevision: 1, nextRevision: 2, baseValue: 2, desiredValue: 5 },
          ],
          preconditions: { payloadHash: digest({ deliveryId: 'deliv-200' }) },
        },
      },
    ]);

    // Rollback without --execute is dry-run
    const dryRb = await runCli(['rollback', '--sheet', 'c-small', '--backup', backupFile], store);
    assert.equal(dryRb.status, 'DRY_RUN_ROLLBACK_READY');
    assert.equal((await store.getDoc('characters/c-small')).protocolVersion, 2);

    // Rollback --execute without --confirm-prod is rejected
    await assert.rejects(
      () => runCli(['rollback', '--sheet', 'c-small', '--backup', backupFile, '--execute'], store),
      /--confirm-prod/
    );

    // Rollback --execute --confirm-prod retains both post-cutover delivery and skill update
    const auditFile = path.join(tmpDir, 'cli-audit.json');
    const execRb = await runCli(
      ['rollback', '--sheet', 'c-small', '--backup', backupFile, '--audit-out', auditFile, '--execute', '--confirm-prod'],
      store
    );
    assert.equal(execRb.status, 'ROLLED_BACK');
    const restored = await store.getDoc('characters/c-small');
    assert.deepEqual(restored.appliedDeliveryIds, ['deliv-100', 'deliv-200']);
    const forAttr = restored.attributes.find(a => a.acronym === 'FOR');
    assert.equal(forAttr.skills.find(s => s.name === 'Atletismo').value, 5);
  } finally {
    await fs.rm(tmpDir, { recursive: true, force: true });
  }
});

test('two-phase staging prevents partial migration exposure and resumes cleanly after mid-migration failure', async () => {
  const legacy = buildRichLegacyCharacter({ id: 'char-resume-1' });
  const innerStore = createInMemoryStore({ 'characters/char-resume-1': legacy });

  let commitCount = 0;
  const flakyStore = {
    getDoc: p => innerStore.getDoc(p),
    listCollection: p => innerStore.listCollection(p),
    async commitBatch(writes) {
      commitCount++;
      // Fail on the 4th batch (mid-way through staging)
      if (commitCount === 4) {
        throw new Error('Simulated transient network outage mid-staging');
      }
      return innerStore.commitBatch(writes);
    },
  };

  await assert.rejects(
    () => migrateSheet({ store: flakyStore, charId: 'char-resume-1', dryRun: false, timestamp: 5000 }),
    /Simulated transient network outage/
  );

  // Root MUST still NOT have protocolVersion=2, and manifest MUST be STAGING
  const midRoot = await innerStore.getDoc('characters/char-resume-1');
  const midManifest = await innerStore.getDoc('characters/char-resume-1/migration/v2');
  assert.notEqual(midRoot.protocolVersion, 2, 'Partial failure must never activate protocolVersion=2');
  assert.equal(midManifest.status, 'STAGING');

  // Resume on healthy store: must NOT wrongly return ALREADY_MIGRATED; must complete and verify!
  const resumed = await migrateSheet({ store: innerStore, charId: 'char-resume-1', dryRun: false, timestamp: 6000 });
  assert.equal(resumed.status, 'MIGRATED_VERIFIED');
  assert.equal((await innerStore.getDoc('characters/char-resume-1')).protocolVersion, 2);
  assert.equal((await innerStore.getDoc('characters/char-resume-1/migration/v2')).status, 'VERIFIED');

  // Rerun when verified and intact returns ALREADY_MIGRATED
  const rerun = await migrateSheet({ store: innerStore, charId: 'char-resume-1', dryRun: false, timestamp: 7000 });
  assert.equal(rerun.status, 'ALREADY_MIGRATED');

  // Corrupting a staged subcollection doc after VERIFIED must fail parity/checksum instead of returning ALREADY_MIGRATED
  await innerStore.commitBatch([{ type: 'delete', path: 'characters/char-resume-1/fields/name' }]);
  await assert.rejects(
    () => migrateSheet({ store: innerStore, charId: 'char-resume-1', dryRun: false, timestamp: 8000 }),
    /subcollection checksum verification|parity check/i
  );

  // Root with protocolVersion=2 without VERIFIED manifest must fail loudly
  await innerStore.commitBatch([
    { type: 'set', path: 'characters/char-resume-1/migration/v2', data: { status: 'STAGING' } },
  ]);
  await assert.rejects(
    () => migrateSheet({ store: innerStore, charId: 'char-resume-1', dryRun: false, timestamp: 9000 }),
    /without a VERIFIED migration manifest/i
  );
});
