package org.unesa.agc

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.unesa.agc.push.AndroidPushBridge
import org.unesa.agc.push.AndroidPushDefaults
import org.unesa.agc.push.AndroidNotificationPresenter
import org.unesa.agc.push.PushRegistrar

class AgcApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        BackendBaseUrl = "http://70.153.144.158:8080"
        val info = packageManager.getApplicationInfo(packageName, android.content.pm.PackageManager.GET_META_DATA)
        info.metaData?.getString("org.unesa.agc.BACKEND_URL")?.let { BackendBaseUrl = it }
        initFirebase()
        val graph = AppGraphHolder.graph
        val notificationPresenter = AndroidNotificationPresenter(this)
        notificationPresenter.ensureChannel()
        graph.alertCenter.presenterOverride = notificationPresenter
        val bridge = AndroidPushBridge(
            alertCenter = graph.alertCenter,
            tokenSink = { token: String ->
                CoroutineScope(Dispatchers.Default).launch {
                    runCatching { graph.api.registerPushToken(token, "android") }
                }
            },
        )
        AndroidPushDefaults.install(bridge)
        graph.pushRegistrar = PushRegistrar(
            provider = bridge,
            register = { token, platform -> graph.api.registerPushToken(token, platform) },
        )
        CoroutineScope(Dispatchers.Default).launch {
            runCatching {
                val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                android.util.Log.i("AgcPush", "fcm token fetched len=${token.length}")
                bridge.onNewToken(token)
            }.onFailure { android.util.Log.w("AgcPush", "fcm token fetch failed", it) }
        }
    }

    private fun initFirebase() {
        if (FirebaseApp.getApps(this).isNotEmpty()) return
        val json = assets.open("google-services.json").use { stream ->
            Json.parseToJsonElement(stream.bufferedReader().readText()).jsonObject
        }
        val project = json["project_info"]!!.jsonObject
        val client = json["client"]!!.jsonArray.first().jsonObject
        val options = FirebaseOptions.Builder()
            .setApiKey(client["api_key"]!!.jsonArray.first().jsonObject["current_key"]!!.jsonPrimitive.content)
            .setApplicationId(client["client_info"]!!.jsonObject["mobilesdk_app_id"]!!.jsonPrimitive.content)
            .setProjectId(project["project_id"]!!.jsonPrimitive.content)
            .setGcmSenderId(project["project_number"]!!.jsonPrimitive.content)
            .build()
        FirebaseApp.initializeApp(this as Context, options)
    }
}
