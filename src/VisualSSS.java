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

    public static void testShortSecret() throws Exception {

        byte[] secret = new byte[] {10, 20, 30, 40, 50, 60};
        int k = 3;
        int n = 5;
        int seed = 10;
        String dir = "resources";

        byte[] permuted = permuteSecret(seed, secret);

        // Distribución
        SecretDistributor dist = new SecretDistributor(permuted, k, n, dir);
        dist.distribute(seed);

        // Recuperación
        SecretRecoverer rec = new SecretRecoverer(k, n, dir);
        byte[] recoveredPermuted = rec.recover();

        byte[] recovered = originalSecret(seed, recoveredPermuted);

        // Validación
        System.out.println("Original:   " + Arrays.toString(secret));
        System.out.println("Recuperado: " + Arrays.toString(recovered));
        System.out.println(Arrays.equals(secret, recovered)
                ? "Test exitoso: recuperación correcta"
                : "Test fallido: datos no coinciden");
    }



    public static void main(String[] args) throws Exception {

//        if (args.length < 4) {
//            printUsageAndExit("Error: argumentos insuficientes.");
//        }
//
//        Map<String, String> params = parseArguments(args);
//
//        String mode = params.get("mode");
//        String secret = params.get("secret");
//        int k = parseInt(params.get("k"), "k");
//
//        int n = params.containsKey("n") ? parseInt(params.get("n"), "n") : -1;
//        String dir = params.getOrDefault("dir", ".");
//
//        if (!secret.endsWith(".bmp")) {
//            printUsageAndExit("Error: el archivo secreto debe tener extensión .bmp");
//        }
//
//        if (mode.equals("d")) {
//            int seed = 10;
//            BmpImage secret_image = new BmpImage(secret);
//            byte[] originalSecret = secret_image.getPixelData();
//            byte[] permutedSecret = permuteSecret(seed, originalSecret); //Check seed.
//            SecretDistributor distributor = new SecretDistributor(permutedSecret, k, n, dir);
//            distributor.distribute(seed);
//        } else if (mode.equals("r")) {
//            SecretRecoverer recoverer = new SecretRecoverer(k, n, dir);
//            byte[] permutedSecret = recoverer.recover();
//            byte[] originalSecret = originalSecret(recoverer.getSeed(), permutedSecret);
//
//            // Load the reference image to get its header
//            BmpImage referenceImage = new BmpImage(recoverer.getReferenceHeader());
//            byte[] header = referenceImage.getHeader();
//            BmpImage outputImage = new BmpImage(header, originalSecret);
//            outputImage.save(secret); // 'secret' is the output filename from args
//        } else {
//             printUsageAndExit("Error: modo inválido, debe ser -d o -r.");
//        }

        testShortSecret();
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
        System.err.println("  Distribuir: visualSSS -d -secret <archivo.bmp> -k <num> [-n <num>] [-dir <directorio>]");
        System.err.println("  Recuperar:  visualSSS -r -secret <archivo.bmp> -k <num> [-n <num>] [-dir <directorio>]");
        System.exit(1);
    }
}
