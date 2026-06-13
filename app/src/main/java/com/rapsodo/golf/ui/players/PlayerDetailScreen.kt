package com.rapsodo.golf.ui.players

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import android.graphics.Paint

import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.rapsodo.golf.domain.model.Player
import com.rapsodo.golf.domain.model.Shot

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerDetailScreen(
    onBack: () -> Unit,
    viewModel: PlayerDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val name = (uiState as? PlayerDetailUiState.Success)?.player?.name ?: ""
                    Text(name)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is PlayerDetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            is PlayerDetailUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is PlayerDetailUiState.Success -> {
                PlayerDetailContent(
                    player = state.player,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun PlayerDetailContent(
    player: Player,
    modifier: Modifier = Modifier
) {
    // sort shots most recent first
    val sortedShots = player.shots.sortedByDescending { it.recordedAt }

    val avgSpeed    = if (player.shots.isEmpty()) 0.0
    else player.shots.map { it.ballSpeedMph }.average()
    val avgDistance = if (player.shots.isEmpty()) 0.0
    else player.shots.map { it.totalDistanceYds }.average()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GlideImage(
                        model = player.avatarUrl,
                        contentDescription = player.name,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Handicap ${player.handicap}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Stats row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Avg Ball Speed",
                    value = "%.1f mph".format(avgSpeed),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Avg Distance",
                    value = "%.0f yds".format(avgDistance),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Total Shots",
                    value = "${player.shots.size}",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Club Distances",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Average carry distance per club",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ClubDistanceChart(shots = player.shots)
                }
            }
        }

        // Shots header
        item {
            Text(
                text = "Shot History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Shot rows
        if (sortedShots.isEmpty()) {
            item {
                Text(
                    text = "No shots recorded",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column {
                        sortedShots.forEachIndexed { index, shot ->
                            ShotRow(shot = shot)
                            if (index < sortedShots.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ShotRow(shot: Shot) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = shot.clubType,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = formatDate(shot.recordedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ShotStat(label = "Ball Speed", value = "${shot.ballSpeedMph} mph")
            ShotStat(label = "Launch", value = "${shot.launchAngleDeg}°")
            ShotStat(label = "Spin", value = "${shot.spinRateRpm} rpm")
            ShotStat(label = "Carry", value = "${shot.carryDistanceYds} yds")
        }
    }
}

@Composable
private fun ShotStat(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDate(recordedAt: String): String = try {
    val instant = Instant.parse(recordedAt)
    val formatter = DateTimeFormatter
        .ofPattern("MMM d, HH:mm")
        .withZone(ZoneId.systemDefault())
    formatter.format(instant)
} catch (e: Exception) {
    recordedAt
}

@Composable
private fun ClubDistanceChart(shots: List<Shot>) {
    if (shots.isEmpty()) return

    // compute average carry per club, sorted longest first
    val clubAverages = shots
        .groupBy { it.clubType }
        .map { (club, clubShots) ->
            club to clubShots.map { it.carryDistanceYds }.average()
        }
        .sortedByDescending { it.second }

    val maxDistance = clubAverages.maxOf { it.second }
    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val valueColor = MaterialTheme.colorScheme.primary.toArgb()

    val barHeight = 36f
    val barSpacing = 16f
    val labelWidth = 160f
    val valueWidth = 80f
    val chartHeight = (clubAverages.size * (barHeight + barSpacing) + barSpacing)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(chartHeight.dp)
            .padding(horizontal = 4.dp)
    ) {
        val availableWidth = size.width - labelWidth - valueWidth
        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 36f
        }

        clubAverages.forEachIndexed { index, (club, avgDistance) ->
            val y = barSpacing + index * (barHeight + barSpacing)
            val barWidth = (avgDistance / maxDistance * availableWidth).toFloat()

            // club label (left)
            paint.color = labelColor
            paint.textAlign = android.graphics.Paint.Align.RIGHT
            drawContext.canvas.nativeCanvas.drawText(
                club,
                labelWidth - 8f,
                y + barHeight * 0.65f,
                paint
            )

            // bar
            drawRoundRect(
                color = barColor,
                topLeft = Offset(labelWidth, y),
                size = Size(barWidth.coerceAtLeast(8f), barHeight),
                cornerRadius = CornerRadius(6f, 6f)
            )

            // distance value (right of bar)
            paint.color = valueColor
            paint.textAlign = android.graphics.Paint.Align.LEFT
            drawContext.canvas.nativeCanvas.drawText(
                "%.0f yds".format(avgDistance),
                labelWidth + barWidth + 8f,
                y + barHeight * 0.65f,
                paint
            )
        }
    }
}