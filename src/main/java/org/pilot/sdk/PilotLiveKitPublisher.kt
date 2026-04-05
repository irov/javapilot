package org.pilot.sdk

import android.content.Context
import android.content.Intent
import android.util.DisplayMetrics
import io.livekit.android.LiveKit
import io.livekit.android.RoomOptions
import io.livekit.android.room.Room
import io.livekit.android.room.participant.VideoTrackPublishDefaults
import io.livekit.android.room.participant.VideoTrackPublishOptions
import io.livekit.android.room.track.LocalScreencastVideoTrack
import io.livekit.android.room.track.LocalVideoTrackOptions
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoCaptureParameter
import io.livekit.android.room.track.VideoEncoding
import io.livekit.android.room.track.screencapture.ScreenCaptureParams
import kotlinx.coroutines.runBlocking
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal class PilotLiveKitPublisher(context: Context) {
    private val appContext = context.applicationContext

    private var room: Room? = null
    private var currentQuality = LiveQuality.default()

    @Synchronized
    @Throws(Exception::class)
    fun start(
        serverUrl: String,
        participantToken: String,
        presetName: String,
        maxDimension: Int,
        framesPerSecond: Int
    ) {
        stop()

        val initialQuality = LiveQuality(
            presetName = presetName,
            maxDimension = maxDimension,
            framesPerSecond = framesPerSecond
        )
        val captureDefaults = createCaptureOptions(initialQuality)
        val publishDefaults = createPublishDefaults(initialQuality)

        val createdRoom = LiveKit.create(
            appContext = appContext,
            options = RoomOptions(
                adaptiveStream = false,
                dynacast = false,
                screenShareTrackCaptureDefaults = captureDefaults,
                screenShareTrackPublishDefaults = publishDefaults
            )
        )

        try {
            runBlocking {
                createdRoom.connect(serverUrl, participantToken)
            }
            room = createdRoom
            currentQuality = initialQuality
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
    @Throws(Exception::class)
    fun updateQuality(
        presetName: String,
        maxDimension: Int,
        framesPerSecond: Int
    ): Boolean {
        val activeRoom = room ?: throw IllegalStateException("Room not connected")
        val nextQuality = LiveQuality(
            presetName = presetName,
            maxDimension = maxDimension,
            framesPerSecond = framesPerSecond
        )
        val nextCaptureDefaults = createCaptureOptions(nextQuality)
        val nextPublishDefaults = createPublishDefaults(nextQuality)

        val previousQuality = currentQuality
        val previousCaptureDefaults = activeRoom.screenShareTrackCaptureDefaults
        val previousPublishDefaults = activeRoom.screenShareTrackPublishDefaults

        activeRoom.screenShareTrackCaptureDefaults = nextCaptureDefaults
        activeRoom.screenShareTrackPublishDefaults = nextPublishDefaults

        val screenShareTrack = activeRoom.localParticipant
            .getTrackPublication(Track.Source.SCREEN_SHARE)
            ?.track as? LocalScreencastVideoTrack

        if (screenShareTrack == null) {
            currentQuality = nextQuality
            return false
        }

        val previousTrackOptions = screenShareTrack.options

        try {
            runBlocking {
                activeRoom.localParticipant.unpublishTrack(screenShareTrack, false)
                reconfigureScreenShareTrack(screenShareTrack, nextCaptureDefaults)
                val published = activeRoom.localParticipant.publishVideoTrack(
                    screenShareTrack,
                    VideoTrackPublishOptions(null, nextPublishDefaults)
                )
                if (!published) {
                    throw IllegalStateException("Failed to republish screen share track")
                }
            }
            currentQuality = nextQuality
            return true
        } catch (throwable: Throwable) {
            activeRoom.screenShareTrackCaptureDefaults = previousCaptureDefaults
            activeRoom.screenShareTrackPublishDefaults = previousPublishDefaults

            try {
                reconfigureScreenShareTrack(screenShareTrack, previousTrackOptions)
            } catch (_: Exception) {
            }

            try {
                runBlocking {
                    activeRoom.localParticipant.publishVideoTrack(
                        screenShareTrack,
                        VideoTrackPublishOptions(null, previousPublishDefaults)
                    )
                }
            } catch (_: Exception) {
            }

            currentQuality = previousQuality
            throw throwable
        }
    }

    @Synchronized
    fun stop() {
        val activeRoom = room
        room = null
        currentQuality = LiveQuality.default()
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

    private fun reconfigureScreenShareTrack(
        track: LocalScreencastVideoTrack,
        options: LocalVideoTrackOptions
    ) {
        track.options = options
        val captureSize = resolveCaptureDimensions(options.captureParams)
        track.capturer.changeCaptureFormat(
            captureSize.first,
            captureSize.second,
            options.captureParams.maxFps
        )
    }

    private fun createCaptureOptions(quality: LiveQuality): LocalVideoTrackOptions {
        val captureHeight = resolveCaptureHeight(quality.maxDimension)
        return LocalVideoTrackOptions(
            isScreencast = true,
            position = null,
            captureParams = VideoCaptureParameter(
                quality.maxDimension,
                captureHeight,
                quality.framesPerSecond,
                false
            )
        )
    }

    private fun createPublishDefaults(quality: LiveQuality): VideoTrackPublishDefaults {
        return VideoTrackPublishDefaults(
            videoEncoding = VideoEncoding(
                maxBitrate = resolveMaxBitrate(quality),
                maxFps = quality.framesPerSecond
            ),
            simulcast = false
        )
    }

    private fun resolveCaptureDimensions(captureParams: VideoCaptureParameter): Pair<Int, Int> {
        val displayMetrics: DisplayMetrics = appContext.resources.displayMetrics
        val displayWidth = max(displayMetrics.widthPixels, 1)
        val displayHeight = max(displayMetrics.heightPixels, 1)

        if (captureParams.width == 0 || captureParams.height == 0) {
            return Pair(displayWidth, displayHeight)
        }

        return if (displayWidth >= displayHeight) {
            Pair(captureParams.width, captureParams.height)
        } else {
            Pair(captureParams.height, captureParams.width)
        }
    }

    private fun resolveCaptureHeight(maxDimension: Int): Int {
        val displayMetrics: DisplayMetrics = appContext.resources.displayMetrics
        val displayWidth = max(displayMetrics.widthPixels, 1)
        val displayHeight = max(displayMetrics.heightPixels, 1)
        val longestSide = max(displayWidth, displayHeight)
        val shortestSide = min(displayWidth, displayHeight)

        if (longestSide <= 0 || shortestSide <= 0) {
            return ((maxDimension * 9f) / 16f).roundToInt()
        }

        val scaledHeight = (maxDimension.toFloat() * shortestSide.toFloat() / longestSide.toFloat()).roundToInt()
        return max(scaledHeight, 1)
    }

    private fun resolveMaxBitrate(quality: LiveQuality): Int {
        return when (quality.presetName.lowercase()) {
            "low" -> 300_000
            "balanced" -> 600_000
            "high" -> 1_200_000
            else -> (quality.maxDimension * quality.framesPerSecond * 278)
                .coerceIn(180_000, 2_500_000)
        }
    }

    private data class LiveQuality(
        val presetName: String,
        val maxDimension: Int,
        val framesPerSecond: Int
    ) {
        companion object {
            fun default(): LiveQuality {
                return LiveQuality(
                    presetName = "low",
                    maxDimension = 540,
                    framesPerSecond = 2
                )
            }
        }
    }
}