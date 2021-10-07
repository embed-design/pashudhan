package com.embed.pashudhan.Fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.embed.pashudhan.Adapters.NotificationAdapter
import com.embed.pashudhan.DataModels.NotificationListData
import com.embed.pashudhan.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*


class NotificationsFragment : Fragment() {

    companion object {
        const val TAG = "NotifFragment==>"
    }

    private lateinit var PashudhanDB: FirebaseFirestore
    private lateinit var mNotificationList: ArrayList<NotificationListData>
    private lateinit var mNotificationsRecyclerView: RecyclerView
    private lateinit var mNotificationsAdapter: NotificationAdapter
    private lateinit var mNotificationsProgressLayout: LinearLayout
    private lateinit var mActivity: FragmentActivity
    private lateinit var mUserUUID: String
    private lateinit var mNullNotificationPlaceholder: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity = requireActivity()
        mUserUUID = FirebaseAuth.getInstance().currentUser?.phoneNumber!!
        mNotificationsProgressLayout = view.findViewById(R.id.notification_progressLayout)
        mNotificationsRecyclerView = view.findViewById(R.id.notificationRecyclerView)
        mNullNotificationPlaceholder = view.findViewById(R.id.notificationNullPlaceholder)
        mNotificationsProgressLayout.visibility = View.VISIBLE
        mNotificationsRecyclerView.visibility = View.GONE

        loadDataView(view)
    }

    fun loadDataView(view: View) {
        mNotificationsRecyclerView.layoutManager = LinearLayoutManager(mActivity)


        mNotificationsRecyclerView.setHasFixedSize(true)
        mNotificationList = arrayListOf()


        mNotificationsAdapter = NotificationAdapter(mNotificationList, view.context)
        mNotificationsRecyclerView.adapter = mNotificationsAdapter

        EventChangeListener()
    }

    fun EventChangeListener() {
        mNotificationsProgressLayout.visibility = View.VISIBLE
        mNotificationsRecyclerView.visibility = View.GONE
        mNotificationList.clear()
        PashudhanDB = FirebaseFirestore.getInstance()
        PashudhanDB.collection("notifications").whereEqualTo("sentTo", mUserUUID).orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener(object : EventListener<QuerySnapshot> {
                @SuppressLint("NotifyDataSetChanged")
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
                            Log.d(TAG, dc.document.toString())
                            val newDoc = dc.document.toObject(NotificationListData::class.java)
                            newDoc.id = dc.document.id
                            mNotificationList.add(newDoc)
                        }
                    }
                    mNotificationsAdapter.notifyDataSetChanged()
                    mNotificationsProgressLayout.visibility = View.GONE
                    if(mNotificationList.isEmpty()) {
                        mNullNotificationPlaceholder.visibility = View.VISIBLE
                        mNotificationsRecyclerView.visibility = View.GONE
                    }else {
                        mNullNotificationPlaceholder.visibility = View.GONE
                        mNotificationsRecyclerView.visibility = View.VISIBLE
                    }
                }
            })
    }
}