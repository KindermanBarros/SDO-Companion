import { initializeApp } from 'firebase-admin/app';
import { getDatabase } from 'firebase-admin/database';
import { getFirestore } from 'firebase-admin/firestore';
import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { onValueCreated } from 'firebase-functions/v2/database';
import { CommandError, executeCommand, validateEnvelope } from './protocol.js';

initializeApp();
export const submitSheetCommand = onCall({ region: 'us-central1', maxInstances: 20 }, async request => {
  if (!request.auth) throw new HttpsError('unauthenticated', 'Sign in before syncing.');
  try {
    validateEnvelope(request.data);
    const envelope = request.data;
    if (envelope.command.type === 'TRANSFER_OWNERSHIP') {
      const ownerId = envelope.command.payload?.ownerId;
      if (typeof ownerId !== 'string' || !/^[A-Za-z0-9_-]{1,128}$/.test(ownerId) || !(await getFirestore().collection('users').doc(ownerId).get()).exists) throw new CommandError('invalid-argument', 'New owner does not exist.');
    }
    const reference = getDatabase().ref(`sheetAggregates/${envelope.sheetId}`);
    const now = Date.now();
    const result = await reference.transaction(current => executeCommand(current, envelope, request.auth.uid, now), undefined, false);
    const aggregate = result.snapshot.val();
    return { receipt: aggregate.receipts[envelope.operationId], state: aggregate.state, revision: aggregate.sync.revision };
  } catch (error) {
    if (error instanceof CommandError) throw new HttpsError(error.code, error.message);
    throw new HttpsError('internal', 'Sheet command failed; retry the same operation ID.');
  }
});

export async function projectEvent(sheetId, eventId, event, firestore = getFirestore()) {
  if (!event.campaignId) return;
  const reference = firestore.collection('campaignAudit').doc(`${sheetId}_${eventId}`);
  await firestore.runTransaction(async transaction => {
    if ((await transaction.get(reference)).exists) return;
    transaction.create(reference, { ...event, id: reference.id, characterId: sheetId, actorId: event.actorUid, createdAt: event.timestamp });
  });
}
export const projectSheetAudit = onValueCreated({ ref: '/sheetAggregates/{sheetId}/events/{eventId}', region: 'us-central1', retry: true }, async event => {
  await projectEvent(event.params.sheetId, event.params.eventId, event.data.val());
  await event.data.ref.child('projected').set(true);
});
