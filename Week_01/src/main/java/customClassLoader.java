import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class customClassLoader extends ClassLoader {
    private static final String INPUT_FILE_NAME = "src/main/resources/Hello.xlass";
    private static final String OUTPUT_FILE_NAME = "src/main/resources/Hello.class";

    public static void main(String[] args) {
        try {
            Class<?> helloClass = new customClassLoader().findClass("Hello");
            Object hello = helloClass.newInstance();
            Method m = helloClass.getDeclaredMethod("hello");
            m.invoke(hello);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = null;

        try {
            Path sourcePath = Paths.get("." + File.separator + INPUT_FILE_NAME);
            bytes = Files.readAllBytes(sourcePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 255 - byte
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (255 - bytes[i]);
        }

        // write out for verification
        String outputPath = "." + File.separator + OUTPUT_FILE_NAME;
        try (FileOutputStream stream = new FileOutputStream(outputPath)) {
            stream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return defineClass(name, bytes, 0, bytes.length);
    }
}
