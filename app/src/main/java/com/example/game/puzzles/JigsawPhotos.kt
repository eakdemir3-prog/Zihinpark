package com.example.game.puzzles

/** Parçaları Birleştir için uygulamaya gömülü gerçek fotoğraf kategorileri (Unsplash lisanslı). */
data class PhotoCategory(val key: String, val emoji: String, val title: String)

object JigsawPhotos {
    const val PER_CATEGORY = 12
    val CATEGORIES = listOf(
        PhotoCategory("forest", "🌲", "Orman"),
        PhotoCategory("mountain", "🏔️", "Dağlar"),
        PhotoCategory("sea", "🌊", "Deniz"),
        PhotoCategory("city", "🏙️", "Şehirler"),
        PhotoCategory("animal", "🐾", "Hayvanlar"),
        PhotoCategory("food", "🍓", "Lezzetler"),
        PhotoCategory("object", "📷", "Nostalji"),
        PhotoCategory("flower", "🌸", "Çiçekler"),
        PhotoCategory("wonder", "✨", "Doğa Harikaları"),
        PhotoCategory("arch", "🏛️", "Mimari")
    )
    val COUNT get() = CATEGORIES.size * PER_CATEGORY
    fun categoryOf(photo: Int) = CATEGORIES[photo / PER_CATEGORY]
    fun resName(photo: Int) = "jig_${categoryOf(photo).key}_${"%02d".format(photo % PER_CATEGORY)}"
}
