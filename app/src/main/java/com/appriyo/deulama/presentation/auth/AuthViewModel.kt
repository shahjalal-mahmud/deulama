package com.appriyo.deulama.presentation.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.domain.model.User
import com.appriyo.deulama.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * High-level auth state for the navigation root. Used to decide
 * startDestination on cold start.
 */
sealed interface AuthUiState {
    data object Loading : AuthUiState
    data object SignedOut : AuthUiState
    data class SignedIn(val user: User) : AuthUiState
}

/** Field names the API returns 422 errors under. Must match api.md exactly. */
private object ApiFields {
    const val EMAIL = "email"
    const val PASSWORD = "password"
    const val FULL_NAME = "full_name"
    const val PASSWORD_CONFIRMATION = "password_confirmation"
}

private const val NETWORK_BANNER = "Can't reach the server. Check your connection."

class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _loginForm = MutableStateFlow(LoginFormState())
    val loginForm: StateFlow<LoginFormState> = _loginForm.asStateFlow()

    private val _registerForm = MutableStateFlow(RegisterFormState())
    val registerForm: StateFlow<RegisterFormState> = _registerForm.asStateFlow()

    init {
        // The AuthRepository's sessionFlow is the single source of truth;
        // SessionManager.prime() (called from Application.onCreate) makes
        // the first emission arrive essentially immediately on cold start.
        viewModelScope.launch {
            authRepository.sessionFlow
                .map { session -> if (session == null) AuthUiState.SignedOut else AuthUiState.SignedIn(session.user) }
                .distinctUntilChanged()
                .collectLatest { _state.value = it }
        }
    }

    // ---- Login ----

    fun onLoginEmailChanged(v: String) =
        _loginForm.update { it.copy(email = v, emailError = null) }

    fun onLoginPasswordChanged(v: String) =
        _loginForm.update { it.copy(password = v, passwordError = null) }

    fun login(onSuccess: () -> Unit) {
        val current = _loginForm.value
        val emailErr = validateLoginEmail(current.email)
        val passErr = validateLoginPassword(current.password)
        if (emailErr != null || passErr != null) {
            _loginForm.update {
                it.copy(emailError = emailErr, passwordError = passErr)
            }
            return
        }
        _loginForm.update { it.copy(isSubmitting = true, banner = null) }
        viewModelScope.launch {
            when (val res = authRepository.login(current.email.trim(), current.password)) {
                is ApiResult.Success -> {
                    _loginForm.value = LoginFormState()
                    onSuccess()
                }
                is ApiResult.ValidationError -> {
                    val byField = res.fieldErrors
                    _loginForm.update {
                        it.copy(
                            isSubmitting = false,
                            emailError = byField[ApiFields.EMAIL]?.firstOrNull(),
                            passwordError = byField[ApiFields.PASSWORD]?.firstOrNull(),
                            banner = firstFieldErrorExcluding(
                                byField,
                                setOf(ApiFields.EMAIL, ApiFields.PASSWORD),
                            ),
                        )
                    }
                }
                is ApiResult.Error -> _loginForm.update { it.copy(isSubmitting = false, banner = res.message) }
                is ApiResult.NetworkError -> _loginForm.update { it.copy(isSubmitting = false, banner = NETWORK_BANNER) }
            }
        }
    }

    // ---- Register ----

    fun onRegisterNameChanged(v: String) =
        _registerForm.update { it.copy(fullName = v, fullNameError = null) }

    fun onRegisterEmailChanged(v: String) =
        _registerForm.update { it.copy(email = v, emailError = null) }

    fun onRegisterPasswordChanged(v: String) =
        _registerForm.update { it.copy(password = v, passwordError = null) }

    fun onRegisterConfirmationChanged(v: String) =
        _registerForm.update { it.copy(confirmation = v, confirmationError = null) }

    fun register(onSuccess: () -> Unit) {
        val current = _registerForm.value
        val nameErr = validateRegisterName(current.fullName)
        val emailErr = validateRegisterEmail(current.email)
        val passErr = validateRegisterPassword(current.password)
        val confirmErr = validateRegisterConfirmation(current.password, current.confirmation)
        if (listOf(nameErr, emailErr, passErr, confirmErr).any { it != null }) {
            _registerForm.update {
                it.copy(
                    fullNameError = nameErr,
                    emailError = emailErr,
                    passwordError = passErr,
                    confirmationError = confirmErr,
                )
            }
            return
        }
        _registerForm.update { it.copy(isSubmitting = true, banner = null) }
        viewModelScope.launch {
            when (val res = authRepository.register(
                fullName = current.fullName.trim(),
                email = current.email.trim(),
                password = current.password,
                passwordConfirmation = current.confirmation,
            )) {
                is ApiResult.Success -> {
                    _registerForm.value = RegisterFormState()
                    onSuccess()
                }
                is ApiResult.ValidationError -> {
                    val byField = res.fieldErrors
                    _registerForm.update {
                        it.copy(
                            isSubmitting = false,
                            fullNameError = byField[ApiFields.FULL_NAME]?.firstOrNull(),
                            emailError = byField[ApiFields.EMAIL]?.firstOrNull(),
                            passwordError = byField[ApiFields.PASSWORD]?.firstOrNull(),
                            confirmationError = byField[ApiFields.PASSWORD_CONFIRMATION]?.firstOrNull(),
                            banner = firstFieldErrorExcluding(
                                byField,
                                setOf(
                                    ApiFields.FULL_NAME,
                                    ApiFields.EMAIL,
                                    ApiFields.PASSWORD,
                                    ApiFields.PASSWORD_CONFIRMATION,
                                ),
                            ),
                        )
                    }
                }
                is ApiResult.Error -> {
                    // 409 means email is already registered. If the server
                    // didn't put the message under "email" already, mirror
                    // it there so it lines up with the field.
                    val emailTaken = res.httpStatus == 409
                    _registerForm.update {
                        it.copy(
                            isSubmitting = false,
                            emailError = if (emailTaken) (it.emailError ?: res.message) else it.emailError,
                            banner = if (emailTaken) null else res.message,
                        )
                    }
                }
                is ApiResult.NetworkError -> _registerForm.update { it.copy(isSubmitting = false, banner = NETWORK_BANNER) }
            }
        }
    }

    // ---- Logout (driven from nav graph on explicit logout or 401) ----

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}

