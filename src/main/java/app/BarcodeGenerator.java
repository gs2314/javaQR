package app;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.datamatrix.DataMatrixWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

public final class BarcodeGenerator {
    private BarcodeGenerator() {
    }

    public static BitMatrix encodeDataMatrix(String data, int size, int margin) throws WriterException {
        DataMatrixWriter writer = new DataMatrixWriter();
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.GS1_FORMAT, Boolean.TRUE);
        hints.put(EncodeHintType.MARGIN, margin);
        return writer.encode(data, BarcodeFormat.DATA_MATRIX, size, size, hints);
    }

    public static BufferedImage generateDataMatrix(String data, int size, int margin) throws WriterException {
        return toImage(encodeDataMatrix(data, size, margin));
    }

    public static BufferedImage toImage(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int color = matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
                image.setRGB(x, y, color);
            }
        }
        return image;
    }

    public static void writePng(BitMatrix matrix, Path output) throws IOException {
        BufferedImage image = toImage(matrix);
        ImageIO.write(image, "PNG", output.toFile());
    }

    public static String toSvg(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        StringBuilder path = new StringBuilder();
        for (int y = 0; y < height; y++) {
            int x = 0;
            while (x < width) {
                if (matrix.get(x, y)) {
                    int runStart = x;
                    while (x < width && matrix.get(x, y)) {
                        x++;
                    }
                    int runLength = x - runStart;
                    path.append("M").append(runStart).append(' ').append(y)
                            .append("h").append(runLength)
                            .append("v1h-").append(runLength)
                            .append("z");
                } else {
                    x++;
                }
            }
        }
        StringBuilder svg = new StringBuilder();
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" viewBox=\"0 0 ")
                .append(width)
                .append(' ')
                .append(height)
                .append("\">\n");
        svg.append("<rect width=\"").append(width).append("\" height=\"").append(height)
                .append("\" fill=\"#ffffff\"/>\n");
        svg.append("<path fill=\"#000000\" d=\"").append(path).append("\"/>\n");
        svg.append("</svg>\n");
        return svg.toString();
    }
}
