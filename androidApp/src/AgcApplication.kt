package org.unesa.agc

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.unesa.agc.push.AndroidPushBridge
import org.unesa.agc.push.AndroidPushDefaults

class AgcApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        BackendBaseUrl = "http://10.0.2.2:8080"
        val info = packageManager.getApplicationInfo(packageName, android.content.pm.PackageManager.GET_META_DATA)
        info.metaData?.getString("org.unesa.agc.BACKEND_URL")?.let { BackendBaseUrl = it }
        initFirebase()
        val graph = AppGraphHolder.graph
        AndroidPushDefaults.install(
            AndroidPushBridge(
                alertCenter = graph.alertCenter,
                tokenSink = { token: String ->
                    CoroutineScope(Dispatchers.Default).launch {
                        runCatching { graph.api.registerPushToken(token, "android") }
                    }
                },
            ),
        )
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
