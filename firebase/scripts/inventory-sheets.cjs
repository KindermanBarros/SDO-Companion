// Read-only inventory using the locally authenticated Firebase CLI account.
const base = '../node_modules/firebase-tools/lib/';
const auth = require(base + 'auth');
async function main() {
  const options = { project: 'sdo-companion', nonInteractive: true };
  const account = auth.getGlobalDefaultAccount();
  if (account) auth.setActiveAccount(options, account);
  await require(base + 'requireAuth').requireAuth(options);
  const { Client } = require(base + 'apiv2');
  const client = new Client({ urlPrefix: 'https://firestore.googleapis.com', apiVersion: 'v1' });
  const { buildAggregate, encodeState, decodeState } = await import('../functions/src/migration.js');
  const { digest } = await import('../functions/src/protocol.js');
  function decode(value) {
    if ('mapValue' in value) return Object.fromEntries(Object.entries(value.mapValue.fields ?? {}).map(([key, item]) => [key, decode(item)]));
    if ('arrayValue' in value) return (value.arrayValue.values ?? []).map(decode);
    if ('integerValue' in value) return Number(value.integerValue);
    if ('nullValue' in value) return null;
    return Object.values(value)[0];
  }
  const summary = { discovered: 0, verified: 0, failed: 0, deleted: 0, standalone: 0, campaignLinked: 0 };
  const campaigns = new Map();
  let pageToken;
  do {
    const response = await client.request({ method: 'GET', path: '/projects/sdo-companion/databases/(default)/documents/characters', queryParams: { pageSize: 100, ...(pageToken ? { pageToken } : {}) } });
    for (const document of response.body.documents ?? []) {
      summary.discovered++;
      try {
        const record = decode({ mapValue: { fields: document.fields } });
        const id = document.name.split('/').at(-1);
        const campaignId = record.campaignId && record.campaignId !== 'default' ? record.campaignId : '';
        if (campaignId) {
          summary.campaignLinked++;
          if (!campaigns.has(campaignId)) {
            const result = await client.request({ method: 'GET', path: `/projects/sdo-companion/databases/(default)/documents/campaigns/${encodeURIComponent(campaignId)}` });
            campaigns.set(campaignId, decode({ mapValue: { fields: result.body.fields } }));
          }
        } else summary.standalone++;
        if (record.deleted) summary.deleted++;
        buildAggregate(id, record, campaigns.get(campaignId), 0);
        const expected = { ...record }; delete expected.dirty; delete expected.lastSyncedAt;
        if (digest(decodeState(encodeState(record))) !== digest(expected)) throw new Error('Round-trip mismatch.');
        summary.verified++;
      } catch (error) { summary.failed++; console.error(`Conversion failure: ${error.message}`); }
    }
    pageToken = response.body.nextPageToken;
  } while (pageToken);
  console.log(JSON.stringify(summary, null, 2));
  if (summary.failed) process.exitCode = 1;
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });
