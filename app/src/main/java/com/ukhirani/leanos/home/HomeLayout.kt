package com.ukhirani.leanos.home

import com.ukhirani.leanos.platform.BatteryState
import com.ukhirani.leanos.platform.NextAlarm
import com.ukhirani.leanos.platform.Settings

/** One chip under the clock: what it shows and what TalkBack announces. */
data class GlanceChip(val kind: Kind, val text: String, val description: String) {
    enum class Kind { Date, Battery, Alarm }
}

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
}
