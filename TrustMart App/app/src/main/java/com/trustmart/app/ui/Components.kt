// Shared Compose theme and reusable controls used across buyer/seller screens.
package com.trustmart.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.trustmart.app.AppViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

// Screenshot-derived tokens. Layout stays adaptive; system bars are real Android UI, not painted copies.
val Teal = Color(0xFF169B8E)
val DeepTeal = Color(0xFF0D5952)
val Navy = Color(0xFF17181B)
val Paper = Color(0xFFF8F8F8)
val Muted = Color(0xFF83858E)
val Line = Color(0xFFE2E3E7)
val Mint = Color(0xFFDFF5F3)
val Orange = Color(0xFFF59D0D)
val Cream = Color(0xFFFFF2DC)
val Red = Color(0xFFF53D45)
val Blush = Color(0xFFFFE3E5)
val LocalBusy = compositionLocalOf { false }
val LocalAccent = compositionLocalOf { Teal }
val LocalBack = compositionLocalOf { {} }

@Composable fun TrustMartTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Teal, secondary = Orange, background = Paper,
        // Material reapplies tint alpha at elevation; a transparent black tint would turn white cards gray.
        surface = Color.White, surfaceTint = Color.White, onSurface = Navy, onBackground = Navy),
        typography = Typography(bodyLarge = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
            bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
            labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)), content = content)
}

/** Screen-bound polling stops when the route leaves STARTED; duplicate submissions remain serialized. */
@Composable fun LoadScreen(vm: AppViewModel, key: Any, poll: Boolean = false, load: suspend () -> Unit) {
    // Preview and local layout tests must never contact the shared marketplace database.
    if (androidx.compose.ui.platform.LocalInspectionMode.current) return
    val owner = LocalLifecycleOwner.current
    val currentLoad by rememberUpdatedState(load)
    LaunchedEffect(owner, key) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            do {
                vm.runScreenLoad { currentLoad() }
                if (!poll) break
                delay(15.seconds)
            } while (true)
        }
    }
}

