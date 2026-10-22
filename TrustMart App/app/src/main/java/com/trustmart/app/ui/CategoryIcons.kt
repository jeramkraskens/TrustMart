package com.trustmart.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CategoryDesign(val emoji: String, val accent: Color, val background: Color)

/** Matches the website's category symbols and tones, independent of database IDs. */
fun categoryDesign(name: String): CategoryDesign {
    fun matches(vararg words: String) = words.any { name.contains(it, ignoreCase = true) }
    val teal = Color(0xFF087C77) to Color(0xFFDFF5F3)
    val blue = Color(0xFF3F82FF) to Color(0xFFE0E9FF)
    val orange = Color(0xFFF59800) to Color(0xFFFFF1DB)
    val green = Color(0xFF09BC60) to Color(0xFFDCF7E9)
    val purple = Color(0xFF8B4CFF) to Color(0xFFEFE6FF)
    val red = Color(0xFFFF424D) to Color(0xFFFFE2E5)
    val (emoji, tone) = when {
        matches("mobile", "phone", "tablet") -> "📱" to teal
        matches("computer", "laptop") -> "💻" to blue
        matches("vehicle") -> "🚗" to orange
        matches("furniture", "home") -> "🏠" to green
        matches("fashion", "cloth") -> "👗" to purple
        matches("camera", "optic") -> "📷" to teal
        matches("gaming", "console") -> "🎮" to red
        matches("tool", "equipment") -> "🔧" to orange
        matches("sport", "outdoor") -> "⚽" to green
        matches("book", "education") -> "📚" to blue
        matches("music", "instrument") -> "🎸" to purple
        matches("baby", "kid") -> "🧸" to red
        else -> "📦" to teal
    }
    return CategoryDesign(emoji, tone.first, tone.second)
}

@Composable
fun CategoryIcon(design: CategoryDesign) {
    // The adjacent category name supplies the accessible label for this decoration.
    Box(Modifier.size(44.dp).background(design.background, RoundedCornerShape(12.dp))
        .clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Text(design.emoji, fontSize = 25.sp, lineHeight = 30.sp)
    }
}
