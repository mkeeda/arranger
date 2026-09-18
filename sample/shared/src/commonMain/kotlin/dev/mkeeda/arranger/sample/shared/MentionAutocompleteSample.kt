package dev.mkeeda.arranger.sample.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.TextColorKey
import dev.mkeeda.arranger.richtext.attributeContainerOf
import dev.mkeeda.arranger.richtext.editor.AutocompleteMatch
import dev.mkeeda.arranger.richtext.editor.AutocompleteTrigger
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.RichTextState
import dev.mkeeda.arranger.richtext.editor.applyCompletion
import dev.mkeeda.arranger.richtext.editor.createPopupPositionProvider
import dev.mkeeda.arranger.richtext.editor.toRgbaColor
import kotlinx.coroutines.launch

private data class ChatUser(
    val id: String,
    val name: String,
    val role: String,
    val color: Color,
)

private data class ChatChannel(
    val id: String,
    val name: String,
    val description: String,
)

private data class ChatMessage(
    val id: String,
    val sender: String,
    val content: RichString,
    val isCurrentUser: Boolean = false,
)

private val sampleUsers =
    listOf(
        ChatUser("1", "Alice", "Product Designer", Color(0xFFE91E63)),
        ChatUser("2", "Bob", "Android Engineer", Color(0xFF2196F3)),
        ChatUser("3", "Charlie", "Tech Lead", Color(0xFF4CAF50)),
        ChatUser("4", "Diana", "QA Engineer", Color(0xFFFF9800)),
        ChatUser("5", "Emma", "Project Manager", Color(0xFF9C27B0)),
    )

private val sampleChannels =
    listOf(
        ChatChannel("general", "general", "General team discussion"),
        ChatChannel("random", "random", "Casual chatter and memes"),
        ChatChannel("arranger", "arranger-dev", "Arranger library development"),
        ChatChannel("releases", "releases", "Release announcements"),
    )

@Composable
fun MentionAutocompleteSample(modifier: Modifier = Modifier) {
    val editorState = remember { RichTextState(RichString("")) }
    var autocompleteMatch by remember { mutableStateOf<AutocompleteMatch?>(null) }
    val messages =
        remember {
            mutableStateListOf(
                ChatMessage(
                    id = "1",
                    sender = "Arranger Bot",
                    content = RichString("Welcome! Try typing '@' to mention team members or '#' to reference channels."),
                    isCurrentUser = false,
                ),
            )
        }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding(),
    ) {
        // Chat messages timeline
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            items(messages, key = { it.id }) { message ->
                ChatMessageBubble(message = message)
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        HorizontalDivider()

        // Editor input area with autocompletion popup
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(20.dp),
                        )
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(20.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (editorState.richString.text.isEmpty()) {
                        Text(
                            text = "Type '@' for mention, '#' for channel...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    RichTextEditor(
                        state = editorState,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle =
                            MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                        autocompleteTriggers =
                            listOf(
                                AutocompleteTrigger(prefix = "@"),
                                AutocompleteTrigger(prefix = "#"),
                            ),
                        onAutocompleteChange = { match ->
                            autocompleteMatch = match
                        },
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val text = editorState.richString.text.trim()
                        if (text.isNotEmpty()) {
                            messages.add(
                                ChatMessage(
                                    id = (messages.size + 1).toString(),
                                    sender = "You",
                                    content = editorState.richString,
                                    isCurrentUser = true,
                                ),
                            )
                            editorState.setRichString(RichString(""))
                            autocompleteMatch = null
                            coroutineScope.launch {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Send")
                }
            }

            // Autocompletion Suggestion Popup
            autocompleteMatch?.let { match ->
                Popup(
                    popupPositionProvider = match.createPopupPositionProvider(offset = IntOffset(0, -8)),
                    onDismissRequest = { autocompleteMatch = null },
                ) {
                    ElevatedCard(
                        modifier =
                            Modifier
                                .width(280.dp)
                                .heightIn(max = 240.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
                        colors =
                            CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                    ) {
                        if (match.trigger.prefix == "@") {
                            val filteredUsers =
                                sampleUsers.filter {
                                    it.name.contains(match.query, ignoreCase = true) ||
                                        it.role.contains(match.query, ignoreCase = true)
                                }
                            if (filteredUsers.isEmpty()) {
                                Box(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "No users found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            } else {
                                LazyColumn {
                                    items(filteredUsers) { user ->
                                        UserSuggestionItem(
                                            user = user,
                                            onClick = {
                                                val replacement = "@${user.name} "
                                                editorState.applyCompletion(
                                                    match = match,
                                                    replacement = replacement,
                                                    attributes =
                                                        attributeContainerOf(
                                                            BoldKey to Unit,
                                                            TextColorKey to user.color.toRgbaColor(),
                                                        ),
                                                )
                                                autocompleteMatch = null
                                            },
                                        )
                                    }
                                }
                            }
                        } else if (match.trigger.prefix == "#") {
                            val filteredChannels =
                                sampleChannels.filter {
                                    it.name.contains(match.query, ignoreCase = true)
                                }
                            if (filteredChannels.isEmpty()) {
                                Box(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "No channels found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            } else {
                                LazyColumn {
                                    items(filteredChannels) { channel ->
                                        ChannelSuggestionItem(
                                            channel = channel,
                                            onClick = {
                                                val replacement = "#${channel.name} "
                                                editorState.applyCompletion(
                                                    match = match,
                                                    replacement = replacement,
                                                    attributes =
                                                        attributeContainerOf(
                                                            BoldKey to Unit,
                                                            TextColorKey to Color(0xFF1976D2).toRgbaColor(),
                                                        ),
                                                )
                                                autocompleteMatch = null
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: ChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isCurrentUser) Alignment.End else Alignment.Start,
    ) {
        Text(
            text = message.sender,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )

        Card(
            shape =
                RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isCurrentUser) 16.dp else 4.dp,
                    bottomEnd = if (message.isCurrentUser) 4.dp else 16.dp,
                ),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (message.isCurrentUser) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                ),
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                RichTextEditor(
                    state = remember(message.content) { RichTextState(message.content) },
                    readOnly = true,
                    enabled = false,
                    textStyle =
                        MaterialTheme.typography.bodyMedium.copy(
                            color =
                                if (message.isCurrentUser) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        ),
                )
            }
        }
    }
}

@Composable
private fun UserSuggestionItem(
    user: ChatUser,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = user.color.copy(alpha = 0.2f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = user.name.take(1),
                    color = user.color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = user.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = user.role,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChannelSuggestionItem(
    channel: ChatChannel,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "#",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = channel.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
