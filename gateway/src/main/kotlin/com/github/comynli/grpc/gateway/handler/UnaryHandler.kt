package com.github.comynli.grpc.gateway.handler

import com.github.comynli.grpc.gateway.codec.context
import com.github.comynli.grpc.gateway.codec.metadata
import com.google.api.HttpRule
import com.github.comynli.grpc.gateway.codec.*
import com.github.comynli.grpc.gateway.rpc.Channels
import com.github.comynli.grpc.gateway.rpc.DynamicClient
import com.github.comynli.grpc.gateway.rpc.Proto
import io.reactivex.Flowable
import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory

/**
 * Created by xuemingli on 2017/2/9.
 */
class UnaryHandler(private val key: HttpRule) : Handler<RoutingContext> {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun handle(event: RoutingContext) {
        logger.debug("${event.request().method().name} ${event.normalisedPath()}")
        try {
            val channel = Channels.get(key)
            val method = Proto.get(key)
            val request = event.getProtoMessage(method.inputType)
            val ctx = context(event.request().headers())
            val md = metadata(event.request().headers())
            val response = DynamicClient(channel, method, ctx, md).call(Flowable.just(request))
            response.doOnError {
                val resp = StatusMapping.get(it)
                event.response()
                        .setStatusCode(resp.status)
                        .setStatusMessage(resp.message)
                        .putHeader("content-type", "application/json")
                        .end(resp.toJson())
            }.subscribe {
                event.response()
                        .putHeader("content-type", "application/json")
                        .end(messageToJson(it))
            }
        } catch (e: Exception) {
            logger.error("call upstream error", e)
            val resp = StatusMapping.get(e)
            event.response()
                    .setStatusCode(resp.status)
                    .setStatusMessage(resp.message)
                    .putHeader("content-type", "application/json")
                    .end(resp.toJson())
        }
    }

}