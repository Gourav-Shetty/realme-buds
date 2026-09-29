import serial
import time
from typing import Optional

# ---------------------------------------------------------------------------
# Protocol helpers and constants are shared with the desktop app: there is a
# single implementation of the OPPOv1 framing / Realme TL codec in
# t200x_core.py, so this CLI cannot drift from it.
#   encode_length / wrap_oppo_v1 are re-exported here for backwards
#   compatibility with scripts that imported them from this module.
# ---------------------------------------------------------------------------
from t200x_core import (
    ANC_MODES,
    CMD_NOISE,
    CMD_SWITCH,
    GAME_FEATURE,
    encode_length,
    make_packet,
    wrap_oppo_v1,
)

# Public surface of this module: the protocol names above are re-exported
# from t200x_core (single source of truth, also used by the desktop app).
__all__ = [
    # protocol (from t200x_core)
    "ANC_MODES", "CMD_NOISE", "CMD_SWITCH", "GAME_FEATURE",
    "encode_length", "make_packet", "wrap_oppo_v1",
    # local CLI
    "PORT", "BAUD", "ANC_VALUES",
    "read_response", "send_command", "drain_startup",
    "set_game_mode", "set_anc", "print_menu", "main",
]


# ============================================================
# T200x CONNECTION
# ============================================================

PORT = "COM4"
BAUD = 115200


# ============================================================
# USER-FACING ANC MODES
# ============================================================
# Menu keys map onto the canonical ANC value table in t200x_core (which keeps
# all 4 entries, including the "ANC Alternate" value 0x04).

ANC_VALUES = {
    "3": (ANC_MODES["transparency"]["name"], ANC_MODES["transparency"]["value"]),
    "4": (ANC_MODES["noise_cancelling"]["name"], ANC_MODES["noise_cancelling"]["value"]),
    "5": (ANC_MODES["normal"]["name"], ANC_MODES["normal"]["value"]),

    # This value is known to produce Noise Cancelling on your
    # T200x, but is kept separate because its internal meaning
    # is not yet fully established.
    "6": (ANC_MODES["anc_alternate"]["name"], ANC_MODES["anc_alternate"]["value"]),
}


# ============================================================
# SERIAL / RESPONSE HANDLING
# ============================================================

def read_response(ser: serial.Serial, timeout: float = 1.5) -> bytes:
    """
    Read available response data for a short period.
    """

    data = bytearray()
    end_time = time.time() + timeout

    while time.time() < end_time:
        chunk = ser.read(4096)

        if chunk:
            data.extend(chunk)

        else:
            time.sleep(0.02)

    return bytes(data)


def send_command(
    ser: serial.Serial,
    command: int,
    payload: bytes,
    transfer_id: int,
) -> bytes:
    """
    Send one TL command and print its TX/RX.
    """

    wire_packet = make_packet(
        command=command,
        transfer_id=transfer_id,
        payload=payload,
    )

    print()
    print(f"TX: {wire_packet.hex(' ')}")

    try:
        ser.write(wire_packet)
        ser.flush()

    except serial.SerialException as exc:
        print(f"Serial write failed: {exc}")
        return b""

    response = read_response(ser)

    if response:
        print(f"RX: {response.hex(' ')}")
    else:
        print("RX: (no response)")

    return response


def drain_startup(ser: serial.Serial) -> None:
    """
    Discard connection/startup traffic so the menu starts cleanly.
    """

    time.sleep(0.8)

    while True:
        data = ser.read(4096)

        if not data:
            break


# ============================================================
# COMMANDS
# ============================================================

def set_game_mode(
    ser: serial.Serial,
    enabled: bool,
    transfer_id: int,
) -> bytes:
    """
    Game Mode:

        command = 1027 / 0x0403
        payload = [06, 01] ON
                  [06, 00] OFF
    """

    state = 0x01 if enabled else 0x00

    return send_command(
        ser=ser,
        command=CMD_SWITCH,
        payload=bytes([
            GAME_FEATURE,
            state,
        ]),
        transfer_id=transfer_id,
    )


