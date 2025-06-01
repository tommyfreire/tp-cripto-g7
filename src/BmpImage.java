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

        pixelData = Arrays.copyOfRange(fullData, offset, fullData.length);
    }

    public BmpImage(byte[] header, byte[] pixelData) {
        this.header = header;
        this.pixelData = pixelData;
        this.offset = header.length;
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
