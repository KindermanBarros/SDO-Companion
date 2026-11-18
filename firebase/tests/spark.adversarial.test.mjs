import { after, before, beforeEach, test } from 'node:test';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import assert from 'node:assert/strict';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  collection,
  collectionGroup,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  runTransaction,
  serverTimestamp,
  setDoc,
  Timestamp,
  updateDoc,
  where,
  writeBatch,
} from 'firebase/firestore';

const here = dirname(fileURLToPath(import.meta.url));
const rules = readFileSync(resolve(here, '../firestore.rules'), 'utf8');
const projectId = 'sdo-companion-test';
let env;

const ids = {
  campaign: 'camp-adv',
  gm: 'gm-1',
  owner: 'player-1',
  peer: 'player-2',
  outsider: 'outsider-1',
  char: 'char-adv-1',
};

function makeRoot(overrides = {}) {
  return {
    id: ids.char,
    ownerId: ids.owner,
    campaignId: '',
    authorizedCampaignId: '',
    deleted: false,
    schemaVersion: 2,
    canonicalSchemaVersion: 1,
    itemSchemaVersion: 1,
    protocolVersion: 2,
    lockType: 'NONE',
    isLocked: false,
    lockedBy: '',
    lockedAt: null,
    revision: 1,
    lastOperationId: 'op-seed',
    updatedBy: ids.owner,
    updatedAt: Timestamp.fromMillis(1000),
    appliedDeliveryIds: [],
    ...overrides,
  };
}

function makeField(key, value, overrides = {}) {
  return {
    key,
    value,
    revision: 1,
    lastOperationId: 'op-seed',
    updatedBy: ids.owner,
    updatedAt: Timestamp.fromMillis(1000),
    ...overrides,
  };
}

function makeItem(id, kind, overrides = {}) {
  return {
    id,
    kind,
    deleted: false,
    revision: 1,
    lastOperationId: 'op-seed',
    updatedBy: ids.owner,
    updatedAt: Timestamp.fromMillis(1000),
    ...overrides,
  };
}

function makeOp(opId, actorUid, type, targets, preconditions = { payloadHash: 'sha256-test' }) {
  return {
    id: opId,
    actorUid,
    type,
    outcome: 'COMMITTED',
    schemaVersion: 2,
    createdAt: serverTimestamp(),
    targets,
    preconditions,
  };
}

async function seedBase({ linked = true, archived = false } = {}) {
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, 'users', ids.owner), { displayName: 'Player 1', role: 'USER' });
    await setDoc(doc(db, 'users', ids.peer), { displayName: 'Player 2', role: 'USER' });
    await setDoc(doc(db, 'campaigns', ids.campaign), {
      id: ids.campaign,
      name: 'Campanha Spark',
      description: 'Teste',
      ownerId: ids.gm,
      state: archived ? 'ARCHIVED' : 'ACTIVE',
      createdAt: 1,
      updatedAt: 1,
      archivedAt: archived ? 2 : null,
    });
    await setDoc(doc(db, 'campaigns', ids.campaign, 'members', ids.owner), {
      campaignId: ids.campaign,
      userId: ids.owner,
      role: 'PLAYER',
      state: 'ACTIVE',
      characterIds: linked ? [ids.char] : [],
      joinedAt: 1,
      updatedAt: 1,
      joinedByInviteId: 'INV1',
    });
    await setDoc(doc(db, 'campaigns', ids.campaign, 'members', ids.peer), {
      campaignId: ids.campaign,
      userId: ids.peer,
      role: 'PLAYER',
      state: 'ACTIVE',
      characterIds: [],
      joinedAt: 1,
      updatedAt: 1,
      joinedByInviteId: 'INV1',
    });
    await setDoc(doc(db, 'characters', ids.char), makeRoot({
      campaignId: linked ? ids.campaign : '',
    }));
    await setDoc(doc(db, 'characters', ids.char, 'fields', 'name'), makeField('name', 'Aria'));
    await setDoc(doc(db, 'characters', ids.char, 'fields', 'money'), makeField('money', 100));
    await setDoc(doc(db, 'characters', ids.char, 'fields', 'life'), makeField('life', {
      current: 12,
      maximum: 15,
      adjustment: 0,
    }));
    await setDoc(doc(db, 'characters', ids.char, 'items', 'inv_potion'), makeItem('inv_potion', 'INVENTORY'));
    await setDoc(doc(db, 'characters', ids.char, 'items', 'inv_potion', 'fields', 'quantity'), makeField('quantity', 2));
    await setDoc(doc(db, 'characters', ids.char, 'items', 'inv_potion', 'fields', 'name'), makeField('name', 'Poção'));
  });
}

before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: { rules },
  });
});

beforeEach(async () => {
  await env.clearFirestore();
});

after(async () => {
  await env.cleanup();
});

test('rejects legacy protocolVersion != 2 and campaignId == default on characters', async () => {
  const db = env.authenticatedContext(ids.owner).firestore();
  for (const badOverrides of [
    { protocolVersion: 1 },
    { protocolVersion: 0 },
    { schemaVersion: 1 },
    { campaignId: 'default' },
  ]) {
    const batch = writeBatch(db);
    batch.set(doc(db, 'characters', 'legacy-sheet'), makeRoot({
      id: 'legacy-sheet',
      lastOperationId: 'op-legacy',
      updatedAt: serverTimestamp(),
      ...badOverrides,
    }));
    batch.set(
      doc(db, 'characters', 'legacy-sheet', 'operations', 'op-legacy'),
      makeOp('op-legacy', ids.owner, 'CREATE_SHEET', [
        { kind: 'SHEET', itemId: '', key: '', op: 'CREATE', expectedRevision: 0, nextRevision: 1 },
      ]),
    );
    await assertFails(batch.commit());
  }
});

test('blocks owner spoof, actor spoof, GM self-promotion, peer member access, and unverified admin bypass while allowing verified admin edits', async () => {
  await seedBase();
  const peerDb = env.authenticatedContext(ids.peer).firestore();
  const outsiderDb = env.authenticatedContext(ids.outsider).firestore();
  const gmDb = env.authenticatedContext(ids.gm).firestore();
  const unverifiedAdminDb = env.authenticatedContext('admin-unverified', {
    email: 'kindbarros@gmail.com',
    email_verified: false,
  }).firestore();
  const adminDb = env.authenticatedContext('admin-uid', {
    email: 'kindbarros@gmail.com',
    email_verified: true,
  }).firestore();

  // Strict ACL: peer member, outsider, and unverified admin cannot read another user's sheet
  await assertFails(getDoc(doc(peerDb, 'characters', ids.char)));
  await assertFails(getDoc(doc(peerDb, 'characters', ids.char, 'fields', 'name')));
  await assertFails(getDoc(doc(unverifiedAdminDb, 'characters', ids.char)));
  await assertFails(getDoc(doc(outsiderDb, 'characters', ids.char)));

  // Verified admin can read and edit another user's sheet fields & item quantities
  await assertSucceeds(getDoc(doc(adminDb, 'characters', ids.char)));
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, 'characters', ids.char, 'items', 'inv_potion'), makeItem('inv_potion', 'INVENTORY'));
    await setDoc(doc(db, 'characters', ids.char, 'items', 'inv_potion', 'fields', 'quantity'), makeField('quantity', 2));
  });
  const adminEditItemQty = writeBatch(adminDb);
  adminEditItemQty.update(doc(adminDb, 'characters', ids.char, 'items', 'inv_potion', 'fields', 'quantity'), {
    value: 5,
    revision: 2,
    lastOperationId: 'op-admin-qty',
    updatedBy: 'admin-uid',
    updatedAt: serverTimestamp(),
  });
  adminEditItemQty.set(
    doc(adminDb, 'characters', ids.char, 'operations', 'op-admin-qty'),
    makeOp('op-admin-qty', 'admin-uid', 'PATCH_FIELDS', [
      { kind: 'ITEM_FIELD', itemId: 'inv_potion', key: 'quantity', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: 5 },
    ]),
  );
  await assertSucceeds(adminEditItemQty.commit());

  // Outsider tries to create sheet claiming ownerId == owner
  const spoofCreate = writeBatch(outsiderDb);
  spoofCreate.set(doc(outsiderDb, 'characters', 'spoof-char'), makeRoot({
    id: 'spoof-char',
    ownerId: ids.owner,
    lastOperationId: 'op-spoof',
    updatedBy: ids.outsider,
    updatedAt: serverTimestamp(),
  }));
  spoofCreate.set(
    doc(outsiderDb, 'characters', 'spoof-char', 'operations', 'op-spoof'),
    makeOp('op-spoof', ids.outsider, 'CREATE_SHEET', [
      { kind: 'SHEET', itemId: '', key: '', op: 'CREATE', expectedRevision: 0, nextRevision: 1 },
    ]),
  );
  await assertFails(spoofCreate.commit());

  // Actor spoof inside receipt (actorUid != auth.uid)
  const actorSpoof = writeBatch(peerDb);
  actorSpoof.update(doc(peerDb, 'characters', ids.char, 'fields', 'name'), {
    value: 'Hacked',
    revision: 2,
    lastOperationId: 'op-actor-spoof',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  actorSpoof.set(
    doc(peerDb, 'characters', ids.char, 'operations', 'op-actor-spoof'),
    makeOp('op-actor-spoof', ids.owner, 'PATCH_FIELDS', [
      { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: 'Hacked' },
    ]),
  );
  await assertFails(actorSpoof.commit());

  // GM self-promotion on members/{uid} and campaign ownerId transfer
  await assertFails(updateDoc(doc(peerDb, 'campaigns', ids.campaign, 'members', ids.peer), {
    role: 'GM',
    updatedAt: 99,
  }));
  await assertFails(updateDoc(doc(peerDb, 'campaigns', ids.campaign, 'members', ids.peer), {
    role: 'HISTORIAN',
    updatedAt: 99,
  }));
  await assertFails(updateDoc(doc(peerDb, 'campaigns', ids.campaign), {
    ownerId: ids.peer,
    updatedAt: 99,
  }));

  // GM cannot steal ownerId on TRANSFER_OWNERSHIP
  const gmSteal = writeBatch(gmDb);
  gmSteal.update(doc(gmDb, 'characters', ids.char), {
    ownerId: ids.gm,
    revision: 2,
    lastOperationId: 'op-gm-steal',
    updatedBy: ids.gm,
    updatedAt: serverTimestamp(),
  });
  gmSteal.set(
    doc(gmDb, 'characters', ids.char, 'operations', 'op-gm-steal'),
    makeOp('op-gm-steal', ids.gm, 'TRANSFER_OWNERSHIP', [
      { kind: 'SHEET', itemId: '', key: '', op: 'TRANSFER', expectedRevision: 1, nextRevision: 2 },
    ]),
  );
  await assertFails(gmSteal.commit());
});

