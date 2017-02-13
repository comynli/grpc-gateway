package com.github.comynli.grpc.gateway.codec

import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import io.vertx.core.json.JsonObject

/**
 * Created by xuemingli on 2017/2/10.
 */
object StatusMapping {
    data class Response(val status: Int, val code: Status.Code, val message: String) {
        fun toJson(): String {
            val node = JsonObject(mapOf("code" to code.value(), "type" to code.name, "message" to message))
            return node.encode()
        }
    }

    fun get(e: Throwable): Response {
        val status: Status
        if (e is StatusException) {
            status = e.status
        } else if (e is StatusRuntimeException) {
            status = e.status
        } else {
            val message = if (e.message.isNullOrBlank()) "unknown error" else e.message
            status = Status.INTERNAL.withDescription(message)
        }
        val message: String
        if (status.description.isNullOrBlank()) {
            message = status.cause?.message ?: "unknown error"
        } else {
            message = status.description!!
        }

        return when (status) {
            Status.OK -> Response(200, status.code, message)
            Status.CANCELLED -> Response(408, status.code, message)
            Status.UNKNOWN -> Response(500, status.code, message)
            Status.INVALID_ARGUMENT -> Response(400, status.code, message)
            Status.DEADLINE_EXCEEDED -> Response(408, status.code, message)
            Status.NOT_FOUND -> Response(404, status.code, message)
            Status.ALREADY_EXISTS -> Response(409, status.code, message)
            Status.PERMISSION_DENIED -> Response(403, status.code, message)
            Status.UNAUTHENTICATED -> Response(401, status.code, message)
            Status.RESOURCE_EXHAUSTED -> Response(403, status.code, message)
            Status.FAILED_PRECONDITION -> Response(412, status.code, message)
            Status.ABORTED -> Response(409, status.code, message)
            Status.OUT_OF_RANGE -> Response(400, status.code, message)
            Status.UNIMPLEMENTED -> Response(501, status.code, message)
            Status.INTERNAL -> Response(500, status.code, message)
            Status.UNAVAILABLE -> Response(503, status.code, message)
            Status.DATA_LOSS -> Response(500, status.code, message)
            else -> Response(500, status.code, message)
        }
    }
}