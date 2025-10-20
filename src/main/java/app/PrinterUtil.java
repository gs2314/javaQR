package app;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

public final class PrinterUtil {
    private PrinterUtil() {
    }

    public static void printBarcode(BufferedImage image, String title, int dpi) throws PrinterException {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName(title == null || title.isBlank() ? "GS1 DataMatrix" : title);
        PageFormat pageFormat = job.defaultPage();
        job.setPrintable(new BarcodePrintable(image, title, dpi), pageFormat);
        if (job.printDialog()) {
            job.print();
        }
    }

    private static class BarcodePrintable implements Printable {
        private final BufferedImage image;
        private final String title;
        private final int dpi;

        private BarcodePrintable(BufferedImage image, String title, int dpi) {
            this.image = image;
            this.title = title;
            this.dpi = dpi;
        }

        @Override
        public int print(java.awt.Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
            if (pageIndex > 0) {
                return NO_SUCH_PAGE;
            }
            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            int targetPixels = (int) Math.round((dpi / 25.4) * 40.0);
            double scaleX = targetPixels / (double) image.getWidth();
            double scaleY = targetPixels / (double) image.getHeight();
            double scale = Math.min(scaleX, scaleY);

            int drawWidth = (int) Math.round(image.getWidth() * scale);
            int drawHeight = (int) Math.round(image.getHeight() * scale);

            int yOffset = 0;
            if (title != null && !title.isBlank()) {
                g2.setFont(g2.getFont().deriveFont(12f));
                g2.drawString(title, 0, g2.getFontMetrics().getAscent());
                yOffset = g2.getFontMetrics().getHeight() + 5;
            }

            g2.drawImage(image, 0, yOffset, drawWidth, yOffset + drawHeight, 0, 0, image.getWidth(), image.getHeight(), null);
            return PAGE_EXISTS;
        }
    }
}
