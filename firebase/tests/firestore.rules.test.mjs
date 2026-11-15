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
  deleteDoc,
  deleteField,
  doc,
  getDoc,
  getDocs,
  query,
  runTransaction,
  setDoc,
  updateDoc,
  where,
  writeBatch,
} from 'firebase/firestore';

test('active campaign deletion detaches characters atomically and preserves their data', async () => {
  await seed();
  const db = env.authenticatedContext(ids.owner).firestore();
  const batch = writeBatch(db);
  batch.update(doc(db, 'characters', ids.character), { campaignId: '', updatedAt: 3 });
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
  historian: 'historian-1',
  invite: 'ABCDEFGH',
  character: 'char-1',
};

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
    await setDoc(doc(db, 'campaignMembers', `${ids.campaign}::${ids.owner}`), {
      campaignId: ids.campaign,
      userId: ids.owner,
      role: 'PLAYER',
      state: 'ACTIVE',
      joinedAt: 1,
      updatedAt: 1,
      characterIds: [],
      joinedByInviteId: '',
    });
    await setDoc(doc(db, 'campaignMembers', `${ids.campaign}::${ids.player}`), {
      campaignId: ids.campaign,
      userId: ids.player,
      role: 'PLAYER',
      state: 'ACTIVE',
      joinedAt: 1,
      updatedAt: 1,
      characterIds: [ids.character],
      joinedByInviteId: ids.invite,
    });
    await setDoc(doc(db, 'campaignMembers', `${ids.campaign}::${ids.historian}`), {
      campaignId: ids.campaign,
      userId: ids.historian,
      role: 'HISTORIAN',
      state: 'ACTIVE',
      joinedAt: 1,
      updatedAt: 1,
      characterIds: [],
      joinedByInviteId: ids.invite,
    });
    await setDoc(doc(db, 'campaignInvites', ids.invite), {
      campaignId: ids.campaign,
      campaignName: 'Teste',
      campaignDescription: 'Campanha de teste',
      code: ids.invite,
      createdBy: ids.owner,
      createdAt: 1,
      expiresAt: null,
      revokedAt: revoked ? 2 : null,
      generation: 1,
    });
    await setDoc(doc(db, 'characters', ids.character), {
      id: ids.character,
      ownerId: ids.player,
      campaignId: ids.campaign,
      name: 'Personagem',
      lockType: 'NONE',
      isLocked: false,
      lockedBy: '',
      lockedAt: null,
      deleted: false,
      appliedDeliveryIds: [],
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

async function seedLibraryAndDelivery({ archived = false } = {}) {
  await seed({ archived });
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, 'campaignLibrary', 'library-1'), {
      id: 'library-1', campaignId: ids.campaign, kind: 'ITEM', name: 'Item', summary: 'Resumo',
      payload: '', catalogEntryId: '', knowledgeBonus: 0, archived: false, version: 1,
      createdBy: ids.owner, createdAt: 1, updatedAt: 1,
    });
    await setDoc(doc(db, 'campaignDeliveries', 'delivery-1'), {
      id: 'delivery-1', campaignId: ids.campaign, libraryEntryId: 'library-1',
      recipientId: ids.player, recipientCharacterId: ids.character,
      snapshotKind: 'ITEM', snapshotName: 'Item', snapshotSummary: 'Resumo', snapshotPayload: '',
      knowledgeMapping: '', knowledgeBonus: 0, state: 'PENDING', createdBy: ids.owner,
      createdAt: 1, updatedAt: 1,
    });
  });
}

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

test('delivery acceptance requires the character application marker in the same atomic write', async () => {
  await seedLibraryAndDelivery();
  const db = env.authenticatedContext(ids.player).firestore();
  await assertFails(updateDoc(doc(db, 'campaignDeliveries', 'delivery-1'), { state: 'ACCEPTED', updatedAt: 2 }));

  const batch = writeBatch(db);
  batch.update(doc(db, 'characters', ids.character), { appliedDeliveryIds: ['delivery-1'], updatedAt: 2 });
  batch.update(doc(db, 'campaignDeliveries', 'delivery-1'), { state: 'ACCEPTED', updatedAt: 2 });
  await assertSucceeds(batch.commit());
  await assertFails(updateDoc(doc(db, 'campaignDeliveries', 'delivery-1'), { state: 'ACCEPTED', updatedAt: 3 }));
});

