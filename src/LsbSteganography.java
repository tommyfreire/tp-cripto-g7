/**
 * Implements Least Significant Bit (LSB) steganography for embedding and extracting data.
 * This class handles the core steganography operations for hiding data in carrier bytes
 * and extracting hidden data from carrier bytes.
 */
public class LsbSteganography {
    private static final int BYTE_SIZE = 8;
    private static final int MAX_BYTE_VALUE = 255;

    /**
     * Embeds data into carrier bytes using LSB steganography.
     * 
     * @param carrierData The carrier bytes where data will be hidden
     * @param dataToHide The data to be hidden in the carrier
     * @param isBorder Array indicating which bytes are border cases (255)
     * @return Modified carrier data with hidden information
     */
    public static byte[] embed(byte[] carrierData, byte[] dataToHide, boolean[] isBorder) {
        if (carrierData == null || dataToHide == null || isBorder == null) {
            throw new IllegalArgumentException("Input arrays cannot be null");
        }
        if (dataToHide.length != isBorder.length) {
            throw new IllegalArgumentException("Data and border arrays must have same length");
        }

        byte[] modified = carrierData.clone();
        int carrierCapacity = carrierData.length;
        int totalBitsToHide = dataToHide.length * BYTE_SIZE;

        for (int bitIndex = 0; bitIndex < totalBitsToHide; bitIndex++) {
            int bitPosition = bitIndex / carrierCapacity; // Which bit to use (0 = LSB, 1 = 2nd bit, ...)
            int carrierIndex = bitIndex % carrierCapacity;
            int byteIndex = bitIndex / BYTE_SIZE;
            int bitInByte = BYTE_SIZE - 1 - (bitIndex % BYTE_SIZE);

            int bitToHide = (dataToHide[byteIndex] >> bitInByte) & 1;

            // Clear the target bit and set it to the new value
            modified[carrierIndex] &= (byte) ~(1 << bitPosition);
            modified[carrierIndex] |= (byte) (bitToHide << bitPosition);

            // Handle special case for byte value 255
            if (dataToHide[byteIndex] == (byte) MAX_BYTE_VALUE) {
                int msbPosition = BYTE_SIZE - 1 - ((bitIndex / carrierCapacity) % BYTE_SIZE);
                if (isBorder[byteIndex]) {
                    modified[carrierIndex] |= (byte) (1 << msbPosition);
                } else {
                    modified[carrierIndex] &= (byte) ~(1 << msbPosition);
                }
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