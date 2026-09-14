package com.kinderman.sdo.domain.catalog

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EquipmentGlossaryTest {
    @Test fun includesEveryRequiredRulesTerm() {
        val terms = EquipmentGlossary.entries.map { it.term }
        listOf(
            "P.G. — Proteção Geral",
            "P.L. — Proteção Local",
            "LA — Limitação de Agilidade",
            "Leve",
            "Pesado",
            "Delicado",
            "Dilaceração",
            "Anti-Pessoal",
            "Anti-Cavalaria",
            "Sobrecarga",
            "Bobina",
            "Passo de dado",
            "Categoria de dado",
            "Qualidade",
            "Durabilidade",
        ).forEach { required -> assertTrue("Termo ausente: $required", required in terms) }
    }

    @Test fun includesAllItemsMaterialsAndModifications() {
        val terms = EquipmentGlossary.entries.map { it.term }.toSet()
        val catalogTerms = listOf(
            ItemCreationRules.weaponMaterials,
            ItemCreationRules.armorMaterials,
            ItemCreationRules.weaponBases,
            ItemCreationRules.armorBases,
            ItemCreationRules.weaponModifications,
            ItemCreationRules.armorModifications,
        ).flatten().map { it.name }

        assertTrue(catalogTerms.all { it in terms })
    }

    @Test fun materialCardsHaveUniqueLazyListKeys() {
        val materials = EquipmentGlossary.entries.filter { it.section == "Materiais" }
        val keys = materials.map { "${it.section}:${it.group}:${it.referenceId}:${it.term}" }

        assertEquals(keys.size, keys.distinct().size)
        assertTrue(materials.any { it.group.endsWith("— Armas") })
        assertTrue(materials.any { it.group.endsWith("— Armaduras") })
    }

    @Test fun playerGlossaryDoesNotExposeMoneyOrHeritageCosts() {
        val definitions = EquipmentGlossary.entries.joinToString { it.definition }

        assertFalse(definitions.contains("PH"))
        assertFalse(definitions.contains("E$"))
        assertFalse(definitions.contains("preço", ignoreCase = true))
    }

    @Test fun playerCharacterScreensDoNotExposePurchaseOrSaleFlows() {
        val workingDirectory = Path.of(System.getProperty("user.dir"))
        val sourceDirectory = sequenceOf(
            workingDirectory.resolve("src/main/java/com/kinderman/sdo/presentation/character"),
            workingDirectory.resolve("app/src/main/java/com/kinderman/sdo/presentation/character"),
        ).first { Files.isDirectory(it) }
        val forbidden = listOf("purchasePrice", "\"E$", "preço", "compra", "venda")
        Files.walk(sourceDirectory).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }.forEach { path ->
                val source = Files.readString(path)
                forbidden.forEach { token ->
                    assertFalse("${path.fileName} expõe $token", source.contains(token, ignoreCase = true))
                }
            }
        }
    }
}
