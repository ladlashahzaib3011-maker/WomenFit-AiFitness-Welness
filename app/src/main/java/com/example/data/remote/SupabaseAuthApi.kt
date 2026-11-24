package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

/**
 * Retrofit API contract for Supabase GoTrue Authentication (/auth/v1)
 * and Supabase PostgreSQL PostgREST (/rest/v1).
 *
 * Security note: Never enable HTTP body logging for auth endpoints so
 * passwords and bearer tokens never appear in Logcat.
 */
interface SupabaseAuthApi {

    @POST("auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") apiKey: String,
        @Body request: SupabaseSignUpRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/token")
    suspend fun signInWithPassword(
        @Header("apikey") apiKey: String,
        @Query("grant_type") grantType: String = "password",
        @Body request: SupabaseSignInRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/token")
    suspend fun refreshSession(
        @Header("apikey") apiKey: String,
        @Query("grant_type") grantType: String = "refresh_token",
        @Body request: SupabaseRefreshTokenRequest
    ): Response<SupabaseAuthResponse>

    @GET("auth/v1/user")
    suspend fun getCurrentUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String
    ): Response<SupabaseUser>

    @POST("auth/v1/logout")
    suspend fun signOut(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String
    ): Response<Unit>

    @POST("auth/v1/recover")
    suspend fun sendPasswordReset(
        @Header("apikey") apiKey: String,
        @Body request: SupabaseRecoverRequest
    ): Response<Unit>

    @GET("rest/v1/womenfit_ai_users")
    suspend fun getUserProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<SupabaseProfileDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("rest/v1/womenfit_ai_users?on_conflict=id")
    suspend fun upsertUserProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body profile: SupabaseProfileDto
    ): Response<List<SupabaseProfileDto>>
}

object SupabaseConfig {
    private fun readBuildConfigField(name: String): String {
        return try {
            val field = BuildConfig::class.java.getField(name)
            (field.get(null) as? String)?.trim().orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    private fun isUsableValue(value: String): Boolean {
        return value.isNotBlank() && value != "DEFAULT_EMPTY"
    }

    private fun normalizeProxyRoot(proxyUrl: String): String {
        val trimmed = proxyUrl.trim().trimEnd('/')
        return when {
            trimmed.endsWith("/api/coach/ask") -> trimmed.removeSuffix("/api/coach/ask")
            trimmed.endsWith("/coach") -> trimmed.removeSuffix("/coach")
            else -> trimmed
        }
    }

    fun resolveEndpoint(
        supabaseUrl: String = readBuildConfigField("SUPABASE_URL"),
        supabaseAnonKey: String = readBuildConfigField("SUPABASE_ANON_KEY"),
        backendProxyUrl: String = readBuildConfigField("BACKEND_PROXY_URL"),
        backendProxyKey: String = readBuildConfigField("BACKEND_PROXY_KEY")
    ): Pair<String, String>? {
        if (isUsableValue(supabaseUrl) &&
            (supabaseUrl.startsWith("https://") || supabaseUrl.startsWith("http://")) &&
            isUsableValue(supabaseAnonKey)
        ) {
            return supabaseUrl to supabaseAnonKey
        }
        val proxyRoot = normalizeProxyRoot(backendProxyUrl)
        if (isUsableValue(proxyRoot) &&
            (proxyRoot.startsWith("https://") || proxyRoot.startsWith("http://"))
        ) {
            val key = if (isUsableValue(backendProxyKey)) backendProxyKey else "proxy-client"
            return proxyRoot to key
        }
        return null
    }

    fun getBaseUrl(): String = resolveEndpoint()?.first.orEmpty()

    fun getAnonKey(): String = resolveEndpoint()?.second.orEmpty()

    fun isConfigured(
        url: String = getBaseUrl(),
        anonKey: String = getAnonKey()
    ): Boolean {
        return isUsableValue(url) &&
            (url.startsWith("https://") || url.startsWith("http://")) &&
            isUsableValue(anonKey)
    }
}

object SupabaseClientProvider {
    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    fun createApi(baseUrl: String): SupabaseAuthApi {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseAuthApi::class.java)
    }

    fun parseErrorMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val adapter = moshi.adapter(SupabaseErrorResponse::class.java)
            val parsed = adapter.fromJson(errorBody)
            parsed?.errorDescription
                ?: parsed?.msg
                ?: parsed?.message
                ?: parsed?.error
        } catch (_: Exception) {
            null
        }
    }
}
