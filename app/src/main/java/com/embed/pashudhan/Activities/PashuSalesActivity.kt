package com.embed.pashudhan.Activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.embed.pashudhan.BitmapUtils
import com.embed.pashudhan.Fragments.PickImageFragment
import com.embed.pashudhan.Helper
import com.embed.pashudhan.R
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class PashuSalesActivity : AppCompatActivity() {

    companion object {
        private val TAG = "PashusalesActivity==>"
        private val PICK_IMAGE = 100
        private val CAPTURE_IMAGE = 101
    }

    private val PashudhanDB = Firebase.firestore
    private val PashudhanStorage = Firebase.storage
    private lateinit var mSelectedImage1: ImageView
    private lateinit var mSelectedImage2: ImageView
    private lateinit var mSelectedImage3: ImageView
    private lateinit var mSelectedImage4: ImageView
    private var mImageURI: Uri? = null
    private lateinit var mUserUUID: String
    private lateinit var mFirstName: String
    private lateinit var mLastName: String
    private lateinit var mAnimalType: String
    private lateinit var mAnimalBreed: String
    private lateinit var mAnimalAge: String
    private lateinit var mAnimalByaat: String
    private lateinit var mAnimalMilkQuantity: String
    private lateinit var mAnimalMilkCapacity: String
    private lateinit var mAnimalPrice: String
    private lateinit var rootLayout: LinearLayout
    private lateinit var mProgressLayout: LinearLayout
    private lateinit var mProgressDescription: TextView
    private lateinit var mFormLayout: ScrollView
    private lateinit var mUserLatitude: String
    private lateinit var mUserLongitude: String

    // Get Access to Helper Functions
    private var helper: Helper = Helper()

    var mImageList = ArrayList<Uri>()
    var mUploadedImages = ArrayList<String>()
    var mSelectedImageOrder = ArrayList<ImageView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pashusales_activity_layout)

        rootLayout = findViewById(R.id.pashuSales_root_layout)
        mProgressLayout = findViewById(R.id.progressLayout)
        mFormLayout = findViewById(R.id.pashuSales_scrollView_layout)
        mProgressDescription = findViewById(R.id.progressDescription)
        mProgressLayout.visibility = View.VISIBLE
        mFormLayout.visibility = View.GONE
        loadData()

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        mUserUUID = FirebaseAuth.getInstance().currentUser?.phoneNumber!!
        mFirstName = sharedPref.getString(getString(R.string.sp_userFirstName), "0").toString()
        mLastName = sharedPref.getString(getString(R.string.sp_userLastName), "0").toString()
        mUserLatitude = sharedPref.getString(getString(R.string.sp_userLatitude), "0").toString()
        mUserLongitude = sharedPref.getString(getString(R.string.sp_userLongitude), "0").toString()
        if (mUserUUID == "0") {
            mUserUUID = intent.getStringExtra(getString(R.string.sp_loginUserUUID)).toString()
        }

