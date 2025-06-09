import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Recovers a secret from k shadow images using Shamir's Secret Sharing and LSB steganography.
 */
public class SecretRecoverer {
    private final int k;
    private final String dir;

    /**
     * Constructs a SecretRecoverer.
     * @param k The threshold for recovery
     * @param n The number of shares (for validation)
     * @param dir The directory containing shadow images
     */
    public SecretRecoverer(int k, int n, String dir) {
        if (k < 2 || k > 10) {
            throw new IllegalArgumentException("El valor de k debe estar entre 2 y 10.");
        }
        if (k > n) {
            throw new IllegalArgumentException("El valor de k debe ser menor o igual a n.");
        }
        if (n < 2) {
            throw new IllegalArgumentException("El valor de n debe ser mayor o igual a 2.");
        }
        this.k = k;
        this.dir = dir;
    }

    /**
     * Gets the permutation seed from the first shadow image.
     * @return The seed value
     */
    public short getSeed() {
        try {
            File carpeta = new File(dir);
            File[] archivos = carpeta.listFiles((d, name) -> name.startsWith("sombra") && name.endsWith(".bmp"));
            if (archivos == null || archivos.length == 0) {
                throw new IOException("No se encontraron sombras en el directorio: " + dir);
            }
            BmpImage sombra = new BmpImage(archivos[0].getAbsolutePath());
            return sombra.getReservedBytes(6);
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo la primera sombra para obtener la semilla", e);
        }
    }

    /**
     * Recovers the permuted secret from k randomly selected shadow images.
     * @return The recovered permuted secret
     * @throws IOException If there is an error during recovery
     */
    public byte[] recover() throws IOException {
        File carpeta = new File(dir);
        File[] archivos = carpeta.listFiles((d, name) -> name.startsWith("sombra") && name.endsWith(".bmp"));
        if (archivos == null || archivos.length < k) {
            throw new IllegalArgumentException("No hay al menos " + k + " sombras en el directorio.");
        }
        List<File> lista = new ArrayList<>();
        Collections.addAll(lista, archivos);
        Collections.shuffle(lista);
        List<BmpImage> sombras = new ArrayList<>();
        int[] sombraIds = new int[k];
        for (int i = 0; i < k; i++) {
            BmpImage bmp = new BmpImage(lista.get(i).getAbsolutePath());
            sombras.add(bmp);
            sombraIds[i] = bmp.getReservedBytes(8);
        }
        int q = sombras.get(0).getIntFromHeader(34);
        if (q <= 0) {
            throw new IllegalArgumentException("Valor de q inválido: " + q);
        }
        byte[][] extracted = new byte[k][q];
        boolean[][] isBorder = new boolean[k][q];
        for (int i = 0; i < k; i++) {
            extracted[i] = LsbSteganography.extract(sombras.get(i).getPixelData(), q, isBorder[i]);
        }
        byte[] recoveredPermuted = new byte[q * k];
        for (int j = 0; j < q; j++) {
            int[] y = new int[k];
            for (int i = 0; i < k; i++) {
                int aux = Byte.toUnsignedInt(extracted[i][j]);
                if(aux == 255) {
                    aux = (isBorder[i][j]) ? 256 : 255;
                }
                y[i] = aux;
            }
            int[] x = sombraIds;
            int[][] A = new int[k][k];
            for (int row = 0; row < k; row++) {
                int xi = x[row];
                int val = 1;
                for (int col = 0; col < k; col++) {
                    A[row][col] = val;
                    val = (val * xi) % 257;
                }
            }
            int[] coef = gaussMod(A, y, 257);
            for (int i = 0; i < k; i++) {
                int index = j * k + i;
                recoveredPermuted[index] = (byte) coef[i];
            }
        }
        return recoveredPermuted;
    }

    public String getReferenceHeader() {
        return "reference_header.bmp";
    }

    /**
     * Solves a system of linear equations modulo mod using Gauss-Jordan elimination.
     */
    private int[] gaussMod(int[][] A, int[] b, int mod) {
        int n = A.length;
        int[][] M = new int[n][n + 1];
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, M[i], 0, n);
            M[i][n] = b[i];
        }
        for (int i = 0; i < n; i++) {
            int inv = modInverse(M[i][i], mod);
            for (int j = i; j <= n; j++) {
                M[i][j] = (M[i][j] * inv) % mod;
            }
            for (int r = 0; r < n; r++) {
                if (r != i) {
                    int factor = M[r][i];
                    for (int j = i; j <= n; j++) {
                        M[r][j] = (M[r][j] - factor * M[i][j] % mod + mod) % mod;
                    }
                }
            }
        }
        int[] x = new int[n];
        for (int i = 0; i < n; i++) {
            x[i] = M[i][n];
        }
        return x;
    }

    /**
     * Computes the modular inverse of a modulo mod.
     */
    private int modInverse(int a, int mod) {
        a = ((a % mod) + mod) % mod;
        int m0 = mod, t, q;
        int x0 = 0, x1 = 1;
        if (mod == 1) return 0;
        while (a > 1) {
            q = a / mod;
            t = mod;
            mod = a % mod;
            a = t;
            t = x0;
            x0 = x1 - q * x0;
            x1 = t;
        }
        return x1 < 0 ? x1 + m0 : x1;
    }
}
