package com.embed.pashudhan.Activities

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.embed.pashudhan.Adapters.*
import com.embed.pashudhan.DataModels.*
import com.embed.pashudhan.Helper
import com.embed.pashudhan.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.pashu_story_activity_layout.*
import java.util.*


class PashuStoryActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "PashuStoryActivity==>"
    }

    private lateinit var mStoryPageAdater: StoryPagerAdapter
    private lateinit var mStoryPageView: ViewPager2
    private lateinit var mCameraBtn: ImageButton
    private lateinit var mBackButton: ImageButton
    private lateinit var PashudhanDB: FirebaseFirestore
    private lateinit var mStoriesList: ArrayList<StoryItem>
    private lateinit var mUserUUID: String
    private var helper = Helper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pashu_story_activity_layout)

        val checkLoginSharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        mUserUUID = FirebaseAuth.getInstance().currentUser?.phoneNumber!!
        var locale = checkLoginSharedPref.getString(getString(R.string.sp_locale), "mr")!!
        helper.changeAppLanguage(this, locale)
        mStoryPageView = findViewById(R.id.storiesHolderViewPager)
        mStoriesList = arrayListOf()
        mStoryPageAdater =
            StoryPagerAdapter(
                this@PashuStoryActivity,
                this,
                mStoriesList,
                this::setItem,
                this::loadProfile
            )

        mStoryPageView.adapter = mStoryPageAdater
        mStoryPageView.orientation = ViewPager2.ORIENTATION_VERTICAL

        mBackButton = findViewById(R.id.pashuStory_AppBarReturnBtn)
        mBackButton.setOnClickListener {
            this.onBackPressed()
        }

        mCameraBtn = findViewById(R.id.cameraBtn)
        mCameraBtn.setOnClickListener {
            val intent = Intent(this, AddStoryActivity::class.java)
            startActivity(intent)
            finish()
        }

        EventChangeListener()
    }

    private fun loadProfile(userUuid: String) {
        val intent = Intent(this, BottomNavigationActivity::class.java)
        intent.putExtra("userID", userUuid)
        intent.putExtra("fragment", "profile")
        startActivity(intent)
    }

    fun setItem(i: Int) {
        mStoryPageView.currentItem = i
    }

    private fun EventChangeListener() {
        PashudhanDB = FirebaseFirestore.getInstance()
        PashudhanDB.collection("Stories").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener(object : EventListener<QuerySnapshot> {
                override fun onEvent(
                    value: QuerySnapshot?,
                    error: FirebaseFirestoreException?
                ) {
                    if (error != null) {
                        FirebaseCrashlytics.getInstance().recordException(error)
                        return
                    }

                    for (dc: DocumentChange in value?.documentChanges!!) {
                        if (dc.type == DocumentChange.Type.ADDED) {
                            var newDoc = dc.document.toObject(StoryItem::class.java)
                            newDoc.id = dc.document.id
                            mStoriesList.add(newDoc)
                        }
                    }
                    mStoryPageAdater.notifyDataSetChanged()
                    if(mStoriesList.isEmpty()) {
                        NullStoriesPlaceholder.visibility = View.VISIBLE
                        mStoryPageView.visibility = View.GONE
                    }else {
                        NullStoriesPlaceholder.visibility = View.GONE
                        mStoryPageView.visibility = View.VISIBLE
                    }
                }
            })
    }


}


