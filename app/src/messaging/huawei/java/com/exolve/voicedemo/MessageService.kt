package com.exolve.voicedemo
import android.app.Application
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.exolve.voicedemo.core.telecom.PushProvider
import com.exolve.voicedemo.core.telecom.TelecomManager
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.HmsMessaging
import com.huawei.hms.push.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val EXOLVE_MESSAGE_SERVICE = "ExolveMessageService"

class MessageService : HmsMessageService() {

    override fun onMessageReceived(p0: RemoteMessage?) {
        super.onMessageReceived(p0)
        Log.d(
            EXOLVE_MESSAGE_SERVICE, "HMS: (markedLog): onMessageReceived( messageId = ${p0?.messageId})" +
                "messageType - ${p0?.messageType} , data = ${p0?.data}")
        p0?.let{
            PushProvider.processPushNotification(context = this, data = it.toString())
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(
                    this@MessageService,
                    "Push received! Data: ${it.data}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onNewToken(p0: String?) {
        super.onNewToken(p0)
        Log.d(EXOLVE_MESSAGE_SERVICE, "onNewToken: HMS token = $p0")
        p0?.let{
            PushProvider.setPushToken(context = this, token = it, pushType = PushProvider.PushType.HMS)
            TelecomManager.getInstance(this.application).setToken(it)
        }
    }

    companion object {
        fun enable(context: Application): Boolean {
            var result = false
            CoroutineScope(Dispatchers.IO).launch {
                HmsMessaging.getInstance(context).turnOnPush().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        result = true
                        Log.i(EXOLVE_MESSAGE_SERVICE, "HMS: enableMessageService: success")
                    } else {
                        Log.w(
                            EXOLVE_MESSAGE_SERVICE,
                            "HMS: enableMessageService failure: ${task.exception.message}"
                        )
                    }
                }
                try {
                    val appId =
                        AGConnectOptionsBuilder().build(context).getString("client/app_id")
                    val pushtoken = HmsInstanceId.getInstance(context).getToken(appId, "HCM")
                    if (!TextUtils.isEmpty(pushtoken)) {
                        Log.d(
                            EXOLVE_MESSAGE_SERVICE,
                            "HMS: enableMessageService: get token: $pushtoken"
                        )
                        TelecomManager.getInstance(context).setToken(pushtoken)
                    }
                } catch (e: Exception) {
                    Log.d(
                        EXOLVE_MESSAGE_SERVICE,
                        "HMS: enableMessageService getToken failed: ${e.message}"
                    )
                }
            }
            return result
        }
    }

}
