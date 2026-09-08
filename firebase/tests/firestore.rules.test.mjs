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
      role: 'HISTORIAN',
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

test('contextual historian can edit linked characters but cannot perform responsible operations', async () => {
  await seed();
  const db = env.authenticatedContext(ids.historian).firestore();
  await assertSucceeds(updateDoc(doc(db, 'characters', ids.character), {
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

test('responsibility transfer is accepted atomically and cannot target a player', async () => {
  await seed();
  const db = env.authenticatedContext(ids.owner).firestore();
  const batch = writeBatch(db);
  batch.update(doc(db, 'campaignMembers', `${ids.campaign}::${ids.historian}`), { role: 'HISTORIAN', updatedAt: 3 });
  batch.update(doc(db, 'campaignMembers', `${ids.campaign}::${ids.owner}`), { role: 'PLAYER', updatedAt: 3 });
  batch.update(doc(db, 'campaigns', ids.campaign), { ownerId: ids.historian, updatedAt: 3 });
  await assertSucceeds(batch.commit());

  await seed();
  const invalid = writeBatch(db);
  invalid.update(doc(db, 'campaignMembers', `${ids.campaign}::${ids.owner}`), { role: 'PLAYER', updatedAt: 4 });
  invalid.update(doc(db, 'campaigns', ids.campaign), { ownerId: ids.player, updatedAt: 4 });
  await assertFails(invalid.commit());
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
  await assertSucceeds(updateDoc(doc(adminDb, 'campaignInvites', ids.invite), {
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

test('unauthenticated users cannot preview invites', async () => {
  await seed();
  const db = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db, 'campaignInvites', ids.invite)));
});
