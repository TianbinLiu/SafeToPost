package app.safetopost.caption

import app.safetopost.model.CaptionFinding
import app.safetopost.model.CaptionKind

/** Rule-based caption scan. The rule list is deliberately small and readable. */
class CaptionScanner {

    private data class Rule(val kind: CaptionKind, val regex: Regex)

    private val rules = listOf(
        Rule(
            CaptionKind.PHONE,
            Regex("""(?:\+?1[\s.-]?)?\(?\d{3}\)?[\s.-]?\d{3}[\s.-]?\d{4}""")
        ),
        Rule(
            CaptionKind.EMAIL,
            Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
        ),
        Rule(
            CaptionKind.ADDRESS,
            Regex(
                """\b\d{1,5}\s+[A-Za-z0-9.'-]+(?:\s+[A-Za-z0-9.'-]+){0,4}\s+(?:Street|St|Avenue|Ave|Road|Rd|Drive|Dr|Boulevard|Blvd|Lane|Ln|Way|Court|Ct)\b""",
                RegexOption.IGNORE_CASE
            )
        ),
        Rule(
            CaptionKind.DATE,
            Regex("""\b(?:0?[1-9]|1[0-2])[/-](?:0?[1-9]|[12]\d|3[01])(?:[/-]\d{2,4})?\b""")
        ),
        Rule(
            CaptionKind.SCHOOL_OR_WORK,
            Regex(
                """\b(?:at|from)\s+(?:[A-Z][A-Za-z.&'-]+\s+){1,3}(?:University|College|School|High|Inc|Corp|LLC)\b"""
            )
        ),
        Rule(CaptionKind.HANDLE, Regex("""(?<![\w@])@[A-Za-z0-9_.]{2,30}""")),
        Rule(CaptionKind.LINK, Regex("""https?://\S+"""))
    )

    fun scan(text: String): List<CaptionFinding> {
        if (text.isBlank()) return emptyList()
        val found = mutableListOf<CaptionFinding>()
        for (rule in rules) {
            for (match in rule.regex.findAll(text)) {
                val overlaps = found.any { existing ->
                    match.range.first <= existing.range.last && existing.range.first <= match.range.last
                }
                if (!overlaps) {
                    found += CaptionFinding(rule.kind, match.range, match.value)
                }
            }
        }
        return found.sortedBy { it.range.first }
    }
}
