/**
 * Implements Least Significant Bit (LSB) steganography for embedding and extracting data.
 * This class handles the core steganography operations for hiding data in carrier bytes
 * and extracting hidden data from carrier bytes.
 */
public class LsbSteganography {
    private static final int BYTE_SIZE = 8;
    private static final int MAX_BYTE_VALUE = 255;

    /**
     * Embeds data into the LSB of each pixel in a BMP 8bpp image, skipping padding bytes.
     * @param carrierData The carrier BMP pixel data (including padding)
     * @param width The width of the BMP image
     * @param height The height of the BMP image
     * @param dataToHide The data to hide
     * @param isBorder Array indicating which bytes are border cases (255)
     * @return Modified carrier data with hidden information
     */
    public static byte[] embed(byte[] carrierData, int width, int height, byte[] dataToHide, boolean[] isBorder) {
        int rowSize = ((width + 3) / 4) * 4;
        byte[] modified = carrierData.clone();
        int totalBitsToHide = dataToHide.length * 8;
        int bitIdx = 0;

        for (int y = 0; y < height && bitIdx < totalBitsToHide; y++) {
            int rowOffset = y * rowSize;
            for (int x = 0; x < width && bitIdx < totalBitsToHide; x++) {
                int byteIndex = bitIdx / 8;
                int bitInByte = 7 - (bitIdx % 8);
                int bitToHide = (dataToHide[byteIndex] >> bitInByte) & 1;

                // Modifica solo el LSB del píxel real
                modified[rowOffset + x] = (byte) ((modified[rowOffset + x] & 0xFE) | bitToHide);

                // Si es un byte de borde, setea el MSB según isBorder
                if ((bitIdx % 8 == 0) && dataToHide[byteIndex] == (byte)255) {
                    if (isBorder[byteIndex]) {
                        modified[rowOffset + x] |= (byte)0x80;
                    } else {
                        modified[rowOffset + x] &= (byte)0x7F;
                    }
                }

                bitIdx++;
            }
        }
        return modified;
    }

    /**
     * Extracts hidden data from carrier bytes using LSB steganography.
     * 
     * @param carrierData The carrier bytes containing hidden data
     * @param numBytes Number of bytes to extract
     * @param isBorder Array to store border case information
     * @return Extracted data bytes
     */
    public static byte[] extract(byte[] carrierData, int numBytes, boolean[] isBorder) {
        if (carrierData == null || isBorder == null) {
            throw new IllegalArgumentException("Input arrays cannot be null");
        }
        if (numBytes <= 0) {
            throw new IllegalArgumentException("Number of bytes to extract must be positive");
        }

        int totalBitsToExtract = numBytes * BYTE_SIZE;
        int carrierCapacity = carrierData.length;

        byte[] result = new byte[numBytes];
        
        for (int bitIndex = 0; bitIndex < totalBitsToExtract; bitIndex++) {
            int bitPosition = (bitIndex / carrierCapacity) % BYTE_SIZE;
            int carrierIndex = bitIndex % carrierCapacity;
            int bit = (carrierData[carrierIndex] >> bitPosition) & 1;

            int byteIndex = bitIndex / BYTE_SIZE;
            result[byteIndex] = (byte) ((result[byteIndex] << 1) | bit);

            // Handle border case detection
            if (bitIndex % BYTE_SIZE == BYTE_SIZE - 1) {
                int msbPosition = BYTE_SIZE - 1 - ((bitIndex / carrierCapacity) % BYTE_SIZE);
                int msb = (carrierData[carrierIndex] >> msbPosition) & 1;
                isBorder[byteIndex] = (msb == 1);
            }
        }

        return result;
    }
}