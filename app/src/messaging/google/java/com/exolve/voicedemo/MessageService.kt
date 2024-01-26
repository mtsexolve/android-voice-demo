package com.exolve.voicedemo

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.exolve.voicedemo.core.telecom.PushProvider
import com.exolve.voicedemo.core.telecom.TelecomManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val EXOLVE_MESSAGE_SERVICE = "ExolveMessageService"

class MessageService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(
            EXOLVE_MESSAGE_SERVICE, "(markedLog): onMessageReceived( messageId = ${message.messageId})" +
                "sender - ${message.senderId} , rawData = ${message.rawData}, data = ${message.data}")
        PushProvider.processPushNotification(context = this, data = message.data.toString())
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(
                this@MessageService,
                "Push received! Data: ${message.data}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PushProvider.setPushToken(context = this, token = token, pushType = PushProvider.PushType.FIREBASE)
        TelecomManager.getInstance().setToken(token)
        Log.d(EXOLVE_MESSAGE_SERVICE, "onNewToken: token = $token")
    }

    companion object {
        fun enable(context: Context): Boolean {
            var result = false
            CoroutineScope(Dispatchers.IO).launch {
                FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w(
                            EXOLVE_MESSAGE_SERVICE,
                            "FCM: registration token failed",
                            task.exception
                        )
                        return@OnCompleteListener
                    }
                    val token = task.result
                    Log.d(
                        EXOLVE_MESSAGE_SERVICE,
                        "FCM: message service is enabled, token is $token"
                    )
                    TelecomManager.getInstance().setToken(token)
                    result = true
                })
            }
            return result
        }
    }

}

