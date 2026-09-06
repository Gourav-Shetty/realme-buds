# Instructions for realme Buds Controller

Follow these simple steps to run and use the application on any Windows PC.

---

## Prerequisites
- **Operating System**: Windows 10 or Windows 11 (64-bit)
- **Bluetooth**: PC must have Bluetooth enabled
- **Earbuds**: realme Buds (T200x, T300, T100, Air series, etc.) paired to your PC

---

## Step 1: Pair Earbuds to Windows
1. Place your earbuds in their charging case with the lid open.
2. Press and hold the pairing button on the case until the status LED begins to blink.
3. On your PC, open **Settings** > **Bluetooth & devices** > **Add device** > **Bluetooth**.
4. Select your realme buds from the list and complete pairing.

---

## Step 2: Run the Application
1. Double-click **`Realme_Buds_T200x.exe`**.
2. No installation is required. It opens directly.
3. The app will automatically scan for the Bluetooth Serial Port and connect.

---

## Step 3: If Not Automatically Connected
1. If the status badge at top right says **Disconnected**:
2. Click the **🔄 (Refresh)** button next to the port dropdown.
3. Select your Bluetooth COM port from the dropdown menu (commonly `COM3`, `COM4`, etc.).
4. Click **Connect**.
5. Once connected, the badge turns green (🟢 **Connected**).

---

## Step 4: Using the Controls

### Noise Control (ANC)
Click any of the 4 cards:
- **Noise Cancelling (🔇)**: Filters out background ambient noise.
- **Transparency (👂)**: Allows external sounds and human voices to pass through.
- **Normal Mode (⚪)**: Standard playback without filtering.
- **ANC Alternate (⚡)**: Alternate noise reduction curve.

*Note: On models without ANC hardware (such as Realme Buds T100), the ANC buttons will have no effect.*

### Game Mode (Low Latency)
- Click the switch to toggle **Game Mode** between **ON** and **OFF**.
- When turned ON, audio delay is reduced to ~50-88ms for video and gaming sync.
- You will hear the prompt audio chime in your earbuds confirming the switch.

### Protocol Monitor / Debug Log
- Click **▼ Show Activity & Protocol Monitor** at the bottom to expand the live packet log.
- This displays real-time `[TX]` (transmitted) and `[RX]` (received) hex communication with your earbuds.
