package com.embed.pashudhan.Adapters

import android.content.Context
import android.text.format.DateUtils
import android.text.format.DateUtils.getRelativeTimeSpanString
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.embed.pashudhan.DataModels.CommentsData
import com.embed.pashudhan.DataModels.users
import com.embed.pashudhan.R
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore

class CommentsAdapter(
    ctx: Context, commentsList: ArrayList<CommentsData>, private val openProfile: (userID: String) -> Unit
) : RecyclerView.Adapter<CommentsAdapter.MyViewHolder>() {

    companion object {
        const val TAG = "CommentsAdapter==>"
    }

    private var mContext = ctx
    private var mCommentsList = commentsList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemview =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.comment_card, parent, false)
        return MyViewHolder(itemview)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        var comment = mCommentsList[position]
        holder.commentLoadingBar.visibility = View.VISIBLE
        holder.commentLayout.visibility = View.GONE

        holder.commentContent.text = comment.commentContent
        holder.commentTimestamp.text = getRelativeTimeSpanString(
            comment.timestamp?.toLong()!! * 1000,
            System.currentTimeMillis(),
            DateUtils.SECOND_IN_MILLIS
        )
        setProfileInformation(holder)
    }

    private fun setProfileInformation(holder: MyViewHolder) {
        val currentPosition = holder.itemViewType
        val currentComment = mCommentsList[currentPosition]
        var PashudhanDB = FirebaseFirestore.getInstance()
        val docRef = PashudhanDB.collection("users").document(currentComment.user_uuid!!)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    var doc = document.toObject(users::class.java)
                    holder.userFullName.text = "${doc?.firstName} ${doc?.lastName}"
                    Glide.with(mContext).load(doc?.profileThumbnail).placeholder(R.drawable.user_placeholder).circleCrop()
                        .into(holder.userProfileImage)
                    holder.commentLoadingBar.visibility = View.GONE
                    holder.commentLayout.visibility = View.VISIBLE

                    holder.userFullName.setOnClickListener {
                        openProfile(currentComment.user_uuid!!)
                    }

                    holder.userProfileImage.setOnClickListener {
                        openProfile(currentComment.user_uuid!!)
                    }
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                FirebaseCrashlytics.getInstance().recordException(exception)
            }
    }



    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemCount(): Int {
        return mCommentsList.size
    }

    class MyViewHolder(itemview: View) : RecyclerView.ViewHolder(itemview) {
        var commentLoadingBar: ProgressBar = itemview.findViewById(R.id.loadCommentProgress)
        var commentLayout: RelativeLayout = itemview.findViewById(R.id.commentLayout)
        var userProfileImage: ImageView = itemview.findViewById(R.id.user_profile_image_comment)
        var userFullName: TextView = itemview.findViewById(R.id.comment_user_fullname)
        var commentContent: TextView = itemview.findViewById(R.id.comment_content)
        var commentTimestamp: TextView = itemview.findViewById(R.id.comment_time_posted)
    }
}