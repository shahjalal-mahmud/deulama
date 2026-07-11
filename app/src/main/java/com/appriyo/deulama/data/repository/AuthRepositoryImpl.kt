package com.appriyo.deulama.data.repository

import com.appriyo.deulama.data.local.datastore.SessionManager
import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.data.remote.api.AuthApi
import com.appriyo.deulama.data.remote.dto.LoginRequest
import com.appriyo.deulama.data.remote.dto.RegisterRequest
import com.appriyo.deulama.data.remote.dto.UserDto
import com.appriyo.deulama.data.remote.safeApiCall
import com.appriyo.deulama.domain.model.Session
import com.appriyo.deulama.domain.model.User
import com.appriyo.deulama.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

/**
 * Concrete [AuthRepository] backed by the Retrofit [AuthApi] and the
 * [SessionManager] for persistence. On register/login success it
 * persists the returned Session so subsequent app launches stay
 * signed in.
 */
class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager,
    private val json: Json,
) : AuthRepository {

    override val sessionFlow: Flow<Session?> = sessionManager.sessionFlow

    override fun currentSession(): Session? = sessionManager.currentSession()

    override suspend fun register(
        fullName: String,
        email: String,
        password: String,
        passwordConfirmation: String,
    ): ApiResult<User> {
        val request = RegisterRequest(
            full_name = fullName,
            email = email,
            password = password,
            password_confirmation = passwordConfirmation,
        )
        return when (val result = safeApiCall(json) { authApi.register(request) }) {
            is ApiResult.Success -> {
                val user = result.value.user.toDomain()
                sessionManager.saveSession(Session(user, result.value.token))
                ApiResult.Success(user)
            }
            is ApiResult.ValidationError -> result
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
        }
    }

    override suspend fun login(email: String, password: String): ApiResult<User> {
        val request = LoginRequest(email = email, password = password)
        return when (val result = safeApiCall(json) { authApi.login(request) }) {
            is ApiResult.Success -> {
                val user = result.value.user.toDomain()
                sessionManager.saveSession(Session(user, result.value.token))
                ApiResult.Success(user)
            }
            is ApiResult.ValidationError -> result
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
        }
    }

    override suspend fun logout() {
        sessionManager.clear()
    }
}

private fun UserDto.toDomain(): User = User(
    userId = user_id,
    fullName = full_name,
    email = email,
    profileImage = profile_image,
    createdAt = created_at,
)