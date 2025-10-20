package app;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

public final class ClipboardUtil {
    private ClipboardUtil() {
    }

    public static void copyToClipboard(String text) {
        if (text == null) {
            return;
        }
        Transferable transferable = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, null);
    }
}
