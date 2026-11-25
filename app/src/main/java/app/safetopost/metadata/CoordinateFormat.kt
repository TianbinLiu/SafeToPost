package app.safetopost.metadata

/** Formats EXIF GPS rationals ("34/1,118/1,1234/100") as degrees/minutes/seconds. */
object CoordinateFormat {

    fun dms(rational: String?, ref: String?): String? {
        if (rational.isNullOrBlank()) return null
        val parts = rational.split(",").map { it.trim() }
        if (parts.size < 2) return rational

        fun toDouble(value: String): Double? {
            val pair = value.split("/")
            return if (pair.size == 2) {
                val a = pair[0].toDoubleOrNull() ?: return null
                val b = pair[1].toDoubleOrNull() ?: return null
                if (b == 0.0) null else a / b
            } else value.toDoubleOrNull()
        }

        val degrees = toDouble(parts[0]) ?: return rational
        val minutes = toDouble(parts[1]) ?: 0.0
        val seconds = if (parts.size > 2) toDouble(parts[2]) ?: 0.0 else 0.0
        val suffix = ref?.trim().orEmpty()
        return "%d\u00b0 %02d' %05.2f\" %s".format(degrees.toInt(), minutes.toInt(), seconds, suffix).trim()
    }
}
