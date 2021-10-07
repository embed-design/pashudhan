package com.embed.pashudhan.Fragments

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.embed.pashudhan.Activities.MainLoginActivity
import com.embed.pashudhan.DataModels.users
import com.embed.pashudhan.Helper
import com.embed.pashudhan.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.fragment_profile.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException


class ProfileFragment : Fragment() {

    companion object {
        val TAG = "Profile==>"
        private val cameraRequest = 1888
        private val pickerRequest = 1889
        fun newInstance(userID: String) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString("userID", userID)
                }
            }
    }

    private lateinit var mSharedPref: SharedPreferences
    private lateinit var addProfileImage: RelativeLayout
    private lateinit var currentPhotoPath: String
    private lateinit var mUserUUID: String
    private lateinit var languageChangeSpinner: Spinner
    private lateinit var mActivity: FragmentActivity
    private lateinit var userProfileImageHolder: ImageView
    private lateinit var addImageInstruction: LinearLayout
    private lateinit var userFullName: String
    private lateinit var userLocation: String
    private lateinit var bioEditText: EditText
    private lateinit var bioLayout: LinearLayout
    private lateinit var bioTextView: TextView
    private lateinit var saveProfileButton: Button
    private lateinit var logoutButton: Button
    private lateinit var uploadsButton: CardView
    private lateinit var storiesButton: CardView
    private lateinit var favouritePostsButton: CardView
    private lateinit var progressLayout: LinearLayout
    private lateinit var dataLayout: LinearLayout
    private var profilePhoto: Bitmap? = null
    private val favouritePostsFragment = UserFavouritePosts()
    private val userUploadedPosts = UserUploadedPosts()
    private val userUploadedStories = UserUploadedStories()
    private var profileDoc: users? = null

    private var helper = Helper()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mUserUUID = arguments?.getString("userID")!!
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val parentView = view
        mActivity = requireActivity()
        progressLayout = parentView.findViewById(R.id.profile_progressLayout)
        dataLayout = parentView.findViewById(R.id.profileDataLayout)
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(parentView.context)
        languageChangeSpinner = parentView.findViewById<Spinner>(R.id.languageChangeSpinner)
        progressLayout.visibility = View.VISIBLE
        dataLayout.visibility = View.GONE

        var PashudhanDB = FirebaseFirestore.getInstance()
        val docRef = PashudhanDB.collection("users").document(mUserUUID)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    profileDoc = document.toObject(users::class.java)
                    loadDataView(parentView)
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
    }

    private fun loadDataView(parentView: View) {
        progressLayout.visibility = View.GONE
        dataLayout.visibility = View.VISIBLE
        if(mUserUUID == FirebaseAuth.getInstance().currentUser?.phoneNumber!!) {
            languageChangeSpinner.visibility = View.VISIBLE
            handleLocaleChange(parentView)
        } else {
            languageChangeSpinner.visibility = View.GONE
        }

        // Profile Image
        userProfileImageHolder = parentView.findViewById(R.id.userProfileImageHolder)
        addImageInstruction = parentView.findViewById(R.id.addImageInstruction)

        if(profileDoc?.profileThumbnail != "") {
            Glide.with(this).load(profileDoc?.profileThumbnail)
                .circleCrop()
                .placeholder(R.drawable.user_placeholder)
                .into(userProfileImageHolder)
            addImageInstruction.visibility = View.GONE
        } else {
            Glide.with(this).load(R.drawable.user_placeholder)
                .circleCrop()
                .into(userProfileImageHolder)
        }

        if(mUserUUID != FirebaseAuth.getInstance().currentUser?.phoneNumber!!) {
            addImageInstruction.visibility = View.GONE

        }else {
            addProfileImage = parentView.findViewById(R.id.addProfileImage)
            addProfileImage.setOnClickListener {
                if (ContextCompat.checkSelfPermission(parentView.context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(
                        mActivity,
                        arrayOf(Manifest.permission.CAMERA),
                        cameraRequest
                    )
                }else {
                    handleProfileImageUpdate(parentView)
                }
            }
        }




        // Name
        userFullName = "${profileDoc?.firstName} ${profileDoc?.lastName}"

        var userFullNameTextView = parentView.findViewById<TextView>(R.id.userFullName)
        userFullNameTextView.text = userFullName

        // Address
        var userLocationTextView = parentView.findViewById<TextView>(R.id.userLocation)

        var city = profileDoc?.district
        var state = profileDoc?.state
        var postalCode = profileDoc?.pinCode
        userLocation = "$city, $state - $postalCode"

        userLocationTextView.text = userLocation

        // Bio
        bioEditText = parentView.findViewById(R.id.user_bio_et)
        bioLayout = parentView.findViewById(R.id.user_bio_layout)
        bioTextView = parentView.findViewById(R.id.user_bio_tv)
        if(mUserUUID != FirebaseAuth.getInstance().currentUser?.phoneNumber!!) {
            if(profileDoc?.bio != "" && !profileDoc?.bio.isNullOrEmpty()) {
                bioLayout.visibility = View.VISIBLE
            }else {
                bioLayout.visibility = View.GONE
            }
            bioEditText.visibility = View.GONE
            bioTextView.text = profileDoc?.bio
        } else {
            bioLayout.visibility = View.GONE
            bioEditText.visibility = View.VISIBLE
            bioEditText.setText(profileDoc?.bio)
        }

        // My Posts
        uploadsButton = parentView.findViewById(R.id.yourUploadsButton)
        if(mUserUUID != FirebaseAuth.getInstance().currentUser?.phoneNumber!!) {
            uploadsButtonText.text = requireActivity().getString(R.string.userUploads)
        }
        uploadsButton.setOnClickListener {
            fragmentManager?.beginTransaction()?.
            replace(R.id.nav_frameLayout, UserUploadedPosts.newInstance(
                mUserUUID
            ))?.
            setReorderingAllowed(true)?.
            addToBackStack(userUploadedPosts.tag)?.
            commit()
        }

        // My Stories
        storiesButton = parentView.findViewById(R.id.yourStoriesButton)
        if(mUserUUID != FirebaseAuth.getInstance().currentUser?.phoneNumber!!) {
            storyButtonText.text = requireActivity().getString(R.string.userStories)
        }
        storiesButton.setOnClickListener {
            fragmentManager?.beginTransaction()?.
            replace(R.id.nav_frameLayout, UserUploadedStories.newInstance(
                mUserUUID
            ))?.
            setReorderingAllowed(true)?.
            addToBackStack(userUploadedStories.tag)?.
            commit()
        }

        // My Favourites
        favouritePostsButton = parentView.findViewById(R.id.yourFavouritesButton)
        if(mUserUUID != FirebaseAuth.getInstance().currentUser?.phoneNumber!!) {
            favouritePostsButton.visibility = View.GONE

        } else {
            favouritePostsButton.visibility = View.VISIBLE
            favouritePostsButton.setOnClickListener {
                fragmentManager?.beginTransaction()
                    ?.replace(R.id.nav_frameLayout, favouritePostsFragment)
                    ?.setReorderingAllowed(true)?.addToBackStack(favouritePostsFragment.tag)
                    ?.commit()
            }
        }
        // Save Profile
        saveProfileButton = parentView.findViewById(R.id.save_profile)
        if(mUserUUID != FirebaseAuth.getInstance().currentUser?.phoneNumber!!) {
            saveProfileButton.visibility = View.GONE
        } else {
            saveProfileButton.visibility = View.VISIBLE
            saveProfileButton.setOnClickListener {
                handleSaveData(parentView)
            }
        }


        // Logout
        logoutButton = parentView.findViewById(R.id.logout_button)
        if(mUserUUID != FirebaseAuth.getInstance().currentUser?.phoneNumber!!) {
            logoutButton.visibility = View.GONE
        } else {
            logoutButton.visibility = View.VISIBLE
            logoutButton.setOnClickListener {
                val builder = AlertDialog.Builder(parentView.context)
                builder.setMessage(parentView.context.getString(R.string.logout_confirmMessage))
                    .setCancelable(false)
                    .setPositiveButton(parentView.context.getString(R.string.yes_button)) { dialog, id ->
                        progressLayout.visibility = View.VISIBLE
                        dataLayout.visibility = View.GONE
                        mSharedPref.all.clear()
                        Firebase.auth.signOut()
                        val intent = Intent(parentView.context, MainLoginActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    }
                    .setNegativeButton(parentView.context.getString(R.string.no_button)) { dialog, id ->
                        // Dismiss the dialog
                        dialog.dismiss()
                    }
                val alert = builder.create()
                alert.show()
            }
        }

    }

    private fun handleProfileImageUpdate(parentView: View) {

        fun pickImage() {
            val pickPhoto = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            startActivityForResult(pickPhoto , pickerRequest)
        }

        fun capture() {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, cameraRequest)
        }

        PickImageFragment.newInstance(
            ::pickImage,
            ::capture
        ).show(mActivity.supportFragmentManager, PickImageFragment.TAG)

    }

    private fun handleSaveData(parentView: View) {
        progressLayout.visibility = View.VISIBLE
        dataLayout.visibility = View.GONE
        if(profilePhoto == null) {
            uploadData("")
        }else {
            val storage = Firebase.storage
            var storageRef = storage.reference
            var fileName = helper.getRandomString(15)
            var imageRef =
                storageRef.child("Profile/${fileName}_${System.currentTimeMillis() / 1000}.jpg")

            val baos = ByteArrayOutputStream()
            profilePhoto?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()
            var uploadTask = imageRef.putBytes(data)
            uploadTask.addOnProgressListener {
            }.addOnPausedListener {
                Log.d(TAG, "Upload is paused")
            }
            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        Log.d(TAG, it.toString())
                    }
                }
                imageRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    uploadData("$downloadUri")
                    Log.d(TAG, "Uploaded image")
                } else {
                    Log.d(TAG, "mUploadImages error")
                }
            }
        }
    }

    private fun uploadData(fileUri: String) {
        if(fileUri != "" || profileDoc?.bio != bioEditText.text.toString()) {
            val PashudhanDB = FirebaseFirestore.getInstance()
            val userRef = PashudhanDB.collection("users").document(mUserUUID)

            var dataToUpdate = hashMapOf(
                "profileThumbnail" to fileUri,
                "bio" to bioEditText.text.toString()
            )

            userRef
                .update(dataToUpdate as Map<String, Any>)
                .addOnSuccessListener {
                    progressLayout.visibility = View.GONE
                    dataLayout.visibility = View.VISIBLE
                    with(mSharedPref.edit()) {
                        putString(
                            getString(R.string.sp_profileImage),
                            fileUri
                        )
                        putString(
                            getString(R.string.sp_bio),
                            bioEditText.text.toString()
                        )
                        apply()
                    }
                    Log.d(TAG, "DocumentSnapshot successfully updated!")
                }
                .addOnFailureListener { e -> Log.w(TAG, "Error updating document", e) }
        } else {
            progressLayout.visibility = View.GONE
            dataLayout.visibility = View.VISIBLE
            Toast.makeText(mActivity, "No changes to profile", Toast.LENGTH_SHORT).show()
        }

    }

    @Throws(IOException::class)
    private fun createImageFile(parentView: View): File {
        // Create an image file name
        val storageDir: File? = parentView.context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "${System.currentTimeMillis() / 1000}_${mUserUUID}", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == cameraRequest && resultCode == RESULT_OK) {
            profilePhoto = data?.extras?.get("data") as Bitmap
            addImageInstruction.visibility = View.GONE
            Glide.with(this).load(profilePhoto)
                .circleCrop()
                .placeholder(R.drawable.download)
                .into(userProfileImageHolder)
        } else if (requestCode == pickerRequest && resultCode == RESULT_OK) {
            val profileImageUri = data?.data
            profilePhoto= MediaStore.Images.Media.getBitmap(activity?.contentResolver, profileImageUri)
            addImageInstruction.visibility = View.GONE
            Glide.with(this).load(profilePhoto)
                .circleCrop()
                .placeholder(R.drawable.download)
                .into(userProfileImageHolder)
        }
    }


    fun handleLocaleChange(parentView: View) {
        var currentLocale = mSharedPref.getString(getString(R.string.sp_locale), "0")
        val languageList = arrayListOf<String>(*resources.getStringArray(R.array.languages))
        if (languageChangeSpinner != null) {
            val languageAdapter = ArrayAdapter(parentView.context, R.layout.spinner_item, languageList)
            languageChangeSpinner.adapter = languageAdapter

            if(currentLocale == "mr") {
                languageChangeSpinner.setSelection(0)
            }else {
                languageChangeSpinner.setSelection(1)
            }
            languageChangeSpinner.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val languageSelected = languageList[position]
                    var locale = getString(R.string.MR_Locale)
                    if(position == 0) {
                        locale = getString(R.string.MR_Locale)
                    }else if(position == 1) {
                        locale = getString(R.string.HI_Locale)
                    }
                    Log.d(TAG, currentLocale.toString())
                    if(currentLocale != locale){
                        Toast.makeText(parentView.context, getString(R.string.languageChange) + languageSelected, Toast.LENGTH_SHORT).show()
                    }
                    helper.changeAppLanguage(parentView.context, locale)
                    with(mSharedPref.edit()) {
                        putString(
                            getString(R.string.sp_locale),
                            locale
                        )
                        apply()
                    }

                    languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // write code to perform some action
                }
            }
        }
    }

}