import { after, before, beforeEach, test } from 'node:test';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
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
  campaign: 'camp-1',
  owner: 'master-1',
  player: 'player-1',
  outsider: 'outsider-1',
  joiner: 'joiner-1',
  otherPlayer: 'player-2',
  invite: 'ABCDEFGH',
  character: 'char-1',
};

function makeRoot(overrides = {}) {
  return {
    id: ids.character,
    ownerId: ids.player,
    campaignId: ids.campaign,
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
    updatedBy: ids.player,
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
    updatedBy: ids.player,
    updatedAt: Timestamp.fromMillis(1000),
    ...overrides,
  };
}

function makeOp(opId, actorUid, type, targets, preconditions = { payloadHash: 'hash-1' }) {
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

function makeMember(campaignId, userId, overrides = {}) {
  return {
    campaignId,
    userId,
    role: 'PLAYER',
    state: 'ACTIVE',
    characterIds: [],
    joinedAt: 1,
    updatedAt: 1,
    joinedByInviteId: '',
    ...overrides,
  };
}

function makeInvite(campaignId, inviteId, overrides = {}) {
  return {
    id: inviteId,
    campaignId,
    code: inviteId,
    campaignName: 'Teste',
    campaignDescription: 'Campanha de teste',
    createdBy: ids.owner,
    recipientUid: '',
    expiresAt: null,
    state: 'PENDING',
    consumedBy: '',
    consumedAt: null,
    revokedAt: null,
    createdAt: 1,
    ...overrides,
  };
}

async function seed({ archived = false, revoked = false } = {}) {
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, 'campaigns', ids.campaign), {
      id: ids.campaign,
      name: 'Teste',
      description: 'Campanha de teste',
      ownerId: ids.owner,
      state: archived ? 'ARCHIVED' : 'ACTIVE',
      createdAt: 1,
      updatedAt: 1,
      archivedAt: archived ? 2 : null,
      allowPlayerCharacterCreation: true,
    });
    await setDoc(doc(db, 'campaigns', ids.campaign, 'members', ids.owner), makeMember(ids.campaign, ids.owner));
    await setDoc(
      doc(db, 'campaigns', ids.campaign, 'members', ids.player),
      makeMember(ids.campaign, ids.player, { characterIds: [ids.character], joinedByInviteId: ids.invite }),
    );
    await setDoc(
      doc(db, 'campaigns', ids.campaign, 'members', ids.otherPlayer),
      makeMember(ids.campaign, ids.otherPlayer, { joinedByInviteId: ids.invite }),
    );
    await setDoc(
      doc(db, 'campaigns', ids.campaign, 'invites', ids.invite),
      makeInvite(ids.campaign, ids.invite, {
        state: revoked ? 'REVOKED' : 'PENDING',
        revokedAt: revoked ? 2 : null,
      }),
    );
    await setDoc(doc(db, 'characters', ids.character), makeRoot());
    await setDoc(doc(db, 'characters', ids.character, 'fields', 'name'), makeField('name', 'Personagem'));
  });
}

async function seedLibraryAndDelivery({ archived = false } = {}) {
  await seed({ archived });
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, 'campaignLibrary', 'library-1'), {
      id: 'library-1',
      campaignId: ids.campaign,
      kind: 'ITEM',
      name: 'Item',
      summary: 'Resumo',
      payload: '',
      catalogEntryId: '',
      knowledgeBonus: 0,
      archived: false,
      version: 1,
      createdBy: ids.owner,
      createdAt: 1,
      updatedAt: 1,
    });
    await setDoc(doc(db, 'campaignDeliveries', 'delivery-1'), {
      id: 'delivery-1',
      campaignId: ids.campaign,
      libraryEntryId: 'library-1',
      recipientId: ids.player,
      recipientCharacterId: ids.character,
      snapshotKind: 'ITEM',
      snapshotName: 'Item',
      snapshotSummary: 'Resumo',
      snapshotPayload: '',
      knowledgeMapping: '',
      knowledgeBonus: 0,
      state: 'PENDING',
      createdBy: ids.owner,
      createdAt: 1,
      updatedAt: 1,
    });
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

