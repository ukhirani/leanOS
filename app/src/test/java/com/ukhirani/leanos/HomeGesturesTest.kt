package com.ukhirani.leanos

import com.ukhirani.leanos.home.GestureOutcome
import com.ukhirani.leanos.home.HomeGestures
import com.ukhirani.leanos.home.SwipeDirection
import com.ukhirani.leanos.home.swipeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeGesturesTest {
    private val service = FakeFocusService()
    private val gestures = HomeGestures(service)

    @Test
    fun `swipe down opens the notification shade through the Focus service`() {
        assertEquals(GestureOutcome.Done, gestures.onSwipeDown())
        assertEquals(listOf("notifications"), service.performed)
    }

    @Test
    fun `double-tap locks the screen through the Focus service`() {
        assertEquals(GestureOutcome.Done, gestures.onDoubleTap())
        assertEquals(listOf("lock"), service.performed)
    }

    @Test
    fun `without the Focus service both gestures ask to enable it and do nothing`() {
        service.running = false
        assertEquals(GestureOutcome.EnableServicePrompt, gestures.onSwipeDown())
        assertEquals(GestureOutcome.EnableServicePrompt, gestures.onDoubleTap())
        assertEquals(emptyList<String>(), service.performed)
    }

    @Test
    fun `a global action the system refuses also asks to enable the service`() {
        service.refuseActions = true
        assertEquals(GestureOutcome.EnableServicePrompt, gestures.onDoubleTap())
    }

    @Test
    fun `a mostly vertical drag past the threshold is a swipe in its direction`() {
        assertEquals(SwipeDirection.Down, swipeDirection(totalDx = 10f, totalDy = 200f, threshold = 100f))
        assertEquals(SwipeDirection.Up, swipeDirection(totalDx = -10f, totalDy = -200f, threshold = 100f))
    }

    @Test
    fun `short or mostly sideways drags are not swipes`() {
        assertNull(swipeDirection(totalDx = 0f, totalDy = 60f, threshold = 100f))
        assertNull(swipeDirection(totalDx = 300f, totalDy = 200f, threshold = 100f))
    }
}
