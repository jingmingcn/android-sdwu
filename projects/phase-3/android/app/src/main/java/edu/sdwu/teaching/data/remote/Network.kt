package edu.sdwu.teaching.data.remote

import edu.sdwu.teaching.BuildConfig
import edu.sdwu.teaching.data.remote.dto.ErrorDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Shared JSON settings: tolerate fields the app doesn't know yet (forward compatible). */
val NetworkJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/**
 * Rewrites every request to the server URL the user configured (stored in DataStore).
 * OkHttp runs interceptors on its own background threads, so blocking briefly to read the
 * (in-memory cached) DataStore value is safe here.
 */
class ServerUrlInterceptor(private val serverUrl: suspend () -> String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val base = runBlocking { serverUrl() }.toHttpUrlOrNull()
            ?: throw IOException("Invalid server URL. Change it in Profile -> Server.")
        val url = base.newBuilder()
            .addEncodedPathSegments(request.url.encodedPath.removePrefix("/"))
            .encodedQuery(request.url.encodedQuery)
            .build()
        return chain.proceed(request.newBuilder().url(url).build())
    }
}

/** Adds "Authorization: Bearer <token>" when a user is signed in. */
class AuthInterceptor(private val token: suspend () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { token() } ?: return chain.proceed(chain.request())
        return chain.proceed(chain.request().newBuilder().header("Authorization", "Bearer $token").build())
    }
}

/**
 * Shared client (connection pool, timeouts, logging). Coil uses it as-is for images; the API
 * client below adds the server-URL and auth interceptors on top. Built once by Hilt.
 */
fun createBaseOkHttpClient(): OkHttpClient =
    OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            },
        )
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

/** newBuilder() shares the base client's connection pool and threads. */
fun createApiOkHttpClient(
    base: OkHttpClient,
    serverUrl: suspend () -> String,
    token: suspend () -> String?,
): OkHttpClient =
    base.newBuilder()
        // Added first so the logging interceptor (from base, runs after) shows the final URL.
        .apply { interceptors().addAll(0, listOf(ServerUrlInterceptor(serverUrl), AuthInterceptor(token))) }
        .build()

fun createTeachingApi(client: OkHttpClient): TeachingApi =
    Retrofit.Builder()
        // Placeholder: ServerUrlInterceptor replaces scheme, host and port on every call.
        .baseUrl("http://localhost/")
        .client(client)
        .addConverterFactory(NetworkJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(TeachingApi::class.java)

/** Turns network exceptions into a sentence a student can act on. */
fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> {
        val detail = runCatching {
            response()?.errorBody()?.string()?.let { NetworkJson.decodeFromString<ErrorDto>(it).detail }
        }.getOrNull()
        when {
            // FastAPI's generic "Not Found" means the URL path is wrong, i.e. a bad server address.
            code() == 404 && detail == "Not Found" -> "Endpoint not found (404). Is the server address right?"
            detail != null -> detail
            code() == 401 -> "Please sign in again"
            code() == 422 -> "Please check the form fields"
            else -> "Server error (${code()})"
        }
    }
    is IOException -> "Can't reach the server. Check your connection and the server address."
    else -> message ?: "Something went wrong"
}
