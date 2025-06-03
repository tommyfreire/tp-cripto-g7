import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class VisualSSS {

    private static final int TEST_SEED = 10;

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

    public static void testBmpSecret() throws Exception {
        String inputBmpPath = "resources/Alfred.bmp";
        String outputBmpPath = "resources/recuperado.bmp";
        String dir = "resources/sombras";

        int k = 3;
        int n = 5;
        int seed = 42;

        BmpImage originalBmp = new BmpImage(inputBmpPath);
        byte[] secret = originalBmp.getPixelData();

        byte[] permuted = permuteSecret(seed, secret);

        SecretDistributor distributor = new SecretDistributor(permuted, k, n, dir);
        distributor.distribute(seed);

        SecretRecoverer recoverer = new SecretRecoverer(k, n, dir);
        byte[] recoveredPermuted = recoverer.recover();

        byte[] recovered = originalSecret(seed, recoveredPermuted);

        System.out.println("Secreto original (bytes): " + secret.length);
        System.out.println("¿Recuperado igual al original?: " + Arrays.equals(secret, recovered));

        BmpImage resultado = new BmpImage(originalBmp.getHeader(), recovered);
        resultado.save(outputBmpPath);
        System.out.println("Imagen recuperada guardada como: " + outputBmpPath);
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
           BmpImage secret_image = new BmpImage(secret);
           byte[] originalSecret = secret_image.getPixelData();
           byte[] permutedSecret = permuteSecret(TEST_SEED, originalSecret); //Check seed.
           SecretDistributor distributor = new SecretDistributor(permutedSecret, k, n, dir);
           distributor.distribute(TEST_SEED);
       } else if (mode.equals("r")) {
           SecretRecoverer recoverer = new SecretRecoverer(k, n, dir);
           byte[] permutedSecret = recoverer.recover();
           byte[] originalSecret = originalSecret(TEST_SEED, permutedSecret);

           // Use the header from the first pre-shadow image in resources/preSombras/
           java.io.File preShadowDir = new java.io.File("resources/preSombras");
           java.io.File[] preShadowFiles = preShadowDir.listFiles((d, name) -> name.toLowerCase().endsWith(".bmp"));
           if (preShadowFiles == null || preShadowFiles.length == 0) {
               printUsageAndExit("No se encontró ninguna pre-sombra en resources/preSombras para obtener el header.");
           }
           BmpImage referenceImage = new BmpImage(preShadowFiles[0].getAbsolutePath());
           byte[] header = referenceImage.getHeader();
           BmpImage outputImage = new BmpImage(header, originalSecret);
           outputImage.save(secret); // 'secret' is the output filename from args
       } else {
            printUsageAndExit("Error: modo inválido, debe ser -d o -r.");
       }
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
