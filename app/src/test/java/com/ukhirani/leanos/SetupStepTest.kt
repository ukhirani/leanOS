package com.ukhirani.leanos

import com.ukhirani.leanos.onboarding.SetupStep
import com.ukhirani.leanos.onboarding.SetupStep.*
import com.ukhirani.leanos.onboarding.nextSetupStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SetupStepTest {
    @Test
    fun `starts with the default launcher and walks the steps in order`() {
        assertEquals(DefaultLauncher, nextSetupStep(emptySet(), emptySet()))
        assertEquals(UsageAccess, nextSetupStep(setOf(DefaultLauncher), emptySet()))
    }

    @Test
    fun `a skipped step is passed over`() {
        assertEquals(NotificationAccess, nextSetupStep(setOf(DefaultLauncher), setOf(UsageAccess)))
    }

    @Test
    fun `a step already granted is never shown`() {
        assertEquals(WeatherCity, nextSetupStep(setOf(UsageAccess, NotificationAccess, Accessibility, DefaultLauncher), emptySet()))
    }

    @Test
    fun `done once every step is granted or skipped`() {
        assertNull(nextSetupStep(setOf(DefaultLauncher, UsageAccess), SetupStep.entries.toSet() - setOf(DefaultLauncher, UsageAccess)))
    }
}
