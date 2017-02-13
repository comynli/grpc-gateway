import com.github.comynli.grpc.gateway.Bootstrap
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import io.grpc.ManagedChannelBuilder
import io.vertx.core.VertxOptions
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory

/**
 * Created by xuemingli on 2017/2/9.
 */
class Application {
    private val config = ConfigFactory.load()
    private val logger = LoggerFactory.getLogger(javaClass)
    private val bootstrap: Bootstrap

    init {
        val vertxOptions = if (config.hasPath("vertx")) VertxOptions(configToJson(config.getConfig("vertx"))) else VertxOptions()
        val httpServerOptions = if (config.hasPath("http")) HttpServerOptions(configToJson(config.getConfig("http"))) else HttpServerOptions()

        bootstrap = Bootstrap({
            ManagedChannelBuilder.forTarget(it)
                    .usePlaintext(true)
                    .build()
        }, vertxOptions, httpServerOptions)
    }

    private fun configToJson(config: Config): JsonObject {
        val s = config.root().render(ConfigRenderOptions.concise())
        return JsonObject(s)
    }

    fun start() {
        config.getConfigList("service").forEach {
            val mountPoint = if (it.hasPath("mount")) it.getString("mount") else "/"
            val proto = it.getString("proto")
            val upstream = it.getString("upstream")
            try {
                bootstrap.register(upstream, proto, mountPoint)
            } catch (e: Exception) {
                logger.error("register $proto to $upstream error", e)
            }
        }
        bootstrap.start()
        Runtime.getRuntime().addShutdownHook(Thread({ shutdown() }))
    }

    fun shutdown() {
        bootstrap.shutdown()
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            Application().start()
        }
    }
}