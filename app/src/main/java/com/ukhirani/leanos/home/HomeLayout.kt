package com.ukhirani.leanos.home

import android.graphics.Bitmap
import com.ukhirani.leanos.platform.BatteryState
import com.ukhirani.leanos.platform.NextAlarm
import com.ukhirani.leanos.platform.NowPlaying
import com.ukhirani.leanos.platform.Settings
import com.ukhirani.leanos.usage.ScreenTimeToday

/** One chip under the clock: what it shows and what TalkBack announces. */
data class GlanceChip(val kind: Kind, val text: String, val description: String) {
    enum class Kind { Date, Battery, Alarm }
}

/** One glance card below the chips, or the "tap to enable" chip standing in for a card that lacks its permission. */
sealed interface GlanceCard {
    enum class Feature { ScreenTime, Media }

    data class Enable(val feature: Feature) : GlanceCard

    /** Today's total, the top 3 apps, and the full list shown when expanded. */
    data class ScreenTime(val total: String, val top: List<AppTime>, val all: List<AppTime>) : GlanceCard

    /** What's playing, with artwork when the app gives one. */
    data class Media(val packageName: String, val title: String, val artist: String?, val artwork: Bitmap?, val playing: Boolean) : GlanceCard
}

data class AppTime(val packageName: String, val time: String)

/** Decides what the home screen shows. */
class HomeLayout(private val format: GlanceFormat) {

    /** Date · battery · alarm. Battery waits for its first reading; alarm shows only while one is set. */
    fun chips(settings: Settings, alarm: NextAlarm?, battery: BatteryState?): List<GlanceChip> = buildList {
        val date = format.dateText()
        add(GlanceChip(GlanceChip.Kind.Date, date, date))
        if (battery != null) {
            val description = "Battery ${battery.level}%" + if (battery.charging) ", charging" else ""
            add(GlanceChip(GlanceChip.Kind.Battery, format.batteryText(battery), description))
        }
        format.alarmText(alarm?.at, settings)?.let {
            add(GlanceChip(GlanceChip.Kind.Alarm, it, "Next alarm " + it.removePrefix("⏰ ")))
        }
    }

    /** Glance cards in order: screen time, then media. Screen time waits for its first reading; without usage access it is an enable chip. */
    fun cards(
        usageAccess: Boolean,
        screenTime: ScreenTimeToday?,
        mediaAccess: Boolean = true,
        nowPlaying: NowPlaying? = null,
    ): List<GlanceCard> = buildList {
        when {
            !usageAccess -> add(GlanceCard.Enable(GlanceCard.Feature.ScreenTime))
            screenTime != null -> {
                val all = screenTime.apps.map { AppTime(it.packageName, format.durationText(it.duration)) }
                add(GlanceCard.ScreenTime(format.durationText(screenTime.total), all.take(3), all))
            }
        }
        // Media shows only while a session is active; without notification access it is an enable chip.
        when {
            !mediaAccess -> add(GlanceCard.Enable(GlanceCard.Feature.Media))
            nowPlaying != null -> with(nowPlaying) { add(GlanceCard.Media(packageName, title, artist, artwork, playing)) }
        }
    }
}
