# FlipperDroid V2

FlipperDroid V2 is an Android security and hardware diagnostics toolkit written in Kotlin and Jetpack Compose.

This fork is based on the open-source **FlipperDroid** project by **Jeremiznoo** and keeps the upstream attribution and license terms.

## V2 goals

V2 focuses on a cleaner, safer and more maintainable baseline:

- modernized home screen and navigation;
- Android 7+ compatibility path (`minSdk 24`);
- reduced Android permissions;
- NFC reading, history and real local export;
- defensive network diagnostics (Ping, DNS and route checks);
- infrared support for compatible devices;
- local password generator;
- device capability/status screen;
- light/dark theme settings;
- high-risk experimental modules are not exposed from the main V2 navigation.

## Version

- `versionCode`: **2**
- `versionName`: **2.0.0**

## Main V2 screens

- **NFC Reader** — inspect compatible tags, keep scan history and export scan data.
- **Network Diagnostics** — basic connectivity and name-resolution checks.
- **Infrared** — use supported consumer IR hardware with devices you own.
- **Password Generator** — generate passwords locally on-device.
- **Device Status** — check Android, NFC, IR and network availability.
- **Settings** — application and theme preferences.
- **About V2** — version, credits and safety model.

## Build

The project uses the Gradle wrapper. From the repository root:

```bash
./gradlew assembleDebug
```

The debug APK is normally produced under:

```text
build/outputs/apk/debug/
```

Depending on the project layout/Gradle configuration, Android Studio can also be used to build and run the application.

## Authorized use

Use FlipperDroid V2 only with devices, tags and networks that you own or are explicitly authorized to test. The V2 navigation intentionally defaults to diagnostic and inspection-oriented functionality.

## Upstream and credits

Original project: **Jeremiznoo/FlipperDroid**.

Thanks to the original FlipperDroid contributors, the Android open-source ecosystem, and the Flipper Zero project for inspiration.

## License

See [`LICENSE`](LICENSE). The upstream project is distributed under the MIT License.
