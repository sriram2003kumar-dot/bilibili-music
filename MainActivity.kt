package com.bilibili.music

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream

class MainActivity : Activity() {

    private lateinit var webView: WebView

    private var chooserCallback:
        ValueCallback<Array<Uri>>? = null

    private val chooserCode = 701
    private val musicPermissionCode = 702

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = true
            setSupportZoom(false)
        }

        val loader =
            WebViewAssetLoader.Builder()
                .addPathHandler(
                    "/assets/",
                    WebViewAssetLoader.AssetsPathHandler(this)
                )
                .build()

        webView.webViewClient =
            object : WebViewClientCompat() {

                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {

                    val url = request.url

                    if (
                        url.path?.startsWith(
                            "/media/"
                        ) == true
                    ) {
                        val id =
                            url.lastPathSegment
                                ?.toLongOrNull()

                        if (id != null) {
                            return openMedia(id)
                        }
                    }

                    if (
                        url.path?.startsWith(
                            "/albumart/"
                        ) == true
                    ) {
                        val id =
                            url.lastPathSegment
                                ?.toLongOrNull()

                        if (id != null) {
                            return openAlbumArt(id)
                        }
                    }

                    return loader.shouldInterceptRequest(
                        url
                    )
                }
            }

        webView.addJavascriptInterface(
            MusicBridge(),
            "MusicBridge"
        )

        webView.webChromeClient =
            object : WebChromeClient() {

                override fun onShowFileChooser(
                    view: WebView?,
                    callback:
                        ValueCallback<Array<Uri>>?,
                    params:
                        FileChooserParams?
                ): Boolean {

                    chooserCallback
                        ?.onReceiveValue(null)

                    chooserCallback = callback

                    val intent =
                        Intent(
                            Intent.ACTION_OPEN_DOCUMENT
                        ).apply {
                            addCategory(
                                Intent.CATEGORY_OPENABLE
                            )

                            type = "audio/*"

                            putExtra(
                                Intent.EXTRA_ALLOW_MULTIPLE,
                                true
                            )
                        }

                    startActivityForResult(
                        Intent.createChooser(
                            intent,
                            "Select music"
                        ),
                        chooserCode
                    )

                    return true
                }
            }

        setContentView(webView)

        webView.loadUrl(
            "https://appassets.androidplatform.net/assets/index.html"
        )
    }

    private fun openMedia(
        id: Long
    ): WebResourceResponse? {

        return try {

            val mediaUri =
                Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )

            val mime =
                contentResolver.getType(
                    mediaUri
                ) ?: "audio/mpeg"

            val stream =
                contentResolver.openInputStream(
                    mediaUri
                )

            if (stream != null) {
                WebResourceResponse(
                    mime,
                    null,
                    stream
                )
            } else {
                null
            }

        } catch (_: Exception) {
            null
        }
    }

    private fun openAlbumArt(
        id: Long
    ): WebResourceResponse? {

        return try {

            val mediaUri =
                Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )

            val retriever =
                MediaMetadataRetriever()

            retriever.setDataSource(
                this,
                mediaUri
            )

            val picture =
                retriever.embeddedPicture

            retriever.release()

            if (picture == null) {
                null
            } else {

                WebResourceResponse(
                    "image/jpeg",
                    null,
                    ByteArrayInputStream(
                        picture
                    )
                )
            }

        } catch (_: Exception) {
            null
        }
    }

    private fun hasMusicPermission():
        Boolean {

        val permission =
            if (Build.VERSION.SDK_INT >= 33) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestMusicPermission() {

        if (hasMusicPermission()) {
            scanDeviceMusic()
            return
        }

        val permission =
            if (Build.VERSION.SDK_INT >= 33) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(permission),
            musicPermissionCode
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions:
            Array<out String>,
        grantResults:
            IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode ==
            musicPermissionCode &&
            hasMusicPermission()
        ) {
            scanDeviceMusic()
        }
    }

    private fun scanDeviceMusic() {

        Thread {

            val result =
                JSONArray()

            val collection =
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

            val projection =
                arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.MIME_TYPE,
                    MediaStore.Audio.Media.SIZE,
                    MediaStore.Audio.Media.DURATION
                )

            val selection =
                "${MediaStore.Audio.Media.IS_MUSIC} != 0"

            val sort =
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

            try {

                contentResolver.query(
                    collection,
                    projection,
                    selection,
                    null,
                    sort
                )?.use { cursor ->

                    val idCol =
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media._ID
                        )

                    val titleCol =
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.TITLE
                        )

                    val artistCol =
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.ARTIST
                        )

                    val nameCol =
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.DISPLAY_NAME
                        )

                    val mimeCol =
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.MIME_TYPE
                        )

                    val sizeCol =
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.SIZE
                        )

                    val durationCol =
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.DURATION
                        )

                    while (
                        cursor.moveToNext()
                    ) {

                        val id =
                            cursor.getLong(idCol)

                        val title =
                            cursor.getString(
                                titleCol
                            ).orEmpty()

                        val artist =
                            cursor.getString(
                                artistCol
                            ).orEmpty()

                        val displayName =
                            cursor.getString(
                                nameCol
                            ).orEmpty()

                        val mime =
                            cursor.getString(
                                mimeCol
                            )
                                .orEmpty()
                                .ifBlank {
                                    "audio/mpeg"
                                }

                        val size =
                            cursor.getLong(sizeCol)

                        val duration =
                            cursor.getLong(
                                durationCol
                            )

                        val mediaUrl =
                            "https://appassets.androidplatform.net/media/$id"

                        val artUrl =
                            "https://appassets.androidplatform.net/albumart/$id"

                        result.put(
                            JSONObject().apply {

                                put(
                                    "id",
                                    "media-$id"
                                )

                                put(
                                    "name",
                                    title.ifBlank {
                                        displayName
                                            .substringBeforeLast(
                                                '.'
                                            )
                                    }
                                )

                                put(
                                    "artist",
                                    if (
                                        artist.isBlank() ||
                                        artist ==
                                        "<unknown>"
                                    ) {
                                        "LOCAL MUSIC"
                                    } else {
                                        artist
                                    }
                                )

                                put(
                                    "size",
                                    "${String.format(
                                        "%.1f",
                                        size /
                                            1024.0 /
                                            1024.0
                                    )} MB"
                                )

                                put(
                                    "type",
                                    mime
                                )

                                put(
                                    "duration",
                                    duration
                                )

                                put(
                                    "url",
                                    mediaUrl
                                )

                                put(
                                    "artUrl",
                                    artUrl
                                )

                                put(
                                    "native",
                                    true
                                )
                            }
                        )
                    }
                }

            } catch (_: Exception) {
            }

            runOnUiThread {

                val json =
                    JSONObject.quote(
                        result.toString()
                    )

                webView.evaluateJavascript(
                    "window.onNativeMusicScan && " +
                        "window.onNativeMusicScan(" +
                        "JSON.parse($json));",
                    null
                )
            }

        }.start()
    }

    inner class MusicBridge {

        @JavascriptInterface
        fun requestMusicAccess() {
            runOnUiThread {
                requestMusicPermission()
            }
        }

        @JavascriptInterface
        fun scanMusic() {
            if (hasMusicPermission()) {
                scanDeviceMusic()
            } else {
                runOnUiThread {
                    requestMusicPermission()
                }
            }
        }
    }

    @Deprecated(
        "Legacy activity result API"
    )
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode != chooserCode) {
            return
        }

        val result:
            Array<Uri>? =
            if (
                resultCode != RESULT_OK ||
                data == null
            ) {
                null
            } else {

                val clips =
                    data.clipData

                if (clips != null) {

                    Array(
                        clips.itemCount
                    ) { i ->
                        clips.itemAt(i).uri
                    }

                } else {

                    data.data?.let {
                        arrayOf(it)
                    }
                }
            }

        chooserCallback
            ?.onReceiveValue(result)

        chooserCallback = null
    }

    override fun onResume() {

        super.onResume()

        if (
            ::webView.isInitialized &&
            hasMusicPermission()
        ) {
            webView.post {
                scanDeviceMusic()
            }
        }
    }

    override fun onBackPressed() {

        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {

        chooserCallback
            ?.onReceiveValue(null)

        chooserCallback = null

        webView.destroy()

        super.onDestroy()
    }
}
