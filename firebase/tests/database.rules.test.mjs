import { readFile } from 'node:fs/promises';
import { after, before, beforeEach, test } from 'node:test';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { ref, get, set, update } from 'firebase/database';

let environment;

before(async () => {
  environment = await initializeTestEnvironment({
    projectId: 'sdo-companion-test',
    database: {
      host: '127.0.0.1',
      port: 9000,
      rules: await readFile(new URL('../database.rules.json', import.meta.url), 'utf8'),
    },
  });
});

beforeEach(async () => {
  await environment.clearDatabase();
  await environment.withSecurityRulesDisabled(async (context) => {
    const db = context.database();
    await set(ref(db, 'sheetAggregates/sheet-1'), {
      control: { permissions: { owner: { read: true, write: true } } },
      state: { ownerId: 'owner' },
    });
    await set(ref(db, 'userSheets/owner/sheet-1'), { id: 'sheet-1' });
    await set(ref(db, 'presence/sheet-1/owner/conn-1'), { fieldKey: 'name' });
    await set(ref(db, 'grid/camp-1/grid-1'), {
      acl: {
        gm: { read: true, write: true, manage: true },
        player: { read: true, write: true, manage: false },
        viewer: { read: true, write: false, manage: false },
      },
      state: { round: 1 },
    });
  });
});

after(async () => environment?.cleanup());

test('retired RTDB sheetAggregates, userSheets, presence, and root are strictly denied', async () => {
  const ownerDb = environment.authenticatedContext('owner').database();
  const gmDb = environment.authenticatedContext('gm').database();
  const anonDb = environment.unauthenticatedContext().database();

  for (const path of [
    'sheetAggregates/sheet-1/state',
    'sheetAggregates/sheet-1/control',
    'userSheets/owner/sheet-1',
    'presence/sheet-1/owner/conn-1',
    'otherRoot/anything',
  ]) {
    await assertFails(get(ref(ownerDb, path)));
    await assertFails(set(ref(ownerDb, path), { hacked: true }));
    await assertFails(get(ref(gmDb, path)));
    await assertFails(get(ref(anonDb, path)));
  }
});

test('isolated RTDB grid enforces RTDB-only acl for read, write, and manage', async () => {
  const gmDb = environment.authenticatedContext('gm').database();
  const playerDb = environment.authenticatedContext('player').database();
  const viewerDb = environment.authenticatedContext('viewer').database();
  const outsiderDb = environment.authenticatedContext('outsider').database();
  const anonDb = environment.unauthenticatedContext().database();

  await assertSucceeds(get(ref(gmDb, 'grid/camp-1/grid-1')));
  await assertSucceeds(get(ref(playerDb, 'grid/camp-1/grid-1')));
  await assertSucceeds(get(ref(viewerDb, 'grid/camp-1/grid-1')));
  await assertFails(get(ref(outsiderDb, 'grid/camp-1/grid-1')));
  await assertFails(get(ref(anonDb, 'grid/camp-1/grid-1')));

  // Player with write:true can update grid state/tokens but cannot escalate acl
  await assertSucceeds(set(ref(playerDb, 'grid/camp-1/grid-1/state'), { round: 2 }));
  await assertFails(set(ref(playerDb, 'grid/camp-1/grid-1/acl/player'), {
    read: true, write: true, manage: true,
  }));
  await assertFails(set(ref(playerDb, 'grid/camp-1/grid-1/acl/outsider'), {
    read: true, write: true, manage: false,
  }));

  // Viewer with read-only cannot write state
  await assertFails(set(ref(viewerDb, 'grid/camp-1/grid-1/state'), { round: 99 }));

  // Manager can update acl with valid schema, but extra/invalid ACL fields are rejected
  await assertSucceeds(set(ref(gmDb, 'grid/camp-1/grid-1/acl/outsider'), {
    read: true, write: false, manage: false,
  }));
  await assertFails(set(ref(gmDb, 'grid/camp-1/grid-1/acl/bad'), {
    read: true, admin: true,
  }));
  await assertSucceeds(get(ref(outsiderDb, 'grid/camp-1/grid-1')));
});

test('manager can create a new isolated grid and cannot overwrite an existing grid root', async () => {
  const gmDb = environment.authenticatedContext('gm').database();
  const outsiderDb = environment.authenticatedContext('outsider').database();

  await assertSucceeds(set(ref(gmDb, 'grid/camp-1/grid-2'), {
    acl: {
      gm: { read: true, write: true, manage: true },
    },
    state: { round: 1 },
  }));

  // Outsider cannot create grid pretending someone else is manager
  await assertFails(set(ref(outsiderDb, 'grid/camp-1/grid-3'), {
    acl: {
      gm: { read: true, write: true, manage: true },
    },
  }));

  // Existing grid root cannot be wiped wholesale via parent set, and non-manager cannot update acl
  await assertFails(set(ref(gmDb, 'grid/camp-1/grid-1'), { state: { wiped: true } }));
  const playerDb = environment.authenticatedContext('player').database();
  await assertFails(update(ref(playerDb, 'grid/camp-1/grid-1'), {
    'acl/player/manage': true,
    'state/round': 3,
  }));
});
