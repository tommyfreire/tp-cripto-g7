import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Distributes a permuted secret into n shadow images using Shamir's Secret Sharing and LSB steganography.
 */
public class SecretDistributor {
    private final byte[] permutedSecret;
    private final int k;
    private final int n;

    /**
     * Constructs a SecretDistributor.
     * @param permutedSecret The permuted secret to distribute
     * @param k The threshold for recovery
     * @param n The number of shares to create
     */
    public SecretDistributor(byte[] permutedSecret, int k, int n) {
        if (permutedSecret.length % k != 0) {
            throw new IllegalArgumentException("La cantidad de bytes del secreto no es divisible por k. " +
                    "No se pueden formar polinomios completos.");
        }
        if (k < 2 || k > 10) {
            throw new IllegalArgumentException("El valor de k debe estar entre 2 y 10.");
        }
        if (n < 2) {
            throw new IllegalArgumentException("El valor de n debe ser al menos 2.");
        }
        if (k > n) {
            throw new IllegalArgumentException("El valor de k debe ser menor o igual a n.");
        }
        this.permutedSecret = permutedSecret;
        this.k = k;
        this.n = n;
    }

    public int getCantidadPolinomios() {
        return permutedSecret.length / k;
    }

    /**
     * Distributes the permuted secret into n shadow images using LSB steganography.
     * @param seed The seed for permutation
     * @throws Exception If there is an error during distribution
     */
    public void distribute(int seed) throws Exception {
        File carpeta = new File("resources/preSombras");
        File[] archivos = carpeta.listFiles((f, name) -> name.toLowerCase().endsWith(".bmp"));
        if (archivos == null || archivos.length < n) {
            throw new IllegalArgumentException("No hay suficientes imágenes BMP en el directorio: resources/preSombras");
        }
        List<BmpImage> portadoras = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            portadoras.add(new BmpImage(archivos[i].getAbsolutePath()));
        }
        int cantidadPolinomios = getCantidadPolinomios();
        for (int sombraId = 1; sombraId <= n; sombraId++) {
            BmpImage img = portadoras.get(sombraId - 1);
            byte[] pixelData = img.getPixelData();
            byte[] valoresAOcultar = new byte[cantidadPolinomios];
            boolean[] isBorder = new boolean[cantidadPolinomios];
            for (int j = 0; j < cantidadPolinomios; j++) {
                int inicio = j * k;
                int[] coef = new int[k];
                for (int ci = 0; ci < k; ci++) {
                    coef[ci] = Byte.toUnsignedInt(permutedSecret[inicio + ci]);
                }
                int resultado = 0;
                for (int i = 0; i < k; i++) {
                    resultado += coef[i] * (int) Math.pow(sombraId, i);
                }
                resultado = resultado % 257;
                if(resultado == 256) {
                    isBorder[j] = true;
                    resultado = 255;
                } else {
                    isBorder[j] = false;
                }
                valoresAOcultar[j] = (byte) resultado;
            }
            byte[] cuerpoModificado = LsbSteganography.embed(pixelData, valoresAOcultar, isBorder);
            img.setPixelData(cuerpoModificado);
            img.setReservedBytes(6, (short) seed);
            img.setReservedBytes(8, (short) sombraId);
            img.setReservedBytes(34,(short) cantidadPolinomios);
            String nombreSalida = String.format("resources/sombras/sombra%d.bmp", sombraId);
            img.save(nombreSalida);
        }
    }
}
