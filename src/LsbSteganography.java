public class LsbSteganography {

    /**
     * Oculta los bits de 'data' dentro de los bytes de 'carrierData', usando LSB replacement.
     * Cada byte de 'data' se oculta en 8 bytes de 'carrierData'.
     */
    public static byte[] embed(byte[] carrierData, byte[] dataToHide) {
        if (carrierData.length < dataToHide.length * 8) {
            throw new IllegalArgumentException("No hay suficientes bytes para ocultar la información.");
        }

        byte[] modified = carrierData.clone();

        int dataIndex = 0;
        int carrierIndex = 0;

        while (dataIndex < dataToHide.length) {
            byte b = dataToHide[dataIndex];
            for (int i = 7; i >= 0; i--) {
                int bit = (b >> i) & 1;
                modified[carrierIndex] = (byte) ((modified[carrierIndex] & 0xFE) | bit);
                carrierIndex++;
            }
            dataIndex++;
        }

        return modified;
    }

    /**
     * Extrae 'numBytes' ocultos desde los LSB de los bytes de 'carrierData'.
     */
    public static byte[] extract(byte[] carrierData, int numBytes) {
        if (carrierData.length < numBytes * 8) {
            throw new IllegalArgumentException("No hay suficientes datos para extraer los bytes ocultos.");
        }

        byte[] result = new byte[numBytes];
        int carrierIndex = 0;

        for (int i = 0; i < numBytes; i++) {
            byte b = 0;
            for (int j = 0; j < 8; j++) {
                b = (byte) ((b << 1) | (carrierData[carrierIndex] & 1));
                carrierIndex++;
            }
            result[i] = b;
        }

        return result;
    }
}
