package com.rapsodo.golf.ui.players

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.rapsodo.golf.domain.logger.AppLogger
import com.rapsodo.golf.domain.model.Player
import io.github.aakira.napier.Napier

private const val TAG = AppLogger.TAG

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerListScreen(
    onPlayerClick: (String) -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedClub by viewModel.selectedClub.collectAsStateWithLifecycle()
    val clubs by viewModel.availableClubs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Golf Players") }) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState is PlayerUiState.Loading,
            onRefresh = {
                Napier.d(tag = TAG) { "Pull to refresh triggered" }
                viewModel.syncFromNetwork()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Search players...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = {
                                Napier.d(tag = TAG) { "Search cleared" }
                                viewModel.onSearchQueryChanged("")
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (clubs.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedClub == null,
                            onClick = {
                                Napier.d(tag = TAG) { "Club filter cleared" }
                                viewModel.onClubSelected(null)
                            },
                            label = { Text("All") }
                        )
                        clubs.forEach { club ->
                            FilterChip(
                                selected = selectedClub == club,
                                onClick = {
                                    val next = if (selectedClub == club) null else club
                                    Napier.d(tag = TAG) { "Club filter selected: $next" }
                                    viewModel.onClubSelected(next)
                                },
                                label = { Text(club) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                when (val state = uiState) {
                    is PlayerUiState.Loading -> {
                        Napier.d(tag = TAG) { "State: Loading" }
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is PlayerUiState.Success -> {
                        if (state.players.isEmpty()) {
                            Napier.i(tag = TAG) { "State: Success — no results for query='$searchQuery' club='$selectedClub'" }
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No players match your search",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Napier.i(tag = TAG) { "State: Success — showing ${state.players.size} players" }
                            PlayerList(
                                players = state.players,
                                onPlayerClick = { playerId ->
                                    Napier.i(tag = AppLogger.TAG) { "Player tapped: $playerId" }
                                    onPlayerClick(playerId)
                                }
                            )
                        }
                    }
                    is PlayerUiState.Empty -> {
                        Napier.w(tag = TAG) { "State: Empty — no data and sync failed" }
                        ErrorCard()
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerList(
    players: List<Player>,
    onPlayerClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(players, key = { it.id }) { player ->
            PlayerCard(
                player = player,
                onClick = { onPlayerClick(player.id) }
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun PlayerCard(
    player: Player,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlideImage(
                model = player.avatarUrl,
                contentDescription = player.name,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Handicap: ${player.handicap}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${player.shots.size} shots recorded",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ErrorCard() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Sorry, we have some issues getting player data",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Please check your connection and pull down to retry.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}