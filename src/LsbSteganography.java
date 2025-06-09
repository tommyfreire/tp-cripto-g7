public class LsbSteganography {

    public static byte[] embed(byte[] carrierData, byte[] dataToHide, boolean[] isBorder) {
        byte[] modified = carrierData.clone();
        int carrierCapacity = carrierData.length;
        int totalBitsToHide = dataToHide.length * 8;

        int bitIndex = 0;

        while (bitIndex < totalBitsToHide) {
            int bitPosition = bitIndex / carrierCapacity; // Which bit to use (0 = LSB, 1 = 2nd bit, ...)

            int carrierIndex = bitIndex % carrierCapacity;

            int byteIndex = bitIndex / 8;
            int bitInByte = 7 - (bitIndex % 8);

            int bitToHide = (dataToHide[byteIndex] >> bitInByte) & 1;

            modified[carrierIndex] &= (byte) ~(1 << bitPosition);
            modified[carrierIndex] |= (byte) (bitToHide << bitPosition);

            if (dataToHide[byteIndex] == (byte) 255) {
                int msbPosition = 7 - ((bitIndex / carrierCapacity) % 8); // Cycle through MSB, 2nd MSB, ...

                if (isBorder[byteIndex]) {
                    modified[carrierIndex] |= (byte) (1 << msbPosition); // Set the current MSB to 1
                } else {
                    modified[carrierIndex] &= (byte) ~(1 << msbPosition); // Set the current MSB to 0
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
            int bitPosition = (bitIndex / carrierCapacity) % 8;

            int carrierIndex = bitIndex % carrierCapacity;
            int bit = (carrierData[carrierIndex] >> bitPosition) & 1;

            int byteIndex = bitIndex / 8;
            result[byteIndex] = (byte) ((result[byteIndex] << 1) | bit);

            // Handle the MSB for "border" cases
            if (bitIndex % 8 == 7) { // First byte of the group
                int msbPosition = 7 - ((bitIndex / carrierCapacity) % 8); // Cycle through MSB, 2nd MSB, ...
                int msb = (carrierData[carrierIndex] >> msbPosition) & 1;
                isBorder[byteIndex] = (msb == 1);
            }

            bitIndex++;
        }

        return result;
    }
}
