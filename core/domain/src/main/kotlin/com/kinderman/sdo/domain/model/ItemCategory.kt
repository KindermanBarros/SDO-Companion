package com.kinderman.sdo.domain.model

import java.text.Normalizer
import java.util.Locale
import kotlinx.serialization.Serializable

/** Top-level inventory and filter groupings shared by the character sheet, creation wizard, and catalog pickers. */
@Serializable
enum class InventorySectionGroup(val label: String) {
    WEAPONS("Armas"),
    ARMOR("Armaduras"),
    ITEMS("Itens"),
}

/** Canonical item categories covering weapons, protections, general equipment, legacy, missing, and custom values. */
@Serializable
enum class CanonicalItemCategory(
    val canonicalLabel: String,
    val sectionGroup: InventorySectionGroup,
) {
    SHORT_WEAPON("Arma curta", InventorySectionGroup.WEAPONS),
    MEDIUM_WEAPON("Arma média", InventorySectionGroup.WEAPONS),
    LARGE_WEAPON("Arma grande", InventorySectionGroup.WEAPONS),
    POLEARM("Arma de haste", InventorySectionGroup.WEAPONS),
    BOW_OR_CROSSBOW("Arco ou besta", InventorySectionGroup.WEAPONS),
    STEAM_WEAPON("Arma de vapor", InventorySectionGroup.WEAPONS),
    TESLA_WEAPON("Arma de Tesla", InventorySectionGroup.WEAPONS),
    SPECIAL_WEAPON("Arma especial", InventorySectionGroup.WEAPONS),
    GENERIC_WEAPON("Arma", InventorySectionGroup.WEAPONS),

    ARMOR("Armadura", InventorySectionGroup.ARMOR),
    SHIELD("Escudo", InventorySectionGroup.ARMOR),
    ACCESSORY("Acessório", InventorySectionGroup.ARMOR),

    AMMUNITION("Munição", InventorySectionGroup.ITEMS),
    CONSUMABLE("Consumível", InventorySectionGroup.ITEMS),
    ARCANE_CONSUMABLE("Consumível arcano", InventorySectionGroup.ITEMS),
    TOOL("Ferramenta", InventorySectionGroup.ITEMS),
    CONTAINER("Recipiente de Carga", InventorySectionGroup.ITEMS),
    ARCANE_COMPONENT("Componente arcano", InventorySectionGroup.ITEMS),
    GEM("Gema", InventorySectionGroup.ITEMS),
    TECHNOLOGY("Tecnologia", InventorySectionGroup.ITEMS),
    RELIC("Relíquia do Antes-Véu", InventorySectionGroup.ITEMS),
    MATERIAL("Material", InventorySectionGroup.ITEMS),
    ENHANCEMENT("Aprimoramento", InventorySectionGroup.ITEMS),
    ASH("Cinza", InventorySectionGroup.ITEMS),
    GENERAL_ITEM("Item", InventorySectionGroup.ITEMS),
    LEGACY_NARRATIVE("Narrativo (legado)", InventorySectionGroup.ITEMS),
    UNCATEGORIZED("Sem categoria", InventorySectionGroup.ITEMS),
    UNKNOWN("Outra categoria", InventorySectionGroup.ITEMS),
}

data class ResolvedItemCategory(
    val canonical: CanonicalItemCategory,
    val displayLabel: String,
    val filterKey: String,
) {
    val sectionGroup: InventorySectionGroup get() = canonical.sectionGroup
}

object ItemCategoryMapper {
    private val knownWeaponBaseIds = setOf(
        "faca", "adaga", "punhal", "espada_curta", "machadinha", "porrete", "soqueira", "chicote",
        "espada_longa", "sabre", "machado_de_batalha", "maca", "martelo_de_guerra", "florete", "clava",
        "montante", "machado_grande", "malho", "espada_bastarda", "foice_pesada",
        "lanca", "alabarda", "pique", "glaive", "tridente", "bastao",
        "arco_curto", "arco_longo", "arco_composto", "lancadora", "besta_leve", "besta_pesada",
        "pistola_a_vapor", "rifle_a_vapor", "escopeta_a_vapor", "canhao_portatil_a_vapor",
        "pistola_de_tesla", "rifle_de_tesla", "bobina_portatil_de_tesla", "lanca_de_arco_voltaico",
        "rede", "boleadeira", "chakram", "zarabatana", "funda",
    )

    private val knownShieldBaseIds = setOf("escudo_leve", "escudo_pesado", "broquel", "pavise")
    private val knownArmorBaseIds = setOf(
        "gibao", "couro_batido", "cota_de_malha", " couraca", "couraca", "armadura_completa",
        "casaco_reforcado", "placa_peitoral", "grevas", "manoplas", "elmo", "elmo_fechado",
    )

    fun normalizeKey(value: String): String =
        Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")

