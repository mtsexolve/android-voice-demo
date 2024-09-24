package com.exolve.voicedemo.core.telecom

import android.content.Context
import android.content.Intent
import android.util.Log
import com.exolve.voicesdk.Communicator
import com.exolve.voicesdk.PushNotificationProvider

private const val PUSH_PROVIDER = "PushProvider"

object PushProvider {

    private val pushTypeChooser = mapOf(
        PushType.FIREBASE to PushNotificationProvider.FIREBASE,
        PushType.HMS to PushNotificationProvider.HUAWEI,
    )

    fun setPushToken(context: Context, token: String, pushType: PushType) {
        Log.d(PUSH_PROVIDER, "setPushToken: $pushType token = $token")
        Communicator.onNewPushToken(
            context,
            token,
            pushTypeChooser[pushType]
        )
    }

    fun processPushNotification(context: Context, data: String) {
        Log.d(
            PUSH_PROVIDER,
            "processPushNotification: new notification = $data"
        )
        Communicator.processVoipPushNotification(context, data)
    }

    fun broadcastCallIntent(context: Context, intent: Intent) {
        Log.d(PUSH_PROVIDER, "broadcastCallIntent: intent = ${intent.data}")
        Communicator.broadcastCallIntent(context, intent)
    }

    enum class PushType {
        FIREBASE,
        HMS,
    }
}