test('enforces mutual getAfter: rejects receipt without effect, effect without receipt, desiredValue mismatch, reused receipt, and reused opId', async () => {
  await seedBase();
  const db = env.authenticatedContext(ids.owner).firestore();

  // 1. Receipt without effect
  await assertFails(setDoc(
    doc(db, 'characters', ids.char, 'operations', 'op-no-effect'),
    makeOp('op-no-effect', ids.owner, 'PATCH_FIELDS', [
      { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: 'Fantasma' },
    ]),
  ));

  // 2. Effect without receipt
  await assertFails(updateDoc(doc(db, 'characters', ids.char, 'fields', 'name'), {
    value: 'Sem recibo',
    revision: 2,
    lastOperationId: 'op-missing',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  }));

  // 3. Receipt desiredValue mismatch (client writes different value than receipt declares)
  const mismatchBatch = writeBatch(db);
  mismatchBatch.update(doc(db, 'characters', ids.char, 'fields', 'name'), {
    value: 'Valor Adulterado',
    revision: 2,
    lastOperationId: 'op-mismatch',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  mismatchBatch.set(
    doc(db, 'characters', ids.char, 'operations', 'op-mismatch'),
    makeOp('op-mismatch', ids.owner, 'PATCH_FIELDS', [
      { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: 'Aria v2' },
    ]),
  );
  await assertFails(mismatchBatch.commit());

  // 4. Valid commit creates op-valid-1
  const validBatch = writeBatch(db);
  validBatch.update(doc(db, 'characters', ids.char, 'fields', 'name'), {
    value: 'Aria v2',
    revision: 2,
    lastOperationId: 'op-valid-1',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  validBatch.set(
    doc(db, 'characters', ids.char, 'operations', 'op-valid-1'),
    makeOp('op-valid-1', ids.owner, 'PATCH_FIELDS', [
      { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 1, nextRevision: 2, baseValue: 'Aria', desiredValue: 'Aria v2' },
    ]),
  );
  await assertSucceeds(validBatch.commit());

  // 5. Reused receipt: referencing existing op-valid-1 fails
  await assertFails(updateDoc(doc(db, 'characters', ids.char, 'fields', 'name'), {
    value: 'Reused receipt',
    revision: 3,
    lastOperationId: 'op-valid-1',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  }));

  // 6. Reused opId with different payload fails
  const replayBatch = writeBatch(db);
  replayBatch.update(doc(db, 'characters', ids.char, 'fields', 'name'), {
    value: 'Replay payload',
    revision: 3,
    lastOperationId: 'op-valid-1',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  replayBatch.set(
    doc(db, 'characters', ids.char, 'operations', 'op-valid-1'),
    makeOp('op-valid-1', ids.owner, 'PATCH_FIELDS', [
      { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 2, nextRevision: 3, desiredValue: 'Replay payload' },
    ], { payloadHash: 'different-hash' }),
  );
  await assertFails(replayBatch.commit());
  await assertFails(updateDoc(doc(db, 'characters', ids.char, 'operations', 'op-valid-1'), { outcome: 'REJECTED' }));
  await assertFails(deleteDoc(doc(db, 'characters', ids.char, 'operations', 'op-valid-1')));
});

test('owner can create and update the structured transports and familiars fields', async () => {
  await seedBase();
  const db = env.authenticatedContext(ids.owner).firestore();
  for (const key of ['transports', 'familiars']) {
    const value = JSON.stringify([{ id: `${key}-1`, name: 'Cavalo' }]);
    const opId = `op-create-${key}`;
    const batch = writeBatch(db);
    batch.set(doc(db, 'characters', ids.char, 'fields', key), {
      key,
      value,
      revision: 1,
      lastOperationId: opId,
      updatedBy: ids.owner,
      updatedAt: serverTimestamp(),
    });
    batch.set(
      doc(db, 'characters', ids.char, 'operations', opId),
      makeOp(opId, ids.owner, 'PATCH_FIELDS', [
        { kind: 'FIELD', itemId: '', key, op: 'CREATE', expectedRevision: 0, nextRevision: 1, desiredValue: value },
      ]),
    );
    await assertSucceeds(batch.commit());
  }
});

test('rejects extra fields, non-allowlisted field keys, coarse item bundles, invalid types, and invalid revisions', async () => {
  await seedBase();
  const db = env.authenticatedContext(ids.owner).firestore();

  // Extra field on fields/name
  const extraFieldBatch = writeBatch(db);
  extraFieldBatch.update(doc(db, 'characters', ids.char, 'fields', 'name'), {
    value: 'Aria',
    revision: 2,
    lastOperationId: 'op-extra-1',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
    isAdmin: true,
  });
  extraFieldBatch.set(
    doc(db, 'characters', ids.char, 'operations', 'op-extra-1'),
    makeOp('op-extra-1', ids.owner, 'PATCH_FIELDS', [
      { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: 'Aria' },
    ]),
  );
  await assertFails(extraFieldBatch.commit());

  // Non-allowlisted fieldKey
  const bogusKeyBatch = writeBatch(db);
  bogusKeyBatch.set(doc(db, 'characters', ids.char, 'fields', 'hackedKey'), {
    key: 'hackedKey',
    value: 'anything',
    revision: 1,
    lastOperationId: 'op-bogus-key',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  bogusKeyBatch.set(
    doc(db, 'characters', ids.char, 'operations', 'op-bogus-key'),
    makeOp('op-bogus-key', ids.owner, 'PATCH_FIELDS', [
      { kind: 'FIELD', itemId: '', key: 'hackedKey', op: 'CREATE', expectedRevision: 0, nextRevision: 1, desiredValue: 'anything' },
    ]),
  );
  await assertFails(bogusKeyBatch.commit());

  // Rejected coarse bundle on inv_potion (must use granular fields like name/quantity)
  const coarseBatch = writeBatch(db);
  coarseBatch.set(doc(db, 'characters', ids.char, 'items', 'inv_potion', 'fields', 'identity'), {
    key: 'identity',
    value: { name: 'Poção' },
    revision: 1,
    lastOperationId: 'op-coarse',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  coarseBatch.set(
    doc(db, 'characters', ids.char, 'operations', 'op-coarse'),
    makeOp('op-coarse', ids.owner, 'PATCH_FIELDS', [
      { kind: 'ITEM_FIELD', itemId: 'inv_potion', key: 'identity', op: 'CREATE', expectedRevision: 0, nextRevision: 1, desiredValue: { name: 'Poção' } },
    ]),
  );
  await assertFails(coarseBatch.commit());

  // Out-of-bounds level (21)
  const badLevelBatch = writeBatch(db);
  badLevelBatch.set(doc(db, 'characters', ids.char, 'fields', 'level'), {
    key: 'level',
    value: 21,
    revision: 1,
    lastOperationId: 'op-bad-lvl',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  badLevelBatch.set(
    doc(db, 'characters', ids.char, 'operations', 'op-bad-lvl'),
    makeOp('op-bad-lvl', ids.owner, 'PATCH_FIELDS', [
      { kind: 'FIELD', itemId: '', key: 'level', op: 'CREATE', expectedRevision: 0, nextRevision: 1, desiredValue: 21 },
    ]),
  );
  await assertFails(badLevelBatch.commit());

  // Invalid revision jump (1 -> 3)
  const badRevBatch = writeBatch(db);
  badRevBatch.update(doc(db, 'characters', ids.char, 'fields', 'name'), {
    value: 'Aria',
    revision: 3,
    lastOperationId: 'op-bad-rev',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  badRevBatch.set(
    doc(db, 'characters', ids.char, 'operations', 'op-bad-rev'),
    makeOp('op-bad-rev', ids.owner, 'PATCH_FIELDS', [
      { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 1, nextRevision: 3, desiredValue: 'Aria' },
    ]),
  );
  await assertFails(badRevBatch.commit());

  // Invalid attr_FAKE item
  const badAttrBatch = writeBatch(db);
  badAttrBatch.set(doc(db, 'characters', ids.char, 'items', 'attr_FAKE'), {
    id: 'attr_FAKE',
    kind: 'ATTRIBUTE',
    deleted: false,
    revision: 1,
    lastOperationId: 'op-bad-attr',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  badAttrBatch.set(
    doc(db, 'characters', ids.char, 'operations', 'op-bad-attr'),
    makeOp('op-bad-attr', ids.owner, 'UPSERT_ITEM', [
      { kind: 'ITEM', itemId: 'attr_FAKE', key: '', op: 'CREATE', expectedRevision: 0, nextRevision: 1 },
    ]),
  );
  await assertFails(badAttrBatch.commit());
});

test('enforces DELTA arithmetic and bounds and rejects illegal balance or partial multi-effect actions', async () => {
  await seedBase();
  const db = env.authenticatedContext(ids.owner).firestore();

  // Valid DELTA on money (100 - 25 = 75)
  const goodMoney = writeBatch(db);
  goodMoney.update(doc(db, 'characters', ids.char, 'fields', 'money'), {
    value: 75,
    revision: 2,
    lastOperationId: 'op-delta-money-ok',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  goodMoney.set(
    doc(db, 'characters', ids.char, 'operations', 'op-delta-money-ok'),
    makeOp('op-delta-money-ok', ids.owner, 'ADJUST_RESOURCE', [
      { kind: 'FIELD', itemId: '', key: 'money', op: 'DELTA', expectedRevision: 1, nextRevision: 2, delta: -25 },
    ]),
  );
  await assertSucceeds(goodMoney.commit());

  // Illegal balance: money 75 - 100 = -25 fails
  const negMoney = writeBatch(db);
  negMoney.update(doc(db, 'characters', ids.char, 'fields', 'money'), {
    value: -25,
    revision: 3,
    lastOperationId: 'op-delta-neg',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  negMoney.set(
    doc(db, 'characters', ids.char, 'operations', 'op-delta-neg'),
    makeOp('op-delta-neg', ids.owner, 'ADJUST_RESOURCE', [
      { kind: 'FIELD', itemId: '', key: 'money', op: 'DELTA', expectedRevision: 2, nextRevision: 3, delta: -100 },
    ]),
  );
  await assertFails(negMoney.commit());

  // Arithmetic lie: receipt says delta -5 on life (12 -> 7) but writes current: 20
  const lieLife = writeBatch(db);
  lieLife.update(doc(db, 'characters', ids.char, 'fields', 'life'), {
    value: { current: 20, maximum: 15, adjustment: 0 },
    revision: 2,
    lastOperationId: 'op-delta-lie',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  lieLife.set(
    doc(db, 'characters', ids.char, 'operations', 'op-delta-lie'),
    makeOp('op-delta-lie', ids.owner, 'ADJUST_RESOURCE', [
      { kind: 'FIELD', itemId: '', key: 'life', op: 'DELTA', expectedRevision: 1, nextRevision: 2, delta: -5 },
    ]),
  );
  await assertFails(lieLife.commit());

  // Omitting negative item delta at targets[0] under ITEM_CONSUME (only positive life heal) fails
  const healOnlyConsume = writeBatch(db);
  healOnlyConsume.update(doc(db, 'characters', ids.char, 'fields', 'life'), {
    value: { current: 15, maximum: 15, adjustment: 0 },
    revision: 2,
    lastOperationId: 'op-heal-only',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  healOnlyConsume.set(
    doc(db, 'characters', ids.char, 'operations', 'op-heal-only'),
    makeOp('op-heal-only', ids.owner, 'ITEM_CONSUME', [
      { kind: 'FIELD', itemId: '', key: 'life', op: 'DELTA', expectedRevision: 1, nextRevision: 2, delta: 3 },
    ]),
  );
  await assertFails(healOnlyConsume.commit());

  // Partial multi-effect ITEM_CONSUME: receipt declares inv_potion/quantity + life, but omits quantity write
  const partialConsume = writeBatch(db);
  partialConsume.update(doc(db, 'characters', ids.char, 'fields', 'life'), {
    value: { current: 15, maximum: 15, adjustment: 0 },
    revision: 2,
    lastOperationId: 'op-partial',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  partialConsume.set(
    doc(db, 'characters', ids.char, 'operations', 'op-partial'),
    makeOp('op-partial', ids.owner, 'ITEM_CONSUME', [
      { kind: 'ITEM_FIELD', itemId: 'inv_potion', key: 'quantity', op: 'DELTA', expectedRevision: 1, nextRevision: 2, delta: -1 },
      { kind: 'FIELD', itemId: '', key: 'life', op: 'DELTA', expectedRevision: 1, nextRevision: 2, delta: 3 },
    ]),
  );
  await assertFails(partialConsume.commit());

  // Complete atomic multi-effect ITEM_CONSUME succeeds (targets[0] is negative item delta)
  const fullConsume = writeBatch(db);
  fullConsume.update(doc(db, 'characters', ids.char, 'items', 'inv_potion', 'fields', 'quantity'), {
    value: 1,
    revision: 2,
    lastOperationId: 'op-full-consume',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  fullConsume.update(doc(db, 'characters', ids.char, 'fields', 'life'), {
    value: { current: 15, maximum: 15, adjustment: 0 },
    revision: 2,
    lastOperationId: 'op-full-consume',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  fullConsume.set(
    doc(db, 'characters', ids.char, 'operations', 'op-full-consume'),
    makeOp('op-full-consume', ids.owner, 'ITEM_CONSUME', [
      { kind: 'ITEM_FIELD', itemId: 'inv_potion', key: 'quantity', op: 'DELTA', expectedRevision: 1, nextRevision: 2, delta: -1 },
      { kind: 'FIELD', itemId: '', key: 'life', op: 'DELTA', expectedRevision: 1, nextRevision: 2, delta: 3 },
    ]),
  );
  await assertSucceeds(fullConsume.commit());

  // Consuming below 0 quantity fails
  const emptyConsume = writeBatch(db);
  emptyConsume.update(doc(db, 'characters', ids.char, 'items', 'inv_potion', 'fields', 'quantity'), {
    value: -1,
    revision: 3,
    lastOperationId: 'op-empty-consume',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  emptyConsume.set(
    doc(db, 'characters', ids.char, 'operations', 'op-empty-consume'),
    makeOp('op-empty-consume', ids.owner, 'ITEM_CONSUME', [
      { kind: 'ITEM_FIELD', itemId: 'inv_potion', key: 'quantity', op: 'DELTA', expectedRevision: 2, nextRevision: 3, delta: -2 },
    ]),
  );
  await assertFails(emptyConsume.commit());
});

test('tombstones prevent resurrection by common writes and block item field writes via getAfter', async () => {
  await seedBase();
  const db = env.authenticatedContext(ids.owner).firestore();

  const delBatch = writeBatch(db);
  delBatch.update(doc(db, 'characters', ids.char, 'items', 'inv_potion'), {
    deleted: true,
    revision: 2,
    lastOperationId: 'op-item-del',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  delBatch.set(
    doc(db, 'characters', ids.char, 'operations', 'op-item-del'),
    makeOp('op-item-del', ids.owner, 'DELETE_ITEM', [
      { kind: 'ITEM', itemId: 'inv_potion', key: '', op: 'TOMBSTONE', expectedRevision: 1, nextRevision: 2 },
    ]),
  );
  await assertSucceeds(delBatch.commit());

  await assertFails(deleteDoc(doc(db, 'characters', ids.char, 'items', 'inv_potion')));

  // Stale edit to granular item field fails because item.deleted == true via getAfter
  const staleField = writeBatch(db);
  staleField.update(doc(db, 'characters', ids.char, 'items', 'inv_potion', 'fields', 'quantity'), {
    value: 99,
    revision: 2,
    lastOperationId: 'op-stale-item-field',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  staleField.set(
    doc(db, 'characters', ids.char, 'operations', 'op-stale-item-field'),
    makeOp('op-stale-item-field', ids.owner, 'PATCH_FIELDS', [
      { kind: 'ITEM_FIELD', itemId: 'inv_potion', key: 'quantity', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: 99 },
    ]),
  );
  await assertFails(staleField.commit());

  // Common UPSERT_ITEM cannot resurrect tombstone
  const resurrectAttempt = writeBatch(db);
  resurrectAttempt.update(doc(db, 'characters', ids.char, 'items', 'inv_potion'), {
    deleted: false,
    revision: 3,
    lastOperationId: 'op-resurrect-bad',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  resurrectAttempt.set(
    doc(db, 'characters', ids.char, 'operations', 'op-resurrect-bad'),
    makeOp('op-resurrect-bad', ids.owner, 'UPSERT_ITEM', [
      { kind: 'ITEM', itemId: 'inv_potion', key: '', op: 'SET', expectedRevision: 2, nextRevision: 3 },
    ]),
  );
  await assertFails(resurrectAttempt.commit());

  // Explicit RESTORE_ITEM succeeds
  const restoreBatch = writeBatch(db);
  restoreBatch.update(doc(db, 'characters', ids.char, 'items', 'inv_potion'), {
    deleted: false,
    revision: 3,
    lastOperationId: 'op-restore-ok',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  restoreBatch.set(
    doc(db, 'characters', ids.char, 'operations', 'op-restore-ok'),
    makeOp('op-restore-ok', ids.owner, 'RESTORE_ITEM', [
      { kind: 'ITEM', itemId: 'inv_potion', key: '', op: 'RESTORE', expectedRevision: 2, nextRevision: 3 },
    ]),
  );
  await assertSucceeds(restoreBatch.commit());
});

test('archived campaign blocks edits, allows owner unlink, revokes GM access, and restores owner edit access', async () => {
  await seedBase({ archived: true });
  const ownerDb = env.authenticatedContext(ids.owner).firestore();
  const gmDb = env.authenticatedContext(ids.gm).firestore();

  const gmUnlink = writeBatch(gmDb);
  gmUnlink.update(doc(gmDb, 'characters', ids.char), {
    campaignId: '',
    authorizedCampaignId: '',
    revision: 2,
    lastOperationId: 'op-gm-arch-unlink',
    updatedBy: ids.gm,
    updatedAt: serverTimestamp(),
  });
  gmUnlink.set(
    doc(gmDb, 'characters', ids.char, 'operations', 'op-gm-arch-unlink'),
    makeOp('op-gm-arch-unlink', ids.gm, 'UNLINK_CAMPAIGN', [
      { kind: 'SHEET', itemId: '', key: '', op: 'UNLINK', expectedRevision: 1, nextRevision: 2 },
    ]),
  );
  await assertFails(gmUnlink.commit());

  const ownerUnlink = writeBatch(ownerDb);
  ownerUnlink.update(doc(ownerDb, 'characters', ids.char), {
    campaignId: '',
    authorizedCampaignId: '',
    revision: 2,
    lastOperationId: 'op-owner-arch-unlink',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  ownerUnlink.set(
    doc(ownerDb, 'characters', ids.char, 'operations', 'op-owner-arch-unlink'),
    makeOp('op-owner-arch-unlink', ids.owner, 'UNLINK_CAMPAIGN', [
      { kind: 'SHEET', itemId: '', key: '', op: 'UNLINK', expectedRevision: 1, nextRevision: 2 },
    ]),
  );
  await assertSucceeds(ownerUnlink.commit());

  await assertFails(getDoc(doc(gmDb, 'characters', ids.char)));

  const ownerEdit = writeBatch(ownerDb);
  ownerEdit.update(doc(ownerDb, 'characters', ids.char, 'fields', 'name'), {
    value: 'Aria Livre',
    revision: 2,
    lastOperationId: 'op-standalone-edit',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  ownerEdit.set(
    doc(ownerDb, 'characters', ids.char, 'operations', 'op-standalone-edit'),
    makeOp('op-standalone-edit', ids.owner, 'PATCH_FIELDS', [
      { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: 'Aria Livre' },
    ]),
  );
  await assertSucceeds(ownerEdit.commit());
});

test('invites reject replay, wrong recipientUid, expired invites, and uncoupled membership/invite writes', async () => {
  await seedBase();
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    const baseInvite = {
      campaignId: ids.campaign,
      campaignName: 'Campanha Spark',
      campaignDescription: 'Teste',
      createdBy: ids.gm,
      state: 'PENDING',
      consumedBy: '',
      consumedAt: null,
      revokedAt: null,
      createdAt: 1,
    };
    await setDoc(doc(db, 'campaigns', ids.campaign, 'invites', 'OPEN1111'), {
      ...baseInvite,
      id: 'OPEN1111',
      code: 'OPEN1111',
      recipientUid: '',
      expiresAt: Date.now() + 60000,
    });
    await setDoc(doc(db, 'campaigns', ids.campaign, 'invites', 'TARGET22'), {
      ...baseInvite,
      id: 'TARGET22',
      code: 'TARGET22',
      recipientUid: 'user-target',
      expiresAt: Date.now() + 60000,
    });
    await setDoc(doc(db, 'campaigns', ids.campaign, 'invites', 'EXPIRE33'), {
      ...baseInvite,
      id: 'EXPIRE33',
      code: 'EXPIRE33',
      recipientUid: '',
      expiresAt: 1,
    });
  });

  const joinerDb = env.authenticatedContext('user-joiner').firestore();
  const outsiderDb = env.authenticatedContext(ids.outsider).firestore();
  const targetDb = env.authenticatedContext('user-target').firestore();

  await assertFails(getDoc(doc(joinerDb, 'campaigns', ids.campaign, 'invites', 'TARGET22')));
  const wrongRecip = writeBatch(joinerDb);
  wrongRecip.update(doc(joinerDb, 'campaigns', ids.campaign, 'invites', 'TARGET22'), {
    state: 'CONSUMED',
    consumedBy: 'user-joiner',
    consumedAt: serverTimestamp(),
  });
  wrongRecip.set(doc(joinerDb, 'campaigns', ids.campaign, 'members', 'user-joiner'), {
    campaignId: ids.campaign,
    userId: 'user-joiner',
    role: 'PLAYER',
    state: 'ACTIVE',
    characterIds: [],
    joinedAt: 2,
    updatedAt: 2,
    joinedByInviteId: 'TARGET22',
  });
  await assertFails(wrongRecip.commit());

  const expiredBatch = writeBatch(joinerDb);
  expiredBatch.update(doc(joinerDb, 'campaigns', ids.campaign, 'invites', 'EXPIRE33'), {
    state: 'CONSUMED',
    consumedBy: 'user-joiner',
    consumedAt: serverTimestamp(),
  });
  expiredBatch.set(doc(joinerDb, 'campaigns', ids.campaign, 'members', 'user-joiner'), {
    campaignId: ids.campaign,
    userId: 'user-joiner',
    role: 'PLAYER',
    state: 'ACTIVE',
    characterIds: [],
    joinedAt: 2,
    updatedAt: 2,
    joinedByInviteId: 'EXPIRE33',
  });
  await assertFails(expiredBatch.commit());

  await assertFails(updateDoc(doc(joinerDb, 'campaigns', ids.campaign, 'invites', 'OPEN1111'), {
    state: 'CONSUMED',
    consumedBy: 'user-joiner',
    consumedAt: serverTimestamp(),
  }));

  const okBatch = writeBatch(joinerDb);
  okBatch.update(doc(joinerDb, 'campaigns', ids.campaign, 'invites', 'OPEN1111'), {
    state: 'CONSUMED',
    consumedBy: 'user-joiner',
    consumedAt: serverTimestamp(),
  });
  okBatch.set(doc(joinerDb, 'campaigns', ids.campaign, 'members', 'user-joiner'), {
    campaignId: ids.campaign,
    userId: 'user-joiner',
    role: 'PLAYER',
    state: 'ACTIVE',
    characterIds: [],
    joinedAt: 2,
    updatedAt: 2,
    joinedByInviteId: 'OPEN1111',
  });
  await assertSucceeds(okBatch.commit());

  const replayInvite = writeBatch(outsiderDb);
  replayInvite.update(doc(outsiderDb, 'campaigns', ids.campaign, 'invites', 'OPEN1111'), {
    state: 'CONSUMED',
    consumedBy: ids.outsider,
    consumedAt: serverTimestamp(),
  });
  replayInvite.set(doc(outsiderDb, 'campaigns', ids.campaign, 'members', ids.outsider), {
    campaignId: ids.campaign,
    userId: ids.outsider,
    role: 'PLAYER',
    state: 'ACTIVE',
    characterIds: [],
    joinedAt: 3,
    updatedAt: 3,
    joinedByInviteId: 'OPEN1111',
  });
  await assertFails(replayInvite.commit());

  const targetBatch = writeBatch(targetDb);
  targetBatch.update(doc(targetDb, 'campaigns', ids.campaign, 'invites', 'TARGET22'), {
    state: 'CONSUMED',
    consumedBy: 'user-target',
    consumedAt: serverTimestamp(),
  });
  targetBatch.set(doc(targetDb, 'campaigns', ids.campaign, 'members', 'user-target'), {
    campaignId: ids.campaign,
    userId: 'user-target',
    role: 'PLAYER',
    state: 'ACTIVE',
    characterIds: [],
    joinedAt: 4,
    updatedAt: 4,
    joinedByInviteId: 'TARGET22',
  });
  await assertSucceeds(targetBatch.commit());
});

test('presence enforces strict sheet ACL (owner + active GM only), rejects spoof/>120s; media enforces <=64KiB and <=256px', async () => {
  await seedBase();
  const ownerDb = env.authenticatedContext(ids.owner).firestore();
  const gmDb = env.authenticatedContext(ids.gm).firestore();
  const peerDb = env.authenticatedContext(ids.peer).firestore();
  const outsiderDb = env.authenticatedContext(ids.outsider).firestore();

  const validExpiry = Timestamp.fromMillis(Date.now() + 60000);
  const excessiveExpiry = Timestamp.fromMillis(Date.now() + 300000);

  await assertFails(setDoc(doc(peerDb, 'characters', ids.char, 'presence', `${ids.owner}_c1`), {
    uid: ids.owner,
    connectionId: 'c1',
    fieldKey: 'name',
    label: 'Nome',
    updatedAt: serverTimestamp(),
    expiresAt: validExpiry,
  }));
  await assertFails(setDoc(doc(ownerDb, 'characters', ids.char, 'presence', `${ids.owner}_c1`), {
    uid: ids.owner,
    connectionId: 'c1',
    fieldKey: 'name',
    label: 'Nome',
    updatedAt: serverTimestamp(),
    expiresAt: excessiveExpiry,
  }));
  await assertFails(setDoc(doc(outsiderDb, 'characters', ids.char, 'presence', `${ids.outsider}_c1`), {
    uid: ids.outsider,
    connectionId: 'c1',
    fieldKey: 'name',
    label: 'Nome',
    updatedAt: serverTimestamp(),
    expiresAt: validExpiry,
  }));

  // Valid presence write: GM can read, peer campaign member & outsider CANNOT read, owner can delete
  await assertSucceeds(setDoc(doc(ownerDb, 'characters', ids.char, 'presence', `${ids.owner}_c1`), {
    uid: ids.owner,
    connectionId: 'c1',
    fieldKey: 'name',
    label: 'Nome',
    updatedAt: serverTimestamp(),
    expiresAt: validExpiry,
  }));
  await assertSucceeds(getDoc(doc(gmDb, 'characters', ids.char, 'presence', `${ids.owner}_c1`)));
  await assertFails(getDoc(doc(peerDb, 'characters', ids.char, 'presence', `${ids.owner}_c1`)));
  await assertFails(getDoc(doc(outsiderDb, 'characters', ids.char, 'presence', `${ids.owner}_c1`)));
  await assertSucceeds(deleteDoc(doc(ownerDb, 'characters', ids.char, 'presence', `${ids.owner}_c1`)));

  // Media > 64 KiB fails, 65536 bytes succeeds, > 256px fails, non-avatar doc fails
  await assertFails(setDoc(doc(ownerDb, 'characters', ids.char, 'media', 'avatar'), {
    bytesBase64: 'A'.repeat(65537),
    mimeType: 'image/webp',
    width: 128,
    height: 128,
    updatedAt: serverTimestamp(),
    updatedBy: ids.owner,
  }));
  await assertFails(setDoc(doc(ownerDb, 'characters', ids.char, 'media', 'avatar'), {
    bytesBase64: 'A'.repeat(1024),
    mimeType: 'image/webp',
    width: 257,
    height: 128,
    updatedAt: serverTimestamp(),
    updatedBy: ids.owner,
  }));
  await assertFails(setDoc(doc(ownerDb, 'characters', ids.char, 'media', 'fullsize'), {
    bytesBase64: 'A'.repeat(1024),
    mimeType: 'image/webp',
    width: 128,
    height: 128,
    updatedAt: serverTimestamp(),
    updatedBy: ids.owner,
  }));
  await assertFails(setDoc(doc(peerDb, 'characters', ids.char, 'media', 'avatar'), {
    bytesBase64: 'A'.repeat(1024),
    mimeType: 'image/webp',
    width: 128,
    height: 128,
    updatedAt: serverTimestamp(),
    updatedBy: ids.peer,
  }));
  await assertSucceeds(setDoc(doc(ownerDb, 'characters', ids.char, 'media', 'avatar'), {
    bytesBase64: 'A'.repeat(65536),
    mimeType: 'image/webp',
    width: 256,
    height: 256,
    updatedAt: serverTimestamp(),
    updatedBy: ids.owner,
  }));
  await assertFails(getDoc(doc(peerDb, 'characters', ids.char, 'media', 'avatar')));
  await assertFails(getDoc(doc(outsiderDb, 'characters', ids.char, 'media', 'avatar')));
});

test('avatar rejects simultaneous encodings that exceed the single-thumbnail budget', async () => {
  await seedBase();
  const db = env.authenticatedContext(ids.owner).firestore();
  await assertFails(setDoc(doc(db, 'characters', ids.char, 'media', 'avatar'), {
    bytesBase64: 'A'.repeat(40000),
    dataUri: 'B'.repeat(40000),
    mimeType: 'image/png', width: 128, height: 128,
    updatedAt: serverTimestamp(), updatedBy: ids.owner,
  }));
});

test('HISTORIAN lock blocks player field edits while allowing GM edits and retired paths are denied', async () => {
  await seedBase();
  const gmDb = env.authenticatedContext(ids.gm).firestore();
  const ownerDb = env.authenticatedContext(ids.owner).firestore();

  const lockBatch = writeBatch(gmDb);
  lockBatch.update(doc(gmDb, 'characters', ids.char), {
    lockType: 'HISTORIAN',
    isLocked: true,
    lockedBy: ids.gm,
    lockedAt: serverTimestamp(),
    revision: 2,
    lastOperationId: 'op-hist-lock',
    updatedBy: ids.gm,
    updatedAt: serverTimestamp(),
  });
  lockBatch.set(
    doc(gmDb, 'characters', ids.char, 'operations', 'op-hist-lock'),
    makeOp('op-hist-lock', ids.gm, 'SET_LOCK', [
      { kind: 'SHEET', itemId: '', key: '', op: 'LOCK', expectedRevision: 1, nextRevision: 2 },
    ]),
  );
  await assertSucceeds(lockBatch.commit());

  const blockedField = writeBatch(ownerDb);
  blockedField.update(doc(ownerDb, 'characters', ids.char, 'fields', 'name'), {
    value: 'Blocked',
    revision: 2,
    lastOperationId: 'op-blocked-field',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  blockedField.set(
    doc(ownerDb, 'characters', ids.char, 'operations', 'op-blocked-field'),
    makeOp('op-blocked-field', ids.owner, 'PATCH_FIELDS', [
      { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: 'Blocked' },
    ]),
  );
  await assertFails(blockedField.commit());

  const gmField = writeBatch(gmDb);
  gmField.update(doc(gmDb, 'characters', ids.char, 'fields', 'name'), {
    value: 'GM Edited',
    revision: 2,
    lastOperationId: 'op-gm-field',
    updatedBy: ids.gm,
    updatedAt: serverTimestamp(),
  });
  gmField.set(
    doc(gmDb, 'characters', ids.char, 'operations', 'op-gm-field'),
    makeOp('op-gm-field', ids.gm, 'PATCH_FIELDS', [
      { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: 'GM Edited' },
    ]),
  );
  await assertSucceeds(gmField.commit());

  await assertFails(getDoc(doc(ownerDb, 'sheetRoutes', ids.char)));
  await assertFails(getDoc(doc(ownerDb, 'campaignMembers', `${ids.campaign}::${ids.owner}`)));
  await assertFails(getDoc(doc(ownerDb, 'campaignInvites', 'INV1')));
});

test('enforces Rules access budget: 5 ITEM_FIELD and 7 FIELD transactions succeed, while >5 ITEM_FIELD and >7 FIELD fail', async () => {
  await seedBase();
  const db = env.authenticatedContext(ids.owner).firestore();

  await env.withSecurityRulesDisabled(async (context) => {
    const admin = context.firestore();
    await setDoc(doc(admin, 'characters', ids.char, 'items', 'attr_FOR'), makeItem('attr_FOR', 'ATTRIBUTE'));
    for (const [k, v] of [
      ['name', 'Força'],
      ['acronym', 'FOR'],
      ['value', 2],
      ['modifier', 0],
      ['skill_Luta_value', 1],
      ['skill_Atletismo_value', 1],
    ]) {
      await setDoc(doc(admin, 'characters', ids.char, 'items', 'attr_FOR', 'fields', k), makeField(k, v));
    }
    for (const [k, v] of [
      ['race', 'Humano'],
      ['occupation', 'Soldado'],
      ['height', '1.80'],
      ['age', '25'],
      ['sex', 'M'],
      ['size', 'Médio'],
      ['pathName', 'Aço'],
      ['pathMotto', 'Firme'],
    ]) {
      await setDoc(doc(admin, 'characters', ids.char, 'fields', k), makeField(k, v));
    }
  });

  // 5 ITEM_FIELD targets + 1 receipt (9 + 5*2 = 17 <= 20 accesses without cache) succeeds
  const itemBatch5 = writeBatch(db);
  const itemUpdates5 = [
    ['value', 3],
    ['modifier', 1],
    ['skill_Luta_value', 2],
    ['skill_Atletismo_value', 2],
    ['name', 'Força Bruta'],
  ];
  for (const [k, v] of itemUpdates5) {
    itemBatch5.update(doc(db, 'characters', ids.char, 'items', 'attr_FOR', 'fields', k), {
      value: v,
      revision: 2,
      lastOperationId: 'op-budget-if-5',
      updatedBy: ids.owner,
      updatedAt: serverTimestamp(),
    });
  }
  itemBatch5.set(
    doc(db, 'characters', ids.char, 'operations', 'op-budget-if-5'),
    makeOp(
      'op-budget-if-5',
      ids.owner,
      'PATCH_FIELDS',
      itemUpdates5.map(([k, v]) => ({
        kind: 'ITEM_FIELD', itemId: 'attr_FOR', key: k, op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: v,
      })),
    ),
  );
  await assertSucceeds(itemBatch5.commit());

  // 6 ITEM_FIELD targets fails budget cap
  const itemBatch6 = writeBatch(db);
  const itemUpdates6 = [
    ['value', 4, 3],
    ['modifier', 2, 3],
    ['skill_Luta_value', 3, 3],
    ['skill_Atletismo_value', 3, 3],
    ['name', 'Força Máxima', 3],
    ['acronym', 'STR', 2],
  ];
  for (const [k, v, rev] of itemUpdates6) {
    itemBatch6.update(doc(db, 'characters', ids.char, 'items', 'attr_FOR', 'fields', k), {
      value: v,
      revision: rev,
      lastOperationId: 'op-budget-if-6',
      updatedBy: ids.owner,
      updatedAt: serverTimestamp(),
    });
  }
  itemBatch6.set(
    doc(db, 'characters', ids.char, 'operations', 'op-budget-if-6'),
    makeOp(
      'op-budget-if-6',
      ids.owner,
      'PATCH_FIELDS',
      itemUpdates6.map(([k, v, rev]) => ({
        kind: 'ITEM_FIELD', itemId: 'attr_FOR', key: k, op: 'SET', expectedRevision: rev - 1, nextRevision: rev, desiredValue: v,
      })),
    ),
  );
  await assertFails(itemBatch6.commit());

  // 7 FIELD targets + 1 receipt (9 + 7*1 = 16 <= 20 accesses without cache) succeeds
  const fieldBatch7 = writeBatch(db);
  const fieldUpdates7 = [
    ['race', 'Elfo'],
    ['occupation', 'Guardião'],
    ['height', '1.85'],
    ['age', '30'],
    ['sex', 'F'],
    ['size', 'Médio'],
    ['pathName', 'Ferro'],
  ];
  for (const [k, v] of fieldUpdates7) {
    fieldBatch7.update(doc(db, 'characters', ids.char, 'fields', k), {
      value: v,
      revision: 2,
      lastOperationId: 'op-budget-f-7',
      updatedBy: ids.owner,
      updatedAt: serverTimestamp(),
    });
  }
  fieldBatch7.set(
    doc(db, 'characters', ids.char, 'operations', 'op-budget-f-7'),
    makeOp(
      'op-budget-f-7',
      ids.owner,
      'PATCH_FIELDS',
      fieldUpdates7.map(([k, v]) => ({
        kind: 'FIELD', itemId: '', key: k, op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: v,
      })),
    ),
  );
  await assertSucceeds(fieldBatch7.commit());

  // 8 FIELD targets rejected by targets.size() <= 7 budget cap
  const fieldBatch8 = writeBatch(db);
  const fieldUpdates8 = [
    ['race', 'Anão', 3],
    ['occupation', 'Ferreiro', 3],
    ['height', '1.50', 3],
    ['age', '40', 3],
    ['sex', 'M', 3],
    ['size', 'Pequeno', 3],
    ['pathName', 'Pedra', 3],
    ['pathMotto', 'Rocha', 2],
  ];
  for (const [k, v, rev] of fieldUpdates8) {
    fieldBatch8.update(doc(db, 'characters', ids.char, 'fields', k), {
      value: v,
      revision: rev,
      lastOperationId: 'op-budget-f-8',
      updatedBy: ids.owner,
      updatedAt: serverTimestamp(),
    });
  }
  fieldBatch8.set(
    doc(db, 'characters', ids.char, 'operations', 'op-budget-f-8'),
    makeOp(
      'op-budget-f-8',
      ids.owner,
      'PATCH_FIELDS',
      fieldUpdates8.map(([k, v, rev]) => ({
        kind: 'FIELD', itemId: '', key: k, op: 'SET', expectedRevision: rev - 1, nextRevision: rev, desiredValue: v,
      })),
    ),
  );
  await assertFails(fieldBatch8.commit());
});

test('representative 10-user load: 1 GM + 9 players join, create sheets, transact concurrently, heartbeat presence, and upload avatars', async () => {
  const gmUid = 'load-gm';
  const campaignId = 'load-camp';
  const playerUids = Array.from({ length: 9 }, (_, i) => `load-p${i + 1}`);

  const gmDb = env.authenticatedContext(gmUid).firestore();
  await assertSucceeds(setDoc(doc(gmDb, 'campaigns', campaignId), {
    id: campaignId,
    name: 'Mesa 10 Usuários',
    description: 'Carga representativa',
    ownerId: gmUid,
    state: 'ACTIVE',
    createdAt: 1,
    updatedAt: 1,
  }));

  await Promise.all(playerUids.map((uid, idx) =>
    assertSucceeds(setDoc(doc(gmDb, 'campaigns', campaignId, 'invites', `LOADINV${idx + 1}`), {
      id: `LOADINV${idx + 1}`,
      campaignId,
      code: `LOADINV${idx + 1}`,
      campaignName: 'Mesa 10 Usuários',
      campaignDescription: 'Carga representativa',
      createdBy: gmUid,
      recipientUid: uid,
      expiresAt: Date.now() + 120000,
      state: 'PENDING',
      consumedBy: '',
      consumedAt: null,
      revokedAt: null,
      createdAt: 1,
    })),
  ));

  await Promise.all(playerUids.map(async (uid, idx) => {
    const pDb = env.authenticatedContext(uid).firestore();
    const inviteCode = `LOADINV${idx + 1}`;
    const snap = await assertSucceeds(getDocs(query(collectionGroup(pDb, 'invites'), where('code', '==', inviteCode))));
    assert.equal(snap.size, 1);

    const joinBatch = writeBatch(pDb);
    joinBatch.update(doc(pDb, 'campaigns', campaignId, 'invites', inviteCode), {
      state: 'CONSUMED',
      consumedBy: uid,
      consumedAt: serverTimestamp(),
    });
    joinBatch.set(doc(pDb, 'campaigns', campaignId, 'members', uid), {
      campaignId,
      userId: uid,
      role: 'PLAYER',
      state: 'ACTIVE',
      characterIds: [`sheet-${idx + 1}`],
      joinedAt: 2,
      updatedAt: 2,
      joinedByInviteId: inviteCode,
    });
    await assertSucceeds(joinBatch.commit());
  }));

  await Promise.all(playerUids.map(async (uid, idx) => {
    const pDb = env.authenticatedContext(uid).firestore();
    const charId = `sheet-${idx + 1}`;
    const createBatch = writeBatch(pDb);
    createBatch.set(doc(pDb, 'characters', charId), makeRoot({
      id: charId,
      ownerId: uid,
      campaignId,
      lastOperationId: `op-create-${idx + 1}`,
      updatedBy: uid,
      updatedAt: serverTimestamp(),
    }));
    createBatch.set(doc(pDb, 'characters', charId, 'fields', 'name'), {
      key: 'name',
      value: `Personagem ${idx + 1}`,
      revision: 1,
      lastOperationId: `op-create-${idx + 1}`,
      updatedBy: uid,
      updatedAt: serverTimestamp(),
    });
    createBatch.set(doc(pDb, 'characters', charId, 'fields', 'life'), {
      key: 'life',
      value: { current: 20, maximum: 20, adjustment: 0 },
      revision: 1,
      lastOperationId: `op-create-${idx + 1}`,
      updatedBy: uid,
      updatedAt: serverTimestamp(),
    });
    createBatch.set(
      doc(pDb, 'characters', charId, 'operations', `op-create-${idx + 1}`),
      makeOp(`op-create-${idx + 1}`, uid, 'CREATE_SHEET', [
        { kind: 'SHEET', itemId: '', key: '', op: 'CREATE', expectedRevision: 0, nextRevision: 1 },
        { kind: 'FIELD', itemId: '', key: 'name', op: 'CREATE', expectedRevision: 0, nextRevision: 1, desiredValue: `Personagem ${idx + 1}` },
        { kind: 'FIELD', itemId: '', key: 'life', op: 'CREATE', expectedRevision: 0, nextRevision: 1, desiredValue: { current: 20, maximum: 20, adjustment: 0 } },
      ]),
    );
    await assertSucceeds(createBatch.commit());
  }));

  await Promise.all([
    ...playerUids.map(async (uid, idx) => {
      const pDb = env.authenticatedContext(uid).firestore();
      const charId = `sheet-${idx + 1}`;
      await assertSucceeds(setDoc(doc(pDb, 'characters', charId, 'presence', `${uid}_dev1`), {
        uid,
        connectionId: 'dev1',
        fieldKey: 'life',
        label: 'Vida',
        updatedAt: serverTimestamp(),
        expiresAt: Timestamp.fromMillis(Date.now() + 60000),
      }));
      await assertSucceeds(setDoc(doc(pDb, 'characters', charId, 'media', 'avatar'), {
        bytesBase64: 'B'.repeat(256),
        mimeType: 'image/webp',
        width: 128,
        height: 128,
        updatedAt: serverTimestamp(),
        updatedBy: uid,
      }));
      await assertSucceeds(runTransaction(pDb, async (tx) => {
        const lifeRef = doc(pDb, 'characters', charId, 'fields', 'life');
        const snap = await tx.get(lifeRef);
        const cur = snap.data().value;
        const rev = snap.data().revision;
        tx.update(lifeRef, {
          value: { ...cur, current: cur.current - 2 },
          revision: rev + 1,
          lastOperationId: `op-dmg-${idx + 1}`,
          updatedBy: uid,
          updatedAt: serverTimestamp(),
        });
        tx.set(
          doc(pDb, 'characters', charId, 'operations', `op-dmg-${idx + 1}`),
          makeOp(`op-dmg-${idx + 1}`, uid, 'ADJUST_RESOURCE', [
            { kind: 'FIELD', itemId: '', key: 'life', op: 'DELTA', expectedRevision: rev, nextRevision: rev + 1, delta: -2 },
          ]),
        );
      }));
    }),
    (async () => {
      await assertSucceeds(runTransaction(gmDb, async (tx) => {
        const nameRef = doc(gmDb, 'characters', 'sheet-1', 'fields', 'name');
        const snap = await tx.get(nameRef);
        const rev = snap.data().revision;
        tx.update(nameRef, {
          value: 'Personagem 1 (Mestre)',
          revision: rev + 1,
          lastOperationId: 'op-gm-edit-1',
          updatedBy: gmUid,
          updatedAt: serverTimestamp(),
        });
        tx.set(
          doc(gmDb, 'characters', 'sheet-1', 'operations', 'op-gm-edit-1'),
          makeOp('op-gm-edit-1', gmUid, 'PATCH_FIELDS', [
            { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: rev, nextRevision: rev + 1, desiredValue: 'Personagem 1 (Mestre)' },
          ]),
        );
      }));
    })(),
  ]);

  const roster = await assertSucceeds(getDocs(query(collection(gmDb, 'characters'), where('campaignId', '==', campaignId))));
  assert.equal(roster.size, 9);
  const sheet1Life = await assertSucceeds(getDoc(doc(gmDb, 'characters', 'sheet-1', 'fields', 'life')));
  assert.equal(sheet1Life.data().value.current, 18);
  const sheet1Name = await assertSucceeds(getDoc(doc(gmDb, 'characters', 'sheet-1', 'fields', 'name')));
  assert.equal(sheet1Name.data().value, 'Personagem 1 (Mestre)');

  const p2Db = env.authenticatedContext('load-p2').firestore();
  const crossEdit = writeBatch(p2Db);
  crossEdit.update(doc(p2Db, 'characters', 'sheet-1', 'fields', 'life'), {
    value: { current: 0, maximum: 20, adjustment: 0 },
    revision: 3,
    lastOperationId: 'op-cross-hack',
    updatedBy: 'load-p2',
    updatedAt: serverTimestamp(),
  });
  crossEdit.set(
    doc(p2Db, 'characters', 'sheet-1', 'operations', 'op-cross-hack'),
    makeOp('op-cross-hack', 'load-p2', 'ADJUST_RESOURCE', [
      { kind: 'FIELD', itemId: '', key: 'life', op: 'DELTA', expectedRevision: 2, nextRevision: 3, delta: -18 },
    ]),
  );
  await assertFails(crossEdit.commit());
});

test('denies all client MIGRATE_V2 and blocks ACCEPT_DELIVERY field/state bypasses for owner and linked GM', async () => {
  const tryMigrateAndDeliveryFieldBypass = async (actorDb, actorUid, label, extraSetup = async () => {}) => {
    await env.clearFirestore();
    await seedBase();
    await env.withSecurityRulesDisabled(async (context) => {
      const admin = context.firestore();
      await setDoc(doc(admin, 'campaignDeliveries', 'deliv-bypass'), {
        id: 'deliv-bypass',
        campaignId: ids.campaign,
        libraryEntryId: 'lib-1',
        recipientId: ids.owner,
        recipientCharacterId: ids.char,
        snapshotKind: 'ITEM',
        snapshotName: 'Poção de Entrega',
        snapshotSummary: 'Entrega',
        snapshotPayload: '{}',
        knowledgeMapping: '',
        knowledgeBonus: 0,
        state: 'PENDING',
        createdBy: ids.gm,
        createdAt: 1,
        updatedAt: 1,
      });
      await extraSetup(admin);
    });

    // 1. Client MIGRATE_V2 field edit fails
    const migField = writeBatch(actorDb);
    migField.update(doc(actorDb, 'characters', ids.char, 'fields', 'name'), {
      value: `Hacked-${label}`,
      revision: 2,
      lastOperationId: `op-mig-f-${label}`,
      updatedBy: actorUid,
      updatedAt: serverTimestamp(),
    });
    migField.set(
      doc(actorDb, 'characters', ids.char, 'operations', `op-mig-f-${label}`),
      makeOp(`op-mig-f-${label}`, actorUid, 'MIGRATE_V2', [
        { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: `Hacked-${label}` },
      ]),
    );
    await assertFails(migField.commit());

    // 2. Client MIGRATE_V2 root create fails
    const migCreate = writeBatch(actorDb);
    migCreate.set(doc(actorDb, 'characters', `mig-${label}`), makeRoot({
      id: `mig-${label}`,
      ownerId: actorUid,
      lastOperationId: `op-mig-c-${label}`,
      updatedBy: actorUid,
      updatedAt: serverTimestamp(),
    }));
    migCreate.set(
      doc(actorDb, 'characters', `mig-${label}`, 'operations', `op-mig-c-${label}`),
      makeOp(`op-mig-c-${label}`, actorUid, 'MIGRATE_V2', [
        { kind: 'SHEET', itemId: '', key: '', op: 'CREATE', expectedRevision: 0, nextRevision: 1 },
      ]),
    );
    await assertFails(migCreate.commit());

    // 3. ACCEPT_DELIVERY receipt trying to edit fields/name or fields/life fails even with coupled delivery acceptance
    for (const [fieldKey, nextVal] of [
      ['name', `Bypass-${label}`],
      ['life', { current: 99, maximum: 99, adjustment: 0 }],
    ]) {
      const delivField = writeBatch(actorDb);
      delivField.update(doc(actorDb, 'characters', ids.char), {
        appliedDeliveryIds: ['deliv-bypass'],
        revision: 2,
        lastOperationId: `op-deliv-f-${fieldKey}-${label}`,
        updatedBy: actorUid,
        updatedAt: serverTimestamp(),
      });
      delivField.update(doc(actorDb, 'characters', ids.char, 'fields', fieldKey), {
        value: nextVal,
        revision: 2,
        lastOperationId: `op-deliv-f-${fieldKey}-${label}`,
        updatedBy: actorUid,
        updatedAt: serverTimestamp(),
      });
      delivField.update(doc(actorDb, 'campaignDeliveries', 'deliv-bypass'), {
        state: 'ACCEPTED',
        updatedAt: 2,
      });
      delivField.set(
        doc(actorDb, 'characters', ids.char, 'operations', `op-deliv-f-${fieldKey}-${label}`),
        makeOp(
          `op-deliv-f-${fieldKey}-${label}`,
          actorUid,
          'ACCEPT_DELIVERY',
          [
            { kind: 'SHEET', itemId: '', key: '', op: 'DELIVERY', expectedRevision: 1, nextRevision: 2 },
            { kind: 'FIELD', itemId: '', key: fieldKey, op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: nextVal },
          ],
          { payloadHash: 'sha256-bypass', deliveryId: 'deliv-bypass' },
        ),
      );
      await assertFails(delivField.commit());
    }

    // 4. Legitimate ACCEPT_DELIVERY item scoped write fails when HISTORIAN-locked / deleted / ARCHIVED (and always for GM)
    if (label !== 'owner-active') {
      const delivItem = writeBatch(actorDb);
      delivItem.update(doc(actorDb, 'characters', ids.char), {
        appliedDeliveryIds: ['deliv-bypass'],
        revision: 2,
        lastOperationId: `op-deliv-state-${label}`,
        updatedBy: actorUid,
        updatedAt: serverTimestamp(),
      });
      delivItem.update(doc(actorDb, 'campaignDeliveries', 'deliv-bypass'), {
        state: 'ACCEPTED',
        updatedAt: 2,
      });
      delivItem.set(
        doc(actorDb, 'characters', ids.char, 'operations', `op-deliv-state-${label}`),
        makeOp(
          `op-deliv-state-${label}`,
          actorUid,
          'ACCEPT_DELIVERY',
          [{ kind: 'SHEET', itemId: '', key: '', op: 'DELIVERY', expectedRevision: 1, nextRevision: 2 }],
          { payloadHash: 'sha256-deliv', deliveryId: 'deliv-bypass' },
        ),
      );
      await assertFails(delivItem.commit());
    }
  };

  const ownerDb = env.authenticatedContext(ids.owner).firestore();
  const gmDb = env.authenticatedContext(ids.gm).firestore();

  await tryMigrateAndDeliveryFieldBypass(ownerDb, ids.owner, 'owner-active');
  await tryMigrateAndDeliveryFieldBypass(gmDb, ids.gm, 'gm-active');

  await tryMigrateAndDeliveryFieldBypass(ownerDb, ids.owner, 'owner-historian', async (admin) => {
    await updateDoc(doc(admin, 'characters', ids.char), {
      lockType: 'HISTORIAN',
      isLocked: true,
      lockedBy: ids.gm,
      lockedAt: Timestamp.fromMillis(1000),
    });
  });
  await tryMigrateAndDeliveryFieldBypass(gmDb, ids.gm, 'gm-historian', async (admin) => {
    await updateDoc(doc(admin, 'characters', ids.char), {
      lockType: 'HISTORIAN',
      isLocked: true,
      lockedBy: ids.gm,
      lockedAt: Timestamp.fromMillis(1000),
    });
  });

  await tryMigrateAndDeliveryFieldBypass(ownerDb, ids.owner, 'owner-deleted', async (admin) => {
    await updateDoc(doc(admin, 'characters', ids.char), { deleted: true });
  });
  await tryMigrateAndDeliveryFieldBypass(gmDb, ids.gm, 'gm-deleted', async (admin) => {
    await updateDoc(doc(admin, 'characters', ids.char), { deleted: true });
  });

  await tryMigrateAndDeliveryFieldBypass(ownerDb, ids.owner, 'owner-archived', async (admin) => {
    await updateDoc(doc(admin, 'campaigns', ids.campaign), { state: 'ARCHIVED', archivedAt: 2 });
  });
  await tryMigrateAndDeliveryFieldBypass(gmDb, ids.gm, 'gm-archived', async (admin) => {
    await updateDoc(doc(admin, 'campaigns', ids.campaign), { state: 'ARCHIVED', archivedAt: 2 });
  });
});

test('ACCEPT_DELIVERY requires matching PENDING->ACCEPTED delivery, recipient character/uid, and delivery-scoped targets', async () => {
  await seedBase();
  await env.withSecurityRulesDisabled(async (context) => {
    const admin = context.firestore();
    await setDoc(doc(admin, 'characters', 'char-other'), makeRoot({
      id: 'char-other',
      ownerId: ids.owner,
      campaignId: ids.campaign,
    }));
    await setDoc(doc(admin, 'characters', ids.char, 'fields', 'learnedKnowledges'), makeField('learnedKnowledges', [
      { id: 'know-1', name: 'Alquimia', adjustment: 0 },
    ]));
    await setDoc(doc(admin, 'campaignDeliveries', 'deliv-wrong-char'), {
      id: 'deliv-wrong-char',
      campaignId: ids.campaign,
      libraryEntryId: 'lib-1',
      recipientId: ids.owner,
      recipientCharacterId: 'char-other',
      snapshotKind: 'ITEM',
      snapshotName: 'Elixir',
      snapshotSummary: 'Cura',
      snapshotPayload: '{}',
      knowledgeMapping: '',
      knowledgeBonus: 0,
      state: 'PENDING',
      createdBy: ids.gm,
      createdAt: 1,
      updatedAt: 1,
    });
    await setDoc(doc(admin, 'campaignDeliveries', 'deliv-wrong-user'), {
      id: 'deliv-wrong-user',
      campaignId: ids.campaign,
      libraryEntryId: 'lib-1',
      recipientId: ids.peer,
      recipientCharacterId: ids.char,
      snapshotKind: 'ITEM',
      snapshotName: 'Elixir',
      snapshotSummary: 'Cura',
      snapshotPayload: '{}',
      knowledgeMapping: '',
      knowledgeBonus: 0,
      state: 'PENDING',
      createdBy: ids.gm,
      createdAt: 1,
      updatedAt: 1,
    });
    await setDoc(doc(admin, 'campaignDeliveries', 'deliv-valid'), {
      id: 'deliv-valid',
      campaignId: ids.campaign,
      libraryEntryId: 'lib-2',
      recipientId: ids.owner,
      recipientCharacterId: ids.char,
      snapshotKind: 'ITEM',
      snapshotName: 'Elixir Solar',
      snapshotSummary: 'Cura 5',
      snapshotPayload: '{}',
      knowledgeMapping: 'know-1',
      knowledgeBonus: 1,
      state: 'PENDING',
      createdBy: ids.gm,
      createdAt: 1,
      updatedAt: 1,
    });
  });

  const ownerDb = env.authenticatedContext(ids.owner).firestore();

  // 1. ACCEPT_DELIVERY without updating campaignDeliveries/{id} to ACCEPTED fails
  const uncoupledDeliv = writeBatch(ownerDb);
  uncoupledDeliv.update(doc(ownerDb, 'characters', ids.char), {
    appliedDeliveryIds: ['deliv-valid'],
    revision: 2,
    lastOperationId: 'op-deliv-uncoupled',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  uncoupledDeliv.set(
    doc(ownerDb, 'characters', ids.char, 'operations', 'op-deliv-uncoupled'),
    makeOp(
      'op-deliv-uncoupled',
      ids.owner,
      'ACCEPT_DELIVERY',
      [{ kind: 'SHEET', itemId: '', key: '', op: 'DELIVERY', expectedRevision: 1, nextRevision: 2 }],
      { payloadHash: 'sha256-uncoupled', deliveryId: 'deliv-valid' },
    ),
  );
  await assertFails(uncoupledDeliv.commit());

  // 2. ACCEPT_DELIVERY for wrong recipientCharacterId fails
  const wrongCharDeliv = writeBatch(ownerDb);
  wrongCharDeliv.update(doc(ownerDb, 'characters', ids.char), {
    appliedDeliveryIds: ['deliv-wrong-char'],
    revision: 2,
    lastOperationId: 'op-deliv-wrong-char',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  wrongCharDeliv.update(doc(ownerDb, 'campaignDeliveries', 'deliv-wrong-char'), {
    state: 'ACCEPTED',
    updatedAt: 2,
  });
  wrongCharDeliv.set(
    doc(ownerDb, 'characters', ids.char, 'operations', 'op-deliv-wrong-char'),
    makeOp(
      'op-deliv-wrong-char',
      ids.owner,
      'ACCEPT_DELIVERY',
      [{ kind: 'SHEET', itemId: '', key: '', op: 'DELIVERY', expectedRevision: 1, nextRevision: 2 }],
      { payloadHash: 'sha256-wrong-char', deliveryId: 'deliv-wrong-char' },
    ),
  );
  await assertFails(wrongCharDeliv.commit());

  // 3. ACCEPT_DELIVERY for wrong recipientId fails
  const wrongUserDeliv = writeBatch(ownerDb);
  wrongUserDeliv.update(doc(ownerDb, 'characters', ids.char), {
    appliedDeliveryIds: ['deliv-wrong-user'],
    revision: 2,
    lastOperationId: 'op-deliv-wrong-user',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  wrongUserDeliv.update(doc(ownerDb, 'campaignDeliveries', 'deliv-wrong-user'), {
    state: 'ACCEPTED',
    updatedAt: 2,
  });
  wrongUserDeliv.set(
    doc(ownerDb, 'characters', ids.char, 'operations', 'op-deliv-wrong-user'),
    makeOp(
      'op-deliv-wrong-user',
      ids.owner,
      'ACCEPT_DELIVERY',
      [{ kind: 'SHEET', itemId: '', key: '', op: 'DELIVERY', expectedRevision: 1, nextRevision: 2 }],
      { payloadHash: 'sha256-wrong-user', deliveryId: 'deliv-wrong-user' },
    ),
  );
  await assertFails(wrongUserDeliv.commit());

  // 4. ACCEPT_DELIVERY with non-delivery-scoped itemId fails
  const arbitraryItemDeliv = writeBatch(ownerDb);
  arbitraryItemDeliv.update(doc(ownerDb, 'characters', ids.char), {
    appliedDeliveryIds: ['deliv-valid'],
    revision: 2,
    lastOperationId: 'op-deliv-bad-item',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  arbitraryItemDeliv.set(doc(ownerDb, 'characters', ids.char, 'items', 'inv_arbitrary'), {
    ...makeItem('inv_arbitrary', 'INVENTORY', { lastOperationId: 'op-deliv-bad-item', updatedAt: serverTimestamp() }),
  });
  arbitraryItemDeliv.update(doc(ownerDb, 'campaignDeliveries', 'deliv-valid'), {
    state: 'ACCEPTED',
    updatedAt: 2,
  });
  arbitraryItemDeliv.set(
    doc(ownerDb, 'characters', ids.char, 'operations', 'op-deliv-bad-item'),
    makeOp(
      'op-deliv-bad-item',
      ids.owner,
      'ACCEPT_DELIVERY',
      [
        { kind: 'SHEET', itemId: '', key: '', op: 'DELIVERY', expectedRevision: 1, nextRevision: 2 },
        { kind: 'ITEM', itemId: 'inv_arbitrary', key: '', op: 'CREATE', expectedRevision: 0, nextRevision: 1, desiredValue: { deleted: false } },
      ],
      { payloadHash: 'sha256-bad-item', deliveryId: 'deliv-valid' },
    ),
  );
  await assertFails(arbitraryItemDeliv.commit());

  // 5. Valid atomic ACCEPT_DELIVERY with delivery-scoped item + fields + knowledge bonus succeeds (<= 20 tx accesses)
  const delivItemId = 'inv_delivery:deliv-valid';
  const nextKnowledges = [{ id: 'know-1', name: 'Alquimia', adjustment: 1 }];
  const validDeliv = writeBatch(ownerDb);
  validDeliv.update(doc(ownerDb, 'characters', ids.char), {
    appliedDeliveryIds: ['deliv-valid'],
    revision: 2,
    lastOperationId: 'op-deliv-ok',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  validDeliv.set(
    doc(ownerDb, 'characters', ids.char, 'items', delivItemId),
    makeItem(delivItemId, 'INVENTORY', { lastOperationId: 'op-deliv-ok', updatedAt: serverTimestamp() }),
  );
  validDeliv.set(
    doc(ownerDb, 'characters', ids.char, 'items', delivItemId, 'fields', 'name'),
    makeField('name', 'Elixir Solar', { lastOperationId: 'op-deliv-ok', updatedAt: serverTimestamp() }),
  );
  validDeliv.update(doc(ownerDb, 'characters', ids.char, 'fields', 'learnedKnowledges'), {
    value: nextKnowledges,
    revision: 2,
    lastOperationId: 'op-deliv-ok',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  validDeliv.update(doc(ownerDb, 'campaignDeliveries', 'deliv-valid'), {
    state: 'ACCEPTED',
    updatedAt: 2,
  });
  validDeliv.set(
    doc(ownerDb, 'characters', ids.char, 'operations', 'op-deliv-ok'),
    makeOp(
      'op-deliv-ok',
      ids.owner,
      'ACCEPT_DELIVERY',
      [
        { kind: 'SHEET', itemId: '', key: '', op: 'DELIVERY', expectedRevision: 1, nextRevision: 2 },
        { kind: 'ITEM', itemId: delivItemId, key: '', op: 'CREATE', expectedRevision: 0, nextRevision: 1, desiredValue: { id: delivItemId } },
        { kind: 'ITEM_FIELD', itemId: delivItemId, key: 'name', op: 'CREATE', expectedRevision: 0, nextRevision: 1, desiredValue: 'Elixir Solar' },
        { kind: 'FIELD', itemId: '', key: 'learnedKnowledges', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: nextKnowledges },
      ],
      { payloadHash: 'sha256-deliv-ok', deliveryId: 'deliv-valid' },
    ),
  );
  await assertSucceeds(validDeliv.commit());
});

test('ITEM_CONSUME enforces mechanicalEffects and istate decrease; SESSION_ACTION enforces ABILITY_USE costValue and player formats', async () => {
  await seedBase();
  await env.withSecurityRulesDisabled(async (context) => {
    const admin = context.firestore();
    await setDoc(doc(admin, 'characters', ids.char, 'items', 'inv_potion', 'fields', 'mechanicalEffects'), makeField('mechanicalEffects', [
      { id: 'heal-life', kind: 'HEAL', amount: 3 },
    ]));
    await setDoc(doc(admin, 'characters', ids.char, 'items', 'inv_water'), makeItem('inv_water', 'INVENTORY'));
    await setDoc(doc(admin, 'characters', ids.char, 'items', 'inv_water', 'fields', 'quantity'), makeField('quantity', 2));
    await setDoc(doc(admin, 'characters', ids.char, 'items', 'istate_elixir'), makeItem('istate_elixir', 'ITEM_STATE'));
    await setDoc(doc(admin, 'characters', ids.char, 'items', 'istate_elixir', 'fields', 'runtime'), makeField('runtime', { doses: 2 }));
    await setDoc(doc(admin, 'characters', ids.char, 'fields', 'energy'), makeField('energy', {
      current: 10,
      maximum: 10,
      adjustment: 0,
    }));
    await setDoc(doc(admin, 'characters', ids.char, 'items', 'abil_slash'), makeItem('abil_slash', 'ABILITY'));
    await setDoc(doc(admin, 'characters', ids.char, 'items', 'abil_slash', 'fields', 'costType'), makeField('costType', 'ENERGY'));
    await setDoc(doc(admin, 'characters', ids.char, 'items', 'abil_slash', 'fields', 'costValue'), makeField('costValue', 3));
    await setDoc(doc(admin, 'characters', ids.char, 'items', 'abil_slash', 'fields', 'active'), makeField('active', false));
  });

  const ownerDb = env.authenticatedContext(ids.owner).firestore();
  const gmDb = env.authenticatedContext(ids.gm).firestore();

  // 1. ITEM_CONSUME on item with mechanicalEffects omitting secondary target MUST FAIL for player
  const omittedEffect = writeBatch(ownerDb);
  omittedEffect.update(doc(ownerDb, 'characters', ids.char, 'items', 'inv_potion', 'fields', 'quantity'), {
    value: 1,
    revision: 2,
    lastOperationId: 'op-omit-mech',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  omittedEffect.set(
    doc(ownerDb, 'characters', ids.char, 'operations', 'op-omit-mech'),
    makeOp('op-omit-mech', ids.owner, 'ITEM_CONSUME', [
      { kind: 'ITEM_FIELD', itemId: 'inv_potion', key: 'quantity', op: 'DELTA', expectedRevision: 1, nextRevision: 2, delta: -1 },
    ]),
  );
  await assertFails(omittedEffect.commit());

  // 2. Unsupported mechanical formulas require explicit GM adjustment, even with declared effects
  const withEffect = writeBatch(ownerDb);
  withEffect.update(doc(ownerDb, 'characters', ids.char, 'items', 'inv_potion', 'fields', 'quantity'), {
    value: 1,
    revision: 2,
    lastOperationId: 'op-with-mech',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  withEffect.update(doc(ownerDb, 'characters', ids.char, 'fields', 'life'), {
    value: { current: 15, maximum: 15, adjustment: 0 },
    revision: 2,
    lastOperationId: 'op-with-mech',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  withEffect.set(
    doc(ownerDb, 'characters', ids.char, 'operations', 'op-with-mech'),
    makeOp('op-with-mech', ids.owner, 'ITEM_CONSUME', [
      { kind: 'ITEM_FIELD', itemId: 'inv_potion', key: 'quantity', op: 'DELTA', expectedRevision: 1, nextRevision: 2, delta: -1 },
      { kind: 'FIELD', itemId: '', key: 'life', op: 'DELTA', expectedRevision: 1, nextRevision: 2, delta: 3 },
    ]),
  );
  await assertFails(withEffect.commit());

  // 3. ITEM_CONSUME on item WITHOUT mechanicalEffects succeeds with single negative delta
  const plainWater = writeBatch(ownerDb);
  plainWater.update(doc(ownerDb, 'characters', ids.char, 'items', 'inv_water', 'fields', 'quantity'), {
    value: 1,
    revision: 2,
    lastOperationId: 'op-water',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  plainWater.set(
    doc(ownerDb, 'characters', ids.char, 'operations', 'op-water'),
    makeOp('op-water', ids.owner, 'ITEM_CONSUME', [
      { kind: 'ITEM_FIELD', itemId: 'inv_water', key: 'quantity', op: 'DELTA', expectedRevision: 1, nextRevision: 2, delta: -1 },
    ]),
  );
  await assertSucceeds(plainWater.commit());

  // 4. ITEM_CONSUME on istate_* runtime must strictly decrease doses
  const nonDecreasingIstate = writeBatch(ownerDb);
  nonDecreasingIstate.update(doc(ownerDb, 'characters', ids.char, 'items', 'istate_elixir', 'fields', 'runtime'), {
    value: { doses: 2 },
    revision: 2,
    lastOperationId: 'op-istate-same',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  nonDecreasingIstate.set(
    doc(ownerDb, 'characters', ids.char, 'operations', 'op-istate-same'),
    makeOp('op-istate-same', ids.owner, 'ITEM_CONSUME', [
      { kind: 'ITEM_FIELD', itemId: 'istate_elixir', key: 'runtime', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: { doses: 2 } },
    ]),
  );
  await assertFails(nonDecreasingIstate.commit());

  const decreasingIstate = writeBatch(ownerDb);
  decreasingIstate.update(doc(ownerDb, 'characters', ids.char, 'items', 'istate_elixir', 'fields', 'runtime'), {
    value: { doses: 1 },
    revision: 2,
    lastOperationId: 'op-istate-dec',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  decreasingIstate.set(
    doc(ownerDb, 'characters', ids.char, 'operations', 'op-istate-dec'),
    makeOp('op-istate-dec', ids.owner, 'ITEM_CONSUME', [
      { kind: 'ITEM_FIELD', itemId: 'istate_elixir', key: 'runtime', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: { doses: 1 } },
    ]),
  );
  await assertSucceeds(decreasingIstate.commit());

  // 5. SESSION_ACTION ABILITY_USE omitting server costValue (-3 energy) MUST FAIL
  const omitCost = writeBatch(ownerDb);
  omitCost.update(doc(ownerDb, 'characters', ids.char, 'items', 'abil_slash', 'fields', 'active'), {
    value: true,
    revision: 2,
    lastOperationId: 'op-abil-omit-cost',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  omitCost.set(
    doc(ownerDb, 'characters', ids.char, 'operations', 'op-abil-omit-cost'),
    makeOp(
      'op-abil-omit-cost',
      ids.owner,
      'SESSION_ACTION',
      [
        { kind: 'ITEM_FIELD', itemId: 'abil_slash', key: 'active', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: true },
      ],
      { payloadHash: 'sha256-abil-omit', actionType: 'ABILITY_USE', abilityItemId: 'abil_slash' },
    ),
  );
  await assertFails(omitCost.commit());

  // 6. SESSION_ACTION ABILITY_USE with wrong cost amount (-1 instead of -3) MUST FAIL
  const underpayCost = writeBatch(ownerDb);
  underpayCost.update(doc(ownerDb, 'characters', ids.char, 'fields', 'energy'), {
    value: { current: 9, maximum: 10, adjustment: 0 },
    revision: 2,
    lastOperationId: 'op-abil-underpay',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  underpayCost.set(
    doc(ownerDb, 'characters', ids.char, 'operations', 'op-abil-underpay'),
    makeOp(
      'op-abil-underpay',
      ids.owner,
      'SESSION_ACTION',
      [
        { kind: 'FIELD', itemId: '', key: 'energy', op: 'DELTA', expectedRevision: 1, nextRevision: 2, delta: -1 },
      ],
      { payloadHash: 'sha256-abil-underpay', actionType: 'ABILITY_USE', abilityItemId: 'abil_slash' },
    ),
  );
  await assertFails(underpayCost.commit());

  // 7. SESSION_ACTION ABILITY_USE with exact -costValue (-3 energy) + active toggle SUCCEEDS
  const validAbilityUse = writeBatch(ownerDb);
  validAbilityUse.update(doc(ownerDb, 'characters', ids.char, 'fields', 'energy'), {
    value: { current: 7, maximum: 10, adjustment: 0 },
    revision: 2,
    lastOperationId: 'op-abil-ok',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  validAbilityUse.update(doc(ownerDb, 'characters', ids.char, 'items', 'abil_slash', 'fields', 'active'), {
    value: true,
    revision: 2,
    lastOperationId: 'op-abil-ok',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  validAbilityUse.set(
    doc(ownerDb, 'characters', ids.char, 'operations', 'op-abil-ok'),
    makeOp(
      'op-abil-ok',
      ids.owner,
      'SESSION_ACTION',
      [
        { kind: 'FIELD', itemId: '', key: 'energy', op: 'DELTA', expectedRevision: 1, nextRevision: 2, delta: -3 },
        { kind: 'ITEM_FIELD', itemId: 'abil_slash', key: 'active', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: true },
      ],
      { payloadHash: 'sha256-abil-ok', actionType: 'ABILITY_USE', abilityItemId: 'abil_slash' },
    ),
  );
  await assertSucceeds(validAbilityUse.commit());

  // 8. Unvalidated complex formula SESSION_ACTION rejected for player, allowed for active GM
  const playerComplex = writeBatch(ownerDb);
  playerComplex.update(doc(ownerDb, 'characters', ids.char, 'fields', 'name'), {
    value: 'Complex Formula',
    revision: 2,
    lastOperationId: 'op-complex-p',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  playerComplex.set(
    doc(ownerDb, 'characters', ids.char, 'operations', 'op-complex-p'),
    makeOp(
      'op-complex-p',
      ids.owner,
      'SESSION_ACTION',
      [
        { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: 'Complex Formula' },
      ],
      { payloadHash: 'sha256-complex', actionType: 'EFFECT_OPERATION' },
    ),
  );
  await assertFails(playerComplex.commit());

  const gmComplex = writeBatch(gmDb);
  gmComplex.update(doc(gmDb, 'characters', ids.char, 'fields', 'name'), {
    value: 'GM Adjusted',
    revision: 2,
    lastOperationId: 'op-complex-gm',
    updatedBy: ids.gm,
    updatedAt: serverTimestamp(),
  });
  gmComplex.set(
    doc(gmDb, 'characters', ids.char, 'operations', 'op-complex-gm'),
    makeOp(
      'op-complex-gm',
      ids.gm,
      'SESSION_ACTION',
      [
        { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: 'GM Adjusted' },
      ],
      { payloadHash: 'sha256-complex-gm', actionType: 'EFFECT_OPERATION' },
    ),
  );
  await assertSucceeds(gmComplex.commit());
});

test('rejects substituted or omitted server mechanical effects in automated consumption', async () => {
  await seedBase();
  await env.withSecurityRulesDisabled(async c => {
    await setDoc(doc(c.firestore(), 'characters', ids.char, 'items', 'inv_potion', 'fields', 'mechanicalEffects'), makeField('mechanicalEffects', [
      { id: 'heal-life', kind: 'HEAL', amount: 3 },
      { id: 'restore-sanity', kind: 'SANITY', amount: 2 },
    ]));
  });
  for (const actor of [ids.owner, ids.gm]) {
  const db = env.authenticatedContext(actor).firestore();
  for (const effectKey of ['money', 'life']) {
    const opId = `op-substitute-${effectKey}`;
    const batch = writeBatch(db);
    batch.update(doc(db, 'characters', ids.char, 'items', 'inv_potion', 'fields', 'quantity'), {
      value: 1, revision: 2, lastOperationId: opId, updatedBy: actor, updatedAt: serverTimestamp(),
    });
    batch.update(doc(db, 'characters', ids.char, 'fields', effectKey), {
      value: effectKey === 'money' ? 101 : { current: 15, maximum: 15, adjustment: 0 },
      revision: 2, lastOperationId: opId, updatedBy: actor, updatedAt: serverTimestamp(),
    });
    batch.set(doc(db, 'characters', ids.char, 'operations', opId), makeOp(opId, actor, 'ITEM_CONSUME', [
      { kind: 'ITEM_FIELD', itemId: 'inv_potion', key: 'quantity', op: 'DELTA', expectedRevision: 1, nextRevision: 2, delta: -1 },
      { kind: 'FIELD', itemId: '', key: effectKey, op: 'DELTA', expectedRevision: 1, nextRevision: 2, delta: effectKey === 'money' ? 1 : 3 },
    ]));
    await assertFails(batch.commit());
  }
  }
});

test('rejects resource overflow at stored ceiling and fixed corruption ceiling', async () => {
  await seedBase();
  await env.withSecurityRulesDisabled(async c => {
    await setDoc(doc(c.firestore(), 'characters', ids.char, 'fields', 'corruption'), makeField('corruption', { current: 99, maximum: 100, adjustment: 0 }));
  });
  const db = env.authenticatedContext(ids.owner).firestore();
  for (const [key, previous, maximum, delta] of [['life', 12, 15, 100], ['corruption', 99, 100, 2]]) {
    const opId = `op-overflow-${key}`;
    const batch = writeBatch(db);
    batch.update(doc(db, 'characters', ids.char, 'fields', key), {
      value: { current: previous + delta, maximum, adjustment: 0 },
      revision: 2, lastOperationId: opId, updatedBy: ids.owner, updatedAt: serverTimestamp(),
    });
    batch.set(doc(db, 'characters', ids.char, 'operations', opId), makeOp(opId, ids.owner, 'ADJUST_RESOURCE', [
      { kind: 'FIELD', itemId: '', key, op: 'DELTA', expectedRevision: 1, nextRevision: 2, delta },
    ]));
    await assertFails(batch.commit());
  }
});

test('rejects arbitrary rope payment for DOSE and AshCost ability activation', async () => {
  await seedBase();
  for (const actor of [ids.owner, ids.gm]) {
  const db = env.authenticatedContext(actor).firestore();
  for (const costType of ['DOSE', 'AshCost']) {
    await env.withSecurityRulesDisabled(async c => {
      const admin = c.firestore();
      await setDoc(doc(admin, 'characters', ids.char, 'items', 'inv_rope'), makeItem('inv_rope', 'INVENTORY'));
      await setDoc(doc(admin, 'characters', ids.char, 'items', 'inv_rope', 'fields', 'quantity'), makeField('quantity', 3));
      await setDoc(doc(admin, 'characters', ids.char, 'items', 'abil_magic'), makeItem('abil_magic', 'ABILITY'));
      for (const [key, value] of [['costType', costType], ['costValue', 1], ['active', false]]) {
        await setDoc(doc(admin, 'characters', ids.char, 'items', 'abil_magic', 'fields', key), makeField(key, value));
      }
    });
    const opId = `op-rope-${costType}`;
    const batch = writeBatch(db);
    for (const [itemId, key, value] of [['inv_rope', 'quantity', 2], ['abil_magic', 'active', true]]) {
      batch.update(doc(db, 'characters', ids.char, 'items', itemId, 'fields', key), {
        value, revision: 2, lastOperationId: opId, updatedBy: actor, updatedAt: serverTimestamp(),
      });
    }
    batch.set(doc(db, 'characters', ids.char, 'operations', opId), makeOp(opId, actor, 'SESSION_ACTION', [
      { kind: 'ITEM_FIELD', itemId: 'inv_rope', key: 'quantity', op: 'DELTA', expectedRevision: 1, nextRevision: 2, delta: -1 },
      { kind: 'ITEM_FIELD', itemId: 'abil_magic', key: 'active', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: true },
    ], { payloadHash: 'sha256-rope', actionType: 'ABILITY_USE', abilityItemId: 'abil_magic' }));
    await assertFails(batch.commit());
  }
  }
});

test('legacy zero ceilings permit spending, reject automated increases, and preserve explicit GM adjustment', async () => {
  await seedBase();
  await env.withSecurityRulesDisabled(async c => {
    await setDoc(doc(c.firestore(), 'characters', ids.char, 'fields', 'life'), makeField('life', { current: 12, maximum: 0, adjustment: 0 }));
  });
  const ownerDb = env.authenticatedContext(ids.owner).firestore();
  async function change(db, uid, opId, type, op, value, delta, expectedRevision) {
    const batch = writeBatch(db);
    batch.update(doc(db, 'characters', ids.char, 'fields', 'life'), {
      value, revision: expectedRevision + 1, lastOperationId: opId, updatedBy: uid, updatedAt: serverTimestamp(),
    });
    batch.set(doc(db, 'characters', ids.char, 'operations', opId), makeOp(opId, uid, type, [
      { kind: 'FIELD', itemId: '', key: 'life', op, expectedRevision, nextRevision: expectedRevision + 1,
        ...(op === 'DELTA' ? { delta } : { desiredValue: value }) },
    ], { payloadHash: 'sha256-legacy-ceiling', actionType: 'GM_ADJUSTMENT' }));
    return batch.commit();
  }
  await assertFails(change(ownerDb, ids.owner, 'op-zero-raise', 'ADJUST_RESOURCE', 'DELTA', { current: 13, maximum: 0, adjustment: 0 }, 1, 1));
  await assertSucceeds(change(ownerDb, ids.owner, 'op-zero-spend', 'ADJUST_RESOURCE', 'DELTA', { current: 11, maximum: 0, adjustment: 0 }, -1, 1));
  const gmDb = env.authenticatedContext(ids.gm).firestore();
  await assertSucceeds(change(gmDb, ids.gm, 'op-explicit-adjust', 'SESSION_ACTION', 'SET', { current: 14, maximum: 0, adjustment: 0 }, null, 2));
});

test('fast-create, edit, and delete integrity with owner, verified admin, unverified admin, and unauthorized users', async () => {
  await seedBase();
  const ownerDb = env.authenticatedContext(ids.owner, { email: 'player1@example.com', email_verified: true }).firestore();
  const adminDb = env.authenticatedContext('admin-uid', { email: 'kindbarros@gmail.com', email_verified: true }).firestore();
  const unverifiedAdminDb = env.authenticatedContext('admin-unverified', { email: 'kindbarros@gmail.com', email_verified: false }).firestore();
  const outsiderDb = env.authenticatedContext(ids.outsider, { email: 'outsider@example.com', email_verified: true }).firestore();

  // 1. Owner fast-creates a personal sheet with minimal non-default fields only (no attr_* items, no default fields).
  const fastId = 'char-fast-owner';
  const fastBatch = writeBatch(ownerDb);
  fastBatch.set(doc(ownerDb, 'characters', fastId), makeRoot({
    id: fastId,
    ownerId: ids.owner,
    lastOperationId: 'op-fast-1',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  }));
  fastBatch.set(doc(ownerDb, 'characters', fastId, 'fields', 'creationStatus'), makeField('creationStatus', 'DRAFT', {
    lastOperationId: 'op-fast-1',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  }));
  fastBatch.set(doc(ownerDb, 'characters', fastId, 'operations', 'op-fast-1'), makeOp('op-fast-1', ids.owner, 'CREATE_SHEET', [
    { kind: 'SHEET', itemId: '', key: '', op: 'CREATE', expectedRevision: 0, nextRevision: 1 },
    { kind: 'FIELD', itemId: '', key: 'creationStatus', op: 'CREATE', expectedRevision: 0, nextRevision: 1, desiredValue: 'DRAFT' },
  ]));
  await assertSucceeds(fastBatch.commit());

  // 2. Owner edits a previously omitted field (rev 0 -> 1 CREATE) and updates creationStatus (rev 1 -> 2 SET).
  const editBatch = writeBatch(ownerDb);
  editBatch.set(doc(ownerDb, 'characters', fastId, 'fields', 'name'), makeField('name', 'Kael Fast', {
    revision: 1,
    lastOperationId: 'op-fast-edit',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  }));
  editBatch.update(doc(ownerDb, 'characters', fastId, 'fields', 'creationStatus'), {
    value: 'COMPLETED',
    revision: 2,
    lastOperationId: 'op-fast-edit',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  editBatch.set(doc(ownerDb, 'characters', fastId, 'operations', 'op-fast-edit'), makeOp('op-fast-edit', ids.owner, 'PATCH_FIELDS', [
    { kind: 'FIELD', itemId: '', key: 'name', op: 'CREATE', expectedRevision: 0, nextRevision: 1, desiredValue: 'Kael Fast' },
    { kind: 'FIELD', itemId: '', key: 'creationStatus', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: 'COMPLETED' },
  ]));
  await assertSucceeds(editBatch.commit());

  // 3. Owner and verified admin can read the personal sheet, while unverified admin and outsider cannot.
  const ownerRead = await assertSucceeds(getDoc(doc(ownerDb, 'characters', fastId)));
  assert.equal(ownerRead.data().ownerId, ids.owner);
  await assertSucceeds(getDoc(doc(adminDb, 'campaigns', ids.campaign)));
  await assertSucceeds(getDoc(doc(adminDb, 'characters', fastId)));
  await assertFails(getDoc(doc(unverifiedAdminDb, 'characters', fastId)));
  await assertFails(getDoc(doc(outsiderDb, 'characters', fastId)));

  // 4. Verified admin can edit fields/item quantities on another user's sheet, while outsider cannot edit or delete.
  const adminEditOther = writeBatch(adminDb);
  adminEditOther.update(doc(adminDb, 'characters', fastId, 'fields', 'name'), {
    value: 'Admin Override',
    revision: 2,
    lastOperationId: 'op-admin-edit-other',
    updatedBy: 'admin-uid',
    updatedAt: serverTimestamp(),
  });
  adminEditOther.set(doc(adminDb, 'characters', fastId, 'operations', 'op-admin-edit-other'), makeOp('op-admin-edit-other', 'admin-uid', 'PATCH_FIELDS', [
    { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: 'Admin Override' },
  ]));
  await assertSucceeds(adminEditOther.commit());

  const outsiderDel = writeBatch(outsiderDb);
  outsiderDel.update(doc(outsiderDb, 'characters', fastId), {
    deleted: true,
    revision: 2,
    lastOperationId: 'op-outsider-del',
    updatedBy: ids.outsider,
    updatedAt: serverTimestamp(),
  });
  outsiderDel.set(doc(outsiderDb, 'characters', fastId, 'operations', 'op-outsider-del'), makeOp('op-outsider-del', ids.outsider, 'DELETE_SHEET', [
    { kind: 'SHEET', itemId: '', key: '', op: 'TOMBSTONE', expectedRevision: 1, nextRevision: 2 },
  ]));
  await assertFails(outsiderDel.commit());
  await assertFails(deleteDoc(doc(ownerDb, 'characters', fastId)));

  // 5. Verified admin as own sheet owner / GM can fast-create, edit, and tombstone-delete own sheet.
  const adminSheetId = 'char-fast-admin';
  const adminCreate = writeBatch(adminDb);
  adminCreate.set(doc(adminDb, 'characters', adminSheetId), makeRoot({
    id: adminSheetId,
    ownerId: 'admin-uid',
    lastOperationId: 'op-admin-create',
    updatedBy: 'admin-uid',
    updatedAt: serverTimestamp(),
  }));
  adminCreate.set(doc(adminDb, 'characters', adminSheetId, 'operations', 'op-admin-create'), makeOp('op-admin-create', 'admin-uid', 'CREATE_SHEET', [
    { kind: 'SHEET', itemId: '', key: '', op: 'CREATE', expectedRevision: 0, nextRevision: 1 },
  ]));
  await assertSucceeds(adminCreate.commit());

  const adminDeleteOwn = writeBatch(adminDb);
  adminDeleteOwn.update(doc(adminDb, 'characters', adminSheetId), {
    deleted: true,
    revision: 2,
    lastOperationId: 'op-admin-del',
    updatedBy: 'admin-uid',
    updatedAt: serverTimestamp(),
  });
  adminDeleteOwn.set(doc(adminDb, 'characters', adminSheetId, 'operations', 'op-admin-del'), makeOp('op-admin-del', 'admin-uid', 'DELETE_SHEET', [
    { kind: 'SHEET', itemId: '', key: '', op: 'TOMBSTONE', expectedRevision: 1, nextRevision: 2 },
  ]));
  await assertSucceeds(adminDeleteOwn.commit());

  // 6. Owner logically deletes own fast-created sheet; subsequent edits are rejected.
  const ownerDel = writeBatch(ownerDb);
  ownerDel.update(doc(ownerDb, 'characters', fastId), {
    deleted: true,
    revision: 2,
    lastOperationId: 'op-owner-del',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  ownerDel.set(doc(ownerDb, 'characters', fastId, 'operations', 'op-owner-del'), makeOp('op-owner-del', ids.owner, 'DELETE_SHEET', [
    { kind: 'SHEET', itemId: '', key: '', op: 'TOMBSTONE', expectedRevision: 1, nextRevision: 2 },
  ]));
  await assertSucceeds(ownerDel.commit());

  const editAfterDelete = writeBatch(ownerDb);
  editAfterDelete.update(doc(ownerDb, 'characters', fastId, 'fields', 'name'), {
    value: 'Resurrect',
    revision: 2,
    lastOperationId: 'op-edit-deleted',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  editAfterDelete.set(doc(ownerDb, 'characters', fastId, 'operations', 'op-edit-deleted'), makeOp('op-edit-deleted', ids.owner, 'PATCH_FIELDS', [
    { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: 'Resurrect' },
  ]));
  await assertFails(editAfterDelete.commit());
});
