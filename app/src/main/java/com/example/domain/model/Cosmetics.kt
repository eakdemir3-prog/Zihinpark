package com.example.domain.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.NeonBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonYellow

import com.example.ui.components.MascotMood
import com.example.ui.components.MascotStyle
import com.example.ui.components.OwlExtra
import com.example.ui.components.OwlEyes
import com.example.ui.components.OwlHat

/**
 * Profil avatarı. [cost] 0 ise herkese açıktır.
 * [mascot] doluysa emoji yerine o kılıktaki Bilge çizilir; [animated] ise sürekli hareket eder.
 */
data class Avatar(
    val id: Int, val emoji: String, val name: String, val cost: Int,
    val mascot: MascotStyle? = null, val mood: MascotMood = MascotMood.Happy,
    val animated: Boolean = false, val bg: List<Color> = emptyList()
) {
    val shopId: String get() = "avatar_$id"
}

/** Çerçeve biçimleri: düz halka dışında şekilli ve hareketli olanlar. */
enum class FrameShape { Ring, Dots, Hex, Scallop, Gear, Burst, Petal, Orbit, Pulse, Crown }

/** Profil çerçevesi (avatarın etrafındaki neon halka ya da şekil). */
data class Frame(
    val id: String, val name: String, val colors: List<Color>, val cost: Int,
    val shape: FrameShape = FrameShape.Ring, val animated: Boolean = true
) {
    val shopId: String get() = "frame_$id"
}

object Cosmetics {
    private val Lav = listOf(Color(0xFFEDE4FF), Color(0xFFFFD9EC))
    private val Night = listOf(Color(0xFF3A2A7A), Color(0xFF140E2E))
    private val Sky = listOf(Color(0xFFBDE7FF), Color(0xFFE9F7FF))
    private val Sun = listOf(Color(0xFFFFE9B8), Color(0xFFFFC978))
    private val Mint = listOf(Color(0xFFCFF7E4), Color(0xFFE8FFF4))
    private val Rose = listOf(Color(0xFFFFD3E0), Color(0xFFFFEEF3))

    val AVATARS = listOf(
        Avatar(0, "🧠", "Beyin", 0),
        Avatar(1, "🧑‍🔬", "Bilim İnsanı", 0),
        Avatar(2, "🕵️", "Dedektif", 0),
        Avatar(3, "🦉", "Baykuş", 0),
        Avatar(4, "🚀", "Roket", 0),
        Avatar(5, "💡", "Fikir", 0),
        Avatar(6, "🎨", "Sanatçı", 0),
        Avatar(7, "🧩", "Yapboz", 0),
        // ---- Bilge avatarları ----
        Avatar(200, "🦉", "Bilge", 0, MascotStyle.Default, MascotMood.Happy, bg = Lav),
        Avatar(201, "🦉", "Göz Kırpan Bilge", 0, MascotStyle.Default, MascotMood.Wink, bg = Sky),
        Avatar(202, "🦉", "Neşeli Bilge", 0, MascotStyle.Default, MascotMood.Cheer, animated = true, bg = Sun),
        Avatar(203, "🦉", "Düşünür Bilge", 80, MascotStyle(extra = OwlExtra.Bowtie), MascotMood.Think, bg = Mint),
        Avatar(204, "🦉", "Havalı Bilge", 120, MascotStyle(eyes = OwlEyes.Sun, hat = OwlHat.None), bg = Sun),
        Avatar(205, "🦉", "Parti Bilgesi", 150, MascotStyle(hat = OwlHat.Party, accent = Color(0xFFFF4DA6)), MascotMood.Cheer, animated = true, bg = Rose),
        Avatar(206, "🦉", "Müziksever Bilge", 150, MascotStyle(hat = OwlHat.Headphones, accent = Color(0xFF22D3EE)), MascotMood.Wink, animated = true, bg = Night),
        Avatar(207, "🦉", "Kış Bilgesi", 180, MascotStyle(Color(0xFF8FD0FF), Color(0xFF3B82E0), Color(0xFF2B62B8), Color(0xFFEAF6FF), OwlHat.Beanie, extra = OwlExtra.Scarf, accent = Color(0xFFFF5A5F)), bg = Sky),
        Avatar(208, "🦉", "Ninja Bilge", 200, MascotStyle(Color(0xFF4B5578), Color(0xFF1E2440), Color(0xFF141A30), Color(0xFFD7DBEA), OwlHat.Headband, OwlEyes.Plain, accent = Color(0xFFE11D48)), bg = Night),
        Avatar(209, "🦉", "Korsan Bilge", 220, MascotStyle(Color(0xFFFFB37A), Color(0xFFD9662B), Color(0xFFB04A18), Color(0xFFFFF1E2), OwlHat.Bandana, OwlEyes.Patch, accent = Color(0xFFD62839)), MascotMood.Happy, bg = Sky),
        Avatar(210, "🦉", "Astronot Bilge", 250, MascotStyle(Color(0xFFF4F6FB), Color(0xFFB9C3DA), Color(0xFF8E9BBA), Color(0xFFD9ECFF), OwlHat.Helmet, OwlEyes.Plain), animated = true, bg = Night),
        Avatar(211, "🦉", "Büyücü Bilge", 300, MascotStyle(hat = OwlHat.Wizard, sparkle = true), MascotMood.Wink, animated = true, bg = Night),
        Avatar(212, "🦉", "Süper Bilge", 350, MascotStyle(Color(0xFF7CC4FF), Color(0xFF2563EB), Color(0xFF1D4ED8), Color(0xFFFFE680), OwlHat.None, OwlEyes.Mask, OwlExtra.Cape, Color(0xFFEF4444)), MascotMood.Happy, animated = true, bg = Sun),
        Avatar(213, "🦉", "Altın Bilge", 500, MascotStyle(Color(0xFFFFE680), Color(0xFFF2A516), Color(0xFFD98A0B), Color(0xFFFFF7D6), sparkle = true), MascotMood.Happy, animated = true, bg = Night),
        Avatar(214, "🦉", "Kral Bilge", 600, MascotStyle(Color(0xFFC79BFF), Color(0xFF6D28D9), Color(0xFF5B21B6), Color(0xFFF3E8FF), OwlHat.Crown, extra = OwlExtra.Cape, accent = Color(0xFFDC2626), sparkle = true), MascotMood.Happy, animated = true, bg = Sun),
        Avatar(215, "🦉", "Gökkuşağı Bilgesi", 800, MascotStyle(rainbow = true, sparkle = true), MascotMood.Cheer, animated = true, bg = Lav),
        // ---- Emoji avatarları ----
        Avatar(100, "🤖", "Siber Zihin", 120),
        Avatar(101, "👾", "Piksel Uzaylı", 150),
        Avatar(102, "🦊", "Kurnaz Tilki", 150),
        Avatar(103, "🐉", "Neon Ejder", 250),
        Avatar(104, "🧙", "Zihin Büyücüsü", 250),
        Avatar(105, "🦄", "Tek Boynuz", 300),
        Avatar(106, "👑", "Kral Zekâ", 400),
        Avatar(107, "🪐", "Kozmik Akıl", 400)
    )

