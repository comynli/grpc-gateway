import com.github.comynli.grpc.proto.GreeterGrpc
import com.github.comynli.grpc.proto.HelloRequest
import io.grpc.stub.StreamObserver
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by xuemingli on 2017/2/13.
 */

class Endpoint : GreeterGrpc.GreeterImplBase() {
    override fun sayHelloClientStream(responseObserver: StreamObserver<HelloRequest>): StreamObserver<HelloRequest> {
        return object : StreamObserver<HelloRequest> {
            override fun onCompleted() {
                responseObserver.onCompleted()
            }

            override fun onNext(value: HelloRequest) {
                responseObserver.onNext(HelloRequest.newBuilder(value).setAge(value.age + 1).build())
            }

            override fun onError(t: Throwable) {
                responseObserver.onError(t)
            }
        }
    }

    override fun sayHelloServerStream(request: HelloRequest, responseObserver: StreamObserver<HelloRequest>) {
        val executor = Executors.newSingleThreadExecutor()
        val age = AtomicInteger(request.age)

        executor.execute {
            (0..10).forEach {
                responseObserver.onNext(HelloRequest.newBuilder(request).setAge(age.incrementAndGet()).build())
                Thread.sleep(1000)
            }
        }
    }

    override fun sayHelloBidiStream(responseObserver: StreamObserver<HelloRequest>): StreamObserver<HelloRequest> {
        return object : StreamObserver<HelloRequest> {
            override fun onCompleted() {
                responseObserver.onCompleted()
            }

            override fun onNext(value: HelloRequest) {
                responseObserver.onNext(HelloRequest.newBuilder(value).setAge(value.age + 1).build())
            }

            override fun onError(t: Throwable) {
                responseObserver.onError(t)
            }
        }
    }

    override fun sayHello(request: HelloRequest, responseObserver: StreamObserver<HelloRequest>) {
        responseObserver.onNext(HelloRequest.newBuilder(request).setAge(request.age + 1).build())
        responseObserver.onCompleted()
    }

}