beforeEach(async () => {
  await env.clearFirestore();
});

after(async () => {
  await env.cleanup();
});

test('active campaign members can read campaign and linked character', async () => {
  await seed();
  const db = env.authenticatedContext(ids.player).firestore();
  await assertSucceeds(getDoc(doc(db, 'campaigns', ids.campaign)));
  await assertSucceeds(getDoc(doc(db, 'characters', ids.character)));
});

test('campaign authority allows listing linked characters', async () => {
  await seed();
  for (const uid of [ids.player, ids.owner]) {
    const db = env.authenticatedContext(uid).firestore();
    await assertSucceeds(getDocs(query(
      collection(db, 'characters'),
      where('campaignId', '==', ids.campaign),
    )));
  }
});

test('outsiders cannot read campaign or linked character', async () => {
  await seed();
  const db = env.authenticatedContext(ids.outsider).firestore();
  await assertFails(getDoc(doc(db, 'campaigns', ids.campaign)));
  await assertFails(getDoc(doc(db, 'characters', ids.character)));
});

test('authenticated usable invite can be previewed but revoked invite cannot', async () => {
  await seed();
  const db = env.authenticatedContext(ids.joiner).firestore();
  await assertSucceeds(getDoc(doc(db, 'campaignInvites', ids.invite)));

  await env.withSecurityRulesDisabled(async (context) => {
    await updateDoc(doc(context.firestore(), 'campaignInvites', ids.invite), { revokedAt: 2 });
  });
  await assertFails(getDoc(doc(db, 'campaignInvites', ids.invite)));
});

test('invite allows authenticated player to create only their own active membership', async () => {
  await seed();
  const db = env.authenticatedContext(ids.joiner).firestore();
  await assertSucceeds(setDoc(doc(db, 'campaignMembers', `${ids.campaign}::${ids.joiner}`), {
    campaignId: ids.campaign,
    userId: ids.joiner,
    role: 'PLAYER',
    state: 'ACTIVE',
    joinedAt: 3,
    updatedAt: 3,
    characterIds: [],
    joinedByInviteId: ids.invite,
  }));
  await assertFails(setDoc(doc(db, 'campaignMembers', `${ids.campaign}::${ids.outsider}`), {
    campaignId: ids.campaign,
    userId: ids.outsider,
    role: 'PLAYER',
    state: 'ACTIVE',
    joinedAt: 3,
    updatedAt: 3,
    characterIds: [],
    joinedByInviteId: ids.invite,
  }));
});

test('player can edit their own character but another active member cannot', async () => {
  await seed();
  const playerDb = env.authenticatedContext(ids.player).firestore();
  const outsiderDb = env.authenticatedContext(ids.joiner).firestore();
  await assertSucceeds(updateDoc(doc(playerDb, 'characters', ids.character), {
    name: 'Atualizado pelo jogador',
    updatedAt: 2,
  }));
  await assertFails(updateDoc(doc(outsiderDb, 'characters', ids.character), {
    name: 'Não permitido',
    updatedAt: 3,
  }));
});

test('player can synchronize links for their own active campaign membership', async () => {
  await seed();
  const playerDb = env.authenticatedContext(ids.player).firestore();
  const otherMemberDb = env.authenticatedContext(ids.historian).firestore();
  const membership = doc(playerDb, 'campaignMembers', `${ids.campaign}::${ids.player}`);

  await assertSucceeds(updateDoc(membership, {
    characterIds: [ids.character, 'second-character'],
    updatedAt: 2,
  }));
  await assertFails(updateDoc(doc(otherMemberDb, 'campaignMembers', `${ids.campaign}::${ids.player}`), {
    characterIds: [],
    updatedAt: 3,
  }));
});

test('campaign owner can edit linked characters while campaign is active', async () => {
  await seed();
  const db = env.authenticatedContext(ids.owner).firestore();
  await assertSucceeds(updateDoc(doc(db, 'characters', ids.character), {
    name: 'Atualizado pela mestre',
    updatedAt: 2,
  }));
});

