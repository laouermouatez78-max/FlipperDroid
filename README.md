# FlipperDroid V4

FlipperDroid V4 is an Android cybersecurity, radio and hardware toolkit built with Kotlin and Jetpack Compose.

This fork is based on the open-source **FlipperDroid** project by **Jeremiznoo** and keeps upstream attribution and MIT license terms.

## Version

- `versionCode`: **4**
- `versionName`: **4.0.0**
- `minSdk`: **24**
- `targetSdk`: **35**

## V4 goal

V4 fixes the main weakness of V3: several screens looked like tools but only simulated behavior. V4 replaces those simulations with Android-supported functions that can be tested on hardware and networks you own.

## Active tools

### BLE Explorer
- real BLE advertisement scan;
- name, MAC/address, RSSI, connectability and advertised UUIDs;
- real GATT connection to a selected connectable device;
- service and characteristic discovery;
- readable/writable/notifiable characteristic capability flags.

### BLE Advertiser
- real Android BLE advertising;
- FlipperDroid V4 test service UUID;
- configurable device name;
- designed for testing between two devices you control.

### Wi-Fi Analyzer
- real `WifiManager` scan results;
- corrected Android 13+ permission model;
- SSID/BSSID, RSSI, frequency and channel;
- WPA/WPA2/WPA3/WEP/open classification;
- cached-result support when Android scan throttling is active.

### LAN Analyzer
- automatically detects the phone's private IPv4 address;
- scans only the connected private `/24`;
- discovers active hosts;
- checks a short set of common TCP ports for inventory/diagnostics.

### USB Inspector
- real Android USB Host device discovery;
- VID/PID and USB class;
- interfaces, subclasses, protocols and endpoints;
- attached-device count in Device Status.

V4 no longer pretends stock Android USB Host automatically turns a phone into a USB HID keyboard. That requires device-specific USB gadget support/root/kernel configuration on many phones.

### NFC / RFID
- UID and NFC technology inspection;
- NDEF metadata;
- real NDEF text writing on compatible writable tags;
- explicit MIFARE Classic memory reading for authorized tags;
- scan history and export.

### Other modules
- network diagnostics;
- infrared tools on phones with an IR emitter;
- QR analyzer/generator;
- password generator;
- privacy-first EMV application metadata reader;
- synthetic APDU sandbox;
- expanded hardware diagnostics.

## V4 visual system

V4 disables Android dynamic-color overriding by default and uses a consistent Flipper-inspired interface:

- graphite/black background;
- orange primary color;
- high-contrast cards;
- active/private/sandbox badges;
- redesigned home dashboard.

## Build

```bash
./gradlew clean test assembleDebug
```

APK output:

```text
build/outputs/apk/debug/
```

## Authorized use

Use active functions only on hardware, NFC tags and networks you own or are explicitly authorized to assess. V4 intentionally does not include disruptive Wi-Fi deauthentication, BLE flooding, USB keystroke injection, or real payment-card cloning.

## Credits

Original project: **Jeremiznoo/FlipperDroid**.

## License

See [`LICENSE`](LICENSE).
