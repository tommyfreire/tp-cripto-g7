import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;

public class BmpImage {
    private byte[] header;
    private byte[] pixelData;
    private int offset;

    public BmpImage(String path) throws IOException {
        byte[] fullData = Files.readAllBytes(new File(path).toPath());

        // Leer offset al comienzo de los datos (bytes 10–13)
        offset = ((fullData[13] & 0xFF) << 24) | ((fullData[12] & 0xFF) << 16) |
                ((fullData[11] & 0xFF) << 8) | (fullData[10] & 0xFF);

        header = Arrays.copyOfRange(fullData, 0, offset);

        // Check if the image is RGB or grayscale: byte 28 of the header should be 8
        if (header[28] != 8) {
            throw new IOException("The image is not grayscale");
        }

        pixelData = Arrays.copyOfRange(fullData, offset, fullData.length);
    }

    public BmpImage(byte[] header, byte[] pixelData) {
        this.header = header;
        this.pixelData = pixelData;
        this.offset = header.length;
    }

    public static byte[] extractBmpPixelMatrix(String bmpPath) throws IOException {
        try (FileInputStream fis = new FileInputStream(bmpPath)) {
            byte[] header = new byte[54];
            if (fis.read(header) != 54) throw new IOException("Invalid BMP header");

            // Leer offset (bytes 10-13, little endian)
            int offset = ((header[13] & 0xFF) << 24) | ((header[12] & 0xFF) << 16) |
                    ((header[11] & 0xFF) << 8) | (header[10] & 0xFF);

            // Leer ancho (bytes 18-21, little endian)
            int width = ((header[21] & 0xFF) << 24) | ((header[20] & 0xFF) << 16) |
                    ((header[19] & 0xFF) << 8) | (header[18] & 0xFF);

            // Leer alto (bytes 22-25, little endian)
            int height = ((header[25] & 0xFF) << 24) | ((header[24] & 0xFF) << 16) |
                    ((header[23] & 0xFF) << 8) | (header[22] & 0xFF);

            if (width <= 0 || height <= 0) {
                throw new IOException("Invalid BMP dimensions");
            }

            // Calcular tamaño por fila incluyendo padding (alineado a múltiplo de 4 bytes)
            int bytesPerPixel = 1; // para 8 bits por píxel
            int rowSize = ((width * bytesPerPixel + 3) / 4) * 4;

            // Saltar hasta el inicio de la matriz de píxeles
            fis.skip(offset - 54);

            byte[] pixelMatrix = new byte[width * height];
            byte[] row = new byte[rowSize];

            // BMP guarda las filas de abajo hacia arriba
            for (int y = height - 1; y >= 0; y--) {
                if (fis.read(row) != rowSize) throw new IOException("Unexpected EOF in pixel data");
                System.arraycopy(row, 0, pixelMatrix, y * width, width);
            }

            return pixelMatrix;
        }
    }

    public byte[] getPixelData() {
        return pixelData;
    }

    public byte[] getHeader() {
        return header;
    }

    public int getOffset() {
        return offset;
    }

    public void setPixelData(byte[] newPixelData) {
        this.pixelData = newPixelData;
    }

    public void save(String outputPath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            fos.write(header);
            fos.write(pixelData);
        }
    }

    public void setReservedBytes(int position, short value) {
        header[position] = (byte) (value & 0xFF);
        header[position + 1] = (byte) ((value >> 8) & 0xFF);
    }

    public short getReservedBytes(int position) {
        return (short) (((header[position + 1] & 0xFF) << 8) | (header[position] & 0xFF));
    }
}
