package app.safetopost.metadata

/** Minimal TIFF/EXIF reader for GPS tags, used when the platform reader returns zeros. */
object ExifGpsParser {

    data class Gps(val latitude: String, val longitude: String)

    private data class Entry(
        val type: Int,
        val count: Long,
        val valueOrOffset: Long,
        val entryOffset: Int
    )

    fun parse(bytes: ByteArray, segments: List<JpegSegment>): Gps? {
        val segment = segments.firstOrNull { it.kind == SegmentKind.APP1 } ?: return null
        val payload = segment.offset + 4
        if (payload + 6 > bytes.size) return null
        if (bytes[payload] != 'E'.code.toByte() || bytes[payload + 1] != 'x'.code.toByte()) return null
        val tiff = payload + 6
        if (tiff + 8 > bytes.size) return null

        val little = when (bytes[tiff].toInt() and 0xFF) {
            0x49 -> true
            0x4D -> false
            else -> return null
        }

        fun u16(pos: Int): Int {
            if (pos + 2 > bytes.size) return -1
            val a = bytes[pos].toInt() and 0xFF
            val b = bytes[pos + 1].toInt() and 0xFF
            return if (little) a or (b shl 8) else (a shl 8) or b
        }

        fun u32(buffer: ByteArray, pos: Int): Long {
            if (pos + 4 > buffer.size) return -1
            var value = 0L
            for (index in 0 until 4) {
                val part = (buffer[pos + index].toInt() and 0xFF).toLong()
                value = if (little) value or (part shl (8 * index)) else (value shl 8) or part
            }
            return value
        }

        fun entries(ifdOffset: Int): Map<Int, Entry> {
            val count = u16(ifdOffset)
            if (count <= 0 || count > 512) return emptyMap()
            val out = HashMap<Int, Entry>()
            for (index in 0 until count) {
                val entryOffset = ifdOffset + 2 + index * 12
                if (entryOffset + 12 > bytes.size) break
                out[u16(entryOffset)] = Entry(
                    type = u16(entryOffset + 2),
                    count = u32(bytes, entryOffset + 4),
                    valueOrOffset = u32(bytes, entryOffset + 8),
                    entryOffset = entryOffset
                )
            }
            return out
        }

        fun valueBytes(entry: Entry): ByteArray? {
            val unit = when (entry.type) {
                1, 2, 6, 7 -> 1
                3, 8 -> 2
                4, 9 -> 4
                5, 10 -> 8
                else -> return null
            }
            val total = entry.count * unit
            if (entry.count <= 0 || total > 1024) return null
            val source = if (total <= 4) entry.entryOffset + 8 else tiff + entry.valueOrOffset.toInt()
            if (source < 0 || source + total > bytes.size) return null
            return bytes.copyOfRange(source, (source + total).toInt())
        }

        fun ascii(entry: Entry): String? =
            valueBytes(entry)?.toString(Charsets.UTF_8)?.trim('\u0000', ' ', '\n')

        fun rationals(entry: Entry): String? {
            val data = valueBytes(entry) ?: return null
            if (data.size < entry.count * 8) return null
            val parts = (0 until entry.count.toInt()).map { index ->
                "${u32(data, index * 8)}/${u32(data, index * 8 + 4)}"
            }
            return parts.joinToString(",")
        }

        val ifd0Offset = u32(bytes, tiff + 4).toInt()
        val ifd0 = entries(tiff + ifd0Offset)
        val gpsPointer = ifd0[0x8825] ?: return null
        val gps = entries(tiff + gpsPointer.valueOrOffset.toInt())

        val latRaw = gps[0x0002]?.let { rationals(it) } ?: return null
        val lonRaw = gps[0x0004]?.let { rationals(it) } ?: return null
        val latRef = gps[0x0001]?.let { ascii(it) }
        val lonRef = gps[0x0003]?.let { ascii(it) }

        val lat = CoordinateFormat.dms(latRaw, latRef) ?: return null
        val lon = CoordinateFormat.dms(lonRaw, lonRef) ?: return null
        return Gps(lat, lon)
    }
}
