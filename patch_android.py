from pathlib import Path
root=Path(".")
files=list(root.rglob("MainActivity.kt"))
if not files: raise SystemExit("MainActivity.kt not found")
p=files[0]; s=p.read_text(encoding="utf-8")
if 'url.path?.startsWith("/albumart/")' not in s:
    marker='if (url.path?.startsWith("/media/") == true) {'
    block='''if (url.path?.startsWith("/albumart/") == true) {
                    val albumId = url.lastPathSegment?.toLongOrNull()
                    if (albumId != null) {
                        return try {
                            val artUri = Uri.parse("content://media/external/audio/albumart/$albumId")
                            val stream = contentResolver.openInputStream(artUri)
                            if (stream != null) WebResourceResponse("image/jpeg", null, stream) else null
                        } catch (_: Exception) { null }
                    }
                }
                '''
    s=s.replace(marker,block+marker,1)
if 'MediaStore.Audio.Media.ALBUM_ID' not in s:
    s=s.replace('MediaStore.Audio.Media.DURATION\n            )','MediaStore.Audio.Media.DURATION,\n                MediaStore.Audio.Media.ALBUM_ID\n            )',1)
if 'val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)' not in s:
    s=s.replace('val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)','val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)\n                    val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)',1)
if 'val albumId = cursor.getLong(albumIdCol)' not in s:
    s=s.replace('val duration = cursor.getLong(durationCol)','val duration = cursor.getLong(durationCol)\n                        val albumId = cursor.getLong(albumIdCol)',1)
if 'put("albumId", albumId)' not in s:
    s=s.replace('put("duration", duration)\n                            put("url", uri)','put("duration", duration)\n                            put("albumId", albumId)\n                            put("url", uri)',1)
p.write_text(s,encoding="utf-8")
print("patched",p)
