package space.o4bit.projectasteria.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.o4bit.projectasteria.data.api.NasaApodService
import space.o4bit.projectasteria.data.model.AstronomyPicture
import space.o4bit.projectasteria.data.model.EnhancedAstronomyPicture
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

class SpaceRepository(
    private val apodDao: space.o4bit.projectasteria.data.local.ApodDao,
    private val nasaApodService: NasaApodService = NasaApodService.create()
) {

    // Cache for today's picture to avoid unnecessary DB hits
    private var memoryCache: EnhancedAstronomyPicture? = null

    suspend fun getTodaysAstronomyPicture(): EnhancedAstronomyPicture = withContext(Dispatchers.IO) {
        // Return memory cache if available
        memoryCache?.let { return@withContext it }

        try {
            // Try network first
            val apod = nasaApodService.getLatestAstronomyPicture()
            val spaceFact = getRandomSpaceFact()
            val notificationTitle = "Today's Space Discovery: ${apod.title}"
            val notificationBody = createNotificationBody(apod)

            val enhanced = EnhancedAstronomyPicture(
                astronomyPicture = apod,
                shortFact = spaceFact,
                notificationTitle = notificationTitle,
                notificationBody = notificationBody
            )

            // Save to DB
            apodDao.insertApod(space.o4bit.projectasteria.data.local.ApodEntity.from(enhanced))
            memoryCache = enhanced

            enhanced
        } catch (e: Exception) {
            // Fallback to local DB
            val cached = apodDao.getLatestApod()
            if (cached != null) {
                val enhanced = cached.toEnhancedAstronomyPicture()
                memoryCache = enhanced
                enhanced
            } else {
                throw e
            }
        }
    }

    suspend fun getHistory(limit: Int = 20, offset: Int = 0): List<EnhancedAstronomyPicture> = withContext(Dispatchers.IO) {
        val cached = apodDao.getPagedApods(limit, offset)
        if (cached.isEmpty() && offset == 0) {
            // If empty cache on first load, try to fetch last 10 days as a seed
            // This is a simple implementation. For production, Paging 3 RemoteMediator is better.
            try {
                // Fetching a range is complicated with this API service structure,
                // so we rely on the worker or individual fetches for now.
                // Or if the API supports it, we could add range support.
                // For now, return empty or what we have.
                emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            cached.map { it.toEnhancedAstronomyPicture() }
        }
    }

    // Helper to fetch range from API (if we add the endpoint support)
    // suspend fun fetchRange(...)

    @Suppress("unused")
    internal suspend fun getAstronomyPictureForDate(date: Date): EnhancedAstronomyPicture = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val formattedDate = dateFormat.format(date)

        // Check DB first
        val cached = apodDao.getApodByDate(formattedDate)
        if (cached != null) {
            return@withContext cached.toEnhancedAstronomyPicture()
        }

        // Use the new /apod/{date} endpoint
        val apod = nasaApodService.getAstronomyPictureByDate(formattedDate)
        val spaceFact = getRandomSpaceFact()
        val notificationTitle = "Space Discovery: ${apod.title}"
        val notificationBody = createNotificationBody(apod)

        val enhanced = EnhancedAstronomyPicture(
            astronomyPicture = apod,
            shortFact = spaceFact,
            notificationTitle = notificationTitle,
            notificationBody = notificationBody
        )

        // Save to DB
        apodDao.insertApod(space.o4bit.projectasteria.data.local.ApodEntity.from(enhanced))

        enhanced
    }

    /**
     * Get an APOD by date string (yyyy-MM-dd). Checks DB first, then network.
     */
    suspend fun getApodByDate(dateString: String): EnhancedAstronomyPicture? = withContext(Dispatchers.IO) {
        try {
            val cached = apodDao.getApodByDate(dateString)
            if (cached != null) {
                return@withContext cached.toEnhancedAstronomyPicture()
            }
            // Try network
            val apod = nasaApodService.getAstronomyPictureByDate(dateString)
            val enhanced = EnhancedAstronomyPicture(
                astronomyPicture = apod,
                shortFact = getRandomSpaceFact(),
                notificationTitle = "Space Discovery: ${apod.title}",
                notificationBody = createNotificationBody(apod)
            )
            apodDao.insertApod(space.o4bit.projectasteria.data.local.ApodEntity.from(enhanced))
            enhanced
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchHistory(days: Int = 10) {
        val today = Date()
        val calendar = java.util.Calendar.getInstance()
        calendar.time = today

        // Fetch last 'days' days in parallel
        val jobs = (0 until days).map { i ->
            val date = calendar.time
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
            date
        }

        // We use supervisorScope to allow individual failures
        withContext(Dispatchers.IO) {
            jobs.forEach { date ->
                try {
                    // This already checks DB and saves result
                    getAstronomyPictureForDate(date)
                } catch (e: Exception) {
                    // Ignore failure for individual day in history fetch
                    e.printStackTrace()
                }
            }
        }
    }


    private fun createNotificationBody(apod: AstronomyPicture): String {
        val explanation = apod.explanation
        val firstSentence = explanation.split(". ").firstOrNull()?.plus(".") ?: explanation

        return if (firstSentence.length <= 100) {
            firstSentence
        } else {
            firstSentence.substring(0, 97) + "..."
        }
    }

    private fun getRandomSpaceFact(): String {
        val facts = listOf(
            "Light from the Sun takes about 8 minutes to reach Earth.",
            "A day on Venus is longer than a year on Venus.",
            "The largest volcano in our solar system is on Mars - Olympus Mons.",
            "The Great Red Spot on Jupiter is a storm that has been raging for at least 400 years.",
            "Saturn's rings are made mostly of ice particles, with a small amount of rocky debris.",
            "Neptune's winds are the fastest in the solar system, reaching speeds of 1,200 mph.",
            "The temperature at the Sun's core is about 27 million degrees Fahrenheit.",
            "One million Earths could fit inside the Sun.",
            "The Milky Way galaxy is estimated to contain 100-400 billion stars.",
            "The Hubble Space Telescope orbits Earth at about 17,000 mph.",
            "The universe is estimated to be about 13.8 billion years old.",
            "Black holes have gravitational pulls so strong that even light cannot escape.",
            "Neutron stars can rotate up to 600 times per second.",
            "There are more stars in the universe than grains of sand on all of Earth's beaches.",
            "The closest known galaxy to the Milky Way is the Canis Major Dwarf Galaxy.",
            "The largest known star, UY Scuti, has a radius about 1,700 times that of the Sun.",
            "A teaspoonful of neutron star material would weigh about a billion tons.",
            "The footprints left by Apollo astronauts on the Moon will likely last for millions of years.",
            "The Moon is moving away from Earth at a rate of about 1.5 inches per year.",
            "Pluto's orbit is so eccentric that it sometimes comes closer to the Sun than Neptune."
        )

        return facts[Random().nextInt(facts.size)]
    }

    suspend fun getPagedHistory(page: Int, pageSize: Int): List<EnhancedAstronomyPicture> {
        return withContext(Dispatchers.IO) {
            val offset = (page - 1) * pageSize
            apodDao.getPagedApods(pageSize, offset).map { it.toEnhancedAstronomyPicture() }
        }
    }

    fun getApodHistory(): kotlinx.coroutines.flow.Flow<androidx.paging.PagingData<EnhancedAstronomyPicture>> {
        return androidx.paging.Pager(
            config = androidx.paging.PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { space.o4bit.projectasteria.data.paging.ApodPagingSource(this) }
        ).flow
    }
}
