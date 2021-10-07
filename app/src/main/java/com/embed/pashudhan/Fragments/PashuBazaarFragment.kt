package com.embed.pashudhan.Fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.embed.pashudhan.Activities.PashuSalesActivity
import com.embed.pashudhan.Adapters.BazaarAdapter
import com.embed.pashudhan.DataModels.Pashubazaar
import com.embed.pashudhan.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.fragment_pashubazaar.*


class PashuBazaarFragment : Fragment() {

    companion object {
        private var TAG = "PashuBazaarFragment==>"
    }


    private lateinit var mPashuSalesCTA: Button
    private lateinit var pashuFilter: Button
    private lateinit var mBazaarRecyclerView: RecyclerView
    private lateinit var mBazaarAdapter: BazaarAdapter
    private lateinit var mAnimalList: ArrayList<Pashubazaar>
    private lateinit var PashudhanDB: FirebaseFirestore
    private lateinit var mFirebaseAuth: FirebaseAuth
    private lateinit var mActivity: FragmentActivity
    private var filter: String? = null
    private lateinit var bazaarProgressLayout: LinearLayout
    private lateinit var bazaarDataLayout: RelativeLayout
    private lateinit var nullPlaceholder: LinearLayout


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pashubazaar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity = requireActivity()
        bazaarProgressLayout = view.findViewById(R.id.bazaar_progressLayout)
        bazaarDataLayout = view.findViewById(R.id.bazaarDataLayout)
        nullPlaceholder = view.findViewById(R.id.NullAnimalPlaceholder)
        bazaarProgressLayout.visibility = View.VISIBLE
        bazaarDataLayout.visibility = View.GONE
        loadFilter(view)
    }

    private fun loadFilter(view: View) {
        var animalTypeList = arrayListOf<String>()
        var db = FirebaseFirestore.getInstance()
        val collectionRef = db.collection("animalData")
        collectionRef.get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    animalTypeList.add(document.id)
                }
                bazaarProgressLayout.visibility = View.GONE
                bazaarDataLayout.visibility = View.VISIBLE
                loadLayout(view, animalTypeList)
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
    }

    private fun loadLayout(view: View, data: ArrayList<String>) {
        mBazaarRecyclerView = view.findViewById(R.id.pashuBazaar_recView)
        mBazaarRecyclerView.layoutManager = LinearLayoutManager(mActivity)

        mAnimalList = arrayListOf()


        mBazaarAdapter = BazaarAdapter(mAnimalList, view.context)
        mBazaarRecyclerView.adapter = mBazaarAdapter

        mPashuSalesCTA = view.findViewById(R.id.pashuSalesCTA)
        mPashuSalesCTA.setOnClickListener {
            val intent = Intent(requireContext(), PashuSalesActivity::class.java)
            startActivity(intent)
        }

        EventChangeListener(filter)

        pashuFilter = view.findViewById(R.id.pashuBazaar_filterBtn)
        pashuFilter.setOnClickListener {
            //Creating the instance of PopupMenu
            val popup = PopupMenu(mActivity, pashuFilter)
            //Inflating the Popup using xml file
            data.forEachIndexed { index, s ->
                popup.menu.add(Menu.NONE, index+1, Menu.NONE, s )
            }
            //registering popup with OnMenuItemClickListener
            popup.setOnMenuItemClickListener { item ->
                filter = item.title as String
                Toast.makeText(
                    mActivity,
                    "${mActivity.getString(R.string.pashuBazar_filterMessage1)} " +
                            "${item.title} " +
                            "${mActivity.getString(R.string.pashuBazar_filterMessage2)}",
                    Toast.LENGTH_SHORT
                ).show()
                pashuBazaar_clearFilterBtn.visibility = View.VISIBLE
                EventChangeListener(filter)
                true
            }

            popup.show() //showing popup menu
        }
        pashuBazaar_clearFilterBtn.setOnClickListener {
            EventChangeListener(null)
            Toast.makeText(
                mActivity,
                getString(R.string.pashuBazar_clearFilterMessage),
                Toast.LENGTH_SHORT
            ).show()
            pashuBazaar_clearFilterBtn.visibility = View.GONE
        }
    }

    private fun EventChangeListener(filter: String?=null) {
        mAnimalList.clear()
        PashudhanDB = FirebaseFirestore.getInstance()
        var pashuBazaarRef = PashudhanDB.collection("Pashubazaar")

        if(filter != null) {
            var pashuQuery = pashuBazaarRef.whereEqualTo("animalType", filter.trim())
            pashuQuery.addSnapshotListener(object : EventListener<QuerySnapshot> {
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
                            var doc = dc.document.toObject(Pashubazaar::class.java)
                            doc.id = dc.document.id
                            mAnimalList.add(doc)
                        }
                    }
                    mBazaarAdapter.notifyDataSetChanged()
                    if(mAnimalList.isEmpty()) {
                        NullAnimalPlaceholder.visibility = View.VISIBLE
                        bazaarDataLayout.visibility = View.GONE
                    }else {
                        NullAnimalPlaceholder.visibility = View.GONE
                        bazaarDataLayout.visibility = View.VISIBLE
                    }
                }
            })
        }else {
            pashuBazaarRef.addSnapshotListener(object : EventListener<QuerySnapshot> {
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
                            var doc = dc.document.toObject(Pashubazaar::class.java)
                            doc.id = dc.document.id
                            mAnimalList.add(doc)
                        }
                    }
                    mBazaarAdapter.notifyDataSetChanged()
                    if(mAnimalList.isEmpty()) {
                        nullPlaceholder.visibility = View.VISIBLE
                        bazaarDataLayout.visibility = View.GONE
                    }else {
                        nullPlaceholder.visibility = View.GONE
                        bazaarDataLayout.visibility = View.VISIBLE
                    }
                }
        })
            }



    }
}