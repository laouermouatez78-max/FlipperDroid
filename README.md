# FlipperDroid V3

FlipperDroid V3 is an Android cybersecurity, radio and hardware diagnostics toolkit written in Kotlin and Jetpack Compose.

This fork is based on the open-source **FlipperDroid** project by **Jeremiznoo** and keeps the upstream attribution and MIT license terms.

## Version

- `versionCode`: **3**
- `versionName`: **3.0.0**
- `minSdk`: **24** (Android 7+)
- `targetSdk`: **35**

## What V3 restores and improves

V3 brings the major historical modules back into one dashboard while modernizing Android permissions, navigation, diagnostics and safety defaults.

### NFC / RFID

- NFC UID and technology inspection;
- NDEF metadata inspection;
- scan history and local export;
- MIFARE Classic memory reading only after an explicit authorization confirmation;
- metadata-first behavior instead of automatically dumping memory whenever a tag is presented.

### Bluetooth LE

- passive BLE scanner with name, address, RSSI, connectability and advertised service UUIDs;
- Apple/Samsung legacy advertisement-profile catalogue;
- controlled BLE Lab simulator with selectable profiles and a transmission-preview log.

The historical continuous BLE-spam engine is not used by V3; BLE Lab simulates the profile rotation locally so nearby devices are not flooded.

### Wi-Fi

- nearby network inventory;
- SSID/BSSID, RSSI, channel and frequency;
- WPA/WPA2/WPA3/WEP/open-network classification;
- security posture findings;
- Android 13+ `NEARBY_WIFI_DEVICES` permission support;
- cached-result handling when Android throttles active scans.

The historical `WifiDeauther` source name is retained for compatibility, but the V3 module is a defensive Wi-Fi audit tool and does not transmit deauthentication frames.

### Network toolkit

- existing defensive connectivity, DNS, route and network diagnostics remain available;
- bundled network tooling from the upstream project remains part of the repository where applicable.

### USB HID Lab

- USB Host capability detection;
- script authoring and validation;
- local step-by-step HID workflow simulation;
- preview logs and explicit lab sessions.

The historical BadUSB HID injection engine has been replaced by a simulator; V3 does not inject keyboard input into another computer.

### EMV / ISO-DEP

- privacy-first EMV metadata reader;
- explicit user action before reading a detected NFC tag;
- scheme/AID identification only;
- no PAN, expiry date, cardholder name or Track 2 extraction;
- synthetic EMV APDU Lab with test profiles and a local APDU transcript;
- legacy Host Card Emulation service neutralized and removed from the manifest;
- legacy payment AID replaced by a synthetic non-payment lab AID.

### Other tools

- infrared controls for compatible Android hardware;
- local password generator;
- QR payload analyzer and local QR generator;
- expanded device-status screen for Android, NFC, BLE, Wi-Fi, USB Host, IR and network capabilities;
- light/dark theme and application settings;
- V3 authorization notice and clearer module descriptions.

## Build locally

The project uses the Gradle wrapper. From the repository root:

```bash
./gradlew clean test assembleDebug
```

The debug APK is produced under:

```text
build/outputs/apk/debug/
```

## GitHub Actions

The V3 branch includes a GitHub Actions workflow that runs tests, builds the debug APK and uploads the APK as a workflow artifact.

## Authorized use

Use FlipperDroid only with hardware, tags, cards and networks that you own or are explicitly authorized to test. High-impact historical modules are represented by controlled laboratory simulations rather than disruptive actions.

## Upstream and credits

Original project: **Jeremiznoo/FlipperDroid**.

Thanks to the original FlipperDroid contributors, the Android open-source ecosystem, and the Flipper Zero project for inspiration.

## License

See [`LICENSE`](LICENSE). The upstream project is distributed under the MIT License.
