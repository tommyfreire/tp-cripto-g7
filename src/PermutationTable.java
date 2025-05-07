import java.util.Random;

public class PermutationTable {
    private byte[] table;

    public PermutationTable(int seed, int size) {
        table = new byte[size];
        Random rng = new Random(seed);

        for (int i = 0; i < size; i++) {
            table[i] = (byte) rng.nextInt(256); // valor entre 0 y 255
        }
    }

    public byte[] getTable() {
        return table;
    }

    public byte getAt(int index) {
        return table[index];
    }

    public int size() {
        return table.length;
    }
}
