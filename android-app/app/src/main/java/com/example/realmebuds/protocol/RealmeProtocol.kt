package com.example.realmebuds.protocol

import java.io.ByteArrayOutputStream

/**
 * Realme TL protocol & OPPOv1 packet framing.
 *
 * Every byte produced here must stay compatible with the Windows controller
 * (`t200x_core.py` in the repository root), which implements the same wire
 * protocol. Keep this file in sync with `encode_length()` / `wrap_oppo_v1()` /
 * `make_packet()` over there.
 *
 * ## OPPOv1 frame
 * ```
 *  offset        size  field
 *  0             1     0xAA                 frame marker
 *  1             1..N  varint7(frame_len)   frame_len = len(inner) + 2
 *  1 + N         2     0x00 0x00            reserved
 *  3 + N         M     inner (TL packet)    see below
 * ```
 *
 * ## Inner TL packet
 * ```
 *  offset        size  field
 *  0             2     command, little endian (e.g. 0x0403 -> 03 04)
 *  2             1     transfer id, 0..255, wraps with (x + 1) & 0xFF
 *  3             2     payload length, little endian
 *  5             P     payload
 * ```
 */
object RealmeProtocol {

    /** CMD_SWITCH — game mode toggle. Payload: `[0x06, state]`. */
    const val CMD_SWITCH = 0x0403 // Decimal 1027

    /** CMD_NOISE — ANC/transparency mode. Payload: `[0x01, 0x01, value]`. */
    const val CMD_NOISE = 0x0404  // Decimal 1028

    /**
     * Battery query/response command (category `0x06`, sub-command `0x01`).
     * Sent as the raw 9-byte frame from [makeBatteryQueryPacket]; its little
     * endian encoding (`06 01`) is what the battery parser scans for.
     */
    const val CMD_BATTERY = 0x0106

    /** Feature byte used by [CMD_SWITCH] to address game mode. */
    const val GAME_FEATURE = 0x06.toByte()

    /**
     * Noise control modes. The wire `value` is the third byte of the
     * `CMD_NOISE` payload (`0x08` ANC, `0x02` transparency, `0x01` normal).
     */
    enum class AncMode(
        val key: String,
        val displayName: String,
        val value: Byte,
        val icon: String,
        val description: String
    ) {
        NOISE_CANCELLING("noise_cancelling", "Noise Cancelling", 0x08.toByte(), "🔇", "Blocks ambient background noise"),
        TRANSPARENCY("transparency", "Transparency", 0x02.toByte(), "👂", "Passes external sound & voices through"),
        NORMAL("normal", "Normal Mode", 0x01.toByte(), "⚪", "Standard audio without active filtering"),
        ANC_ALTERNATE("anc_alternate", "ANC Alternate", 0x04.toByte(), "⚡", "Alternative noise cancellation curve")
    }

    /**
     * OPPOv1 7-bit variable-length integer encoding (little endian, most
     * significant group first, high bit = "more bytes follow").
     *
     * ```
     *   0    -> 00
     *   14   -> 0E
     *   127  -> 7F
     *   128  -> 80 01
     *   300  -> AC 02
     * ```
     *
     * Mirrors `encode_length()` in `t200x_core.py`.
     */
    fun encodeLength(value: Int): ByteArray {
        var v = value
        val out = ByteArrayOutputStream()
        while (true) {
            val chunk = v and 0x7F
            v = v ushr 7
            if (v != 0) {
                out.write(chunk or 0x80)
            } else {
                out.write(chunk)
                break
            }
        }
        return out.toByteArray()
    }

    /**
     * OPPOv1 single-frame wrapper:
     * `[0xAA] [varint7(len(payload) + 2)] [0x00, 0x00] [payload]`
     *
     * The encoded length counts the two reserved bytes plus the payload
     * (`frame_len = len(inner) + 2`), matching `wrap_oppo_v1()`.
     */
    fun wrapOppoV1(payload: ByteArray): ByteArray {
        val frameLength = payload.size + 2
        val encodedLen = encodeLength(frameLength)
        val out = ByteArrayOutputStream()
        out.write(0xAA)
        out.write(encodedLen)
        out.write(0x00)
        out.write(0x00)
        out.write(payload)
        return out.toByteArray()
    }

