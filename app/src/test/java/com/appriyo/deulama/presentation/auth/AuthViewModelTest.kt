package com.appriyo.deulama.presentation.auth

import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.domain.model.Session
import com.appriyo.deulama.domain.model.User
import com.appriyo.deulama.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Backs `presentation/auth/AuthViewModel.kt`, specifically the
 * "wrong credentials" branch of [AuthViewModel.login].
 *
 * Per API.md, the server returns an HTTP 401 with envelope message
 * "Invalid email or password." for both "unknown email" and "wrong
 * password" — no enumeration vector. We pin the banner copy
 * client-side as well so:
 *  - the UI stays enumeration-resistant even if the backend's
 *    message phrasing changes;
 *  - a non-envelope 401 (e.g. empty body from a proxy) still surfaces
 *    the correct user-facing text instead of a generic
 *    "Request failed (HTTP 401)".
 *
 * These tests are PROTECTIVE — they pin the banner text exactly so a
 * future refactor can't quietly regress this case.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val authRepository: AuthRepository = mockk()
    private val sessionFlow = MutableStateFlow<Session?>(null)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        every { authRepository.sessionFlow } returns sessionFlow
        // AuthViewModel uses viewModelScope under the hood; pin the
        // main dispatcher to UnconfinedTestDispatcher so coroutines
        // launched inside `login()` complete inline. (The email
        // validator now uses a plain JVM regex — see the same field
        // in AuthViewModel — so we don't need to mock
        // android.util.Patterns anymore.)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = AuthViewModel(authRepository)

    private fun validUser(): User = User(
        userId = 1,
        fullName = "Alice",
        email = "alice@example.com",
        profileImage = null,
        createdAt = "2026-01-01 00:00:00",
    )

    /** Seed the form state by calling the field setters. */
    private fun viewModelSeededWith(
        email: String = "alice@example.com",
        password: String = "right-password",
    ): AuthViewModel {
        val vm = viewModel()
        vm.onLoginEmailChanged(email)
        vm.onLoginPasswordChanged(password)
        return vm
    }

    // -----------------------------------------------------------------
    // Wrong credentials — banner copy
    // -----------------------------------------------------------------

    @Test
    fun `login on HTTP 401 surfaces the hardcoded Invalid email or password banner`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns
            ApiResult.Error(httpStatus = 401, message = "Invalid email or password.")
        val vm = viewModelSeededWith()

        vm.login(onSuccess = { })

        // Drain any pending state updates, then check the latest form state.
        val form = vm.loginForm.first { !it.isSubmitting }
        assertEquals("Invalid email or password.", form.banner)
    }

    @Test
    fun `login on HTTP 401 with a non-envelope body still surfaces Invalid email or password`() = runTest {
        // Simulates a misconfigured proxy / CDN that returns a bare
        // 401 with no JSON body. `safeApiCall` would normally produce
        // a "Request failed (HTTP 401)" message — the ViewModel must
        // override that for /api/auth/login specifically.
        coEvery { authRepository.login(any(), any()) } returns
            ApiResult.Error(httpStatus = 401, message = "Request failed (HTTP 401)")
        val vm = viewModelSeededWith()

        vm.login(onSuccess = { })

        val form = vm.loginForm.first { !it.isSubmitting }
        assertEquals("Invalid email or password.", form.banner)
    }

    @Test
    fun `login on HTTP 401 with the server's canonical message still pins the exact banner copy`() = runTest {
        // Pinning the literal exact text (period at the end) so the
        // test will fail if anyone trims, lower-cases, or otherwise
        // mutates the copy in a way that affects the visible banner.
        coEvery { authRepository.login(any(), any()) } returns
            ApiResult.Error(httpStatus = 401, message = "Whatever the server happens to say.")
        val vm = viewModelSeededWith()

        vm.login(onSuccess = { })

        val form = vm.loginForm.first { !it.isSubmitting }
        assertEquals("Invalid email or password.", form.banner)
    }

    @Test
    fun `login on HTTP 500 (server crash) keeps the server's message instead of substituting the credentials banner`() = runTest {
        // Non-401 errors MUST still surface server messaging so users
        // can report useful detail — we only override 401.
        coEvery { authRepository.login(any(), any()) } returns
            ApiResult.Error(httpStatus = 500, message = "Internal server error")
        val vm = viewModelSeededWith()

        vm.login(onSuccess = { })

        val form = vm.loginForm.first { !it.isSubmitting }
        assertEquals("Internal server error", form.banner)
    }

    // -----------------------------------------------------------------
    // Wrong credentials — banner clears between attempts
    // -----------------------------------------------------------------

    @Test
    fun `login clears any prior banner before re-submitting`() = runTest {
        // First attempt: server returns 401 (we expect the canonical banner).
        // Second attempt: server returns 500 (we expect the raw server message).
        // Confirms the pre-submit `banner = null` reset actually fires.
        coEvery { authRepository.login(any(), any()) } returnsMany listOf(
            ApiResult.Error(httpStatus = 401, message = "Invalid email or password."),
            ApiResult.Error(httpStatus = 500, message = "Internal server error"),
        )
        val vm = viewModelSeededWith()

        vm.login(onSuccess = { })
        val firstForm = vm.loginForm.first { !it.isSubmitting && it.banner != null }
        assertEquals("Invalid email or password.", firstForm.banner)

        // Reset the form to clear field state and resubmit.
        vm.onLoginEmailChanged("alice@example.com")
        vm.onLoginPasswordChanged("another-attempt")
        vm.login(onSuccess = { })

        val secondForm = vm.loginForm.first { !it.isSubmitting && it.banner == "Internal server error" }
        assertEquals("Internal server error", secondForm.banner)
    }

    // -----------------------------------------------------------------
    // Network unreachable — banner copy unchanged
    // -----------------------------------------------------------------

    @Test
    fun `login on NetworkError keeps the network banner`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns
            ApiResult.NetworkError(java.io.IOException("offline"))
        val vm = viewModelSeededWith()

        vm.login(onSuccess = { })

        val form = vm.loginForm.first { !it.isSubmitting && it.banner != null }
        assertEquals("Can't reach the server. Check your connection.", form.banner)
    }

    // -----------------------------------------------------------------
    // Pre-flight validation guards — banner should NOT be set
    // -----------------------------------------------------------------

    @Test
    fun `login with a blank email short-circuits without contacting the repository and leaves banner null`() = runTest {
        val vm = viewModelSeededWith(email = "", password = "anything")
        var successCalled = false

        vm.login(onSuccess = { successCalled = true })

        val form = vm.loginForm.first { it.emailError != null || it.banner != null || !it.isSubmitting }
        assertEquals("Enter your email.", form.emailError)
        assertNull(form.banner)
        assertEquals(false, successCalled)
    }

    @Test
    fun `login with a blank password short-circuits without contacting the repository and leaves banner null`() = runTest {
        val vm = viewModelSeededWith(email = "alice@example.com", password = "")
        var successCalled = false

        vm.login(onSuccess = { successCalled = true })

        val form = vm.loginForm.first { it.passwordError != null || it.banner != null || !it.isSubmitting }
        assertEquals("Enter your password.", form.passwordError)
        assertNull(form.banner)
        assertEquals(false, successCalled)
    }

    // -----------------------------------------------------------------
    // Success path — banner should be cleared
    // -----------------------------------------------------------------

    @Test
    fun `login on Success resets the form and clears the banner`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns ApiResult.Success(validUser())
        val vm = viewModelSeededWith()
        var successCalled = false

        vm.login(onSuccess = { successCalled = true })

        val form = vm.loginForm.first { it.email.isEmpty() && it.password.isEmpty() }
        assertEquals("", form.email)
        assertEquals("", form.password)
        assertNull(form.banner)
        assertEquals(false, form.isSubmitting)
        assertEquals(true, successCalled)
    }
}
