package com.github.comynli.grpc.gateway.rpc

import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import io.grpc.MethodDescriptor
import java.io.InputStream

/**
 * Created by xuemingli on 2017/2/9.
 */
class DynamicMessageMarshaller(private val descriptor: Descriptors.Descriptor) : MethodDescriptor.Marshaller<DynamicMessage> {
    override fun parse(stream: InputStream): DynamicMessage {
        return DynamicMessage.newBuilder(descriptor).mergeFrom(stream).build()
    }

    override fun stream(value: DynamicMessage): InputStream {
        return value.toByteString().newInput()
    }
}