// Read-only inventory using the locally authenticated Firebase CLI account.
const path = require('node:path');
const { pathToFileURL } = require('node:url');

const base = '../node_modules/firebase-tools/lib/';
const auth = require(base + 'auth');

function decodeFirestoreValue(value) {
  if ('mapValue' in value) {
    return Object.fromEntries(
      Object.entries(value.mapValue.fields ?? {}).map(([key, item]) => [key, decodeFirestoreValue(item)])
    );
  }
  if ('arrayValue' in value) return (value.arrayValue.values ?? []).map(decodeFirestoreValue);
  if ('integerValue' in value) return Number(value.integerValue);
  if ('doubleValue' in value) return Number(value.doubleValue);
  if ('nullValue' in value) return null;
  return Object.values(value)[0];
}

async function main() {
  const options = { project: 'sdo-companion', nonInteractive: true };
  const account = auth.getGlobalDefaultAccount();
  if (account) auth.setActiveAccount(options, account);
  await require(base + 'requireAuth').requireAuth(options);
  const { Client } = require(base + 'apiv2');
  const client = new Client({ urlPrefix: 'https://firestore.googleapis.com', apiVersion: 'v1' });

  const converterUrl = pathToFileURL(path.resolve(__dirname, '../migration/spark-converter.mjs')).href;
  const {
    convertLegacySheetToSparkV2,
    reconstructLegacyFromSparkV2,
    compareParity,
    convertLegacyMemberToSparkV2,
    convertLegacyInviteToSparkV2,
  } = await import(converterUrl);

  const summary = {
    discovered: 0,
    verified: 0,
    failed: 0,
    deleted: 0,
    standalone: 0,
    campaignLinked: 0,
    media: {
      totalWithPhotoUri: 0,
      dataUriWithin64KiB: 0,
      dataUriOver64KiB: 0,
      localOrExternalUri: 0,
      requiresLocalPreservation: 0,
      items: [],
    },
    campaignMembersDiscovered: 0,
    campaignInvitesDiscovered: 0,
  };

  const campaigns = new Map();
  let pageToken;
  do {
    const response = await client.request({
      method: 'GET',
      path: '/projects/sdo-companion/databases/(default)/documents/characters',
      queryParams: { pageSize: 100, ...(pageToken ? { pageToken } : {}) },
    });
    for (const document of response.body.documents ?? []) {
      summary.discovered++;
      try {
        const record = decodeFirestoreValue({ mapValue: { fields: document.fields } });
        const id = document.name.split('/').at(-1);
        const campaignId = record.campaignId && record.campaignId !== 'default' ? record.campaignId : '';
        if (campaignId) {
          summary.campaignLinked++;
          if (!campaigns.has(campaignId)) {
            const result = await client.request({
              method: 'GET',
              path: `/projects/sdo-companion/databases/(default)/documents/campaigns/${encodeURIComponent(campaignId)}`,
            });
            campaigns.set(campaignId, decodeFirestoreValue({ mapValue: { fields: result.body.fields } }));
          }
        } else {
          summary.standalone++;
        }
        if (record.deleted) summary.deleted++;

        const converted = convertLegacySheetToSparkV2(id, record, campaigns.get(campaignId));
        const reconstructed = reconstructLegacyFromSparkV2(converted);
        const parity = compareParity(record, reconstructed, {
          legacyMediaInventory: converted.legacyMediaInventory,
        });
        if (!parity.ok) {
          throw new Error(`Parity mismatch: ${JSON.stringify(parity.mismatches)}`);
        }

        const mediaInv = converted.legacyMediaInventory;
        if (mediaInv.hasMedia) {
          summary.media.totalWithPhotoUri++;
          if (mediaInv.migratedToThumbnail) summary.media.dataUriWithin64KiB++;
          if (mediaInv.reason === 'EXCEEDS_64KIB_LIMIT') summary.media.dataUriOver64KiB++;
          if (mediaInv.kind === 'LOCAL_URI' || mediaInv.kind === 'REMOTE_URL') summary.media.localOrExternalUri++;
          if (mediaInv.requiresLocalPreservation) summary.media.requiresLocalPreservation++;
          const { originalPhotoUri: _omitted, ...safeMeta } = mediaInv;
          summary.media.items.push(safeMeta);
        }

        summary.verified++;
      } catch (error) {
        summary.failed++;
        console.error(`Conversion failure: ${error.message}`);
      }
    }
    pageToken = response.body.nextPageToken;
  } while (pageToken);

  // Inventory legacy campaignMembers and campaignInvites
  let memberToken;
  do {
    const res = await client.request({
      method: 'GET',
      path: '/projects/sdo-companion/databases/(default)/documents/campaignMembers',
      queryParams: { pageSize: 100, ...(memberToken ? { pageToken: memberToken } : {}) },
    });
    for (const doc of res.body.documents ?? []) {
      summary.campaignMembersDiscovered++;
      const data = decodeFirestoreValue({ mapValue: { fields: doc.fields } });
      convertLegacyMemberToSparkV2(doc.name.split('/').at(-1), data);
    }
    memberToken = res.body.nextPageToken;
  } while (memberToken);

  let inviteToken;
  do {
    const res = await client.request({
      method: 'GET',
      path: '/projects/sdo-companion/databases/(default)/documents/campaignInvites',
      queryParams: { pageSize: 100, ...(inviteToken ? { pageToken: inviteToken } : {}) },
    });
    for (const doc of res.body.documents ?? []) {
      summary.campaignInvitesDiscovered++;
      const data = decodeFirestoreValue({ mapValue: { fields: doc.fields } });
      convertLegacyInviteToSparkV2(doc.name.split('/').at(-1), data);
    }
    inviteToken = res.body.nextPageToken;
  } while (inviteToken);

  console.log(JSON.stringify(summary, null, 2));
  if (summary.failed) process.exitCode = 1;
}

main().catch(error => {
  console.error(error.message);
  process.exitCode = 1;
});