test('a legacy contextual historian is only a player and cannot edit another player sheet', async () => {
  await seed();
  const db = env.authenticatedContext(ids.historian).firestore();
  await assertFails(updateDoc(doc(db, 'characters', ids.character), {
    name: 'Atualizado pela historiadora',
    updatedAt: 2,
  }));
  await assertFails(updateDoc(doc(db, 'campaignMembers', `${ids.campaign}::${ids.player}`), {
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
  await assertSucceeds(getDocs(query(collection(emptyDb, 'campaignMembers'), where('userId', '==', 'account-with-no-data'))));
  await assertSucceeds(getDocs(query(collection(emptyDb, 'characters'), where('ownerId', '==', 'account-with-no-data'))));
});

test('campaign creator remains the only master and ownership or roles cannot be transferred', async () => {
  await seed();
  const db = env.authenticatedContext(ids.owner).firestore();
  await assertFails(updateDoc(doc(db, 'campaigns', ids.campaign), { ownerId: ids.historian, updatedAt: 3 }));
  await assertFails(updateDoc(doc(db, 'campaignMembers', `${ids.campaign}::${ids.player}`), {
    role: 'HISTORIAN', updatedAt: 3,
  }));
});

test('campaign master can create a linked sheet assigned to an active player', async () => {
  await seed();
  const db = env.authenticatedContext(ids.owner).firestore();
  await assertSucceeds(setDoc(doc(db, 'characters', 'assigned-by-master'), {
    id: 'assigned-by-master', ownerId: ids.player, campaignId: ids.campaign,
    name: 'Ficha atribuída', lockType: 'NONE', isLocked: false,
    lockedBy: '', lockedAt: null, deleted: false, updatedAt: 2,
  }));
  await assertFails(setDoc(doc(db, 'characters', 'assigned-outsider'), {
    id: 'assigned-outsider', ownerId: ids.outsider, campaignId: ids.campaign,
    name: 'Inválida', lockType: 'NONE', isLocked: false,
    lockedBy: '', lockedAt: null, deleted: false, updatedAt: 2,
  }));
});

test('player can check a missing character UUID and publish a newly created sheet with protocol v1 revisions', async () => {
  await seed();
  const unauthDb = env.unauthenticatedContext().firestore();
  const db = env.authenticatedContext(ids.player).firestore();
  const reference = doc(db, 'characters', 'new-player-character');

  await assertFails(getDoc(doc(unauthDb, 'characters', 'new-player-character')));
  await assertSucceeds(getDoc(reference));
  await assertSucceeds(setDoc(reference, {
    id: 'new-player-character',
    ownerId: ids.player,
    campaignId: ids.campaign,
    name: 'Nova personagem',
    lockType: 'NONE',
    isLocked: false,
    lockedBy: '',
    lockedAt: null,
    deleted: false,
    appliedDeliveryIds: [],
    fieldUpdatedAt: { '@protocol': 1, '@revision': 1 },
    updatedAt: 2,
  }));
  await assertFails(updateDoc(reference, {
    name: 'Revisão repetida inválida',
    fieldUpdatedAt: { '@protocol': 1, '@revision': 1 },
    updatedAt: 3,
  }));
  await assertSucceeds(updateDoc(reference, {
    name: 'Revisão incrementada válida',
    fieldUpdatedAt: { '@protocol': 1, '@revision': 2 },
    updatedAt: 3,
  }));
});

test('sheet owner can unlink and delete their own locked sheet even when campaign is archived', async () => {
  await seed({ archived: true });
  await env.withSecurityRulesDisabled(async (context) => {
    await updateDoc(doc(context.firestore(), 'characters', ids.character), {
      lockType: 'HISTORIAN', isLocked: true, lockedBy: ids.owner, lockedAt: 2,
    });
  });
  const db = env.authenticatedContext(ids.player).firestore();
  await assertSucceeds(updateDoc(doc(db, 'characters', ids.character), {
    campaignId: '', updatedAt: 3,
  }));
  await assertSucceeds(deleteDoc(doc(db, 'characters', ids.character)));
});

test('campaign master may edit and unlink but may not delete another player sheet', async () => {
  await seed();
  const db = env.authenticatedContext(ids.owner).firestore();
  await assertSucceeds(updateDoc(doc(db, 'characters', ids.character), { name: 'Edição da Mestre', updatedAt: 2 }));
  await assertSucceeds(updateDoc(doc(db, 'characters', ids.character), { campaignId: '', updatedAt: 3 }));
  await assertFails(deleteDoc(doc(db, 'characters', ids.character)));
});

test('campaign master cannot alter sheet data while unlinking another player sheet', async () => {
  await seed();
  const db = env.authenticatedContext(ids.owner).firestore();
  await assertFails(updateDoc(doc(db, 'characters', ids.character), {
    campaignId: '', name: 'Alterada durante desvinculação', updatedAt: 3,
  }));
});

test('campaign master unlink may stamp lastEditedBy and drop legacy projections', async () => {
  await seed();
  await env.withSecurityRulesDisabled(async (context) => {
    await updateDoc(doc(context.firestore(), 'characters', ids.character), {
      dirty: false, notes: '', protections: {}, bodyRegions: [], organs: [], conditions: [],
    });
  });
  const db = env.authenticatedContext(ids.owner).firestore();
  await assertSucceeds(updateDoc(doc(db, 'characters', ids.character), {
    campaignId: '', updatedAt: 3, lastEditedBy: ids.owner,
    dirty: deleteField(), notes: deleteField(), protections: deleteField(),
    bodyRegions: deleteField(), organs: deleteField(), conditions: deleteField(),
  }));
});

test('campaign master unlink cannot rewrite legacy projections', async () => {
  await seed();
  const db = env.authenticatedContext(ids.owner).firestore();
  await assertFails(updateDoc(doc(db, 'characters', ids.character), {
    campaignId: '', updatedAt: 3, notes: 'injected by unlink',
  }));
});

test('audit operation can be created without a forbidden read of a missing document', async () => {
  await seed();
  const db = env.authenticatedContext(ids.player).firestore();
  await assertSucceeds(setDoc(doc(db, 'campaignAudit', 'operation-1'), {
    idempotencyKey: 'operation-1', campaignId: ids.campaign, characterId: ids.character,
    actorId: ids.player, type: 'RESOURCE', createdAt: 2,
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
    email: 'other@example.com', email_verified: true,
  }).firestore();
  await assertFails(getDoc(doc(legacyMasterDb, 'users', ids.player)));
  await assertFails(updateDoc(doc(legacyMasterDb, 'users', ids.player), { displayName: 'Invadido' }));

  const selfDb = env.authenticatedContext('new-user').firestore();
  await assertFails(setDoc(doc(selfDb, 'users', 'new-user'), { displayName: 'Nova', role: 'MASTER' }));
  await assertSucceeds(setDoc(doc(selfDb, 'users', 'new-user'), { displayName: 'Nova', role: 'USER' }));
  await assertFails(updateDoc(doc(selfDb, 'users', 'new-user'), { role: 'MASTER' }));
});

test('administrator requires the configured verified auth identity', async () => {
  await env.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), 'users', ids.player), { role: 'USER' });
  });
  const verified = env.authenticatedContext('admin', {
    email: 'kindbarros@gmail.com', email_verified: true,
  }).firestore();
  const unverified = env.authenticatedContext('lookalike', {
    email: 'kindbarros@gmail.com', email_verified: false,
  }).firestore();
  await assertSucceeds(getDoc(doc(verified, 'users', ids.player)));
  await assertFails(getDoc(doc(unverified, 'users', ids.player)));
});

