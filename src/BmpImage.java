import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;

public class BmpImage {
    private byte[] header;
    private byte[] pixelData;
    private int offset;
    private int width;
    private int height;

    public BmpImage(String path) throws IOException {
        byte[] fullData = Files.readAllBytes(new File(path).toPath());

        // Leer offset al comienzo de los datos (bytes 10–13)
        offset = ((fullData[13] & 0xFF) << 24) | ((fullData[12] & 0xFF) << 16) |
                ((fullData[11] & 0xFF) << 8) | (fullData[10] & 0xFF);

        // Leer ancho (bytes 18–21) y alto (bytes 22–25)
        width = ((fullData[21] & 0xFF) << 24) | ((fullData[20] & 0xFF) << 16) |
                ((fullData[19] & 0xFF) << 8) | (fullData[18] & 0xFF);
        height = ((fullData[25] & 0xFF) << 24) | ((fullData[24] & 0xFF) << 16) |
                ((fullData[23] & 0xFF) << 8) | (fullData[22] & 0xFF);

        // Leer encabezado completo
        header = Arrays.copyOfRange(fullData, 0, offset);

        // Leer datos de píxeles
        pixelData = Arrays.copyOfRange(fullData, offset, fullData.length);
    }

    public BmpImage(byte[] header, byte[] pixelData) {
        this.header = header;
        this.pixelData = pixelData;
        this.offset = header.length;
    }

    public byte[] getHeader() {
        return header;
    }

    public byte[] getPixelData() {
        return pixelData;
    }

    public int getOffset() {
        return offset;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
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
