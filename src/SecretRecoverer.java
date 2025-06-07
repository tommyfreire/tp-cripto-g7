import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;

public class SecretRecoverer {
    private final int k;
    private final String dir;

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

    public int getSeed() {
        // Obtener la semilla de la primera sombra ubicada en los bytes 6 y 7 del header
        try {
            File carpeta = new File(dir);
            File[] archivos = carpeta.listFiles((d, name) -> name.startsWith("sombra") && name.endsWith(".bmp"));
            BmpImage sombra = new BmpImage(archivos[0].getAbsolutePath());
            return sombra.getReservedBytes(6);
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo la primera sombra para obtener la semilla", e);
        }
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
        int q = sombras.get(0).getIntFromHeader(34); // cantidad de bytes a extraer por sombra
        if (q <= 0) {
            throw new IllegalArgumentException("Valor de q inválido: " + q);
        }

        // Paso 3: extraer los q valores de cada sombra
        byte[][] extracted = new byte[k][q];
        boolean[][] isBorder = new boolean[k][q];
        for (int i = 0; i < k; i++) {
            extracted[i] = LsbSteganography.extract(sombras.get(i).getPixelData(), q, isBorder[i]);
        }

        // Paso 4: resolver q sistemas de k ecuaciones para recuperar los coeficientes
        byte[] recoveredPermuted = new byte[q * k];
        //byte[] expectedPermuted = Files.readAllBytes(Paths.get("resources/permutado_secreto.bin"));

        for (int j = 0; j < q; j++) {
            int[] y = new int[k];
            for (int i = 0; i < k; i++) {
                int aux = Byte.toUnsignedInt(extracted[i][j]); // Pj(x = sombraId)
                if(aux == 255) {
                    aux = (isBorder[i][j]) ? 256 : 255;
                }
                y[i] = aux;
            }

            int[] x = sombraIds;

            // Debug prints for failing bytes
            if (j >= 14391/3 && j <= 14393/3 || j >= 17589/3 && j <= 17591/3 || 
                j >= 31020/3 && j <= 31022/3 || j >= 49071/3 && j <= 49073/3) {
                System.out.printf("\nDebug for polynomial %d (bytes %d-%d):\n", j, j*3, j*3+2);
                System.out.println("Shadow IDs (x values): " + Arrays.toString(x));
                System.out.println("Extracted values (y values): " + Arrays.toString(y));
            }

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

            // Debug prints for failing bytes
            if (j >= 14391/3 && j <= 14393/3 || j >= 17589/3 && j <= 17591/3 || 
                j >= 31020/3 && j <= 31022/3 || j >= 49071/3 && j <= 49073/3) {
                System.out.println("Recovered coefficients: " + Arrays.toString(coef));
                System.out.println("Verifying polynomial evaluation:");
                for (int i = 0; i < k; i++) {
                    int eval = 0;
                    for (int c = 0; c < k; c++) {
                        eval = (eval + coef[c] * (int)Math.pow(x[i], c)) % 257;
                    }
                    System.out.printf("P(%d) = %d (expected %d)\n", x[i], eval, y[i]);
                }
            }

            for (int i = 0; i < k; i++) {
                int index = j * k + i;
                recoveredPermuted[index] = (byte) coef[i];

//                int expected = Byte.toUnsignedInt(expectedPermuted[index]);
//                int actual = Byte.toUnsignedInt(recoveredPermuted[index]);
//                if (expected != actual && index <= 500) {
//                    System.out.printf("❌ Byte %d: esperado = %d, recuperado = %d (polinomio %d, coeficiente %d)%n",
//                            index, expected, actual, j, i);
//                }
            }

        }

        return recoveredPermuted;
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

