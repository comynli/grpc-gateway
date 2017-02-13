package com.github.comynli.grpc.gateway.rpc

import com.google.api.HttpRule
import io.grpc.*

/**
 * Created by xuemingli on 2017/2/9.
 */
object Channels {
    private val channels = mutableMapOf<String, ManagedChannel>()
    private val mapping = mutableMapOf<HttpRule, String>()

    fun get(key: HttpRule): ManagedChannel {
        return channels[mapping[key]!!]!!
    }

    fun create(upstream: String, key: HttpRule, maker: (String) -> ManagedChannel) {
        if (!channels.containsKey(upstream)) {
            channels[upstream] = maker(upstream)
        }
        mapping[key] = upstream
    }

    fun close() {
        channels.forEach { it.value.shutdown() }
    }


}