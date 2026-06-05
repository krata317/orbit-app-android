package com.krata.orbit.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krata.orbit.ui.theme.BelfastGroteskBlackFamily
import com.krata.orbit.ui.theme.HarmonyOsSansFamily

// ── Tab banner (replaces TopAppBar for each tab) ──────────────────────────────
@Composable
fun TabBanner(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 20.dp, end = 20.dp, top = 56.dp, bottom = 12.dp)
    ) {
        Text(
            text  = title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily   = BelfastGroteskBlackFamily,
                fontWeight   = FontWeight.Black,
                fontSize     = 32.sp,
                color        = MaterialTheme.colorScheme.onBackground
            )
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

// ── Section heading ───────────────────────────────────────────────────────────
@Composable
fun SectionHeading(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text     = text,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        style    = MaterialTheme.typography.labelLarge.copy(
            fontFamily   = HarmonyOsSansFamily,
            color        = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.5.sp,
            fontWeight   = FontWeight.Bold
        )
    )
}

// ── Rounded card (traverse-android-master style) ──────────────────────────────
@Composable
fun OrbitCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = content
    )
}

// ── Circular checkbox ─────────────────────────────────────────────────────────
@Composable
fun CircularCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary
                      else Color.Transparent,
        animationSpec = tween(200),
        label = "checkbox_bg"
    )
    val scale by animateFloatAsState(
        targetValue = if (checked) 1.1f else 1f,
        animationSpec = tween(150),
        label = "checkbox_scale"
    )

    Box(
        modifier = modifier
            .size(26.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(bgColor)
            .border(
                width = 2.dp,
                color = if (checked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector        = Icons.Default.Check,
                contentDescription = "Checked",
                tint               = MaterialTheme.colorScheme.onPrimary,
                modifier           = Modifier.size(16.dp)
            )
        }
    }
}

// ── Primary action button ─────────────────────────────────────────────────────
@Composable
fun OrbitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    isDestructive: Boolean = false
) {
    Button(
        onClick  = onClick,
        modifier = modifier.height(44.dp),
        shape    = RoundedCornerShape(12.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor = if (isDestructive) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
            contentColor   = if (isDestructive) MaterialTheme.colorScheme.onError
                            else MaterialTheme.colorScheme.onPrimary
        )
    ) {
        if (icon != null) {
            icon()
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text  = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

// ── Outlined secondary button ─────────────────────────────────────────────────
@Composable
fun OrbitOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null
) {
    OutlinedButton(
        onClick  = onClick,
        modifier = modifier.height(44.dp),
        shape    = RoundedCornerShape(12.dp),
        colors   = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        if (icon != null) {
            icon()
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text  = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

// ── Difficulty chip ───────────────────────────────────────────────────────────
@Composable
fun DifficultyChip(difficulty: String, modifier: Modifier = Modifier) {
    val (bg, fg) = when (difficulty.lowercase()) {
        "easy"   -> Pair(Color(0xFF1A3A2A), Color(0xFFA8E6CF))
        "medium" -> Pair(Color(0xFF3A2A1A), Color(0xFFFFD3B6))
        "hard"   -> Pair(Color(0xFF3A1A1A), Color(0xFFFFAAA5))
        else     -> Pair(Color(0xFF2C2C2C), Color(0xFFD0D0D0))
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text  = difficulty,
            style = MaterialTheme.typography.labelSmall.copy(color = fg, fontWeight = FontWeight.Bold)
        )
    }
}

// ── Gradient background for User profile banner ───────────────────────────────
@Composable
fun GradientBanner(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460)
                    )
                )
            ),
        content = content
    )
}

// ── Empty state placeholder ───────────────────────────────────────────────────
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier       = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
