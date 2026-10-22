// Network layer for the buyer/seller app.
// Api declares HTTP operations; Repository handles responses, JSON decoding, pagination and file uploads.
package com.trustmart.app.data

import android.content.Context
import android.net.Uri
import com.google.gson.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.Response as ApiResponse
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Small transport surface; typed decoding lives at the repository boundary. */
interface Api {
    @GET suspend fun get(@Url path: String, @QueryMap query: Map<String, String> = emptyMap()): ApiResponse<JsonElement>
    @POST suspend fun post(@Url path: String, @Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<JsonElement>
    @PATCH suspend fun patch(@Url path: String, @Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<JsonElement>
    @Multipart @POST suspend fun upload(@Url path: String, @Part parts: List<MultipartBody.Part>): ApiResponse<JsonElement>
    @Multipart @PATCH suspend fun uploadPatch(@Url path: String, @Part parts: List<MultipartBody.Part>): ApiResponse<JsonElement>
    @DELETE suspend fun delete(@Url path: String): ApiResponse<JsonElement>
}
class ApiFailure(val status: Int, message: String): IOException(message)
val apiGson: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

fun normalizedServer(input: String, allowHttp: Boolean): String {
    val url = input.trim().trimEnd('/').toHttpUrl()
    require(url.username.isEmpty() && url.password.isEmpty() && url.query == null && url.fragment == null) { "Use a server URL without passwords, query or fragment." }
    require(allowHttp || url.isHttps) { "Release builds require HTTPS." }
    require(url.encodedPath.trimEnd('/').endsWith("/api")) { "The URL must end with /api/." }
    return url.toString().trimEnd('/') + "/"
}

class Repository(private val context: Context, val session: SessionStore) {
    private val base = session.server.toHttpUrl()
    private val plain = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build()
    // Keep a plain client for session work and add the session interceptor to normal API requests.
    private val client = plain.newBuilder().addInterceptor(SessionInterceptor(base, session, plain)).build()
    private val api = Retrofit.Builder().baseUrl(base).client(client)
        .addConverterFactory(GsonConverterFactory.create(apiGson)).build().create(Api::class.java)

    // Turn an HTTP failure into an exception so callers can display an error instead of treating it as data.
    private fun unwrap(res: ApiResponse<JsonElement>): JsonElement {
        if (!res.isSuccessful) {
            val error = res.errorBody()?.string().orEmpty()
            val message = if (error.trim().startsWith("{")) error.take(700) else "Server returned HTTP ${res.code()}. Please retry."
            throw ApiFailure(res.code(), message)
        }
        return res.body() ?: JsonObject()
    }
    suspend fun get(path: String, query: Map<String, String> = emptyMap()) = unwrap(api.get(path, query))
    suspend fun post(path: String, body: Map<String, Any> = emptyMap()) = unwrap(api.post(path, body))
    suspend fun patch(path: String, body: Map<String, Any>) = unwrap(api.patch(path, body))
    suspend fun delete(path: String) = unwrap(api.delete(path))
    fun <T> decode(json: JsonElement, type: Class<T>): T = apiGson.fromJson(json, type)

    /** Follow page numbers at the configured origin rather than trusting absolute pagination URLs. */
    suspend fun <T> all(path: String, type: Class<T>, query: Map<String, String> = emptyMap()): List<T> {
        val items = mutableListOf<T>()
        var page = 1
        do {
            val data = get(path, query + ("page" to page.toString()))
            val array = if (data.isJsonArray) data.asJsonArray else data.asJsonObject["results"].asJsonArray
            items += array.map { decode(it, type) }
            val next = if (data.isJsonObject) data.asJsonObject["next"] else null
            if (next == null || next.isJsonNull) break
            check(page++ < 1000) { "Too many pages. Narrow your search." }
        } while (true)
        return items
    }

    /** Files come from Android's document picker: no broad storage permission is needed. */
    suspend fun upload(path: String, fields: Map<String, String>, files: List<Pair<String, Uri>>, patch: Boolean = false): JsonElement = withContext(Dispatchers.IO) {
        val parts = fields.map { MultipartBody.Part.createFormData(it.key, it.value) }.toMutableList()
        files.forEachIndexed { index, (field, uri) ->
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                val data = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    require(data.size() + count <= 10 * 1024 * 1024) { "Each attachment must be 10 MB or smaller." }
                    data.write(buffer, 0, count)
                }
                data.toByteArray()
            } ?: throw IOException("Cannot read selected attachment. Select it again.")
            val extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "bin"
            parts += MultipartBody.Part.createFormData(field, "upload-$index.$extension", bytes.toRequestBody(mime.toMediaType()))
        }
        unwrap(if (patch) api.uploadPatch(path, parts) else api.upload(path, parts))
    }
    fun mediaUrl(path: String): String = base.resolve(path)?.toString() ?: path
}
