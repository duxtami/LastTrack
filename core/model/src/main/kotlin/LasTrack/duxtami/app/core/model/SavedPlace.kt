package LasTrack.duxtami.app.core.model

enum class PlaceCategory(val displayName: String) {
    PARKING("Bike / Parking"),
    FAVORITE("Favorite"),
    WORK("Work"),
    HOME("Home"),
    SCENIC("Scenic Point"),
    FOOD("Food & Cafe"),
    FUEL("Fuel Station"),
    CAMPING("Camping"),
    CUSTOM("Other");

    companion object {
        fun fromString(name: String?): PlaceCategory {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: CUSTOM
        }
    }
}

data class SavedPlace(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val category: PlaceCategory = PlaceCategory.CUSTOM,
    val customLabel: String? = null,
    val description: String? = null,
    val imagePath: String? = null,
    val isReminderActive: Boolean = false,
    val reminderNote: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val iconKey: String? = null,
) {
    val displayCategory: String
        get() = if (!customLabel.isNullOrBlank()) customLabel else category.displayName
}
