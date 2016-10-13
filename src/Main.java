import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Moklev Vyacheslav
 */
public class Main {

    public static final String DOWNLOAD_ROOT = "C:/";
    public static final String FILESYSTEM_ROOT = "C:/";

    public static void main(String[] args) throws IOException {
        ServerSocket ss = new ServerSocket(12345);
        while (true) {
            Socket s = ss.accept();
            System.err.println("Socket accepted");
            new Thread(new SocketProcessor(s)).start();
        }
    }

    private static class SocketProcessor implements Runnable {
        private Socket s;
        private BufferedReader br;
        private PrintWriter pw;

        private SocketProcessor(Socket s) throws IOException {
            this.s = s;
            this.br = new BufferedReader(new InputStreamReader(s.getInputStream()));
            this.pw = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true);
        }

        @Override
        public void run() {
            try {
                br.lines().forEach(line -> {
                    try {
                        boolean unknown = false;
                        System.out.print(line + "... ");
                        if (line.equals("list")) {
                            pw.println(
                                    Files.list(new File(DOWNLOAD_ROOT).toPath())
                                            .map(path -> path.getName(path.getNameCount() - 1).toString())
                                            .collect(Collectors.joining("/"))
                            );
                        } else
                        if (line.equals("media")) {
                            pw.println(
                                    Arrays.stream(File.listRoots()).map(File::toPath)
                                            .map(path -> path.toAbsolutePath().toString())
                                            .collect(Collectors.joining("/"))
                            );
                        } else
                        if (line.equals("space")) {
                            File file = new File(FILESYSTEM_ROOT);
                            pw.println(
                                    (file.getFreeSpace() / 1073741824.0) +
                                            "/" +
                                            (file.getTotalSpace() / 1073741824.0)
                            );
                        } else
                        if (line.startsWith("ext_used")) {
                            String fileName = line.substring(9);
                            File file = new File(fileName);
                            pw.println(file.getFreeSpace() + "/" + file.getTotalSpace());
                        } else
                        if (line.startsWith("used_space")) {
                            String fileName = line.substring(11);
                            File file = new File(DOWNLOAD_ROOT + fileName);
                            if (file.isFile()) {
                                pw.println(file.length() / 1073741824.0);
                            } else {
                                pw.println(getDirSize(file) / 1073741824.0);
                            }
                        } else if (line.startsWith("copy")) {
                            String[] parts = line.split(";");
                            String fileName = DOWNLOAD_ROOT + parts[1];
                            String destPath = parts[2];
                            File file = new File(fileName);
                            long size;
                            if (file.isFile()) {
                                size = file.length();
                            } else {
                                size = getDirSize(file);
                            }
                            new ProgressCopy(file, destPath, pw, getCallback(size)).copy();
                            pw.println("end");
                        } else if (line.startsWith("delete")) {
                            String fileName = DOWNLOAD_ROOT + line.substring(7);
                            File file = new File(fileName);
                            long count = getFileCount(file);
                            new ProgressDelete(file, pw, getCallback(count)).delete();
                            pw.println("end");
                        } else {
                            unknown = true;
                        }
                        System.out.println(unknown ? "unknown message." : "done.");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    s.close();
                } catch (IOException ignored) {}
                System.err.println("Socket closed");
            }
        }

        private Predicate<Long> getCallback(long count) {
            return deleted -> {
                try {
                    double progress = (double) deleted / count;
                    pw.println(progress);
                    String response = br.readLine();
                    return !response.equals("cancel");
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            };
        }

        private long getDirSize(File dir) {
            long size = 0;
            File[] tmp = dir.listFiles();
            if ( tmp != null ) {
                for (File file: tmp) {
                    if (file.isFile())
                        size += file.length();
                    else
                        size += getDirSize(file);
                }
            }
            return size;
        }

        private long getFileCount(File dir) {
            if (dir.isFile())
                return 1;
            long size = 0;
            File[] tmp = dir.listFiles();
            if (tmp != null) {
                for (File file: tmp) {
                    if (file.isFile())
                        size++;
                    else
                        size += getFileCount(file);
                }
            }
            return size;
        }
    }
}
