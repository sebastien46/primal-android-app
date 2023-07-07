package net.primal.android.profile.details

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.primal.android.R
import net.primal.android.core.compose.AppBarIcon
import net.primal.android.core.compose.AvatarThumbnailListItemImage
import net.primal.android.core.compose.IconText
import net.primal.android.core.compose.NostrUserText
import net.primal.android.core.compose.feed.FeedLazyColumn
import net.primal.android.core.compose.feed.LoadingItem
import net.primal.android.core.compose.feed.NoFeedContent
import net.primal.android.core.compose.icons.PrimalIcons
import net.primal.android.core.compose.icons.primaliconpack.ArrowBack
import net.primal.android.core.compose.icons.primaliconpack.Key
import net.primal.android.core.compose.icons.primaliconpack.Link
import net.primal.android.core.compose.isEmpty
import net.primal.android.core.ext.findByUrl
import net.primal.android.core.ext.findNearestOrNull
import net.primal.android.core.utils.isPrimalIdentifier
import net.primal.android.nostr.ext.asEllipsizedNpub
import net.primal.android.profile.details.model.ProfileDetailsUi
import net.primal.android.profile.details.model.ProfileStatsUi
import net.primal.android.theme.AppTheme
import java.text.NumberFormat

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onClose: () -> Unit,
    onPostClick: (String) -> Unit,
    onProfileClick: (String) -> Unit,
) {
    val uiState = viewModel.state.collectAsState()

    AdjustProfileStatusBarColor()

    ProfileScreen(
        state = uiState.value,
        onClose = onClose,
        onPostClick = onPostClick,
        onProfileClick = onProfileClick,
    )
}

