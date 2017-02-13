package com.github.comynli.grpc.gateway.rpc

import com.google.api.HttpRule
import com.google.protobuf.Descriptors

/**
 * Created by xuemingli on 2017/2/9.
 */
object Proto {
    val methods = mutableMapOf<HttpRule, Descriptors.MethodDescriptor>()

    fun get(key: HttpRule): Descriptors.MethodDescriptor {
        return methods[key]!!
    }

    fun put(key: HttpRule, descriptor: Descriptors.MethodDescriptor) {
        methods[key] = descriptor
    }
}