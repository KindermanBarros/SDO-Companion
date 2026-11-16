import { createHash } from 'node:crypto';

export const PROTOCOL_VERSION = 2;
export const SCHEMA_VERSION = 2;
export const MAX_INT_32 = 2147483647;
export const MAX_THUMBNAIL_BYTES = 65536;
export const MAX_THUMBNAIL_DIMENSION = 256;

export const SAFE_ID_RE = /^[A-Za-z0-9_-]{1,128}$/;
export const ITEM_ID_RE = /^(inv|istate|abil|pow|mys|cond|lcond|note|attr)_[A-Za-z0-9_.:=-]{1,128}$/;
export const SKILL_PROP_KEY_RE = /^skill_[A-Za-z0-9_.-]{1,64}_(value|modifier|name)$/;

export const LOCK_TYPES = new Set(['NONE', 'PLAYER', 'HISTORIAN']);
export const RESOURCE_KEYS = new Set(['life', 'sanity', 'arcane', 'energy', 'destiny', 'exhaustion', 'corruption']);
export const STRING_FIELD_KEYS = new Set([
  'name',
  'race',
  'subRace',
  'raceAttribute',
  'occupation',
  'height',
  'age',
  'sex',
  'size',
  'avatarPreset',
  'pathName',
  'pathMotto',
  'story',
  'agilityLimit',
  'creationStatus',
  'itemCreationDraft',
  'migrationReviewPayload',
  'canonicalBodyPayload',
  'activeModifiersPayload',
  'scopedItemCatalogPayload',
  'canonicalProgressionPayload',
]);
export const INT_FIELD_KEYS = new Set([
  'level',
  'money',
  'creationStep',
  'creationRulesVersion',
  'creationCompletedAt',
  'containerCapacity',
  'progressionLifeBonus',
  'progressionSanityBonus',
  'progressionArcaneBonus',
  'progressionEnergyBonus',
]);
export const LIST_FIELD_KEYS = new Set([
  'positiveTraits',
  'negativeTraits',
  'pathKeywords',
  'pathPillars',
  'learnedKnowledges',
  'arcaneKnowledges',
  'battleTechniques',
  'progressionHistory',
]);
export const MAP_FIELD_KEYS = new Set(['protectionAdjustments']);

export const ALL_FIELD_KEYS = new Set([
  ...STRING_FIELD_KEYS,
  ...INT_FIELD_KEYS,
  ...RESOURCE_KEYS,
  ...LIST_FIELD_KEYS,
  ...MAP_FIELD_KEYS,
]);

export const INV_FIELD_KEYS = new Set([
  'name',
  'state',
  'quantity',
  'durabilityCurrent',
  'durabilityMax',
  'itemCondition',
  'region',
  'effect',
  'load',
  'backpackCapacity',
  'agilityLimit',
  'quality',
  'pg',
  'pl',
  'category',
  'linkedAshId',
  'ashPurity',
  'enhancements',
  'mechanicalEffects',
  'meta',
]);

export const ISTATE_FIELD_KEYS = new Set([
  'definition',
  'scope',
  'material',
  'installations',
  'stack',
  'consumable',
  'ammo',
  'reload',
  'container',
]);

export const ABILITY_FIELD_KEYS = new Set([
  'name',
  'cost',
  'costType',
  'costValue',
  'action',
  'range',
  'duration',
  'effect',
  'active',
  'favorite',
  'available',
  'modifiers',
  'linkedItemId',
  'meta',
]);

export const CONDITION_FIELD_KEYS = new Set([
  'name',
  'intensity',
  'duration',
  'summary',
  'origin',
  'payload',
]);

export const NOTE_FIELD_KEYS = new Set(['title', 'text']);
export const ATTR_SCALAR_FIELD_KEYS = new Set(['name', 'acronym', 'value', 'modifier']);

export const STANDARD_ATTRIBUTE_ORDER = ['FOR', 'AGI', 'VIG', 'INT', 'PER', 'POD'];

export function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical);
  if (value && typeof value === 'object') {
    const out = {};
    for (const key of Object.keys(value).sort()) {
      const item = value[key];
      if (item !== undefined) out[key] = canonical(item);
    }
    return out;
  }
  return value;
}

export function digest(value) {
  return createHash('sha256').update(JSON.stringify(canonical(value))).digest('hex');
}

export function safeId(value) {
  return typeof value === 'string' && SAFE_ID_RE.test(value);
}

function toSafeInt(value, fallback = 0) {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return Math.trunc(value);
  }
  return fallback;
}

function normalizeCampaignId(raw) {
  if (typeof raw !== 'string') return '';
  return raw === 'default' ? '' : raw;
}

function makeItemId(prefix, rawId, index, usedIds, warnings) {
  const base = typeof rawId === 'string' && rawId.length > 0 ? rawId : `idx_${index}`;
  const sanitized = base.replace(/[^A-Za-z0-9_.:=-]/g, '_').slice(0, 120);
  let candidate = `${prefix}_${sanitized || `idx_${index}`}`;
  if (!ITEM_ID_RE.test(candidate)) {
    candidate = `${prefix}_h_${digest(base).slice(0, 16)}`;
  }
  if (sanitized !== base) {
    warnings.push(`Sanitized ${prefix} item ID "${base}" -> "${candidate}".`);
  }
  let unique = candidate;
  let suffix = 1;
  while (usedIds.has(unique)) {
    const tag = `_${suffix++}`;
    unique = `${candidate.slice(0, 128 - tag.length)}${tag}`;
  }
  usedIds.add(unique);
  return unique;
}

function makeSkillSlug(name, index, usedSlugs) {
  const ascii = String(name || `skill_${index}`)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^A-Za-z0-9_.-]+/g, '_')
    .replace(/^_+|_+$/g, '');
  const base = (ascii || `skill_${index}`).slice(0, 56);
  let candidate = base;
  let suffix = 1;
  while (
    usedSlugs.has(candidate) ||
    !SKILL_PROP_KEY_RE.test(`skill_${candidate}_value`)
  ) {
    const tag = `_${suffix++}`;
    candidate = `${(ascii || 'skill').slice(0, 56 - tag.length)}${tag}`;
  }
  usedSlugs.add(candidate);
  return candidate;
}

function parseJsonArraySafe(raw, label, warnings) {
  if (typeof raw !== 'string' || raw.trim() === '') return null;
  try {
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) return parsed;
    warnings.push(`${label} is not a JSON array; preserving raw payload.`);
    return null;
  } catch {
    warnings.push(`${label} contains invalid JSON; cannot unpack items.`);
    return null;
  }
}

