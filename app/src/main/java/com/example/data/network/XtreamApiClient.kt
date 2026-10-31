package com.example.data.network

import com.example.data.model.XtreamAuthResponse
import com.example.data.model.XtreamCategory
import com.example.data.model.XtreamEpgResponse
import com.example.data.model.XtreamLiveStream
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class XtreamApiClient(
    customDns: Dns = Dns.SYSTEM
) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .dns(customDns)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "IPTVSmarters/3.1.5 (Linux; Android 14)")
                .header("Accept", "*/*")
                .build()
            chain.proceed(request)
        }
        .build()

    suspend fun authenticate(
        serverUrl: String,
        username: String,
        pass: String
    ): Result<XtreamAuthResponse> = withContext(Dispatchers.IO) {
        try {
            val base = sanitizeUrl(serverUrl)
            val url = "$base/player_api.php?username=$username&password=$pass"
            val request = Request.Builder().url(url).build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("خطأ في الاتصال بالسيرفر: ${response.code}"))
                }
                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("استجابة السيرفر فارغة"))

                val adapter = moshi.adapter(XtreamAuthResponse::class.java)
                val authRes = adapter.fromJson(body)
                if (authRes?.userInfo?.auth == 1 || authRes?.userInfo?.status?.equals("Active", ignoreCase = true) == true) {
                    Result.success(authRes)
                } else {
                    val msg = authRes?.userInfo?.message ?: "اسم المستخدم أو كلمة المرور غير صحيحة"
                    Result.failure(Exception(msg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLiveCategories(
        serverUrl: String,
        username: String,
        pass: String
    ): Result<List<XtreamCategory>> = withContext(Dispatchers.IO) {
        try {
            val base = sanitizeUrl(serverUrl)
            val url = "$base/player_api.php?username=$username&password=$pass&action=get_live_categories"
            val request = Request.Builder().url(url).build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("فشل جلب الأقسام: ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.success(emptyList())
                val listType = Types.newParameterizedType(List::class.java, XtreamCategory::class.java)
                val adapter = moshi.adapter<List<XtreamCategory>>(listType)
                val categories = adapter.fromJson(body) ?: emptyList()
                Result.success(categories)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLiveStreams(
        serverUrl: String,
        username: String,
        pass: String,
        categoryId: String? = null
    ): Result<List<XtreamLiveStream>> = withContext(Dispatchers.IO) {
        try {
            val base = sanitizeUrl(serverUrl)
            val action = if (categoryId.isNullOrEmpty() || categoryId == "all") {
                "get_live_streams"
            } else {
                "get_live_streams&category_id=$categoryId"
            }
            val url = "$base/player_api.php?username=$username&password=$pass&action=$action"
            val request = Request.Builder().url(url).build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("فشل جلب القنوات: ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.success(emptyList())
                val listType = Types.newParameterizedType(List::class.java, XtreamLiveStream::class.java)
                val adapter = moshi.adapter<List<XtreamLiveStream>>(listType)
                val streams = adapter.fromJson(body) ?: emptyList()
                Result.success(streams)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getShortEpg(
        serverUrl: String,
        username: String,
        pass: String,
        streamId: String
    ): Result<XtreamEpgResponse> = withContext(Dispatchers.IO) {
        try {
            val base = sanitizeUrl(serverUrl)
            val url = "$base/player_api.php?username=$username&password=$pass&action=get_short_epg&stream_id=$streamId"
            val request = Request.Builder().url(url).build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("فشل جلب دليل البرامج: ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("استجابة فارغة"))
                val adapter = moshi.adapter(XtreamEpgResponse::class.java)
                val epg = adapter.fromJson(body)
                if (epg != null) {
                    Result.success(epg)
                } else {
                    Result.failure(Exception("تعذر معالجة بيانات EPG"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchM3uContent(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("فشل تحميل قائمة M3U: ${response.code}"))
                }
                val body = response.body?.string() ?: ""
                Result.success(body)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun sanitizeUrl(url: String): String {
            var trimmed = url.trim()
            if (!trimmed.startsWith("http://", ignoreCase = true) &&
                !trimmed.startsWith("https://", ignoreCase = true)
            ) {
                trimmed = "http://$trimmed"
            }
            if (trimmed.endsWith("/")) {
                trimmed = trimmed.substring(0, trimmed.length - 1)
            }
            return trimmed
        }

        fun buildLiveStreamUrl(
            serverUrl: String,
            username: String,
            pass: String,
            streamId: String,
            format: String = "m3u8"
        ): String {
            val base = sanitizeUrl(serverUrl)
            return "$base/live/$username/$pass/$streamId.$format"
        }
    }
}
