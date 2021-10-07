package com.embed.pashudhan.Adapters

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.embed.pashudhan.DataModels.NotificationListData
import com.embed.pashudhan.R

class NotificationAdapter(
    private val mNotificationList: ArrayList<NotificationListData>,
    private val mContext: Context
) : RecyclerView.Adapter<NotificationAdapter.MyViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemview =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.notification_card, parent, false)
        return MyViewHolder(itemview)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val notification = mNotificationList[position]
        holder.title.text = notification.title
        holder.message.text = notification.message
        holder.time.text = DateUtils.getRelativeTimeSpanString(
            notification.timestamp?.toLong()!! * 1000,
            System.currentTimeMillis(),
            DateUtils.SECOND_IN_MILLIS
        )
    }

    override fun getItemCount(): Int {
        return mNotificationList.size
    }

    class MyViewHolder(itemview: View) : RecyclerView.ViewHolder(itemview) {
        val title: TextView = itemview.findViewById(R.id.notificationTitle)
        val message: TextView = itemview.findViewById(R.id.notificationMessage)
        val time: TextView = itemview.findViewById(R.id.notificationTime)
    }



}