function parseImageDimensions(buffer, mimeType) {
  if (!Buffer.isBuffer(buffer) || buffer.length < 24) return null;
  if (
    buffer[0] === 0x89 &&
    buffer[1] === 0x50 &&
    buffer[2] === 0x4e &&
    buffer[3] === 0x47
  ) {
    return {
      width: buffer.readUInt32BE(16),
      height: buffer.readUInt32BE(20),
    };
  }
  if (buffer.slice(0, 4).toString('ascii') === 'GIF8' && buffer.length >= 10) {
    return {
      width: buffer.readUInt16LE(6),
      height: buffer.readUInt16LE(8),
    };
  }
  if (buffer[0] === 0xff && buffer[1] === 0xd8) {
    let offset = 2;
    while (offset + 9 < buffer.length) {
      if (buffer[offset] !== 0xff) break;
      const marker = buffer[offset + 1];
      const len = buffer.readUInt16BE(offset + 2);
      if (marker === 0xc0 || marker === 0xc2) {
        return {
          height: buffer.readUInt16BE(offset + 5),
          width: buffer.readUInt16BE(offset + 7),
        };
      }
      if (len < 2) break;
      offset += 2 + len;
    }
  }
  if (buffer.slice(0, 4).toString('ascii') === 'RIFF' && buffer.slice(8, 12).toString('ascii') === 'WEBP') {
    const chunk = buffer.slice(12, 16).toString('ascii');
    if (chunk === 'VP8X' && buffer.length >= 30) {
      const w = 1 + buffer.readUIntLE(24, 3);
      const h = 1 + buffer.readUIntLE(27, 3);
      return { width: w, height: h };
    }
  }
  return mimeType ? null : null;
}

export function inspectLegacyMedia(charId, photoUri, { updatedBy, updatedAt, warnings = [] } = {}) {
  if (typeof photoUri !== 'string' || photoUri.trim() === '') {
    return {
      media: null,
      legacyMediaInventory: {
        charId,
        hasMedia: false,
        kind: 'NONE',
        mimeType: '',
        byteLength: 0,
        decodedBytes: 0,
        requiresLocalPreservation: false,
        migratedToThumbnail: false,
        sha256: null,
      },
    };
  }

  const byteLength = Buffer.byteLength(photoUri, 'utf8');
  const dataMatch = /^data:(image\/[a-zA-Z0-9+.-]+)(;base64)?,(.*)$/is.exec(photoUri);
  if (dataMatch) {
    const mimeType = dataMatch[1].toLowerCase();
    const isBase64 = Boolean(dataMatch[2]);
    const payload = dataMatch[3] ?? '';
    const decodedBuffer = isBase64
      ? Buffer.from(payload, 'base64')
      : Buffer.from(decodeURIComponent(payload), 'utf8');
    const sha256 = createHash('sha256').update(decodedBuffer).digest('hex');
    const dims = parseImageDimensions(decodedBuffer, mimeType);
    const width = dims?.width ?? MAX_THUMBNAIL_DIMENSION;
    const height = dims?.height ?? MAX_THUMBNAIL_DIMENSION;

    if (byteLength <= MAX_THUMBNAIL_BYTES) {
      if (width > MAX_THUMBNAIL_DIMENSION || height > MAX_THUMBNAIL_DIMENSION) {
        warnings.push(
          `photoUri dimensions ${width}x${height} exceed ${MAX_THUMBNAIL_DIMENSION}x${MAX_THUMBNAIL_DIMENSION}; keeping out of Firestore media/avatar and marking for local preservation.`
        );
        return {
          media: null,
          legacyMediaInventory: {
            charId,
            hasMedia: true,
            kind: 'DATA_URI',
            mimeType,
            byteLength,
            decodedBytes: decodedBuffer.length,
            width,
            height,
            requiresLocalPreservation: true,
            migratedToThumbnail: false,
            reason: 'EXCEEDS_256PX_DIMENSION',
            sha256,
            originalPhotoUri: photoUri,
          },
        };
      }
      return {
        media: {
          dataUri: photoUri,
          mimeType,
          width,
          height,
          updatedBy: updatedBy ?? '',
          updatedAt: updatedAt ?? 0,
        },
        legacyMediaInventory: {
          charId,
          hasMedia: true,
          kind: 'DATA_URI',
          mimeType,
          byteLength,
          decodedBytes: decodedBuffer.length,
          width,
          height,
          requiresLocalPreservation: false,
          migratedToThumbnail: true,
          sha256,
        },
      };
    }

    warnings.push(
      `photoUri (${byteLength} bytes) exceeds ${MAX_THUMBNAIL_BYTES} bytes Spark thumbnail limit; kept out of Firestore and flagged for local preservation.`
    );
    return {
      media: null,
      legacyMediaInventory: {
        charId,
        hasMedia: true,
        kind: 'DATA_URI',
        mimeType,
        byteLength,
        decodedBytes: decodedBuffer.length,
        width: dims?.width ?? null,
        height: dims?.height ?? null,
        requiresLocalPreservation: true,
        migratedToThumbnail: false,
        reason: 'EXCEEDS_64KIB_LIMIT',
        sha256,
        originalPhotoUri: photoUri,
      },
    };
  }

  const isRemote = /^https?:\/\//i.test(photoUri);
  warnings.push(
    `photoUri is a ${isRemote ? 'remote URL' : 'local device URI'}; excluded from Firestore media/avatar and recorded for local preservation.`
  );
  return {
    media: null,
    legacyMediaInventory: {
      charId,
      hasMedia: true,
      kind: isRemote ? 'REMOTE_URL' : 'LOCAL_URI',
      mimeType: '',
      byteLength,
      decodedBytes: 0,
      requiresLocalPreservation: true,
      migratedToThumbnail: false,
      reason: isRemote ? 'REMOTE_URL' : 'LOCAL_DEVICE_URI',
      sha256: createHash('sha256').update(photoUri, 'utf8').digest('hex'),
      originalPhotoUri: photoUri,
    },
  };
}

function makeFieldDoc(key, value, meta) {
  return {
    key,
    value: structuredClone(value),
    revision: meta.revision,
    lastOperationId: meta.lastOperationId,
    updatedBy: meta.updatedBy,
    updatedAt: meta.updatedAt,
  };
}

const INVENTORY_DIRECT_KEYS = new Set([
  'name',
  'state',
  'quantity',
  'durabilityCurrent',
  'durabilityMax',
  'itemCondition',
  'region',
  'effect',
  'load',
  'backpackCapacity',
  'agilityLimit',
  'quality',
  'pg',
  'pl',
  'category',
  'linkedAshId',
  'ashPurity',
  'mechanicalEffects',
]);

