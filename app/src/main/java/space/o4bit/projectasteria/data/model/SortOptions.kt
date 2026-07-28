package space.o4bit.projectasteria.data.model

enum class SortDirection {
    ASCENDING,
    DESCENDING;

    fun toggle(): SortDirection = if (this == ASCENDING) DESCENDING else ASCENDING
}

enum class AsteroidSortBy(val label: String) {
    DISTANCE("Miss Distance"),
    SPEED("Relative Speed"),
    SIZE("Estimated Size"),
    NAME("Asteroid Name")
}

enum class LaunchSortBy(val label: String) {
    DATE("Launch Date"),
    NAME("Mission Name")
}
