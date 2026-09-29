# 📖 Instructions for realme Buds Controller (Windows & Android)

Step-by-step instructions for running the realme Buds Controller on **Windows** and **Android**.

---

## 🪟 Windows Instructions

### Prerequisites
- **OS**: Windows 10 or Windows 11 (64-bit)
- **Bluetooth**: A PC with Bluetooth turned on
- **File**: `Realme_Buds_T200x.exe` (standalone — no Python needed)

### 1. Pair Your Earbuds
1. Open Windows **Settings → Bluetooth & devices → Add device**.
2. Put your earbuds in pairing mode (hold the case button until the LED blinks).
3. Select your realme Buds and pair them.

> The buds must be paired **and connected** in Windows before the Bluetooth COM port appears. Windows only creates the outgoing serial port for a live device.

### 2. Run the Application
1. Double-click **`Realme_Buds_T200x.exe`**.
2. The app scans for Bluetooth serial ports, sorts them numerically, and shows each port's description in the dropdown — so you can tell the buds' port apart from other COM ports.
3. The last port you used is pre-selected (saved in `config.json`).

### 3. Connect
1. Pick your port from the dropdown (use the 🔄 button to re-scan) and click **Connect**.
2. Or tick **Auto-connect on launch** so the app connects automatically every time you start it.
3. Watch the **status badge** in the top bar:

   | Badge | Meaning |
   | :--- | :--- |
   | 🟢 **Connected (COMx)** | Live connection on that port — controls are enabled. |
   | ⚪ **Connecting...** | Handshake in progress, wait a moment. |
   | 🔴 **Disconnected** | Not connected — check the port, or that the buds are awake. |

### 4. Noise Control — Three Tiles
Click any of the three circular tiles:

| Tile | What it does |
| :--- | :--- |
| 🔇 **Noise cancellation** | ANC on — blocks ambient background noise. |
| ⚪ **Off** | Normal mode — standard playback, no active filtering. |
| 👂 **Transparency** | Passes surrounding voices and ambient sound through. |

- The selected tile gets a **highlighted (active) icon** and a bold label; hover shows which tile you're about to press.
- A **busy lock** prevents double-clicks from firing while a command is still in flight — wait for the tile to settle before clicking again.
- If you're not connected, the app reminds you instead of sending anything.

> There is no "ANC Alternate" tile in the GUI — the GUI offers exactly these three modes. The undocumented 4th value (`0x04`) is only available in the optional CLI (see below).

### 5. Game Mode Switch
- Flip the **Game mode** switch for super-low audio latency (great for gaming and video sync).
- The switch shows the **confirmed device state** — if the buds don't acknowledge the command, the switch snaps back rather than lying to you.

### 6. Reading the Battery Pills
Three pills show live telemetry for **L** (left bud), **R** (right bud), and **Case**:

| Color | Level |
| :--- | :--- |
| 🟢 **Green** | 50% and above |
| 🟡 **Amber** | 20% – 49% |
| 🔴 **Red** | Below 20% |
| ⚪ **`--`** | No value reported (the case shows `--` when it reports 0) |

Battery is polled automatically every ~8 seconds while connected.

### 7. The "Last Action" Line
A status line under the controls records your most recent action together with a **timestamp** (e.g. the time the Transparency command was sent), so you always know what was sent and when.

### 8. Opening the Activity Log
- Click **Activity Log ▼** at the bottom to expand the **Protocol Activity Monitor**.
- It shows the raw `TX` (sent) and `RX` (received) hex traffic, color-coded by direction/type, with timestamps.
- The log is size-capped so it can't slow the app down; click **Clear** to empty it.
- Click **Activity Log ▲** to collapse it again.

### 9. Optional: Command-Line Controller
For scripting or protocol debugging there's a plain-text menu version:

1. Double-click **`run_t200x_controller.bat`** (or run `python t200x_controller.py`).
2. It opens the configured COM port and shows a numbered menu: Game Mode ON/OFF, Transparency, Noise Cancelling, Normal Mode — plus a 4th **"ANC Alternate"** option that the GUI does not expose.
3. Each action prints the exact `TX` and `RX` bytes, so you can see the protocol in action.
4. Press `0` to exit.

> The CLI uses the COM port configured at the top of `t200x_controller.py` — edit it if your buds aren't on that port.

---

## 🤖 Android Instructions

### Prerequisites
- **OS**: Android 7.0 (Nougat, API 24) up to Android 15 / 16 (targetSdk 36)
- **Bluetooth**: Turned on, with your earbuds paired
- **File**: `Realme_Buds_Controller.apk`
- **App name**: **RealmeBuds**

