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

    /**
     * Sets a 2-byte value in the header at the specified position in little endian format.
     * @param position The starting position in the header (0-based)
     * @param value The 2-byte value to store (unsigned 16-bit)
     */
    public void setReservedBytes(int position, int value) {
        // Store in little endian format (unsigned 16-bit)
        header[position] = (byte) (value & 0xFF);         // Least significant byte
        header[position + 1] = (byte) ((value >> 8) & 0xFF); // Most significant byte
    }

    /**
     * Gets a 2-byte value from the header at the specified position in little endian format.
     * @param position The starting position in the header (0-based)
     * @return The 2-byte value (unsigned 16-bit)
     */
    public int getReservedBytes(int position) {
        // Read in little endian format (unsigned 16-bit)
        return ((header[position + 1] & 0xFF) << 8) | (header[position] & 0xFF);
    }

    /**
     * Gets the width of the image from the header.
     * @return The width in pixels
     */
    public int getWidth() {
        return getIntFromHeader(18);
    }

    /**
     * Gets the height of the image from the header.
     * @return The height in pixels
     */
    public int getHeight() {
        return getIntFromHeader(22);
    }

    /**
     * Gets a 4-byte integer from the header at the specified position in little endian format.
     * @param position The starting position in the header (0-based)
     * @return The 4-byte integer value
     */
    public int getIntFromHeader(int position) {
        return ((header[position + 3] & 0xFF) << 24) |
               ((header[position + 2] & 0xFF) << 16) |
               ((header[position + 1] & 0xFF) << 8) |
               (header[position] & 0xFF);
    }

    /**
     * Checks if this image has the same dimensions as another image.
     * @param other The other image to compare with
     * @return true if both images have the same width and height
     */
    public boolean hasSameDimensions(BmpImage other) {
        return this.getWidth() == other.getWidth() && 
               this.getHeight() == other.getHeight();
    }

    /**
     * Crops the image to the specified dimensions, taking the central pixels.
     * @param targetWidth The desired width
     * @param targetHeight The desired height
     * @return A new BmpImage with the cropped dimensions
     */
    public BmpImage cropToSize(int targetWidth, int targetHeight) {
        int currentWidth = getWidth();
        int currentHeight = getHeight();

        int startX = (currentWidth - targetWidth) / 2;
        int startY = (currentHeight - targetHeight) / 2;

        // BMP: filas de abajo hacia arriba, con padding por fila
        int currentRowSize = ((currentWidth + 3) / 4) * 4; // bytes por fila con padding
        int targetRowSize = ((targetWidth + 3) / 4) * 4;   // bytes por fila con padding

        byte[] newPixelData = new byte[targetRowSize * targetHeight];

        for (int y = 0; y < targetHeight; y++) {
            int sourceY = startY + y;
            int sourceRow = currentHeight - 1 - sourceY; // invertido
            int targetRow = targetHeight - 1 - y;        // invertido

            int sourceRowOffset = sourceRow * currentRowSize;
            int targetRowOffset = targetRow * targetRowSize;

            // Copiar los píxeles centrales de la fila
            System.arraycopy(
                pixelData,
                sourceRowOffset + startX,
                newPixelData,
                targetRowOffset,
                targetWidth
            );
            // El resto del targetRow (si targetWidth no es múltiplo de 4) queda en 0 (padding)
        }

        // Crear nuevo header
        byte[] newHeader = header.clone();
        // Actualizar ancho y alto
        newHeader[18] = (byte) (targetWidth & 0xFF);
        newHeader[19] = (byte) ((targetWidth >> 8) & 0xFF);
        newHeader[20] = (byte) ((targetWidth >> 16) & 0xFF);
        newHeader[21] = (byte) ((targetWidth >> 24) & 0xFF);

        newHeader[22] = (byte) (targetHeight & 0xFF);
        newHeader[23] = (byte) ((targetHeight >> 8) & 0xFF);
        newHeader[24] = (byte) ((targetHeight >> 16) & 0xFF);
        newHeader[25] = (byte) ((targetHeight >> 24) & 0xFF);

        // Actualizar tamaño de archivo
        int newFileSize = newHeader.length + newPixelData.length;
        newHeader[2] = (byte) (newFileSize & 0xFF);
        newHeader[3] = (byte) ((newFileSize >> 8) & 0xFF);
        newHeader[4] = (byte) ((newFileSize >> 16) & 0xFF);
        newHeader[5] = (byte) ((newFileSize >> 24) & 0xFF);

        // Actualizar tamaño de la imagen (bytes 34-37)
        newHeader[34] = (byte) (newPixelData.length & 0xFF);
        newHeader[35] = (byte) ((newPixelData.length >> 8) & 0xFF);
        newHeader[36] = (byte) ((newPixelData.length >> 16) & 0xFF);
        newHeader[37] = (byte) ((newPixelData.length >> 24) & 0xFF);

        return new BmpImage(newHeader, newPixelData);
    }
}
