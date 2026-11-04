package com.ukhirani.leanos

import com.ukhirani.leanos.usage.LimitDecision
import com.ukhirani.leanos.usage.LimitPolicy
import com.ukhirani.leanos.usage.ScreenTime
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LimitPolicyTest {
    // Wed 4 Nov 2026, 22:00 in Paris.
    private val clock = FakeClockSource("2026-11-04T22:00:00+01:00[Europe/Paris]")
    private val usage = FakeUsageSource()
    private val policy = LimitPolicy(ScreenTime(usage, clock, ownPackage = "com.ukhirani.leanos"), clock)
    private val parisMidnight = ZonedDateTime.parse("2026-11-05T00:00:00+01:00[Europe/Paris]")

    private fun t(hhmm: String) = "2026-11-04T$hhmm:00+01:00[Europe/Paris]"

    @Test
    fun `an app without a limit is unlimited`() {
        usage.resumed("video", t("08:00")); usage.paused("video", t("12:00"))
        assertEquals(LimitDecision.Unlimited, policy.decide("video", mapOf("chat" to 30)))
    }

    @Test
    fun `under the limit the app is allowed with the time remaining`() {
        usage.resumed("video", t("08:00")); usage.paused("video", t("08:20"))
        assertEquals(LimitDecision.Allowed(Duration.ofMinutes(10)), policy.decide("video", mapOf("video" to 30)))
    }

    @Test
    fun `at the limit the app is blocked until local midnight`() {
        usage.resumed("video", t("08:00")); usage.paused("video", t("08:30"))
        assertEquals(
            LimitDecision.Blocked(used = Duration.ofMinutes(30), unlocksAt = parisMidnight),
            policy.decide("video", mapOf("video" to 30)),
        )
    }

    @Test
    fun `over the limit, counting a session still open, the app is blocked`() {
        usage.resumed("video", t("21:00"))
        assertEquals(
            LimitDecision.Blocked(used = Duration.ofMinutes(60), unlocksAt = parisMidnight),
            policy.decide("video", mapOf("video" to 45)),
        )
    }

    @Test
    fun `the limit resets at local midnight`() {
        usage.resumed("video", t("20:00")); usage.paused("video", t("21:00"))
        clock.current = ZonedDateTime.parse("2026-11-05T00:01:00+01:00[Europe/Paris]")
        assertEquals(LimitDecision.Allowed(Duration.ofMinutes(30)), policy.decide("video", mapOf("video" to 30)))
    }

    @Test
    fun `after a time-zone change the day starts at the new zone's midnight`() {
        usage.resumed("video", t("08:00")); usage.paused("video", t("09:00"))
        // Flying east: 23:30 in Paris is already 07:30 tomorrow in Tokyo, a fresh day there.
        clock.current = ZonedDateTime.parse("2026-11-05T07:30:00+09:00[Asia/Tokyo]")
        assertEquals(LimitDecision.Allowed(Duration.ofMinutes(30)), policy.decide("video", mapOf("video" to 30)))
        // And it unlocks at Tokyo's midnight, not Paris's.
        usage.resumed("video", "2026-11-05T07:30:00+09:00[Asia/Tokyo]")
        clock.current = ZonedDateTime.parse("2026-11-05T08:00:00+09:00[Asia/Tokyo]")
        assertEquals(
            LimitDecision.Blocked(Duration.ofMinutes(30), ZonedDateTime.parse("2026-11-06T00:00:00+09:00[Asia/Tokyo]")),
            policy.decide("video", mapOf("video" to 30)),
        )
    }

    @Test
    fun `without usage access limits cannot be enforced`() {
        usage.access = false
        assertEquals(LimitDecision.Unlimited, policy.decide("video", mapOf("video" to 30)))
    }

    private fun at(hhmm: String): Instant = ZonedDateTime.parse(t(hhmm)).toInstant()

    @Test
    fun `a blocked app can be extended once today`() {
        usage.resumed("video", t("08:00")); usage.paused("video", t("08:30"))
        assertTrue((policy.decide("video", mapOf("video" to 30)) as LimitDecision.Blocked).canExtend)
    }

    @Test
    fun `an extension allows exactly five more minutes from when it was granted`() {
        usage.resumed("video", t("21:00"))
        clock.current = ZonedDateTime.parse(t("21:30"))
        val extended = mapOf("video" to at("21:30"))
        assertEquals(LimitDecision.Allowed(Duration.ofMinutes(5)), policy.decide("video", mapOf("video" to 30), extended))
        clock.current = ZonedDateTime.parse(t("21:33"))
        assertEquals(LimitDecision.Allowed(Duration.ofMinutes(2)), policy.decide("video", mapOf("video" to 30), extended))
    }

    @Test
    fun `once the extension is used up the app is blocked with no further extension`() {
        usage.resumed("video", t("21:00"))
        clock.current = ZonedDateTime.parse(t("21:35"))
        val decision = policy.decide("video", mapOf("video" to 30), mapOf("video" to at("21:30")))
        assertEquals(LimitDecision.Blocked(Duration.ofMinutes(35), parisMidnight, canExtend = false), decision)
    }

    @Test
    fun `an extension is per app`() {
        usage.resumed("video", t("08:00")); usage.paused("video", t("08:30"))
        val decision = policy.decide("video", mapOf("video" to 30), mapOf("chat" to at("21:00")))
        assertTrue((decision as LimitDecision.Blocked).canExtend)
    }

    @Test
    fun `yesterday's extension doesn't count, so a new one is available after midnight`() {
        usage.resumed("video", "2026-11-05T00:00:00+01:00[Europe/Paris]")
        clock.current = ZonedDateTime.parse("2026-11-05T00:30:00+01:00[Europe/Paris]")
        val decision = policy.decide("video", mapOf("video" to 30), mapOf("video" to at("23:58")))
        assertTrue((decision as LimitDecision.Blocked).canExtend)
    }
}