def set_anc(
    ser: serial.Serial,
    value: int,
    transfer_id: int,
) -> bytes:
    """
    Noise/ANC:

        command = 1028 / 0x0404
        payload = [01, 01, XX]
    """

    return send_command(
        ser=ser,
        command=CMD_NOISE,
        payload=bytes([
            0x01,
            0x01,
            value,
        ]),
        transfer_id=transfer_id,
    )


# ============================================================
# DISPLAY
# ============================================================

def print_menu(
    game_on: Optional[bool],
    anc_name: str,
) -> None:

    if game_on is True:
        game_status = "ON"
    elif game_on is False:
        game_status = "OFF"
    else:
        game_status = "UNKNOWN"

    print()
    print("=" * 50)
    print("realme Buds T200x Controller")
    print("=" * 50)
    print(f"Game Mode : {game_status}")
    print(f"Noise Mode: {anc_name}")
    print("-" * 50)

    print("1. Turn Game Mode ON")
    print("2. Turn Game Mode OFF")
    print("3. Transparency")
    print("4. Noise Cancelling")
    print("5. Normal Mode")
    print("6. ANC Alternate")
    print("0. Exit")

    print("-" * 50)


# ============================================================
# MAIN
# ============================================================

def main() -> None:

    print("Connecting to realme Buds T200x...")
    print(f"RFCOMM serial port: {PORT}")

    try:
        ser = serial.Serial(
            port=PORT,
            baudrate=BAUD,
            timeout=0.25,
            write_timeout=2,
        )

    except serial.SerialException as exc:
        print()
        print(f"ERROR: Could not open {PORT}")
        print(exc)
        input("\nPress Enter to exit...")
        return

    print("Connected.")

    # Clean old startup packets.
    drain_startup(ser)

    # Transfer ID is managed by the protocol.
    transfer_id = 0

    # These are UI-side states.
    game_on: Optional[bool] = None
    anc_name = "Unknown"

    try:

        while True:

            print_menu(
                game_on=game_on,
                anc_name=anc_name,
            )

            choice = input("> ").strip()

            # ------------------------------------------------
            # EXIT
            # ------------------------------------------------

            if choice == "0":
                break

            # ------------------------------------------------
            # GAME MODE ON
            # ------------------------------------------------

            if choice == "1":

                print("\nTurning Game Mode ON...")

                response = set_game_mode(
                    ser=ser,
                    enabled=True,
                    transfer_id=transfer_id,
                )

                transfer_id = (transfer_id + 1) & 0xFF

                if response:
                    game_on = True
                    print("Game Mode command sent.")
                else:
                    print("No response received.")
                    print("Game Mode status not confirmed.")

                continue

            # ------------------------------------------------
            # GAME MODE OFF
            # ------------------------------------------------

            if choice == "2":

                print("\nTurning Game Mode OFF...")

                response = set_game_mode(
                    ser=ser,
                    enabled=False,
                    transfer_id=transfer_id,
                )

                transfer_id = (transfer_id + 1) & 0xFF

                if response:
                    game_on = False
                    print("Game Mode command sent.")
                else:
                    print("No response received.")
                    print("Game Mode status not confirmed.")

                continue

            # ------------------------------------------------
            # ANC / NOISE CONTROL
            # ------------------------------------------------

            if choice in ANC_VALUES:

                name, value = ANC_VALUES[choice]

                print(f"\nSetting {name}...")

                response = set_anc(
                    ser=ser,
                    value=value,
                    transfer_id=transfer_id,
                )

                transfer_id = (transfer_id + 1) & 0xFF

                if response:
                    anc_name = name
                    print(f"{name} command sent.")
                else:
                    print("No response received.")

                continue

            # ------------------------------------------------
            # INVALID
            # ------------------------------------------------

            print("\nInvalid choice.")

    except KeyboardInterrupt:
        print("\n\nInterrupted.")

    finally:
        try:
            ser.close()
        except Exception:
            pass

        print("\nBluetooth controller closed.")


if __name__ == "__main__":
    main()