// ---- Form state shapes ----

data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isSubmitting: Boolean = false,
    val banner: String? = null,
)

data class RegisterFormState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmation: String = "",
    val fullNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmationError: String? = null,
    val isSubmitting: Boolean = false,
    val banner: String? = null,
)

// ---- Validation rules — mirror api.md exactly ----

private fun validateLoginPassword(pw: String): String? = when {
    pw.isEmpty() -> "Enter your password."
    pw.length > 255 -> "Password is too long."
    else -> null
}

private fun validateLoginEmail(email: String): String? = when {
    email.isBlank() -> "Enter your email."
    !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Enter a valid email address."
    else -> null
}

private fun validateRegisterName(name: String): String? = when {
    name.trim().length < 2 -> "Name must be at least 2 characters."
    name.trim().length > 150 -> "Name must be 150 characters or fewer."
    else -> null
}

private fun validateRegisterEmail(email: String): String? = when {
    email.isBlank() -> "Enter your email."
    email.length > 191 -> "Email must be 191 characters or fewer."
    !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Enter a valid email address."
    else -> null
}

private fun validateRegisterPassword(pw: String): String? = when {
    pw.length < 8 -> "Password must be at least 8 characters."
    pw.length > 255 -> "Password must be 255 characters or fewer."
    else -> null
}

private fun validateRegisterConfirmation(pw: String, confirm: String): String? = when {
    confirm != pw -> "Passwords don't match."
    else -> null
}

private fun firstFieldErrorExcluding(
    byField: Map<String, List<String>>,
    exclude: Set<String>,
): String? {
    for ((k, v) in byField) {
        if (k in exclude) continue
        val first = v.firstOrNull() ?: continue
        if (first.isNotBlank()) return first
    }
    return null
}