# Bilibili Music Android

Native Android app shell for the Bilibili Music player.

## Included
- Nothing OS-style black music player UI
- Local `+ ADD MUSIC` picker
- Persistent IndexedDB storage for manually added songs
- Automatic Android device music scan using MediaStore
- Android 13+ `READ_MEDIA_AUDIO` permission handling
- Android 12 and below `READ_EXTERNAL_STORAGE` handling
- Search, playback controls, shuffle and repeat
- Dynamic Island-style player UI
- GitHub Actions APK build

## Automatic music scan
On first launch, the app requests music-library permission. After permission is granted, songs from the Android MediaStore are automatically loaded into the playlist. The scan runs again when the app resumes, so newly downloaded music can appear without manually adding files.

The automatic scan does not copy music into the app database; it plays the device's MediaStore files directly. Manually added files continue to use the app's persistent storage.
