import { readFile } from 'node:fs/promises';
import { after, before, test } from 'node:test';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { ref, get, set, serverTimestamp } from 'firebase/database';
let environment;
before(async () => {
  environment = await initializeTestEnvironment({ projectId: 'sdo-companion-test', database: { host: '127.0.0.1', port: 9000, rules: await readFile(new URL('../database.rules.json', import.meta.url), 'utf8') } });
  await environment.withSecurityRulesDisabled(async context => set(ref(context.database(), 'sheetAggregates/sheet'), { control: { permissions: { owner: { read: true, write: true }, gm: { read: true, write: true } } }, state: { ownerId: 'owner' } }));
});
after(async () => environment?.cleanup());
test('owner and GM read a sheet, stranger and anonymous cannot', async () => {
  for (const uid of ['owner', 'gm']) await assertSucceeds(get(ref(environment.authenticatedContext(uid).database(), 'sheetAggregates/sheet/state')));
  await assertFails(get(ref(environment.authenticatedContext('stranger').database(), 'sheetAggregates/sheet/state')));
  await assertFails(get(ref(environment.unauthenticatedContext().database(), 'sheetAggregates/sheet/state')));
});
test('even owner cannot write authoritative state, ACL or root', async () => {
  const db = environment.authenticatedContext('owner').database();
  for (const path of ['sheetAggregates/sheet/state', 'sheetAggregates/sheet/control', 'sheetAggregates/sheet/receipts/op', 'sheetAggregates']) await assertFails(set(ref(db, path), { granted: true }));
});
test('presence belongs to connection actor and has bounded schema', async () => {
  const db = environment.authenticatedContext('owner').database();
  await assertSucceeds(set(ref(db, 'presence/sheet/owner/connection'), { fieldKey: 'name', timestamp: serverTimestamp() }));
  await assertFails(set(ref(db, 'presence/sheet/gm/connection'), { fieldKey: 'name', timestamp: serverTimestamp() }));
  await assertFails(set(ref(db, 'presence/sheet/owner/bad'), { fieldKey: 'name', timestamp: serverTimestamp(), role: 'ADMIN' }));
  await assertSucceeds(set(ref(db, 'presence/sheet/owner/connection'), null));
});
