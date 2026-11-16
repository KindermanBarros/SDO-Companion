package com.kinderman.sdo.presentation.sync

import com.kinderman.sdo.domain.model.SheetConflictItem
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ManualConflictValueTest {
    private fun conflict(local: String, remote: String = local, key: String = "field:name") = SheetConflictItem(
        id = "conflict", accountId = "account", sheetId = "sheet", operationId = "operation",
        targetKey = key, label = "Field", baseRevision = 1, remoteRevision = 2,
        baseSummary = "", localSummary = "", remoteSummary = "",
        basePayloadJson = local, localPayloadJson = local, remotePayloadJson = remote,
    )

    @Test fun textStartsLocalAndEscapesQuotesNewlinesAndBackslashes() {
        val editor = requireNotNull(ManualConflictValue.from(conflict("\"Local\"", "\"Remote\"")))
        assertEquals(listOf("Local"), editor.initialValues)
        val text = "Merged \"quote\"\npath\\file"
        assertEquals(kotlinx.serialization.json.JsonPrimitive(text),
            Json.parseToJsonElement(requireNotNull(editor.encode(listOf(text)))))
    }

    @Test fun integerRejectsMalformedOverflowAndNegativeMoney() {
        val editor = requireNotNull(ManualConflictValue.from(conflict("10", "20", "field:money")))
        listOf("", "abc", "1.5", "2147483648", "-1").forEach { assertNull(editor.encode(listOf(it))) }
        assertEquals("15", editor.encode(listOf("15")))
        val version = requireNotNull(ManualConflictValue.from(conflict("1", "2", "field:creationRulesVersion")))
        assertNull(version.encode(listOf("0")))
        assertEquals("1", version.encode(listOf("1")))
    }

    @Test fun booleanAndDecimalPreserveTypesAndRejectInvalidNumbers() {
        val boolean = requireNotNull(ManualConflictValue.from(conflict("true", "false", "item:abil_a/field:active")))
        assertEquals("false", boolean.encode(listOf("false")))
        assertNull(boolean.encode(listOf("yes")))
        val decimal = requireNotNull(ManualConflictValue.from(conflict("1.5", "2.0", "item:inv_a/field:load")))
        assertEquals("2.25", decimal.encode(listOf("2.25")))
        listOf("NaN", "Infinity", "1e999").forEach { assertNull(decimal.encode(listOf(it))) }
    }

    @Test fun resourceEditsValidateEveryMemberAndPreserveSignedAdjustment() {
        val editor = requireNotNull(ManualConflictValue.from(conflict(
            "{\"current\":10,\"maximum\":20,\"adjustment\":-2}", key = "field:life")))
        assertEquals(listOf("10", "20", "-2"), editor.initialValues)
        assertEquals(Json.parseToJsonElement("{\"current\":12,\"maximum\":20,\"adjustment\":-1}"),
            Json.parseToJsonElement(requireNotNull(editor.encode(listOf("12", "20", "-1")))))
        assertNull(editor.encode(listOf("-1", "20", "0")))
        assertNull(editor.encode(listOf("10", "-1", "0")))
        assertNull(editor.encode(listOf("10", "20", "2147483648")))
    }

    @Test fun structuredAndMismatchedShapesCannotBeEditedAsPlainText() {
        listOf(conflict("[]"), conflict("{}"), conflict("null"), conflict("1", "\"text\""),
            conflict("\"{}\"", key = "field:canonicalBodyPayload"),
            conflict("\"value\"", key = "sheet")).forEach { assertNull(ManualConflictValue.from(it)) }
    }
}
