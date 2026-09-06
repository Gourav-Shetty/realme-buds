# 🎧 realme Buds Windows Controller

A clean, modern, ultra-fast standalone Windows desktop application for controlling **realme Buds** features directly from your PC.

Easily toggle **Active Noise Cancellation (ANC)**, **Transparency Mode**, and **Super Low-Latency Gaming Mode** without touching your phone.

---

## ⚡ Highlights

- **Single Portable `.exe`**: 100% standalone—no Python installation or extra dependencies required.
- **Fast & Responsive**: Sub-second startup time with fluid 60fps asynchronous Bluetooth communication.
- **Auto-Discovery**: Automatically scans and detects Bluetooth RFCOMM serial ports on Windows.
- **Live Activity Monitor**: Built-in collapsible protocol inspector to monitor raw hex transmission (`TX`/`RX`) in real time.
- **Settings Persistence**: Remembers your preferred COM port and auto-connects on app launch.

---

## 📱 Supported Devices

The application utilizes the universal **Realme Link / OPPOv1** Bluetooth protocol:

| Device | Game Mode (Low Latency) | Active Noise Cancellation (ANC) | Transparency Mode |
| :--- | :---: | :---: | :---: |
| **realme Buds T200x** | ✅ Supported | ✅ Supported | ✅ Supported |
| **realme Buds T300** | ✅ Supported | ✅ Supported | ✅ Supported |
| **realme Buds T100 / T110** | ✅ Supported | ❌ *(Hardware has no ANC)* | ❌ *(Hardware has no ANC)* |
| **realme Buds Air Series (Air 3, Air 5, Air 6)** | ✅ Supported | ✅ Supported | ✅ Supported |
| **Compatible OPPO / OnePlus Buds** | ✅ Supported | ✅ Supported | ✅ Supported |

---

## 🚀 Quick Start Guide

### 1. Pair Your Earbuds
1. Open Windows **Settings** > **Bluetooth & devices**.
2. Put your realme earbuds into pairing mode (open the case and hold the button until the LED blinks).
3. Connect your earbuds to Windows.

### 2. Launch the App
1. Run **`Realme_Buds_T200x.exe`**.
2. The app will automatically scan for Bluetooth Serial Ports (e.g., `COM4`).
3. If not connected automatically:
   - Select your earbud port from the dropdown menu.
   - Click **Connect**.
4. The status badge will switch to 🟢 **Connected**.

### 3. Control Your Audio
- Click any of the **Noise Control** tiles:
  - 🔇 **Noise Cancelling**: Blocks ambient background noise.
  - 👂 **Transparency**: Amplifies ambient voices and surroundings.
  - ⚪ **Normal Mode**: Turns off noise filtering.
  - ⚡ **ANC Alternate**: Alternate noise suppression curve.
- Toggle the **Game Mode switch** for super low-latency audio sync while gaming or watching videos.

---

## 🔍 How to Find Your COM Port Manually

If Windows does not automatically detect your port:
1. Press `Win + R`, type `devmgmt.msc`, and press **Enter** to open **Device Manager**.
2. Expand **Ports (COM & LPT)**.
3. Look for **Standard Serial over Bluetooth link** (e.g., `COM3`, `COM4`, `COM5`).
4. Select that port in the app dropdown and click **Connect**.

Alternatively, in Windows Bluetooth settings:
- Go to `Settings` > `Bluetooth & devices` > `Devices` > `More Bluetooth settings` > **COM Ports** tab to view the assigned outgoing port.

---

## 🛠️ Technical Protocol Overview

Communication is handled over Bluetooth RFCOMM (default baud: `115200`):

- **Transport Layer**: OPPOv1 7-bit variable length packet framing:
  ```text
  [0xAA] [Encoded Length] [0x00, 0x00] [Inner Payload]
  ```
- **Inner Realme TL Packet**:
  ```text
  [Command LE (2B)] [Transfer ID (1B)] [Payload Length LE (2B)] [Payload (NB)]
  ```
- **Key Commands**:
  - `0x0403`: Feature Switch (`[0x06, 0x01]` for Game Mode ON, `[0x06, 0x00]` for OFF)
  - `0x0404`: Noise Reduction (`[0x01, 0x01, Value]`: `0x08` ANC, `0x02` Transparency, `0x01` Normal)

---

## 📄 License

Created for personal and community use. Not officially affiliated with realme or OPPO.
