package com.spaceo.download

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.downloader.OnCancelListener
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.downloader.PRDownloaderConfig
import com.spaceo.download.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.*
import java.io.*
import java.net.URI
import kotlin.coroutines.coroutineContext

@DelicateCoroutinesApi
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var url:String
    lateinit var dirPath:String
    lateinit var fileName:String
    lateinit var downloadId :Any



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.INTERNET,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                312
            )
        }

        PRDownloader.initialize(this)

        val config = PRDownloaderConfig.newBuilder()
            .setDatabaseEnabled(true)
            .build()
        PRDownloader.initialize(applicationContext, config)

        binding.btnDownload.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {

                url = binding.etURL.text.toString().trim()
                dirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    .toString() + "/Download Manager/"
                fileName = getFileName(url)

                downloadId = PRDownloader.download(url, dirPath, fileName)
                    .build()
                    .setOnPauseListener { }
                    .setOnCancelListener(object : OnCancelListener {
                        override fun onCancel() {}
                    })
                    .setOnProgressListener { }
                    .start(object : OnDownloadListener {
                        override fun onDownloadComplete() {}
                        override fun onError(error: com.downloader.Error?) {
                            Log.e("Error","Not Successful")
                        }

                        fun onError(error: Error?) {}
                    })

                startDownloading(url)

            }
        }

        binding.btnPause.setOnClickListener {
            PRDownloader.pause(downloadId as Int)
        }

        binding.btResume.setOnClickListener {
            PRDownloader.resume(downloadId as Int)
        }
    }


    private fun startDownloading(URL: String) {

        val request = Request.Builder().url(URL).build()
        val response = OkHttpClient().newCall(request).execute()
        val `is`: InputStream = response.body!!.byteStream()
        val Path =
            (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString() + "/Download Manager/")
        val file = File(Path)
        if (!file.exists()) file.mkdir()
        val outputFile = File(file, getFileName(URL))
        val input = BufferedInputStream(`is`)
        val output: OutputStream = FileOutputStream(outputFile)
        val data = ByteArray(1024)
        var total: Long
        while (`is`.read(data).also { total = it.toLong() } != -1) {
            output.write(data, 0, total.toInt())
        }
        output.flush()
        output.close()
        input.close()
        Log.d("Download", "Downloaded Successfully")


    }

    private fun pauseDownload(context: Context, downloadTitle: String): Boolean {
        var updatedRows = 0
        val pauseDownload = ContentValues()
        val Path =
            (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString() + "/Download Manager/")
        pauseDownload.put("control", 0) // Pause Control Value
        try {
            updatedRows = context
                .contentResolver
                .update(
                    Uri.parse(Path),
                    pauseDownload,
                    downloadTitle, arrayOf(downloadTitle)
                )
            Log.d("Pause", "Downloading Paused")
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Failed to update control for downloading video")
        }
        return 0 < updatedRows

    }

    private fun resumeDownload(context: Context, downloadTitle: String): Boolean {

        var updatedRows = 0
        val resumeDownload = ContentValues()
        val Path =
            (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString() + "/Download Manager/")
        resumeDownload.put("control", 0) // Resume Control Value
        try {
            updatedRows = context
                .contentResolver
                .update(
                    Uri.parse(Path),
                    resumeDownload,
                    downloadTitle, arrayOf(downloadTitle)
                )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update control for downloading video")
        }
        return 0 < updatedRows

    }

    private fun getFileName(url: String): String {

        val uri = URI.create(url.trim())
        val path = uri.path
        val index = path.indexOfLast { it == '/' }
        if (index != -1) {
            return path.substring(index + 1)
        }
        return "No name"

    }
}