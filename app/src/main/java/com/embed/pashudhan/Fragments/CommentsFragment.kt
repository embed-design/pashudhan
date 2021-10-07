package com.embed.pashudhan.Fragments

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.embed.pashudhan.Activities.BottomNavigationActivity
import com.embed.pashudhan.Adapters.CommentsAdapter
import com.embed.pashudhan.DataModels.CommentsData
import com.embed.pashudhan.DataModels.NotificationData
import com.embed.pashudhan.DataModels.StoryItem
import com.embed.pashudhan.Helper
import com.embed.pashudhan.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_comments.*


class CommentsFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "CommentsFragment==>"
        fun newInstance(docId: String,  changeComments: (ArrayList<CommentsData>) -> Unit) =
            CommentsFragment().apply {
                this.mChangeComments = changeComments
                arguments = Bundle().apply {
                    putString("docId", docId)
                }
            }
    }
    private lateinit var mChangeComments: (ArrayList<CommentsData>) -> Unit
    private lateinit var sendCommentButton: Button
    private lateinit var commentEditText: EditText
    private lateinit var commentRV: RecyclerView
    private lateinit var commentRVAdapter: CommentsAdapter
    private lateinit var PashudhanDB: FirebaseFirestore
    private lateinit var mDocId: String
    private lateinit var mSharedPref: SharedPreferences
    private lateinit var mProfileImage: String
    private lateinit var mFirstName: String
    private lateinit var mLastName: String
    private lateinit var mUserUUID: String
    private lateinit var mBottomSheet: RelativeLayout
    private lateinit var loadingLayout: LinearLayout
    private lateinit var dataLayout: RelativeLayout
    private lateinit var commentList: ArrayList<CommentsData>
    private lateinit var storyItem: StoryItem
    private lateinit var mActivity: FragmentActivity
    private lateinit var nullPlaceholder: LinearLayout
    private var helper = Helper()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mDocId = arguments?.getString("docId")!!
        return inflater.inflate(R.layout.fragment_comments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity = requireActivity()
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(view.context)
        mProfileImage = mSharedPref.getString(getString(R.string.sp_profileImage), "")!!
        mFirstName = mSharedPref.getString(getString(R.string.sp_userFirstName), "")!!
        mLastName = mSharedPref.getString(getString(R.string.sp_userLastName), "")!!
        mUserUUID = mSharedPref.getString(getString(R.string.sp_loginUserUUID), "")!!

        PashudhanDB = FirebaseFirestore.getInstance()

        loadingLayout = view.findViewById(R.id.commentsProgressLayout)
        dataLayout = view.findViewById(R.id.commentsDataLayout)
        nullPlaceholder = view.findViewById(R.id.nullCommentsPlaceholder)

        loadingLayout.visibility = View.VISIBLE
        dataLayout.visibility = View.GONE

        PashudhanDB.collection("Stories").document(mDocId).get()
        .addOnSuccessListener { result ->
            loadingLayout.visibility = View.GONE
            dataLayout.visibility = View.VISIBLE
            storyItem = result.toObject(StoryItem::class.java)!!
            loadLayout(view)
        }
        .addOnFailureListener { exception ->
            Log.d(TAG, "get failed with ", exception)
        }
    }

    private fun loadLayout(view: View) {
        sendCommentButton = view.findViewById(R.id.submitStoryCommentButton)
        commentEditText = view.findViewById(R.id.commentsEditText)
        mBottomSheet = view.findViewById(R.id.comments_bottom_sheet)

        commentList = storyItem.comments!!
        commentList.sortByDescending {
            it.timestamp?.toDouble()
        }
        commentRV = view.findViewById(R.id.comments_rv)
        commentRV.layoutManager = LinearLayoutManager(view.context)
        commentRVAdapter = CommentsAdapter(view.context, commentList, ::loadProfile)
        commentRV.adapter = commentRVAdapter

        if(commentList.size > 0) {
            commentRV.visibility = View.VISIBLE
            nullPlaceholder.visibility = View.GONE

        } else {
            commentRV.visibility = View.GONE
            nullPlaceholder.visibility = View.VISIBLE
        }



        sendCommentButton.setOnClickListener {
            sendComment()
        }

        val mDividerItemDecoration = DividerItemDecoration(
            commentRV.context,
            (commentRV.layoutManager as LinearLayoutManager).orientation
        )
        commentRV.addItemDecoration(mDividerItemDecoration)

        commentsCloseButton.setOnClickListener {
            this.dismiss()
        }
    }

    private fun sendComment() {
        var commentRef = PashudhanDB.collection("Stories").document(mDocId)

        var commentBody = hashMapOf(
            "profileImage" to mProfileImage,
            "firstName" to mFirstName,
            "lastName" to mLastName,
            "commentContent" to commentEditText.text.toString(),
            "timestamp" to "${System.currentTimeMillis() / 1000}",
            "user_uuid" to mUserUUID
        )

        commentRef.update("comments", FieldValue.arrayUnion(commentBody)).addOnSuccessListener {
            commentList.add(CommentsData.from(commentBody))
            commentRVAdapter.notifyDataSetChanged()
            if(commentList.size > 0) {
                commentRV.visibility = View.VISIBLE
                nullPlaceholder.visibility = View.GONE
            }

            mChangeComments(commentList)
            commentEditText.text.clear()

            if(mUserUUID != storyItem.mUserUUID){
                helper.prepareNotification(
                    NotificationData(
                        getString(R.string.storyCommentTitle),
                        "$mFirstName $mLastName ${getString(R.string.storyCommentMessage)}"
                    ),
                    storyItem.mUserUUID!!
                )
            }
        }
    }

    private fun loadProfile(userUuid: String) {
        val intent = Intent(this.context, BottomNavigationActivity::class.java)
        intent.putExtra("userID", userUuid)
        intent.putExtra("fragment", "profile")
        startActivity(intent)
    }

}