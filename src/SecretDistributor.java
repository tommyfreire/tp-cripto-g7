import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Distributes a permuted secret into n shadow images using Shamir's Secret Sharing and LSB steganography.
 */
public class SecretDistributor {
    private final byte[] permutedSecret;
    private final int k;
    private final int n;
    private final String dir;
    private final int secretWidth;
    private final int secretHeight;
    private final BmpImage secretImage;

    /**
     * Constructs a SecretDistributor.
     * @param permutedSecret The permuted secret to distribute
     * @param k The threshold for recovery
     * @param n The number of shares to create
     * @param secretWidth The width of the secret image
     * @param secretHeight The height of the secret image
     * @param secretImage The BmpImage of the secret
     */
    public SecretDistributor(byte[] permutedSecret, int k, int n, int secretWidth, int secretHeight, BmpImage secretImage, String dir) {
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
        this.secretWidth = secretWidth;
        this.secretHeight = secretHeight;
        this.secretImage = secretImage;
        this.dir = dir;
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
        File carpeta = new File(dir);
        File[] archivos = carpeta.listFiles((f, name) -> name.toLowerCase().endsWith(".bmp"));
        if (archivos == null || archivos.length < n) {
            throw new IllegalArgumentException("No hay suficientes imágenes BMP en el directorio: " + dir);
        }

        List<BmpImage> portadoras = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String fileName = archivos[i].getName();
            BmpImage portadora = new BmpImage(archivos[i].getAbsolutePath());
            // Error if carrier is smaller than secret (for any k)
            if (portadora.getWidth() < secretWidth || portadora.getHeight() < secretHeight) {
                System.err.printf("Error: La imagen portadora %d (%s) es más pequeña (%dx%d) que la imagen secreta (%dx%d).\n", i + 1, fileName, portadora.getWidth(), portadora.getHeight(), secretWidth, secretHeight);
                System.err.println("No se puede continuar. Todas las portadoras deben ser al menos del tamaño de la imagen secreta.");
                System.exit(1);
            }
            // For k=8, crop the carrier image to match the secret image size
            if (k == 8) {
                if (portadora.getWidth() != secretWidth || portadora.getHeight() != secretHeight) {
                    portadora = portadora.cropToSize(secretWidth, secretHeight);
                }
            }
            // Use the secret image's header for all shadows
            BmpImage img = new BmpImage(secretImage.getHeader(), portadora.getPixelData());
            portadoras.add(img);
        }

        int cantidadPolinomios = getCantidadPolinomios();

        byte[][] valoresAOcultar = new byte[n][cantidadPolinomios];

        for (int j = 0; j < cantidadPolinomios; j++) {
            boolean coefficientsUpdated;
            do {
                coefficientsUpdated = false;
                for (int sombraId = 1; sombraId <= n; sombraId++) {
                    int resultado = evaluarPolinomio(sombraId, j) % 257;
                    if (resultado == 256) {
                        boolean decremented = false;
                        for (int i = 0; i < k; i++) {
                            int coef = Byte.toUnsignedInt(permutedSecret[j * k + i]);
                            if (coef != 0) {
                                permutedSecret[j * k + i]--;
                                decremented = true;
                                coefficientsUpdated = true;
                                break;
                            }
                        }
                        if (!decremented) {
                            throw new IllegalStateException("All coefficients are zero; cannot decrement further.");
                        }
                    }
                }
            } while (coefficientsUpdated);

            for (int i = 0; i < n; i++) {
                int sombraId = i + 1;
                valoresAOcultar[i][j] = (byte) (evaluarPolinomio(sombraId, j) % 257);
            }
        }

        for(int i=0; i<n; i++) {
            BmpImage img = portadoras.get(i);
            byte[] pixelData = img.getPixelData();
            byte[] cuerpoModificado = LsbSteganography.embed(pixelData, valoresAOcultar[i]);
            img.setPixelData(cuerpoModificado);
            // Store seed in bytes 6-7 (little endian)
            img.setReservedBytes(6, (short) seed);
            // Store shadow number in bytes 8-9 (little endian)
            img.setReservedBytes(8, (short) (i+1));
            // Store number of polynomials in bytes 34-35
            img.setAmountOfBytesToEmbed(34, cantidadPolinomios);
            String nombreSalida = String.format("resources/sombras/sombra%d.bmp", (i+1));
            img.save(nombreSalida);
        }

    }


    private int evaluarPolinomio(int sombraId, int j) {
        int inicio = j * k;
        int resultado = 0;
        for (int i = 0; i < k; i++) {
            int coeficiente = Byte.toUnsignedInt(permutedSecret[inicio + i]);
            resultado += coeficiente * (int) Math.pow(sombraId, i);
        }
        return resultado;
    }

}
