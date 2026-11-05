package com.ukhirani.leanos.platform

import android.app.ActivityOptions
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** What the active media session is playing; [playing] covers buffering too. Artwork is null when the app gives none. */
data class NowPlaying(
    val packageName: String,
    val title: String,
    val artist: String?,
    val artwork: Bitmap? = null,
    val playing: Boolean,
)

/** The active media session and its transport controls. Needs notification access. */
interface MediaSource {
    fun hasAccess(): Boolean

    /** The current session, or null when none is active. Emits the current value, then every change. */
    val nowPlaying: Flow<NowPlaying?>

    fun playPause()
    fun next()
    fun previous()

    /** Opens the app that's playing. */
    fun open()
}

/**
 * Exists only so MediaSessionManager.getActiveSessions works: Android grants media sessions to an enabled
 * notification listener. It overrides nothing, so it never reads or stores notification content.
 */
class MediaListenerService : NotificationListenerService()

class AndroidMediaSource(context: Context) : MediaSource {
    private val context = context.applicationContext
    private val sessions = this.context.getSystemService(MediaSessionManager::class.java)
    private val listener = ComponentName(this.context, MediaListenerService::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var controller: MediaController? = null

    override fun hasAccess(): Boolean =
        context.getSystemService(NotificationManager::class.java).isNotificationListenerAccessGranted(listener)

    override val nowPlaying: Flow<NowPlaying?> = callbackFlow {
        if (!hasAccess()) {
            send(null)
            awaitClose()
            return@callbackFlow
        }
        // This collection's own controller: a finished collection must not clear a newer one's.
        var mine: MediaController? = null
        val callback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) { trySend(mine?.toNowPlaying()) }
            override fun onPlaybackStateChanged(state: PlaybackState?) { trySend(mine?.toNowPlaying()) }
            override fun onSessionDestroyed() { trySend(null) }
        }
        fun pick(active: List<MediaController>?) {
            mine?.unregisterCallback(callback)
            // Prefer a session that's playing; the system lists the rest most recent first.
            mine = active.orEmpty().let { list -> list.firstOrNull { it.isPlaying() } ?: list.firstOrNull() }
            controller = mine
            mine?.registerCallback(callback, main)
            trySend(mine?.toNowPlaying())
        }
        val changed = MediaSessionManager.OnActiveSessionsChangedListener { pick(it) }
        sessions.addOnActiveSessionsChangedListener(changed, listener, main)
        pick(sessions.getActiveSessions(listener))
        awaitClose {
            sessions.removeOnActiveSessionsChangedListener(changed)
            mine?.unregisterCallback(callback)
            if (controller === mine) controller = null
        }
    }

    override fun playPause() {
        val c = controller ?: return
        if (c.isPlaying()) c.transportControls.pause() else c.transportControls.play()
    }

    override fun next() { controller?.transportControls?.skipToNext() }
    override fun previous() { controller?.transportControls?.skipToPrevious() }

    override fun open() {
        val c = controller ?: return
        try {
            val pending = c.sessionActivity ?: return launch(c.packageName)
            // Android 14+ only lets the player's intent start an activity if we, the visible app, opt in.
            val options = if (Build.VERSION.SDK_INT >= 34) {
                @Suppress("DEPRECATION")
                ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                    .toBundle()
            } else {
                null
            }
            pending.send(context, 0, null, null, null, null, options)
        } catch (_: PendingIntent.CanceledException) {
            launch(c.packageName)
        }
    }

    private fun launch(packageName: String) {
        context.packageManager.getLaunchIntentForPackage(packageName)
            ?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    private fun MediaController.toNowPlaying(): NowPlaying? {
        val meta = metadata ?: return null
        val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE) ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: return null
        return NowPlaying(
            packageName = packageName,
            title = title,
            artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE),
            artwork = meta.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: meta.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON),
            playing = isPlaying(),
        )
    }

    private fun MediaController.isPlaying() =
        playbackState?.state.let { it == PlaybackState.STATE_PLAYING || it == PlaybackState.STATE_BUFFERING }
}
