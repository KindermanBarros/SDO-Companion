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
  doc,
  getDoc,
  setDoc,
  updateDoc,
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
      role: 'MASTER',
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
