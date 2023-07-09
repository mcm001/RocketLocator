package com.mcm001.serial

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString
import org.broeuschmeul.android.gps.bluetooth.provider.GenericGpsSource

class WebsocketGpsParser(private val websocketURL: String) : GenericGpsSource() {
    private lateinit var webSocket: WebSocket
    private lateinit var httpClient: OkHttpClient


    private val listener2 = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            // Call to ovserve new data:
            this@WebsocketGpsParser.notifyNmeaSentence(text);
        }

        override fun onMessage(
            webSocket: WebSocket,
            bytes: ByteString
        ) {
        }

        override fun onClosing(
            webSocket: WebSocket,
            code: Int, reason: String
        ) {
        }

        override fun onClosed(
            webSocket: WebSocket,
            code: Int, reason: String
        ) {
        }

        override fun onFailure(
            webSocket: WebSocket,
            throwable: Throwable,
            response: Response?
        ) {
        }
    }

    override fun getDisableReason(): Int {
        return 0
    }

    override fun enable() {
        GlobalScope.launch(Dispatchers.IO) {
            httpClient = OkHttpClient()
            val request = Request.Builder()
                .url(websocketURL)
                .build()

            webSocket = httpClient.newWebSocket(request, listener2)
            httpClient.dispatcher.executorService.shutdown()
        }
    }

    override fun disable(reasonId: Int) {

    }

    override fun disable(restart: Boolean) {}
}


