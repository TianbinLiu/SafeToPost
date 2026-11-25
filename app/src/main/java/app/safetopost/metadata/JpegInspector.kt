package app.safetopost.metadata

enum class SegmentKind { SOI, APP0, APP1, APP2, COMMENT, DQT, SOF, SOS, EOI, OTHER }

data class JpegSegment(val kind: SegmentKind, val offset: Int, val length: Int)

/** Walks the JPEG marker structure without decoding pixels. */
object JpegInspector {

    fun inspect(bytes: ByteArray): List<JpegSegment> {
        val out = mutableListOf<JpegSegment>()
        if (bytes.size < 4) return out
        fun u8(pos: Int) = bytes[pos].toInt() and 0xFF
        if (u8(0) != 0xFF || u8(1) != 0xD8) return out
        out += JpegSegment(SegmentKind.SOI, 0, 2)

        var i = 2
        while (i + 1 < bytes.size) {
            if (u8(i) != 0xFF) { i++; continue }
            var marker = u8(i + 1)
            while (marker == 0xFF && i + 2 < bytes.size) {
                i++
                marker = u8(i + 1)
            }
            if (marker == 0xD9) {
                out += JpegSegment(SegmentKind.EOI, i, 2)
                break
            }
            if (marker in 0xD0..0xD8 || marker == 0x01) {
                i += 2
                continue
            }
            if (i + 3 >= bytes.size) break
            val length = (u8(i + 2) shl 8) or u8(i + 3)
            val kind = when (marker) {
                0xE0 -> SegmentKind.APP0
                0xE1 -> SegmentKind.APP1
                0xE2 -> SegmentKind.APP2
                0xDB -> SegmentKind.DQT
                0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7,
                0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF -> SegmentKind.SOF
                0xDA -> SegmentKind.SOS
                0xFE -> SegmentKind.COMMENT
                else -> SegmentKind.OTHER
            }
            out += JpegSegment(kind, i, 2 + length)
            if (kind == SegmentKind.SOS) break
            i += 2 + length
        }
        return out
    }
}
