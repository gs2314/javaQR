package app;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

public final class PrinterUtil {
    private PrinterUtil() {
    }

    public static void printBarcode(BufferedImage image, PrintOptions options) throws PrinterException {
        PrinterJob job = PrinterJob.getPrinterJob();
        String title = options.title() == null || options.title().isBlank() ? "GS1 DataMatrix" : options.title();
        job.setJobName(title);
        job.setCopies(options.copies());
        PageFormat pageFormat = job.defaultPage();
        job.setPrintable(new BarcodePrintable(image, options), pageFormat);
        if (job.printDialog()) {
            job.print();
        }
    }

    private static class BarcodePrintable implements Printable {
        private final BufferedImage image;
        private final PrintOptions options;

        private BarcodePrintable(BufferedImage image, PrintOptions options) {
            this.image = image;
            this.options = options;
        }

        @Override
        public int print(java.awt.Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
            if (pageIndex > 0) {
                return NO_SUCH_PAGE;
            }
            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            double targetPixels = options.dpi() * (options.sizeMillimetres() / 25.4);
            double scaleX = targetPixels / image.getWidth();
            double scaleY = targetPixels / image.getHeight();
            double scale = Math.min(scaleX, scaleY);

            int drawWidth = (int) Math.round(image.getWidth() * scale);
            int drawHeight = (int) Math.round(image.getHeight() * scale);

            int yOffset = 0;
            if (options.title() != null && !options.title().isBlank()) {
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
                g2.drawString(options.title(), 0, g2.getFontMetrics().getAscent());
                yOffset = g2.getFontMetrics().getHeight() + 5;
            }

            g2.drawImage(image, 0, yOffset, drawWidth, yOffset + drawHeight, 0, 0, image.getWidth(), image.getHeight(), null);

            if (options.includeHri() && options.hriText() != null && !options.hriText().isBlank()) {
                int textY = yOffset + drawHeight + g2.getFontMetrics().getHeight();
                g2.setFont(g2.getFont().deriveFont(10f));
                g2.drawString(options.hriText(), 0, textY);
            }
            return PAGE_EXISTS;
        }
    }
}
