package com.sample.exoplayercachesample

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.cache.CacheSpan
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import kotlinx.android.synthetic.main.activity_main.*
import org.apache.commons.io.FilenameUtils
import java.io.File


class MainActivity : AppCompatActivity() {
    private var player: SimpleExoPlayer? = null

    companion object {
        const val VIDEO_URL = "https://sample-videos.com/video123/mp4/720/big_buck_bunny_720p_1mb.mp4"
        const val MAX_PREVIEW_CACHE_SIZE_IN_BYTES = 15L * 1024L * 1024L
        var cache: SimpleCache? = null
        @JvmStatic
        fun prepareIntentForSharingFile(context: Context, file: File): Intent {
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(FilenameUtils.getExtension(file.name))
            val uri = FileProvider.getUriForFile(context, context.packageName + ".provider", file)
            return Intent(Intent.ACTION_SEND)
                .addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY
                            or Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                )
                .putExtra(Intent.EXTRA_STREAM, uri).setType(mimeType)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (cache == null)
            cache = SimpleCache(
                FilesPaths.VIDEO_CACHE_FOLDER.getFile(this, false),
                LeastRecentlyUsedCacheEvictor(MAX_PREVIEW_CACHE_SIZE_IN_BYTES)
            )
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            tryShareCacheFile()
        }
        player = ExoPlayerFactory.newSimpleInstance(this, DefaultTrackSelector())
        playerView.alpha = 0f
        player!!.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                playerView.keepScreenOn = playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED
                when (playbackState) {
                    Player.STATE_READY -> {
                        playerView.player = player
                        playerView.animate().alpha(1f).start()
                        progressBar.visibility = View.GONE
                    }
                    Player.STATE_ENDED -> {
                    }
                }
            }

        })
        player!!.volume = 1f
        player!!.repeatMode = Player.REPEAT_MODE_ALL
        player!!.prepareToPlayVideoFromUrl(this, VIDEO_URL, cache)
    }

    //NOTE: I know I shouldn't use an AsyncTask. It's just a sample...
    @SuppressLint("StaticFieldLeak")
    fun tryShareCacheFile() {
        val cacheSpan: CacheSpan? = cache?.getCachedSpans(VIDEO_URL)?.pollFirst()
        val cachedFile = cacheSpan?.file
        if (cacheSpan?.isCached == true && cachedFile?.exists() == true) {
            // file is cached and ready to be used
            //TODO this is a bad way to do it, as wrote here: https://github.com/google/ExoPlayer/issues/5569#issuecomment-468040439 , so need to change this
            object : AsyncTask<Void?, Void?, File>() {
                override fun doInBackground(vararg params: Void?): File {
                    val tempFile = FilesPaths.FILE_TO_SHARE.getFile(this@MainActivity, true)
                    //TODO decide if it's better to copy or rename , but either way it's consider unsafe to directly use the cache file
//                    cachedFile.renameTo(tempFile)
                    cachedFile.copyTo(tempFile)
                    return tempFile
                }

                override fun onPostExecute(result: File) {
                    super.onPostExecute(result)
                    val intent = prepareIntentForSharingFile(this@MainActivity, result)
                    startActivity(intent)
                }
            }.execute()
        } else
            Toast.makeText(this, "file not ready yet", Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
        player!!.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        player!!.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.stop()
        player?.release()
        if (!isChangingConfigurations) {
            cache?.release()
            cache = null
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var url: String? = null
        when (item.itemId) {
            R.id.menuItem_all_my_apps -> url = "https://play.google.com/store/apps/developer?id=AndroidDeveloperLB"
            R.id.menuItem_all_my_repositories -> url = "https://github.com/AndroidDeveloperLB"
            R.id.menuItem_current_repository_website -> url =
                "https://github.com/AndroidDeveloperLB/ExoPlayerCacheSample"
        }
        if (url == null)
            return true
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        startActivity(intent)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
}
