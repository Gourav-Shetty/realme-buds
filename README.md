# 🎧 realme Buds Controller (Windows & Android)

A cross-platform controller for **realme Buds** earbuds — one lightweight app for your **Windows PC** and one native app for your **Android** phone. Toggle noise control, Game Mode, and watch live battery telemetry over the same Bluetooth protocol the official Realme Link app uses.

![Platform](https://img.shields.io/badge/platform-Windows%20%7C%20Android-2563EB?style=flat-square)
![Protocol](https://img.shields.io/badge/protocol-OPPOv1%20RFCOMM-10B981?style=flat-square)

---

## 📦 Downloads

Pre-built standalone releases live in this repository:

| Platform | Download File | Size | Description |
| :--- | :--- | :---: | :--- |
| 🪟 **Windows** | **[`Realme_Buds_T200x.exe`](Realme_Buds_T200x.exe)** | ~32 MB | **Standalone portable `.exe`**<br>• Runs instantly — no Python or drivers needed.<br>• Supports Windows 10 & 11 (64-bit). |
| 🤖 **Android** | **[`Realme_Buds_Controller.apk`](Realme_Buds_Controller.apk)** | ~8 MB | **Native Android APK**<br>• Built with Jetpack Compose & Kotlin.<br>• Supports Android 7.0 (API 24) up to Android 15/16 (targetSdk 36). |

---

## ⚡ Features

### 🪟 Windows (`Realme_Buds_T200x.exe`)

- **Auto port discovery**: scans for Bluetooth serial (COM) ports, sorts them numerically, and shows each port's description so you can pick the right one. A 🔄 refresh button re-scans at any time.
- **Auto-connect option**: remember the last port and connect automatically on launch (saved in `config.json`).
- **Connection badge**: a live status pill — green when connected (shows the active port), grey while connecting, red on disconnect or error.
- **Three noise-control modes** as circular buttons with hover and selected feedback, and a busy lock so a second click can't fire while a command is in flight:
  - 🔇 **Noise cancellation** — blocks ambient background noise
  - ⚪ **Off** — standard playback, no active filtering
  - 👂 **Transparency** — passes surrounding voices and sounds through
- **Game Mode switch** that reflects the *confirmed* device state, not just what you clicked — it snaps back if the buds don't acknowledge the command.
- **Three color-coded battery pills** — Left, Right, and Case — green at ≥50%, amber at 20–49%, red below 20%.
- **"Last action" status line** with a timestamp, so you always know what was sent and when.
- **Collapsible protocol activity monitor**: a color-coded `TX`/`RX` hex log of the raw traffic, capped so it can't grow forever, with a **Clear** button.
- **CLI alternative**: `t200x_controller.py`, launched with `run_t200x_controller.bat`, is a plain-text menu for scripting or debugging. It additionally exposes the undocumented 4th "ANC Alternate" value (`0x04`) that the GUI deliberately does not offer.

### 🤖 Android (`RealmeBuds`)

- **Sensible permission flow**: requests `BLUETOOTH_CONNECT` + `BLUETOOTH_SCAN` on Android 12+, and legacy Bluetooth + location permissions on Android 7–11.
- **Paired-device picker**: choose from the earbuds your phone has already paired.
- **Connect / disconnect** with a clear error banner when something goes wrong.
- **Hero battery readout** with three color-coded pills — Left, Right, Case (green ≥50%, amber 20–49%, red <20%).
- **Noise-control card** with three modes, an animated selected ring, haptic feedback on tap, and controls that disable themselves while disconnected.
- **Game mode toggle** for low-latency audio and video sync.
- **Protocol monitor dialog**: auto-scrolling, color-coded `TX`/`RX` log of the raw packets.
- **8-second live battery polling** while connected.

### Shared by both apps

Both apps speak the same wire protocol — **OPPOv1 framing + Realme TL packets** over Bluetooth SPP/RFCOMM (UUID `00001101-0000-1000-8000-00805F9B34FB`). See [How it works](#-how-it-works-protocol-overview) below.

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

## 🚀 Quick Start

### 🪟 Windows

1. Pair your realme Buds with your PC: **Settings → Bluetooth & devices → Add device**, and put the buds in pairing mode (hold the case button until the LED blinks).
2. Download and double-click **[`Realme_Buds_T200x.exe`](Realme_Buds_T200x.exe)** — nothing else to install.
3. The app lists the Bluetooth COM ports it finds (numerically sorted, with descriptions) and pre-selects the last port you used. Tick **Auto-connect on launch** to skip this step next time.
4. Click **Connect** — the badge turns green.
5. Tap a noise tile, flip the **Game mode** switch, and read the battery pills.

Full walkthrough: [`INSTRUCTIONS.md`](INSTRUCTIONS.md)

### 🤖 Android

1. Pair your realme Buds in your phone's **Bluetooth settings**.
2. Download **[`Realme_Buds_Controller.apk`](Realme_Buds_Controller.apk)** and tap **Install** (enable *"Install from unknown sources"* if prompted).
3. Open the **RealmeBuds** app and grant the Bluetooth permission when asked.
4. Open the **⋮ menu → Select Device**, pick your earbuds, and tap **Connect**.
5. Tap any of the three noise cards or toggle the **Game mode** switch.

---

## 🛠️ Building from Source

### 🪟 Windows

Requires Python 3.10+ (64-bit).

```bat
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
python t200x_gui.py
```

`requirements.txt` installs `pyserial`, `Pillow`, `customtkinter`, and `pyinstaller`.

To produce the standalone executable:

```bat
python build_exe.py
```

This runs PyInstaller (onefile, `--collect-all=customtkinter`) and writes `dist/Realme_Buds_T200x.exe`, which the script then copies to the repository root as `Realme_Buds_T200x.exe`.

### 🤖 Android

Requires **JDK 17** and the **Android SDK** (point `sdk.dir` at your SDK in `android-app/local.properties`).

```bat
cd android-app
.\gradlew.bat assembleDebug
```

or `.\gradlew.bat assembleRelease` for a release build. The APK lands in:

- Debug: `android-app/app/build/outputs/apk/debug/app-debug.apk`
- Release: `android-app/app/build/outputs/apk/release/app-release.apk` — **signed and sideload-ready**. It uses your own key if you create `android-app/keystore.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword` — gitignored); otherwise it falls back to the debug keystore so the APK always installs and upgrades in place.

The distributed [`Realme_Buds_Controller.apk`](Realme_Buds_Controller.apk) in the repository root is the built artifact.

---

## 📂 Repository Layout

```text
realme-buds/
├─ t200x_gui.py                 # Windows GUI (customtkinter)
├─ t200x_core.py                # Protocol + threading core (shared by GUI)
├─ t200x_controller.py          # CLI menu alternative
├─ run_t200x_controller.bat     # Double-click launcher for the CLI
├─ build_exe.py                 # PyInstaller build script
├─ Realme_Buds_T200x.spec       # PyInstaller spec
├─ config.json                  # GUI settings: last_port, auto_connect, show_logs
├─ requirements.txt             # pyserial, Pillow, customtkinter, pyinstaller
├─ Realme_Buds_T200x.exe        # Pre-built Windows app (tracked in the repo)
├─ Realme_Buds_Controller.apk   # Pre-built Android app (tracked in the repo)
├─ app_icon.*, realme_buds_case*.png, anc_*.png   # Icons & tile artwork
├─ README.md / README.txt / INSTRUCTIONS.md
└─ android-app/                 # Jetpack Compose + Kotlin app
   ├─ gradlew / gradlew.bat
   ├─ build.gradle.kts, settings.gradle.kts, gradle.properties
   └─ app/src/main/java/com/example/realmebuds/
      ├─ MainActivity.kt, Navigation.kt
      ├─ bluetooth/BluetoothController.kt     # RFCOMM socket + serialised I/O
      ├─ protocol/RealmeProtocol.kt           # OPPOv1 / Realme TL encoder
      └─ ui/main/                             # Compose screens & view model
```

---

## 🔎 How It Works (Protocol Overview)

Communication happens directly over Bluetooth RFCOMM (Serial Port Profile, UUID `00001101-0000-1000-8000-00805F9B34FB`) — no proprietary SDK involved.

- **Transport layer** — OPPOv1 single-frame wrapper with a 7-bit variable-length length field:

  ```text
  [0xAA] [7-bit varint encoded length] [0x00] [0x00] [inner payload]
  ```

  The encoded length is `len(inner payload) + 2` (the two `0x00` bytes count toward it).

- **Inner Realme TL packet**:

  ```text
  [command u16 LE] [transfer ID u8] [payload length u16 LE] [payload]
  ```

- **Transfer ID** — a rolling 0–255 counter, incremented after every write.

- **Commands**:

  | Command | Purpose | Payload |
  | :--- | :--- | :--- |
  | `0x0403` | Feature switch | `[0x06, 0x01]` Game Mode **ON**, `[0x06, 0x00]` **OFF** |
  | `0x0404` | Noise reduction | `[0x01, 0x01, Value]` — `0x08` ANC (noise cancelling), `0x02` Transparency, `0x01` Normal/Off |
  | `0x0106` | Battery telemetry query | Sent as the raw frame `AA 07 00 00 06 01 [seq] 00 00` |

- **Battery reply** — the response to `0x0106` carries `(device, percent)` pairs, where `0x01` = Left earbud, `0x02` = Right earbud, `0x03` = Charging case.

- **ANC Alternate (`0x04`)** — an undocumented fourth noise-reduction value. The GUI and Android app deliberately expose only the three documented modes; the Windows CLI (`t200x_controller.py`) offers it as an extra menu option.

---

## 🧯 Troubleshooting

| Problem | What to try |
| :--- | :--- |
| **COM port not listed (Windows)** | Make sure the buds are paired *and* connected in Windows Bluetooth settings — Windows only creates the outgoing Bluetooth COM port for a live device. Turn Bluetooth off/on, click the 🔄 refresh button, and check **Device Manager → Ports (COM & LPT)** for the Bluetooth serial entry. |
| **"No response received"** | The buds are likely asleep in the case, already claimed by another program, or you're on the wrong COM port. Take the buds out of the case, confirm the port, and try again. |
| **Android 12+ permission denied** | Go to **Settings → Apps → RealmeBuds → Permissions → Bluetooth** and set it to *Allow*. If you previously chose *Don't allow*, the app can't re-prompt — you must flip it manually. |
| **Earbuds in use by Realme Link** | Only **one** app can hold the RFCOMM serial session at a time. Force-close or disconnect from the Realme Link app, then connect here. |
| **Battery pills show `--`** | The case reports `0` when it has nothing to share (for example when the buds are in use outside the case). The case pill shows `--` until the case reports a real value. |

---

## ⚠️ Limitations & Disclaimer

- This project is **not affiliated with, endorsed by, or sponsored by realme or OPPO**. It is an independent, unofficial tool.
- **Not implemented on purpose:** EQ / equalizer presets, sound effects, and dual-device (multi-point) connection are **not** part of this project — on either platform. Please don't look for them; they aren't hidden behind a setting.
- The charging-case battery pill shows `--` whenever the case reports `0`.
- The `0x04` "ANC Alternate" value is undocumented and only exposed through the Windows CLI.

---

## 📄 License & Contributing

Created for **personal and community use**. Not officially affiliated with realme or OPPO.

Bug reports, protocol discoveries, and pull requests are welcome — open an issue or PR on [GitHub](https://github.com/Gourav-Shetty/realme-buds).
