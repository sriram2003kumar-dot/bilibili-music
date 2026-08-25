from pathlib import Path
import re

root=Path(__file__).resolve().parent.parent
app=next(root.glob('android-project/**/app'),None)
if app is None:
    raise SystemExit('app module not found')

manifest=app/'src/main/AndroidManifest.xml'
if manifest.exists():
    s=manifest.read_text()
    if 'POST_NOTIFICATIONS' not in s:
        s=s.replace('<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />','<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />\n    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />')
        manifest.write_text(s)

kt=app/'src/main/java/com/bilibili/music/MainActivity.kt'
s=kt.read_text()
if 'android.media.MediaMetadataRetriever' not in s:
    s=s.replace('import android.Manifest\n','import android.Manifest\nimport android.media.MediaMetadataRetriever\nimport android.util.Base64\n')
if 'notificationPermissionCode' not in s:
    s=s.replace('private val musicPermissionCode = 702','private val musicPermissionCode = 702\n    private val notificationPermissionCode = 703')
    s=s.replace('setContentView(webView)','setContentView(webView)\n        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {\n            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), notificationPermissionCode)\n        }')

old='''MediaStore.Audio.Media.DURATION\n            )'''
new='''MediaStore.Audio.Media.DURATION,\n                MediaStore.Audio.Media.ALBUM,\n                MediaStore.Audio.Media.RELATIVE_PATH\n            )'''
s=s.replace(old,new)
old='''val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)'''
new='''val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)\n                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)\n                    val folderCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)'''
s=s.replace(old,new)
old='''val duration = cursor.getLong(durationCol)\n                        val uri = "https://appassets.androidplatform.net/media/$id"\n\n                        result.put(JSONObject().apply {'''
new='''val duration = cursor.getLong(durationCol)\n                        val album = cursor.getString(albumCol).orEmpty().ifBlank { "UNKNOWN ALBUM" }\n                        val folder = cursor.getString(folderCol).orEmpty().ifBlank { "Music" }\n                        val mediaUri = Uri.withAppendedPath(collection, id.toString())\n                        val art = try {\n                            val retriever = MediaMetadataRetriever()\n                            retriever.setDataSource(this@MainActivity, mediaUri)\n                            val bytes = retriever.embeddedPicture\n                            retriever.release()\n                            if (bytes != null) "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP) else ""\n                        } catch (_: Exception) { "" }\n                        val uri = "https://appassets.androidplatform.net/media/$id"\n\n                        result.put(JSONObject().apply {'''
s=s.replace(old,new)
old='''put("duration", duration)\n                            put("url", uri)\n                            put("native", true)'''
new='''put("duration", duration)\n                            put("album", album)\n                            put("folder", folder)\n                            put("art", art)\n                            put("url", uri)\n                            put("native", true)'''
s=s.replace(old,new)
kt.write_text(s)
