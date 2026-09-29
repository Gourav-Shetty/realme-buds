"""
Realme Buds T200x Core Protocol & Communication Controller
Handles OPPOv1 framing, serial communication, and background worker threads.
"""

import re
import time
import queue
import threading
from typing import Optional, Callable, Dict, Any, List
import serial
import serial.tools.list_ports


# ============================================================
# PROTOCOL CONSTANTS
# ============================================================

CMD_SWITCH = 0x0403       # Decimal 1027
CMD_NOISE = 0x0404        # Decimal 1028
GAME_FEATURE = 0x06

ANC_MODES = {
    "noise_cancelling": {
        "name": "Noise Cancelling",
        "value": 0x08,
        "icon": "🔇",
        "desc": "Blocks ambient background noise"
    },
    "transparency": {
        "name": "Transparency",
        "value": 0x02,
        "icon": "👂",
        "desc": "Passes external voices & sound through"
    },
    "normal": {
        "name": "Normal Mode",
        "value": 0x01,
        "icon": "⚪",
        "desc": "Standard audio without active filtering"
    },
    "anc_alternate": {
        "name": "ANC Alternate",
        "value": 0x04,
        "icon": "⚡",
        "desc": "Alternative active noise suppression"
    },
}

DEFAULT_BAUD = 115200

# Dropdown label separator: "COM4 — Standard Serial over Bluetooth link"
PORT_LABEL_SEPARATOR = " — "


# ============================================================
# PACKET ENCODING HELPERS
# ============================================================

def encode_length(value: int) -> bytes:
    """OPPOv1 7-bit variable-length encoding."""
    output = bytearray()
    while True:
        chunk = value & 0x7F
        value >>= 7
        if value:
            output.append(chunk | 0x80)
        else:
            output.append(chunk)
            break
    return bytes(output)


def wrap_oppo_v1(payload: bytes) -> bytes:
    """OPPOv1 single-frame wrapper."""
    frame_length = len(payload) + 2
    return (
        bytes([0xAA])
        + encode_length(frame_length)
        + bytes([0x00, 0x00])
        + payload
    )


def make_packet(command: int, transfer_id: int, payload: bytes) -> bytes:
    """
    Construct inner Realme TL packet:
        command       : 2 bytes LE
        transfer ID   : 1 byte
        payload length: 2 bytes LE
        payload       : N bytes
    Then wraps using OPPOv1.
    """
    inner = (
        command.to_bytes(2, "little")
        + bytes([transfer_id & 0xFF])
        + len(payload).to_bytes(2, "little")
        + payload
    )
    return wrap_oppo_v1(inner)


# ============================================================
# PORT ENUMERATION
# ============================================================

def _com_port_index(device: str) -> int:
    """Numeric index of a 'COM<n>' style device name (COM4 sorts before COM40)."""
    match = re.search(r"(\d+)$", device or "")
    return int(match.group(1)) if match else 1 << 30


def is_bluetooth_port(description: str, hwid: str) -> bool:
    """True when a port looks like a Windows Bluetooth (RFCOMM) link."""
    desc_l = (description or "").lower()
    hwid_l = (hwid or "").lower()
    return "bth" in hwid_l or "bluetooth" in hwid_l or "bluetooth" in desc_l


def get_available_ports() -> List[Dict[str, Any]]:
    """
    List all COM ports with metadata identifying Bluetooth / Realme links.

    Ports are sorted numerically by their COM index (COM4 before COM40) and
    every entry carries its Windows ``description`` so UIs can show labels
    such as ``COM4 — Standard Serial over Bluetooth link``.
    """
    ports = []
    for p in serial.tools.list_ports.comports():
        desc = (p.description or "").strip()
        hwid = (p.hwid or "").strip()
        is_bt = is_bluetooth_port(desc, hwid)
        ports.append({
            "device": p.device,
            "description": desc,
            "hwid": hwid,
            "is_bluetooth": is_bt,
            # A port is a Realme candidate only when it is a Bluetooth link.
            # (Never match "COM4" inside "COM40"!)
            "is_candidate": is_bt,
        })
    ports.sort(key=lambda entry: _com_port_index(entry["device"]))
    return ports


def format_port_label(port: Dict[str, Any]) -> str:
    """Human readable dropdown label: 'COM4 — Standard Serial over Bluetooth link'."""
    device = str(port.get("device") or "").strip()
    desc = str(port.get("description") or "").strip()
    if desc and desc.lower() not in (device.lower(), "n/a"):
        return f"{device}{PORT_LABEL_SEPARATOR}{desc}"
    return device


