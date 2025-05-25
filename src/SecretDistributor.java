import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SecretDistributor {
    private final byte[] permutedSecret;
    private final int k;
    private final int n;

    private final String dir;

    public SecretDistributor(byte[] permutedSecret, int k, int n, String dir) {
        if (permutedSecret.length % k != 0) {
            throw new IllegalArgumentException("La cantidad de bytes del secreto no es divisible por k. " +
                    "No se pueden formar polinomios completos.");
        }

        this.permutedSecret = permutedSecret;
        this.k = k;
        this.n = n;
        this.dir = dir;
    }

    public int getCantidadPolinomios() {
        return permutedSecret.length / k;
    }

    /**
     * Evalúa el polinomio p-ésimo (0-based) en x, usando aritmética módulo 257.
     */
    public int evaluarPolinomio(int p, int x) {
        if (p < 0 || p >= getCantidadPolinomios()) {
            throw new IllegalArgumentException("Índice de polinomio inválido: " + p);
        }

        int resultado = 0;
        int inicio = p * k;
        
        for (int i = 0; i < k; i++) {
            int coef = Byte.toUnsignedInt(permutedSecret[inicio + i]);
            resultado += coef * (int) (Math.pow(x, i));
        }

        return resultado % 256;
    }

    public void distribute(int semilla) throws Exception {

        File carpeta = new File(dir);
        File[] archivos = carpeta.listFiles((f, name) -> name.toLowerCase().endsWith(".bmp"));

        if (archivos == null || archivos.length < n) {
            throw new IllegalArgumentException("No hay suficientes imágenes BMP en el directorio: " + dir);
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

            for (int j = 0; j < cantidadPolinomios; j++) {
                int valor = evaluarPolinomio(j, sombraId);
                valoresAOcultar[j] = (byte) valor;
            }

            byte[] cuerpoModificado = LsbSteganography.embed(pixelData, valoresAOcultar);
            img.setPixelData(cuerpoModificado);

            img.setReservedBytes(6, (short) semilla);
            img.setReservedBytes(8, (short) sombraId);

            String nombreSalida = String.format("%s/sombra%d.bmp", dir, sombraId);
            img.save(nombreSalida);
        }
    }
}