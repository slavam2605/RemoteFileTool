import java.io.*;
import java.nio.file.Files;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author Moklev Vyacheslav
 */
public class ProgressCopy {
    private Predicate<Long> callback;
    private File source;
    private String destPath;
    private PrintWriter pw;
    private long copied;
    private boolean cancel;

    public ProgressCopy(File source, String destPath, PrintWriter pw, Predicate<Long> callback) {
        this.callback = callback;
        this.source = source;
        this.destPath = new File(destPath).toString();
        this.pw = pw;
    }

    public void copy() {
        copied = 0;
        cancel = false;
        innerCopy(source, destPath);
    }

    private void innerCopy(File file, String dest) {
        System.out.println("innerCopy(" + file + ", " + dest + ")");
        if (file.isFile()) {
            rawCopy(file, dest);
        } else if (file.isDirectory()) {
            dest += File.separator + file.getName();
            try {
                Files.createDirectories(new File(dest).toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            File[] files = file.listFiles();
            if (files == null)
                throw new IllegalStateException("Null list for directory");
            for (File inFile: files) {
                if (inFile.isDirectory()) {
                    innerCopy(inFile, dest);
                    if (cancel)
                        return;
                } else {
                    rawCopy(inFile, dest);
                    if (cancel)
                        return;
                }
            }
        } else {
            throw new IllegalArgumentException("Unknown type: not file and not directory");
        }
    }

    private void rawCopy(File file, String dest) {
        System.out.println("rawCopy(" + file + ", " + dest + ")");
        try {
            dest += File.separator + file.getName();
            long fullSize = file.length();
            final int BLOCK_SIZE = fullSize < 1024 * 1024 * 100 ? 1024 : 1024 * 1024;
            byte[] buffer = new byte[BLOCK_SIZE];
            long count = 0;
            InputStream is = new FileInputStream(file);
            OutputStream os = new FileOutputStream(dest);
            while (count < fullSize) {
                int readBytes = is.read(buffer);
                if (readBytes < 0)
                    break;
                os.write(buffer, 0, readBytes);
                count += readBytes;
                copied += readBytes;
                cancel = !callback.test(copied);
                if (cancel)
                    break;
            }
            is.close();
            os.close();
            if (!cancel && count != fullSize)
                throw new IllegalStateException("End of read before EOF");
        } catch (IOException e) {
            e.printStackTrace();
            pw.println("error:" + e.toString());
            System.out.println("error:" + e.toString());
            cancel = true;
        }
    }
}
