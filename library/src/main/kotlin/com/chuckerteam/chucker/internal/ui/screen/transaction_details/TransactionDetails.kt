package com.chuckerteam.chucker.internal.ui.screen.transaction_details

import android.content.Context
import android.text.SpannableStringBuilder
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.text.HtmlCompat
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chuckerteam.chucker.R
import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import com.chuckerteam.chucker.internal.support.calculateLuminance
import com.chuckerteam.chucker.internal.support.combineLatest
import com.chuckerteam.chucker.internal.ui.transaction.PayloadType
import com.chuckerteam.chucker.internal.ui.transaction.TransactionPayloadItem
import com.chuckerteam.chucker.internal.ui.transaction.TransactionViewModel
import com.chuckerteam.chucker.internal.ui.transaction.TransactionViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionDetails(
    transactionId: Long
) {
    val context = LocalContext.current
    val viewmodel =
        viewModel<TransactionViewModel>(factory = TransactionViewModelFactory(transactionId = transactionId))
    val scope = rememberCoroutineScope()
    val horizontalState = rememberPagerState {
        3
    }
    val transactionsData = viewmodel.transaction.asFlow().collectAsState(initial = null)
    val payloadItems = remember {
        mutableStateListOf<TransactionPayloadItem>()
    }
    LaunchedEffect(key1 = horizontalState.currentPage, block = {
        Log.d("testrohit", "TransactionDetails: current: ${horizontalState.currentPage}")
        Log.d("testrohit", "TransactionDetails: target: ${horizontalState.targetPage}")
        when (horizontalState.currentPage) {
            1 -> {
                val items = processPayload(type = PayloadType.REQUEST, transactionsData.value, true, context)
                payloadItems.clear()
                payloadItems.addAll(
                    items
                )
            }
            2 -> {
                val items = processPayload(type = PayloadType.RESPONSE, transactionsData.value, true, context)
                payloadItems.clear()
                payloadItems.addAll(
                    items
                )
            }
        }
    })

    val titles = arrayOf(
        context.getString(R.string.chucker_overview),
        context.getString(R.string.chucker_request),
        context.getString(R.string.chucker_response)
    )
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(text = titles[horizontalState.currentPage])
            })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = horizontalState.currentPage) {
                titles.forEachIndexed { index, s ->
                    Tab(selected = index == horizontalState.currentPage, onClick = {
                        scope.launch {
                            horizontalState.animateScrollToPage(index)
                        }
                    }) {
                        Text(text = s)
                    }
                }
            }
            HorizontalPager(
                state = horizontalState,
                userScrollEnabled = true,
            ) { page: Int ->
                when (page) {
                    1, 2 -> {
                        LazyColumn (modifier = Modifier.fillMaxSize()) {
                            items(payloadItems) {
                                TransactionPayloadBody(transactionPayloadItem = it)
                            }
                        }
                    }
                    else -> {
                        TransactionOverview(
                            httpTransaction = transactionsData.value,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

            }


        }
    }

}

private suspend fun processPayload(
    type: PayloadType,
    transaction: HttpTransaction?,
    formatRequestBody: Boolean,
    context: Context
): MutableList<TransactionPayloadItem> {
    if (transaction == null) {
        return mutableListOf()
    }
    return withContext(Dispatchers.Default) {
        val result = mutableListOf<TransactionPayloadItem>()
        val headersString: String
        val isBodyEncoded: Boolean
        val bodyString: CharSequence

        if (type == PayloadType.REQUEST) {
            headersString = transaction.getRequestHeadersString(true)
            isBodyEncoded = transaction.isRequestBodyEncoded
            bodyString = if (formatRequestBody) {
                transaction.getSpannedRequestBody(context)
            } else {
                transaction.requestBody ?: ""
            }
        } else {
            headersString = transaction.getResponseHeadersString(true)
            isBodyEncoded = transaction.isResponseBodyEncoded
            bodyString = transaction.getSpannedResponseBody(context)
        }
        if (headersString.isNotBlank()) {
            result.add(
                TransactionPayloadItem.HeaderItem(
                    HtmlCompat.fromHtml(
                        headersString,
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                )
            )
        }

        // The body could either be an image, plain text, decoded binary or not decoded binary.
        val responseBitmap = transaction.responseImageBitmap

        if (type == PayloadType.RESPONSE && responseBitmap != null) {
            val bitmapLuminance = responseBitmap.calculateLuminance()
            result.add(TransactionPayloadItem.ImageItem(responseBitmap, bitmapLuminance))
            return@withContext result
        }

        when {
            isBodyEncoded -> {
                val text = context.getString(R.string.chucker_body_omitted)
                result.add(TransactionPayloadItem.BodyLineItem(SpannableStringBuilder.valueOf(text)))
            }

            bodyString.isBlank() -> {
                val text = context.getString(R.string.chucker_body_empty)
                result.add(TransactionPayloadItem.BodyLineItem(SpannableStringBuilder.valueOf(text)))
            }

            else -> bodyString.lines().forEach {
                result.add(
                    TransactionPayloadItem.BodyLineItem(
                        SpannableStringBuilder.valueOf(it)
                    )
                )
            }
        }
        return@withContext result
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
internal fun TransactionDetailsPreview() {
    TransactionDetails(0L)
}
