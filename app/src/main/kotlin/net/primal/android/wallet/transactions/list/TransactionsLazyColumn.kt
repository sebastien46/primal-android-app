package net.primal.android.wallet.transactions.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import net.primal.android.R
import net.primal.android.core.compose.ListLoading
import net.primal.android.core.compose.isEmpty
import net.primal.android.theme.AppTheme

@ExperimentalFoundationApi
@Composable
fun TransactionsLazyColumn(
    modifier: Modifier,
    pagingItems: LazyPagingItems<TransactionDataUi>,
    listState: LazyListState,
    onProfileClick: (String) -> Unit,
    onTransactionClick: (String) -> Unit,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    val today = stringResource(id = R.string.wallet_transactions_today).lowercase()
    val yesterday = stringResource(id = R.string.wallet_transactions_yesterday).lowercase()
    val numberFormat = remember { NumberFormat.getNumberInstance() }
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = paddingValues,
    ) {
        if (header != null) {
            item(
                contentType = { "Header" },
            ) {
                header()
            }
        }

        pagingItems.itemSnapshotList.items.groupBy {
            it.txInstant.formatDay(
                todayTranslation = today,
                yesterdayTranslation = yesterday,
            )
        }.forEach { (day, dayTransactions) ->
            stickyHeader(
                key = day,
                contentType = "DayHeader",
            ) {
                TransactionsHeaderListItem(
                    modifier = Modifier
                        .background(color = AppTheme.colorScheme.surfaceVariant)
                        .fillMaxWidth(),
                    day = day,
                )
            }

            items(
                items = dayTransactions,
                key = { it.txId },
                contentType = { "Transaction" },
            ) {
                TransactionListItem(
                    data = it,
                    numberFormat = numberFormat,
                    onAvatarClick = onProfileClick,
                    onClick = onTransactionClick,
                )
            }
        }

        if (pagingItems.isEmpty()) {
            when (pagingItems.loadState.refresh) {
                LoadState.Loading -> {
                    item(contentType = "LoadingRefresh") {
                        ListLoading(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(bottom = 80.dp),
                        )
                    }
                }
                else -> Unit
            }
        }

        if (footer != null) {
            item(
                contentType = { "Footer" },
            ) {
                footer()
            }
        }
    }
}

private fun Instant.formatDay(todayTranslation: String, yesterdayTranslation: String): String {
    val zoneId = ZoneId.systemDefault()
    val zonedDateTime: ZonedDateTime = this.atZone(zoneId)
    val now = ZonedDateTime.now(zoneId)

    return if (now.toLocalDate() == zonedDateTime.toLocalDate()) {
        todayTranslation
    } else if (now.minusDays(1).toLocalDate() == zonedDateTime.toLocalDate()) {
        yesterdayTranslation
    } else {
        zonedDateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
}
