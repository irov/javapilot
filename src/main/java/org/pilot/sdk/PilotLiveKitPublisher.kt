package org.pilot.sdk

import android.content.Context
import android.content.Intent
import io.livekit.android.LiveKit
import io.livekit.android.RoomOptions
import io.livekit.android.room.Room
import io.livekit.android.room.participant.VideoTrackPublishDefaults
import io.livekit.android.room.track.screencapture.ScreenCaptureParams
import kotlinx.coroutines.runBlocking

internal class PilotLiveKitPublisher(context: Context) {
    private val appContext = context.applicationContext

    private var room: Room? = null

    @Synchronized
    @Throws(Exception::class)
    fun start(serverUrl: String, participantToken: String) {
        stop()

        val createdRoom = LiveKit.create(
            appContext = appContext,
            options = RoomOptions(
                adaptiveStream = false,
                dynacast = false,
                screenShareTrackPublishDefaults = VideoTrackPublishDefaults(simulcast = false),
            ),
        )

        try {
            runBlocking {
                createdRoom.connect(serverUrl, participantToken)
            }
            room = createdRoom
        } catch (throwable: Throwable) {
            try {
                createdRoom.release()
            } catch (_: Exception) {
            }
            throw throwable
        }
    }

    @Synchronized
    @Throws(Exception::class)
    fun enableScreenShare(mediaProjectionData: Intent) {
        val activeRoom = room ?: throw IllegalStateException("Room not connected")
        val params = ScreenCaptureParams(mediaProjectionPermissionResultData = mediaProjectionData)
        runBlocking {
            activeRoom.localParticipant.setScreenShareEnabled(true, params)
        }
    }

    @Synchronized
    fun stop() {
        val activeRoom = room
        room = null
        if (activeRoom != null) {
            try {
                runBlocking {
                    activeRoom.localParticipant.setScreenShareEnabled(false)
                }
            } catch (_: Exception) {
            }
            try {
                activeRoom.release()
            } catch (_: Exception) {
            }
        }
    }
}