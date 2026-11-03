package edu.sdwu.teaching.domain.model

data class User(
    val id: Int,
    val email: String,
    val displayName: String,
    val isInstructor: Boolean,
    /** Optional profile picture, loaded with Coil. Initials are shown when null. */
    val avatarUrl: String? = null,
)

/** Who is using the app right now. The root UI shows the login screen only for [SignedOut]. */
sealed interface SessionState {
    data object SignedOut : SessionState

    /** "Continue without account": everything works offline, progress stays on the device. */
    data object Guest : SessionState

    data class SignedIn(val user: User, val token: String) : SessionState
}
