import { digest, safeId } from './protocol.js';

export function encodeState(record) {
  const state = structuredClone(record);
  delete state.dirty; delete state.lastSyncedAt;
  if (state.fieldUpdatedAt && typeof state.fieldUpdatedAt !== 'string') state.fieldUpdatedAt = JSON.stringify(state.fieldUpdatedAt);
  return state;
}
// RTDB omits empty maps, empty arrays and nulls; compare their wire-normalized form.
export function rtdbValue(value) {
  if (value === null || value === undefined) return null;
  if (Array.isArray(value)) return value.length ? value.map(rtdbValue) : null;
  if (typeof value === 'object') {
    const entries = Object.entries(value).map(([key, item]) => [key, rtdbValue(item)]).filter(([, item]) => item !== null);
    return entries.length ? Object.fromEntries(entries) : null;
  }
  return value;
}
export function decodeState(state) {
  const record = structuredClone(state);
  if (typeof record.fieldUpdatedAt === 'string') record.fieldUpdatedAt = JSON.parse(record.fieldUpdatedAt);
  return record;
}
export function buildAggregate(id, record, campaign, now) {
  if (!safeId(id) || !safeId(record.ownerId)) throw new Error('Invalid sheet ID or missing owner.');
  const campaignId = record.campaignId && record.campaignId !== 'default' ? record.campaignId : '';
  if (campaignId && (!campaign || !safeId(campaign.ownerId))) throw new Error('Campaign link cannot be verified.');
  const permissions = { [record.ownerId]: { read: true, write: true, manage: true } };
  if (campaignId) permissions[campaign.ownerId] = { read: true, write: true, manage: true };
  const state = encodeState({ ...record, id });
  return {
    control: { ownerUid: record.ownerId, campaignId, permissions, accessEpoch: 1, campaignState: campaign?.state ?? 'ACTIVE', writeGate: 'CLOSED', schemaVersion: 1, minimumProtocolVersion: 1 },
    state,
    sync: { revision: 0 },
    migration: { sourceHash: digest(state), version: 1, createdAt: now },
  };
}

export async function migrateAll({ firestore, database, dryRun = true, pageSize = 100, onRecord = () => {} }) {
  const summary = { discovered: 0, verified: 0, failed: 0, migrated: 0 };
  let cursor;
  for (;;) {
    let query = firestore.collection('characters').orderBy('__name__').limit(pageSize);
    if (cursor) query = query.startAfter(cursor);
    const page = await query.get();
    if (page.empty) break;
    for (const doc of page.docs) {
      summary.discovered++;
      const manifest = firestore.collection('sheetMigration').doc(doc.id);
      try {
        let source = doc.data();
        const campaignId = source.campaignId && source.campaignId !== 'default' ? source.campaignId : '';
        const campaign = campaignId ? (await firestore.collection('campaigns').doc(campaignId).get()).data() : null;
        if (!dryRun) {
          const route = (await firestore.collection('sheetRoutes').doc(doc.id).get()).data();
          if (route?.status === 'RTDB_ACTIVE') {
            const existing = (await database.ref(`sheetAggregates/${doc.id}`).get()).val();
            const prior = (await manifest.get()).data();
            if (!existing?.state || prior?.status !== 'VERIFIED') throw new Error('Active route is missing verified migration state.');
            summary.migrated++; summary.verified++;
            onRecord({ id: doc.id, status: 'ALREADY_MIGRATED' });
            continue;
          }
          // The deployed Firestore rules consult this server-owned fence before every legacy write.
          await firestore.collection('sheetRoutes').doc(doc.id).set({ status: 'MIGRATING', accessEpoch: 1 }, { merge: true });
          source = (await doc.ref.get()).data();
          await manifest.set({ status: 'MIGRATING', source, startedAt: Date.now() }, { merge: true });
        }
        const aggregate = buildAggregate(doc.id, source, campaign, Date.now());
        if (!dryRun) {
          const reference = database.ref(`sheetAggregates/${doc.id}`);
          const existing = (await reference.get()).val();
          if (existing?.control?.writeGate === 'OPEN') {
            const prior = (await manifest.get()).data();
            if (prior?.status !== 'VERIFIED') throw new Error('Existing live destination has no verified migration.');
          } else {
            await reference.transaction(current => current ?? aggregate);
            const actual = (await reference.child('state').get()).val();
            if (digest(rtdbValue(actual)) !== digest(rtdbValue(aggregate.state))) throw new Error('Destination semantic checksum differs.');
            await manifest.set({ status: 'VERIFIED', sourceHash: digest(aggregate.state), verifiedAt: Date.now() }, { merge: true });
            const updates = { [`sheetAggregates/${doc.id}/control/writeGate`]: 'OPEN' };
            for (const uid of Object.keys(aggregate.control.permissions)) updates[`userSheets/${uid}/${doc.id}`] = true;
            await database.ref().update(updates);
          }
          await firestore.collection('sheetRoutes').doc(doc.id).set({ status: 'RTDB_ACTIVE', accessEpoch: 1 }, { merge: true });
          summary.migrated++;
        }
        summary.verified++;
        onRecord({ id: doc.id, status: dryRun ? 'DRY_RUN_VERIFIED' : 'VERIFIED', checksum: aggregate.migration.sourceHash });
      } catch (error) {
        summary.failed++;
        if (!dryRun) await manifest.set({ status: 'FAILED', error: error.message, failedAt: Date.now() }, { merge: true });
        onRecord({ id: doc.id, status: 'FAILED', error: error.message });
      }
    }
    cursor = page.docs.at(-1);
  }
  return summary;
}

export async function rollbackSheet({ firestore, database, sheetId }) {
  const reference = database.ref(`sheetAggregates/${sheetId}`);
  await reference.child('control/writeGate').set('CLOSED');
  const aggregate = (await reference.get()).val();
  if (!aggregate?.state) throw new Error('Missing RTDB sheet; refusing rollback.');
  await firestore.collection('sheetRoutes').doc(sheetId).set({ status: 'MIGRATING' }, { merge: true });
  await firestore.collection('characters').doc(sheetId).set(decodeState(aggregate.state));
  const copied = encodeState((await firestore.collection('characters').doc(sheetId).get()).data());
  if (digest(rtdbValue(copied)) !== digest(rtdbValue(aggregate.state))) throw new Error('Rollback verification failed.');
  await firestore.collection('sheetRoutes').doc(sheetId).set({ status: 'FIRESTORE_ACTIVE' }, { merge: true });
  return { sheetId, status: 'ROLLED_BACK', revision: aggregate.sync.revision };
}