const INVENTORY_ENHANCEMENT_KEYS = new Set([
  'modificationIds',
  'gemIds',
  'technologyIds',
  'gemSlots',
  'technologySlots',
  'installedEnhancements',
  'enhancementSlots',
  'enhancementChargesCurrent',
  'enhancementChargesMax',
]);

function splitInventoryItemGranular(item) {
  const fields = {};
  const enhancements = {};
  const meta = {};

  for (const [k, v] of Object.entries(item)) {
    if (v === undefined) continue;
    if (INVENTORY_DIRECT_KEYS.has(k)) {
      if (k === 'quantity') {
        fields[k] = Math.max(0, toSafeInt(v, 0));
      } else if (['durabilityCurrent', 'durabilityMax', 'load', 'backpackCapacity', 'pg', 'pl'].includes(k)) {
        fields[k] = toSafeInt(v, 0);
      } else {
        fields[k] = structuredClone(v);
      }
    } else if (INVENTORY_ENHANCEMENT_KEYS.has(k)) {
      enhancements[k] = structuredClone(v);
    } else {
      meta[k] = structuredClone(v);
    }
  }

  if (Object.keys(enhancements).length > 0) {
    fields.enhancements = enhancements;
  }
  if (Object.keys(meta).length > 0) {
    fields.meta = meta;
  }
  return fields;
}

const ABILITY_DIRECT_KEYS = new Set([
  'name',
  'cost',
  'costType',
  'costValue',
  'action',
  'range',
  'duration',
  'effect',
  'active',
  'favorite',
  'available',
  'modifiers',
  'linkedItemId',
]);

function splitAbilityLikeGranular(entry) {
  const fields = {};
  const meta = {};
  for (const [k, v] of Object.entries(entry)) {
    if (v === undefined) continue;
    if (ABILITY_DIRECT_KEYS.has(k)) {
      fields[k] = structuredClone(v);
    } else {
      meta[k] = structuredClone(v);
    }
  }
  if (Object.keys(meta).length > 0) {
    fields.meta = meta;
  }
  return fields;
}

function splitItemStateGranular(entry) {
  const fields = {};
  for (const k of ISTATE_FIELD_KEYS) {
    if (k in entry && entry[k] !== undefined) {
      fields[k] = structuredClone(entry[k]);
    }
  }
  return fields;
}

function splitConditionInstanceGranular(entry) {
  const fields = {
    payload: structuredClone(entry),
  };
  if (typeof entry.payload?.intensity === 'number') {
    fields.intensity = entry.payload.intensity;
  }
  if (entry.payload?.duration !== undefined) {
    fields.duration = structuredClone(entry.payload.duration);
  }
  return fields;
}

function splitLegacyConditionGranular(entry, itemId) {
  const fields = {};
  for (const k of ['name', 'intensity', 'duration', 'summary', 'origin']) {
    if (k in entry && entry[k] !== undefined) {
      fields[k] = structuredClone(entry[k]);
    }
  }
  const extra = {};
  for (const [k, v] of Object.entries(entry)) {
    if (v === undefined) continue;
    if (!['name', 'intensity', 'duration', 'summary', 'origin'].includes(k)) {
      if (k === 'id' && itemId === `lcond_${v}`) continue;
      extra[k] = structuredClone(v);
    }
  }
  if (Object.keys(extra).length > 0) {
    fields.payload = extra;
  }
  return fields;
}

