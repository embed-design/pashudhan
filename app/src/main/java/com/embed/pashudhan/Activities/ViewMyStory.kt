package com.embed.pashudhan.Activities

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.format.DateUtils
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.embed.pashudhan.DataModels.CommentsData
import com.embed.pashudhan.DataModels.NotificationData
import com.embed.pashudhan.DataModels.StoryItem
import com.embed.pashudhan.Fragments.CommentsFragment
import com.embed.pashudhan.Helper
import com.embed.pashudhan.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_view_my_story.*

class ViewMyStory : AppCompatActivity() {

    private lateinit var storyID: String
    private lateinit var mUserUUID: String
    private lateinit var mUserFirstName: String
    private lateinit var mUserLastName: String
    private lateinit var mUserLatitude: String
    private lateinit var mUserLongitude: String
    private lateinit var PashudhanDB: FirebaseFirestore
    private var helper = Helper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_my_story)
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        storyID = intent.getStringExtra("storyID")!!
        mUserUUID = FirebaseAuth.getInstance().currentUser?.phoneNumber!!
        mUserFirstName = sharedPref.getString(getString(R.string.sp_userFirstName), "0").toString()
        mUserLastName = sharedPref.getString(getString(R.string.sp_userLastName), "0").toString()
        mUserLatitude = sharedPref.getString(getString(R.string.sp_userLatitude), "0").toString()
        mUserLongitude = sharedPref.getString(getString(R.string.sp_userLongitude), "0").toString()
        PashudhanDB = FirebaseFirestore.getInstance()
        myStory_ProgressLayout.visibility = View.VISIBLE
        myStoryDataLayout.visibility = View.GONE
        loadStory()
    }

    private fun loadStory() {
        PashudhanDB
            .collection("Stories")
            .document(storyID)
            .get()
            .addOnSuccessListener { result ->
                val story = result.toObject(StoryItem::class.java)!!
                loadLayout(story)
            }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun loadLayout(story: StoryItem) {
        myStory_ProgressLayout.visibility = View.GONE
        myStoryDataLayout.visibility = View.VISIBLE
        val myStoryRootLayout = findViewById<RelativeLayout>(R.id.my_story_root_layout)
        // Image
        Glide.with(this).load(story.imageUri)
            .placeholder(R.drawable.download)
            .into(myStory_image)

        // User's Full Name
        myStoryuserFullName.text = story.name

        // Time of Posting Story
        myStorytimeStoryText.text = DateUtils.getRelativeTimeSpanString(
            story.timestamp?.toLong()!! * 1000,
            System.currentTimeMillis(),
            DateUtils.SECOND_IN_MILLIS
        )

        // Distance of Story
        myStorylocationStoryText.text = getDistance(story)

        // Likes
        if (isLiked(story.likes!!)) {
            myStorylikeStoryButton.setImageDrawable(getDrawable(R.drawable.ic_favourite_filled))
        } else {
            myStorylikeStoryButton.setImageDrawable(getDrawable(R.drawable.ic_favourite_outline))
        }
        myStorylikeStoryCount.text = "${story.likes?.size}"
        myStorylikeStoryButton.setOnClickListener {
            setLike(story)
        }

        // Comments
        myStorycommentStoryCount.text = "${story.comments?.size}"
        fun setComments(comments: ArrayList<CommentsData>) {
            story.comments = comments
            myStorycommentStoryCount.text = "${story.comments?.size}"
        }
        myStorycommentStoryButton.setOnClickListener {
            CommentsFragment.newInstance(
                storyID,
                ::setComments
            ).show(supportFragmentManager, CommentsFragment.TAG)
        }

        // Back Button
        myStory_AppBarReturnBtn.setOnClickListener {
            this.onBackPressed()
        }

        deleteStoryBtn.setOnClickListener {
            FirebaseFirestore.getInstance().collection("Stories").document(storyID)
                .delete()
                .addOnSuccessListener {
                    this.onBackPressed()
                    finish()
                }
                .addOnFailureListener { helper.showSnackbar(
                    this,
                    myStoryRootLayout,
                    getString(R.string.tryAgainMessage),
                    helper.ERROR_STATE,
                ) }
        }
    }

    private fun isLiked(likesList: ArrayList<String>) : Boolean{
        return likesList.contains(mUserUUID)
    }

    private fun getDistance(story: StoryItem): String? {
        var results = FloatArray(1)
        Location.distanceBetween(
            mUserLatitude.toDouble(),
            mUserLongitude.toDouble(),
            story.location?.get(0)!!.toDouble(),
            story.location?.get(1)!!.toDouble(),
            results
        )
        return "${results[0].toInt() / 1000} ${getString(R.string.distanceString)}"
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setLike(story: StoryItem) {
        val storyRef = PashudhanDB.collection("Stories").document(storyID)
        if (isLiked(story.likes!!)) {
            story.likes?.remove(mUserUUID)
        }else {
            story.likes?.add(mUserUUID)
        }
        storyRef.update("likes", story.likes).addOnSuccessListener {
            if (isLiked(story.likes!!)) {
                myStorylikeStoryButton.setImageDrawable(getDrawable(R.drawable.ic_favourite_filled))
                if(mUserUUID != story.mUserUUID){
                    helper.prepareNotification(
                        NotificationData(
                            getString(R.string.storyLikedTitle),
                            "$mUserFirstName $mUserLastName ${getString(R.string.storyLikedMessage)}"
                        ),
                        story.mUserUUID!!
                    )
                }
            }else {
                myStorylikeStoryButton.setImageDrawable(getDrawable(R.drawable.ic_favourite_outline))
            }
            myStorylikeStoryCount.text = "${story.likes?.size}"
        }
    }
}