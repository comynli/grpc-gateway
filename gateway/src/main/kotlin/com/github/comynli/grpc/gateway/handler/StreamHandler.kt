package com.github.comynli.grpc.gateway.handler

import com.github.comynli.grpc.gateway.codec.*
import com.github.comynli.grpc.gateway.codec.*
import com.google.api.HttpRule
import com.google.protobuf.DynamicMessage
import com.github.comynli.grpc.gateway.rpc.Channels
import com.github.comynli.grpc.gateway.rpc.DynamicClient
import com.github.comynli.grpc.gateway.rpc.Proto
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import org.slf4j.LoggerFactory


/**
 * Created by xuemingli on 2017/2/10.
 */
object StreamHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun create(key: HttpRule, vertx: Vertx): SockJSHandler {
        return SockJSHandler.create(vertx).socketHandler {
            val channel = Channels.get(key)
            val method = Proto.get(key)
            val ctx = context(it.headers())
            val md = metadata(it.headers())
            val socket = it
            val request = Flowable.create<DynamicMessage>({
                val emitter = it
                socket.handler {
                    logger.debug(it.toString("UTF-8"))
                    emitter.onNext(it.getProtoMessage(method.inputType))
                }.endHandler {
                    emitter.onComplete()
                }
            }, BackpressureStrategy.BUFFER)
            val response = DynamicClient(channel, method, ctx, md).call(request)
            response.doOnError {
                try {
                    val resp = StatusMapping.get(it)
                    socket.end(Buffer.buffer(resp.toJson()))
                } catch (e: Exception) {
                    logger.warn("end of client connect error")
                }
            }.doOnComplete {
                try {
                    socket.end()
                } catch (e: Exception) {
                    logger.warn("end of client connect error")
                }
            }.subscribe {
                socket.write(Buffer.buffer(messageToJson(it)))
            }
        }
    }

}