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
import com.embed.pashudhan.Adapters.FavouritePostsAdapter
import com.embed.pashudhan.DataModels.Pashubazaar
import com.embed.pashudhan.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*


class UserFavouritePosts : Fragment() {

    companion object {
        private var TAG = "UserFavouritePosts==>"
    }

    private lateinit var PashudhanDB: FirebaseFirestore
    private lateinit var mFavPashuList: ArrayList<Pashubazaar>
    private lateinit var mFavouritePostsRecyclerView: RecyclerView
    private lateinit var mFavouritePostsAdapter: FavouritePostsAdapter
    private lateinit var favPostsProgressBar: ProgressBar
    private lateinit var mActivity: FragmentActivity
    private lateinit var mUserUUID: String
    private lateinit var mNullFavouritesPlaceholder: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_favourite_posts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity = requireActivity()
        mUserUUID = FirebaseAuth.getInstance().currentUser?.phoneNumber!!
        favPostsProgressBar = view.findViewById(R.id.favPost_loadDataProgressBar)
        mFavouritePostsRecyclerView = view.findViewById(R.id.favouritePostRV)
        mNullFavouritesPlaceholder = view.findViewById(R.id.NullFavouritesPlaceholder)
        favPostsProgressBar.visibility = View.VISIBLE
        mFavouritePostsRecyclerView.visibility = View.GONE

        loadData(view)


    }

    private fun loadData(view: View) {
        mFavouritePostsRecyclerView.layoutManager = LinearLayoutManager(mActivity)


        mFavPashuList = arrayListOf()


        mFavouritePostsAdapter = FavouritePostsAdapter(mFavPashuList, view.context, ::EventChangeListener)
        mFavouritePostsRecyclerView.adapter = mFavouritePostsAdapter

        EventChangeListener()
    }

    private fun EventChangeListener() {
        favPostsProgressBar.visibility = View.VISIBLE
        mFavouritePostsRecyclerView.visibility = View.GONE
        mFavPashuList.clear()
        PashudhanDB = FirebaseFirestore.getInstance()
        PashudhanDB.collection("Pashubazaar").whereArrayContains("favouritesOf", mUserUUID)
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
                            mFavPashuList.add(newDoc)
                        }
                    }
                    mFavouritePostsAdapter.notifyDataSetChanged()
                    favPostsProgressBar.visibility = View.GONE
                    if(mFavPashuList.isEmpty()) {
                        mNullFavouritesPlaceholder.visibility = View.VISIBLE
                        mFavouritePostsRecyclerView.visibility = View.GONE
                    }else {
                        mNullFavouritesPlaceholder.visibility = View.GONE
                        mFavouritePostsRecyclerView.visibility = View.VISIBLE
                    }
                    mFavouritePostsRecyclerView.visibility = View.VISIBLE
                }
            })
    }
}