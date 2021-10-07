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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.embed.pashudhan.Adapters.BazaarAdapter
import com.embed.pashudhan.Adapters.MyPostsAdapter
import com.embed.pashudhan.DataModels.Pashubazaar
import com.embed.pashudhan.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.fragment_user_uploaded_posts.*

class UserUploadedPosts : Fragment() {

    companion object {
        private var TAG = "UserUploadedPosts==>"
        fun newInstance(userID: String) =
            UserUploadedPosts().apply {
                arguments = Bundle().apply {
                    putString("userID", userID)
                }
            }
    }

    private lateinit var PashudhanDB: FirebaseFirestore
    private lateinit var mMyPashuList: ArrayList<Pashubazaar>
    private lateinit var mMyPostsRecyclerView: RecyclerView
    private lateinit var mMyPostsAdapter: MyPostsAdapter
    private lateinit var mBazaarAdapter: BazaarAdapter
    private lateinit var myPostsProgressBar: ProgressBar
    private lateinit var mActivity: FragmentActivity
    private lateinit var mUserUUID: String
    private lateinit var mNullFavouritesPlaceholder: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mUserUUID = arguments?.getString("userID")!!
        return inflater.inflate(R.layout.fragment_user_uploaded_posts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity = requireActivity()
        myPostsProgressBar = view.findViewById(R.id.yourPost_loadDataProgressBar)
        mMyPostsRecyclerView = view.findViewById(R.id.yourPostRV)
        mNullFavouritesPlaceholder = view.findViewById(R.id.NullPostsPlaceholder)
        myPostsProgressBar.visibility = View.VISIBLE
        mMyPostsRecyclerView.visibility = View.GONE
        if(mUserUUID != FirebaseAuth.getInstance().currentUser?.phoneNumber!!) {
            yourUploadsHeading.text = getString(R.string.userUploads)
        }
        loadData(view)
    }

    private fun loadData(view: View) {
        mMyPostsRecyclerView.layoutManager = LinearLayoutManager(mActivity)


        mMyPashuList = arrayListOf()
        mBazaarAdapter = BazaarAdapter(mMyPashuList, view.context)
        mMyPostsAdapter = MyPostsAdapter(mMyPashuList, view.context, ::EventChangeListener)
        if(mUserUUID != FirebaseAuth.getInstance().currentUser?.phoneNumber!!) {
            mMyPostsRecyclerView.adapter = mBazaarAdapter
        } else {
            mMyPostsRecyclerView.adapter = mMyPostsAdapter
        }



        EventChangeListener()
    }

    private fun EventChangeListener() {
        myPostsProgressBar.visibility = View.VISIBLE
        mMyPostsRecyclerView.visibility = View.GONE
        mMyPashuList.clear()
        PashudhanDB = FirebaseFirestore.getInstance()
        PashudhanDB.collection("Pashubazaar").whereEqualTo("user_uuid", mUserUUID)
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
                            var newDoc = dc.document.toObject(Pashubazaar::class.java)
                            newDoc.id = dc.document.id
                            mMyPashuList.add(newDoc)
                        }
                    }
                    mMyPostsAdapter.notifyDataSetChanged()
                    myPostsProgressBar.visibility = View.GONE
                    if(mMyPashuList.isEmpty()) {
                        mNullFavouritesPlaceholder.visibility = View.VISIBLE
                        mMyPostsRecyclerView.visibility = View.GONE
                    }else {
                        mNullFavouritesPlaceholder.visibility = View.GONE
                        mMyPostsRecyclerView.visibility = View.VISIBLE
                    }
                    mMyPostsRecyclerView.visibility = View.VISIBLE
                }
            })
    }
}