# 🎧 realme Buds Controller (Windows & Android)

A clean, modern, ultra-fast application for controlling **realme Buds** directly from your **Windows PC** and **Android Phone**.

Easily toggle **Active Noise Cancellation (ANC)**, **Transparency Mode**, and **Super Low-Latency Gaming Mode** with zero bloat.

---

## 📦 Downloads & Builds

Pre-built standalone releases are available directly in this repository:

| Platform | Download File | Size | Description |
| :--- | :--- | :---: | :--- |
| 🪟 **Windows** | **[`Realme_Buds_T200x.exe`](Realme_Buds_T200x.exe)** | ~32 MB | **Standalone Portable `.exe`**<br>• Runs instantly with no Python or drivers needed.<br>• Supports Windows 10 & 11 (64-bit). |
| 🤖 **Android** | **[`Realme_Buds_Controller.apk`](Realme_Buds_Controller.apk)** | ~11 MB | **Native Android APK**<br>• Built with Jetpack Compose & Kotlin.<br>• Supports Android 7.0 to Android 15/16. |

---

## ⚡ Key Features

- **Cross-Platform Native Experience**: Dedicated standalone builds for both Windows and Android with a matching realme electric yellow dark theme.
- **Active Noise Cancellation (ANC)**: Switch between 4 modes:
  - 🔇 **Noise Cancelling**: Blocks ambient background noise.
  - 👂 **Transparency**: Amplifies surrounding voices and ambient audio.
  - ⚪ **Normal Mode**: Standard playback without active filtering.
  - ⚡ **ANC Alternate**: Alternative noise suppression curve.
- **Low-Latency Game Mode**: One-tap toggle for 50–88ms super low audio latency.
- **Automatic Device Discovery**: Scans and lists your paired Bluetooth audio devices automatically.
- **Live Activity Monitor**: Expandable console displaying real-time raw hex communication (`TX` and `RX`) with the earbuds.

---

## 📱 Supported Devices

Uses the universal **Realme Link / OPPOv1** Bluetooth RFCOMM protocol:

| Device | Game Mode (Low Latency) | Active Noise Cancellation (ANC) | Transparency Mode |
| :--- | :---: | :---: | :---: |
| **realme Buds T200x** | ✅ Supported | ✅ Supported | ✅ Supported |
| **realme Buds T300** | ✅ Supported | ✅ Supported | ✅ Supported |
| **realme Buds T100 / T110** | ✅ Supported | ❌ *(Hardware has no ANC)* | ❌ *(Hardware has no ANC)* |
| **realme Buds Air Series (Air 3, Air 5, Air 6, etc.)** | ✅ Supported | ✅ Supported | ✅ Supported |
| **Compatible OPPO / OnePlus Buds** | ✅ Supported | ✅ Supported | ✅ Supported |

---

## 🚀 Quick Setup & Usage

### 🪟 Windows Setup
1. Pair your realme Buds to your PC via Windows Bluetooth settings.
2. Download and double-click **[`Realme_Buds_T200x.exe`](Realme_Buds_T200x.exe)**.
3. The app will auto-detect your Bluetooth port (or pick your `COM` port from the dropdown).
4. Click **Connect** and switch modes instantly!

### 🤖 Android Setup
1. Pair your realme Buds to your phone in Android Bluetooth settings.
2. Download **[`Realme_Buds_Controller.apk`](Realme_Buds_Controller.apk)** and tap **Install** *(enable "Install from unknown sources" if prompted)*.
3. Open the **realme Buds** app and allow Bluetooth permissions.
4. Select your earbuds from the device dropdown and tap **Connect**.
5. Tap any ANC mode or toggle the Game Mode switch!

---

## 🛠️ Technical Protocol Overview

Communication is handled directly over Bluetooth RFCOMM (Serial Port Profile, UUID `00001101-0000-1000-8000-00805F9B34FB`):

- **Transport Layer**: OPPOv1 7-bit variable length packet framing:
  ```text
  [0xAA] [Encoded Length] [0x00, 0x00] [Inner Payload]
  ```
- **Inner Realme TL Packet**:
  ```text
  [Command LE (2B)] [Transfer ID (1B)] [Payload Length LE (2B)] [Payload (NB)]
  ```
- **Commands**:
  - `0x0403`: Feature Switch (`[0x06, 0x01]` for Game Mode ON, `[0x06, 0x00]` for OFF)
  - `0x0404`: Noise Reduction (`[0x01, 0x01, Value]`: `0x08` ANC, `0x02` Transparency, `0x01` Normal)

---

## 📄 License

Created for personal and community use. Not officially affiliated with realme or OPPO.
