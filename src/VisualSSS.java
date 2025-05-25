import java.util.HashMap;
import java.util.Map;

public class VisualSSS {

    static int seed = 10;

    private static byte[] permuteSecret(byte[] secretTest) {
        PermutationTable tabla = new PermutationTable(seed, secretTest.length);

        byte[] permutedSecret = new byte[secretTest.length];
        for (int i = 0; i < secretTest.length; i++) {
            int original = Byte.toUnsignedInt(secretTest[i]);
            int randomVal = Byte.toUnsignedInt(tabla.getAt(i));
            permutedSecret[i] = (byte) ((original + randomVal) % 256);
        }

        return permutedSecret;
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
        String dir = params.getOrDefault("dir", ".");

        if (!secret.endsWith(".bmp")) {
            printUsageAndExit("Error: el archivo secreto debe tener extensión .bmp");
        }

        if (mode.equals("d")) {
            byte[] permutedSecret = permuteSecret(secretTest);
            SecretDistributor distributor = new SecretDistributor(permutedSecret, k, n, dir);
            distributor.distribute(seed);
        } else if (mode.equals("r")) {
            SecretRecoverer recoverer = new SecretRecoverer(k, n, dir);
            recoverer.recover();
        }
//        } else {
//            printUsageAndExit("Error: modo inválido, debe ser -d o -r.");
//        }
    }

    public static void testDistributor() {

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
