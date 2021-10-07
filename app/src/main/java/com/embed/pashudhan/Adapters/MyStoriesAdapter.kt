package com.embed.pashudhan.Adapters

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.embed.pashudhan.Activities.ViewMyStory
import com.embed.pashudhan.DataModels.StoryItem
import com.embed.pashudhan.R
import com.google.firebase.auth.FirebaseAuth


class MyStoriesAdapter(
    private val mStoryList: ArrayList<StoryItem>,
    private val mContext: Context,
    private val EventChangeListener: (() -> Unit)
) :
    RecyclerView.Adapter<MyStoriesAdapter.MyViewHolder>() {

    private lateinit var mSharedPref: SharedPreferences
    private lateinit var mUserUUID: String

    companion object {
        private val TAG = "MyStoriesAdapter==>"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemview =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.my_story_card_grid_item, parent, false)
        return MyViewHolder(itemview)

    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(mContext)
        mUserUUID = FirebaseAuth.getInstance().currentUser?.phoneNumber!!

        val storyItem: StoryItem = mStoryList[position]

        holder.storyDate.text = DateUtils.getRelativeTimeSpanString(
            storyItem.timestamp?.toLong()!! * 1000,
            System.currentTimeMillis(),
            DateUtils.SECOND_IN_MILLIS
        )

        Glide.with(mContext).load(storyItem.imageUri)
            .placeholder(R.drawable.download)
            .into(holder.storyImage)

        holder.storyView.setOnClickListener {
            val intent = Intent(mContext, ViewMyStory::class.java)
            intent.putExtra("storyID", storyItem.id)
            mContext.startActivity(intent)

        }
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemCount(): Int {
        return mStoryList.size
    }

    class MyViewHolder(itemview: View) : RecyclerView.ViewHolder(itemview) {
        val storyImage: ImageView = itemview.findViewById(R.id.storyImage)
        val storyDate: TextView = itemview.findViewById(R.id.storyDate)
        val storyView: View = itemview
    }

}