def pick_default_port(ports: List[Dict[str, Any]], preferred: str = "") -> Optional[str]:
    """
    Choose the port to pre-select:
    1. the previously used port, if it still exists,
    2. COM4, but only if that exact port exists,
    3. the first Bluetooth port,
    4. the first available port.
    """
    devices = [p["device"] for p in ports]
    if preferred and preferred in devices:
        return preferred
    if "COM4" in devices:
        return "COM4"
    for p in ports:
        if p.get("is_bluetooth"):
            return p["device"]
    return devices[0] if devices else None


# ============================================================
# CONTROLLER CLASS
# ============================================================

class BudsController:
    """
    Thread-safe asynchronous controller for Realme Buds T200x.
    All serial operations are processed on a dedicated background thread.
    """

    def __init__(self):
        self._serial: Optional[serial.Serial] = None
        self._port: Optional[str] = None
        self._baud: int = DEFAULT_BAUD
        self._transfer_id: int = 0

        self._game_mode: Optional[bool] = None
        self._anc_mode: Optional[str] = None
        self._battery_left: Optional[int] = None
        self._battery_right: Optional[int] = None
        self._battery_case: Optional[int] = None

        self._cmd_queue: queue.Queue = queue.Queue()
        self._running: bool = True

        # Callbacks (assigned by GUI)
        self.on_connection_change: Optional[Callable[[bool, str, Optional[str]], None]] = None
        self.on_state_change: Optional[Callable[[Optional[bool], Optional[str]], None]] = None
        self.on_battery_change: Optional[Callable[[Optional[int], Optional[int], Optional[int]], None]] = None
        self.on_packet_log: Optional[Callable[[str, str, Optional[bytes]], None]] = None
        # Result of every user command: (action, ok, detail).
        # action is "GAME_MODE" or "ANC_MODE"; ok is False when the device did
        # not answer (or the write failed) so the UI can revert optimistically.
        self.on_command_result: Optional[Callable[[str, bool, str], None]] = None

        # Start worker thread
        self._worker_thread = threading.Thread(target=self._worker_loop, daemon=True)
        self._worker_thread.start()

    @property
    def is_connected(self) -> bool:
        return self._serial is not None and self._serial.is_open

    @property
    def game_mode(self) -> Optional[bool]:
        return self._game_mode

    @property
    def anc_mode(self) -> Optional[str]:
        return self._anc_mode

    @property
    def battery_left(self) -> Optional[int]:
        return self._battery_left

    @property
    def battery_right(self) -> Optional[int]:
        return self._battery_right

    @property
    def battery_case(self) -> Optional[int]:
        return self._battery_case

    @property
    def current_port(self) -> Optional[str]:
        return self._port

    def log(self, level: str, message: str, raw_bytes: Optional[bytes] = None) -> None:
        if self.on_packet_log:
            try:
                self.on_packet_log(level, message, raw_bytes)
            except Exception:
                pass

    def _emit_command_result(self, action: str, ok: bool, detail: str) -> None:
        if self.on_command_result:
            try:
                self.on_command_result(action, ok, detail)
            except Exception:
                pass

    def connect(self, port: str, baud: int = DEFAULT_BAUD) -> None:
        """Request connection to port asynchronously."""
        self._cmd_queue.put(("CONNECT", (port, baud)))

    def disconnect(self) -> None:
        """Request disconnect asynchronously."""
        self._cmd_queue.put(("DISCONNECT", None))

    def set_game_mode(self, enabled: bool) -> None:
        """Set Low-Latency Game Mode (ON/OFF)."""
        self._cmd_queue.put(("GAME_MODE", enabled))

    def set_anc(self, mode_key: str) -> None:
        """Set Active Noise Control mode."""
        if mode_key in ANC_MODES:
            self._cmd_queue.put(("ANC_MODE", mode_key))

    def query_battery(self) -> None:
        """Request live battery levels asynchronously."""
        self._cmd_queue.put(("QUERY_BATTERY", None))

    def close(self) -> None:
        """
        Stop the worker and always release the serial port.

        The QUIT item is enqueued *first* while ``_running`` is still True, so
        the worker is guaranteed to handle it and run ``_do_disconnect()``.
        Only afterwards is the run flag cleared, and as a last resort the port
        is closed synchronously if the worker did not exit in time.
        """
        try:
            self._cmd_queue.put(("QUIT", None), timeout=1.0)
        except Exception:
            pass

        if self._worker_thread.is_alive():
            self._worker_thread.join(timeout=2.0)

        # Clear the flag only after the worker had its chance to handle QUIT.
        self._running = False

        # Guarantee the port is released even if the worker was stuck in I/O.
        if self._serial is not None:
            self._do_disconnect()

    # ------------------------------------------------------------
    # INTERNAL WORKER THREAD
    # ------------------------------------------------------------

    def _worker_loop(self) -> None:
        last_battery_poll = 0.0
        while self._running:
            # Poll live battery every 8 seconds if connected. Everything runs
            # on this single worker thread, so the poll is serialised with
            # user commands; it is deferred while commands are queued so a
            # button press is never stuck behind a battery read.
            now = time.time()
            if (
                self.is_connected
                and (now - last_battery_poll) > 8.0
                and self._cmd_queue.empty()
            ):
                last_battery_poll = now
                self._do_query_battery()

            try:
                action, payload = self._cmd_queue.get(timeout=0.25)
            except queue.Empty:
                continue

            try:
                if action == "CONNECT":
                    port, baud = payload
                    self._do_connect(port, baud)
                    if self.is_connected:
                        last_battery_poll = time.time()
                        self._do_query_battery()
                elif action == "DISCONNECT":
                    self._do_disconnect()
                elif action == "GAME_MODE":
                    self._do_set_game_mode(payload)
                elif action == "ANC_MODE":
                    self._do_set_anc(payload)
                elif action == "QUERY_BATTERY":
                    last_battery_poll = time.time()
                    self._do_query_battery()
                elif action == "QUIT":
                    self._do_disconnect()
                    break
            except Exception as e:
                self.log("ERROR", f"Unexpected error during {action}: {e}")
            finally:
                self._cmd_queue.task_done()

    def _do_connect(self, port: str, baud: int) -> None:
        self._do_disconnect(silent=True)
        self.log("INFO", f"Connecting to {port} at {baud} baud...")

        try:
            ser = serial.Serial(
                port=port,
                baudrate=baud,
                timeout=0.25,
                write_timeout=2.0
            )
            self._serial = ser
            self._port = port
            self._baud = baud

            # Drain initial startup/connection noise
            time.sleep(0.6)
            while True:
                discard = ser.read(4096)
                if not discard:
                    break

            self.log("INFO", f"Connected to {port} successfully.")
            if self.on_connection_change:
                self.on_connection_change(True, port, None)

        except Exception as exc:
            error_msg = str(exc)
            self._serial = None
            self._port = None
            self.log("ERROR", f"Connection to {port} failed: {error_msg}")
            if self.on_connection_change:
                self.on_connection_change(False, port, error_msg)

    def _do_disconnect(self, silent: bool = False) -> None:
        if self._serial:
            port = self._port or "Unknown"
            try:
                self._serial.close()
            except Exception:
                pass
            self._serial = None
            self._port = None
            if not silent:
                self.log("INFO", f"Disconnected from {port}.")
                if self.on_connection_change:
                    self.on_connection_change(False, port, None)

    # First byte may take the full timeout, but once data starts arriving we
    # return after this much silence (keeps every button press snappy).
    RESPONSE_IDLE_SETTLE = 0.12
    # Slice length used while polling the port inside _read_response().
    RESPONSE_POLL_SLICE = 0.05

    def _read_response(self, timeout: float = 1.5) -> bytes:
        """
        Read a response without blocking for the whole timeout:

        * wait up to ``timeout`` for the *first* byte,
        * then return as soon as ~120 ms pass with no new data.
        """
        ser = self._serial
        if not ser:
            return b""

        data = bytearray()
        original_timeout = ser.timeout
        try:
            ser.timeout = self.RESPONSE_POLL_SLICE
            deadline = time.time() + timeout
            idle_deadline: Optional[float] = None

            while time.time() < deadline:
                try:
                    chunk = ser.read(4096)
                except Exception as exc:
                    self.log("ERROR", f"Serial read error: {exc}")
                    break

                if chunk:
                    data.extend(chunk)
                    idle_deadline = time.time() + self.RESPONSE_IDLE_SETTLE
                elif idle_deadline is None:
                    continue

                if idle_deadline is not None and time.time() >= idle_deadline:
                    break
        finally:
            try:
                ser.timeout = original_timeout
            except Exception:
                pass

        res = bytes(data)
        if res:
            self._parse_battery_packet(res)
        return res

    def _send_tl_command(self, command: int, payload: bytes, desc: str) -> Optional[bytes]:
        if not self.is_connected or not self._serial:
            self.log("ERROR", f"Cannot send '{desc}': Not connected to any COM port.")
            return None

        packet = make_packet(
            command=command,
            transfer_id=self._transfer_id,
            payload=payload,
        )

        hex_tx = packet.hex(" ").upper()
        self.log("TX", f"[{desc}] {hex_tx}", packet)

        try:
            self._serial.write(packet)
            self._serial.flush()
        except serial.SerialException as exc:
            self.log("ERROR", f"Serial write failed: {exc}")
            self._do_disconnect()
            return None
        finally:
            # transfer_id rule (kept in sync with the Android app): the
            # *current* id is used above and it is incremented after every
            # write attempt, whether or not a response came back.
            self._transfer_id = (self._transfer_id + 1) & 0xFF

        response = self._read_response()
        if response:
            hex_rx = response.hex(" ").upper()
            self.log("RX", f"[{desc}] {hex_rx}", response)
            return response
        else:
            self.log("WARN", f"[{desc}] No response received within timeout.")
            return None

    def _do_set_game_mode(self, enabled: bool) -> None:
        desc = f"Game Mode {'ON' if enabled else 'OFF'}"
        state_byte = 0x01 if enabled else 0x00
        payload = bytes([GAME_FEATURE, state_byte])

        resp = self._send_tl_command(CMD_SWITCH, payload, desc)
        if resp is not None:
            self._game_mode = enabled
            self.log("SUCCESS", f"Game Mode successfully set to {'ON' if enabled else 'OFF'}")
            if self.on_state_change:
                self.on_state_change(self._game_mode, self._anc_mode)
            self._emit_command_result("GAME_MODE", True, desc)
        else:
            self.log("WARN", f"{desc} was not confirmed by the device.")
            self._emit_command_result("GAME_MODE", False, desc)

    def _do_set_anc(self, mode_key: str) -> None:
        info = ANC_MODES[mode_key]
        desc = f"ANC: {info['name']}"
        payload = bytes([0x01, 0x01, info["value"]])

        resp = self._send_tl_command(CMD_NOISE, payload, desc)
        if resp is not None:
            self._anc_mode = mode_key
            self.log("SUCCESS", f"ANC Mode successfully set to {info['name']}")
            if self.on_state_change:
                self.on_state_change(self._game_mode, self._anc_mode)
            self._emit_command_result("ANC_MODE", True, desc)
        else:
            self.log("WARN", f"{desc} was not confirmed by the device.")
            self._emit_command_result("ANC_MODE", False, desc)

    def _do_query_battery(self) -> None:
        if not self.is_connected or not self._serial:
            return

        # transfer_id rule (kept in sync with the Android app): send with the
        # *current* transfer id, then increment after the write attempt.
        seq = self._transfer_id & 0xFF
        # OPPO V1 Battery Query Packet (Category 0x06, Sub-command 0x01)
        pkt = bytes([0xAA, 0x07, 0x00, 0x00, 0x06, 0x01, seq, 0x00, 0x00])

        self.log("TX", f"[Battery Query] {pkt.hex(' ').upper()}", pkt)
        try:
            self._serial.write(pkt)
            self._serial.flush()
        except serial.SerialException as exc:
            self.log("ERROR", f"Battery query write failed: {exc}")
            self._do_disconnect()
            return
        finally:
            self._transfer_id = (self._transfer_id + 1) & 0xFF

        resp = self._read_response(timeout=1.2)
        if resp:
            self.log("RX", f"[Battery Query] {resp.hex(' ').upper()}", resp)

    def _parse_battery_packet(self, data: bytes) -> None:
        for i in range(len(data) - 8):
            if data[i] == 0xAA and data[i + 4] == 0x06 and (data[i + 5] & 0x7F) == 0x01:
                if i + 10 >= len(data):
                    break
                count = data[i + 10]
                left, right, case = None, None, None
                pos = i + 11
                for _ in range(count):
                    if pos + 1 >= len(data):
                        break
                    dev_id = data[pos]
                    pct = data[pos + 1]
                    if dev_id == 0x01:
                        left = pct
                    elif dev_id == 0x02:
                        right = pct
                    elif dev_id == 0x03:
                        case = pct
                    pos += 2

                if left is not None or right is not None or case is not None:
                    self._battery_left = left
                    self._battery_right = right
                    self._battery_case = case
                    self.log("INFO", f"Live Battery update: L={left}%, R={right}%, Case={case}%")
                    if self.on_battery_change:
                        self.on_battery_change(left, right, case)
                break

