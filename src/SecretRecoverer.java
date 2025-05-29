import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SecretRecoverer {
    private final int k;
    private final int n;
    private final String dir;

    public SecretRecoverer(int k, int n, String dir) {
        if (k < 2 || k > n) {
            throw new IllegalArgumentException("El valor de k debe estar entre 2 y n.");
        }
        this.k = k;
        this.n = n;
        this.dir = dir;
    }

    public byte[] recover() throws IOException {
        // Paso 1: seleccionar k imágenes aleatorias del directorio
        File carpeta = new File(dir);
        File[] archivos = carpeta.listFiles((d, name) -> name.startsWith("sombra") && name.endsWith(".bmp"));

        if (archivos == null || archivos.length < k) {
            throw new IllegalArgumentException("No hay al menos " + k + " sombras en el directorio.");
        }

        List<File> lista = new ArrayList<>();
        Collections.addAll(lista, archivos);
        Collections.shuffle(lista); // selección aleatoria
        List<BmpImage> sombras = new ArrayList<>();
        int[] sombraIds = new int[k];

        for (int i = 0; i < k; i++) {
            BmpImage bmp = new BmpImage(lista.get(i).getAbsolutePath());
            sombras.add(bmp);
            sombraIds[i] = bmp.getReservedBytes(8); // ID de sombra
        }

        // Paso 2: determinar cuántos bytes por sombra
        int q = sombras.get(0).getReservedBytes(34); // cantidad de bytes extraídos por sombra

        // Paso 3: extraer los q valores de cada sombra
        byte[][] extracted = new byte[k][q];
        for (int i = 0; i < k; i++) {
            extracted[i] = LsbSteganography.extract(sombras.get(i).getPixelData(), q);
        }

        // Paso 4: resolver q sistemas de k ecuaciones para recuperar los coeficientes
        byte[] recoveredPermuted = new byte[q * k];
        for (int j = 0; j < q; j++) {
            int[] y = new int[k];
            for (int i = 0; i < k; i++) {
                y[i] = Byte.toUnsignedInt(extracted[i][j]); // Pj(x = sombraId)
            }

            int[] x = sombraIds;

            // Construir la matriz de Vandermonde y resolver con Gauss
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
                recoveredPermuted[j * k + i] = (byte) coef[i];
            }
        }

        return recoveredPermuted;
    }

    public int getSeed() {
        // Hardcoded seed for simplicity
        return 69;
    }

    public String getReferenceHeader() {
        // Hardcoded reference header for simplicity
        return "reference_header.bmp";
    }

    // Gauss-Jordan con módulo (resolver Ax = b mod m)
    private int[] gaussMod(int[][] A, int[] b, int mod) {
        int n = A.length;
        int[][] M = new int[n][n + 1];

        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, M[i], 0, n);
            M[i][n] = b[i];
        }

        for (int i = 0; i < n; i++) {
            // Pivote no nulo
            int inv = modInverse(M[i][i], mod);
            for (int j = i; j <= n; j++) {
                M[i][j] = (M[i][j] * inv) % mod;
            }

            // Eliminar otras filas
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

