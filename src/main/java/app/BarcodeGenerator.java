package app;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.datamatrix.DataMatrixWriter;
import com.google.zxing.common.BitMatrix;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

public final class BarcodeGenerator {
    private BarcodeGenerator() {
    }

    public static BufferedImage generateDataMatrix(String data, int size) throws WriterException {
        DataMatrixWriter writer = new DataMatrixWriter();
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.GS1_FORMAT, Boolean.TRUE);
        BitMatrix matrix = writer.encode(data, BarcodeFormat.DATA_MATRIX, size, size, hints);
        return toImage(matrix);
    }

    private static BufferedImage toImage(BitMatrix matrix) {
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
}
