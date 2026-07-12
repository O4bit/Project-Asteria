package space.o4bit.projectasteria.ui.components

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