test('administrator can list every character and manage records from other accounts', async () => {
  await seed();
  const adminDb = env.authenticatedContext('admin', {
    email: 'kindbarros@gmail.com', email_verified: true,
  }).firestore();
  const outsiderDb = env.authenticatedContext(ids.outsider).firestore();

  await assertSucceeds(getDocs(collection(adminDb, 'characters')));
  await assertSucceeds(getDocs(collection(adminDb, 'campaigns')));
  await assertSucceeds(getDocs(collection(adminDb, 'campaignInvites')));
  await assertSucceeds(setDoc(doc(adminDb, 'campaignInvites', 'ADMIN123'), {
    campaignId: ids.campaign,
    campaignName: 'Teste',
    campaignDescription: 'Campanha de teste',
    code: 'ADMIN123',
    createdBy: 'admin',
    createdAt: 2,
    expiresAt: null,
    revokedAt: null,
    generation: 1,
  }));
  await assertFails(updateDoc(doc(adminDb, 'campaignInvites', ids.invite), {
    revokedAt: 2,
  }));
  await assertSucceeds(updateDoc(doc(adminDb, 'characters', ids.character), {
    name: 'Atualizado pelo admin',
    updatedAt: 2,
  }));
  await assertSucceeds(deleteDoc(doc(adminDb, 'characters', ids.character)));

  await seed();
  await assertFails(getDocs(collection(outsiderDb, 'characters')));
});

