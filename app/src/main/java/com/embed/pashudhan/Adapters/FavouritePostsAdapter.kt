package com.embed.pashudhan.Adapters

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Location.distanceBetween
import android.net.Uri
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.embed.pashudhan.Activities.PashuDetailsActivity
import com.embed.pashudhan.BitmapUtils
import com.embed.pashudhan.DataModels.Pashubazaar
import com.embed.pashudhan.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class FavouritePostsAdapter(
    private val mAnimalList: ArrayList<Pashubazaar>,
    private val mContext: Context,
    private val EventChangeListener: (() -> Unit)
) :
    RecyclerView.Adapter<FavouritePostsAdapter.MyViewHolder>() {

    private lateinit var mViewPagerAdapter: PashuBazaarFavPostPagerAdapter
    private lateinit var mSharedPref: SharedPreferences
    private lateinit var mUserLatitude: String
    private lateinit var mUserLongitude: String
    private lateinit var mUserUUID: String

    companion object {
        private val TAG = "BazaarAdapter==>"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {

        val itemview =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.pashubazar_card_item, parent, false)
        return MyViewHolder(itemview)

    }



    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        fun getItem(i: Int): Int {
            return holder.animalImages.currentItem + i
        }

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(mContext)
        mUserUUID = FirebaseAuth.getInstance().currentUser?.phoneNumber!!
        var firstName = mSharedPref.getString(mContext.getString(R.string.sp_userFirstName), "0")
        var city = mSharedPref.getString(mContext.getString(R.string.sp_userDistrict), "0")
        mUserLatitude = mSharedPref.getString(mContext.getString(R.string.sp_userLatitude), "0").toString()
        mUserLongitude = mSharedPref.getString(mContext.getString(R.string.sp_userLongitude), "0").toString()

        val animalData: Pashubazaar = mAnimalList[position]
        holder.animalTypeBreed.text = "${animalData.animalType}, ${animalData.animalBreed}"
        holder.animalPrice.text = "${animalData.animalPrice}"

        holder.animalDistance.text = getDistance(animalData)

        var imageUriList = arrayListOf<Uri>()
        for (item in animalData.animalImages!!) {
            imageUriList.add(Uri.parse(item))
        }
        if(animalData.favouritesOf == null) {
            holder.addToFavBtn.setImageDrawable(holder.itemView.resources.getDrawable(R.drawable.ic_favourite_outline))
        }else {
            if (isFavourite(animalData.favouritesOf!!)) {
                holder.addToFavBtn.setImageDrawable(holder.itemView.resources.getDrawable(R.drawable.ic_favourite_filled))
            } else {
                holder.addToFavBtn.setImageDrawable(holder.itemView.resources.getDrawable(R.drawable.ic_favourite_outline))
            }
        }


        mViewPagerAdapter = PashuBazaarFavPostPagerAdapter(holder, imageUriList)
        holder.animalImages.isUserInputEnabled = false
        holder.animalImages.adapter = mViewPagerAdapter
        holder.nextImageButton.setOnClickListener {
            holder.animalImages.setCurrentItem(getItem(+1), true)
        }
        holder.prevImageButton.setOnClickListener {
            holder.animalImages.setCurrentItem(getItem(-1), true)
        }

        holder.whatsappShareBtn.setOnClickListener {
            sendToWhatsapp(imageUriList.get(0), animalData.user_uuid!!, firstName!!, city!!, animalData.animalType!!)
        }

        holder.view.setOnClickListener {
            val intent = Intent(mContext, PashuDetailsActivity::class.java)
            intent.putExtra("animalDocId", animalData.id)
            mContext.startActivity(intent)
        }

        holder.addToFavBtn.setOnClickListener {
            addToFavorites(holder)
        }
    }

    private fun isFavourite(favouriteList: ArrayList<String>) : Boolean{
        return favouriteList.contains(mUserUUID)
    }

    private fun addToFavorites(holder: MyViewHolder) {
        val currentPosition = holder.itemViewType
        val currentPashuItem = mAnimalList.get(currentPosition)
        val postRef = FirebaseFirestore.getInstance().collection("Pashubazaar").document(mAnimalList.get(currentPosition).id!!)
        if(currentPashuItem.favouritesOf != null) {
            if (isFavourite(currentPashuItem.favouritesOf!!)) {
                currentPashuItem.favouritesOf?.remove(mUserUUID)
            } else {
                currentPashuItem.favouritesOf?.add(mUserUUID)
            }
        }else {
            currentPashuItem.favouritesOf = arrayListOf(mUserUUID)
        }
        postRef.update("favouritesOf", currentPashuItem.favouritesOf).addOnSuccessListener {
            if(currentPashuItem.favouritesOf == null) {
                holder.addToFavBtn.setImageDrawable(holder.itemView.resources.getDrawable(R.drawable.ic_favourite_filled))
            } else {
                if (isFavourite(currentPashuItem.favouritesOf!!)) {
                    holder.addToFavBtn.setImageDrawable(holder.itemView.resources.getDrawable(R.drawable.ic_favourite_filled))
                }else {
                    holder.addToFavBtn.setImageDrawable(holder.itemView.resources.getDrawable(R.drawable.ic_favourite_outline))
                }
            }
            EventChangeListener()
        }
    }

    private fun getDistance(animalData: Pashubazaar): String? {
        var results = FloatArray(1)
        distanceBetween(
            mUserLatitude.toDouble(),
            mUserLongitude.toDouble(),
            animalData.location?.get(0)!!.toDouble(),
            animalData.location?.get(1)!!.toDouble(),
            results
        )
        return "${results[0].toInt() / 1000} ${mContext.getString(R.string.distanceString)}"
    }


    fun sendToWhatsapp(imgUri: Uri, sendTo: String, firstName: String, city: String, animalType: String){
        Glide.with(mContext)
            .asBitmap()
            .load(imgUri)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    var bmpUtils = BitmapUtils()
                    var cBitmap = bmpUtils.addWatermark(mContext, resource)
                    val imgUri = BitmapUtils().getImageUri(mContext, cBitmap)
                    val whatsappIntent = Intent(Intent.ACTION_SEND)
                    var sendToNumber = sendTo.drop(1)
                    whatsappIntent.setPackage("com.whatsapp")
                    whatsappIntent.putExtra("jid", sendToNumber + "@s.whatsapp.net")
                    whatsappIntent.type = "text/plain"
                    whatsappIntent.putExtra(Intent.EXTRA_TEXT, "नमस्ते जी, मैं $firstName हूँ- $city से। आपकी ये $animalType मैंने ${mContext.getString(R.string.app_name)} पर देखी और मुझे पसंद आई। क्या ये अभी बिकाऊ है?")
                    whatsappIntent.putExtra(Intent.EXTRA_STREAM, imgUri)
                    whatsappIntent.type = "image/jpeg"
                    whatsappIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    try {
                        mContext.startActivity(whatsappIntent)
                    } catch (ex: ActivityNotFoundException) {

                        Toast.makeText(mContext, "Whatsapp have not been installed.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // this is called when imageView is cleared on lifecycle call or for
                    // some other reason.
                    // if you are referencing the bitmap somewhere else too other than this imageView
                    // clear it here as you can no longer have the bitmap
                }
            })


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
        val animalDistance: TextView = itemview.findViewById(R.id.pashuBazaar_cardDistance)
        val nextImageButton: FloatingActionButton =
            itemview.findViewById(R.id.pashubazaar_cardNextImageButton)
        val prevImageButton: FloatingActionButton =
            itemview.findViewById(R.id.pashubazaar_cardPrevImageButton)
        val whatsappShareBtn: ImageButton = itemview.findViewById(R.id.pashuBazaar_shareWhatsapp)
        val addToFavBtn: ImageButton = itemview.findViewById(R.id.pashuBazaar_addToFav)
        val view = itemview

    }

}