//        byaat spinner adapter
        val animalByaatList = resources.getStringArray(R.array.pashuSalesActivity_animalByaat)
        val animalByaatSpinner = findViewById<Spinner>(R.id.animalByaatSpinner)
        if (animalByaatSpinner != null) {
            val byaat_adapter = ArrayAdapter(this, R.layout.spinner_item, animalByaatList)
            animalByaatSpinner.adapter = byaat_adapter
            animalByaatSpinner.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    mAnimalByaat = animalByaatList[position]
                    byaat_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // write code to perform some action
                }
            }
        }

        val selectPhotoButton1 = findViewById<Button>(R.id.selectPhotoButton1)
        val selectPhotoButton2 = findViewById<Button>(R.id.selectPhotoButton2)
        val selectPhotoButton3 = findViewById<Button>(R.id.selectPhotoButton3)
        val selectPhotoButton4 = findViewById<Button>(R.id.selectPhotoButton4)

        mSelectedImage1 = findViewById(R.id.selectedImage1)
        mSelectedImage2 = findViewById(R.id.selectedImage2)
        mSelectedImage3 = findViewById(R.id.selectedImage3)
        mSelectedImage4 = findViewById(R.id.selectedImage4)

        selectPhotoButton1.setOnClickListener {
            loadImage(mSelectedImage1)
        }
        selectPhotoButton2.setOnClickListener {
            loadImage(mSelectedImage2)
        }
        selectPhotoButton3.setOnClickListener {
            loadImage(mSelectedImage3)
        }
        selectPhotoButton4.setOnClickListener {
            loadImage(mSelectedImage4)
        }

        val uploadDataButton = findViewById<Button>(R.id.pashuSales_submitButton)
        uploadDataButton.setOnClickListener {

            mAnimalPrice = findViewById<EditText>(R.id.animalPriceEditText).text.toString()
            mAnimalAge = findViewById<EditText>(R.id.animalAgeEditText).text.toString()
            mAnimalMilkCapacity =
                findViewById<EditText>(R.id.animalMilkCapacityEditText).text.toString()
            mAnimalMilkQuantity =
                findViewById<EditText>(R.id.animalMilkQuantityEditText).text.toString()

            if (mAnimalType != "" && mAnimalBreed != "" && mAnimalAge != "" && mAnimalByaat != "" && mAnimalMilkCapacity != "" && mAnimalMilkQuantity != "" && mAnimalPrice != "") {
                if (mImageList.size < 1) {
                    helper.showSnackbar(
                        this,
                        rootLayout,
                        getString(R.string.pashuSalesActivity_allImagesNotUploadedErrorMessage),
                        helper.ERROR_STATE
                    )
                } else {
                    uploadImages()
                }
            } else {
                helper.showSnackbar(
                    this,
                    rootLayout,
                    getString(R.string.incompleteFormErrorMessage),
                    helper.ERROR_STATE
                )
            }

        }

    }

    private fun loadImage(selectedImage: ImageView) {
        setupPermissions()

        fun pickImage() {
            val pickPhoto = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            startActivityForResult(pickPhoto , PICK_IMAGE)
        }

        fun capture() {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, CAPTURE_IMAGE)
        }

        PickImageFragment.newInstance(
            ::pickImage,
            ::capture
        ).show(supportFragmentManager, PickImageFragment.TAG)

        mSelectedImageOrder.add(selectedImage)
    }

    private fun loadData() {
        var animalTypeList = HashMap<Any, Any>()
        var db = FirebaseFirestore.getInstance()
        val collectionRef = db.collection("animalData")
        collectionRef.get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    animalTypeList.set(document.id, document.data)
                }

                setDataIntoFields(animalTypeList)
            }
            .addOnFailureListener { exception ->
                FirebaseCrashlytics.getInstance().recordException(exception)
            }
    }

    private fun setDataIntoFields(data: HashMap<Any, Any>) {
        var animalTypeList: ArrayList<String> = ArrayList()
        var animalBreedList: ArrayList<String> = ArrayList()

        for (item in data) {
            animalTypeList.add(item.key.toString())
        }

        animalTypeList.sortDescending()
        val animalTypeSpinner = findViewById<Spinner>(R.id.animalTypeSpinner)
        if (animalTypeSpinner != null) {
            val animalTypeAdapter =
                ArrayAdapter(this@PashuSalesActivity, R.layout.spinner_item, animalTypeList)
            animalTypeSpinner.adapter = animalTypeAdapter
            animalTypeSpinner.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    mAnimalType = animalTypeList[position]
                    var animalBreedData = data.get(mAnimalType) as HashMap<*, *>
                    animalBreedList = animalBreedData.get("breed") as ArrayList<String>
                    animalTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    if (animalBreedList.get(0) != getString(R.string.pashuSalesActivity_defaultSpinnerOption)) {
                        animalBreedList.add(
                            0,
                            getString(R.string.pashuSalesActivity_defaultSpinnerOption)
                        )
                    }
                    val animalBreedSpinner = findViewById<Spinner>(R.id.animalBreedSpinner)
                    if (animalBreedSpinner != null) {
                        val animalBreedAdapter = ArrayAdapter(
                            this@PashuSalesActivity,
                            R.layout.spinner_item,
                            animalBreedList
                        )
                        animalBreedSpinner.adapter = animalBreedAdapter
                        animalBreedSpinner.onItemSelectedListener = object :
                            AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: AdapterView<*>?,
                                view: View,
                                position: Int,
                                id: Long
                            ) {
                                mAnimalBreed = animalBreedList[position]
                                animalBreedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            }

                            override fun onNothingSelected(parent: AdapterView<*>) {
                                // write code to perform some action
                            }
                        }
                    }

                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    parent.setSelection(0)
                }
            }
        }
        mProgressLayout.visibility = View.GONE
        mFormLayout.visibility = View.VISIBLE
    }

    private fun uploadImages() {
        val storageRef = PashudhanStorage.reference

        for (image in mImageList) {
            val fileName = helper.getRandomString(15)
            val imageRef = storageRef.child("Pashubazaar/${fileName}.jpg")
            val uploadTask = imageRef.putFile(image)
            uploadTask.addOnProgressListener {
                mProgressLayout.visibility = View.VISIBLE
                mFormLayout.visibility = View.GONE
            }.addOnPausedListener {
                Log.d(TAG, "Upload is paused")
            }
            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        FirebaseCrashlytics.getInstance().recordException(it)
                    }
                }
                imageRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    mUploadedImages.add(downloadUri.toString())
                    if (mImageList.size == mUploadedImages.size) {
                        uploadData(mUploadedImages)
                    }
                } else {
//                    Log.d(TAG, "mUploadImages error")
                    FirebaseCrashlytics.getInstance().recordException(FirebaseException("FILE_UPLOAD_EXCEPTION"))
                }
            }
        }
    }

    private fun uploadData(imageUriList: ArrayList<String>) {

        val animalEntry = hashMapOf<String, Any>(
            "timestamp" to "${System.currentTimeMillis() / 1000}",
            "user_uuid" to mUserUUID,
            "animalType" to mAnimalType.trim(),
            "animalBreed" to mAnimalBreed.trim(),
            "animalAge" to mAnimalAge,
            "animalByaat" to mAnimalByaat,
            "animalMilkQuantity" to mAnimalMilkQuantity,
            "animalMilkCapacity" to mAnimalMilkCapacity,
            "animalPrice" to mAnimalPrice,
            "animalImages" to imageUriList,
            "name" to "$mFirstName $mLastName",
            "location" to arrayListOf(mUserLatitude, mUserLongitude),
            "favouritesOf" to arrayListOf<String>()
        )


        PashudhanDB.collection("Pashubazaar")
            .add(animalEntry)
            .addOnSuccessListener {
                helper.showSnackbar(
                    this,
                    rootLayout,
                    getString(R.string.pashuSalesActivity_dataUploadSuccess),
                    helper.SUCCESS_STATE
                )
                findViewById<ProgressBar>(R.id.loadDataProgressBar).visibility = View.GONE
                mProgressDescription.visibility = View.GONE
                findViewById<ImageView>(R.id.successIcon).visibility = View.VISIBLE
                var handler = Handler()

                handler.postDelayed({
                    val intent = Intent(this, BottomNavigationActivity::class.java)
                    startActivity(intent)
                    finish()
                }, 1000)
            }
            .addOnFailureListener {
                mProgressLayout.visibility = View.GONE
                mFormLayout.visibility = View.VISIBLE
                helper.showSnackbar(
                    this,
                    rootLayout,
                    getString(R.string.tryAgainMessage),
                    helper.ERROR_STATE
                )
            }

        mImageList.clear()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            mImageURI = data?.data
            mImageList.add(mImageURI!!)
            if (!mSelectedImageOrder.isNullOrEmpty()) {
                if (!mImageList.isNullOrEmpty()) {
                    mSelectedImageOrder.last().setImageURI(mImageList.last())
                }
            }
        }
        if (resultCode == RESULT_OK && requestCode == CAPTURE_IMAGE) {
            val photo = data?.extras?.get("data") as Bitmap
            var bitmapUtils = BitmapUtils()
            mImageURI = bitmapUtils.getImageUri(this, photo)
            mImageList.add(mImageURI!!)
            if (!mSelectedImageOrder.isNullOrEmpty()) {
                if (!mImageList.isNullOrEmpty()) {
                    mSelectedImageOrder.last().setImageURI(mImageList.last())
                }
            }
        }
    }

    private fun setupPermissions() {
        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Need permissions to proceed further.", Toast.LENGTH_SHORT).show()
            makeRequest()
        }
    }

    private fun makeRequest() {
        val RECORD_REQUEST_CODE = 101
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            RECORD_REQUEST_CODE
        )
    }

}