export function convertLegacySheetToSparkV2(charId, legacyDoc, campaignDoc, options = {}) {
  if (!safeId(charId)) {
    throw new Error(`Invalid sheet ID: ${charId}`);
  }
  if (!legacyDoc || typeof legacyDoc !== 'object') {
    throw new Error(`Missing legacy sheet document for ${charId}.`);
  }
  if (!safeId(legacyDoc.ownerId)) {
    throw new Error(`Missing or invalid ownerId on sheet ${charId}.`);
  }

  const campaignId = normalizeCampaignId(legacyDoc.campaignId);
  if (campaignId) {
    if (!safeId(campaignId)) {
      throw new Error(`Invalid campaignId on sheet ${charId}: ${campaignId}`);
    }
    if (!campaignDoc || !safeId(campaignDoc.ownerId)) {
      throw new Error('Campaign link cannot be verified.');
    }
    if (campaignDoc.id && campaignDoc.id !== campaignId) {
      throw new Error(`Campaign document ID mismatch: expected ${campaignId}, got ${campaignDoc.id}.`);
    }
  }

  const warnings = [];
  const revision = Number.isSafeInteger(options.revision) && options.revision >= 1 ? options.revision : 1;
  const lastOperationId = options.operationId ?? 'migration_v2_init';
  const updatedBy = options.actorUid ?? (legacyDoc.lastEditedBy || legacyDoc.ownerId);
  const updatedAt = options.timestamp ?? legacyDoc.updatedAt ?? 0;
  const meta = { revision, lastOperationId, updatedBy, updatedAt };

  let lockType = typeof legacyDoc.lockType === 'string' && LOCK_TYPES.has(legacyDoc.lockType)
    ? legacyDoc.lockType
    : 'NONE';
  if (lockType === 'NONE' && legacyDoc.isLocked === true) {
    lockType = 'HISTORIAN';
    warnings.push('Legacy sheet had isLocked=true with lockType=NONE; normalized to HISTORIAN.');
  }
  const isLocked = lockType !== 'NONE';
  const lockedBy = isLocked ? String(legacyDoc.lockedBy ?? '') : '';
  const lockedAt = isLocked ? (legacyDoc.lockedAt ?? null) : null;

  const root = {
    id: charId,
    ownerId: legacyDoc.ownerId,
    campaignId,
    authorizedCampaignId: String(legacyDoc.authorizedCampaignId ?? ''),
    deleted: Boolean(legacyDoc.deleted),
    schemaVersion: SCHEMA_VERSION,
    canonicalSchemaVersion: Math.max(0, toSafeInt(legacyDoc.canonicalSchemaVersion, 0)),
    itemSchemaVersion: Math.max(0, toSafeInt(legacyDoc.itemSchemaVersion, 0)),
    protocolVersion: PROTOCOL_VERSION,
    lockType,
    isLocked,
    lockedBy,
    lockedAt,
    revision,
    lastOperationId,
    updatedBy,
    updatedAt,
    appliedDeliveryIds: Array.isArray(legacyDoc.appliedDeliveryIds) ? [...legacyDoc.appliedDeliveryIds] : [],
  };

  const fields = {};
  for (const key of STRING_FIELD_KEYS) {
    if (legacyDoc[key] === undefined) continue;
    if (key === 'itemCreationDraft') {
      if (legacyDoc.itemCreationDraft === null) continue;
      const serialized = typeof legacyDoc.itemCreationDraft === 'string'
        ? legacyDoc.itemCreationDraft
        : JSON.stringify(legacyDoc.itemCreationDraft);
      fields[key] = makeFieldDoc(key, serialized, meta);
      continue;
    }
    fields[key] = makeFieldDoc(key, String(legacyDoc[key] ?? ''), meta);
  }

  for (const key of INT_FIELD_KEYS) {
    if (legacyDoc[key] === undefined || legacyDoc[key] === null) continue;
    let val = toSafeInt(legacyDoc[key], 0);
    if (key === 'level') {
      if (val < 1 || val > 20) {
        warnings.push(`Clamped out-of-range level ${val} -> ${Math.min(20, Math.max(1, val))}.`);
        val = Math.min(20, Math.max(1, val));
      }
    } else if (key === 'creationStep') {
      if (val < 1 || val > 20) {
        warnings.push(`Clamped out-of-range creationStep ${val} -> ${Math.min(20, Math.max(1, val))}.`);
        val = Math.min(20, Math.max(1, val));
      }
    } else if (key === 'creationRulesVersion') {
      if (val < 1) {
        warnings.push(`Normalized creationRulesVersion ${val} -> 1.`);
        val = 1;
      }
    } else if (key === 'creationCompletedAt') {
      val = Math.max(0, val);
    } else {
      val = Math.min(MAX_INT_32, Math.max(0, val));
    }
    fields[key] = makeFieldDoc(key, val, meta);
  }

  for (const key of RESOURCE_KEYS) {
    if (legacyDoc[key] === undefined || legacyDoc[key] === null) continue;
    const raw = legacyDoc[key];
    fields[key] = makeFieldDoc(
      key,
      {
        current: Math.max(0, toSafeInt(raw.current, 0)),
        maximum: Math.max(0, toSafeInt(raw.maximum, 0)),
        adjustment: toSafeInt(raw.adjustment, 0),
      },
      meta
    );
  }

  for (const key of LIST_FIELD_KEYS) {
    if (legacyDoc[key] === undefined || legacyDoc[key] === null) continue;
    const list = Array.isArray(legacyDoc[key]) ? legacyDoc[key] : [];
    if (list.length > 500) {
      warnings.push(`Field ${key} has ${list.length} items (exceeds Spark Rules limit of 500).`);
    }
    fields[key] = makeFieldDoc(key, list, meta);
  }

  for (const key of MAP_FIELD_KEYS) {
    if (legacyDoc[key] === undefined || legacyDoc[key] === null) continue;
    const mapVal = typeof legacyDoc[key] === 'object' && !Array.isArray(legacyDoc[key]) ? legacyDoc[key] : {};
    fields[key] = makeFieldDoc(key, mapVal, meta);
  }

  if (Array.isArray(legacyDoc.bodyRegions) && legacyDoc.bodyRegions.length > 0 && !legacyDoc.canonicalBodyPayload) {
    warnings.push('Legacy bodyRegions present without canonicalBodyPayload.');
  }
  if (Array.isArray(legacyDoc.organs) && legacyDoc.organs.length > 0 && !legacyDoc.canonicalBodyPayload) {
    warnings.push('Legacy organs present without canonicalBodyPayload.');
  }

  const items = {};
  const itemFields = {};
  const usedItemIds = new Set();

  const registerItem = (itemId, kind, fieldMap) => {
    items[itemId] = {
      id: itemId,
      kind,
      deleted: false,
      revision,
      lastOperationId,
      updatedBy,
      updatedAt,
    };
    itemFields[itemId] = {};
    for (const [fKey, fVal] of Object.entries(fieldMap)) {
      itemFields[itemId][fKey] = makeFieldDoc(fKey, fVal, meta);
    }
  };

  // Attributes + granular per-skill properties (skill_<slug>_name|value|modifier)
  if (Array.isArray(legacyDoc.attributes)) {
    for (let i = 0; i < legacyDoc.attributes.length; i++) {
      const attr = legacyDoc.attributes[i];
      const acronym = String(attr?.acronym || `ATTR${i}`);
      const itemId = makeItemId('attr', acronym, i, usedItemIds, warnings);
      const fieldMap = {
        name: String(attr?.name ?? ''),
        acronym,
        value: toSafeInt(attr?.value, 0),
        modifier: toSafeInt(attr?.modifier, 0),
      };
      const usedSlugs = new Set();
      const skills = Array.isArray(attr?.skills) ? attr.skills : [];
      for (let sIdx = 0; sIdx < skills.length; sIdx++) {
        const skill = skills[sIdx];
        const slug = makeSkillSlug(skill?.name, sIdx, usedSlugs);
        fieldMap[`skill_${slug}_name`] = String(skill?.name ?? '');
        fieldMap[`skill_${slug}_value`] = toSafeInt(skill?.value, 0);
        fieldMap[`skill_${slug}_modifier`] = toSafeInt(skill?.modifier, 0);
      }
      registerItem(itemId, 'ATTRIBUTE', fieldMap);
    }
  }

  // Inventory (granular properties)
  if (Array.isArray(legacyDoc.inventory)) {
    for (let i = 0; i < legacyDoc.inventory.length; i++) {
      const item = legacyDoc.inventory[i];
      const itemId = makeItemId('inv', item?.id, i, usedItemIds, warnings);
      registerItem(itemId, 'INVENTORY', splitInventoryItemGranular(item));
    }
  }

  // ItemStates (granular properties)
  const rawItemStates = Array.isArray(legacyDoc.itemStates) && legacyDoc.itemStates.length > 0
    ? legacyDoc.itemStates
    : parseJsonArraySafe(legacyDoc.canonicalItemsPayload, 'canonicalItemsPayload', warnings);
  if (Array.isArray(rawItemStates)) {
    for (let i = 0; i < rawItemStates.length; i++) {
      const entry = rawItemStates[i];
      const rawId = typeof entry?.id === 'string' ? entry.id : entry?.id?.value;
      const itemId = makeItemId('istate', rawId, i, usedItemIds, warnings);
      registerItem(itemId, 'ITEM_STATE', splitItemStateGranular(entry));
    }
  }

  // Powers (granular properties)
  if (Array.isArray(legacyDoc.powers)) {
    for (let i = 0; i < legacyDoc.powers.length; i++) {
      const p = legacyDoc.powers[i];
      const itemId = makeItemId('pow', p?.id, i, usedItemIds, warnings);
      registerItem(itemId, 'POWER', splitAbilityLikeGranular(p));
    }
  }

  // MysticAbilities (granular properties)
  if (Array.isArray(legacyDoc.mysticAbilities)) {
    for (let i = 0; i < legacyDoc.mysticAbilities.length; i++) {
      const m = legacyDoc.mysticAbilities[i];
      const itemId = makeItemId('mys', m?.id, i, usedItemIds, warnings);
      registerItem(itemId, 'MYSTIC', splitAbilityLikeGranular(m));
    }
  }

  // Canonical Abilities (granular properties)
  const rawAbilities = Array.isArray(legacyDoc.abilities) && legacyDoc.abilities.length > 0
    ? legacyDoc.abilities
    : parseJsonArraySafe(legacyDoc.canonicalAbilitiesPayload, 'canonicalAbilitiesPayload', warnings);
  if (Array.isArray(rawAbilities)) {
    for (let i = 0; i < rawAbilities.length; i++) {
      const a = rawAbilities[i];
      const itemId = makeItemId('abil', a?.id, i, usedItemIds, warnings);
      registerItem(itemId, 'ABILITY', splitAbilityLikeGranular(a));
    }
  }

  // Canonical ConditionInstances
  const rawConditions = Array.isArray(legacyDoc.conditionInstances) && legacyDoc.conditionInstances.length > 0
    ? legacyDoc.conditionInstances
    : parseJsonArraySafe(legacyDoc.canonicalConditionsPayload, 'canonicalConditionsPayload', warnings);
  if (Array.isArray(rawConditions)) {
    for (let i = 0; i < rawConditions.length; i++) {
      const c = rawConditions[i];
      const rawId = typeof c?.instanceId === 'string' ? c.instanceId : c?.instanceId?.value ?? c?.id;
      const itemId = makeItemId('cond', rawId, i, usedItemIds, warnings);
      registerItem(itemId, 'CONDITION', splitConditionInstanceGranular(c));
    }
  }

  // Legacy ConditionEffects
  if (Array.isArray(legacyDoc.conditions)) {
    for (let i = 0; i < legacyDoc.conditions.length; i++) {
      const c = legacyDoc.conditions[i];
      const itemId = makeItemId('lcond', c?.id, i, usedItemIds, warnings);
      registerItem(itemId, 'LEGACY_CONDITION', splitLegacyConditionGranular(c, itemId));
    }
  }

  // PersonalNotes + legacy notes fallback
  const personalNotes = Array.isArray(legacyDoc.personalNotes) ? legacyDoc.personalNotes : [];
  for (let i = 0; i < personalNotes.length; i++) {
    const n = personalNotes[i];
    const itemId = makeItemId('note', n?.id, i, usedItemIds, warnings);
    registerItem(itemId, 'NOTE', {
      title: String(n?.title ?? ''),
      text: String(n?.text ?? ''),
    });
  }
  if (typeof legacyDoc.notes === 'string' && legacyDoc.notes.trim() !== '') {
    const alreadyInPersonal = personalNotes.some(n => n?.text === legacyDoc.notes);
    if (!alreadyInPersonal) {
      const itemId = makeItemId('note', 'legacy_notes', personalNotes.length, usedItemIds, warnings);
      registerItem(itemId, 'NOTE', {
        title: 'Registro Pessoal 1',
        text: legacyDoc.notes,
      });
      warnings.push('Legacy notes string migrated into NOTE item.');
    }
  }

  const { media, legacyMediaInventory } = inspectLegacyMedia(charId, legacyDoc.photoUri ?? '', {
    updatedBy,
    updatedAt,
    warnings,
  });

  return {
    root,
    fields,
    items,
    itemFields,
    media,
    legacyMediaInventory,
    warnings,
  };
}

