# FlipperDroid

**FlipperDroid** is an Android application developed in Kotlin, inspired by the well-known **Flipper Zero** device. It brings a powerful set of wireless and hardware security testing tools to Android smartphones, leveraging their native capabilities.

<p align="center">
  <img src="https://github.com/Jeremiznoo/FlipperDroid/blob/main/src/main/ic_launcher-playstore.png">
</p>

---

## Overview

FlipperDroid transforms a modern Android phone into a flexible and portable alternative to Flipper Zero. It enables security professionals, researchers, and enthusiasts to interact with and analyze a wide range of communication protocols and interfaces, directly from a mobile device.

The application includes modules for scanning, analyzing, and emulating NFC, Bluetooth, RFID, and more, as well as tools for network diagnostics and USB-based attacks, making it a comprehensive mobile toolkit.

---

## Features

- **NFC & EMV Reading and Emulation**  
  Read and emulate NFC cards, including contactless bank cards and other chip-based identifiers.

- **Bluetooth Interface**  
  Scan for Bluetooth devices, connect to them, and prepare for future emulation capabilities.

- **Network Tools**  
  Integrated interface for tools like Nmap, Wi-Fi scanning, and packet analysis (in development).

- **RFID and Infrared**  
  Modules to read, interact with, or send signals using RFID or infrared communication.

- **BadUSB Module**  
  Emulate malicious USB HID devices for penetration testing and user awareness purposes.

- **Advanced Logging System**  
  View, manage, and export logs related to device interactions, scans, and application activity.

- **Customizable Technical Settings**  
  Modify key behaviors and parameters within the app for advanced customization.

- **Material UI Compatibility**  
  Built with respect to Material Design guidelines for native Android look and feel.

---

### Requirements

- Android 10 or newer
- Tested with LineageOS on a rooted Google Pixel 7

---

## Usage and Legal Disclaimer

**This application is intended strictly for educational, research, and legal penetration testing purposes.**  
Misuse of this software for unauthorized access, emulation, or attacks on third-party devices or systems is strictly prohibited. The developer is not responsible for any consequences resulting from illegal use.

---

## Project Structure

```
FlipperDroid/
├── src/ # Application source code
├── build/ # Gradle build artifacts
├── .gradle/ .idea/ .kotlin/ # Configuration and IDE metadata
├── build.gradle.kts # Kotlin DSL build configuration
├── settings.gradle.kts
├── gradlew / gradlew.bat # Gradle wrapper
├── proguard-rules.pro # Obfuscation and shrink rules
└── local.properties # Local Android SDK path
```

## License

This project is released under the **MIT License**. See the [`LICENSE`](LICENSE) file for details.

---

## Acknowledgments

- The Flipper Zero project for functional inspiration
- LineageOS and the Android open-source ecosystem
- F-Droid for supporting free and open software distribution

---

## Screenshots
