package space.o4bit.projectasteria.ui.components

/**
 * Types of animated backgrounds available in the app.
 * Expanded with more animated background types.
 */
enum class BackgroundType(val displayName: String) {
    STARRY("Starry"),
    SPACE("Space"),
    CIRCLES("Circles"),
    RINGS("Rings"),
    MESH("Mesh"),
    GRID("Grid"),
    PARTICLES("Particles"),
    SHAPES("Shapes"),
    NONE("None");

    companion object {
        val DEFAULT = STARRY

        fun fromName(name: String): BackgroundType {
            return entries.find { it.name == name } ?: DEFAULT
        }
    }
}
