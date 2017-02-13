package com.github.comynli.grpc.gateway.rpc

import com.google.protobuf.Descriptors
import io.grpc.stub.ClientCalls
import com.google.protobuf.DynamicMessage
import io.grpc.*
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import io.reactivex.*
import org.slf4j.LoggerFactory


/**
 * Created by xuemingli on 2017/2/9.
 */
class DynamicClient @JvmOverloads constructor(private val channel: Channel,
                                              private val descriptor: Descriptors.MethodDescriptor,
                                              private val ctx: Context = Context.ROOT,
                                              private val md: Metadata = Metadata(),
                                              private val options: CallOptions = CallOptions.DEFAULT) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val type: MethodDescriptor.MethodType
    private val name = MethodDescriptor.generateFullMethodName(descriptor.service.fullName, descriptor.name)

    init {
        val cs = descriptor.toProto().clientStreaming
        val ss = descriptor.toProto().serverStreaming
        if (!cs && !ss) {
            type = MethodDescriptor.MethodType.UNARY
        } else if (!cs && ss) {
            type = MethodDescriptor.MethodType.SERVER_STREAMING
        } else if (cs && !ss) {
            type = MethodDescriptor.MethodType.CLIENT_STREAMING
        } else {
            type = MethodDescriptor.MethodType.BIDI_STREAMING
        }
    }

    private fun createCall(): ClientCall<DynamicMessage, DynamicMessage> {
        val method = MethodDescriptor.create(type,
                name,
                DynamicMessageMarshaller(descriptor.inputType),
                DynamicMessageMarshaller(descriptor.outputType))
        return ClientInterceptors
                .intercept(channel, MetadataUtils.newAttachHeadersInterceptor(md))
                .newCall(method, options)
    }

    private fun unaryCall(request: DynamicMessage, response: StreamObserver<DynamicMessage>) {
        ctx.run {
            ClientCalls.asyncUnaryCall(createCall(), request, response)
        }
    }

    private fun clientStreamingCall(response: StreamObserver<DynamicMessage>): StreamObserver<DynamicMessage> {
        return ctx.call {
            ClientCalls.asyncClientStreamingCall(createCall(), response)
        }
    }

    private fun serverStreamingCall(request: DynamicMessage, response: StreamObserver<DynamicMessage>) {
        ctx.run {
            ClientCalls.asyncServerStreamingCall(createCall(), request, response)
        }
    }

    private fun bidiStreamCall(response: StreamObserver<DynamicMessage>): StreamObserver<DynamicMessage> {
        return ctx.call {
            ClientCalls.asyncBidiStreamingCall(createCall(), response)
        }
    }

    fun call(request: Flowable<DynamicMessage>): Flowable<DynamicMessage> {
        return Flowable.create({
            val emitter = it
            try {
                val response = object : StreamObserver<DynamicMessage> {
                    override fun onError(t: Throwable) {
                        emitter.onError(t)
                    }

                    override fun onCompleted() {
                        emitter.onComplete()
                    }

                    override fun onNext(value: DynamicMessage) {
                        emitter.onNext(value)
                    }

                }
                if (type == MethodDescriptor.MethodType.UNARY) {
                    request.subscribe { unaryCall(it, response) }
                }
                if (type == MethodDescriptor.MethodType.CLIENT_STREAMING) {
                    val observer = clientStreamingCall(response)
                    request.doOnComplete { observer.onCompleted() }
                            .doOnError { observer.onError(it) }
                            .subscribe { observer.onNext(it) }
                }
                if (type == MethodDescriptor.MethodType.SERVER_STREAMING) {
                    request.subscribe { serverStreamingCall(it, response) }
                }
                if (type == MethodDescriptor.MethodType.BIDI_STREAMING) {
                    val observer = bidiStreamCall(response)
                    request.doOnComplete { observer.onCompleted() }
                            .doOnError { observer.onError(it) }
                            .subscribe { observer.onNext(it) }
                }
            } catch (e: Throwable) {
                logger.error("call $name error", e)
                emitter.onError(e)
            }

        }, BackpressureStrategy.BUFFER)
    }
}