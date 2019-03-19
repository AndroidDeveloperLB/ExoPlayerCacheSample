package com.sample.exoplayercachesample

import android.content.Context
import android.net.Uri
import androidx.annotation.RawRes
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.LoopingMediaSource
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.util.Util
import java.io.File

fun SimpleExoPlayer.prepareToPlayVideoFromAssets(context: Context, assetFilePath: String) {
    when {
        assetFilePath.startsWith("/android_asset/") or assetFilePath.startsWith("asset:///") -> prepareToPlayVideoFromUrl(context, assetFilePath)
        assetFilePath.startsWith("/") -> prepareToPlayVideoFromUrl(context, "/android_asset$assetFilePath")
        else -> prepareToPlayVideoFromUrl(context, "/android_asset/$assetFilePath")
    }
}

fun SimpleExoPlayer.prepareToPlayVideoFromRawResource(context: Context, @RawRes rawVideoRes: Int, loopIterationCount: Int = Integer.MAX_VALUE) {
    val dataSpec = DataSpec(RawResourceDataSource.buildRawResourceUri(rawVideoRes))
    val rawResourceDataSource = RawResourceDataSource(context)
    rawResourceDataSource.open(dataSpec)
    val factory: DataSource.Factory = DataSource.Factory { rawResourceDataSource }
    prepare(LoopingMediaSource(ExtractorMediaSource.Factory(factory).createMediaSource(rawResourceDataSource.uri), if (loopIterationCount <= 0) Integer.MAX_VALUE else loopIterationCount))
}

fun SimpleExoPlayer.prepareToPlayVideoFromUrl(context: Context, url: String, cache: Cache? = null) = prepareToPlayVideoFromUri(context, Uri.parse(url), cache)

fun SimpleExoPlayer.prepareToPlayVideoFromFile(context: Context, file: File) = prepareToPlayVideoFromUri(context, Uri.fromFile(file))

fun SimpleExoPlayer.prepareToPlayVideoFromUri(context: Context, uri: Uri, cache: Cache? = null) {
    val factory = if (cache != null)
        CacheDataSourceFactory(cache, DefaultHttpDataSourceFactory(ExoPlayerEx.getUserAgent(context)))
    else
        DefaultDataSourceFactory(context, ExoPlayerEx.getUserAgent(context))
    val mediaSource = ExtractorMediaSource.Factory(factory).createMediaSource(uri)
    prepare(mediaSource)
}

fun SimpleExoPlayer.setAudioStreamTypeIfNeeded(@C.StreamType audioStreamType: Int) {
    val currentAudioStreamType = Util.getStreamTypeForAudioUsage(audioAttributes.usage)
    if (currentAudioStreamType == audioStreamType)
        return
    val usage = Util.getAudioUsageForStreamType(audioStreamType)
    val contentType = Util.getAudioContentTypeForStreamType(audioStreamType)
    val audioAttributes = AudioAttributes.Builder().setUsage(usage).setContentType(contentType).build()
    setAudioAttributes(audioAttributes, false)
}

object ExoPlayerEx {
    @JvmStatic
    fun getUserAgent(context: Context): String {
        val packageManager = context.packageManager
        val info = packageManager.getPackageInfo(context.packageName, 0)
        val appName = info.applicationInfo.loadLabel(packageManager).toString()
        return Util.getUserAgent(context, appName)
    }

    @JvmStatic
    fun convertPlaybackStateToString(playbackState: Int?): String {
        return when (playbackState) {
            Player.STATE_IDLE -> "STATE_IDLE"
            Player.STATE_BUFFERING -> "STATE_BUFFERING"
            Player.STATE_READY -> "STATE_READY"
            Player.STATE_ENDED -> "STATE_ENDED"
            else -> "unknown"
        }
    }
}
