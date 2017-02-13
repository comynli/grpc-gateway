package com.github.comynli.grpc.gateway

import com.google.api.AnnotationsProto
import com.google.api.HttpRule
import com.google.protobuf.Descriptors
import com.github.comynli.grpc.gateway.handler.StreamHandler
import com.github.comynli.grpc.gateway.handler.UnaryHandler
import com.github.comynli.grpc.gateway.rpc.Channels
import com.github.comynli.grpc.gateway.rpc.Proto
import io.grpc.ManagedChannel
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import org.slf4j.LoggerFactory

/**
 * Created by xuemingli on 2017/2/10.
 */
class Bootstrap @JvmOverloads constructor(private val channelMaker: (String) -> ManagedChannel,
                                          vertxOptions: VertxOptions = VertxOptions(),
                                          private val httpServerOptions: HttpServerOptions = HttpServerOptions()) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val vertx = Vertx.vertx(vertxOptions)
    private val root = Router.router(vertx)
    private val loader = javaClass.classLoader

    init {
        root.route().handler(BodyHandler.create())
    }

    fun register(upstream: String, proto: String, mountPoint: String = "/") {
        val router = Router.router(vertx)
        val klass = loader.loadClass(proto)
        val method = klass.getMethod("getDescriptor")
        val descriptor = method.invoke(null) as Descriptors.FileDescriptor
        descriptor.services.forEach {
            it.methods.filter { it.options.hasExtension(AnnotationsProto.http) }.forEach {
                val rule = it.options.getExtension(AnnotationsProto.http)
                if (it.toProto().clientStreaming || it.toProto().serverStreaming) {
                    if (rule.patternCase == HttpRule.PatternCase.CUSTOM) {
                        router.route("${rule.custom.path}/*").handler(StreamHandler.create(rule, vertx))
                    } else {
                        when (rule.patternCase) {
                            HttpRule.PatternCase.GET -> router.route("${rule.get}/*").handler(StreamHandler.create(rule, vertx))
                            HttpRule.PatternCase.POST -> router.route("${rule.post}/*").handler(StreamHandler.create(rule, vertx))
                            HttpRule.PatternCase.PUT -> router.route("${rule.put}/*").handler(StreamHandler.create(rule, vertx))
                            HttpRule.PatternCase.DELETE -> router.route("${rule.delete}/*").handler(StreamHandler.create(rule, vertx))
                            HttpRule.PatternCase.PATCH -> router.route("${rule.patch}/*").handler(StreamHandler.create(rule, vertx))
                            else -> logger.warn("nonsupport method")
                        }
                    }
                } else {
                    when (rule.patternCase) {
                        HttpRule.PatternCase.GET -> router.get(rule.get).handler(UnaryHandler(rule))
                        HttpRule.PatternCase.POST -> router.post(rule.post).handler(UnaryHandler(rule))
                        HttpRule.PatternCase.PUT -> router.put(rule.put).handler(UnaryHandler(rule))
                        HttpRule.PatternCase.DELETE -> router.delete(rule.delete).handler(UnaryHandler(rule))
                        HttpRule.PatternCase.PATCH -> router.patch(rule.patch).handler(UnaryHandler(rule))
                        else -> logger.warn("nonsupport method")
                    }
                }
                Channels.create(upstream, rule, channelMaker)
                Proto.put(rule, it)
            }
        }
        root.mountSubRouter(mountPoint, router)
    }

    fun start() {
        vertx.createHttpServer(httpServerOptions).requestHandler { root.accept(it) }.listen()
    }

    fun shutdown() {
        vertx.close { Channels.close() }
    }
}