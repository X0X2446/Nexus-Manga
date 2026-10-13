# Nexus

Fork of [Mihon](https://github.com/mihonapp/mihon) with an AnymeX-inspired immersive UI:

- **Catalog home** — browse manga from trackers (MangaBaka, AniList, MyAnimeList, Kitsu) or installed Mihon-compatible extensions
- **Top-left catalog bubble** — switch the active catalog between trackers and extension sources
- **Floating bubble nav** — bottom-right control that expands to Home, Search, Extensions, and Settings
- **Read from tracker** — open a tracker title and pick an installed extension to search/read it
- Full Mihon extension system and settings tree preserved

Requires **Android 8.0+**.

## Build

```bash
# Android SDK required (compile SDK 37.2, NDK 29, JDK 17+)
export ANDROID_HOME=/path/to/android-sdk
echo "sdk.dir=$ANDROID_HOME" > local.properties

./gradlew :app:assembleDebug
```

Debug APKs (split per ABI, plus universal):

`app/build/outputs/apk/debug/app-universal-debug.apk`

Install beside stock Mihon (`applicationId` is `app.nexus.mihon`).

## License

Apache License 2.0. Based on Mihon / Tachiyomi. See [LICENSE](LICENSE).