    private val Rainbow = listOf(Color(0xFFFF4D6D), Color(0xFFFFB02E), Color(0xFFFFE94D), Color(0xFF3DDC84), Color(0xFF22D3EE), Color(0xFF8B5CF6))
    private val GoldC = listOf(NeonYellow, Color(0xFFFF8A00), NeonYellow)

    val FRAMES = listOf(
        Frame("none", "Çerçevesiz", listOf(NeonBorder, NeonBorder), 0, animated = false),
        Frame("dots", "Noktalı Halka", listOf(NeonPurple, NeonPink), 80, FrameShape.Dots, false),
        Frame("cyan", "Camgöbeği Işık", listOf(NeonCyan, NeonCyan), 100),
        Frame("hex", "Altıgen Kalkan", listOf(NeonCyan, NeonPurple), 120, FrameShape.Hex, false),
        Frame("pink", "Pembe Neon", listOf(NeonPink, NeonPurple), 150),
        Frame("scallop", "Papatya", listOf(NeonPink, NeonYellow), 150, FrameShape.Scallop, false),
        Frame("toxic", "Zehirli Yeşil", listOf(NeonGreen, NeonYellow), 200),
        Frame("gear", "Dönen Çark", listOf(Color(0xFF94A3B8), NeonCyan), 220, FrameShape.Gear),
        Frame("burst", "Güneş Patlaması", listOf(NeonYellow, Color(0xFFFF8A00)), 250, FrameShape.Burst),
        Frame("petal", "Lotus", listOf(NeonPink, NeonPurple, NeonCyan), 280, FrameShape.Petal, false),
        Frame("aurora", "Kutup Işığı", listOf(NeonCyan, NeonPurple, NeonPink), 300),
        Frame("orbit", "Yörünge", listOf(NeonCyan, NeonPurple), 300, FrameShape.Orbit),
        Frame("pulse", "Nabız", listOf(NeonPink, NeonPurple), 350, FrameShape.Pulse),
        Frame("gold", "Altın Taç", GoldC, 450),
        Frame("rainbow", "Gökkuşağı Yıldızı", Rainbow, 500, FrameShape.Burst),
        Frame("royal", "Kraliyet Tacı", GoldC, 700, FrameShape.Crown)
    )

    fun avatar(id: Int): Avatar = AVATARS.firstOrNull { it.id == id } ?: AVATARS[0]
    fun frame(id: String): Frame = FRAMES.firstOrNull { it.id == id } ?: FRAMES[0]

    fun owns(owned: Set<String>, a: Avatar) = a.cost == 0 || a.shopId in owned
    fun owns(owned: Set<String>, f: Frame) = f.cost == 0 || f.shopId in owned
}
