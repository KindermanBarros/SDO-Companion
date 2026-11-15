import { createHash } from 'node:crypto';

export class CommandError extends Error {
  constructor(code, message) { super(message); this.code = code; }
}
export const fail = (code, message) => { throw new CommandError(code, message); };
export function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical);
  if (value && typeof value === 'object') return Object.fromEntries(Object.keys(value).sort().map(key => [key, canonical(value[key])]));
  return value;
}
export const digest = value => createHash('sha256').update(JSON.stringify(canonical(value))).digest('hex');
export const safeId = value => typeof value === 'string' && /^[A-Za-z0-9_-]{1,128}$/.test(value);
const forbidden = new Set(['id', 'ownerId', 'campaignId', 'deleted', 'isLocked', 'lockType', 'lockedBy', 'lockedAt', 'fieldUpdatedAt', 'updatedAt', 'lastEditedBy', 'dirty', 'lastSyncedAt', 'appliedDeliveryIds']);
const fields = new Set('name race subRace raceAttribute occupation age level creationStatus creationStep creationCompletedAt creationRulesVersion itemSchemaVersion canonicalSchemaVersion migrationReviewPayload canonicalAbilitiesPayload canonicalBodyPayload canonicalConditionsPayload activeModifiersPayload canonicalItemsPayload scopedItemCatalogPayload canonicalProgressionPayload progressionLifeBonus progressionSanityBonus progressionArcaneBonus progressionEnergyBonus progressionHistory money life sanity arcane energy destiny exhaustion corruption attributes protections protectionAdjustments positiveTraits negativeTraits pathName pathMotto powers inventory itemCreationDraft story notes personalNotes height sex size photoUri learnedKnowledges arcaneKnowledges battleTechniques pathKeywords pathPillars containerCapacity bodyRegions agilityLimit organs mysticAbilities conditions'.split(' '));
const equal = (a, b) => JSON.stringify(canonical(a)) === JSON.stringify(canonical(b));
const elementId = item => item?.id ?? item?.instanceId?.value ?? item?.instanceId ?? item?.acronym ?? item?.region ?? item?.organ;
export function mergeIntent(remote, base, edited) {
  if (equal(base, edited)) return remote;
  if (Array.isArray(base) && Array.isArray(edited) && Array.isArray(remote)) {
    const keyed = [...base, ...edited, ...remote].every(item => typeof elementId(item) === 'string');
    if (!keyed) return edited;
    for (const list of [base, edited, remote]) if (new Set(list.map(elementId)).size !== list.length) fail('invalid-argument', 'Duplicate element IDs.');
    const before = new Map(base.map(item => [elementId(item), item]));
    const wanted = new Map(edited.map(item => [elementId(item), item]));
    const result = remote.filter(item => !before.has(elementId(item)) || wanted.has(elementId(item))).map(item => {
      const id = elementId(item);
      return before.has(id) && wanted.has(id) ? mergeIntent(item, before.get(id), wanted.get(id)) : item;
    });
    for (const item of edited) if (!before.has(elementId(item)) && !remote.some(existing => elementId(existing) === elementId(item))) result.push(item);
    return result;
  }
  if (remote && base && edited && !Array.isArray(base) && typeof base === 'object' && typeof edited === 'object' && typeof remote === 'object') {
    const result = structuredClone(remote);
    for (const key of new Set([...Object.keys(base), ...Object.keys(edited)])) {
      if (equal(base[key], edited[key])) continue;
      if (!(key in edited)) delete result[key];
      else if (key in base && !(key in remote)) continue;
      else result[key] = mergeIntent(remote[key], base[key], edited[key]);
    }
    return result;
  }
  return edited;
}
export function validateEnvelope(envelope) {
  if (!envelope || envelope.protocolVersion !== 1) fail('failed-precondition', 'Unsupported protocol version.');
  for (const key of ['sheetId', 'operationId', 'deviceId']) if (!safeId(envelope[key])) fail('invalid-argument', `Invalid ${key}.`);
  for (const key of ['sequence', 'baseRevision', 'accessEpoch']) if (!Number.isSafeInteger(envelope[key]) || envelope[key] < (key === 'sequence' ? 1 : 0)) fail('invalid-argument', `Invalid ${key}.`);
  if (!envelope.command || JSON.stringify(envelope).length > 500000) fail('invalid-argument', 'Invalid or oversized command.');
}
function validateJson(value, depth = 0) {
  if (depth > 32) fail('invalid-argument', 'Nested payload is too deep.');
  if (typeof value === 'number' && !Number.isFinite(value)) fail('invalid-argument', 'Invalid number.');
  if (value && typeof value === 'object') for (const [key, item] of Object.entries(value)) {
    if (['__proto__', 'constructor', 'prototype'].includes(key)) fail('invalid-argument', 'Invalid property.');
    validateJson(item, depth + 1);
  }
}
function validateState(state) {
  for (const key of ['money', 'level', 'containerCapacity', 'progressionLifeBonus', 'progressionSanityBonus', 'progressionArcaneBonus', 'progressionEnergyBonus']) {
    if (state[key] !== undefined && (!Number.isSafeInteger(state[key]) || state[key] < 0 || state[key] > 2147483647)) fail('invalid-argument', `Invalid ${key}.`);
  }
  for (const key of ['life', 'sanity', 'arcane', 'energy', 'destiny', 'exhaustion', 'corruption']) {
    if (state[key] === undefined) continue;
    if (!state[key] || typeof state[key] !== 'object' || Array.isArray(state[key])) fail('invalid-argument', `Invalid ${key}.`);
    for (const [field, value] of Object.entries(state[key])) if (!['current', 'maximum', 'adjustment'].includes(field) || !Number.isSafeInteger(value) || Math.abs(value) > 2147483647 || (field !== 'adjustment' && value < 0)) fail('invalid-argument', `Invalid ${key}.${field}.`);
  }
  for (const item of state.inventory ?? []) for (const key of ['quantity', 'loadedAmmo', 'chargesCurrent', 'doses']) if (item[key] !== undefined && (!Number.isSafeInteger(item[key]) || item[key] < 0)) fail('invalid-argument', `Invalid inventory ${key}.`);
}
export function reduceCommand(state, command, context) {
  const next = structuredClone(state);
  const payload = command.payload ?? {};
  validateJson(payload);
  if (command.type === 'SESSION') {
    if (['RESOURCE', 'HEAL', 'DESTINY', 'MONEY'].includes(payload.type)) {
      const resource = payload.type === 'MONEY' ? 'money' : payload.resource ?? (payload.type === 'DESTINY' ? 'destiny' : 'life');
      return reduceCommand(state, { type: 'ADJUST_RESOURCE', payload: { resource, delta: payload.amount } }, context);
    }
    if (payload.type === 'NOTE') next.personalNotes = [...(next.personalNotes ?? []), { id: context.operationId, title: payload.label || 'Registro', text: payload.detail ?? '' }];
    else if (payload.type === 'CONDITION_ADD') {
      if (!payload.condition?.kind) fail('invalid-argument', 'Typed condition is required.');
      const conditions = JSON.parse(next.canonicalConditionsPayload || '[]');
      conditions.push({ instanceId: context.operationId, payload: payload.condition, source: { type: 'Narrative', id: context.operationId }, appliedAt: context.now });
      next.canonicalConditionsPayload = JSON.stringify(conditions);
    } else if (payload.type === 'CONDITION_REMOVE') {
      const id = payload.conditionId?.value ?? payload.conditionId ?? payload.targetId;
      const conditions = JSON.parse(next.canonicalConditionsPayload || '[]');
      next.canonicalConditionsPayload = JSON.stringify(conditions.filter(item => elementId(item) !== id));
    } else if (!['NOTE', 'USAGE_RESET', 'REWARD'].includes(payload.type)) fail('failed-precondition', 'This session operation requires the canonical server reducer and is not enabled yet.');
  } else if (command.type === 'SET_LOCK') {
    if (!['NONE', 'PLAYER', 'HISTORIAN'].includes(payload.lockType)) fail('invalid-argument', 'Invalid lock type.');
    next.lockType = payload.lockType; next.isLocked = payload.lockType !== 'NONE';
    next.lockedBy = next.isLocked ? context.uid : ''; next.lockedAt = next.isLocked ? context.now : null;
  } else if (command.type === 'PATCH') {
    if (!payload.changes || Array.isArray(payload.changes)) fail('invalid-argument', 'Changes must be a map.');
    for (const [field, value] of Object.entries(payload.changes)) {
      if (forbidden.has(field) || !fields.has(field)) fail('permission-denied', `Field cannot be edited: ${field}`);
      if (value === null) delete next[field];
      else if (field.endsWith('Payload') && payload.base?.[field] && state[field] && typeof value === 'string') {
        try { next[field] = JSON.stringify(mergeIntent(JSON.parse(state[field]), JSON.parse(payload.base[field]), JSON.parse(value))); }
        catch (error) { if (error instanceof CommandError) throw error; fail('invalid-argument', 'Invalid canonical payload.'); }
      } else next[field] = mergeIntent(state[field], payload.base?.[field], value);
    }
  } else if (command.type === 'ADJUST_RESOURCE') {
    const key = String(payload.resource).toLowerCase();
    if (!['money', 'life', 'sanity', 'arcane', 'energy', 'destiny', 'exhaustion', 'corruption'].includes(key)) fail('invalid-argument', 'Unknown resource.');
    if (!Number.isSafeInteger(payload.delta)) fail('invalid-argument', 'Invalid delta.');
    const before = key === 'money' ? next.money ?? 0 : next[key]?.current ?? 0;
    const after = before + payload.delta;
    if (!Number.isSafeInteger(after) || after < 0 || after > 2147483647) fail('failed-precondition', 'Insufficient resource or numeric overflow.');
    if (key === 'money') next.money = after; else next[key] = { ...next[key], current: after };
  } else if (command.type === 'DELETE') {
    next.deleted = true;
  } else if (command.type === 'TRANSFER_OWNERSHIP') {
    if (!safeId(payload.ownerId)) fail('invalid-argument', 'Invalid owner.');
    next.ownerId = payload.ownerId;
  } else fail('invalid-argument', `Unsupported command: ${command.type}`);
  validateState(next);
  next.updatedAt = context.now;
  next.lastEditedBy = context.uid;
  return next;
}
export function executeCommand(aggregate, envelope, uid, now) {
  validateEnvelope(envelope);
  if (!aggregate) fail('not-found', 'Sheet does not exist.');
  const hash = digest(envelope);
  const existing = aggregate.receipts?.[envelope.operationId];
  if (existing) {
    if (existing.actorUid !== uid || existing.payloadHash !== hash) fail('already-exists', 'Operation ID has different contents.');
    return aggregate;
  }
  const control = aggregate.control;
  if (!control?.permissions?.[uid]?.write || control.writeGate !== 'OPEN' || control.accessEpoch !== envelope.accessEpoch) fail('permission-denied', 'Sheet access changed or writes are closed.');
  if (aggregate.state.deleted) fail('failed-precondition', 'Sheet is deleted.');
  if (control.campaignId && control.campaignState !== 'ACTIVE') fail('failed-precondition', 'Campaign is inactive.');
  if (aggregate.state.isLocked && aggregate.state.lockedBy !== uid && !control.permissions[uid].manage) fail('permission-denied', 'Sheet is locked.');
  const stream = digest([uid, envelope.deviceId]);
  const watermark = aggregate.sync?.devices?.[stream] ?? 0;
  if (envelope.sequence !== watermark + 1) fail('failed-precondition', 'Device sequence must be contiguous.');
  const next = structuredClone(aggregate);
  const revision = (aggregate.sync?.revision ?? 0) + 1;
  if (!Number.isSafeInteger(revision)) fail('resource-exhausted', 'Revision overflow.');
  const receipt = { actorUid: uid, payloadHash: hash, deviceId: envelope.deviceId, sequence: envelope.sequence, outcome: 'COMMITTED', committedRevision: revision, timestamp: now };
  try {
    if (envelope.command.type === 'PATCH') {
      const guarded = ['money', 'life', 'sanity', 'arcane', 'energy', 'destiny', 'exhaustion', 'corruption', 'inventory', 'canonicalItemsPayload', 'canonicalAbilitiesPayload', 'activeModifiersPayload'];
      if (Object.keys(envelope.command.payload?.changes ?? {}).some(key => guarded.includes(key)) && envelope.baseRevision !== (aggregate.sync?.revision ?? 0)) fail('failed-precondition', 'Refresh the sheet before assigning resource or item state.');
    }
    if (['DELETE', 'SET_LOCK', 'TRANSFER_OWNERSHIP'].includes(envelope.command.type) && !control.permissions[uid].manage) fail('permission-denied', 'Manage permission is required.');
    if (envelope.command.type === 'TRANSFER_OWNERSHIP' && control.ownerUid !== uid) fail('permission-denied', 'Only the owner can transfer a sheet.');
    next.state = reduceCommand(aggregate.state, envelope.command, { uid, now, operationId: envelope.operationId });
    if (envelope.command.type === 'TRANSFER_OWNERSHIP') {
      delete next.control.permissions[control.ownerUid];
      next.control.ownerUid = next.state.ownerId;
      next.control.permissions[next.state.ownerId] = { read: true, write: true, manage: true };
      next.control.accessEpoch++;
    }
  }
  catch (error) {
    if (!(error instanceof CommandError)) throw error;
    receipt.outcome = 'REJECTED'; receipt.error = { code: error.code, message: error.message };
  }
  next.sync = { ...next.sync, revision, devices: { ...next.sync?.devices, [stream]: envelope.sequence } };
  const metadata = typeof next.state.fieldUpdatedAt === 'string' ? JSON.parse(next.state.fieldUpdatedAt) : next.state.fieldUpdatedAt ?? {};
  next.state.fieldUpdatedAt = JSON.stringify({ ...metadata, '@revision': revision });
  next.receipts = { ...next.receipts, [envelope.operationId]: receipt };
  next.events = { ...next.events, [envelope.operationId]: { actorUid: uid, sheetId: envelope.sheetId, campaignId: control.campaignId ?? '', revision, timestamp: now, type: envelope.command.type, outcome: receipt.outcome } };
  return next;
}
