package com.github.comynli.grpc.gateway.codec

import io.grpc.Context
import io.grpc.Deadline
import io.grpc.Metadata
import io.vertx.core.MultiMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Created by xuemingli on 2017/2/13.
 */
fun context(headers: MultiMap): Context {
    var ctx = Context.ROOT
    headers.forEach {
        if (it.key.toLowerCase() == "authorization") {
            val key: Context.Key<String> = Context.key("authorization")
            ctx = ctx.withValue(key, it.value)
        }
        if (it.key.toLowerCase() == "x-grpc-timeout") {
            try {
                val timeout = it.value.toLong()
                ctx = ctx.withDeadline(Deadline.after(timeout, TimeUnit.SECONDS),
                        Executors.newSingleThreadScheduledExecutor())
            } catch (e: Exception) {

            }
        }
    }
    return ctx
}

fun metadata(headers: MultiMap): Metadata {
    val meta = Metadata()
    headers.forEach {
        val name = it.key.toLowerCase()
        if (name.startsWith("x-grpc-metadata-")) {
            val key = Metadata.Key.of(name.removePrefix("x-grpc-metadata-"), Metadata.BINARY_BYTE_MARSHALLER)
            meta.put(key, it.value.toByteArray())
        }
    }
    return meta
}