test('active campaign deletion detaches characters atomically and preserves their data', async () => {
  await seed();
  const db = env.authenticatedContext(ids.owner).firestore();
  const batch = writeBatch(db);
  batch.update(doc(db, 'characters', ids.character), {
    campaignId: '',
    authorizedCampaignId: '',
    revision: 2,
    lastOperationId: 'op-unlink-del',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  batch.set(
    doc(db, 'characters', ids.character, 'operations', 'op-unlink-del'),
    makeOp('op-unlink-del', ids.owner, 'UNLINK_CAMPAIGN', [
      { kind: 'SHEET', itemId: '', key: '', op: 'UNLINK', expectedRevision: 1, nextRevision: 2 },
    ]),
  );
  batch.update(doc(db, 'campaigns', ids.campaign), { state: 'DELETED', updatedAt: 3 });
  await assertSucceeds(batch.commit());
  await assertFails(updateDoc(doc(db, 'campaigns', ids.campaign), { state: 'ACTIVE' }));
  const playerDb = env.authenticatedContext(ids.player).firestore();
  const preserved = await assertSucceeds(getDoc(doc(playerDb, 'characters', ids.character)));
  if (preserved.data().campaignId !== '') throw new Error('Character not detached');
});

test('players cannot delete campaigns or detach someone else through a deletion batch', async () => {
  await seed();
  const db = env.authenticatedContext(ids.player).firestore();
  await assertFails(updateDoc(doc(db, 'campaigns', ids.campaign), { state: 'DELETED' }));
});

test('archived campaigns reject every library mutation and new delivery', async () => {
  await seedLibraryAndDelivery({ archived: true });
  const db = env.authenticatedContext(ids.owner).firestore();
  await assertFails(setDoc(doc(db, 'campaignLibrary', 'library-2'), {
    id: 'library-2', campaignId: ids.campaign, kind: 'NOTE', name: 'Nota', summary: '', payload: '',
    catalogEntryId: '', knowledgeBonus: 0, archived: false, version: 1,
    createdBy: ids.owner, createdAt: 2, updatedAt: 2,
  }));
  await assertFails(updateDoc(doc(db, 'campaignLibrary', 'library-1'), { name: 'Alterado', updatedAt: 2 }));
  await assertFails(setDoc(doc(db, 'campaignDeliveries', 'delivery-2'), {
    id: 'delivery-2', campaignId: ids.campaign, libraryEntryId: 'library-1',
    recipientId: ids.player, recipientCharacterId: ids.character,
    snapshotKind: 'ITEM', snapshotName: 'Item', snapshotSummary: '', snapshotPayload: '',
    knowledgeMapping: '', knowledgeBonus: 0, state: 'PENDING', createdBy: ids.owner,
    createdAt: 2, updatedAt: 2,
  }));
});

test('delivery acceptance requires the character application marker and receipt in the same atomic write', async () => {
  await seedLibraryAndDelivery();
  const db = env.authenticatedContext(ids.player).firestore();
  await assertFails(updateDoc(doc(db, 'campaignDeliveries', 'delivery-1'), { state: 'ACCEPTED', updatedAt: 2 }));

  const batch = writeBatch(db);
  batch.update(doc(db, 'characters', ids.character), {
    appliedDeliveryIds: ['delivery-1'],
    revision: 2,
    lastOperationId: 'op-deliv-1',
    updatedBy: ids.player,
    updatedAt: serverTimestamp(),
  });
  batch.set(
    doc(db, 'characters', ids.character, 'operations', 'op-deliv-1'),
    makeOp(
      'op-deliv-1',
      ids.player,
      'ACCEPT_DELIVERY',
      [{ kind: 'SHEET', itemId: '', key: '', op: 'DELIVERY', expectedRevision: 1, nextRevision: 2 }],
      { payloadHash: 'hash-deliv', deliveryId: 'delivery-1' },
    ),
  );
  batch.update(doc(db, 'campaignDeliveries', 'delivery-1'), { state: 'ACCEPTED', updatedAt: 2 });
  await assertSucceeds(batch.commit());
  await assertFails(updateDoc(doc(db, 'campaignDeliveries', 'delivery-1'), { state: 'ACCEPTED', updatedAt: 3 }));
});

test('sheet owner and active campaign GM can read linked character; other active campaign member cannot', async () => {
  await seed();
  const playerDb = env.authenticatedContext(ids.player).firestore();
  const gmDb = env.authenticatedContext(ids.owner).firestore();
  const otherDb = env.authenticatedContext(ids.otherPlayer).firestore();

  await assertSucceeds(getDoc(doc(playerDb, 'campaigns', ids.campaign)));
  await assertSucceeds(getDoc(doc(playerDb, 'characters', ids.character)));
  await assertSucceeds(getDoc(doc(playerDb, 'characters', ids.character, 'fields', 'name')));
  await assertSucceeds(getDoc(doc(gmDb, 'characters', ids.character)));
  await assertSucceeds(getDoc(doc(gmDb, 'characters', ids.character, 'fields', 'name')));

  // Strict Spark v2 ACL: another active campaign member cannot read another player's sheet
  await assertSucceeds(getDoc(doc(otherDb, 'campaigns', ids.campaign)));
  await assertFails(getDoc(doc(otherDb, 'characters', ids.character)));
  await assertFails(getDoc(doc(otherDb, 'characters', ids.character, 'fields', 'name')));
});

test('owner and GM can list their authorized characters', async () => {
  await seed();
  const playerDb = env.authenticatedContext(ids.player).firestore();
  const gmDb = env.authenticatedContext(ids.owner).firestore();

  await assertSucceeds(getDocs(query(collection(playerDb, 'characters'), where('ownerId', '==', ids.player))));
  await assertSucceeds(getDocs(query(collection(gmDb, 'characters'), where('campaignId', '==', ids.campaign))));
});

test('outsiders cannot read campaign or linked character', async () => {
  await seed();
  const db = env.authenticatedContext(ids.outsider).firestore();
  await assertFails(getDoc(doc(db, 'campaigns', ids.campaign)));
  await assertFails(getDoc(doc(db, 'characters', ids.character)));
  await assertFails(getDoc(doc(db, 'characters', ids.character, 'fields', 'name')));
});

test('authenticated usable invite can be previewed and found via collectionGroup, revoked invite cannot', async () => {
  await seed();
  const db = env.authenticatedContext(ids.joiner).firestore();
  await assertSucceeds(getDoc(doc(db, 'campaigns', ids.campaign, 'invites', ids.invite)));
  const found = await assertSucceeds(getDocs(query(
    collectionGroup(db, 'invites'),
    where('code', '==', ids.invite),
  )));
  if (found.size !== 1) throw new Error('Expected 1 invite from collectionGroup');

  await env.withSecurityRulesDisabled(async (context) => {
    await updateDoc(doc(context.firestore(), 'campaigns', ids.campaign, 'invites', ids.invite), {
      state: 'REVOKED',
      revokedAt: 2,
    });
  });
  await assertFails(getDoc(doc(db, 'campaigns', ids.campaign, 'invites', ids.invite)));
});

test('invite consumption atomically creates only own active player membership', async () => {
  await seed();
  const db = env.authenticatedContext(ids.joiner).firestore();
  const inviteRef = doc(db, 'campaigns', ids.campaign, 'invites', ids.invite);
  const memberRef = doc(db, 'campaigns', ids.campaign, 'members', ids.joiner);

  await assertFails(setDoc(memberRef, makeMember(ids.campaign, ids.joiner, { joinedByInviteId: ids.invite })));

  const batch = writeBatch(db);
  batch.update(inviteRef, {
    state: 'CONSUMED',
    consumedBy: ids.joiner,
    consumedAt: serverTimestamp(),
  });
  batch.set(memberRef, makeMember(ids.campaign, ids.joiner, { joinedByInviteId: ids.invite }));
  await assertSucceeds(batch.commit());

  await env.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), 'campaigns', ids.campaign, 'invites', 'INV22222'), makeInvite(ids.campaign, 'INV22222'));
  });
  const badBatch = writeBatch(db);
  badBatch.update(doc(db, 'campaigns', ids.campaign, 'invites', 'INV22222'), {
    state: 'CONSUMED',
    consumedBy: ids.joiner,
    consumedAt: serverTimestamp(),
  });
  badBatch.set(doc(db, 'campaigns', ids.campaign, 'members', ids.outsider), makeMember(ids.campaign, ids.outsider, { joinedByInviteId: 'INV22222' }));
  await assertFails(badBatch.commit());
});

