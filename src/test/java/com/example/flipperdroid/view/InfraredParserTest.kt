package com.example.flipperdroid.view

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InfraredParserTest {
    @Test
    fun parsesStrictPositiveTimingList() {
        assertArrayEquals(intArrayOf(9000, 4500, 560, 560), parseIrPattern("9000, 4500 560;560"))
    }

    @Test
    fun rejectsInvalidOrOutOfRangeTimings() {
        assertNull(parseIrPattern("9000,abc,560"))
        assertNull(parseIrPattern("9000,0,560"))
        assertNull(parseIrPattern("9000,200001,560"))
        assertNull(parseIrPattern(""))
    }

    @Test
    fun necFrameHasExpectedHeaderAndLength() {
        val frame = buildNecPattern(0x00, 0x10)
        assertEquals(67, frame.size)
        assertEquals(9000, frame[0])
        assertEquals(4500, frame[1])
        assertEquals(560, frame.last())
    }
}
