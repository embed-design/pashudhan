package com.embed.pashudhan.Activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.embed.pashudhan.BitmapUtils
import com.embed.pashudhan.DataModels.NotificationData
import com.embed.pashudhan.DataModels.Pashubazaar
import com.embed.pashudhan.Helper
import com.embed.pashudhan.R
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore

class PashuDetailsActivity : AppCompatActivity() {

    companion object {
        private var TAG = "PashuDetailsActivity==>"
    }

    private lateinit var loadingLayout: LinearLayout
    private lateinit var dataLayout: ScrollView
    private lateinit var mSharedPref: SharedPreferences
    private lateinit var mUserLatitude: String
    private lateinit var mUserLongitude: String
    private lateinit var mUserUUID: String
    private lateinit var mFirstName: String
    private lateinit var mLastName: String
    private var helper = Helper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pashu_details)

        loadingLayout = findViewById(R.id.pashuDetails_progressLayout)
        dataLayout = findViewById(R.id.pashuDetailsMainDataView)

        loadingLayout.visibility = View.VISIBLE
        dataLayout.visibility = View.GONE
        var animalDocId = intent.getStringExtra("animalDocId")
        var PashudhanDB = FirebaseFirestore.getInstance()
        val docRef = PashudhanDB.collection("Pashubazaar").document(animalDocId!!)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    var doc = document.toObject(Pashubazaar::class.java)
                    loadDataView(doc)
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                FirebaseCrashlytics.getInstance().recordException(exception)
            }

    }

    fun loadDataView(doc: Pashubazaar?) {
        loadingLayout.visibility = View.GONE
        dataLayout.visibility = View.VISIBLE

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        mFirstName = mSharedPref.getString(getString(R.string.sp_userFirstName), "0")!!
        mLastName = mSharedPref.getString(getString(R.string.sp_userLastName), "0")!!
        var city = mSharedPref.getString(getString(R.string.sp_userDistrict), "0")
        mUserUUID = mSharedPref.getString(getString(R.string.sp_loginUserUUID), "0")!!
        mUserLatitude = mSharedPref.getString(getString(R.string.sp_userLatitude), "0").toString()
        mUserLongitude = mSharedPref.getString(getString(R.string.sp_userLongitude), "0").toString()

        var heading = findViewById<TextView>(R.id.pashuDetailsHeading)
        heading.text = "${doc?.animalType}, ${doc?.animalBreed}"

        var distanceText = findViewById<TextView>(R.id.pashuBazaar_cardDistance)
        distanceText.text = doc?.let { getDistance(it) }

        var mainImage = findViewById<ImageView>(R.id.pashuDetails_mainImage)
        var imageListArray = arrayListOf<ImageView>()
        imageListArray.add(findViewById(R.id.animalImage1))
        imageListArray.add(findViewById(R.id.animalImage2))
        imageListArray.add(findViewById(R.id.animalImage3))
        imageListArray.add(findViewById(R.id.animalImage4))

        var imageList = doc?.animalImages!!

        for(i in 0..3) {
            imageListArray.get(i).visibility = View.GONE
        }

        Glide.with(this).load(imageList.get(0)).placeholder(R.drawable.download)
            .into(mainImage)

        for(i in 0 until imageList.size) {
            imageListArray.get(i).visibility = View.VISIBLE
            Glide.with(this).load(imageList.get(i)).placeholder(R.drawable.download)
                .into(imageListArray.get(i))
            imageListArray[i].setOnClickListener {
                Glide.with(this).load(imageList.get(i)).placeholder(R.drawable.download)
                    .into(mainImage)
            }
        }


        var priceText = findViewById<TextView>(R.id.detailsPriceText)
        priceText.text = "₹ ${doc.animalPrice}"

        var pashuTypeText = findViewById<TextView>(R.id.tablePashuTypeText)
        var breedText = findViewById<TextView>(R.id.tablePashuBreedText)
        var ageText = findViewById<TextView>(R.id.tablePashuAgeText)
        var byaatText = findViewById<TextView>(R.id.tablePashuByaatText)
        var milkQuantityText = findViewById<TextView>(R.id.tablePashuMilkQuantityText)
        var milkCapacityText = findViewById<TextView>(R.id.tablePashuMilkCapacityText)

        pashuTypeText.text = doc.animalType
        breedText.text = doc.animalBreed
        ageText.text = doc.animalAge
        byaatText.text = doc.animalByaat
        milkQuantityText.text = doc.animalMilkQuantity
        milkCapacityText.text = doc.animalMilkCapacity

        var ownerNameData = findViewById<TextView>(R.id.ownerNameData)
        var ownerPhoneData = findViewById<TextView>(R.id.ownerPhoneData)

        ownerNameData.text = doc.name
        ownerNameData.setOnClickListener {
            loadProfile(doc.user_uuid!!)
        }
        ownerPhoneData.text = doc.user_uuid

        var callButton = findViewById<Button>(R.id.callOwnerButton)
        var whatsappButton = findViewById<Button>(R.id.whatsappOwnerButton)

        callButton.setOnClickListener {
            val dialIntent = Intent(Intent.ACTION_DIAL)
            dialIntent.data = Uri.parse("tel:" + doc.user_uuid)
            startActivity(dialIntent)
        }
        whatsappButton.setOnClickListener {
            sendToWhatsapp(Uri.parse(imageList.get(0)), doc.user_uuid!!, mFirstName, city!!, doc.animalType!!)
        }
    }

    fun sendToWhatsapp(imgUri: Uri, sendTo: String, firstName: String, city: String, animalType: String){
        Glide.with(this)
            .asBitmap()
            .load(imgUri)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    var bmpUtils = BitmapUtils()
                    var cBitmap = bmpUtils.addWatermark(this@PashuDetailsActivity, resource)
                    val imgUri = BitmapUtils().getImageUri(this@PashuDetailsActivity, cBitmap)
                    val whatsappIntent = Intent(Intent.ACTION_SEND)
                    var sendToNumber = sendTo.drop(1)
                    whatsappIntent.setPackage("com.whatsapp")
                    whatsappIntent.putExtra("jid", sendToNumber + "@s.whatsapp.net")
                    whatsappIntent.type = "text/plain"
                    whatsappIntent.putExtra(Intent.EXTRA_TEXT, "नमस्ते जी, मैं $firstName हूँ- $city से। आपकी ये $animalType मैंने ${getString(R.string.app_name)} पर देखी और मुझे पसंद आई। क्या ये अभी बिकाऊ है?")
                    whatsappIntent.putExtra(Intent.EXTRA_STREAM, imgUri)
                    whatsappIntent.type = "image/jpeg"
                    whatsappIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    try {
                        startActivity(whatsappIntent)
                        if(mUserUUID != sendTo){
                            helper.prepareNotification(
                                NotificationData(
                                    getString(R.string.informationTitle),
                                    "$mFirstName $mLastName ${getString(R.string.informationMessage)}"
                                ),
                                sendTo
                            )
                        }
                    } catch (ex: ActivityNotFoundException) {
                        Toast.makeText(this@PashuDetailsActivity, "Whatsapp have not been installed.", Toast.LENGTH_SHORT).show()
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

    private fun getDistance(animalData: Pashubazaar): String? {
        var results = FloatArray(1)
        Location.distanceBetween(
            mUserLatitude.toDouble(),
            mUserLongitude.toDouble(),
            animalData.location?.get(0)!!.toDouble(),
            animalData.location?.get(1)!!.toDouble(),
            results
        )
        return "${results[0].toInt() / 1000} ${getString(R.string.distanceString)}"
    }

    private fun loadProfile(userUuid: String) {
        val intent = Intent(this, BottomNavigationActivity::class.java)
        intent.putExtra("userID", userUuid)
        intent.putExtra("fragment", "profile")
        startActivity(intent)
    }
}