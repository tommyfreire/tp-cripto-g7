public class LsbSteganography {

    public static byte[] embed(byte[] carrierData, byte[] dataToHide) {
        byte[] modified = carrierData.clone();
        int carrierCapacity = carrierData.length;
        int bitIndex = 0;

        for (int i = 0; i < dataToHide.length; i++) {
            int value = Byte.toUnsignedInt(dataToHide[i]);

            // Embed 8 bits of the value
            for (int bit = 0; bit < 8; bit++) {
                int carrierIndex = bitIndex % carrierCapacity;
                int bitPosition = (bitIndex / carrierCapacity) % 8;

                int bitToHide = (value >> (7 - bit)) & 1;

                modified[carrierIndex] &= (byte) ~(1 << bitPosition); // Clear the bit
                modified[carrierIndex] |= (byte) (bitToHide << bitPosition); // Set the bit

                bitIndex++;
            }

        }

        return modified;
    }

    public static byte[] extract(byte[] carrierData, int numBytes) {

        byte[] result = new byte[numBytes];
        int carrierCapacity = carrierData.length;
        int bitIndex = 0;

        for (int i = 0; i < numBytes; i++) {
            int value = 0;

            // Extract 8 bits of the value
            for (int bit = 0; bit < 8; bit++) {
                int carrierIndex = bitIndex % carrierCapacity;
                int bitPosition = (bitIndex / carrierCapacity) % 8;

                int bitValue = (carrierData[carrierIndex] >> bitPosition) & 1;
                value = (value << 1) | bitValue;

                bitIndex++;
            }

            result[i] = (byte) value;

        }
        return result;

    }
}