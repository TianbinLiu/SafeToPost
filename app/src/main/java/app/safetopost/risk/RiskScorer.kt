package app.safetopost.risk

import app.safetopost.metadata.ValueFormat
import app.safetopost.model.CaptionFinding
import app.safetopost.model.CaptionKind
import app.safetopost.model.PhotoMetadata
import app.safetopost.model.Risk
import app.safetopost.model.RiskFactor
import app.safetopost.model.RiskLevel
import app.safetopost.model.RiskScore
import app.safetopost.model.RiskSource

/** Combines metadata entries and caption findings into one explained score. */
object RiskScorer {

    fun captionWeight(kind: CaptionKind): Int = when (kind) {
        CaptionKind.PHONE -> 25
        CaptionKind.ADDRESS -> 25
        CaptionKind.EMAIL -> 15
        CaptionKind.SCHOOL_OR_WORK -> 10
        CaptionKind.DATE -> 6
        CaptionKind.HANDLE -> 6
        CaptionKind.LINK -> 4
    }

    fun score(metadata: PhotoMetadata, findings: List<CaptionFinding>): RiskScore {
        val factors = mutableListOf<RiskFactor>()

        for (entry in metadata.entries) {
            val weight = when (entry.risk) {
                Risk.HIGH -> 25
                Risk.MEDIUM -> 10
                Risk.LOW -> 3
            }
            factors += RiskFactor(
                source = RiskSource.METADATA,
                weight = weight,
                explanation = "${entry.label}: ${ValueFormat.short(entry.value, 42)}"
            )
        }

        for (finding in findings) {
            val weight = captionWeight(finding.kind)
            factors += RiskFactor(
                source = RiskSource.CAPTION,
                weight = weight,
                explanation = finding.kind.name.lowercase().replace('_', ' ') + ": " +
                    ValueFormat.short(finding.matched, 32)
            )
        }

        val total = factors.sumOf { it.weight }.coerceAtMost(100)
        val level = when {
            total >= 65 -> RiskLevel.HIGH
            total >= 30 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
        return RiskScore(total, level, factors)
    }
}