test('archived campaign is read-only for characters', async () => {
  await seed({ archived: true });
  const ownerDb = env.authenticatedContext(ids.owner).firestore();
  const playerDb = env.authenticatedContext(ids.player).firestore();
  await assertSucceeds(getDoc(doc(playerDb, 'characters', ids.character)));
  await assertFails(updateDoc(doc(ownerDb, 'characters', ids.character), {
    name: 'Bloqueado',
    updatedAt: 2,
  }));
  await assertFails(updateDoc(doc(playerDb, 'characters', ids.character), {
    name: 'Bloqueado também',
    updatedAt: 2,
  }));
});

test('campaign owner can dismantle and delete an archived campaign safely', async () => {
  await seed({ archived: true });
  const ownerDb = env.authenticatedContext(ids.owner).firestore();

  const batch = writeBatch(ownerDb);
  batch.update(doc(ownerDb, 'characters', ids.character), { campaignId: '', updatedAt: 3 });
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
  const historianDb = env.authenticatedContext(ids.historian).firestore();
  const adminDb = env.authenticatedContext('admin-uid', { email: 'kindbarros@gmail.com', email_verified: true }).firestore();
  const journalQuery = (db) => query(collection(db, 'campaignJournal'), where('campaignId', '==', ids.campaign));

  // Query works even when empty for admin, GM, and active player, and denies outsider
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

  // Forged author or oversized title fails
  await assertFails(setDoc(entryRef(playerDb), { ...baseEntry, authorId: ids.owner }));
  await assertFails(setDoc(entryRef(playerDb), { ...baseEntry, title: 'x'.repeat(121) }));
  await assertFails(setDoc(entryRef(outsiderDb), { ...baseEntry, authorId: ids.outsider, lastEditorId: ids.outsider }));

  // Valid create succeeds
  await assertSucceeds(setDoc(entryRef(playerDb), baseEntry));
  await assertSucceeds(getDoc(entryRef(ownerDb)));
  await assertSucceeds(getDoc(entryRef(historianDb)));
  await assertSucceeds(getDoc(entryRef(adminDb)));
  await assertSucceeds(getDocs(journalQuery(adminDb)));
  await assertSucceeds(getDocs(journalQuery(ownerDb)));
  await assertSucceeds(getDocs(journalQuery(playerDb)));
  await assertFails(getDoc(entryRef(outsiderDb)));
  await assertFails(getDocs(journalQuery(outsiderDb)));

  // Another member (even with HISTORIAN member enum) cannot edit/delete someone else's entry
  await assertFails(updateDoc(entryRef(historianDb), {
    title: 'Alterado',
    updatedAt: 11,
    revision: 2,
    lastEditorId: ids.historian,
  }));

  // Author can edit with revision + 1, stale revision fails
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

  // GM (campaign owner) moderates while preserving authorId and createdAt; forging authorId fails
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

  // Physical delete is always denied
  await assertFails(deleteDoc(entryRef(ownerDb)));

  // Archived campaign blocks writes while keeping reads
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
  await assertFails(getDoc(doc(db, 'campaignInvites', ids.invite)));
});


test('administrator can delete an active campaign owned by another account', async () => {
  await seed();
  const db = env.authenticatedContext('admin', {
    email: 'kindbarros@gmail.com', email_verified: true,
  }).firestore();
  const batch = writeBatch(db);
  batch.update(doc(db, 'characters', ids.character), { campaignId: '', updatedAt: 3 });
  batch.update(doc(db, 'campaigns', ids.campaign), { state: 'DELETED', updatedAt: 3 });
  await assertSucceeds(batch.commit());
  const preserved = await assertSucceeds(getDoc(doc(db, 'characters', ids.character)));
  if (preserved.data().campaignId !== '') throw new Error('Character must survive detached');
});


