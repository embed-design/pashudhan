package com.embed.pashudhan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.embed.pashudhan.Activities.BottomNavigationActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random


class NotificationService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "pashudhan_notification"
        private const val TAG = "NotificationService==>"
    }


    override fun onNewToken(token: String) {
        super.onNewToken(token)
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

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val intent = Intent(this, BottomNavigationActivity::class.java)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationID = Random.nextInt()

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        val icon: Bitmap = BitmapFactory.decodeResource(this.resources, R.drawable.app_icon)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, FLAG_ONE_SHOT)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(message.data["title"])
            .setContentText(message.data["message"])
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setLargeIcon(icon)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(message.data["message"]))
            .build()

        notificationManager.notify(notificationID, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager){
        val channelName = "pashudhan_notification"
        val channel = NotificationChannel(CHANNEL_ID, channelName, IMPORTANCE_HIGH).apply {
            description = "Pashudhan notificaton channel"
            enableLights(true)
            lightColor = Color.GREEN
        }

        notificationManager.createNotificationChannel(channel)
    }
}