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
  doc,
  getDoc,
  getDocs,
  query,
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

test('campaign master may edit but may not unlink or delete another player sheet', async () => {
  await seed();
  const db = env.authenticatedContext(ids.owner).firestore();
  await assertSucceeds(updateDoc(doc(db, 'characters', ids.character), { name: 'Edição da Mestre', updatedAt: 2 }));
  await assertFails(updateDoc(doc(db, 'characters', ids.character), { campaignId: '', updatedAt: 3 }));
  await assertFails(deleteDoc(doc(db, 'characters', ids.character)));
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
