package com.kinderman.sdo.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class UserRole { PLAYER, MASTER }

data class ResourceValue(val current: Int = 0, val maximum: Int = 0)
data class SkillValue(val name: String = "", val value: Int = 0, val modifier: Int = 0)
data class AttributeValue(val name: String = "", val acronym: String = "", val value: Int = 0, val modifier: Int = 0, val skills: List<SkillValue> = emptyList())
data class Power(val name: String = "", val origin: String = "", val cost: String = "", val action: String = "", val range: String = "", val duration: String = "", val limit: String = "", val effect: String = "")
data class InventoryItem(val id: String = java.util.UUID.randomUUID().toString(), val state: String = "M", val name: String = "", val load: Int = 0, val durability: String = "", val region: String = "", val effect: String = "")

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val ownerId: String = "",
    val campaignId: String = "default",
    val name: String = "Novo personagem",
    val race: String = "",
    val subRace: String = "",
    val occupation: String = "",
    val age: String = "",
    val level: Int = 1,
    val money: Int = 0,
    val life: ResourceValue = ResourceValue(),
    val sanity: ResourceValue = ResourceValue(),
    val arcane: ResourceValue = ResourceValue(),
    val energy: ResourceValue = ResourceValue(),
    val destiny: ResourceValue = ResourceValue(1, 5),
    val exhaustion: ResourceValue = ResourceValue(0, 10),
    val corruption: ResourceValue = ResourceValue(0, 100),
    val attributes: List<AttributeValue> = defaultAttributes(),
    val protections: Map<String, Int> = mapOf("Geral" to 10, "Esquiva" to 10, "Postura" to 10, "Mental" to 10, "Arcana" to 10),
    val positiveTraits: List<String> = emptyList(),
    val negativeTraits: List<String> = emptyList(),
    val pathName: String = "",
    val pathMotto: String = "",
    val powers: List<Power> = emptyList(),
    val inventory: List<InventoryItem> = emptyList(),
    val story: String = "",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val dirty: Boolean = true
)

fun defaultAttributes() = listOf(
    AttributeValue("Força", "FOR", skills = listOf("Atletismo", "Brutalidade", "Luta", "Arremesso").map { SkillValue(it) }),
    AttributeValue("Vigor", "VIG", skills = listOf("Energia", "Vitalidade", "Tolerância", "Regeneração").map { SkillValue(it) }),
    AttributeValue("Agilidade", "AGI", skills = listOf("Furtividade", "Reflexos", "Movimento", "Pontaria").map { SkillValue(it) }),
    AttributeValue("Poder", "POD", skills = listOf("Arcano", "Sentidos", "Controle", "Recuperação").map { SkillValue(it) }),
    AttributeValue("Intelecto", "INT", skills = listOf("Sanidade", "Intuição", "Religião", "Raciocínio").map { SkillValue(it) }),
    AttributeValue("Carisma", "CAR", skills = listOf("Política", "Lábia", "Enganação", "Intimidação").map { SkillValue(it) })
)

class Converters {
    @TypeConverter fun resourceToJson(v: ResourceValue) = "${v.current}|${v.maximum}"
    @TypeConverter fun jsonToResource(v: String) = v.split('|').let { ResourceValue(it[0].toInt(), it[1].toInt()) }
    @TypeConverter fun attributesToJson(v: List<AttributeValue>) = encodeAttributes(v)
    @TypeConverter fun jsonToAttributes(v: String) = decodeAttributes(v)
    @TypeConverter fun stringsToJson(v: List<String>) = v.joinToString("\u001f")
    @TypeConverter fun jsonToStrings(v: String) = if (v.isEmpty()) emptyList() else v.split("\u001f")
    @TypeConverter fun mapToJson(v: Map<String, Int>) = v.entries.joinToString("\u001f") { "${it.key}|${it.value}" }
    @TypeConverter fun jsonToMap(v: String) = if(v.isEmpty()) emptyMap() else v.split("\u001f").associate { it.substringBefore('|') to it.substringAfter('|').toInt() }
    @TypeConverter fun powersToJson(v: List<Power>) = v.joinToString("\u001e") { listOf(it.name,it.origin,it.cost,it.action,it.range,it.duration,it.limit,it.effect).joinToString("\u001f") }
    @TypeConverter fun jsonToPowers(v: String) = if(v.isEmpty()) emptyList() else v.split("\u001e").map { it.split("\u001f").let { p -> Power(p.getOrElse(0){""},p.getOrElse(1){""},p.getOrElse(2){""},p.getOrElse(3){""},p.getOrElse(4){""},p.getOrElse(5){""},p.getOrElse(6){""},p.getOrElse(7){""}) } }
    @TypeConverter fun inventoryToJson(v: List<InventoryItem>) = v.joinToString("\u001e") { listOf(it.id,it.state,it.name,it.load.toString(),it.durability,it.region,it.effect).joinToString("\u001f") }
    @TypeConverter fun jsonToInventory(v: String) = if(v.isEmpty()) emptyList() else v.split("\u001e").map { it.split("\u001f").let { p -> InventoryItem(p[0],p[1],p[2],p[3].toInt(),p.getOrElse(4){""},p.getOrElse(5){""},p.getOrElse(6){""}) } }
    private fun encodeAttributes(v: List<AttributeValue>) = v.joinToString("\u001d") { a -> listOf(a.name,a.acronym,a.value.toString(),a.modifier.toString(),a.skills.joinToString("\u001e") { "${it.name}\u001f${it.value}\u001f${it.modifier}" }).joinToString("\u001c") }
    private fun decodeAttributes(v: String) = if(v.isEmpty()) defaultAttributes() else v.split("\u001d").map { row -> row.split("\u001c").let { a -> AttributeValue(a[0],a[1],a[2].toInt(),a[3].toInt(),a.getOrElse(4){""}.split("\u001e").filter(String::isNotEmpty).map { s -> s.split("\u001f").let { SkillValue(it[0],it[1].toInt(),it[2].toInt()) } }) } }
}

@Dao interface CharacterDao {
    @Query("SELECT * FROM characters WHERE ownerId = :uid OR :isMaster = 1 ORDER BY updatedAt DESC") fun observe(uid: String, isMaster: Boolean): Flow<List<CharacterEntity>>
    @Query("SELECT * FROM characters WHERE id = :id") fun observeOne(id: String): Flow<CharacterEntity?>
    @Query("SELECT * FROM characters WHERE dirty = 1") suspend fun dirty(): List<CharacterEntity>
    @Upsert suspend fun upsert(character: CharacterEntity)
    @Query("UPDATE characters SET dirty = 0 WHERE id = :id") suspend fun markSynced(id: String)
}

@Database(entities = [CharacterEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() { abstract fun characterDao(): CharacterDao }
