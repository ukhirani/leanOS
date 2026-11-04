package com.ukhirani.leanos

import com.ukhirani.leanos.home.AppTime
import com.ukhirani.leanos.home.GlanceCard
import com.ukhirani.leanos.home.GlanceChip
import com.ukhirani.leanos.home.GlanceFormat
import com.ukhirani.leanos.home.HomeLayout
import com.ukhirani.leanos.platform.BatteryState
import com.ukhirani.leanos.platform.NextAlarm
import com.ukhirani.leanos.platform.NowPlaying
import com.ukhirani.leanos.platform.Settings
import com.ukhirani.leanos.usage.AppUsage
import com.ukhirani.leanos.usage.ScreenTimeToday
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeLayoutTest {
    private val clock = FakeClockSource("2026-11-04T22:47:30+01:00[Europe/Paris]")
    private val layout = HomeLayout(GlanceFormat(clock, Locale.US))
    private val alarms = FakeAlarmSource()
    private val batteries = FakeBatterySource(BatteryState(level = 82, charging = true))

    private suspend fun chips(settings: Settings = Settings()) =
        layout.chips(settings, alarms.nextAlarm.first(), batteries.battery.first())

    @Test
    fun `chips show date, battery and next alarm in that order`() = runBlocking {
        alarms.set(NextAlarm(ZonedDateTime.parse("2026-11-05T07:00:00+01:00[Europe/Paris]").toInstant()))
        assertEquals(
            listOf(
                GlanceChip(GlanceChip.Kind.Date, "Wed, 4 Nov", "Wed, 4 Nov"),
                GlanceChip(GlanceChip.Kind.Battery, "⚡ 82%", "Battery 82%, charging"),
                GlanceChip(GlanceChip.Kind.Alarm, "⏰ 7:00 · in 8h 13m", "Next alarm 7:00 · in 8h 13m"),
            ),
            chips(),
        )
    }

    @Test
    fun `alarm chip is hidden when no alarm is set`() = runBlocking {
        assertEquals(listOf(GlanceChip.Kind.Date, GlanceChip.Kind.Battery), chips().map { it.kind })
    }

    @Test
    fun `alarm chip appears and goes as alarms are set and dismissed`() = runBlocking {
        alarms.set(NextAlarm(ZonedDateTime.parse("2026-11-04T23:00:00+01:00[Europe/Paris]").toInstant()))
        assertEquals("⏰ 23:00 · in 13m", chips().last().text)
        alarms.set(null)
        assertEquals(GlanceChip.Kind.Battery, chips().last().kind)
    }

    @Test
    fun `battery chip is hidden until the battery has reported`() {
        assertEquals(listOf(GlanceChip.Kind.Date), layout.chips(Settings(), null, null).map { it.kind })
    }

    @Test
    fun `battery not charging is described plainly`() = runBlocking {
        batteries.set(BatteryState(level = 15, charging = false))
        assertEquals(GlanceChip(GlanceChip.Kind.Battery, "15%", "Battery 15%"), chips()[1])
    }

    private fun usage(vararg apps: Pair<String, Long>) = ScreenTimeToday(
        total = Duration.ofMinutes(apps.sumOf { it.second }),
        apps = apps.map { (pkg, minutes) -> AppUsage(pkg, Duration.ofMinutes(minutes)) },
    )

    @Test
    fun `screen time card shows today's total, the top 3 apps and the full list`() {
        val card = layout.cards(usageAccess = true, screenTime = usage("chat" to 50L, "maps" to 15L, "mail" to 7L, "news" to 3L))
        assertEquals(
            listOf(
                GlanceCard.ScreenTime(
                    total = "1h 15m",
                    top = listOf(AppTime("chat", "50m"), AppTime("maps", "15m"), AppTime("mail", "7m")),
                    all = listOf(AppTime("chat", "50m"), AppTime("maps", "15m"), AppTime("mail", "7m"), AppTime("news", "3m")),
                ),
            ),
            card,
        )
    }

    @Test
    fun `without usage access the screen time card is swapped for a tap to enable chip`() {
        assertEquals(listOf(GlanceCard.Enable(GlanceCard.Feature.ScreenTime)), layout.cards(usageAccess = false, screenTime = null))
    }

    @Test
    fun `screen time card waits for its first reading`() {
        assertEquals(emptyList<GlanceCard>(), layout.cards(usageAccess = true, screenTime = null))
    }

    private val media = FakeMediaSource()

    private suspend fun mediaCards() = layout.cards(
        usageAccess = true, screenTime = null, mediaAccess = media.hasAccess(), nowPlaying = media.nowPlaying.first(),
    )

    @Test
    fun `media card shows what's playing only while a session is active`() = runBlocking {
        assertEquals(emptyList<GlanceCard>(), mediaCards())
        media.play(NowPlaying("pkg.music", title = "Song", artist = "Band", playing = true))
        assertEquals(listOf(GlanceCard.Media("pkg.music", "Song", "Band", null, playing = true)), mediaCards())
        media.play(null)
        assertEquals(emptyList<GlanceCard>(), mediaCards())
    }

    @Test
    fun `paused media keeps its card so it can be resumed`() = runBlocking {
        media.play(NowPlaying("pkg.music", title = "Song", artist = null, playing = false))
        assertEquals(listOf(GlanceCard.Media("pkg.music", "Song", null, null, playing = false)), mediaCards())
    }

    @Test
    fun `without notification access the media card is swapped for a tap to enable chip`() = runBlocking {
        media.access = false
        media.play(NowPlaying("pkg.music", title = "Song", artist = "Band", playing = true))
        assertEquals(listOf(GlanceCard.Enable(GlanceCard.Feature.Media)), mediaCards())
    }

    @Test
    fun `media card follows the screen time card`() {
        val cards = layout.cards(
            usageAccess = false, screenTime = null, mediaAccess = true,
            nowPlaying = NowPlaying("pkg.music", title = "Song", artist = "Band", playing = true),
        )
        assertEquals(listOf(GlanceCard.Enable::class, GlanceCard.Media::class), cards.map { it::class })
    }
}
