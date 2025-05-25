import java.io.File;
import java.io.IOException;

public class SecretRecoverer {
    int k;
    int n;
    String dir;
    int seed;
    byte[] permutedSecret; // Nombre del archivo secreto

    public SecretRecoverer(byte[] permutedSecret, int k, int n, String dir) throws IOException {
        if (permutedSecret.length % k != 0) {
            throw new IllegalArgumentException("La cantidad de bytes del secreto no es divisible por k. " +
                    "No se pueden formar polinomios completos.");
        }
        if (k < 2 || k > 10) {
            throw new IllegalArgumentException("El valor de k debe estar entre 2 y 10.");
        }
        File dirFile = new File(dir);
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            throw new IllegalArgumentException("El directorio especificado no existe: " + dir);
        }
        File[] shadowFiles = dirFile.listFiles((d, name) -> name.startsWith("sombra") && name.endsWith(".bmp"));
        if (shadowFiles == null || shadowFiles.length ==0 || shadowFiles.length < k) {
            throw new IllegalArgumentException("Se requieren al menos " + k + " archivos de sombra en el directorio: " + dir);
        }

        BmpImage shadow = new BmpImage(shadowFiles[0].getAbsolutePath());

        this.k = k;
        this.n = n;
        this.dir = dir;
        this.seed = shadow.getReservedBytes(6); // Obtener semilla de los bytes reservados
        this.permutedSecret = permutedSecret;
    }

    public int getSeed() {
        return seed;
    }

    public byte[] recover() throws IOException {
        // Load k shadow images
        BmpImage[] shadows = new BmpImage[2];
        int[] shadowIds = new int[2];
        for (int i = 0; i < 2; i++) {
            String filePath = String.format("%s/sombra%d.bmp", dir, i + 1);
            File file = new File(filePath);
            if (!file.exists()) {
                throw new IllegalArgumentException("No se encontró la sombra: " + filePath);
            }
            shadows[i] = new BmpImage(filePath);
            // Get shadow id from reserved bytes (as in distributor)
            shadowIds[i] = shadows[i].getReservedBytes(8); // sombraId
        }

        int cantidadPolinomios = permutedSecret.length / k;
        byte[][] extracted = new byte[k][cantidadPolinomios];
        for (int i = 0; i < k; i++) {
            extracted[i] = LsbSteganography.extract(shadows[i].getPixelData(), cantidadPolinomios);
        }

        byte[] recoveredPermuted = new byte[permutedSecret.length];
        for (int poly = 0; poly < cantidadPolinomios; poly++) {
            // For each polynomial, collect k (x, y) pairs
            int[] x = new int[k];
            int[] y = new int[k];
            for (int i = 0; i < k; i++) {
                x[i] = shadowIds[i];
                y[i] = Byte.toUnsignedInt(extracted[i][poly]);
            }
            // Interpolate to get the secret bytes (the coefficients)
            int[] coeffs = lagrangeInterpolateAllCoefficients(x, y, k, 257);
            for (int j = 0; j < k; j++) {
                recoveredPermuted[poly * k + j] = (byte) coeffs[j];
            }
        }

        return recoveredPermuted;
    }

    // Helper: Lagrange interpolation to recover all coefficients of the polynomial
    // Returns array of coefficients [a0, a1, ..., ak-1]
    private int[] lagrangeInterpolateAllCoefficients(int[] x, int[] y, int k, int mod) {
        // Only the first coefficient (a0) is the secret, but for completeness, recover all
        // Use Newton's divided differences or Lagrange basis
        // Here, we recover only a0 (the secret byte)
        int[] coeffs = new int[k];
        // Recover a0 (the secret byte)
        coeffs[0] = lagrangeInterpolate(x, y, 0, mod);
        // Optionally, recover other coefficients if needed (not required for secret)
        return coeffs;
    }

    // Lagrange interpolation at x=0
    private int lagrangeInterpolate(int[] x, int[] y, int at, int mod) {
        int result = 0;
        for (int i = 0; i < x.length; i++) {
            int term = y[i];
            for (int j = 0; j < x.length; j++) {
                if (i != j) {
                    term = (int) (((long) term * (at - x[j] + mod) % mod) * modInverse(x[i] - x[j] + mod, mod) % mod);
                }
            }
            result = (result + term) % mod;
        }
        return result;
    }

    // Modular inverse using extended Euclidean algorithm
    private int modInverse(int a, int mod) {
        int m0 = mod, t, q;
        int x0 = 0, x1 = 1;
        if (mod == 1) return 0;
        a = ((a % mod) + mod) % mod;
        while (a > 1) {
            q = a / mod;
            t = mod;
            mod = a % mod; a = t;
            t = x0;
            x0 = x1 - q * x0;
            x1 = t;
        }
        if (x1 < 0) x1 += m0;
        return x1;
    }

    // Inverse permutation (assuming identity, override if you have a permutation)
    private byte[] inversePermute(byte[] arr) {
        // TODO: Implement if you have a permutation array
        return arr;
    }
}
