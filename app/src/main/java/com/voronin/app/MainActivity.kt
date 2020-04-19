package com.voronin.app

import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.libp2p.core.Host
import io.libp2p.core.dsl.HostBuilder
import io.libp2p.core.multiformats.Multiaddr
import io.libp2p.protocol.Ping
import io.libp2p.protocol.PingController
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    val handler = Handler()

    val node: Host = HostBuilder()
        .protocol(Ping())
        .listen("/ip4/127.0.0.1/tcp/0")
        .build()

    private val apiTest = Runnable {
        // start listening
        node.start().get()

        val multiaddrs = (node.listenAddresses())
        multiaddrs.forEachIndexed{ index, value ->
            Log.d("voronin", "Address $index = $value")
        }

        val address = Multiaddr.fromString(multiaddrs[0].toString())
        val pinger : PingController = Ping().dial(node, address).controller.get()
//        /ip4/127.0.0.1/tcp/32912/ipfs/QmNMaqmmFbwiXgwfrpt55aq8FYSo5HxDJZNE6zrnLrSN3z
        Log.d("voronin", "Sending 5 ping messages to $address")
        for (i in 1..5) {
            val latency = pinger.ping().get()
            Log.d("voronin", "Ping " + i + ", latency " + latency + "ms")
        }
        node.stop().get()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonLoad.setOnClickListener {
            handler.post(apiTest)
        }
    }
}
