package com.github.comynli.grpc.gateway.codec

import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import com.google.protobuf.util.JsonFormat
import io.vertx.core.MultiMap
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.RoutingContext

/**
 * Created by xuemingli on 2017/2/10.
 */
private val logger = LoggerFactory.getLogger("com.ymatou.op.gateway.codec.ext")

private fun unflatten(key: String, value: String, node: JsonObject) {
    if (key.contains('.')) {
        val arr = key.split('.', limit = 2)
        val o = JsonObject()
        node.put(arr[0], o)
        unflatten(arr[1], value, o)
    } else {
        node.put(key, value)
    }
}

private fun unflatten(source: MultiMap): JsonObject {
    val node = JsonObject()
    source.forEach {
        unflatten(it.key, it.value, node)
    }
    return node
}

private fun parse(body: String, builder: DynamicMessage.Builder) {
    if (!body.isNullOrBlank()) {
        JsonFormat.parser().merge(body, builder)
    }
}

private fun parse(params: MultiMap, builder: DynamicMessage.Builder) {
    if (params.isEmpty) {
        return
    }
    parse(unflatten(params).encode(), builder)
}

fun RoutingContext.getProtoMessage(descriptor: Descriptors.Descriptor): DynamicMessage {
    val builder = DynamicMessage.newBuilder(descriptor)
    parse(bodyAsString, builder)
    parse(request().params(), builder)
    return builder.build()
}

fun Buffer.getProtoMessage(descriptor: Descriptors.Descriptor): DynamicMessage {
    val builder = DynamicMessage.newBuilder(descriptor)
    JsonFormat.parser().merge(this.toString("UTF-8"), builder)
    return builder.build()
}