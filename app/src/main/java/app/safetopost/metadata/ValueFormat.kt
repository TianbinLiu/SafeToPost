package app.safetopost.metadata

object ValueFormat {
    fun short(value: String, max: Int = 60): String =
        if (value.length <= max) value else value.take(max - 1) + "\u2026"
}