    /**
     * Constructs a Realme TL packet and wraps it in OPPOv1:
     * ```
     *  command       : 2 bytes Little Endian
     *  transfer ID   : 1 byte  (current id; caller increments after the write attempt)
     *  payload length: 2 bytes Little Endian
     *  payload       : N bytes
     * ```
     *
     * Mirrors `make_packet()` in `t200x_core.py`.
     */
    fun makePacket(command: Int, transferId: Int, payload: ByteArray): ByteArray {
        val inner = ByteArrayOutputStream()
        // Command (2 bytes LE)
        inner.write(command and 0xFF)
        inner.write((command ushr 8) and 0xFF)
        // Transfer ID (1 byte)
        inner.write(transferId and 0xFF)
        // Payload length (2 bytes LE)
        inner.write(payload.size and 0xFF)
        inner.write((payload.size ushr 8) and 0xFF)
        // Payload
        inner.write(payload)

        return wrapOppoV1(inner.toByteArray())
    }

    /**
     * CMD_SWITCH (0x0403) game mode packet.
     *
     * Payload is `[0x06, state]` where state is `0x01` on / `0x00` off, e.g.
     * `AA 09 00 00 03 04 <id> 02 00 06 01`.
     */
    fun makeGameModePacket(enabled: Boolean, transferId: Int): ByteArray {
        val state: Byte = if (enabled) 0x01.toByte() else 0x00.toByte()
        val payload = byteArrayOf(GAME_FEATURE, state)
        return makePacket(CMD_SWITCH, transferId, payload)
    }

    /**
     * CMD_NOISE (0x0404) noise-control packet.
     *
     * Payload is `[0x01, 0x01, value]` where value comes from [AncMode.value]
     * (`0x08` ANC, `0x02` transparency, `0x01` normal), e.g.
     * `AA 0A 00 00 04 04 <id> 03 00 01 01 08`.
     */
    fun makeAncPacket(ancValue: Byte, transferId: Int): ByteArray {
        val payload = byteArrayOf(0x01.toByte(), 0x01.toByte(), ancValue)
        return makePacket(CMD_NOISE, transferId, payload)
    }

    /**
     * Raw battery query frame (OPPOv1, category 0x06 / sub-command 0x01):
     * `AA 07 00 00 06 01 <seq> 00 00`.
     *
     * Byte-for-byte identical to `makePacket(CMD_BATTERY, transferId, ByteArray(0))`.
     * `[seq]` is the *current* transfer id (see the transfer_id rule in
     * `BluetoothController`), incremented after every write attempt.
     */
    fun makeBatteryQueryPacket(transferId: Int): ByteArray {
        val seq = (transferId and 0xFF).toByte()
        return byteArrayOf(
            0xAA.toByte(), 0x07.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x06.toByte(), 0x01.toByte(), seq, 0x00.toByte(), 0x00.toByte()
        )
    }

    /**
     * Parsed battery levels, percentages in `0..100`, `null` when absent or
     * rejected as implausible.
     */
    data class BatteryStatus(val left: Int?, val right: Int?, val case: Int?)

    /**
     * Scans a received frame for a battery report (cmd 0x0106):
     * ```
     *  i+0        0xAA
     *  i+4..i+5   command 0x0106 (LE: 06 01) -> data[i+4] == 0x06, data[i+5] & 0x7F == 0x01
     *  i+7..i+8   payload length (LE)
     *  i+9        payload[0] (reserved)
     *  i+10       device count
     *  i+11..     count * (device id, percent): 0x01 left, 0x02 right, 0x03 case
     * ```
     *
     * Mirrors `_parse_battery_packet()` in `t200x_core.py`, plus defensive
     * handling: counts are read unsigned and percentages outside `0..100` are
     * rejected instead of being surfaced to the UI.
     *
     * @return the first plausible battery report, or `null` for malformed /
     * short / unrelated data.
     */
    fun parseBatteryResponse(data: ByteArray): BatteryStatus? {
        for (i in 0 until data.size - 8) {
            if (data[i] == 0xAA.toByte() && data[i + 4] == 0x06.toByte() && (data[i + 5].toInt() and 0x7F) == 0x01) {
                if (i + 10 >= data.size) break
                // Read the count unsigned (a stray high bit must not go negative)
                // and cap it to the plausible number of (id, pct) pairs.
                val count = (data[i + 10].toInt() and 0xFF).coerceAtMost(8)
                var left: Int? = null
                var right: Int? = null
                var case: Int? = null
                var pos = i + 11
                for (d in 0 until count) {
                    if (pos + 1 >= data.size) break
                    val devId = data[pos].toInt() and 0xFF
                    val pct = data[pos + 1].toInt() and 0xFF
                    pos += 2
                    // Reject nonsense percentages (> 100) instead of showing them.
                    if (pct > 100) continue
                    when (devId) {
                        0x01 -> left = pct
                        0x02 -> right = pct
                        0x03 -> case = pct
                    }
                }
                if (left != null || right != null || case != null) {
                    return BatteryStatus(left, right, case)
                }
            }
        }
        return null
    }

    fun ByteArray.toHexString(): String {
        return joinToString(" ") { String.format("%02X", it) }
    }
}
