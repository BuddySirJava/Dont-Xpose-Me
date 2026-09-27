// SPDX-License-Identifier: Apache-2.0

package me.dontxpose.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TestEventLogTest {
    @Test
    fun parseNewestFirst() {
        val text = "1000\ttest_match\t4\n2000\ttest_match\t6\n"
        val events = TestEventLog.parse(text)
        assertEquals(2, events.size)
        assertEquals(2000L, events[0].atMs)
        assertEquals(6, events[0].pinLength)
        assertEquals(1000L, events[1].atMs)
    }

    @Test
    fun parseIgnoresBlankAndBadLines() {
        assertTrue(TestEventLog.parse("").isEmpty())
        assertTrue(TestEventLog.parse("not-a-line\n").isEmpty())
    }
}
