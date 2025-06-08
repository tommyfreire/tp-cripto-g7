import java.util.Random;

/**
 * Generates and manages a table of random bytes for permutation operations.
 * This class is used to create a deterministic sequence of random bytes based on a seed.
 */
public class PermutationTable {
    private static final int BYTE_RANGE = 256;
    private final byte[] table;

    /**
     * Creates a new permutation table with the specified size and seed.
     * 
     * @param seed The seed value for the random number generator
     * @param size The size of the permutation table
     * @throws IllegalArgumentException If size is negative
     */
    public PermutationTable(int seed, int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Table size cannot be negative");
        }
        
        table = new byte[size];
        Random rng = new Random(seed);

        for (int i = 0; i < size; i++) {
            table[i] = (byte) rng.nextInt(BYTE_RANGE);
        }
    }

    /**
     * Returns the entire permutation table.
     * 
     * @return The permutation table
     */
    public byte[] getTable() {
        return table;
    }

    /**
     * Returns the byte at the specified index in the permutation table.
     * 
     * @param index The index to retrieve
     * @return The byte at the specified index
     * @throws IndexOutOfBoundsException If index is out of bounds
     */
    public byte getAt(int index) {
        if (index < 0 || index >= table.length) {
            throw new IndexOutOfBoundsException("Index " + index + " is out of bounds for table size " + table.length);
        }
        return table[index];
    }

    /**
     * Returns the size of the permutation table.
     * 
     * @return The number of bytes in the table
     */
    public int size() {
        return table.length;
    }
}