export function reconstructItemFromSparkV2(itemId, itemDoc, fMap = {}) {
  const val = k => fMap[k]?.value;

  switch (itemDoc.kind) {
    case 'ATTRIBUTE': {
      const skillBuckets = new Map();
      for (const [fKey, fDoc] of Object.entries(fMap)) {
        const m = /^skill_([A-Za-z0-9_.-]{1,64})_(value|modifier|name)$/.exec(fKey);
        if (!m) continue;
        const [, slug, prop] = m;
        const bucket = skillBuckets.get(slug) ?? { name: slug, value: 0, modifier: 0 };
        bucket[prop] = fDoc.value;
        skillBuckets.set(slug, bucket);
      }
      return {
        name: String(val('name') ?? ''),
        acronym: String(val('acronym') ?? itemId.replace(/^attr_/, '')),
        value: toSafeInt(val('value'), 0),
        modifier: toSafeInt(val('modifier'), 0),
        skills: [...skillBuckets.values()],
      };
    }

    case 'INVENTORY': {
      const entry = { ...structuredClone(val('meta') ?? {}) };
      for (const k of INVENTORY_DIRECT_KEYS) {
        if (k in fMap && val(k) !== undefined) {
          entry[k] = structuredClone(val(k));
        }
      }
      if (val('enhancements') && typeof val('enhancements') === 'object') {
        Object.assign(entry, structuredClone(val('enhancements')));
      }
      if (!entry.id) entry.id = itemId.replace(/^inv_/, '');
      return entry;
    }

    case 'ITEM_STATE': {
      const entry = { id: itemId.replace(/^istate_/, '') };
      for (const k of ISTATE_FIELD_KEYS) {
        if (k in fMap && val(k) !== undefined) {
          entry[k] = structuredClone(val(k));
        }
      }
      return entry;
    }

    case 'POWER':
    case 'MYSTIC':
    case 'ABILITY': {
      const entry = { ...structuredClone(val('meta') ?? {}) };
      for (const k of ABILITY_DIRECT_KEYS) {
        if (k in fMap && val(k) !== undefined) {
          entry[k] = structuredClone(val(k));
        }
      }
      if (!entry.id) entry.id = itemId.replace(/^(pow|mys|abil)_/, '');
      return entry;
    }

    case 'CONDITION': {
      const base = val('payload') && typeof val('payload') === 'object'
        ? structuredClone(val('payload'))
        : { instanceId: itemId.replace(/^cond_/, ''), payload: {} };
      if (base.payload && typeof base.payload === 'object') {
        if (val('intensity') !== undefined) base.payload.intensity = val('intensity');
        if (val('duration') !== undefined) base.payload.duration = structuredClone(val('duration'));
      }
      return base;
    }

    case 'LEGACY_CONDITION': {
      const base = {
        id: itemId.replace(/^lcond_/, ''),
        ...(val('payload') && typeof val('payload') === 'object' ? structuredClone(val('payload')) : {}),
      };
      for (const k of ['name', 'intensity', 'duration', 'summary', 'origin']) {
        if (k in fMap && val(k) !== undefined) {
          base[k] = structuredClone(val(k));
        }
      }
      return base;
    }

    case 'NOTE': {
      if (itemId === 'note_legacy_notes') {
        return { isLegacyNotes: true, text: String(val('text') ?? '') };
      }
      return {
        id: itemId.replace(/^note_/, ''),
        title: String(val('title') ?? ''),
        text: String(val('text') ?? ''),
      };
    }

    default:
      return null;
  }
}

