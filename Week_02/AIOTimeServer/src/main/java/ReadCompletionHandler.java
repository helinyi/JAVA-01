import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Date;

public class ReadCompletionHandler implements CompletionHandler<Integer, ByteBuffer> {

    private AsynchronousSocketChannel channel;

    public ReadCompletionHandler(AsynchronousSocketChannel channel){
        this.channel = channel;
    }

    @Override
    public void completed(Integer result, ByteBuffer attachment) {
        attachment.flip();
        byte[] body = new byte[attachment.remaining()];
        // move from ByteBuffer to byte array
        attachment.get(body);
        try {
            // public String(byte bytes[], String charsetName)
            String req = new String(body, "UTF-8");
            System.out.println("The time server receive order: " + req);
            if ("BYE".equalsIgnoreCase(req.trim())) channel.close();
            String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(req.trim()) ? new Date(System.currentTimeMillis()).toString() : "BAD ORDER";
            doWrite(currentTime+'\n');
        } catch (UnsupportedEncodingException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doWrite(String currentTime) {
        if (currentTime != null && currentTime.trim().length() > 0) {
            byte[] bytes = currentTime.getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
            writeBuffer.put(bytes);
            writeBuffer.flip();

            CompletionHandler writeCompletionHandler = new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    // continue sending if not all has been sent
                    if (attachment.hasRemaining())
                        channel.write(attachment, attachment, this);
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    try {
                        // just close it if failed
                        channel.close();
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                }
            };
            channel.write(writeBuffer, writeBuffer, writeCompletionHandler);
            // loop reading
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            channel.read(buffer, buffer, this);
        }
    }

    @Override
    public void failed(Throwable exc, ByteBuffer attachment) {
        try {
            this.channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
