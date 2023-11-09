package net.primal.android.core.compose.feed.note

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import net.primal.android.R
import net.primal.android.core.compose.DropdownMenuItemText
import net.primal.android.core.compose.icons.PrimalIcons
import net.primal.android.core.compose.icons.primaliconpack.ContextCopyNoteId
import net.primal.android.core.compose.icons.primaliconpack.ContextCopyNoteLink
import net.primal.android.core.compose.icons.primaliconpack.ContextCopyNoteText
import net.primal.android.core.compose.icons.primaliconpack.ContextCopyPublicKey
import net.primal.android.core.compose.icons.primaliconpack.ContextCopyRawData
import net.primal.android.core.compose.icons.primaliconpack.ContextMuteUser
import net.primal.android.core.compose.icons.primaliconpack.ContextShare
import net.primal.android.core.compose.icons.primaliconpack.More
import net.primal.android.core.utils.copyText
import net.primal.android.core.utils.resolvePrimalNoteLink
import net.primal.android.core.utils.systemShareText
import net.primal.android.crypto.hexToNoteHrp
import net.primal.android.crypto.hexToNpubHrp
import net.primal.android.theme.AppTheme

@Composable
fun NoteDropdownMenuIcon(
    modifier: Modifier,
    noteId: String,
    noteContent: String,
    noteRawData: String,
    authorId: String,
    onMuteUserClick: () -> Unit,
) {
    var menuVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val uiScope = rememberCoroutineScope()
    val copyConfirmationText = stringResource(id = R.string.feed_context_copied_toast)

    Box(
        modifier = modifier.clickable {
            menuVisible = true
        },
    ) {
        Icon(
            modifier = Modifier.wrapContentSize(align = Alignment.TopEnd),
            imageVector = PrimalIcons.More,
            contentDescription = null,
        )

        DropdownMenu(
            modifier = Modifier.background(color = AppTheme.extraColorScheme.surfaceVariantAlt1),
            expanded = menuVisible,
            onDismissRequest = { menuVisible = false },
        ) {
            DropdownMenuItem(
                trailingIcon = {
                    Icon(
                        imageVector = PrimalIcons.ContextShare,
                        contentDescription = null
                    )
                },
                text = { DropdownMenuItemText(text = stringResource(id = R.string.feed_context_share_note)) },
                onClick = {
                    systemShareText(
                        context = context,
                        text = resolvePrimalNoteLink(noteId = noteId)
                    )
                    menuVisible = false
                }
            )
            DropdownMenuItem(
                trailingIcon = {
                    Icon(
                        imageVector = PrimalIcons.ContextCopyNoteLink,
                        contentDescription = null
                    )
                },
                text = { DropdownMenuItemText(text = stringResource(id = R.string.feed_context_copy_note_link)) },
                onClick = {
                    copyText(context = context, text = resolvePrimalNoteLink(noteId = noteId))
                    menuVisible = false
                    uiScope.launch {
                        Toast.makeText(
                            context,
                            copyConfirmationText,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
            DropdownMenuItem(
                trailingIcon = {
                    Icon(
                        imageVector = PrimalIcons.ContextCopyNoteText,
                        contentDescription = null
                    )
                },
                text = { DropdownMenuItemText(text = stringResource(id = R.string.feed_context_copy_note_text)) },
                onClick = {
                    copyText(context = context, text = noteContent)
                    menuVisible = false
                    uiScope.launch {
                        Toast.makeText(
                            context,
                            copyConfirmationText,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
            DropdownMenuItem(
                trailingIcon = {
                    Icon(
                        imageVector = PrimalIcons.ContextCopyNoteId,
                        contentDescription = null
                    )
                },
                text = { DropdownMenuItemText(text = stringResource(id = R.string.feed_context_copy_note_id)) },
                onClick = {
                    copyText(context = context, text = noteId.hexToNoteHrp())
                    menuVisible = false
                    uiScope.launch {
                        Toast.makeText(
                            context,
                            copyConfirmationText,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
            DropdownMenuItem(
                trailingIcon = {
                    Icon(
                        imageVector = PrimalIcons.ContextCopyRawData,
                        contentDescription = null
                    )
                },
                text = { DropdownMenuItemText(text = stringResource(id = R.string.feed_context_copy_raw_data)) },
                onClick = {
                    copyText(context = context, text = noteRawData)
                    menuVisible = false
                    uiScope.launch {
                        Toast.makeText(
                            context,
                            copyConfirmationText,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
            DropdownMenuItem(
                trailingIcon = {
                    Icon(
                        imageVector = PrimalIcons.ContextCopyPublicKey,
                        contentDescription = null
                    )
                },
                text = { DropdownMenuItemText(text = stringResource(id = R.string.feed_context_copy_user_id)) },
                onClick = {
                    copyText(context = context, text = authorId.hexToNpubHrp())
                    menuVisible = false
                    uiScope.launch {
                        Toast.makeText(
                            context,
                            copyConfirmationText,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
            DropdownMenuItem(
                trailingIcon = {
                    Icon(
                        imageVector = PrimalIcons.ContextMuteUser,
                        contentDescription = null,
                        tint = AppTheme.colorScheme.error,
                    )
                },
                text = {
                    DropdownMenuItemText(
                        text = stringResource(id = R.string.context_menu_mute_user),
                        color = AppTheme.colorScheme.error,
                    )
                },
                onClick = {
                    onMuteUserClick()
                    menuVisible = false
                }
            )
        }
    }
}