export function reconstructLegacyFromSparkV2({ root, fields = {}, items = {}, itemFields = {}, media = null }) {
  if (!root || typeof root !== 'object') {
    throw new Error('Missing Spark v2 root document.');
  }

  const reconstructed = {
    id: root.id,
    ownerId: root.ownerId,
    campaignId: root.campaignId ?? '',
    deleted: Boolean(root.deleted),
    canonicalSchemaVersion: toSafeInt(root.canonicalSchemaVersion, 0),
    itemSchemaVersion: toSafeInt(root.itemSchemaVersion, 0),
    lockType: root.lockType ?? 'NONE',
    isLocked: Boolean(root.isLocked),
    lockedBy: root.lockedBy ?? '',
    lockedAt: root.lockedAt ?? null,
    updatedAt: root.updatedAt ?? 0,
    lastEditedBy: root.updatedBy ?? '',
    appliedDeliveryIds: Array.isArray(root.appliedDeliveryIds) ? [...root.appliedDeliveryIds] : [],
  };
  if (root.authorizedCampaignId) {
    reconstructed.authorizedCampaignId = root.authorizedCampaignId;
  }

  for (const [key, fieldDoc] of Object.entries(fields)) {
    if (!fieldDoc || fieldDoc.value === undefined) continue;
    if (key === 'itemCreationDraft') {
      const raw = fieldDoc.value;
      if (typeof raw === 'string' && raw.trim().startsWith('{')) {
        try {
          reconstructed.itemCreationDraft = JSON.parse(raw);
          continue;
        } catch {
          // keep string
        }
      }
      reconstructed.itemCreationDraft = raw;
      continue;
    }
    reconstructed[key] = structuredClone(fieldDoc.value);
  }

  if (media?.dataUri) {
    reconstructed.photoUri = media.dataUri;
  } else if (media?.bytesBase64 && media?.mimeType) {
    reconstructed.photoUri = `data:${media.mimeType};base64,${media.bytesBase64}`;
  } else {
    reconstructed.photoUri = '';
  }

  const attributes = [];
  const inventory = [];
  const itemStates = [];
  const powers = [];
  const mysticAbilities = [];
  const abilities = [];
  const conditionInstances = [];
  const conditions = [];
  const personalNotes = [];

  for (const [itemId, itemDoc] of Object.entries(items)) {
    if (!itemDoc || itemDoc.deleted) continue;
    const entry = reconstructItemFromSparkV2(itemId, itemDoc, itemFields[itemId] ?? {});
    if (!entry) continue;

    switch (itemDoc.kind) {
      case 'ATTRIBUTE':
        attributes.push(entry);
        break;
      case 'INVENTORY':
        inventory.push(entry);
        break;
      case 'ITEM_STATE':
        itemStates.push(entry);
        break;
      case 'POWER':
        powers.push(entry);
        break;
      case 'MYSTIC':
        mysticAbilities.push(entry);
        break;
      case 'ABILITY':
        abilities.push(entry);
        break;
      case 'CONDITION':
        conditionInstances.push(entry);
        break;
      case 'LEGACY_CONDITION':
        conditions.push(entry);
        break;
      case 'NOTE':
        if (entry.isLegacyNotes) {
          reconstructed.notes = entry.text;
        } else {
          personalNotes.push(entry);
        }
        break;
      default:
        break;
    }
  }

  attributes.sort((a, b) => {
    const ai = STANDARD_ATTRIBUTE_ORDER.indexOf(a.acronym);
    const bi = STANDARD_ATTRIBUTE_ORDER.indexOf(b.acronym);
    if (ai !== -1 && bi !== -1) return ai - bi;
    if (ai !== -1) return -1;
    if (bi !== -1) return 1;
    return String(a.acronym).localeCompare(String(b.acronym));
  });

  if (attributes.length > 0) reconstructed.attributes = attributes;
  if (inventory.length > 0) reconstructed.inventory = inventory;
  if (itemStates.length > 0) {
    reconstructed.itemStates = itemStates;
    reconstructed.canonicalItemsPayload = JSON.stringify(itemStates);
  }
  if (powers.length > 0) reconstructed.powers = powers;
  if (mysticAbilities.length > 0) reconstructed.mysticAbilities = mysticAbilities;
  if (abilities.length > 0) {
    reconstructed.abilities = abilities;
    reconstructed.canonicalAbilitiesPayload = JSON.stringify(abilities);
  }
  if (conditionInstances.length > 0) {
    reconstructed.conditionInstances = conditionInstances;
    reconstructed.canonicalConditionsPayload = JSON.stringify(conditionInstances);
  }
  if (conditions.length > 0) reconstructed.conditions = conditions;
  if (personalNotes.length > 0) reconstructed.personalNotes = personalNotes;

  return reconstructed;
}

function indexByKey(list, getKey) {
  const map = new Map();
  for (let i = 0; i < list.length; i++) {
    const item = list[i];
    const key = String(getKey(item, i) ?? `idx_${i}`);
    map.set(key, item);
  }
  return map;
}

function compareElementCollections(field, expectedList, actualList, getKey, mismatches) {
  const exp = Array.isArray(expectedList) ? expectedList : [];
  const act = Array.isArray(actualList) ? actualList : [];
  if (exp.length !== act.length) {
    mismatches.push({
      field,
      reason: 'LENGTH_MISMATCH',
      expected: exp.length,
      actual: act.length,
    });
    return;
  }
  const expMap = indexByKey(exp, getKey);
  const actMap = indexByKey(act, getKey);
  for (const [key, expItem] of expMap.entries()) {
    if (!actMap.has(key)) {
      mismatches.push({ field: `${field}[${key}]`, reason: 'MISSING_ID', expected: expItem, actual: undefined });
      continue;
    }
    const actItem = actMap.get(key);
    if (digest(expItem) !== digest(actItem)) {
      mismatches.push({ field: `${field}[${key}]`, reason: 'VALUE_MISMATCH', expected: expItem, actual: actItem });
    }
  }
  for (const key of actMap.keys()) {
    if (!expMap.has(key)) {
      mismatches.push({ field: `${field}[${key}]`, reason: 'UNEXPECTED_ID', expected: undefined, actual: actMap.get(key) });
    }
  }
}

