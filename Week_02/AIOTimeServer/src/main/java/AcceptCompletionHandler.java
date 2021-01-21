import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class AcceptCompletionHandler implements CompletionHandler<AsynchronousSocketChannel, AsyncTimeServerHandler> {
    @Override
    public void completed(AsynchronousSocketChannel result, AsyncTimeServerHandler attachment) {
        // continue accepting when this one is accepted, forming a loop
        // this is exactly the same as the content of doAccept
        attachment.asynchronousServerSocketChannel.accept(attachment, this);

        // when accept is done, start reading immediately; this is more streamlined than I/O multiplexing
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        // asynchronous read: when read is completed, ReadCompletionHandler is triggered
        // CompletionHandler<Integer,? super A> handler
        result.read(buffer, buffer, new ReadCompletionHandler(result));
    }

    @Override
    public void failed(Throwable exc, AsyncTimeServerHandler attachment) {
        exc.printStackTrace();
        // when countdown, AsyncTimeServerHandler thread proceeds and ends itself
        attachment.latch.countDown();
    }
}
