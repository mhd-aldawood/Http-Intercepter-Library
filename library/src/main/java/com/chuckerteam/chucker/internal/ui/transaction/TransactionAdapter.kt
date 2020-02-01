package com.chuckerteam.chucker.internal.ui.transaction

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chuckerteam.chucker.R
import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import com.chuckerteam.chucker.internal.data.entity.HttpTransactionTuple
import java.text.DateFormat

internal class TransactionAdapter internal constructor(
    context: Context,
    private val listener: TransactionClickListListener?
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {
    private var transactions: List<HttpTransactionTuple> = arrayListOf()

    private val colorDefault: Int = ContextCompat.getColor(context, R.color.status_default)
    private val colorRequested: Int = ContextCompat.getColor(context, R.color.status_requested)
    private val colorError: Int = ContextCompat.getColor(context, R.color.status_error)
    private val color500: Int = ContextCompat.getColor(context, R.color.status_500)
    private val color400: Int = ContextCompat.getColor(context, R.color.status_400)
    private val color300: Int = ContextCompat.getColor(context, R.color.status_300)

    override fun getItemCount(): Int = transactions.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_transaction, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(transactions[position])

    fun setData(httpTransactions: List<HttpTransactionTuple>) {
        this.transactions = httpTransactions
        notifyDataSetChanged()
    }

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private val code: TextView = view.findViewById(R.id.code)
        private val path: TextView = view.findViewById(R.id.path)
        private val host: TextView = view.findViewById(R.id.host)
        private val start: TextView = view.findViewById(R.id.time_start)
        private val duration: TextView = view.findViewById(R.id.duration)
        private val size: TextView = view.findViewById(R.id.size)
        private val ssl: ImageView = view.findViewById(R.id.ssl)

        @SuppressLint("SetTextI18n")
        fun bind(transaction: HttpTransactionTuple) {
            path.text = "${transaction.method} ${transaction.path}"
            host.text = transaction.host
            start.text = DateFormat.getTimeInstance().format(transaction.requestDate)
            ssl.visibility = if (transaction.isSsl) View.VISIBLE else View.GONE
            if (transaction.status === HttpTransaction.Status.Complete) {
                code.text = transaction.responseCode.toString()
                duration.text = transaction.durationString
                size.text = transaction.totalSizeString
            } else {
                code.text = ""
                duration.text = ""
                size.text = ""
            }
            if (transaction.status === HttpTransaction.Status.Failed) {
                code.text = "!!!"
            }
            setStatusColor(this, transaction)
            view.setOnClickListener {
                listener?.onTransactionClick(transaction.id, adapterPosition)
            }
        }

        private fun setStatusColor(holder: ViewHolder, transaction: HttpTransactionTuple) {
            val color: Int = when {
                transaction.status === HttpTransaction.Status.Failed -> colorError
                transaction.status === HttpTransaction.Status.Requested -> colorRequested
                transaction.responseCode == null -> colorDefault
                transaction.responseCode!! >= SERVER_ERRORS -> color500
                transaction.responseCode!! >= CLIENT_ERRORS -> color400
                transaction.responseCode!! >= REDIRECTS -> color300
                else -> colorDefault
            }
            holder.code.setTextColor(color)
            holder.path.setTextColor(color)
        }
    }

    interface TransactionClickListListener {
        fun onTransactionClick(transactionId: Long, position: Int)
    }
}

private const val SERVER_ERRORS = 500
private const val CLIENT_ERRORS = 400
private const val REDIRECTS = 300
