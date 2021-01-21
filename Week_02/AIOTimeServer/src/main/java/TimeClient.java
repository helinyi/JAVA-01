import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class TimeClient {
    private AsynchronousSocketChannel channel;
    private CountDownLatch latch;

    public static void main(String[] args) {
        TimeClient tc = new TimeClient();
        tc.doit();
    }

    public void doit() {
        latch = new CountDownLatch(1);
        int port = 8080;
        try {
            channel = AsynchronousSocketChannel.open();
            // another style with future
            Future<Void> future = channel.connect(new InetSocketAddress("127.0.0.1", port));
            future.get();

            String req = "query time order";
            byte[] bytes = req.getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(req.length());

            writeBuffer.put(bytes);
            writeBuffer.flip();
            channel.write(writeBuffer, writeBuffer, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    // write till all has been written
                    if (attachment.hasRemaining())
                        channel.write(attachment, attachment, this);
                    // TODO: 半包读写问题
                    // ByteBuffer readBuffer = ByteBuffer.allocate(30);
                    ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                    channel.read(readBuffer, readBuffer, new CompletionHandler<Integer, ByteBuffer>() {
                        @Override
                        public void completed(Integer result, ByteBuffer attachment) {
                            attachment.flip();
                            byte[] bytes = new byte[attachment.remaining()];
                            attachment.get(bytes);
                            try {
                                String resp = new String(bytes, "UTF-8");
                                System.out.println("client getting response: " + resp);
                                channel.close();
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                // countdown no matter
                                latch.countDown();
                            }
                        }

                        @Override
                        public void failed(Throwable exc, ByteBuffer attachment) {
                            System.out.println("client read failed");
                            try {
                                channel.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            latch.countDown();
                        }
                    });
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    try {
                        channel.close();
                        latch.countDown();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            // adding latch.await() here because channel write returns immediately and may proceed and end this program
            latch.await();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}
