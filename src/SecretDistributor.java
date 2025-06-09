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
            throw new IllegalArgumentException("No hay suficientes imágenes BMP en el directorio: resources/preSombras");
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
                    System.out.printf("Recortando imagen portadora %d (%s):\n", i + 1, fileName);
                    System.out.printf("  - Tamaño original: %dx%d píxeles\n", portadora.getWidth(), portadora.getHeight());
                    System.out.printf("  - Tamaño objetivo: %dx%d píxeles\n", secretWidth, secretHeight);
                    System.out.printf("  - Se tomarán los píxeles centrales:\n");
                    System.out.printf("    * Horizontal: desde %d hasta %d\n", 
                        (portadora.getWidth() - secretWidth) / 2,
                        (portadora.getWidth() + secretWidth) / 2);
                    System.out.printf("    * Vertical: desde %d hasta %d\n",
                        (portadora.getHeight() - secretHeight) / 2,
                        (portadora.getHeight() + secretHeight) / 2);
                    portadora = portadora.cropToSize(secretWidth, secretHeight);
                }
            }
            // Use the secret image's header for all shadows
            BmpImage img = new BmpImage(secretImage.getHeader(), portadora.getPixelData());
            portadoras.add(img);
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

            byte[] cuerpoModificado = LsbSteganography.embed(
                pixelData,
                valoresAOcultar,
                isBorder
            );
            img.setPixelData(cuerpoModificado);

            // Store seed in bytes 6-7 (little endian)
            img.setReservedBytes(6, (short) seed);
            // Store shadow number in bytes 8-9 (little endian)
            img.setReservedBytes(8, (short) sombraId);
            // Store number of polynomials in bytes 34-35
            img.setAmountOfBytesToEmbed(34, cantidadPolinomios);

            String nombreSalida = String.format("resources/sombras/sombra%d.bmp", sombraId);
            img.save(nombreSalida);
        }
    }
}
