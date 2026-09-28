package com.nvllz.stepsy.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class MeasureFormatTest {

    @Test
    fun wholeValuesDropTheDecimal() {
        Locale.setDefault(Locale.US)
        assertEquals("180", Util.formatMeasure(180.0))
        assertEquals("72.5", Util.formatMeasure(72.5))
    }

    @Test
    fun commaAndDotBothParse() {
        assertEquals(72.5, Util.parseMeasure("72,5")!!, 1e-9)
        assertEquals(172.3, Util.parseMeasure(" 172.3 ")!!, 1e-9)
        assertNull(Util.parseMeasure("abc"))
    }

    @Test
    fun decimalHeightFeedsStepLength() {
        assertEquals(172.5 * 0.415, Util.estimateStepLength(172.5, null).toDouble(), 1e-4)
    }
}
