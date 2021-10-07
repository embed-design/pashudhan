package com.embed.pashudhan.Fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.embed.pashudhan.Adapters.BlogsAdapter
import com.embed.pashudhan.DataModels.BlogItem
import com.embed.pashudhan.R
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.fragment_pashu_samachar.*

class PashuSamachar : Fragment() {


    companion object{
        val TAG = "Samachar==>"
    }

    private lateinit var PashudhanDB: FirebaseFirestore
    private lateinit var mBlogList: ArrayList<BlogItem>
    private lateinit var mBlogsAdapter: BlogsAdapter
    private lateinit var mActivity: FragmentActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pashu_samachar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity = requireActivity()
        samacharRV.layoutManager = GridLayoutManager(mActivity, 2)


        samacharRV.setHasFixedSize(true)
        mBlogList = arrayListOf()


        mBlogsAdapter = BlogsAdapter(mBlogList, view.context)
        samacharRV.adapter = mBlogsAdapter

        EventChangeListener()
    }

    private fun EventChangeListener() {
        samachar_loadDataProgressBar.visibility = View.VISIBLE
        samacharRV.visibility = View.GONE
        mBlogList.clear()
        PashudhanDB = FirebaseFirestore.getInstance()
        PashudhanDB.collection("blogs")
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
                            var newDoc = dc.document.toObject(BlogItem::class.java)
                            newDoc.id = dc.document.id
                            mBlogList.add(newDoc)
                        }
                    }
                    mBlogsAdapter.notifyDataSetChanged()
                    samachar_loadDataProgressBar.visibility = View.GONE
                    if(mBlogList.isEmpty()) {
                        NullBlogsPlaceholder.visibility = View.VISIBLE
                        samacharRV.visibility = View.GONE
                    }else {
                        NullBlogsPlaceholder.visibility = View.GONE
                        samacharRV.visibility = View.VISIBLE
                    }
                    samacharRV.visibility = View.VISIBLE
                }
            })
    }
}