@Composable
private fun AdjustProfileStatusBarColor() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val originalStatusBarColor = window.statusBarColor
        window.statusBarColor = Color.Transparent.toArgb()
        onDispose {
            window.statusBarColor = originalStatusBarColor
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
    state: ProfileContract.UiState,
    onClose: () -> Unit,
    onPostClick: (String) -> Unit,
    onProfileClick: (String) -> Unit,
) {
    val density = LocalDensity.current

    val maxAvatarSizeDp = 80.dp
    val maxAvatarSizePx = with(density) { maxAvatarSizeDp.roundToPx().toFloat() }
    val avatarSizePx = rememberSaveable { mutableStateOf(maxAvatarSizePx) }

    val maxCoverHeightDp = 112.dp
    val minCoverHeightDp = 64.dp
    val statusBarHeightDp = with(density) {
        WindowInsets.statusBars.getTop(density).toDp()
    }
    val maxCoverHeightPx = with(density) {
        (maxCoverHeightDp + statusBarHeightDp).roundToPx().toFloat()
    }
    val minCoverHeightPx = with(density) {
        (minCoverHeightDp + statusBarHeightDp).roundToPx().toFloat()
    }
    val coverHeightPx = rememberSaveable { mutableStateOf(maxCoverHeightPx) }

    val topBarTitleVisible = rememberSaveable { mutableStateOf(false) }
    val coverTransparency = rememberSaveable { mutableStateOf(0f) }

    val listState = rememberLazyListState()
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .filter { it.first == 0 }
            .map { it.second }
            .collect { scrollOffset ->
                val newCoverHeight = maxCoverHeightPx - scrollOffset
                coverHeightPx.value = newCoverHeight.coerceIn(minCoverHeightPx, maxCoverHeightPx)

                val newAvatarSize = maxAvatarSizePx - (scrollOffset * 1f)
                avatarSizePx.value = newAvatarSize.coerceIn(0f, maxAvatarSizePx)

                topBarTitleVisible.value = scrollOffset > maxAvatarSizePx

                val newCoverAlpha = 0f + scrollOffset / (maxCoverHeightPx - minCoverHeightPx)
                coverTransparency.value = newCoverAlpha.coerceIn(0.0f, 0.7f)
            }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { visiblePage ->
                if (visiblePage >= 1) {
                    topBarTitleVisible.value = true
                    coverHeightPx.value = minCoverHeightPx
                    avatarSizePx.value = 0f
                    coverTransparency.value = 0.7f
                }
            }
    }

    Surface {
        val pagingItems = state.authoredPosts.collectAsLazyPagingItems()
        FeedLazyColumn(
            contentPadding = PaddingValues(0.dp),
            pagingItems = pagingItems,
            listState = listState,
            onPostClick = onPostClick,
            onProfileClick = {
                if (state.profileId != it) {
                    onProfileClick(it)
                }
            },
            shouldShowLoadingState = false,
            shouldShowNoContentState = false,
            stickyHeader = {
                ProfileTopCoverBar(
                    state = state,
                    coverHeight = with(density) { coverHeightPx.value.toDp() },
                    coverAlpha = coverTransparency.value,
                    avatarSize = with(density) { avatarSizePx.value.toDp() },
                    avatarPadding = with(density) { (maxAvatarSizePx - avatarSizePx.value).toDp() },
                    avatarOffsetY = with(density) { (maxAvatarSizePx * 0.65f).toDp() },
                    navigationIcon = {
                        AppBarIcon(
                            icon = PrimalIcons.ArrowBack,
                            backgroundColor = Color.Black.copy(alpha = 0.5f),
                            tint = Color.White,
                            onClick = onClose,
                        )
                    },
                    title = {
                        AnimatedVisibility(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            visible = topBarTitleVisible.value,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            NostrUserText(
                                displayName = state.profileDetails?.displayName
                                    ?: state.profileId.asEllipsizedNpub(),
                                internetIdentifier = state.profileDetails?.internetIdentifier,
                            )
                        }
                    },
                )
            },
            header = {
                UserProfileDetails(
                    profileId = state.profileId,
                    profileDetails = state.profileDetails,
                    profileStats = state.profileStats,
                )

                if (pagingItems.isEmpty()) {
                    when (pagingItems.loadState.refresh) {
                        LoadState.Loading -> LoadingItem(
                            modifier = Modifier
                                .padding(vertical = 64.dp)
                                .fillMaxWidth(),
                        )

                        is LoadState.NotLoading -> NoFeedContent(
                            modifier = Modifier
                                .padding(vertical = 64.dp)
                                .fillMaxWidth(),
                            onRefresh = { pagingItems.refresh() }
                        )

                        is LoadState.Error -> Unit
                    }
                }
            }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ProfileTopCoverBar(
    state: ProfileContract.UiState,
    navigationIcon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    avatarSize: Dp,
    coverHeight: Dp,
    coverAlpha: Float = 0.0f,
    avatarOffsetY: Dp = 0.dp,
    avatarOffsetX: Dp = 0.dp,
    avatarPadding: Dp = 0.dp,
) {
    val coverBlur = AppTheme.colorScheme.surface.copy(alpha = coverAlpha)
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val resource = state.resources.findByUrl(url = state.profileDetails?.coverUrl)
        val variant = resource?.variants.findNearestOrNull(
            maxWidthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        )
        val imageSource = variant?.mediaUrl ?: state.profileDetails?.coverUrl
        SubcomposeAsyncImage(
            modifier = Modifier
                .background(color = AppTheme.colorScheme.surface)
                .fillMaxWidth()
                .height(coverHeight)
                .drawWithCache {
                    onDrawWithContent {
                        drawContent()
                        drawRect(color = coverBlur)
                    }
                },
            model = imageSource,
            loading = { CoverLoading() },
            error = { CoverUnavailable() },
            contentDescription = "Cover",
            contentScale = ContentScale.Crop,
        )

        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Unspecified,
                scrolledContainerColor = Color.Unspecified,
            ),
            navigationIcon = navigationIcon,
            title = title,
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(y = avatarOffsetY, x = avatarOffsetX)
                .padding(horizontal = 16.dp)
        ) {
            AvatarThumbnailListItemImage(
                modifier = Modifier
                    .size(avatarSize)
                    .padding(
                        top = avatarPadding * 0 / 3,
                        bottom = avatarPadding * 3 / 3,
                        start = avatarPadding * 1 / 8,
                        end = avatarPadding * 7 / 8,
                    ),
                source = state.profileDetails?.avatarUrl,
                hasBorder = state.profileDetails?.internetIdentifier.isPrimalIdentifier(),
            )
        }
    }
}

@Composable
private fun CoverLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = AppTheme.colorScheme.surface
            ),
    )
}

@Composable
private fun CoverUnavailable() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = AppTheme.colorScheme.surface
            ),
    )
}

