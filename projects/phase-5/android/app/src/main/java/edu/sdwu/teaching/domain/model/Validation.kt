package edu.sdwu.teaching.domain.model

/**
 * Client-side checks for instant feedback. The backend validates again.
 * Plain Kotlin (not android.util.Patterns) so ViewModel unit tests run on the JVM.
 */
object Validation {
    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun isValidEmail(email: String): Boolean = EMAIL.matches(email.trim())

    /** Mirrors the backend rule: 6-72 characters with at least one letter and one digit. */
    fun passwordProblem(password: String): String? = when {
        password.length < 6 -> "Password must have at least 6 characters"
        password.length > 72 -> "Password must have at most 72 characters"
        password.none { it.isLetter() } || password.none { it.isDigit() } -> "Use at least one letter and one digit"
        else -> null
    }

    fun isValidHttpUrl(url: String): Boolean =
        Regex("^https?://[^\\s/]+(/\\S*)?$").matches(url.trim())
}