export function compareParity(legacyDoc, reconstructed, options = {}) {
  const mismatches = [];
  const ignoredFields = new Set(['dirty', 'lastSyncedAt', 'fieldUpdatedAt', 'updatedAt', 'lastEditedBy']);
  const mediaInventory = options.legacyMediaInventory ?? null;

  if ((legacyDoc.id ?? reconstructed.id) !== reconstructed.id) {
    mismatches.push({ field: 'id', reason: 'VALUE_MISMATCH', expected: legacyDoc.id, actual: reconstructed.id });
  }
  if (legacyDoc.ownerId !== reconstructed.ownerId) {
    mismatches.push({ field: 'ownerId', reason: 'VALUE_MISMATCH', expected: legacyDoc.ownerId, actual: reconstructed.ownerId });
  }
  const expectedCampaign = normalizeCampaignId(legacyDoc.campaignId);
  if (expectedCampaign !== (reconstructed.campaignId ?? '')) {
    mismatches.push({ field: 'campaignId', reason: 'VALUE_MISMATCH', expected: expectedCampaign, actual: reconstructed.campaignId });
  }
  if (Boolean(legacyDoc.deleted) !== Boolean(reconstructed.deleted)) {
    mismatches.push({ field: 'deleted', reason: 'TOMBSTONE_MISMATCH', expected: Boolean(legacyDoc.deleted), actual: Boolean(reconstructed.deleted) });
  }

  const expectedLockType = legacyDoc.lockType === 'NONE' && legacyDoc.isLocked
    ? 'HISTORIAN'
    : (legacyDoc.lockType ?? 'NONE');
  const expectedIsLocked = expectedLockType !== 'NONE';
  if (expectedLockType !== reconstructed.lockType || expectedIsLocked !== reconstructed.isLocked) {
    mismatches.push({
      field: 'lockState',
      reason: 'LOCK_MISMATCH',
      expected: { lockType: expectedLockType, isLocked: expectedIsLocked },
      actual: { lockType: reconstructed.lockType, isLocked: reconstructed.isLocked },
    });
  }
  if ((expectedIsLocked ? (legacyDoc.lockedBy ?? '') : '') !== (reconstructed.lockedBy ?? '')) {
    mismatches.push({ field: 'lockedBy', reason: 'LOCK_MISMATCH', expected: legacyDoc.lockedBy ?? '', actual: reconstructed.lockedBy });
  }
  if ((expectedIsLocked ? (legacyDoc.lockedAt ?? null) : null) !== (reconstructed.lockedAt ?? null)) {
    mismatches.push({ field: 'lockedAt', reason: 'LOCK_MISMATCH', expected: legacyDoc.lockedAt ?? null, actual: reconstructed.lockedAt });
  }

  const expDeliveries = Array.isArray(legacyDoc.appliedDeliveryIds) ? legacyDoc.appliedDeliveryIds : [];
  const actDeliveries = Array.isArray(reconstructed.appliedDeliveryIds) ? reconstructed.appliedDeliveryIds : [];
  if (digest(expDeliveries) !== digest(actDeliveries)) {
    mismatches.push({ field: 'appliedDeliveryIds', reason: 'VALUE_MISMATCH', expected: expDeliveries, actual: actDeliveries });
  }

  const legacyPhoto = legacyDoc.photoUri ?? '';
  const reconPhoto = reconstructed.photoUri ?? '';
  if (legacyPhoto !== reconPhoto) {
    const preserved = Boolean(
      mediaInventory?.requiresLocalPreservation && mediaInventory.originalPhotoUri === legacyPhoto && reconPhoto === ''
    );
    if (!preserved) {
      mismatches.push({ field: 'photoUri', reason: 'MEDIA_MISMATCH', expected: legacyPhoto, actual: reconPhoto });
    }
  }

  if (Array.isArray(legacyDoc.attributes) || Array.isArray(reconstructed.attributes)) {
    const expAttrs = Array.isArray(legacyDoc.attributes) ? legacyDoc.attributes : [];
    const actAttrs = Array.isArray(reconstructed.attributes) ? reconstructed.attributes : [];
    compareElementCollections(
      'attributes',
      expAttrs.map(a => ({ ...a, skills: Object.fromEntries((a.skills ?? []).map(s => [s.name, s])) })),
      actAttrs.map(a => ({ ...a, skills: Object.fromEntries((a.skills ?? []).map(s => [s.name, s])) })),
      a => a.acronym,
      mismatches
    );
  }
  if (Array.isArray(legacyDoc.inventory) || Array.isArray(reconstructed.inventory)) {
    compareElementCollections('inventory', legacyDoc.inventory, reconstructed.inventory, it => it.id, mismatches);
  }
  if (Array.isArray(legacyDoc.powers) || Array.isArray(reconstructed.powers)) {
    compareElementCollections('powers', legacyDoc.powers, reconstructed.powers, p => p.id, mismatches);
  }
  if (Array.isArray(legacyDoc.mysticAbilities) || Array.isArray(reconstructed.mysticAbilities)) {
    compareElementCollections('mysticAbilities', legacyDoc.mysticAbilities, reconstructed.mysticAbilities, m => m.id, mismatches);
  }
  if (Array.isArray(legacyDoc.conditions) || Array.isArray(reconstructed.conditions)) {
    compareElementCollections('conditions', legacyDoc.conditions, reconstructed.conditions, c => c.id, mismatches);
  }
  if (Array.isArray(legacyDoc.personalNotes) || Array.isArray(reconstructed.personalNotes)) {
    compareElementCollections('personalNotes', legacyDoc.personalNotes, reconstructed.personalNotes, n => n.id, mismatches);
  }
  if (typeof legacyDoc.notes === 'string' && legacyDoc.notes.trim() !== '') {
    const foundInNotes = reconstructed.notes === legacyDoc.notes;
    const foundInPersonal = (reconstructed.personalNotes ?? []).some(n => n.text === legacyDoc.notes);
    if (!foundInNotes && !foundInPersonal) {
      mismatches.push({ field: 'notes', reason: 'VALUE_MISMATCH', expected: legacyDoc.notes, actual: reconstructed.notes ?? '' });
    }
  }

  const expAbilities = Array.isArray(legacyDoc.abilities) && legacyDoc.abilities.length > 0
    ? legacyDoc.abilities
    : (parseJsonArraySafe(legacyDoc.canonicalAbilitiesPayload, 'canonicalAbilitiesPayload', []) ?? []);
  const actAbilities = Array.isArray(reconstructed.abilities) ? reconstructed.abilities : [];
  if (expAbilities.length > 0 || actAbilities.length > 0) {
    compareElementCollections('abilities', expAbilities, actAbilities, a => a.id, mismatches);
  }

  const expItemStates = Array.isArray(legacyDoc.itemStates) && legacyDoc.itemStates.length > 0
    ? legacyDoc.itemStates
    : (parseJsonArraySafe(legacyDoc.canonicalItemsPayload, 'canonicalItemsPayload', []) ?? []);
  const actItemStates = Array.isArray(reconstructed.itemStates) ? reconstructed.itemStates : [];
  if (expItemStates.length > 0 || actItemStates.length > 0) {
    compareElementCollections(
      'itemStates',
      expItemStates.map(it => ({ ...it, id: typeof it.id === 'string' ? it.id : it.id?.value })),
      actItemStates.map(it => ({ ...it, id: typeof it.id === 'string' ? it.id : it.id?.value })),
      it => it.id,
      mismatches
    );
  }

  const expCondInstances = Array.isArray(legacyDoc.conditionInstances) && legacyDoc.conditionInstances.length > 0
    ? legacyDoc.conditionInstances
    : (parseJsonArraySafe(legacyDoc.canonicalConditionsPayload, 'canonicalConditionsPayload', []) ?? []);
  const actCondInstances = Array.isArray(reconstructed.conditionInstances) ? reconstructed.conditionInstances : [];
  if (expCondInstances.length > 0 || actCondInstances.length > 0) {
    compareElementCollections(
      'conditionInstances',
      expCondInstances,
      actCondInstances,
      c => (typeof c.instanceId === 'string' ? c.instanceId : c.instanceId?.value ?? c.id),
      mismatches
    );
  }

  for (const key of ALL_FIELD_KEYS) {
    if (ignoredFields.has(key)) continue;
    const expVal = legacyDoc[key];
    const actVal = reconstructed[key];
    if (expVal === undefined && actVal === undefined) continue;
    if (expVal === null && actVal === undefined && (key === 'creationCompletedAt' || key === 'itemCreationDraft')) continue;
    if (digest(expVal) !== digest(actVal)) {
      mismatches.push({ field: key, reason: 'FIELD_MISMATCH', expected: expVal, actual: actVal });
    }
  }

  for (const payloadKey of [
    'migrationReviewPayload',
    'canonicalBodyPayload',
    'activeModifiersPayload',
    'scopedItemCatalogPayload',
    'canonicalProgressionPayload',
  ]) {
    if (legacyDoc[payloadKey] !== undefined && legacyDoc[payloadKey] !== reconstructed[payloadKey]) {
      mismatches.push({
        field: payloadKey,
        reason: 'PAYLOAD_MISMATCH',
        expected: legacyDoc[payloadKey],
        actual: reconstructed[payloadKey],
      });
    }
  }

  return {
    ok: mismatches.length === 0,
    mismatchCount: mismatches.length,
    mismatches,
  };
}

