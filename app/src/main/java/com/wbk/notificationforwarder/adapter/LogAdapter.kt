package com.wbk.notificationforwarder.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wbk.notificationforwarder.R
import com.wbk.notificationforwarder.utils.HistoryManager
import com.wbk.notificationforwarder.utils.HistoryModel

class LogAdapter(private var list: List<HistoryModel>) : RecyclerView.Adapter<LogAdapter.Holder>() {

    fun updateData(newList: List<HistoryModel>) {
        list = newList
        notifyDataSetChanged()
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAppName: TextView = view.findViewById(R.id.tvLogAppName)
        val tvTime: TextView = view.findViewById(R.id.tvLogTime)
        val tvMessage: TextView = view.findViewById(R.id.tvLogMessage)
        val tvStatus: TextView = view.findViewById(R.id.tvLogStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log_history, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = list[position]

        holder.tvAppName.text = item.appName
        holder.tvTime.text = HistoryManager.formatTime(item.timestamp)
        holder.tvMessage.text = item.message

        holder.tvStatus.text = item.status
        when (item.status) {
            "SUCCESS" -> holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")) // Hijau
            "FAILED" -> holder.tvStatus.setTextColor(Color.parseColor("#D32F2F")) // Merah
            else -> holder.tvStatus.setTextColor(Color.parseColor("#FFA000")) // Kuning (Filtered/Unknown)
        }
    }

    override fun getItemCount(): Int = list.size
}