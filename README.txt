realme Buds Controller (Windows & Android)
==========================================

An unofficial, cross-platform controller for realme Buds earbuds.
One lightweight app for your Windows PC, one native app for your Android
phone. Both speak the same Bluetooth SPP/RFCOMM protocol (OPPOv1 / Realme
TL) that the official Realme Link app uses.

Not affiliated with or endorsed by realme or OPPO.


DOWNLOADS
---------
Windows:  Realme_Buds_T200x.exe   (portable, ~32 MB, Windows 10/11 64-bit)
Android:  Realme_Buds_Controller.apk  (~8 MB, Android 7.0 up to 15/16)

Both files are in the repository root:
https://github.com/Gourav-Shetty/realme-buds


FEATURES
--------
Windows (Realme_Buds_T200x.exe):
  - Auto-discovers Bluetooth COM ports (sorted numerically, with port
    descriptions) and can auto-connect on launch
  - Connection status badge: green = connected, grey = connecting,
    red = disconnected/error
  - Three noise-control tiles: Noise cancellation, Off, Transparency
    (with hover/selected feedback and a busy lock against double-clicks)
  - Game Mode switch that reflects the confirmed device state
  - Three color-coded battery pills: Left, Right, Case
  - "Last action" status line with a timestamp
  - Collapsible Activity Log: color-coded TX/RX hex packet monitor
    (size-capped, with a Clear button)
  - Optional command-line menu: run_t200x_controller.bat

Android (RealmeBuds):
  - Permission flow for Android 12+ (Bluetooth) and Android 7-11
    (location + legacy Bluetooth)
  - Paired-device picker, connect/disconnect with an error banner
  - Three color-coded battery pills: Left, Right, Case
  - Three noise-control cards with animated selection ring + haptics
    (disabled while disconnected)
  - Game mode toggle
  - Protocol monitor dialog (auto-scrolling, color-coded TX/RX log)
  - Live battery polling every 8 seconds


THE THREE ANC MODES
-------------------
  Noise cancellation  - ANC on, blocks ambient background noise   (0x08)
  Off                 - normal playback, no active filtering      (0x01)
  Transparency        - passes surrounding voices and sound
                        through                                    (0x02)

An undocumented fourth value (0x04, "ANC Alternate") exists in the
protocol; it is only offered by the Windows CLI menu, not by the GUI
or the Android app.


BATTERY PILL COLORS
-------------------
  Green   50% and above
  Amber   20% - 49%
  Red     below 20%
  --      no value reported (the charging case shows "--" when it
          reports 0)

Left bud = 0x01, right bud = 0x02, charging case = 0x03 in the
battery telemetry reply.


QUICK START - WINDOWS
---------------------
1. Pair your realme Buds in Windows Settings > Bluetooth & devices.
2. Run Realme_Buds_T200x.exe (no Python or drivers needed).
3. Pick your Bluetooth COM port from the dropdown (ports are listed
   with their descriptions) or tick "Auto-connect on launch".
4. Click Connect. The badge turns green.
5. Use the three noise tiles, the Game mode switch, and read the
   battery pills. Expand "Activity Log" to watch raw packets.

Bluetooth COM port missing? The buds must be paired AND connected
in Windows - Windows only creates the port for a live device. Click
the refresh button or check Device Manager > Ports.


QUICK START - ANDROID
---------------------
1. Pair your realme Buds in the phone's Bluetooth settings.
2. Install Realme_Buds_Controller.apk (allow "install from unknown
   sources" if asked).
3. Open the RealmeBuds app and grant the Bluetooth permission
   (Android 12+) or the Location permission (Android 7-11).
4. Tap the ... menu > Select Device, choose your earbuds, tap Connect.
5. Tap a noise card or flip the Game mode switch. Battery pills and
   the Protocol Monitor update live.

Permission denied? Enable it under Settings > Apps > RealmeBuds >
Permissions. Device missing? Pair it in system settings first, then
Refresh in the device picker.


BUILDING FROM SOURCE
--------------------
Windows (Python 3.10+):
    python -m venv .venv
    .venv\Scripts\activate
    pip install -r requirements.txt
    python t200x_gui.py          # run from source
    python build_exe.py          # build dist/Realme_Buds_T200x.exe

Android (JDK 17 + Android SDK, sdk.dir set in android-app/local.properties):
    cd android-app
    gradlew.bat assembleRelease    # signed, sideload-ready APK
    # APK: app/build/outputs/apk/release/app-release.apk
    # (add android-app/keystore.properties to sign with your own key)

Source code: https://github.com/Gourav-Shetty/realme-buds


DISCLAIMER
----------
This is an independent, unofficial tool. It is not affiliated with,
endorsed by, or sponsored by realme or OPPO.

Not implemented on either platform: EQ / equalizer presets, sound
effects, and dual-device (multi-point) connection. These are not
hidden features - they simply do not exist in this project.

Only one app at a time can hold the earbuds' Bluetooth serial
session. Close the Realme Link app (or this app) before connecting
with the other.

Created for personal and community use.
