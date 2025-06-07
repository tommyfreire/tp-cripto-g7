public class LsbSteganography {

    public static byte[] embed(byte[] carrierData, byte[] dataToHide, boolean[] isBorder) {
        byte[] modified = carrierData.clone();
        int carrierCapacity = carrierData.length;
        int totalBitsToHide = dataToHide.length * 8;

        int bitIndex = 0;

        while (bitIndex < totalBitsToHide) {
            int bitPosition = bitIndex / carrierCapacity; // Which bit to use (0 = LSB, 1 = 2nd bit, ...)
            if (bitPosition >= 8) {
                throw new IllegalArgumentException("No hay suficientes bits disponibles para ocultar toda la información.");
            }

            int carrierIndex = bitIndex % carrierCapacity;

            int byteIndex = bitIndex / 8;
            int bitInByte = 7 - (bitIndex % 8);

            int bitToHide = (dataToHide[byteIndex] >> bitInByte) & 1;

            modified[carrierIndex] &= (byte) ~(1 << bitPosition);
            modified[carrierIndex] |= (byte) (bitToHide << bitPosition);

            // Handle the MSB for "border" cases
            if (dataToHide[byteIndex] == (byte) 255) { // First byte of the group
                if (isBorder[byteIndex]) {
                    modified[carrierIndex] |= (byte) (1 << 7); // Set MSB to 1
                } else {
                    modified[carrierIndex] &= (byte) ~(1 << 7); // Set MSB to 0
                }
            }

            bitIndex++;
        }

        return modified;
    }

    public static byte[] extract(byte[] carrierData, int numBytes, boolean[] isBorder) {
        int totalBitsToExtract = numBytes * 8;
        int carrierCapacity = carrierData.length;

        byte[] result = new byte[numBytes];
        int bitIndex = 0;

        while (bitIndex < totalBitsToExtract) {
            int bitPosition = bitIndex / carrierCapacity; // Which bit to read
            if (bitPosition >= 8) {
                throw new IllegalArgumentException("No hay suficientes datos disponibles para extraer toda la información.");
            }

            int carrierIndex = bitIndex % carrierCapacity;
            int bit = (carrierData[carrierIndex] >> bitPosition) & 1;

            int byteIndex = bitIndex / 8;
            result[byteIndex] = (byte) ((result[byteIndex] << 1) | bit);

            // Handle the MSB for "border" cases
            if (bitIndex % 8 == 7) { // First byte of the group
                int msb = (carrierData[carrierIndex] >> 7) & 1;
                isBorder[byteIndex] = (msb == 1);
            }

            bitIndex++;
        }

        return result;
    }
}
