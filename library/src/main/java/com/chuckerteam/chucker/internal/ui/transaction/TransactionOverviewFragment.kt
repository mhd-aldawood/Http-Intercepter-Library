package com.chuckerteam.chucker.internal.ui.transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.chuckerteam.chucker.R
import com.chuckerteam.chucker.internal.support.combineLatest

internal class TransactionOverviewFragment : Fragment() {

    private lateinit var url: TextView
    private lateinit var method: TextView
    private lateinit var protocol: TextView
    private lateinit var status: TextView
    private lateinit var response: TextView
    private lateinit var ssl: TextView
    private lateinit var requestTime: TextView
    private lateinit var responseTime: TextView
    private lateinit var duration: TextView
    private lateinit var requestSize: TextView
    private lateinit var responseSize: TextView
    private lateinit var totalSize: TextView
    private lateinit var viewModel: TransactionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel = ViewModelProvider(requireActivity())[TransactionViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.chucker_fragment_transaction_overview, container, false)
            .also {
                url = it.findViewById(R.id.url)
                method = it.findViewById(R.id.method)
                protocol = it.findViewById(R.id.protocol)
                status = it.findViewById(R.id.status)
                response = it.findViewById(R.id.response)
                ssl = it.findViewById(R.id.ssl)
                requestTime = it.findViewById(R.id.request_time)
                responseTime = it.findViewById(R.id.response_time)
                duration = it.findViewById(R.id.duration)
                requestSize = it.findViewById(R.id.request_size)
                responseSize = it.findViewById(R.id.response_size)
                totalSize = it.findViewById(R.id.total_size)
            }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.findItem(R.id.save_body).isVisible = false
        menu.findItem(R.id.encode_url).isVisible = true

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.transaction
            .combineLatest(viewModel.encodeUrl)
            .observe(
                viewLifecycleOwner,
                Observer { (transaction, encodeUrl) ->
                    url.text = transaction?.getFormattedUrl(encodeUrl)
                    method.text = transaction?.method
                    protocol.text = transaction?.protocol
                    status.text = transaction?.status?.toString()
                    response.text = transaction?.responseSummaryText
                    ssl.setText(if (transaction?.isSsl == true) R.string.chucker_yes else R.string.chucker_no)
                    requestTime.text = transaction?.requestDateString
                    responseTime.text = transaction?.responseDateString
                    duration.text = transaction?.durationString
                    requestSize.text = transaction?.requestSizeString
                    responseSize.text = transaction?.responseSizeString
                    totalSize.text = transaction?.totalSizeString
                }
            )
    }
}
