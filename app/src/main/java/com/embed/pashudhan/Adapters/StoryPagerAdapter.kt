package com.embed.pashudhan.Adapters


import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Location
import android.net.Uri
import android.preference.PreferenceManager
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.embed.pashudhan.BitmapUtils
import com.embed.pashudhan.DataModels.CommentsData
import com.embed.pashudhan.DataModels.NotificationData
import com.embed.pashudhan.DataModels.StoryItem
import com.embed.pashudhan.Fragments.CommentsFragment
import com.embed.pashudhan.Helper
import com.embed.pashudhan.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class StoryPagerAdapter(
    activity: AppCompatActivity,
    context: Context,
    stories: ArrayList<StoryItem>,
    changeStory: (Int) -> Unit,
    loadProfile: (String) -> Unit,
) :
    RecyclerView.Adapter<StoryPagerAdapter.StoryPagerViewHolder>() {

    companion object {
        private val TAG = "StoryAdapter==>"
    }

    private var mContext = context
    private var mActivity = activity
    private var mStoriesList = stories
    private lateinit var mUserUUID: String
    private lateinit var mUserFirstName: String
    private lateinit var mUserLastName: String
    private lateinit var mUserLatitude: String
    private lateinit var mUserLongitude: String
    private lateinit var mUserProfileImage: String
    private lateinit var PashudhanDB: FirebaseFirestore
    private var mChangeStory = changeStory
    private var mLoadProfile = loadProfile
    private lateinit var storyItem: StoryItem
    private var helper = Helper()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryPagerViewHolder {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(parent.context)
        mUserUUID = FirebaseAuth.getInstance().currentUser?.phoneNumber!!
        mUserFirstName =
            sharedPref.getString(parent.resources.getString(R.string.sp_userFirstName), "0")
                .toString()
        mUserLastName =
            sharedPref.getString(parent.resources.getString(R.string.sp_userLastName), "0")
                .toString()
        mUserProfileImage =
            sharedPref.getString(parent.resources.getString(R.string.sp_profileImage), "0")
                .toString()
        mUserLatitude = sharedPref.getString(mContext.getString(R.string.sp_userLatitude), "0").toString()
        mUserLongitude = sharedPref.getString(mContext.getString(R.string.sp_userLongitude), "0").toString()
        PashudhanDB = FirebaseFirestore.getInstance()


        var view: View =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.story_container_copy, parent, false)
        return StoryPagerViewHolder(view)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: StoryPagerViewHolder, position: Int) {
        holder.setIsRecyclable(false)
        storyItem = mStoriesList.get(position)
        Glide.with(mContext).load(storyItem.imageUri)
            .placeholder(R.drawable.download)
            .into(holder.singleStoryImageView)
        holder.userFullName.text = storyItem.name
        holder.userFullName.setOnClickListener {
            mLoadProfile(storyItem.mUserUUID!!)
        }
        holder.timeStoryText.text = DateUtils.getRelativeTimeSpanString(
            storyItem.timestamp?.toLong()!! * 1000,
            System.currentTimeMillis(),
            DateUtils.SECOND_IN_MILLIS
        )


        if (isLiked(storyItem.likes!!)) {
            holder.likeButton.setImageDrawable(holder.itemView.resources.getDrawable(R.drawable.ic_favourite_filled))
        } else {
            holder.likeButton.setImageDrawable(holder.itemView.resources.getDrawable(R.drawable.ic_favourite_outline))
        }
        holder.likeCountTextView.text = "${storyItem.likes?.size}"
        holder.commentTextView.text = "${storyItem.comments?.size}"
        holder.locationStoryText.text = getDistance(holder)
        holder.likeButton.setOnClickListener {
            setLike(holder)
        }
        holder.whatsappShareStory.setOnClickListener {
            shareStoryOnWhatsapp(holder)
        }

        holder.skipBtn.setOnClickListener {
            if (holder.itemViewType < (mStoriesList.size - 1)) {
                mChangeStory(holder.itemViewType + 1)
            }
        }
        holder.reverseBtn.setOnClickListener {
            if (holder.itemViewType != 0) {
                mChangeStory(holder.itemViewType - 1)
            }
        }
        fun setComments(comments: ArrayList<CommentsData>) {
            val currentPosition = holder.itemViewType
            val currentStoryItem = mStoriesList.get(currentPosition)
            currentStoryItem.comments = comments
            holder.commentTextView.text = "${currentStoryItem.comments?.size}"
        }

        holder.commentButton.setOnClickListener {
            CommentsFragment.newInstance(
                mStoriesList.get(holder.itemViewType).id!!,
                ::setComments
            ).show(mActivity.supportFragmentManager, CommentsFragment.TAG)
        }

    }

    private fun shareStoryOnWhatsapp(holder: StoryPagerViewHolder) {
        val currentPosition = holder.itemViewType
        val currentStoryItem = mStoriesList.get(currentPosition)
        sendToWhatsapp(currentStoryItem.imageUri?.toUri()!!)
    }

    fun sendToWhatsapp(imgUri: Uri){
        Glide.with(mContext)
            .asBitmap()
            .load(imgUri)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    var bmpUtils = BitmapUtils()
                    var cBitmap = bmpUtils.addWatermark(mContext, resource)
                    val imgUri = BitmapUtils().getImageUri(mContext, cBitmap)
                    val whatsappIntent = Intent(Intent.ACTION_SEND)
                    whatsappIntent.setPackage("com.whatsapp")
                    whatsappIntent.putExtra(Intent.EXTRA_STREAM, imgUri)
                    whatsappIntent.type = "image/jpeg"
                    whatsappIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    try {
                        mContext.startActivity(whatsappIntent)
                    } catch (ex: ActivityNotFoundException) {

                        Toast.makeText(mContext, "Whatsapp have not been installed.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // this is called when imageView is cleared on lifecycle call or for
                    // some other reason.
                    // if you are referencing the bitmap somewhere else too other than this imageView
                    // clear it here as you can no longer have the bitmap
                }
            })


    }

    private fun isLiked(likesList: ArrayList<String>) : Boolean{
        return likesList.contains(mUserUUID)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setLike(holder: StoryPagerViewHolder) {
        val currentPosition = holder.itemViewType
        val currentStoryItem = mStoriesList.get(currentPosition)
        val storyRef = PashudhanDB.collection("Stories").document(mStoriesList.get(currentPosition).id!!)
        if (isLiked(currentStoryItem.likes!!)) {
            currentStoryItem.likes?.remove(mUserUUID)
        }else {
            currentStoryItem.likes?.add(mUserUUID)
        }
        storyRef.update("likes", currentStoryItem.likes).addOnSuccessListener {
            if (isLiked(currentStoryItem.likes!!)) {
                holder.likeButton.setImageDrawable(holder.itemView.resources.getDrawable(R.drawable.ic_favourite_filled))
                if(mUserUUID != currentStoryItem.mUserUUID){
                    helper.prepareNotification(
                        NotificationData(
                            mContext.getString(R.string.storyLikedTitle),
                            "$mUserFirstName $mUserLastName ${mContext.getString(R.string.storyLikedMessage)}"
                        ),
                        currentStoryItem.mUserUUID!!
                    )
                }
            }else {
                holder.likeButton.setImageDrawable(holder.itemView.resources.getDrawable(R.drawable.ic_favourite_outline))
            }
            holder.likeCountTextView.text = "${currentStoryItem.likes?.size}"


        }
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemCount(): Int {
        return mStoriesList.size
    }

    private fun getDistance(holder: StoryPagerViewHolder): String? {
        var currentPosition = holder.itemViewType
        val currentStoryItem = mStoriesList.get(currentPosition)
        var results = FloatArray(1)
        Location.distanceBetween(
            mUserLatitude.toDouble(),
            mUserLongitude.toDouble(),
            currentStoryItem.location?.get(0)!!.toDouble(),
            currentStoryItem.location?.get(1)!!.toDouble(),
            results
        )
        return "${results[0].toInt() / 1000} ${mContext.getString(R.string.distanceString)}"
    }

    class StoryPagerViewHolder(itemview: View) : RecyclerView.ViewHolder(itemview) {
        val singleStoryImageView: ImageView = itemview.findViewById(R.id.story_image)
        val reverseBtn: View = itemview.findViewById(R.id.reverse)
        val skipBtn: View = itemview.findViewById(R.id.skip)
        val likeButton: ImageButton = itemview.findViewById(R.id.likeStoryButton)
        val likeCountTextView: TextView = itemview.findViewById(R.id.likeStoryCount)
        val userFullName: TextView = itemview.findViewById(R.id.userFullName)
        val timeStoryText: TextView = itemview.findViewById(R.id.timeStoryText)
        val locationStoryText: TextView = itemview.findViewById(R.id.locationStoryText)
        val commentButton: ImageButton = itemview.findViewById(R.id.commentStoryButton)
        val commentTextView: TextView = itemview.findViewById(R.id.commentStoryCount)
        val whatsappShareStory: ImageButton = itemview.findViewById(R.id.storyWhatsappShareButton)

    }



}