    private fun extractEffectCategoryHint(effect: String): String =
        effect.lineSequence()
            .map(String::trim)
            .firstOrNull { it.startsWith("Categoria:", ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()
            .orEmpty()

    fun resolve(
        rawCategory: String,
        effect: String = "",
        baseId: String = "",
        catalogEntryId: String = "",
        linkedAshId: String = "",
        backpackCapacity: Int = 0,
    ): ResolvedItemCategory {
        if (linkedAshId.isNotBlank() || catalogEntryId.startsWith("ash.", ignoreCase = true)) {
            return ResolvedItemCategory(CanonicalItemCategory.ASH, CanonicalItemCategory.ASH.canonicalLabel, "ASH")
        }

        val trimmedRaw = rawCategory.trim()
        val hintFromEffect = if (trimmedRaw.isEmpty() || trimmedRaw.equals("LEGACY_NARRATIVE", ignoreCase = true) || trimmedRaw.equals("Item", ignoreCase = true)) {
            extractEffectCategoryHint(effect)
        } else {
            ""
        }

        val candidate = when {
            trimmedRaw.isNotEmpty() &&
                !trimmedRaw.equals("LEGACY_NARRATIVE", ignoreCase = true) &&
                !(trimmedRaw.equals("Item", ignoreCase = true) && hintFromEffect.isNotEmpty()) -> trimmedRaw
            hintFromEffect.isNotEmpty() -> hintFromEffect
            else -> trimmedRaw
        }

        val cleanBase = baseId.trim().lowercase(Locale.ROOT)
        if (candidate.isEmpty() || candidate.equals("Item", ignoreCase = true) || candidate.equals("LEGACY_NARRATIVE", ignoreCase = true)) {
            when {
                cleanBase in knownWeaponBaseIds -> return fromCanonical(CanonicalItemCategory.GENERIC_WEAPON)
                cleanBase in knownShieldBaseIds -> return fromCanonical(CanonicalItemCategory.SHIELD)
                cleanBase in knownArmorBaseIds -> return fromCanonical(CanonicalItemCategory.ARMOR)
                backpackCapacity > 0 -> return fromCanonical(CanonicalItemCategory.CONTAINER)
            }
        }

        if (candidate.isEmpty()) {
            return fromCanonical(CanonicalItemCategory.UNCATEGORIZED)
        }

        val key = normalizeKey(candidate)
        val matched = when (key) {
            "legacy_narrative", "legado", "narrativo", "item narrativo", "narrativo (legado)" ->
                CanonicalItemCategory.LEGACY_NARRATIVE
            "sem categoria", "indefinido", "indefinida", "none", "null", "unknown" ->
                CanonicalItemCategory.UNCATEGORIZED
            "arma curta", "armas curtas" -> CanonicalItemCategory.SHORT_WEAPON
            "arma media", "armas medias" -> CanonicalItemCategory.MEDIUM_WEAPON
            "arma grande", "armas grandes" -> CanonicalItemCategory.LARGE_WEAPON
            "arma de haste", "armas de haste", "haste" -> CanonicalItemCategory.POLEARM
            "arco ou besta", "arcos ou bestas", "arco e besta", "arcos e bestas", "arco", "besta", "arma a distancia", "arma de disparo" ->
                CanonicalItemCategory.BOW_OR_CROSSBOW
            "arma de vapor", "armas de vapor", "vapor" -> CanonicalItemCategory.STEAM_WEAPON
            "arma de tesla", "armas de tesla", "tesla" -> CanonicalItemCategory.TESLA_WEAPON
            "arma especial", "armas especiais" -> CanonicalItemCategory.SPECIAL_WEAPON
            "arma", "armas", "arma branca", "arma marcial", "arma simples" -> CanonicalItemCategory.GENERIC_WEAPON
            "armadura", "armaduras", "armadura leve", "armadura media", "armadura pesada", "protecao", "vestimenta blindada" ->
                CanonicalItemCategory.ARMOR
            "escudo", "escudos", "broquel" -> CanonicalItemCategory.SHIELD
            "acessorio", "acessorios", "acessorio de protecao", "vestuario", "amuleto", "anel", "capa" ->
                CanonicalItemCategory.ACCESSORY
            "municao", "municoes", "projeteis", "flechas", "virotes", "cartuchos" ->
                CanonicalItemCategory.AMMUNITION
            "consumivel", "consumiveis", "pocao", "alquimia", "suprimento", "kit medico" ->
                CanonicalItemCategory.CONSUMABLE
            "consumivel arcano", "consumiveis arcanos", "pergaminho", "elixir arcano" ->
                CanonicalItemCategory.ARCANE_CONSUMABLE
            "ferramenta", "ferramentas", "instrumento", "oficio", "kit de ferramenta", "kit de ferramentas" ->
                CanonicalItemCategory.TOOL
            "recipiente de carga", "recipientes de carga", "recipiente", "mochila", "bolsa", "alforge", "bau" ->
                CanonicalItemCategory.CONTAINER
            "componente arcano", "componentes arcanos", "catalisador", "foco arcano" ->
                CanonicalItemCategory.ARCANE_COMPONENT
            "gema", "gemas", "gema arcana" -> CanonicalItemCategory.GEM
            "tecnologia", "tecnologias", "implante", "protese", "dispositivo" -> CanonicalItemCategory.TECHNOLOGY
            "reliquia do antes-veu", "reliquias do antes-veu", "reliquia", "artefato" ->
                CanonicalItemCategory.RELIC
            "material", "materiais", "materia-prima", "minerio", "liga" -> CanonicalItemCategory.MATERIAL
            "aprimoramento", "aprimoramentos", "modificacao", "modificacao de arma", "modificacao de armadura" ->
                CanonicalItemCategory.ENHANCEMENT
            "cinza", "cinzas", "cinza bruta", "cinza refinada", "cinza pura" -> CanonicalItemCategory.ASH
            "item", "itens", "item geral", "equipamento", "equipamento geral", "utilitario", "outros" ->
                if (backpackCapacity > 0) CanonicalItemCategory.CONTAINER else CanonicalItemCategory.GENERAL_ITEM
            else -> when {
                key.startsWith("arma") && !key.startsWith("armadura") -> CanonicalItemCategory.GENERIC_WEAPON
                key.startsWith("arco ") || key.startsWith("besta ") -> CanonicalItemCategory.BOW_OR_CROSSBOW
                key.startsWith("escudo") -> CanonicalItemCategory.SHIELD
                key.startsWith("armadura") -> CanonicalItemCategory.ARMOR
                key.startsWith("acessorio") -> CanonicalItemCategory.ACCESSORY
                key.startsWith("munic") -> CanonicalItemCategory.AMMUNITION
                key.contains("consumivel") && key.contains("arcano") -> CanonicalItemCategory.ARCANE_CONSUMABLE
                key.startsWith("consumiv") -> CanonicalItemCategory.CONSUMABLE
                key.startsWith("ferrament") -> CanonicalItemCategory.TOOL
                key.contains("recipiente") || backpackCapacity > 0 -> CanonicalItemCategory.CONTAINER
                else -> null
            }
        }

        if (matched != null) return fromCanonical(matched)
        return ResolvedItemCategory(
            canonical = CanonicalItemCategory.UNKNOWN,
            displayLabel = candidate,
            filterKey = "UNKNOWN:$key",
        )
    }

    private fun fromCanonical(category: CanonicalItemCategory): ResolvedItemCategory =
        ResolvedItemCategory(
            canonical = category,
            displayLabel = category.canonicalLabel,
            filterKey = category.name,
        )
}

fun InventoryItem.resolvedCategory(): ResolvedItemCategory =
    ItemCategoryMapper.resolve(
        rawCategory = category,
        effect = effect,
        baseId = baseId,
        catalogEntryId = catalogEntryId,
        linkedAshId = linkedAshId,
        backpackCapacity = backpackCapacity,
    )

fun CatalogEntry.resolvedItemCategory(): ResolvedItemCategory =
    if (kind == CatalogKind.ASH) {
        ResolvedItemCategory(CanonicalItemCategory.ASH, CanonicalItemCategory.ASH.canonicalLabel, CanonicalItemCategory.ASH.name)
    } else {
        ItemCategoryMapper.resolve(
            rawCategory = group,
            effect = summary,
            catalogEntryId = id,
            backpackCapacity = backpackCapacity,
        )
    }

fun String.resolveItemCategory(): ResolvedItemCategory = ItemCategoryMapper.resolve(rawCategory = this)

fun InventoryItem.isWeaponItem(): Boolean =
    resolvedCategory().sectionGroup == InventorySectionGroup.WEAPONS

fun InventoryItem.isShieldItem(): Boolean =
    resolvedCategory().canonical == CanonicalItemCategory.SHIELD

fun InventoryItem.isArmorPieceItem(): Boolean =
    resolvedCategory().canonical == CanonicalItemCategory.ARMOR

fun InventoryItem.isAccessoryItem(): Boolean =
    resolvedCategory().canonical == CanonicalItemCategory.ACCESSORY

fun InventoryItem.isWieldableItem(): Boolean {
    val cat = resolvedCategory()
    return cat.sectionGroup == InventorySectionGroup.WEAPONS || cat.canonical == CanonicalItemCategory.SHIELD
}

fun InventoryItem.isBodyEquippableItem(): Boolean {
    val cat = resolvedCategory().canonical
    return cat == CanonicalItemCategory.ARMOR ||
        cat == CanonicalItemCategory.ACCESSORY ||
        cat == CanonicalItemCategory.SHIELD ||
        cat == CanonicalItemCategory.CONTAINER ||
        backpackCapacity > 0
}

fun InventoryItem.isContainerItem(): Boolean =
    resolvedCategory().canonical == CanonicalItemCategory.CONTAINER || backpackCapacity > 0

fun InventoryItem.isStackableItem(): Boolean {
    val cat = resolvedCategory().canonical
    return cat in setOf(
        CanonicalItemCategory.AMMUNITION,
        CanonicalItemCategory.CONSUMABLE,
        CanonicalItemCategory.ARCANE_CONSUMABLE,
        CanonicalItemCategory.ARCANE_COMPONENT,
        CanonicalItemCategory.GEM,
        CanonicalItemCategory.MATERIAL,
        CanonicalItemCategory.ASH,
    ) || quantity > 1
}
