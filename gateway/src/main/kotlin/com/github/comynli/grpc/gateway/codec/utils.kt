package com.github.comynli.grpc.gateway.codec

import com.google.protobuf.DynamicMessage
import com.google.protobuf.util.JsonFormat

/**
 * Created by xuemingli on 2017/2/10.
 */
fun messageToJson(message: DynamicMessage) = JsonFormat.printer().includingDefaultValueFields().print(message)!!