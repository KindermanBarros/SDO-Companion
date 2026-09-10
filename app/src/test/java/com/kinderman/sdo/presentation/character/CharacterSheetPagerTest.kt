package com.kinderman.sdo.presentation.character

import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterSheetPagerTest {
    @Test
    fun `mystic page is immediately to the right of powers`() {
        val pages = SheetPage.entries
        val powersIndex = pages.indexOf(SheetPage.POWERS)

        assertEquals(SheetPage.MYSTIC, pages[powersIndex + 1])
    }

    @Test
    fun `inventory page is immediately to the right of mystic`() {
        val pages = SheetPage.entries
        val mysticIndex = pages.indexOf(SheetPage.MYSTIC)

        assertEquals(SheetPage.INVENTORY, pages[mysticIndex + 1])
    }
}
