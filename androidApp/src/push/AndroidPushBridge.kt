package org.unesa.agc.push

class AndroidPushBridge(
    val alertCenter: AlertCenter,
    private val tokenSink: (String) -> Unit,
) : PushTokenProvider {

    @Volatile
    private var latestToken: String? = null

    override suspend fun currentToken(): String? = latestToken

    fun onNewToken(token: String) {
        latestToken = token
        tokenSink(token)
    }
}