test('player can edit their own character fields with receipt, another active member cannot', async () => {
  await seed();
  const playerDb = env.authenticatedContext(ids.player).firestore();
  const otherDb = env.authenticatedContext(ids.otherPlayer).firestore();

  const batch = writeBatch(playerDb);
  batch.update(doc(playerDb, 'characters', ids.character, 'fields', 'name'), {
    value: 'Atualizado pelo jogador',
    revision: 2,
    lastOperationId: 'op-patch-1',
    updatedBy: ids.player,
    updatedAt: serverTimestamp(),
  });
  batch.set(
    doc(playerDb, 'characters', ids.character, 'operations', 'op-patch-1'),
    makeOp('op-patch-1', ids.player, 'PATCH_FIELDS', [
      { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 1, nextRevision: 2, baseValue: 'Personagem', desiredValue: 'Atualizado pelo jogador' },
    ]),
  );
  await assertSucceeds(batch.commit());

  const badBatch = writeBatch(otherDb);
  badBatch.update(doc(otherDb, 'characters', ids.character, 'fields', 'name'), {
    value: 'Não permitido',
    revision: 3,
    lastOperationId: 'op-patch-2',
    updatedBy: ids.otherPlayer,
    updatedAt: serverTimestamp(),
  });
  badBatch.set(
    doc(otherDb, 'characters', ids.character, 'operations', 'op-patch-2'),
    makeOp('op-patch-2', ids.otherPlayer, 'PATCH_FIELDS', [
      { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 2, nextRevision: 3, desiredValue: 'Não permitido' },
    ]),
  );
  await assertFails(badBatch.commit());
});

test('player can synchronize links for their own active campaign membership', async () => {
  await seed();
  const playerDb = env.authenticatedContext(ids.player).firestore();
  const otherMemberDb = env.authenticatedContext(ids.otherPlayer).firestore();
  const membership = doc(playerDb, 'campaigns', ids.campaign, 'members', ids.player);

  await assertSucceeds(updateDoc(membership, {
    characterIds: [ids.character, 'second-character'],
    updatedAt: 2,
  }));
  await assertFails(updateDoc(doc(otherMemberDb, 'campaigns', ids.campaign, 'members', ids.player), {
    characterIds: [],
    updatedAt: 3,
  }));
});

