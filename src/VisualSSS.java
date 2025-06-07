import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class VisualSSS {

    private static byte[] permuteSecret(int seed, byte[] secretTest) {
        PermutationTable tabla = new PermutationTable(seed, secretTest.length);

        byte[] permutedSecret = new byte[secretTest.length];
        for (int i = 0; i < secretTest.length; i++) {
            int original = Byte.toUnsignedInt(secretTest[i]);
            int randomVal = Byte.toUnsignedInt(tabla.getAt(i));
            permutedSecret[i] = (byte) ((original + randomVal) % 256);
        }

        return permutedSecret;
    }

    private static int generateSeed() {
        return (int) (Math.random() * 65535);
    }

    private static byte[] originalSecret(int seed, byte[] permutedSecret) {
        PermutationTable tabla = new PermutationTable(seed, permutedSecret.length);
        byte[] originalSecret = new byte[permutedSecret.length];
        for (int i = 0; i < permutedSecret.length; i++) {
            int permuted = Byte.toUnsignedInt(permutedSecret[i]);
            int randomVal = Byte.toUnsignedInt(tabla.getAt(i));
            int original = (permuted - randomVal + 256) % 256;
            originalSecret[i] = (byte) original;
        }
        return originalSecret;
    }

    public static void compararBMPs(String originalPath, String recuperadoPath) throws Exception {
        BmpImage original = new BmpImage(originalPath);
        BmpImage recuperado = new BmpImage(recuperadoPath);

        byte[] a = original.getPixelData();
        byte[] b = recuperado.getPixelData();

        if (a.length != b.length) {
            System.out.println("❌ Las imágenes tienen diferente cantidad de bytes: " + a.length + " vs " + b.length);
            return;
        }

        int errores = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                errores++;
                System.out.printf("Byte %d: original = %d, recuperado = %d%n", i, Byte.toUnsignedInt(a[i]), Byte.toUnsignedInt(b[i]));
            }
        }

        System.out.println("Total de diferencias: " + errores + " de " + a.length + " píxeles (" + (100.0 * errores / a.length) + "%)");

        if (errores == 0) {
            System.out.println("✅ ¡Las imágenes son idénticas!");
        }
    }

    public static void testCasoBorde() throws Exception {
        // Este polinomio genera 256 al evaluarse en x = 2
        int k = 2;
        int n = 3;
        int seed = 42;

        byte[] permutedSecret = new byte[] { (byte)101, (byte)78, (byte)55, (byte)240};

        String dir = "resources/sombras";

        SecretDistributor dist = new SecretDistributor(permutedSecret, k, n);
        dist.distribute(seed);

        SecretRecoverer rec = new SecretRecoverer(k, n, dir);
        byte[] recovered = rec.recover();

        System.out.println("Original permutedSecret: " + Arrays.toString(permutedSecret));
        System.out.println("Recovered permuted:      " + Arrays.toString(recovered));
    }


    private static byte[] hardcodedFacundoHeader() throws IOException {
        BmpImage alfred = new BmpImage("resources/Facundo.bmp");
        return alfred.getHeader();
    }

    public static void main(String[] args) throws Exception {

       if (args.length < 4) {
           printUsageAndExit("Error: argumentos insuficientes.");
       }

       Map<String, String> params = parseArguments(args);

       String mode = params.get("mode");
       String secret = params.get("secret");
       int k = parseInt(params.get("k"), "k");

       int n = params.containsKey("n") ? parseInt(params.get("n"), "n") : -1;
       String dir = params.getOrDefault("dir", "resources/sombras");

       if (!secret.endsWith(".bmp")) {
           printUsageAndExit("Error: el archivo secreto debe tener extensión .bmp");
       }

       if (mode.equals("d")) {
            int seed = generateSeed();
            BmpImage secret_image = new BmpImage(secret);
           byte[] originalSecret = secret_image.getPixelData();
           byte[] permutedSecret = permuteSecret(seed, originalSecret);
           Files.write(Paths.get("resources/permutado_secreto.bin"), permutedSecret);
           SecretDistributor distributor = new SecretDistributor(permutedSecret, k, n);
           distributor.distribute(seed);
       } else if (mode.equals("r")) {
           SecretRecoverer recoverer = new SecretRecoverer(k, n, dir);
           byte[] permutedSecret = recoverer.recover();
           int seed = recoverer.getSeed();
           byte[] originalSecret = originalSecret(seed, permutedSecret);
           BmpImage outputImage = new BmpImage(hardcodedFacundoHeader(), originalSecret);
           outputImage.save(secret);
           compararBMPs(secret, "resources/Facundo.bmp");
       } else {
            printUsageAndExit("Error: modo inválido, debe ser -d o -r.");
       }
//        testCasoBorde();
    }

    private static Map<String, String> parseArguments(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-d":
                    map.put("mode", "d");
                    break;
                case "-r":
                    map.put("mode", "r");
                    break;
                case "-secret":
                    if (i + 1 >= args.length) printUsageAndExit("Falta nombre de archivo después de -secret.");
                    map.put("secret", args[++i]);
                    break;
                case "-k":
                    if (i + 1 >= args.length) printUsageAndExit("Falta número después de -k.");
                    map.put("k", args[++i]);
                    break;
                case "-n":
                    if (i + 1 >= args.length) printUsageAndExit("Falta número después de -n.");
                    map.put("n", args[++i]);
                    break;
                case "-dir":
                    if (i + 1 >= args.length) printUsageAndExit("Falta directorio después de -dir.");
                    map.put("dir", args[++i]);
                    break;
                default:
                    printUsageAndExit("Parámetro no reconocido: " + args[i]);
            }
        }
        return map;
    }

    private static int parseInt(String value, String paramName) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            printUsageAndExit("El parámetro -" + paramName + " debe ser un número entero.");
            return -1;
        }
    }

    private static void printUsageAndExit(String message) {
        System.err.println(message);
        System.err.println("Uso:");
        System.err.println("  Distribuir: visualSSS -d -secret <archivo.bmp> -k <num> [-n <num>] [-dir <directorio>]\n       (usa portadoras de resources/preSombras y guarda sombras en resources/sombras)");
        System.err.println("  Recuperar:  visualSSS -r -secret <archivo.bmp> -k <num> [-n <num>] [-dir <directorio>]");
        System.exit(1);
    }
}
