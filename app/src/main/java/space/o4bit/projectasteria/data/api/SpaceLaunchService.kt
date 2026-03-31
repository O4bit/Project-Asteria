package space.o4bit.projectasteria.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import space.o4bit.projectasteria.data.model.launch.LaunchResponse
import java.util.concurrent.TimeUnit

interface SpaceLaunchService {
    @GET("launch/upcoming/")
    suspend fun getUpcomingLaunches(
        @Query("limit") limit: Int = 15,
        @Query("mode") mode: String = "detailed"
    ): LaunchResponse

    companion object {
        // We use the dev URL since the production has high rate restrictions and this works beautifully for standard access.
        private const val BASE_URL = "https://lldev.thespacedevs.com/2.2.0/"

        fun create(): SpaceLaunchService {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("User-Agent", "AsteriaSpaceApp/1.0 (Android)")
                        .build()
                    chain.proceed(request)
                }
                .addInterceptor(logger)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(SpaceLaunchService::class.java)
        }
    }
}