test('player can rejoin after leaving with a valid invite without changing original join time', async () => {
  await seed();
  const db = env.authenticatedContext(ids.player).firestore();
  const reference = doc(db, 'campaignMembers', `${ids.campaign}::${ids.player}`);
  await assertSucceeds(updateDoc(reference, { state: 'LEFT', characterIds: [], updatedAt: 2 }));
  await assertSucceeds(updateDoc(reference, {
    state: 'ACTIVE', joinedByInviteId: ids.invite, updatedAt: 3,
  }));
  await assertSucceeds(getDoc(doc(db, 'campaigns', ids.campaign)));
});

test('GM linking an orphan campaign sheet atomically enrolls its owner and restores player access', async () => {
  await seed();
  await env.withSecurityRulesDisabled(async context => {
    await deleteDoc(doc(context.firestore(), 'campaignMembers', `${ids.campaign}::${ids.player}`));
  });
  const db = env.authenticatedContext(ids.owner).firestore();
  await assertSucceeds(runTransaction(db, async transaction => {
    const sheet = doc(db, 'characters', ids.character);
    const member = doc(db, 'campaignMembers', `${ids.campaign}::${ids.player}`);
    const snapshot = await transaction.get(sheet);
    await transaction.get(member);
    transaction.set(member, {
      campaignId: ids.campaign, userId: ids.player, role: 'PLAYER', state: 'ACTIVE',
      joinedAt: 3, updatedAt: 3, characterIds: [ids.character], joinedByInviteId: '',
    });
    transaction.set(sheet, {
      ...snapshot.data(), campaignId: ids.campaign, updatedAt: 3,
      fieldUpdatedAt: { '@protocol': 1, '@revision': 1 },
    });
  }));
  const playerDb = env.authenticatedContext(ids.player).firestore();
  await assertSucceeds(getDoc(doc(playerDb, 'campaigns', ids.campaign)));
  await assertSucceeds(getDocs(query(collection(playerDb, 'characters'), where('campaignId', '==', ids.campaign))));
  await assertSucceeds(updateDoc(doc(playerDb, 'characters', ids.character), {
    name: 'Saved by owner', fieldUpdatedAt: { '@protocol': 1, '@revision': 2 },
  }));
});


test('first invite join is confirmed transactionally and retry preserves existing character links', async () => {
  await seed();
  const db = env.authenticatedContext(ids.joiner).firestore();
  const reference = doc(db, 'campaignMembers', `${ids.campaign}::${ids.joiner}`);
  async function join() {
    return runTransaction(db, async transaction => {
      await transaction.get(doc(db, 'campaignInvites', ids.invite));
      const member = await transaction.get(reference);
      if (member.exists() && member.data().state === 'ACTIVE') return member.data();
      const joined = {
        campaignId: ids.campaign, userId: ids.joiner, role: 'PLAYER', state: 'ACTIVE',
        joinedAt: 3, updatedAt: 3, characterIds: [], joinedByInviteId: ids.invite,
      };
      transaction.set(reference, joined);
      return joined;
    });
  }
  await assertSucceeds(join());
  await assertSucceeds(getDoc(doc(db, 'campaigns', ids.campaign)));
  await assertSucceeds(updateDoc(reference, { characterIds: ['own-sheet'], updatedAt: 4 }));
  const retried = await assertSucceeds(join());
  if (retried.characterIds[0] !== 'own-sheet' || retried.joinedAt !== 3) {
    throw new Error('Retry reset existing membership');
  }
});

