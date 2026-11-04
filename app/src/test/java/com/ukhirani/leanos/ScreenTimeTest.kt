package com.ukhirani.leanos

import com.ukhirani.leanos.usage.AppUsage
import com.ukhirani.leanos.usage.ScreenTime
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenTimeTest {
    // Wed 4 Nov 2026, 22:00 in Paris; local midnight was 00:00+01:00.
    private val clock = FakeClockSource("2026-11-04T22:00:00+01:00[Europe/Paris]")
    private val usage = FakeUsageSource()
    private val screenTime = ScreenTime(usage, clock, ownPackage = "com.ukhirani.leanos")

    private fun t(hhmm: String) = "2026-11-04T$hhmm:00+01:00[Europe/Paris]"

    @Test
    fun `sums each app's foreground sessions and sorts apps by time`() {
        usage.resumed("maps", t("08:00")); usage.paused("maps", t("08:10"))
        usage.resumed("chat", t("09:00")); usage.paused("chat", t("09:30"))
        usage.resumed("maps", t("12:00")); usage.paused("maps", t("12:05"))
        val today = screenTime.today()!!
        assertEquals(listOf(AppUsage("chat", Duration.ofMinutes(30)), AppUsage("maps", Duration.ofMinutes(15))), today.apps)
        assertEquals(Duration.ofMinutes(45), today.total)
    }

    @Test
    fun `a session still open counts up to now`() {
        usage.resumed("video", t("21:20"))
        assertEquals(listOf(AppUsage("video", Duration.ofMinutes(40))), screenTime.today()!!.apps)
    }

    @Test
    fun `a session that began before midnight counts from midnight`() {
        usage.resumed("chat", "2026-11-03T23:50:00+01:00[Europe/Paris]")
        usage.paused("chat", t("00:20"))
        usage.resumed("chat", t("07:00")); usage.paused("chat", t("07:05"))
        assertEquals(listOf(AppUsage("chat", Duration.ofMinutes(25))), screenTime.today()!!.apps)
    }

    @Test
    fun `time on LeanOS itself is not counted`() {
        usage.resumed("com.ukhirani.leanos", t("10:00")); usage.paused("com.ukhirani.leanos", t("10:30"))
        usage.resumed("maps", t("10:30")); usage.paused("maps", t("10:32"))
        val today = screenTime.today()!!
        assertEquals(listOf("maps"), today.apps.map { it.packageName })
        assertEquals(Duration.ofMinutes(2), today.total)
    }

    @Test
    fun `top 3 are the three longest, the total still counts every app`() {
        listOf("a" to 5L, "b" to 40L, "c" to 1L, "d" to 20L).forEachIndexed { i, (pkg, minutes) ->
            usage.resumed(pkg, t("1$i:00")); usage.paused(pkg, t("1$i:%02d".format(minutes)))
        }
        val today = screenTime.today()!!
        assertEquals(listOf("b", "d", "a"), today.top3.map { it.packageName })
        assertEquals(Duration.ofMinutes(66), today.total)
    }

    @Test
    fun `without usage access there is no screen time`() {
        usage.resumed("maps", t("08:00"))
        usage.access = false
        assertNull(screenTime.today())
    }
}
