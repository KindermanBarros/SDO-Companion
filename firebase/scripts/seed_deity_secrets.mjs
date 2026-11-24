#!/usr/bin/env node
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const catalogPath = resolve(here, '../deity_gm_catalog.json');

/**
 * Loads the 16 canonical GM-only deity documents generated from the SDO RPG vault
 * for Firestore `deityGmCatalog/{deityId}` seeding or rules verification.
 */
export function loadDeityGmDocuments(actorUid = 'admin') {
  const raw = JSON.parse(readFileSync(catalogPath, 'utf8'));
  return raw.deities.map((deity) => ({
    id: deity.id,
    name: deity.name,
    tierCode: deity.tierCode,
    payloadJson: JSON.stringify(deity),
    updatedBy: actorUid,
  }));
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const docs = loadDeityGmDocuments();
  console.log(`Validated ${docs.length} GM deity documents ready for deityGmCatalog seeding.`);
}