### 1. Transfer & Install
1. Copy **`Realme_Buds_Controller.apk`** to your phone (download it directly, or send it via USB/cloud).
2. Tap the APK in your file manager and tap **Install**.
3. If prompted, allow installation from **unknown sources** for your file manager/browser.

### 2. Grant Permissions
The app asks for Bluetooth permissions on first launch — the prompt differs by Android version:

| Android version | What's requested | Why |
| :--- | :--- | :--- |
| **Android 12+** (API 31+) | **Bluetooth** (`Nearby devices`) — `BLUETOOTH_CONNECT` + `BLUETOOTH_SCAN` | The modern runtime Bluetooth permissions. |
| **Android 7 – 11** (API 24–30) | **Location** + legacy Bluetooth (`BLUETOOTH`, `BLUETOOTH_ADMIN`) | Older Android requires location access for Bluetooth scanning, even though the app never uses your location. |

Tap **Allow**. If you tapped *Don't allow*, enable it manually later under **Settings → Apps → RealmeBuds → Permissions → Bluetooth / Nearby devices**.

### 3. Pair Your Earbuds
1. Open your phone's **Bluetooth settings**.
2. Put the buds in pairing mode and pair them as usual.

### 4. Open the App & Select Your Device
1. Launch **RealmeBuds** from your app drawer.
2. Tap the **⋮ menu** in the top bar → **Select Device**.
3. Your paired earbuds appear in the picker — tap one to select it.
   - If a device is missing, tap **Refresh**, or pair it first in system settings.

### 5. Connect
- Tap **Connect** in the ⋮ menu (it reads **Disconnect** when already connected).
- The header shows **Connected ●** in green when the session is live.
- Connection problems appear as an **error banner** instead of a silent failure.

### 6. Noise Control — Three Cards
Tap any of the three cards in the **Noise control** card:

| Card | Mode |
| :--- | :--- |
| 🔇 **Noise cancellation** | ANC on |
| ⚪ **Off** | Normal mode |
| 👂 **Transparency** | Ambient sound passes through |

- The selected card gets an **animated ring** around its icon plus **haptic feedback** when tapped.
- All three cards are **disabled while disconnected** — connect first.

### 7. Game Mode Toggle
Flip the **Game mode** switch for low-latency gaming and video sync (available while connected).

### 8. Battery Pills
The hero section shows three pills — **L**, **R**, and **Case**:

| Color | Level |
| :--- | :--- |
| 🟢 Green | ≥ 50% |
| 🟡 Amber | 20% – 49% |
| 🔴 Red | < 20% |
| `--` | Not reported (the case shows `--` when it reports 0) |

Battery refreshes automatically every **8 seconds** while connected.

### 9. Protocol Monitor
- Open the **Protocol Monitor** dialog to watch the raw `TX` and `RX` packets in real time.
- It auto-scrolls and color-codes each line; close it with the back button/dismiss gesture.

### 🧯 Android Troubleshooting
| Problem | Fix |
| :--- | :--- |
| **Permission denied** | **Settings → Apps → RealmeBuds → Permissions** → allow *Bluetooth* (Android 12+) or *Location* (Android 7–11), then reopen the app. |
| **Device not listed** | Pair the buds in system Bluetooth settings first, then reopen **⋮ → Select Device** and tap **Refresh**. |
| **Won't connect / dropped** | Tap **Disconnect**, wait a couple of seconds, tap **Connect** again. |
| **Fights with Realme Link** | Only **one** app can hold the RFCOMM session — close Realme Link first. |
| **Permission prompt never appears** | Android remembers a previous "Don't allow". Set the permission manually in system settings. |

---

## 🔄 Reinstalling / Updating

**Windows** — just run the new `Realme_Buds_T200x.exe`; there's nothing to uninstall first. Your `config.json` (last port, auto-connect, activity-log preference) is kept alongside the app, so settings survive an update.

**Android** — install the new `Realme_Buds_Controller.apk` over the existing app (same signature). If Android reports a signature conflict, uninstall first (you'll need to re-grant permissions).

## 🗑️ Uninstall

**Windows** — delete `Realme_Buds_T200x.exe` (and optionally `config.json`, which holds your saved port and options). Nothing is written to the registry.

**Android** — long-press the **RealmeBuds** icon → **Uninstall** (or **Settings → Apps → RealmeBuds → Uninstall**). This removes the app; your system Bluetooth pairings are unaffected.
