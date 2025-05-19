public class LsbSteganography {

    /**
     * Oculta los bits de 'dataToHide' dentro de los bits menos significativos de cada byte de 'carrierData'.
     * Si no hay espacio suficiente en los LSB, usa progresivamente el segundo, tercer... hasta octavo bit menos significativo.
     */
    public static byte[] embed(byte[] carrierData, byte[] dataToHide) {
<<<<<<< HEAD
=======
        if (carrierData.length < dataToHide.length * 8) {
            // Manejar el caso de usar un bit menos significativo mas
            throw new IllegalArgumentException("No hay suficientes bytes para ocultar la información.");
        }

>>>>>>> 3f97c936a0ad40ef591baf46e98acea457c46f81
        byte[] modified = carrierData.clone();
        int carrierCapacity = carrierData.length;
        int totalBitsToHide = dataToHide.length * 8;

        int bitIndex = 0; // Índice global de bit a ocultar (0 a totalBitsToHide-1)

        while (bitIndex < totalBitsToHide) {
            int bitPosition = bitIndex / carrierCapacity; // Qué bit menos significativo usar (0 = LSB, 1 = 2do bit, ...)
            if (bitPosition >= 8) {
                throw new IllegalArgumentException("No hay suficientes bits disponibles para ocultar toda la información.");
            }

            int carrierIndex = bitIndex % carrierCapacity;

            int byteIndex = bitIndex / 8;
            int bitInByte = 7 - (bitIndex % 8); // de MSB a LSB

            int bitToHide = (dataToHide[byteIndex] >> bitInByte) & 1;

            // Limpio el bit que voy a usar y lo seteo con bitToHide
            modified[carrierIndex] &= ~(1 << bitPosition);
            modified[carrierIndex] |= (bitToHide << bitPosition);

            bitIndex++;
        }

        return modified;
    }

    /**
     * Extrae 'numBytes' ocultos desde los bits menos significativos de cada byte de 'carrierData'.
     * Recupera secuencialmente desde el LSB, luego el segundo bit, etc., hasta completar los datos ocultos.
     */
    public static byte[] extract(byte[] carrierData, int numBytes) {
        int totalBitsToExtract = numBytes * 8;
        int carrierCapacity = carrierData.length;

        byte[] result = new byte[numBytes];
        int bitIndex = 0;

        while (bitIndex < totalBitsToExtract) {
            int bitPosition = bitIndex / carrierCapacity; // qué bit menos significativo se está leyendo
            if (bitPosition >= 8) {
                throw new IllegalArgumentException("No hay suficientes datos disponibles para extraer toda la información.");
            }

            int carrierIndex = bitIndex % carrierCapacity;
            int bit = (carrierData[carrierIndex] >> bitPosition) & 1;

            int byteIndex = bitIndex / 8;
            result[byteIndex] = (byte) ((result[byteIndex] << 1) | bit);

            bitIndex++;
        }

        return result;
    }
}
