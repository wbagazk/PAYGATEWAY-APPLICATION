package com.wbk.notificationforwarder.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.wbk.notificationforwarder.R
import com.wbk.notificationforwarder.model.PaymentItem

class AppAdapter(private val list: List<PaymentItem>) : RecyclerView.Adapter<AppAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvAppName)
        val tvPackage: TextView = view.findViewById(R.id.tvAppPackage)
        val switchApp: SwitchMaterial = view.findViewById(R.id.switchApp)
        val imgIcon: ImageView = view.findViewById(R.id.imgAppIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_list, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = list[position]
        holder.tvName.text = item.appName
        holder.tvPackage.text = "Target: ${item.targetApp}"

        // Switch hanya indikator visual bahwa server memantau ini (read-only di sisi client)
        holder.switchApp.isChecked = item.isActive
        holder.switchApp.isEnabled = false

        // Note: Icon aplikasi kita biarkan default dulu agar tidak rumit load dari package manager
        holder.imgIcon.setImageResource(R.mipmap.ic_launcher)
    }

    override fun getItemCount(): Int = list.size
}