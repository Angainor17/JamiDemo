package com.voronin.app

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.libp2p.core.*
import io.libp2p.core.dsl.HostBuilder
import io.libp2p.core.multiformats.Multiaddr
import io.libp2p.protocol.Ping
import io.libp2p.protocol.PingController
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.CompletableFuture

class MainActivity : AppCompatActivity() {

    var host: Host = HostBuilder()
        .protocol(Ping())
        .listen(
            "/ip4/127.0.0.1/tcp/0"
        )
        .build()

    init {
        host.addConnectionHandler(object : ConnectionHandler {
            override fun handleConnection(conn: Connection) {
                logMessage("handleConnection localAddress: " + conn.localAddress())
                logMessage("handleConnection remoteAddress: " + conn.remoteAddress())
            }
        })

        host.addStreamHandler(object : StreamHandler<PingController?> {
            override fun handleStream(stream: Stream): CompletableFuture<PingController> {
                logMessage("handleStream remotePeerId =" + stream.remotePeerId())
                logMessage("handleStream localAddress= " + stream.connection.localAddress())
                logMessage("handleStream remoteAddress= " + stream.connection.remoteAddress())
                logMessage("--------------------------------------------------")
                return CompletableFuture()
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonPing.setOnClickListener { ping() }
        buttonStart.setOnClickListener { start() }
        buttonStop.setOnClickListener { stop() }
        buttonRefreshPeerId.setOnClickListener { refreshPeerId() }
        buttonListenTo.setOnClickListener { listenToRefresh() }
        buttonClearLog.setOnClickListener { clearLog() }

        registerForContextMenu(peerId)
    }

    private fun clearLog() {
        logs.text = ""
    }

    private fun listenToRefresh() {
        val listenToAddress = listenToToEditText.text.toString()
        if (listenToAddress.isNotEmpty()) {
            try {
                host = HostBuilder()
                    .protocol(Ping())
                    .listen(
                        "/ip4/127.0.0.1/tcp/0",
                        listenToAddress
                    )
                    .build()
                start()
                logMessage("Listen to $listenToAddress SUCCEED!!!")
            } catch (e: Exception) {
                logMessage("Listen to $listenToAddress FAILED!!!")
                host = HostBuilder()
                    .protocol(Ping())
                    .listen("/ip4/127.0.0.1/tcp/0")
                    .build()
                start()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        stop()
    }

    private fun start() {
        host.start().get()
    }

    private fun stop() {
        host.stop().get()
    }

    private fun refreshPeerId() {
        peerId.text = host.peerId.toBase58()
    }

    private fun ping() {
        val userPeerTo = peerToEditText.text.toString()
        if (userPeerTo.isNotEmpty()) {
            try {
                pingTo(Multiaddr.fromString(userPeerTo))
                val connection = host.network.connect(Multiaddr.fromString(userPeerTo)).get()
                if (connection != null) {
                    logMessage("Connection to $userPeerTo SUCCEED!")
                }

            } catch (e: Exception) {
                logMessage("Connection to $userPeerTo FAIL!")
            }
            return
        }

        val multiaddrs = host.listenAddresses()
        multiaddrs.forEachIndexed { index, value ->
            logMessage("Address $index = $value")
        }

        multiaddrs.map { Multiaddr.fromString(it.toString()) }.forEach { address ->
            pingTo(address)
        }
    }

    private fun pingTo(address: Multiaddr) {
        try {
            val pinger: PingController = Ping().dial(host, address).controller.get()

            logMessage("Sending 5 ping messages to $address")
            for (i in 1..5) {
                val latency = pinger.ping().get()
                logMessage("Ping " + i + ", latency " + latency + "ms")
            }
        } catch (e: Exception) {
            logMessage("Sending 5 ping messages to $address - FAIL!!! ")
        }
    }

    private fun logMessage(text: String) {
        Log.d("voroninDebug", text)
        val oldText = logs.text.toString()
        val newText = "" + (if (oldText.isEmpty()) "" else "\n") + text

        logs.text = oldText + newText
    }
}
