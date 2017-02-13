import io.grpc.ServerBuilder

/**
 * Created by xuemingli on 2017/2/13.
 */
class Application {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val server = ServerBuilder
                    .forPort(10052)
                    .addService(Endpoint())
                    .build()
            Runtime.getRuntime().addShutdownHook(Thread({server.shutdown()}))
            server.start()
            server.awaitTermination()
        }
    }
}