test('campaign owner can edit linked characters while campaign is active', async () => {
  await seed();
  const db = env.authenticatedContext(ids.owner).firestore();
  const batch = writeBatch(db);
  batch.update(doc(db, 'characters', ids.character, 'fields', 'name'), {
    value: 'Atualizado pela mestre',
    revision: 2,
    lastOperationId: 'op-gm-1',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  batch.set(
    doc(db, 'characters', ids.character, 'operations', 'op-gm-1'),
    makeOp('op-gm-1', ids.owner, 'PATCH_FIELDS', [
      { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: 'Atualizado pela mestre' },
    ]),
  );
  await assertSucceeds(batch.commit());
});

test('campaign member cannot gain HISTORIAN role or archive campaign', async () => {
  await seed();
  const db = env.authenticatedContext(ids.otherPlayer).firestore();
  await assertFails(updateDoc(doc(db, 'campaigns', ids.campaign, 'members', ids.otherPlayer), {
    role: 'HISTORIAN',
    updatedAt: 2,
  }));
  await assertFails(updateDoc(doc(db, 'campaigns', ids.campaign), {
    state: 'ARCHIVED',
    archivedAt: 2,
    updatedAt: 2,
  }));
});

test('owner campaign listing and empty real queries are authorized', async () => {
  await seed();
  const ownerDb = env.authenticatedContext(ids.owner).firestore();
  const emptyDb = env.authenticatedContext('account-with-no-data').firestore();
  await assertSucceeds(getDocs(query(collection(ownerDb, 'campaigns'), where('ownerId', '==', ids.owner))));
  await assertSucceeds(getDocs(query(collection(emptyDb, 'campaigns'), where('ownerId', '==', 'account-with-no-data'))));
  await assertSucceeds(getDocs(query(collectionGroup(emptyDb, 'members'), where('userId', '==', 'account-with-no-data'))));
  await assertSucceeds(getDocs(query(collection(emptyDb, 'characters'), where('ownerId', '==', 'account-with-no-data'))));
});

test('campaign creator remains the only master and ownership or roles cannot be transferred', async () => {
  await seed();
  const db = env.authenticatedContext(ids.owner).firestore();
  await assertFails(updateDoc(doc(db, 'campaigns', ids.campaign), { ownerId: ids.otherPlayer, updatedAt: 3 }));
  await assertFails(updateDoc(doc(db, 'campaigns', ids.campaign, 'members', ids.player), {
    role: 'HISTORIAN', updatedAt: 3,
  }));
});

test('campaign master can create a linked sheet assigned to an active player', async () => {
  await seed();
  const db = env.authenticatedContext(ids.owner).firestore();

  const goodBatch = writeBatch(db);
  goodBatch.set(doc(db, 'characters', 'assigned-by-master'), makeRoot({
    id: 'assigned-by-master',
    ownerId: ids.player,
    campaignId: ids.campaign,
    lastOperationId: 'op-create-assigned',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  }));
  goodBatch.set(
    doc(db, 'characters', 'assigned-by-master', 'operations', 'op-create-assigned'),
    makeOp('op-create-assigned', ids.owner, 'CREATE_SHEET', [
      { kind: 'SHEET', itemId: '', key: '', op: 'CREATE', expectedRevision: 0, nextRevision: 1 },
    ]),
  );
  await assertSucceeds(goodBatch.commit());

  const badBatch = writeBatch(db);
  badBatch.set(doc(db, 'characters', 'assigned-outsider'), makeRoot({
    id: 'assigned-outsider',
    ownerId: ids.outsider,
    campaignId: ids.campaign,
    lastOperationId: 'op-create-outsider',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  }));
  badBatch.set(
    doc(db, 'characters', 'assigned-outsider', 'operations', 'op-create-outsider'),
    makeOp('op-create-outsider', ids.owner, 'CREATE_SHEET', [
      { kind: 'SHEET', itemId: '', key: '', op: 'CREATE', expectedRevision: 0, nextRevision: 1 },
    ]),
  );
  await assertFails(badBatch.commit());
});

test('player can check missing character UUID and publish new Spark v2 sheet and fields', async () => {
  await seed();
  const unauthDb = env.unauthenticatedContext().firestore();
  const db = env.authenticatedContext(ids.player).firestore();
  const charRef = doc(db, 'characters', 'new-player-character');

  await assertFails(getDoc(doc(unauthDb, 'characters', 'new-player-character')));
  await assertSucceeds(getDoc(charRef));

  const createBatch = writeBatch(db);
  createBatch.set(charRef, makeRoot({
    id: 'new-player-character',
    ownerId: ids.player,
    campaignId: ids.campaign,
    lastOperationId: 'op-c1',
    updatedBy: ids.player,
    updatedAt: serverTimestamp(),
  }));
  createBatch.set(doc(db, 'characters', 'new-player-character', 'fields', 'name'), {
    key: 'name',
    value: 'Nova personagem',
    revision: 1,
    lastOperationId: 'op-c1',
    updatedBy: ids.player,
    updatedAt: serverTimestamp(),
  });
  createBatch.set(
    doc(db, 'characters', 'new-player-character', 'operations', 'op-c1'),
    makeOp('op-c1', ids.player, 'CREATE_SHEET', [
      { kind: 'SHEET', itemId: '', key: '', op: 'CREATE', expectedRevision: 0, nextRevision: 1 },
      { kind: 'FIELD', itemId: '', key: 'name', op: 'CREATE', expectedRevision: 0, nextRevision: 1, desiredValue: 'Nova personagem' },
    ]),
  );
  await assertSucceeds(createBatch.commit());

  const staleBatch = writeBatch(db);
  staleBatch.update(doc(db, 'characters', 'new-player-character', 'fields', 'name'), {
    value: 'Revisão repetida',
    revision: 1,
    lastOperationId: 'op-c2',
    updatedBy: ids.player,
    updatedAt: serverTimestamp(),
  });
  staleBatch.set(
    doc(db, 'characters', 'new-player-character', 'operations', 'op-c2'),
    makeOp('op-c2', ids.player, 'PATCH_FIELDS', [
      { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 1, nextRevision: 1, desiredValue: 'Revisão repetida' },
    ]),
  );
  await assertFails(staleBatch.commit());

  const nextBatch = writeBatch(db);
  nextBatch.update(doc(db, 'characters', 'new-player-character', 'fields', 'name'), {
    value: 'Revisão incrementada válida',
    revision: 2,
    lastOperationId: 'op-c3',
    updatedBy: ids.player,
    updatedAt: serverTimestamp(),
  });
  nextBatch.set(
    doc(db, 'characters', 'new-player-character', 'operations', 'op-c3'),
    makeOp('op-c3', ids.player, 'PATCH_FIELDS', [
      { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: 'Revisão incrementada válida' },
    ]),
  );
  await assertSucceeds(nextBatch.commit());
});

test('sheet owner can unlink and logically delete their own locked sheet even when campaign is archived', async () => {
  await seed({ archived: true });
  await env.withSecurityRulesDisabled(async (context) => {
    await updateDoc(doc(context.firestore(), 'characters', ids.character), {
      lockType: 'HISTORIAN',
      isLocked: true,
      lockedBy: ids.owner,
      lockedAt: Timestamp.fromMillis(1500),
    });
  });
  const db = env.authenticatedContext(ids.player).firestore();

  const unlinkBatch = writeBatch(db);
  unlinkBatch.update(doc(db, 'characters', ids.character), {
    campaignId: '',
    authorizedCampaignId: '',
    revision: 2,
    lastOperationId: 'op-unlink',
    updatedBy: ids.player,
    updatedAt: serverTimestamp(),
  });
  unlinkBatch.set(
    doc(db, 'characters', ids.character, 'operations', 'op-unlink'),
    makeOp('op-unlink', ids.player, 'UNLINK_CAMPAIGN', [
      { kind: 'SHEET', itemId: '', key: '', op: 'UNLINK', expectedRevision: 1, nextRevision: 2 },
    ]),
  );
  await assertSucceeds(unlinkBatch.commit());

  const deleteBatch = writeBatch(db);
  deleteBatch.update(doc(db, 'characters', ids.character), {
    deleted: true,
    revision: 3,
    lastOperationId: 'op-del',
    updatedBy: ids.player,
    updatedAt: serverTimestamp(),
  });
  deleteBatch.set(
    doc(db, 'characters', ids.character, 'operations', 'op-del'),
    makeOp('op-del', ids.player, 'DELETE_SHEET', [
      { kind: 'SHEET', itemId: '', key: '', op: 'TOMBSTONE', expectedRevision: 2, nextRevision: 3 },
    ]),
  );
  await assertSucceeds(deleteBatch.commit());
  await assertFails(deleteDoc(doc(db, 'characters', ids.character)));
});

test('campaign master may edit and unlink but may not delete another player sheet', async () => {
  await seed();
  const db = env.authenticatedContext(ids.owner).firestore();

  const editBatch = writeBatch(db);
  editBatch.update(doc(db, 'characters', ids.character, 'fields', 'name'), {
    value: 'Edição da Mestre',
    revision: 2,
    lastOperationId: 'op-edit',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  editBatch.set(
    doc(db, 'characters', ids.character, 'operations', 'op-edit'),
    makeOp('op-edit', ids.owner, 'PATCH_FIELDS', [
      { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: 'Edição da Mestre' },
    ]),
  );
  await assertSucceeds(editBatch.commit());

  const delBatch = writeBatch(db);
  delBatch.update(doc(db, 'characters', ids.character), {
    deleted: true,
    revision: 2,
    lastOperationId: 'op-gm-del',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  delBatch.set(
    doc(db, 'characters', ids.character, 'operations', 'op-gm-del'),
    makeOp('op-gm-del', ids.owner, 'DELETE_SHEET', [
      { kind: 'SHEET', itemId: '', key: '', op: 'TOMBSTONE', expectedRevision: 1, nextRevision: 2 },
    ]),
  );
  await assertFails(delBatch.commit());

  const unlinkBatch = writeBatch(db);
  unlinkBatch.update(doc(db, 'characters', ids.character), {
    campaignId: '',
    authorizedCampaignId: '',
    revision: 2,
    lastOperationId: 'op-gm-unlink',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  unlinkBatch.set(
    doc(db, 'characters', ids.character, 'operations', 'op-gm-unlink'),
    makeOp('op-gm-unlink', ids.owner, 'UNLINK_CAMPAIGN', [
      { kind: 'SHEET', itemId: '', key: '', op: 'UNLINK', expectedRevision: 1, nextRevision: 2 },
    ]),
  );
  await assertSucceeds(unlinkBatch.commit());
  await assertFails(deleteDoc(doc(db, 'characters', ids.character)));
});

test('campaign master cannot alter field data during UNLINK_CAMPAIGN', async () => {
  await seed();
  const db = env.authenticatedContext(ids.owner).firestore();
  const batch = writeBatch(db);
  batch.update(doc(db, 'characters', ids.character), {
    campaignId: '',
    authorizedCampaignId: '',
    revision: 2,
    lastOperationId: 'op-bad-unlink',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  batch.update(doc(db, 'characters', ids.character, 'fields', 'name'), {
    value: 'Alterada durante desvinculação',
    revision: 2,
    lastOperationId: 'op-bad-unlink',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  batch.set(
    doc(db, 'characters', ids.character, 'operations', 'op-bad-unlink'),
    makeOp('op-bad-unlink', ids.owner, 'UNLINK_CAMPAIGN', [
      { kind: 'SHEET', itemId: '', key: '', op: 'UNLINK', expectedRevision: 1, nextRevision: 2 },
      { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: 'Alterada durante desvinculação' },
    ]),
  );
  await assertFails(batch.commit());
});

test('audit operation can be created without a forbidden read of a missing document', async () => {
  await seed();
  const db = env.authenticatedContext(ids.player).firestore();
  await assertSucceeds(setDoc(doc(db, 'campaignAudit', 'operation-1'), {
    idempotencyKey: 'operation-1',
    campaignId: ids.campaign,
    characterId: ids.character,
    actorId: ids.player,
    type: 'RESOURCE',
    createdAt: 2,
  }));
  await assertFails(updateDoc(doc(db, 'campaignAudit', 'operation-1'), { type: 'ALTERED' }));
});

test('global profile role cannot grant administration', async () => {
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, 'users', ids.outsider), { displayName: 'Outra', role: 'MASTER' });
    await setDoc(doc(db, 'users', ids.player), { displayName: 'Player', role: 'USER' });
  });
  const legacyMasterDb = env.authenticatedContext(ids.outsider, {
    email: 'other@example.com',
    email_verified: true,
  }).firestore();
  await assertFails(getDoc(doc(legacyMasterDb, 'users', ids.player)));
  await assertFails(updateDoc(doc(legacyMasterDb, 'users', ids.player), { displayName: 'Invadido' }));

  const selfDb = env.authenticatedContext('new-user').firestore();
  await assertFails(setDoc(doc(selfDb, 'users', 'new-user'), { displayName: 'Nova', role: 'MASTER' }));
  await assertSucceeds(setDoc(doc(selfDb, 'users', 'new-user'), { displayName: 'Nova', role: 'USER' }));
  await assertFails(updateDoc(doc(selfDb, 'users', 'new-user'), { role: 'MASTER' }));
});

test('administrator requires verified identity for users/campaigns but CANNOT bypass character sheet ACL', async () => {
  await seed();
  await env.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), 'users', ids.player), { role: 'USER' });
  });
  const verified = env.authenticatedContext('admin', {
    email: 'kindbarros@gmail.com',
    email_verified: true,
  }).firestore();
  const unverified = env.authenticatedContext('lookalike', {
    email: 'kindbarros@gmail.com',
    email_verified: false,
  }).firestore();
  await assertSucceeds(getDoc(doc(verified, 'users', ids.player)));
  await assertFails(getDoc(doc(unverified, 'users', ids.player)));
  await assertSucceeds(getDocs(collection(verified, 'campaigns')));

  // Strict Spark v2 ACL: NO global administrator bypass read/write on another user's sheet!
  await assertFails(getDoc(doc(verified, 'characters', ids.character)));
  await assertFails(getDocs(collection(verified, 'characters')));
  const badAdminEdit = writeBatch(verified);
  badAdminEdit.update(doc(verified, 'characters', ids.character, 'fields', 'name'), {
    value: 'Bypass bloqueado',
    revision: 2,
    lastOperationId: 'op-admin-bypass',
    updatedBy: 'admin',
    updatedAt: serverTimestamp(),
  });
  badAdminEdit.set(
    doc(verified, 'characters', ids.character, 'operations', 'op-admin-bypass'),
    makeOp('op-admin-bypass', 'admin', 'PATCH_FIELDS', [
      { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: 'Bypass bloqueado' },
    ]),
  );
  await assertFails(badAdminEdit.commit());
});

test('archived campaign is read-only for characters', async () => {
  await seed({ archived: true });
  const ownerDb = env.authenticatedContext(ids.owner).firestore();
  const playerDb = env.authenticatedContext(ids.player).firestore();
  await assertSucceeds(getDoc(doc(playerDb, 'characters', ids.character)));

  for (const [uid, db] of [[ids.owner, ownerDb], [ids.player, playerDb]]) {
    const batch = writeBatch(db);
    batch.update(doc(db, 'characters', ids.character, 'fields', 'name'), {
      value: 'Bloqueado',
      revision: 2,
      lastOperationId: `op-arch-${uid}`,
      updatedBy: uid,
      updatedAt: serverTimestamp(),
    });
    batch.set(
      doc(db, 'characters', ids.character, 'operations', `op-arch-${uid}`),
      makeOp(`op-arch-${uid}`, uid, 'PATCH_FIELDS', [
        { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: 'Bloqueado' },
      ]),
    );
    await assertFails(batch.commit());
  }
});

test('campaign owner can dismantle and delete an archived campaign safely', async () => {
  await seed({ archived: true });
  const ownerDb = env.authenticatedContext(ids.owner).firestore();

  const batch = writeBatch(ownerDb);
  batch.update(doc(ownerDb, 'characters', ids.character), {
    campaignId: '',
    authorizedCampaignId: '',
    revision: 2,
    lastOperationId: 'op-dismantle',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  batch.set(
    doc(ownerDb, 'characters', ids.character, 'operations', 'op-dismantle'),
    makeOp('op-dismantle', ids.owner, 'UNLINK_CAMPAIGN', [
      { kind: 'SHEET', itemId: '', key: '', op: 'UNLINK', expectedRevision: 1, nextRevision: 2 },
    ]),
  );
  batch.update(doc(ownerDb, 'campaigns', ids.campaign), { state: 'DELETED', updatedAt: 3 });
  await assertSucceeds(batch.commit());
  await assertFails(updateDoc(doc(ownerDb, 'campaigns', ids.campaign), { state: 'ACTIVE' }));
  const playerDb = env.authenticatedContext(ids.player).firestore();
  const preserved = await assertSucceeds(getDoc(doc(playerDb, 'characters', ids.character)));
  if (preserved.data().campaignId !== '') throw new Error('Character must survive detached');
});

test('campaign journal allows active member create, author edit, GM moderation with immutable author, and blocks outsiders/archived/stale revision', async () => {
  await seed();
  const playerDb = env.authenticatedContext(ids.player).firestore();
  const ownerDb = env.authenticatedContext(ids.owner).firestore();
  const outsiderDb = env.authenticatedContext(ids.outsider).firestore();
  const otherDb = env.authenticatedContext(ids.otherPlayer).firestore();
  const adminDb = env.authenticatedContext('admin-uid', {
    email: 'kindbarros@gmail.com',
    email_verified: true,
  }).firestore();
  const journalQuery = (db) => query(collection(db, 'campaignJournal'), where('campaignId', '==', ids.campaign));

  await assertSucceeds(getDocs(journalQuery(adminDb)));
  await assertSucceeds(getDocs(journalQuery(ownerDb)));
  await assertSucceeds(getDocs(journalQuery(playerDb)));
  await assertFails(getDocs(journalQuery(outsiderDb)));

  const entryRef = (db) => doc(db, 'campaignJournal', 'entry-1');
  const baseEntry = {
    id: 'entry-1',
    campaignId: ids.campaign,
    authorId: ids.player,
    authorName: 'Ana',
    title: 'A chegada a Valdora',
    body: 'Chegamos ao anoitecer.',
    createdAt: 10,
    updatedAt: 10,
    revision: 1,
    lastEditorId: ids.player,
    lastEditorName: 'Ana',
    operationId: 'op-1',
    deleted: false,
    deletedAt: null,
  };

  await assertFails(setDoc(entryRef(playerDb), { ...baseEntry, authorId: ids.owner }));
  await assertFails(setDoc(entryRef(playerDb), { ...baseEntry, title: 'x'.repeat(121) }));
  await assertFails(setDoc(entryRef(outsiderDb), { ...baseEntry, authorId: ids.outsider, lastEditorId: ids.outsider }));

  await assertSucceeds(setDoc(entryRef(playerDb), baseEntry));
  await assertSucceeds(getDoc(entryRef(ownerDb)));
  await assertSucceeds(getDoc(entryRef(otherDb)));
  await assertSucceeds(getDoc(entryRef(adminDb)));
  await assertFails(getDoc(entryRef(outsiderDb)));

  await assertFails(updateDoc(entryRef(otherDb), {
    title: 'Alterado',
    updatedAt: 11,
    revision: 2,
    lastEditorId: ids.otherPlayer,
  }));

  await assertFails(updateDoc(entryRef(playerDb), {
    title: 'Revisao errada',
    updatedAt: 11,
    revision: 1,
    lastEditorId: ids.player,
  }));
  await assertSucceeds(updateDoc(entryRef(playerDb), {
    title: 'A chegada a Valdora (rev)',
    updatedAt: 11,
    revision: 2,
    lastEditorId: ids.player,
  }));

  await assertFails(updateDoc(entryRef(ownerDb), {
    authorId: ids.owner,
    updatedAt: 12,
    revision: 3,
    lastEditorId: ids.owner,
  }));
  await assertSucceeds(updateDoc(entryRef(ownerDb), {
    body: 'Moderado pelo Mestre',
    updatedAt: 12,
    revision: 3,
    lastEditorId: ids.owner,
  }));

  await assertFails(deleteDoc(entryRef(ownerDb)));

  await env.withSecurityRulesDisabled(async (context) => {
    await updateDoc(doc(context.firestore(), 'campaigns', ids.campaign), { state: 'ARCHIVED' });
  });
  await assertSucceeds(getDoc(entryRef(playerDb)));
  await assertFails(updateDoc(entryRef(playerDb), {
    body: 'Em campanha arquivada',
    updatedAt: 13,
    revision: 4,
    lastEditorId: ids.player,
  }));
});

test('non-owner tombstones and physical campaign deletion are denied', async () => {
  await seed();
  const ownerDb = env.authenticatedContext(ids.owner).firestore();
  const playerDb = env.authenticatedContext(ids.player).firestore();

  await assertFails(updateDoc(doc(playerDb, 'campaigns', ids.campaign), { state: 'DELETED' }));
  await assertFails(deleteDoc(doc(ownerDb, 'campaigns', ids.campaign)));
  await assertFails(deleteDoc(doc(playerDb, 'campaigns', ids.campaign)));
});

test('unauthenticated users cannot preview invites', async () => {
  await seed();
  const db = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db, 'campaigns', ids.campaign, 'invites', ids.invite)));
});

test('player can rejoin after leaving with a valid invite without changing original join time', async () => {
  await seed();
  const db = env.authenticatedContext(ids.player).firestore();
  const memberRef = doc(db, 'campaigns', ids.campaign, 'members', ids.player);
  const inviteRef = doc(db, 'campaigns', ids.campaign, 'invites', ids.invite);

  await assertSucceeds(updateDoc(memberRef, { state: 'LEFT', characterIds: [], updatedAt: 2 }));
  const rejoinBatch = writeBatch(db);
  rejoinBatch.update(inviteRef, {
    state: 'CONSUMED',
    consumedBy: ids.player,
    consumedAt: serverTimestamp(),
  });
  rejoinBatch.update(memberRef, {
    state: 'ACTIVE',
    joinedByInviteId: ids.invite,
    updatedAt: 3,
  });
  await assertSucceeds(rejoinBatch.commit());
  await assertSucceeds(getDoc(doc(db, 'campaigns', ids.campaign)));
});

test('GM linking an orphan campaign sheet atomically enrolls its owner and restores player access', async () => {
  await seed();
  await env.withSecurityRulesDisabled(async (context) => {
    await deleteDoc(doc(context.firestore(), 'campaigns', ids.campaign, 'members', ids.player));
  });
  const db = env.authenticatedContext(ids.owner).firestore();
  await assertSucceeds(runTransaction(db, async (transaction) => {
    const sheet = doc(db, 'characters', ids.character);
    const member = doc(db, 'campaigns', ids.campaign, 'members', ids.player);
    const op = doc(db, 'characters', ids.character, 'operations', 'op-link-1');
    const snapshot = await transaction.get(sheet);
    await transaction.get(member);
    transaction.set(member, makeMember(ids.campaign, ids.player, {
      characterIds: [ids.character],
      joinedAt: 3,
      updatedAt: 3,
    }));
    transaction.update(sheet, {
      campaignId: ids.campaign,
      authorizedCampaignId: '',
      revision: snapshot.data().revision + 1,
      lastOperationId: 'op-link-1',
      updatedBy: ids.owner,
      updatedAt: serverTimestamp(),
    });
    transaction.set(op, makeOp('op-link-1', ids.owner, 'LINK_CAMPAIGN', [
      { kind: 'SHEET', itemId: '', key: '', op: 'LINK', expectedRevision: 1, nextRevision: 2 },
    ]));
  }));
  const playerDb = env.authenticatedContext(ids.player).firestore();
  await assertSucceeds(getDoc(doc(playerDb, 'campaigns', ids.campaign)));
  await assertSucceeds(getDocs(query(collection(playerDb, 'characters'), where('ownerId', '==', ids.player))));

  const batch = writeBatch(playerDb);
  batch.update(doc(playerDb, 'characters', ids.character, 'fields', 'name'), {
    value: 'Saved by owner',
    revision: 2,
    lastOperationId: 'op-owner-save',
    updatedBy: ids.player,
    updatedAt: serverTimestamp(),
  });
  batch.set(
    doc(playerDb, 'characters', ids.character, 'operations', 'op-owner-save'),
    makeOp('op-owner-save', ids.player, 'PATCH_FIELDS', [
      { kind: 'FIELD', itemId: '', key: 'name', op: 'SET', expectedRevision: 1, nextRevision: 2, desiredValue: 'Saved by owner' },
    ]),
  );
  await assertSucceeds(batch.commit());
});

test('GM linking a standalone sheet requires owner-consented authorizedCampaignId and clears it on link', async () => {
  await seed();
  await env.withSecurityRulesDisabled(async (context) => {
    await updateDoc(doc(context.firestore(), 'characters', ids.character), {
      campaignId: '',
      authorizedCampaignId: '',
    });
    await deleteDoc(doc(context.firestore(), 'campaigns', ids.campaign, 'members', ids.player));
  });

  const gmDb = env.authenticatedContext(ids.owner).firestore();
  const playerDb = env.authenticatedContext(ids.player).firestore();

  // GM cannot read or capture standalone sheet without owner authorization
  await assertFails(getDoc(doc(gmDb, 'characters', ids.character)));
  const hijackBatch = writeBatch(gmDb);
  hijackBatch.set(doc(gmDb, 'campaigns', ids.campaign, 'members', ids.player), makeMember(ids.campaign, ids.player, {
    characterIds: [ids.character], joinedAt: 2, updatedAt: 2,
  }));
  hijackBatch.update(doc(gmDb, 'characters', ids.character), {
    campaignId: ids.campaign,
    authorizedCampaignId: '',
    revision: 2,
    lastOperationId: 'op-hijack',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  hijackBatch.set(
    doc(gmDb, 'characters', ids.character, 'operations', 'op-hijack'),
    makeOp('op-hijack', ids.owner, 'LINK_CAMPAIGN', [
      { kind: 'SHEET', itemId: '', key: '', op: 'LINK', expectedRevision: 1, nextRevision: 2 },
    ]),
  );
  await assertFails(hijackBatch.commit());

  // Owner pre-authorizes authorizedCampaignId
  const authBatch = writeBatch(playerDb);
  authBatch.update(doc(playerDb, 'characters', ids.character), {
    authorizedCampaignId: ids.campaign,
    revision: 2,
    lastOperationId: 'op-auth-link',
    updatedBy: ids.player,
    updatedAt: serverTimestamp(),
  });
  authBatch.set(
    doc(playerDb, 'characters', ids.character, 'operations', 'op-auth-link'),
    makeOp('op-auth-link', ids.player, 'LINK_CAMPAIGN', [
      { kind: 'SHEET', itemId: '', key: '', op: 'LINK', expectedRevision: 1, nextRevision: 2 },
    ]),
  );
  await assertSucceeds(authBatch.commit());

  // Now GM links atomically, activating owner PLAYER membership and clearing authorizedCampaignId
  const linkBatch = writeBatch(gmDb);
  linkBatch.set(doc(gmDb, 'campaigns', ids.campaign, 'members', ids.player), makeMember(ids.campaign, ids.player, {
    characterIds: [ids.character], joinedAt: 3, updatedAt: 3,
  }));
  linkBatch.update(doc(gmDb, 'characters', ids.character), {
    campaignId: ids.campaign,
    authorizedCampaignId: '',
    revision: 3,
    lastOperationId: 'op-gm-link-ok',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  linkBatch.set(
    doc(gmDb, 'characters', ids.character, 'operations', 'op-gm-link-ok'),
    makeOp('op-gm-link-ok', ids.owner, 'LINK_CAMPAIGN', [
      { kind: 'SHEET', itemId: '', key: '', op: 'LINK', expectedRevision: 2, nextRevision: 3 },
    ]),
  );
  await assertSucceeds(linkBatch.commit());
});

test('invite rejoin rejects revoked, expired, unrelated, archived and removed memberships', async () => {
  await seed();
  const db = env.authenticatedContext(ids.player).firestore();
  const memberRef = doc(db, 'campaigns', ids.campaign, 'members', ids.player);
  const inviteRef = doc(db, 'campaigns', ids.campaign, 'invites', ids.invite);

  await assertSucceeds(updateDoc(memberRef, { state: 'LEFT', characterIds: [], updatedAt: 2 }));
  const rejoin = () => {
    const batch = writeBatch(db);
    batch.update(inviteRef, { state: 'CONSUMED', consumedBy: ids.player, consumedAt: serverTimestamp() });
    batch.update(memberRef, { state: 'ACTIVE', joinedByInviteId: ids.invite, updatedAt: 3 });
    return batch.commit();
  };

  for (const fields of [
    { state: 'REVOKED', revokedAt: 2 },
    { expiresAt: 1 },
  ]) {
    await env.withSecurityRulesDisabled(async (context) => {
      await updateDoc(doc(context.firestore(), 'campaigns', ids.campaign, 'invites', ids.invite), fields);
    });
    await assertFails(rejoin());
    await seed();
    await assertSucceeds(updateDoc(memberRef, { state: 'LEFT', characterIds: [], updatedAt: 2 }));
  }

  await env.withSecurityRulesDisabled(async (context) => {
    await updateDoc(doc(context.firestore(), 'campaigns', ids.campaign), { state: 'ARCHIVED' });
  });
  await assertFails(rejoin());

  await seed();
  await env.withSecurityRulesDisabled(async (context) => {
    await updateDoc(doc(context.firestore(), 'campaigns', ids.campaign, 'members', ids.player), {
      state: 'REMOVED',
      characterIds: [],
    });
  });
  await assertFails(rejoin());
});

test('GM enrollment only grants player membership and requires canonical userId', async () => {
  await seed();
  const db = env.authenticatedContext(ids.owner).firestore();
  const fields = makeMember(ids.campaign, ids.outsider, { joinedAt: 3, updatedAt: 3 });

  await assertFails(setDoc(doc(db, 'campaigns', ids.campaign, 'members', 'forged-id'), fields));
  const member = doc(db, 'campaigns', ids.campaign, 'members', ids.outsider);
  await assertFails(setDoc(member, { ...fields, role: 'HISTORIAN' }));
  const playerDb = env.authenticatedContext(ids.player).firestore();
  await assertFails(setDoc(doc(playerDb, 'campaigns', ids.campaign, 'members', ids.outsider), fields));
  await assertSucceeds(setDoc(member, fields));
});

test('denied sheet link rolls back enrollment and preserves existing sheet data', async () => {
  await seed();
  await env.withSecurityRulesDisabled(async (context) => {
    await updateDoc(doc(context.firestore(), 'characters', ids.character), { campaignId: '' });
  });
  const db = env.authenticatedContext(ids.owner).firestore();
  const member = doc(db, 'campaigns', ids.campaign, 'members', ids.outsider);
  const batch = writeBatch(db);
  batch.set(member, makeMember(ids.campaign, ids.outsider, { characterIds: [ids.character], joinedAt: 3, updatedAt: 3 }));
  batch.update(doc(db, 'characters', ids.character), {
    ownerId: ids.outsider,
    campaignId: ids.campaign,
    revision: 2,
    lastOperationId: 'op-steal',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  batch.set(
    doc(db, 'characters', ids.character, 'operations', 'op-steal'),
    makeOp('op-steal', ids.owner, 'LINK_CAMPAIGN', [
      { kind: 'SHEET', itemId: '', key: '', op: 'LINK', expectedRevision: 1, nextRevision: 2 },
    ]),
  );
  await assertFails(batch.commit());
  const membership = await getDoc(member);
  if (membership.exists()) throw new Error('Denied link left partial membership');
  const sheet = await getDoc(doc(env.authenticatedContext(ids.player).firestore(), 'characters', ids.character));
  if (sheet.data().ownerId !== ids.player || sheet.data().campaignId !== '') {
    throw new Error('Denied link changed sheet data');
  }
});

test('campaign GM lock transitions work through SET_LOCK receipts', async () => {
  await seed();
  const db = env.authenticatedContext(ids.owner).firestore();
  const reference = doc(db, 'characters', ids.character);

  const lockBatch = writeBatch(db);
  lockBatch.update(reference, {
    lockType: 'HISTORIAN',
    isLocked: true,
    lockedBy: ids.owner,
    lockedAt: serverTimestamp(),
    revision: 2,
    lastOperationId: 'op-lock-1',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  lockBatch.set(
    doc(db, 'characters', ids.character, 'operations', 'op-lock-1'),
    makeOp('op-lock-1', ids.owner, 'SET_LOCK', [
      { kind: 'SHEET', itemId: '', key: '', op: 'LOCK', expectedRevision: 1, nextRevision: 2 },
    ]),
  );
  await assertSucceeds(lockBatch.commit());

  const unlockBatch = writeBatch(db);
  unlockBatch.update(reference, {
    lockType: 'NONE',
    isLocked: false,
    lockedBy: '',
    lockedAt: null,
    revision: 3,
    lastOperationId: 'op-lock-2',
    updatedBy: ids.owner,
    updatedAt: serverTimestamp(),
  });
  unlockBatch.set(
    doc(db, 'characters', ids.character, 'operations', 'op-lock-2'),
    makeOp('op-lock-2', ids.owner, 'SET_LOCK', [
      { kind: 'SHEET', itemId: '', key: '', op: 'LOCK', expectedRevision: 2, nextRevision: 3 },
    ]),
  );
  await assertSucceeds(unlockBatch.commit());
});
