package space.o4bit.projectasteria.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import space.o4bit.projectasteria.BuildConfig
import space.o4bit.projectasteria.data.model.AstronomyPicture
import java.util.concurrent.TimeUnit

/**
 * APOD endpoints exposed by the Asteria Rust proxy (`/v2/nasa/apod/...`).
 *
 * The client never talks to `api.nasa.gov` directly anymore — the proxy
 * attaches the server-side NASA_API_KEY, enforces rate limits, caches,
 * and validates inputs. Response bodies are NASA's native APOD shape,
 * so [AstronomyPicture] stays unchanged.
 */
interface NasaApodService {

    /** Today's APOD. */
    @GET("v2/nasa/apod/today")
    suspend fun getLatestAstronomyPicture(): AstronomyPicture

    /** APOD for a specific date (YYYY-MM-DD). */
    @GET("v2/nasa/apod")
    suspend fun getAstronomyPictureByDate(
        @Query("date") date: String
    ): AstronomyPicture

    /**
     * APOD entries between [startDate] and [endDate] inclusive (YYYY-MM-DD).
     * Server caps the range to 30 days and returns HTTP 400 otherwise.
     */
    @GET("v2/nasa/apod/range")
    suspend fun getAstronomyPictureRange(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): List<AstronomyPicture>

    companion object {
        fun create(): NasaApodService {
            val logger = HttpLoggingInterceptor().apply {
                // BASIC only — bodies may be large; never log Authorization-like headers.
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BuildConfig.ASTERIA_API_BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(NasaApodService::class.java)
        }
    }
}
