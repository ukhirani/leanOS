package com.ukhirani.leanos.onboarding

/** The first-run walkthrough's steps, in the order they are offered. */
enum class SetupStep { DefaultLauncher, UsageAccess, NotificationAccess, Accessibility, WeatherCity }

/** The next step to show: the first one neither [granted] nor [skipped]; null when the walkthrough is done. */
fun nextSetupStep(granted: Set<SetupStep>, skipped: Set<SetupStep>): SetupStep? =
    SetupStep.entries.firstOrNull { it !in granted && it !in skipped }
