# LeanOS

A minimal, customizable Android launcher (Kotlin, Jetpack Compose, Material You). Android 12+, sideloaded.

## Build

```sh
./gradlew testDebugUnitTest assembleDebug
```

Point Gradle at an Android SDK with `ANDROID_HOME` or `sdk.dir` in `local.properties`. JDK 17 or newer.

Every push to GitHub runs the unit tests and uploads a release APK as a build artifact (Actions → the run → Artifacts).

## Release signing

Android only installs an update over an existing app when both are signed with the same key, so CI signs every release APK with one permanent keystore kept in GitHub secrets. Without the secrets, CI falls back to the debug key and says so in the job summary. A debug-signed APK will not install over a release-signed one, and each CI run makes a fresh debug key, so debug-signed APKs from different runs won't install over each other either.

1. Create the keystore once and keep it (and its passwords) somewhere safe. If you lose it, you cannot update the installed app without uninstalling it and losing its settings.

   ```sh
   keytool -genkeypair -v -keystore leanos-release.jks -alias leanos \
     -keyalg RSA -keysize 4096 -validity 36500
   ```

2. Base64-encode it:

   ```sh
   base64 -w0 leanos-release.jks > leanos-release.jks.b64   # macOS: base64 -i leanos-release.jks -o leanos-release.jks.b64
   ```

3. In the repo, open Settings → Secrets and variables → Actions → New repository secret, and add:

   | Secret | Value |
   | --- | --- |
   | `LEANOS_KEYSTORE_B64` | contents of `leanos-release.jks.b64` |
   | `LEANOS_KEYSTORE_PASSWORD` | the keystore password |
   | `LEANOS_KEY_ALIAS` | `leanos` (or the alias you chose) |
   | `LEANOS_KEY_PASSWORD` | the key password (same as the keystore password unless you set a different one) |

To sign a release build locally, set `LEANOS_KEYSTORE_FILE` to the `.jks` path plus the three password/alias variables above, then run `./gradlew assembleRelease`.

## License

Google Sans Flex is licensed under the SIL Open Font License 1.1; see [`app/src/main/res/raw/google_sans_flex_ofl.txt`](app/src/main/res/raw/google_sans_flex_ofl.txt).
