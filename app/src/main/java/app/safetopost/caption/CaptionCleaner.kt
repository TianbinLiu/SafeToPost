package app.safetopost.caption

import app.safetopost.model.CaptionFinding
import app.safetopost.model.CaptionKind

/** Replaces detected personal details with readable placeholders. */
object CaptionCleaner {

    fun placeholder(kind: CaptionKind): String = when (kind) {
        CaptionKind.PHONE -> "[phone]"
        CaptionKind.EMAIL -> "[email]"
        CaptionKind.ADDRESS -> "[address]"
        CaptionKind.DATE -> "[date]"
        CaptionKind.SCHOOL_OR_WORK -> "[school/work]"
        CaptionKind.HANDLE -> "[handle]"
        CaptionKind.LINK -> "[link]"
    }

    fun clean(text: String, findings: List<CaptionFinding>): String {
        var result = text
        findings.sortedByDescending { it.range.first }.forEach { finding ->
            val start = finding.range.first.coerceIn(0, result.length)
            val end = (finding.range.last + 1).coerceIn(start, result.length)
            result = result.replaceRange(start, end, placeholder(finding.kind))
        }
        return result
    }
}
