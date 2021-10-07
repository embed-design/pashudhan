package com.embed.pashudhan

import android.content.Context
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.embed.pashudhan.DataModels.NotificationData
import com.embed.pashudhan.DataModels.PushNotificationData
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class Helper : AppCompatActivity() {

    val SUCCESS_STATE = 1
    val ERROR_STATE = 2
    val WARNING_STATE = 3
    val INFO_STATE = 4
    val DEFAULT_STATE = 5

    fun showSnackbar(
        ctx: Context,
        view: View,
        message: String,
        state: Int? = null,
        actionText: String? = null,
        action: (() -> Unit)? = null,
        actionColor: Int? = null
    ) {
        var mSnackbar =
            Snackbar.make(ctx, view, message, Snackbar.LENGTH_SHORT)
        mSnackbar.setAction(actionText, View.OnClickListener {
            action?.invoke()
        })

        when (state) {
            SUCCESS_STATE -> {
                mSnackbar.setBackgroundTint(ContextCompat.getColor(ctx, R.color.success))
            }
            ERROR_STATE -> {
                mSnackbar.setBackgroundTint(ContextCompat.getColor(ctx, R.color.error))
            }
            WARNING_STATE -> {
                mSnackbar.setBackgroundTint(ContextCompat.getColor(ctx, R.color.warning))
            }
            INFO_STATE -> {
                mSnackbar.setBackgroundTint(ContextCompat.getColor(ctx, R.color.info))
            }
            DEFAULT_STATE -> {
            }
        }

        if (actionColor != null) {
            mSnackbar.setActionTextColor(ContextCompat.getColor(ctx, actionColor))
        }
        mSnackbar.show()
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun changeAppLanguage(ctx: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = ctx.resources.configuration
        config.setLocale(locale)
        ctx.createConfigurationContext(config)
        ctx.resources.updateConfiguration(config, ctx.resources.displayMetrics)

    }

    fun getRandomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    fun prepareNotification(notification: NotificationData, to: String) {
        FirebaseFirestore.getInstance().collection("tokens").document(to).get().addOnSuccessListener {
            if(it.exists()){
                val token = it.data?.get("notificationToken") as String
                PushNotificationData(
                    notification,
                    token
                ).also { notification ->
                    sendNotification(notification, to)
                }
            }
        }
    }

    fun sendNotification(notification: PushNotificationData, toUser: String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitInstance.api.postNotification(notification)
            if(response.isSuccessful) {
                FirebaseFirestore.getInstance().collection("notifications").add(
                    hashMapOf<String, Any>(
                        "title" to notification.data.title,
                        "message" to notification.data.message,
                        "sentTo" to toUser,
                        "timestamp" to "${System.currentTimeMillis() / 1000}"
                    )
                ).addOnSuccessListener {
//                    Log.d("Notification", "Notification Added")
                }
            }else {
//                Log.e("APP==>", response.errorBody().toString())
                FirebaseCrashlytics.getInstance().recordException(response.errorBody() as Throwable)

            }
        } catch(e: Exception) {
//            Log.e("APP==>", e.toString())
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    fun updateFCMToken(token: String) {
        val db = FirebaseFirestore.getInstance().collection("tokens")
        val doc = db.document(FirebaseAuth.getInstance().currentUser?.phoneNumber!!)
        doc.get().addOnSuccessListener {
            if(it.exists()) {
                doc.update(hashMapOf("notificationToken" to token, "timestamp" to "${System.currentTimeMillis() / 1000}") as Map<String, Any>)
            }else {
                doc.set(hashMapOf("notificationToken" to token, "timestamp" to "${System.currentTimeMillis() / 1000}"))
            }
        }.addOnFailureListener {
            FirebaseCrashlytics.getInstance().recordException(it)
        }
    }

}