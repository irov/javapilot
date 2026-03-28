package org.pilot.sdk

import android.content.Context
import android.graphics.Bitmap
import io.livekit.android.LiveKit
import io.livekit.android.RoomOptions
import io.livekit.android.room.Room
import io.livekit.android.room.participant.VideoTrackPublishDefaults
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.LocalVideoTrackOptions
import io.livekit.android.room.track.VideoCaptureParameter
import io.livekit.android.room.track.video.BitmapFrameCapturer
import kotlinx.coroutines.runBlocking
import java.util.ArrayDeque

internal class PilotLiveKitPublisher(context: Context) {
    private val appContext = context.applicationContext
    private val pendingBitmaps = ArrayDeque<Bitmap>()

    private var room: Room? = null
    private var capturer: BitmapFrameCapturer? = null
    private var videoTrack: LocalVideoTrack? = null

    @Synchronized
    @Throws(Exception::class)
    fun start(
        serverUrl: String,
        participantToken: String,
        trackName: String,
        maxDimension: Int,
        fps: Int,
    ) {
        stop()

        val captureOptions = LocalVideoTrackOptions(
            isScreencast = true,
            captureParams = VideoCaptureParameter(maxDimension, maxDimension, fps, false),
        )
        val createdRoom = LiveKit.create(
            appContext = appContext,
            options = RoomOptions(
                adaptiveStream = false,
                dynacast = false,
                screenShareTrackPublishDefaults = VideoTrackPublishDefaults(simulcast = false),
            ),
        )

        var createdTrack: LocalVideoTrack? = null
        try {
            runBlocking {
                createdRoom.connect(serverUrl, participantToken)
            }

            val bitmapCapturer = BitmapFrameCapturer()
            createdTrack = createdRoom.localParticipant.createVideoTrack(
                name = trackName,
                capturer = bitmapCapturer,
                options = captureOptions,
            )
            createdTrack.startCapture()

            val published = runBlocking {
                createdRoom.localParticipant.publishVideoTrack(createdTrack)
            }
            check(published) { "Failed to publish LiveKit video track" }

            room = createdRoom
            capturer = bitmapCapturer
            videoTrack = createdTrack
        } catch (throwable: Throwable) {
            try {
                createdTrack?.dispose()
            } catch (_: Exception) {
            }

            try {
                createdRoom.release()
            } catch (_: Exception) {
            }

            throw throwable
        }
    }

    @Synchronized
    fun pushBitmap(bitmap: Bitmap) {
        val activeCapturer = capturer
        if (activeCapturer == null) {
            recycleBitmap(bitmap)
            return
        }

        activeCapturer.pushBitmap(bitmap, 0)
        pendingBitmaps.addLast(bitmap)

        while (pendingBitmaps.size > MAX_PENDING_BITMAPS) {
            recycleBitmap(pendingBitmaps.removeFirst())
        }
    }

    @Synchronized
    fun stop() {
        pendingBitmaps.clear()

        try {
            videoTrack?.stopCapture()
        } catch (_: Exception) {
        }

        try {
            videoTrack?.stop()
        } catch (_: Exception) {
        }

        try {
            videoTrack?.dispose()
        } catch (_: Exception) {
        }

        videoTrack = null
        capturer = null

        val activeRoom = room
        room = null
        if (activeRoom != null) {
            try {
                activeRoom.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun recycleBitmap(bitmap: Bitmap) {
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }

    private companion object {
        const val MAX_PENDING_BITMAPS = 3
    }
}