package com.example.domain.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.NeonBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonYellow

/** Profil avatarı. [cost] 0 ise herkese açıktır. */
data class Avatar(val id: Int, val emoji: String, val name: String, val cost: Int) {
    val shopId: String get() = "avatar_$id"
}

/** Profil çerçevesi (avatarın etrafındaki neon halka). */
data class Frame(val id: String, val name: String, val colors: List<Color>, val cost: Int) {
    val shopId: String get() = "frame_$id"
}

object Cosmetics {
    val AVATARS = listOf(
        Avatar(0, "🧠", "Beyin", 0),
        Avatar(1, "🧑‍🔬", "Bilim İnsanı", 0),
        Avatar(2, "🕵️", "Dedektif", 0),
        Avatar(3, "🦉", "Baykuş", 0),
        Avatar(4, "🚀", "Roket", 0),
        Avatar(5, "💡", "Fikir", 0),
        Avatar(6, "🎨", "Sanatçı", 0),
        Avatar(7, "🧩", "Yapboz", 0),
        Avatar(100, "🤖", "Siber Zihin", 120),
        Avatar(101, "👾", "Piksel Uzaylı", 150),
        Avatar(102, "🦊", "Kurnaz Tilki", 150),
        Avatar(103, "🐉", "Neon Ejder", 250),
        Avatar(104, "🧙", "Zihin Büyücüsü", 250),
        Avatar(105, "🦄", "Tek Boynuz", 300),
        Avatar(106, "👑", "Kral Zekâ", 400),
        Avatar(107, "🪐", "Kozmik Akıl", 400)
    )

    val FRAMES = listOf(
        Frame("none", "Çerçevesiz", listOf(NeonBorder, NeonBorder), 0),
        Frame("cyan", "Camgöbeği Işık", listOf(NeonCyan, NeonCyan), 100),
        Frame("pink", "Pembe Neon", listOf(NeonPink, NeonPurple), 150),
        Frame("toxic", "Zehirli Yeşil", listOf(NeonGreen, NeonYellow), 200),
        Frame("aurora", "Kutup Işığı", listOf(NeonCyan, NeonPurple, NeonPink), 300),
        Frame("gold", "Altın Taç", listOf(NeonYellow, Color(0xFFFF8A00), NeonYellow), 450)
    )

    fun avatar(id: Int): Avatar = AVATARS.firstOrNull { it.id == id } ?: AVATARS[0]
    fun frame(id: String): Frame = FRAMES.firstOrNull { it.id == id } ?: FRAMES[0]

    fun owns(owned: Set<String>, a: Avatar) = a.cost == 0 || a.shopId in owned
    fun owns(owned: Set<String>, f: Frame) = f.cost == 0 || f.shopId in owned
}