export function convertLegacyMemberToSparkV2(legacyDocId, legacyDoc) {
  if (!legacyDoc || typeof legacyDoc !== 'object') {
    throw new Error(`Missing legacy campaignMember document for ${legacyDocId}.`);
  }
  const parts = typeof legacyDocId === 'string' && legacyDocId.includes('::') ? legacyDocId.split('::') : [];
  const campaignId = legacyDoc.campaignId || parts[0];
  const uid = legacyDoc.userId || parts[1];
  if (!safeId(campaignId) || !safeId(uid)) {
    throw new Error(`Invalid campaignMember ID or fields: ${legacyDocId}`);
  }
  const warnings = [];
  if (legacyDoc.role && legacyDoc.role !== 'PLAYER') {
    warnings.push(`Legacy member ${campaignId}::${uid} had role=${legacyDoc.role}; normalized to PLAYER per Spark v2.`);
  }
  const state = ['ACTIVE', 'LEFT', 'REMOVED'].includes(legacyDoc.state) ? legacyDoc.state : 'ACTIVE';
  return {
    path: `campaigns/${campaignId}/members/${uid}`,
    campaignId,
    uid,
    data: {
      campaignId,
      userId: uid,
      role: 'PLAYER',
      state,
      characterIds: Array.isArray(legacyDoc.characterIds) ? [...legacyDoc.characterIds] : [],
      joinedAt: legacyDoc.joinedAt ?? 0,
      updatedAt: legacyDoc.updatedAt ?? 0,
      joinedByInviteId: String(legacyDoc.joinedByInviteId ?? ''),
    },
    warnings,
  };
}

export function convertLegacyInviteToSparkV2(inviteId, legacyDoc) {
  if (!legacyDoc || typeof legacyDoc !== 'object') {
    throw new Error(`Missing legacy campaignInvite document for ${inviteId}.`);
  }
  const id = legacyDoc.id || inviteId;
  const campaignId = legacyDoc.campaignId;
  if (!safeId(id) || !safeId(campaignId)) {
    throw new Error(`Invalid campaignInvite ID or campaignId: ${inviteId}`);
  }
  const warnings = [];
  let state = legacyDoc.state;
  if (!['PENDING', 'CONSUMED', 'REVOKED'].includes(state)) {
    state = legacyDoc.revokedAt != null ? 'REVOKED' : 'PENDING';
  }
  return {
    path: `campaigns/${campaignId}/invites/${id}`,
    campaignId,
    inviteId: id,
    data: {
      id,
      campaignId,
      code: String(legacyDoc.code || id),
      campaignName: String(legacyDoc.campaignName ?? ''),
      campaignDescription: String(legacyDoc.campaignDescription ?? ''),
      createdBy: String(legacyDoc.createdBy ?? ''),
      recipientUid: String(legacyDoc.recipientUid ?? ''),
      expiresAt: legacyDoc.expiresAt ?? null,
      state,
      consumedBy: String(legacyDoc.consumedBy ?? ''),
      consumedAt: legacyDoc.consumedAt ?? null,
      revokedAt: legacyDoc.revokedAt ?? null,
      createdAt: legacyDoc.createdAt ?? 0,
    },
    warnings,
  };
}