@Composable
private fun UserProfileDetails(
    profileId: String,
    profileDetails: ProfileDetailsUi? = null,
    profileStats: ProfileStatsUi? = null,
) {
    val localUriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val uiScope = rememberCoroutineScope()
    val keyCopiedText = stringResource(id = R.string.settings_keys_key_copied)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = AppTheme.colorScheme.surfaceVariant)
    ) {
        ProfileActions()

        NostrUserText(
            modifier = Modifier.padding(horizontal = 16.dp),
            displayName = profileDetails?.displayName ?: profileId.asEllipsizedNpub(),
            internetIdentifier = profileDetails?.internetIdentifier,
            style = AppTheme.typography.titleLarge,
        )

        if (profileDetails?.internetIdentifier?.isNotEmpty() == true) {
            UserInternetIdentifier(internetIdentifier = profileDetails.internetIdentifier)
        }

        UserPublicKey(
            pubkey = profileId,
            onCopyClick = {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                val clip = ClipData.newPlainText("", it)
                clipboard.setPrimaryClip(clip)
                uiScope.launch { Toast.makeText(context, keyCopiedText, Toast.LENGTH_SHORT).show() }
            }
        )

        if (profileDetails?.about?.isNotEmpty() == true) {
            UserAbout(about = profileDetails.about)
        }

        if (profileDetails?.website?.isNotEmpty() == true) {
            UserWebsiteText(
                website = profileDetails.website,
                onClick = {
                    try {
                        localUriHandler.openUri(profileDetails.website)
                    } catch (error: ActivityNotFoundException) {
                        uiScope.launch {
                            Toast.makeText(
                                context,
                                "App not found that could open ${profileDetails.website}.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        }

        UserStats(
            followingCount = profileStats?.followingCount,
            followersCount = profileStats?.followersCount,
            notesCount = profileStats?.notesCount,
        )
    }
}

@Composable
private fun UserStats(
    followingCount: Int?,
    followersCount: Int?,
    notesCount: Int?,
    placeholderText: String = "-",
) {
    val numberFormat = remember { NumberFormat.getNumberInstance() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        TextCounter(
            modifier = Modifier.weight(1f),
            count = if (followingCount != null) numberFormat.format(followingCount) else placeholderText,
            text = stringResource(id = R.string.profile_following_stat),
        )

        TextCounter(
            modifier = Modifier.weight(1f),
            count = if (followingCount != null) numberFormat.format(followersCount) else placeholderText,
            text = stringResource(id = R.string.profile_followers_stat),
        )

        TextCounter(
            modifier = Modifier.weight(1f),
            count = if (notesCount != null) numberFormat.format(notesCount) else placeholderText,
            text = stringResource(id = R.string.profile_notes_stat),
        )
    }
}

@Composable
private fun TextCounter(
    count: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = count,
            style = AppTheme.typography.headlineMedium,
        )

        Text(
            text = text,
            style = AppTheme.typography.bodySmall,
            color = AppTheme.extraColorScheme.onSurfaceVariantAlt4,
        )
    }
}

@Composable
private fun UserInternetIdentifier(
    internetIdentifier: String,
) {
    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = internetIdentifier,
        style = AppTheme.typography.bodySmall,
        color = AppTheme.extraColorScheme.onSurfaceVariantAlt4,
    )
}

@Composable
private fun ProfileActions() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(all = 12.dp)
            .background(AppTheme.colorScheme.surfaceVariant),
        horizontalArrangement = Arrangement.End,
    ) {

    }
}

@Composable
private fun UserAbout(about: String) {
    Text(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        text = about,
        color = AppTheme.colorScheme.onSurface,
        style = AppTheme.typography.bodySmall,
    )
}

@Composable
private fun UserWebsiteText(
    website: String,
    onClick: () -> Unit,
) {
    IconText(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        text = website,
        style = AppTheme.typography.bodySmall,
        color = AppTheme.colorScheme.primary,
        leadingIcon = PrimalIcons.Link,
        leadingIconSize = 16.sp,
        leadingIconTintColor = AppTheme.extraColorScheme.onSurfaceVariantAlt4,
    )
}

@Composable
private fun UserPublicKey(
    pubkey: String,
    onCopyClick: (String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconText(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .wrapContentWidth(),
            text = pubkey.asEllipsizedNpub(),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.extraColorScheme.onSurfaceVariantAlt4,
            leadingIcon = PrimalIcons.Key,
            leadingIconSize = 16.sp,
        )


        Box(
            modifier = Modifier
                .size(20.dp)
                .clickable(
                    onClick = { onCopyClick(pubkey) },
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple()
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                imageVector = Icons.Outlined.ContentCopy,
                colorFilter = ColorFilter.tint(color = AppTheme.colorScheme.primary),
                contentDescription = null
            )

        }
    }
}