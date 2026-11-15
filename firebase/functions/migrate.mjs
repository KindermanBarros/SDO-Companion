import { initializeApp, applicationDefault } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import { getDatabase } from 'firebase-admin/database';
import { migrateAll, rollbackSheet } from './src/migration.js';
const args = process.argv.slice(2);
const project = args.find(value => value.startsWith('--project='))?.split('=')[1];
if (!project) throw new Error('Specify --project=PROJECT_ID explicitly. Defaults to dry run; --execute requires deployed fences.');
initializeApp({ credential: applicationDefault(), projectId: project, databaseURL: `https://${project}-default-rtdb.firebaseio.com` });
const clients = { firestore: getFirestore(), database: getDatabase() };
const rollback = args.find(value => value.startsWith('--rollback='))?.split('=')[1];
if (rollback) {
  if (!args.includes('--execute')) throw new Error('Rollback requires --execute.');
  console.log(JSON.stringify(await rollbackSheet({ ...clients, sheetId: rollback })));
} else {
  const result = await migrateAll({ ...clients, dryRun: !args.includes('--execute'), onRecord: item => console.log(JSON.stringify(item)) });
  console.log(JSON.stringify(result));
  if (result.failed) process.exitCode = 1;
}