test('invite rejoin rejects revoked, expired, unrelated, archived and removed memberships', async () => {
  await seed();
  const db = env.authenticatedContext(ids.player).firestore();
  const reference = doc(db, 'campaignMembers', `${ids.campaign}::${ids.player}`);
  await assertSucceeds(updateDoc(reference, { state: 'LEFT', characterIds: [], updatedAt: 2 }));
  const rejoin = () => updateDoc(reference, { state: 'ACTIVE', updatedAt: 3 });
  for (const fields of [{ revokedAt: 2 }, { expiresAt: 1 }, { campaignId: 'other-campaign' }]) {
    await env.withSecurityRulesDisabled(async context => {
      await updateDoc(doc(context.firestore(), 'campaignInvites', ids.invite), fields);
    });
    await assertFails(rejoin());
    await seed();
    await assertSucceeds(updateDoc(reference, { state: 'LEFT', characterIds: [], updatedAt: 2 }));
  }
  await env.withSecurityRulesDisabled(async context => {
    await updateDoc(doc(context.firestore(), 'campaigns', ids.campaign), { state: 'ARCHIVED' });
  });
  await assertFails(rejoin());
  await seed();
  await env.withSecurityRulesDisabled(async context => {
    await updateDoc(doc(context.firestore(), 'campaignMembers', `${ids.campaign}::${ids.player}`), {
      state: 'REMOVED', characterIds: [],
    });
  });
  await assertFails(rejoin());
});

test('GM enrollment only grants player membership and requires canonical document identity', async () => {
  await seed();
  const db = env.authenticatedContext(ids.owner).firestore();
  const fields = {
    campaignId: ids.campaign, userId: ids.outsider, role: 'PLAYER', state: 'ACTIVE',
    joinedAt: 3, updatedAt: 3, characterIds: [], joinedByInviteId: '',
  };
  await assertFails(setDoc(doc(db, 'campaignMembers', 'forged-id'), fields));
  const member = doc(db, 'campaignMembers', `${ids.campaign}::${ids.outsider}`);
  await assertFails(setDoc(member, { ...fields, role: 'HISTORIAN' }));
  const playerDb = env.authenticatedContext(ids.player).firestore();
  await assertFails(setDoc(doc(playerDb, 'campaignMembers', `${ids.campaign}::${ids.outsider}`), fields));
  await assertSucceeds(setDoc(member, fields));
});

test('denied sheet link rolls back enrollment and preserves existing sheet data', async () => {
  await seed();
  const db = env.authenticatedContext(ids.owner).firestore();
  const member = doc(db, 'campaignMembers', `${ids.campaign}::${ids.outsider}`);
  const batch = writeBatch(db);
  batch.set(member, {
    campaignId: ids.campaign, userId: ids.outsider, role: 'PLAYER', state: 'ACTIVE',
    joinedAt: 3, updatedAt: 3, characterIds: [ids.character], joinedByInviteId: '',
  });
  batch.update(doc(db, 'characters', ids.character), { ownerId: ids.outsider });
  await assertFails(batch.commit());
  const membership = await getDoc(member);
  if (membership.exists()) throw new Error('Denied link left partial membership');
  const sheet = await getDoc(doc(db, 'characters', ids.character));
  if (sheet.data().ownerId !== ids.player || sheet.data().name !== 'Personagem') {
    throw new Error('Denied link changed sheet data');
  }
});

test('administrator links a personal sheet and enrolls its owner in the same transaction', async () => {
  await seed();
  await env.withSecurityRulesDisabled(async context => {
    await updateDoc(doc(context.firestore(), 'characters', ids.character), { campaignId: '' });
    await deleteDoc(doc(context.firestore(), 'campaignMembers', `${ids.campaign}::${ids.player}`));
  });
  const db = env.authenticatedContext('admin-uid', {
    email: 'kindbarros@gmail.com', email_verified: true,
  }).firestore();
  const batch = writeBatch(db);
  batch.set(doc(db, 'campaignMembers', `${ids.campaign}::${ids.player}`), {
    campaignId: ids.campaign, userId: ids.player, role: 'PLAYER', state: 'ACTIVE',
    joinedAt: 3, updatedAt: 3, characterIds: [ids.character], joinedByInviteId: '',
  });
  batch.update(doc(db, 'characters', ids.character), {
    campaignId: ids.campaign, fieldUpdatedAt: { '@protocol': 1, '@revision': 1 },
  });
  await assertSucceeds(batch.commit());
  const playerDb = env.authenticatedContext(ids.player).firestore();
  await assertSucceeds(getDoc(doc(playerDb, 'campaigns', ids.campaign)));
  await assertSucceeds(updateDoc(doc(playerDb, 'characters', ids.character), {
    name: 'Owner edit', fieldUpdatedAt: { '@protocol': 1, '@revision': 2 },
  }));
});