@Composable fun Page(title: String = "", subtitle: String = "", content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (title.isNotBlank()) Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (subtitle.isNotBlank()) Text(subtitle, color = Muted)
        content()
        Spacer(Modifier.height(12.dp))
    }
}
@Composable fun Panel(modifier: Modifier = Modifier, color: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}
@Composable fun Field(label: String, value: String, change: (String) -> Unit, secret: Boolean = false,
    multiline: Boolean = false, enabled: Boolean = true, keyboard: KeyboardType = KeyboardType.Text,
    placeholder: String = "", lockWhileBusy: Boolean = true) {
    var visible by remember { mutableStateOf(false) }
    val accent = LocalAccent.current
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        if (label.isNotEmpty()) Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Navy)
        OutlinedTextField(value, change, Modifier.fillMaxWidth(),
            enabled = enabled && (!lockWhileBusy || !LocalBusy.current), singleLine = !multiline, minLines = if (multiline) 3 else 1,
            textStyle = TextStyle(fontSize = 14.sp), placeholder = { Text(placeholder, color = Muted, fontSize = 13.sp) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = Line,
                focusedContainerColor = Paper, unfocusedContainerColor = Paper),
            visualTransformation = if (secret && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (secret) { { TextButton({ visible = !visible }) { Text(if (visible) "Hide" else "Show", fontSize = 11.sp) } } } else null,
            keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else keyboard))
    }
}
@Composable fun Action(label: String, enabled: Boolean = true, click: () -> Unit) {
    Button(click, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = enabled && !LocalBusy.current,
        colors = ButtonDefaults.buttonColors(containerColor = LocalAccent.current), shape = RoundedCornerShape(15.dp)) { Text(label) }
}
@Composable fun OutlineAction(label: String, enabled: Boolean = true, click: () -> Unit) {
    OutlinedButton(click, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = enabled && !LocalBusy.current,
        border = BorderStroke(1.dp, if (enabled) LocalAccent.current else Line),
        shape = RoundedCornerShape(13.dp)) { Text(label, color = if (enabled) LocalAccent.current else Muted) }
}
@Composable fun Link(label: String, click: () -> Unit) {
    TextButton(click, enabled = !LocalBusy.current) { Text(label, color = LocalAccent.current, fontSize = 12.sp) }
}
@Composable fun Choice(label: String, value: String, options: List<Pair<String, String>>, change: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Box {
            OutlinedButton({ expanded = true }, enabled = !LocalBusy.current,
                border = BorderStroke(1.dp, Line), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(options.firstOrNull { it.first == value }?.second ?: value, color = Navy, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ExpandMore, null, Modifier.size(18.dp), tint = Muted)
            }
            DropdownMenu(expanded, { expanded = false }) {
                options.forEach { (id, title) -> DropdownMenuItem(text = { Text(title) }, onClick = { change(id); expanded = false }) }
            }
        }
    }
}
@Composable fun Empty(text: String) { Panel { Text(text, color = Muted) } }
@Composable fun Status(value: String) {
    val color = when (value.lowercase().replace('_', ' ')) {
        "rejected", "dispute" -> Red
        "held", "in escrow", "pending", "pending verification" -> Orange
        "refunded", "delivered" -> Color(0xFF437EE4)
        else -> Teal
    }
    Surface(color = color.copy(alpha = .12f), shape = RoundedCornerShape(30.dp)) {
        Text(value.replace('_', ' ').replaceFirstChar { it.uppercase() }, Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
            color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}
@Composable fun Confirm(title: String, text: String, accept: () -> Unit, dismiss: () -> Unit) {
    val busy = LocalBusy.current
    AlertDialog(onDismissRequest = { if (!busy) dismiss() }, title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(accept, enabled = !busy) { Text("Confirm") } },
        dismissButton = { TextButton(dismiss, enabled = !busy) { Text("Cancel") } })
}
@Composable fun RoundIcon(icon: ImageVector, label: String, color: Color = Teal, background: Color = Mint, click: () -> Unit) {
    IconButton(click, enabled = !LocalBusy.current, modifier = Modifier.size(48.dp).background(background, CircleShape)) {
        Icon(icon, label, Modifier.size(21.dp), tint = color)
    }
}
@Composable fun BackCircle(light: Boolean = false) {
    RoundIcon(Icons.AutoMirrored.Filled.ArrowBack, "Back", if (light) Color.White else Navy,
        if (light) Color.White.copy(alpha = .18f) else Color.White, LocalBack.current)
}
@Composable fun Avatar(name: String, size: Int = 44) {
    Box(Modifier.size(size.dp).background(Mint, CircleShape), contentAlignment = Alignment.Center) {
        Text(initials(name), color = Teal, fontWeight = FontWeight.Bold, fontSize = (size / 3).sp)
    }
}
@Composable fun SectionHeading(title: String, action: String = "See all", click: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (click != null) Link(action, click)
    }
}
@Composable fun InfoCard(title: String, description: String, color: Color = Teal, icon: ImageVector = Icons.Default.VerifiedUser) {
    Surface(color = color.copy(alpha = .12f), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(25.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = color, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(description, color = color, fontSize = 11.sp, lineHeight = 17.sp)
            }
        }
    }
}
/** Fixed action area sits outside scrolling content, above the actual system navigation inset. */
@Composable fun BottomActions(content: @Composable RowScope.() -> Unit) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}
@Composable fun GradientHero(title: String, subtitle: String, color: Color = Teal, icon: ImageVector? = null) {
    Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(color.copy(red = color.red * .72f, green = color.green * .72f, blue = color.blue * .72f), color)))
        .padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth()) { BackCircle(true) }
        if (icon != null) Icon(icon, null, tint = Color.White, modifier = Modifier.size(38.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 23.sp)
        Text(subtitle, color = Color.White.copy(alpha = .85f), fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
    }
}
