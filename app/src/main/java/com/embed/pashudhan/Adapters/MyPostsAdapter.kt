package com.embed.pashudhan.Adapters

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.preference.PreferenceManager
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.embed.pashudhan.Activities.PashuDetailsActivity
import com.embed.pashudhan.DataModels.Pashubazaar
import com.embed.pashudhan.Helper
import com.embed.pashudhan.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class MyPostsAdapter(
    private val mAnimalList: ArrayList<Pashubazaar>,
    private val mContext: Context,
    private val EventChangeListener: (() -> Unit)
) :
    RecyclerView.Adapter<MyPostsAdapter.MyViewHolder>() {

    private lateinit var mViewPagerAdapter: PashuBazaarMyPostsPagerAdapter
    private lateinit var mSharedPref: SharedPreferences
    private lateinit var mUserLatitude: String
    private lateinit var mUserLongitude: String
    private lateinit var mUserUUID: String
    private var helper = Helper()

    companion object {
        private val TAG = "BazaarAdapter==>"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {

        val itemview =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.my_pashu_card_layout, parent, false)
        return MyViewHolder(itemview)

    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        fun getItem(i: Int): Int {
            return holder.animalImages.currentItem + i
        }

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(mContext)
        mUserUUID = FirebaseAuth.getInstance().currentUser?.phoneNumber!!
        mUserLatitude = mSharedPref.getString(mContext.getString(R.string.sp_userLatitude), "0").toString()
        mUserLongitude = mSharedPref.getString(mContext.getString(R.string.sp_userLongitude), "0").toString()

        val animalData: Pashubazaar = mAnimalList[position]
        holder.animalTypeBreed.text = "${animalData.animalType}, ${animalData.animalBreed}"
        holder.animalPrice.text = "${animalData.animalPrice}"

        holder.pashuDate.text = DateUtils.getRelativeTimeSpanString(
            animalData.timestamp?.toLong()!! * 1000,
            System.currentTimeMillis(),
            DateUtils.SECOND_IN_MILLIS
        )

        var imageUriList = arrayListOf<Uri>()
        for (item in animalData.animalImages!!) {
            imageUriList.add(Uri.parse(item))
        }

        mViewPagerAdapter = PashuBazaarMyPostsPagerAdapter(holder, imageUriList)
        holder.animalImages.isUserInputEnabled = false
        holder.animalImages.adapter = mViewPagerAdapter
        holder.nextImageButton.setOnClickListener {
            holder.animalImages.setCurrentItem(getItem(+1), true)
        }
        holder.prevImageButton.setOnClickListener {
            holder.animalImages.setCurrentItem(getItem(-1), true)
        }

        if(mUserUUID != FirebaseAuth.getInstance().currentUser?.phoneNumber!!) {
            holder.view.setOnClickListener {
                val intent = Intent(mContext, PashuDetailsActivity::class.java)
                intent.putExtra("animalDocId", animalData.id)
                mContext.startActivity(intent)
            }
        }
        val postsRootLayout = holder.view.findViewById<FrameLayout>(R.id.myPostsRootLayout)
        holder.deletePostBtn.setOnClickListener {
            FirebaseFirestore.getInstance().collection("Pashubazaar").document(animalData.id!!)
                .delete()
                .addOnSuccessListener {
                    EventChangeListener()
                }
                .addOnFailureListener { helper.showSnackbar(
                    holder.view.context,
                    postsRootLayout,
                    holder.view.context.getString(R.string.tryAgainMessage),
                    helper.ERROR_STATE,
                ) }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemCount(): Int {
        return mAnimalList.size
    }

    class MyViewHolder(itemview: View) : RecyclerView.ViewHolder(itemview) {
        val animalImages: ViewPager2 = itemview.findViewById(R.id.pashuBazaar_cardViewPager)
        val animalTypeBreed: TextView = itemview.findViewById(R.id.pashuBazaar_cardHeading)
        val animalPrice: TextView = itemview.findViewById(R.id.pashuBazaar_cardPrice)
        val pashuDate: TextView = itemview.findViewById(R.id.pashuBazaar_cardDate)
        val nextImageButton: FloatingActionButton =
            itemview.findViewById(R.id.pashubazaar_cardNextImageButton)
        val prevImageButton: FloatingActionButton =
            itemview.findViewById(R.id.pashubazaar_cardPrevImageButton)
        val view = itemview
        val deletePostBtn: ImageButton = itemview.findViewById(R.id.deletePostBtn)
    }

}


