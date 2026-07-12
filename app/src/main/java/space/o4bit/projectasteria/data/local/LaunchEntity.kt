package space.o4bit.projectasteria.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "launches")
data class LaunchEntity(
    @PrimaryKey val id: String,
    val name: String,
    val statusName: String,
    val statusDescription: String,
    val net: String,
    val providerName: String?,
    val padName: String?,
    val locationName: String?,
    val missionName: String?,
    val missionDescription: String?,
    val image: String?,
    val netMillis: Long
)
