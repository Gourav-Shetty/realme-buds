package com.example.realmebuds.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure-JVM tests for [RealmeProtocol] — no Android framework involved, so they
 * run under `gradlew test`.
 *
 * Every expectation is hand-derived from the wire layout documented in
 * [RealmeProtocol] and cross-checked against `t200x_core.py`
 * (`encode_length` / `wrap_oppo_v1` / `make_packet` / `_parse_battery_packet`).
 */
class RealmeProtocolTest {

    // ---------------------------------------------------------------- encodeLength

    @Test
    fun encodeLength_encodesSingleByteValues() {
        assertArrayEquals(byteArrayOf(0x00), RealmeProtocol.encodeLength(0))
        assertArrayEquals(byteArrayOf(0x07), RealmeProtocol.encodeLength(7))
        assertArrayEquals(byteArrayOf(0x0E), RealmeProtocol.encodeLength(14))
        assertArrayEquals(byteArrayOf(0x7F), RealmeProtocol.encodeLength(127))
    }

    @Test
    fun encodeLength_encodesMultiByteValues() {
        // 128 = 0x00 | 0x80 continuation, then group 1
        assertArrayEquals(byteArrayOf(0x80.toByte(), 0x01), RealmeProtocol.encodeLength(128))
        // 300 = 44 + 2 * 128 -> 44 | 0x80 continuation, then group 2
        assertArrayEquals(byteArrayOf(0xAC.toByte(), 0x02), RealmeProtocol.encodeLength(300))
        // 16384 = 128 * 128 -> three groups
        assertArrayEquals(
            byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x01),
            RealmeProtocol.encodeLength(16384)
        )
    }

    // ------------------------------------------------------------------ wrapOppoV1

    @Test
    fun wrapOppoV1_framesPayloadWithLengthPlusTwo() {
        val payload = byteArrayOf(0x01, 0x02, 0x03)
        // frame_len = len(inner) + 2 = 5
        assertArrayEquals(
            byteArrayOf(0xAA.toByte(), 0x05, 0x00, 0x00, 0x01, 0x02, 0x03),
            RealmeProtocol.wrapOppoV1(payload)
        )
    }

    @Test
    fun wrapOppoV1_usesMultiByteLengthForLargeFrames() {
        val payload = ByteArray(126) { it.toByte() }
        // frame_len = 126 + 2 = 128 -> varint 80 01
        val frame = RealmeProtocol.wrapOppoV1(payload)
        assertArrayEquals(
            byteArrayOf(0xAA.toByte(), 0x80.toByte(), 0x01, 0x00, 0x00),
            frame.copyOfRange(0, 5)
        )
        assertArrayEquals(payload, frame.copyOfRange(5, frame.size))
    }

    // ------------------------------------------------------------------ makePacket

    @Test
    fun makePacket_layoutIsLittleEndian() {
        val payload = byteArrayOf(0x06, 0x01)
        val packet = RealmeProtocol.makePacket(0x0403, 0x05, payload)

        // inner = cmd(03 04) | id(05) | payload_len(02 00) | payload = 7 bytes
        // frame_len = 7 + 2 = 9
        assertArrayEquals(
            byteArrayOf(
                0xAA.toByte(), 0x09, 0x00, 0x00, // OPPOv1 header
                0x03, 0x04,                      // command 0x0403, little endian
                0x05,                            // transfer id
                0x02, 0x00,                      // payload length 2, little endian
                0x06, 0x01                       // payload
            ),
            packet
        )
    }

    @Test
    fun makePacket_encodesMultiBytePayloadLengthLittleEndian() {
        val payload = ByteArray(300)
        val packet = RealmeProtocol.makePacket(0x0403, 0x01, payload)

        // frame_len = 5 (TL header) + 300 + 2 = 307 -> varint B3 02
        assertArrayEquals(
            byteArrayOf(0xAA.toByte(), 0xB3.toByte(), 0x02, 0x00, 0x00),
            packet.copyOfRange(0, 5)
        )
        // payload length 300 = 0x012C, little endian, right after cmd + id
        assertEquals(0x2C, packet[8].toInt() and 0xFF)
        assertEquals(0x01, packet[9].toInt() and 0xFF)
        assertEquals(300, packet.size - 10)
    }

    // --------------------------------------------------- game mode / ANC payloads

    @Test
    fun makeGameModePacket_onUsesFeatureAndOne() {
        val packet = RealmeProtocol.makeGameModePacket(enabled = true, transferId = 0x07)
        assertArrayEquals(
            byteArrayOf(
                0xAA.toByte(), 0x09, 0x00, 0x00,
                0x03, 0x04,       // CMD_SWITCH 0x0403, little endian
                0x07,             // transfer id
                0x02, 0x00,       // payload length = 2
                0x06, 0x01        // payload [GAME_FEATURE, 0x01]
            ),
            packet
        )
    }

    @Test
    fun makeGameModePacket_offUsesZeroState() {
        val packet = RealmeProtocol.makeGameModePacket(enabled = false, transferId = 0x00)
        assertArrayEquals(
            byteArrayOf(
                0xAA.toByte(), 0x09, 0x00, 0x00,
                0x03, 0x04,
                0x00,
                0x02, 0x00,
                0x06, 0x00        // payload [GAME_FEATURE, 0x00]
            ),
            packet
        )
    }

    @Test
    fun makeAncPacket_usesThreeByteNoisePayload() {
        val packet = RealmeProtocol.makeAncPacket(
            RealmeProtocol.AncMode.NOISE_CANCELLING.value,
            transferId = 0x02
        )
        // inner = cmd(04 04) | id(02) | len(03 00) | payload(01 01 08) = 8 bytes
        // frame_len = 8 + 2 = 10
        assertArrayEquals(
            byteArrayOf(
                0xAA.toByte(), 0x0A, 0x00, 0x00,
                0x04, 0x04,       // CMD_NOISE 0x0404, little endian
                0x02,             // transfer id
                0x03, 0x00,       // payload length = 3
                0x01, 0x01, 0x08  // payload [0x01, 0x01, ANC value]
            ),
            packet
        )
    }

    @Test
    fun ancModeValues_matchWireContract() {
        assertEquals(0x08.toByte(), RealmeProtocol.AncMode.NOISE_CANCELLING.value)
        assertEquals(0x02.toByte(), RealmeProtocol.AncMode.TRANSPARENCY.value)
        assertEquals(0x01.toByte(), RealmeProtocol.AncMode.NORMAL.value)
    }

    // -------------------------------------------------------------- battery query

    @Test
    fun makeBatteryQueryPacket_isRawNineByteFrame() {
        assertArrayEquals(
            byteArrayOf(0xAA.toByte(), 0x07, 0x00, 0x00, 0x06, 0x01, 0x2A, 0x00, 0x00),
            RealmeProtocol.makeBatteryQueryPacket(0x2A)
        )
    }

    @Test
    fun makeBatteryQueryPacket_matchesGenericFraming() {
        // Same bytes the generic TL framer would emit for cmd 0x0106, no payload.
        assertArrayEquals(
            RealmeProtocol.makePacket(RealmeProtocol.CMD_BATTERY, 0x2A, ByteArray(0)),
            RealmeProtocol.makeBatteryQueryPacket(0x2A)
        )
    }

    // ----------------------------------------------------------- battery parsing

    /** Hand-built valid battery report: L 90%, R 100%, case 50%. */
    private fun validBatteryFrame(): ByteArray = byteArrayOf(
        0xAA.toByte(), 0x0E, 0x00, 0x00, // OPPOv1 header, frame_len = 14
        0x06, 0x01,                      // cmd 0x0106, little endian
        0x05,                            // transfer id
        0x08, 0x00,                      // payload length = 8
        0x00,                            // payload[0] (reserved)
        0x03,                            // device count
        0x01, 90,                        // left   90%
        0x02, 100,                       // right 100%
        0x03, 50                         // case   50%
    )

    @Test
    fun parseBatteryResponse_readsHandBuiltFrame() {
        val status = RealmeProtocol.parseBatteryResponse(validBatteryFrame())
        assertNotNull(status)
        assertEquals(RealmeProtocol.BatteryStatus(90, 100, 50), status)
    }

    @Test
    fun parseBatteryResponse_scansPastLeadingNoise() {
        val noisy = byteArrayOf(0x11, 0x22, 0x33) + validBatteryFrame()
        assertEquals(
            RealmeProtocol.BatteryStatus(90, 100, 50),
            RealmeProtocol.parseBatteryResponse(noisy)
        )
    }

    @Test
    fun parseBatteryResponse_rejectsNonsensePercentages() {
        val frame = byteArrayOf(
            0xAA.toByte(), 0x0E, 0x00, 0x00,
            0x06, 0x01,
            0x05,
            0x08, 0x00,
            0x00,
            0x03,
            0x01, 0xF4.toByte(), // left 244% -> rejected
            0x02, 60,            // right 60%
            0x03, 55             // case 55%
        )
        assertEquals(
            RealmeProtocol.BatteryStatus(null, 60, 55),
            RealmeProtocol.parseBatteryResponse(frame)
        )
    }

    @Test
    fun parseBatteryResponse_returnsNullForEmptyInput() {
        assertNull(RealmeProtocol.parseBatteryResponse(ByteArray(0)))
    }

    @Test
    fun parseBatteryResponse_returnsNullForShortInput() {
        assertNull(RealmeProtocol.parseBatteryResponse(byteArrayOf(0xAA.toByte(), 0x07, 0x00)))
        // Truncated right before the count byte.
        assertNull(
            RealmeProtocol.parseBatteryResponse(
                byteArrayOf(0xAA.toByte(), 0x07, 0x00, 0x00, 0x06, 0x01, 0x00, 0x00, 0x00)
            )
        )
    }

    @Test
    fun parseBatteryResponse_returnsNullForWrongCommand() {
        val frame = byteArrayOf(
            0xAA.toByte(), 0x0E, 0x00, 0x00,
            0x04, 0x04,  // CMD_NOISE, not the battery command
            0x05,
            0x08, 0x00,
            0x00,
            0x03,
            0x01, 90,
            0x02, 100,
            0x03, 50
        )
        assertNull(RealmeProtocol.parseBatteryResponse(frame))
    }

    @Test
    fun parseBatteryResponse_returnsNullForGarbage() {
        assertNull(RealmeProtocol.parseBatteryResponse(byteArrayOf(0x01, 0x02, 0x03, 0x04)))
        // Correct header but the pair list is missing entirely.
        assertNull(
            RealmeProtocol.parseBatteryResponse(
                byteArrayOf(
                    0xAA.toByte(), 0x07, 0x00, 0x00,
                    0x06, 0x01, 0x05, 0x03, 0x00, 0x00
                )
            )
        )
    }
}
