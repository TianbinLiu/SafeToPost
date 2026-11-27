package app.safetopost.ime

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import app.safetopost.MainActivity
import app.safetopost.R
import app.safetopost.caption.CaptionCleaner
import app.safetopost.caption.CaptionScanner
import app.safetopost.model.CaptionFinding
import app.safetopost.model.CaptionKind

/**
 * Minimal keyboard with a caption-risk strip.
 *
 * It only reads the focused text field (never images), runs the same
 * CaptionScanner as the app, and offers to hand the caption back to
 * Safe to Post for the photo check.
 */
class SafeToPostImeService : InputMethodService() {

    private val scanner = CaptionScanner()
    private lateinit var warning: TextView
    private lateinit var warningBar: LinearLayout
    private lateinit var cleanButton: TextView
    private lateinit var keys: LinearLayout
    private var shifted = false

    override fun onCreateInputView(): View {
        val root = layoutInflater.inflate(R.layout.ime_keyboard, null)
        warning = root.findViewById(R.id.ime_warning)
        warningBar = root.findViewById(R.id.ime_warning_bar)
        cleanButton = root.findViewById(R.id.ime_clean)
        keys = root.findViewById(R.id.ime_keys)
        warning.setOnClickListener { openReview() }
        cleanButton.setOnClickListener { cleanField() }
        buildKeyboard()
        updateWarning(emptyList())
        return root
    }

    override fun onEvaluateInputViewShown(): Boolean = true

    override fun onShowInputRequested(flags: Int, configChange: Boolean): Boolean = true

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        shifted = false
        refreshWarning()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        refreshWarning()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        updateWarning(emptyList())
    }

    // ------------------------------------------------------------- keyboard

    private fun buildKeyboard() {
        keys.removeAllViews()
        addRow(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"))
        addRow(listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"))
        addRow(listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"))
        addRow(listOf("z", "x", "c", "v", "b", "n", "m", "\u232b"))
        addBottomRow()
    }

    private fun addRow(labels: List<String>) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        labels.forEach { label ->
            val key = makeKey(label, 1f)
            when (label) {
                "\u232b" -> key.setOnClickListener { backspace() }
                else -> key.setOnClickListener { commit(if (shifted) label.uppercase() else label) }
            }
            row.addView(key)
        }
        keys.addView(row)
    }

    private fun addBottomRow() {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val shift = makeKey("\u21e7", 1.4f)
        shift.setOnClickListener {
            shifted = !shifted
            shift.alpha = if (shifted) 1f else 0.55f
            rebuildLetters()
        }
        row.addView(shift)

        val space = makeKey("space", 4f)
        space.setOnClickListener { commit(" ") }
        row.addView(space)

        val dot = makeKey(".", 1f)
        dot.setOnClickListener { commit(".") }
        row.addView(dot)

        val enter = makeKey("\u23ce", 1.6f)
        enter.setOnClickListener { enterKey() }
        row.addView(enter)

        keys.addView(row)
    }

    private fun rebuildLetters() {
        // Keep digits/space; rebuilding the whole view is cheap and predictable.
        buildKeyboard()
        refreshWarning()
    }

    private fun makeKey(label: String, weight: Float): TextView {
        return TextView(this).apply {
            text = label
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#14212B"))
            isClickable = true
            isFocusable = true
            setBackgroundResource(R.drawable.ime_key_bg)
            layoutParams = LinearLayout.LayoutParams(0, dp(46), weight).apply {
                setMargins(dp(3), dp(3), dp(3), dp(3))
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    // -------------------------------------------------------------- actions

    private fun commit(text: String) {
        val connection = currentInputConnection ?: return
        connection.commitText(text, 1)
        refreshWarning()
    }

    private fun backspace() {
        val connection = currentInputConnection ?: return
        val selected = connection.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            connection.commitText("", 1)
        } else {
            connection.deleteSurroundingText(1, 0)
        }
        refreshWarning()
    }

    private fun enterKey() {
        val info = currentInputEditorInfo
        val action = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        val multiline = info?.inputType?.and(InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            sendDefaultEditorAction(true)
        } else if (multiline) {
            commit("\n")
        } else {
            sendDefaultEditorAction(true)
        }
    }

    // -------------------------------------------------------------- warning

    private fun currentText(): String {
        val connection = currentInputConnection ?: return ""
        val before = connection.getTextBeforeCursor(500, 0)?.toString().orEmpty()
        val after = connection.getTextAfterCursor(200, 0)?.toString().orEmpty()
        return before + after
    }

    private fun refreshWarning() {
        val findings = scanner.scan(currentText())
        updateWarning(findings)
    }

    private fun updateWarning(findings: List<CaptionFinding>) {
        if (!::warning.isInitialized) return
        if (findings.isEmpty()) {
            warning.text = "Safe to Post \u00b7 caption looks clean"
            warning.setTextColor(Color.parseColor("#2F6B4F"))
            warningBar.background = rounded(Color.parseColor("#E7F0E5"))
            cleanButton.visibility = View.GONE
        } else {
            val kinds = findings.map { label(it.kind) }.distinct().joinToString(" + ")
            warning.text = "\u26a0 $kinds in this caption \u00b7 tap to review"
            warning.setTextColor(Color.parseColor("#B5532F"))
            warningBar.background = rounded(Color.parseColor("#F3DDD4"))
            cleanButton.visibility = View.VISIBLE
        }
    }

    private fun rounded(color: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(12).toFloat()
        }

    private fun label(kind: CaptionKind): String = when (kind) {
        CaptionKind.PHONE -> "phone"
        CaptionKind.EMAIL -> "e-mail"
        CaptionKind.ADDRESS -> "address"
        CaptionKind.DATE -> "date"
        CaptionKind.SCHOOL_OR_WORK -> "school/work"
        CaptionKind.HANDLE -> "handle"
        CaptionKind.LINK -> "link"
    }

    private fun cleanField() {
        val connection = currentInputConnection ?: return
        val before = connection.getTextBeforeCursor(5000, 0)?.toString().orEmpty()
        val after = connection.getTextAfterCursor(2000, 0)?.toString().orEmpty()
        val text = before + after
        val findings = scanner.scan(text)
        if (findings.isEmpty()) return
        val cleaned = CaptionCleaner.clean(text, findings)
        try {
            connection.beginBatchEdit()
            connection.finishComposingText()
            connection.deleteSurroundingText(before.length, after.length)
            connection.commitText(cleaned, 1)
            connection.endBatchEdit()
            refreshWarning()
        } catch (t: Throwable) {
            val clipboard = getSystemService(ClipboardManager::class.java)
            clipboard?.setPrimaryClip(ClipData.newPlainText("Cleaned caption", cleaned))
            Toast.makeText(this, "Cleaned caption copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openReview() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_CAPTION, currentText())
        }
        startActivity(intent)
    }
}
