package com.embed.pashudhan.Fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.embed.pashudhan.Adapters.MyStoriesAdapter
import com.embed.pashudhan.DataModels.StoryItem
import com.embed.pashudhan.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.fragment_user_uploaded_stories.*


class UserUploadedStories : Fragment() {

    companion object {
        private var TAG = "UserUploadedStories==>"
        fun newInstance(userID: String) =
            UserUploadedStories().apply {
                arguments = Bundle().apply {
                    putString("userID", userID)
                }
            }
    }

    private lateinit var PashudhanDB: FirebaseFirestore
    private lateinit var mMyStoriesList: ArrayList<StoryItem>
    private lateinit var mMyStoriesRecyclerView: RecyclerView
    private lateinit var mMyStoriesAdapter: MyStoriesAdapter
    private lateinit var myStoriesProgressBar: ProgressBar
    private lateinit var mActivity: FragmentActivity
    private lateinit var mUserUUID: String
    private lateinit var mNullStoriesPlaceholder: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mUserUUID = arguments?.getString("userID")!!
        return inflater.inflate(R.layout.fragment_user_uploaded_stories, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity = requireActivity()
        myStoriesProgressBar = view.findViewById(R.id.yourStories_loadDataProgressBar)
        mMyStoriesRecyclerView = view.findViewById(R.id.yourStoriesRV)
        mNullStoriesPlaceholder = view.findViewById(R.id.NullStoriesPlaceholder)
        myStoriesProgressBar.visibility = View.VISIBLE
        mMyStoriesRecyclerView.visibility = View.GONE
        if(mUserUUID != FirebaseAuth.getInstance().currentUser?.phoneNumber!!) {
            yourStoriesHeading.text = getString(R.string.userStories)
        }
        loadData(view)
    }

    private fun loadData(view: View) {
        mMyStoriesRecyclerView.layoutManager = GridLayoutManager(mActivity, 3)


        mMyStoriesList = arrayListOf()


        mMyStoriesAdapter = MyStoriesAdapter(mMyStoriesList, view.context, ::EventChangeListener)
        mMyStoriesRecyclerView.adapter = mMyStoriesAdapter

    }

    private fun EventChangeListener() {
        mMyStoriesList.clear()
        myStoriesProgressBar.visibility = View.VISIBLE
        mMyStoriesRecyclerView.visibility = View.GONE
        PashudhanDB = FirebaseFirestore.getInstance()
        PashudhanDB.collection("Stories").whereEqualTo("mUserUUID", mUserUUID)
            .addSnapshotListener(object : EventListener<QuerySnapshot> {
                override fun onEvent(
                    value: QuerySnapshot?,
                    error: FirebaseFirestoreException?
                ) {
                    if (error != null) {
                        Log.e("Firestore error", error.message.toString())
                        return
                    }

                    for (dc: DocumentChange in value?.documentChanges!!) {
                        if (dc.type == DocumentChange.Type.ADDED) {
                            var newDoc = dc.document.toObject(StoryItem::class.java)
                            newDoc.id = dc.document.id
                            mMyStoriesList.add(newDoc)
                        }
                    }
                    mMyStoriesAdapter.notifyDataSetChanged()
                    myStoriesProgressBar.visibility = View.GONE
                    if(mMyStoriesList.isEmpty()) {
                        mNullStoriesPlaceholder.visibility = View.VISIBLE
                        mMyStoriesRecyclerView.visibility = View.GONE
                    }else {
                        mNullStoriesPlaceholder.visibility = View.GONE
                        mMyStoriesRecyclerView.visibility = View.VISIBLE
                    }
                    mMyStoriesRecyclerView.visibility = View.VISIBLE
                }
            })
    }

    override fun onResume() {
        super.onResume()
        EventChangeListener()
    }
}