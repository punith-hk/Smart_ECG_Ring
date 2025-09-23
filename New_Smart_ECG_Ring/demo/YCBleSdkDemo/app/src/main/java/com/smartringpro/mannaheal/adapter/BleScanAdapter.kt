package com.smartringpro.mannaheal.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.ui.activities.HomeActivity
import com.smartringpro.mannaheal.util.ConnectionPreferences
import com.yucheng.ycbtsdk.bean.ScanDeviceBean

class BleScanAdapter(
    private val context: Context,
    private val devices: MutableList<ScanDeviceBean>
) : RecyclerView.Adapter<BleScanAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.nameText)
        val macAddressText: TextView = view.findViewById(R.id.macAddressText)
        val rssiText: TextView = view.findViewById(R.id.rssiText)
        val bondStateText: TextView = view.findViewById(R.id.bondStateText)
        val bleImg: ImageView = view.findViewById(R.id.bleImg)
        val rssiImg: ImageView = view.findViewById(R.id.rssiImg)
        val connectBtn: Button = view.findViewById(R.id.connecBtn)
        val rawDataBtn: TextView = view.findViewById(R.id.rawDataBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.blescan_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]
        holder.nameText.text = device.deviceName ?: "N/A"
        holder.macAddressText.text = device.deviceMac ?: ""
        holder.rssiText.text = device.deviceRssi?.toString() ?: ""
        holder.bondStateText.text = "NOT BONDED"
        // Optionally set icons/images if needed
        holder.rawDataBtn.setOnClickListener {
            val intent = Intent(context, com.smartringpro.mannaheal.BleHelperActivity::class.java)
            context.startActivity(intent)
        }
        holder.connectBtn.setOnClickListener {
            ConnectionPreferences.saveConnectionState(
                context,
                false,
                device.deviceMac ?: "",
                device.deviceName ?: ""
            )

            val intent = Intent(context, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra("openDeviceFragment", true)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = devices.size

    fun addDevice(device: ScanDeviceBean) {
        devices.add(device)
        notifyItemInserted(devices.size - 1)
    }

    fun clearDevices() {
        devices.clear()
        notifyDataSetChanged()
    }
}
