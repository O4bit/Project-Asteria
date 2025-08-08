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

/**
 * Repository for handling space data operations
 */
class SpaceRepository(
    private val nasaApodService: NasaApodService = NasaApodService.create()
) {
    /**
     * Fetch today's astronomy picture with an added space fact
     */
    suspend fun getTodaysAstronomyPicture(): EnhancedAstronomyPicture = withContext(Dispatchers.IO) {
        val apod = nasaApodService.getAstronomyPictureOfDay()

        // Generate enhanced content with a space fact
        val spaceFact = getRandomSpaceFact()
        val notificationTitle = "Today's Space Discovery: ${apod.title}"
        val notificationBody = createNotificationBody(apod)

        EnhancedAstronomyPicture(
            astronomyPicture = apod,
            shortFact = spaceFact,
            notificationTitle = notificationTitle,
            notificationBody = notificationBody
        )
    }

    /**
     * Fetch astronomy picture for a specific date with an added space fact
     * Note: Currently unused but kept for future date-specific features
     */
    @Suppress("unused")
    internal suspend fun getAstronomyPictureForDate(date: Date): EnhancedAstronomyPicture = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val formattedDate = dateFormat.format(date)

        val apod = nasaApodService.getAstronomyPictureOfDay(date = formattedDate)

        // Generate enhanced content with a space fact
        val spaceFact = getRandomSpaceFact()
        val notificationTitle = "Space Discovery: ${apod.title}"
        val notificationBody = createNotificationBody(apod)

        EnhancedAstronomyPicture(
            astronomyPicture = apod,
            shortFact = spaceFact,
            notificationTitle = notificationTitle,
            notificationBody = notificationBody
        )
    }

    /**
     * Create a notification body from the astronomy picture
     */
    private fun createNotificationBody(apod: AstronomyPicture): String {
        // Extract first sentence or a short excerpt from the explanation
        val explanation = apod.explanation
        val firstSentence = explanation.split(". ").firstOrNull()?.plus(".") ?: explanation

        return if (firstSentence.length <= 100) {
            firstSentence
        } else {
            firstSentence.substring(0, 97) + "..."
        }
    }

    /**
     * Get a random space fact to enhance the astronomy picture
     */
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
}
