package com.krata.orbit.ui.coding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.krata.orbit.R
import com.krata.orbit.data.model.ContestItem
import com.krata.orbit.data.model.PotdItem
import com.krata.orbit.ui.components.*
import com.krata.orbit.ui.theme.HarmonyOsSansFamily
import com.krata.orbit.viewmodel.CodingViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CodingScreen(viewModel: CodingViewModel) {
    val uiState  by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item { TabBanner(title = "Coding") }

            // Offline banner
            item {
                AnimatedVisibility(
                    visible = !uiState.isConnected,
                    enter   = expandVertically(),
                    exit    = shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onErrorContainer,
                            modifier           = Modifier.size(18.dp)
                        )
                        Text(
                            text  = "Connect to internet to refresh data",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                }
            }

            // POTD Section
            item { SectionHeading(text = "POTD") }

            if (uiState.potdList.isEmpty()) {
                item {
                    EmptyState(
                        if (uiState.isConnected) "Fetching problems…" else "No cached data. Connect to internet.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(uiState.potdList) { potd ->
                    PotdCard(
                        potd     = potd,
                        onClick  = { runCatching { uriHandler.openUri(potd.url) } },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            item { SectionHeading(text = "CONTESTS") }

            // Filter: only show future/live contests
            val now = System.currentTimeMillis()
            val upcomingContests = uiState.contests.filter { it.endTimeMillis > now }

            if (upcomingContests.isEmpty()) {
                item {
                    EmptyState(
                        if (uiState.isConnected) "Fetching contests…" else "No cached contests. Connect to internet.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(upcomingContests) { contest ->
                    ContestCard(
                        contest  = contest,
                        onClick  = { runCatching { uriHandler.openUri(contest.url) } },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ── POTD card ─────────────────────────────────────────────────────────────────
@Composable
fun PotdCard(potd: PotdItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OrbitCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SiteLogo(site = potd.site, modifier = Modifier.size(40.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = potd.problemName,
                    style    = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color      = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                DifficultyChip(difficulty = potd.difficulty)
            }
        }
    }
}

// ── Contest card ──────────────────────────────────────────────────────────────
@Composable
fun ContestCard(contest: ContestItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val formatter = DateTimeFormatter.ofPattern("d MMM, hh:mm a", Locale.ENGLISH)
    val startStr  = Instant.ofEpochMilli(contest.startTimeMillis)
        .atZone(ZoneId.systemDefault()).format(formatter)
    val endStr    = Instant.ofEpochMilli(contest.endTimeMillis)
        .atZone(ZoneId.systemDefault()).format(formatter)
    val now       = System.currentTimeMillis()
    val isLive    = contest.startTimeMillis <= now && contest.endTimeMillis > now

    OrbitCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SiteLogo(site = contest.site, modifier = Modifier.size(40.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text     = contest.title,
                        style    = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            color      = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isLive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1A3A2A))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("LIVE", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFA8E6CF), fontWeight = FontWeight.Bold))
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = "Start: $startStr",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text  = "End: $endStr",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    }
}

// ── Site logo resolver ─────────────────────────────────────────────────────────
@Composable
fun SiteLogo(site: String, modifier: Modifier = Modifier) {
    val resId = when (site.lowercase()) {
        "leetcode"   -> R.drawable.ic_site_leetcode
        "gfg"        -> R.drawable.ic_site_gfg
        "codechef"   -> R.drawable.ic_site_codechef
        "codeforces" -> R.drawable.ic_site_codeforces
        else         -> R.drawable.ic_site_leetcode
    }
    Image(
        painter            = painterResource(id = resId),
        contentDescription = "$site logo",
        modifier           = modifier.clip(RoundedCornerShape(8.dp))
    )
}
