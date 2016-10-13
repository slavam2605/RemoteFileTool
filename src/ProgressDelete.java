import java.io.File;
import java.io.PrintWriter;
import java.util.function.Predicate;

/**
 * @author Moklev Vyacheslav
 */
public class ProgressDelete {
    private File source;
    private PrintWriter pw;
    private Predicate<Long> callback;
    private boolean cancel;
    private long deleted;

    public ProgressDelete(File source, PrintWriter pw, Predicate<Long> callback) {
        this.source = source;
        this.pw = pw;
        this.callback = callback;
    }

    public void delete() {
        deleted = 0;
        cancel = false;
        innerDelete(source);
    }

    private void innerDelete(File file) {
        if (file.isFile()) {
            if (!file.delete()) {
                cancel = true;
                pw.println("error:Невозможно удалить " + file.getName());
            } else {
                deleted++;
                cancel = !callback.test(deleted);
            }
        } else if (file.isDirectory()) {
            File[] tmp = file.listFiles();
            if (tmp != null) {
                for (File inFile: tmp) {
                    innerDelete(inFile);
                    if (cancel)
                        return;
                }
                if (!file.delete()) {
                    cancel = true;
                    pw.println("error:Невозможно удалить папку " + file.getName());
                } else {
                    deleted++;
                    cancel = !callback.test(deleted);
                }
            }
        } else {
            throw new IllegalArgumentException("Unknown type: not file and not directory");
        }
    }
}
