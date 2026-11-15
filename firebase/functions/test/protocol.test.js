import test from 'node:test';
import assert from 'node:assert/strict';
import { executeCommand, mergeIntent } from '../src/protocol.js';
import { buildAggregate, decodeState, encodeState } from '../src/migration.js';

const sheet = () => ({ control: { permissions: { alice: { read: true, write: true, manage: true } }, accessEpoch: 1, writeGate: 'OPEN' }, state: { id: 'sheet', ownerId: 'alice', money: 5 }, sync: { revision: 0 } });
const command = (overrides = {}) => ({ protocolVersion: 1, sheetId: 'sheet', operationId: 'op1', deviceId: 'device', sequence: 1, baseRevision: 0, accessEpoch: 1, command: { type: 'ADJUST_RESOURCE', payload: { resource: 'money', delta: -3 } }, ...overrides });
test('retries do not spend twice and state/receipt/event share a revision', () => {
  const result = executeCommand(sheet(), command(), 'alice', 10);
  assert.equal(result.state.money, 2);
  assert.deepEqual(executeCommand(result, command(), 'alice', 11), result);
  assert.equal(result.receipts.op1.committedRevision, result.events.op1.revision);
  assert.equal(JSON.parse(result.state.fieldUpdatedAt)['@revision'], 1);
});
test('second spend is rejected atomically and consumes its sequence', () => {
  const first = executeCommand(sheet(), command(), 'alice', 10);
  const second = executeCommand(first, command({ operationId: 'op2', sequence: 2 }), 'alice', 11);
  assert.equal(second.state.money, 2);
  assert.equal(second.receipts.op2.outcome, 'REJECTED');
  const third = executeCommand(second, command({ operationId: 'op3', sequence: 3, command: { type: 'ADJUST_RESOURCE', payload: { resource: 'money', delta: 1 } } }), 'alice', 12);
  assert.equal(third.state.money, 3);
});
test('access, epoch, sequence and operation hash are enforced', () => {
  assert.throws(() => executeCommand(sheet(), command(), 'stranger', 0), /access/);
  assert.throws(() => executeCommand(sheet(), command({ accessEpoch: 2 }), 'alice', 0), /access/);
  assert.throws(() => executeCommand(sheet(), command({ sequence: 2 }), 'alice', 0), /contiguous/);
  const first = executeCommand(sheet(), command(), 'alice', 0);
  assert.throws(() => executeCommand(first, command({ baseRevision: 2 }), 'alice', 0), /different contents/);
});
test('independent element properties merge and removed elements stay removed', () => {
  const base = [{ id: 'a', name: 'A', quantity: 1 }, { id: 'b', name: 'B' }];
  const remote = [{ id: 'a', name: 'Remote', quantity: 1 }];
  const edited = [{ id: 'a', name: 'A', quantity: 2 }, { id: 'b', name: 'Edited' }];
  assert.deepEqual(mergeIntent(remote, base, edited), [{ id: 'a', name: 'Remote', quantity: 2 }]);
});
test('migration preserves all record fields and refuses unresolved ownership', () => {
  const record = { ownerId: 'alice', fieldUpdatedAt: { '@operation/foo': 1 }, photoUri: 'data:photo', canonicalItemsPayload: '[1]', deleted: true };
  assert.deepEqual(decodeState(encodeState(record)), record);
  const aggregate = buildAggregate('sheet', record, null, 0);
  assert.equal(aggregate.state.deleted, true);
  assert.equal(aggregate.control.writeGate, 'CLOSED');
  assert.throws(() => buildAggregate('sheet', { ownerId: '' }, null, 0));
  assert.throws(() => buildAggregate('sheet', { ownerId: 'alice', campaignId: 'missing' }, null, 0));
});
