import static org.junit.Assert.*;

import org.junit.Test;
import java.util.Random;

public class LsbSteganographyTest {
    @Test
    public void testEmbedPolynomialCoefficients() {
        // Example: k=2, n=2, permutedSecret = [a0, a1, a2, a3]
        byte[] permutedSecret = new byte[] {10, 20, 30, 40}; // a0=10, a1=20
        int k = 2;
        int n = 2;

        int p1 = (10 + 20*1) % 256; // 30
        int p2 = (30 + 40*1) % 256; // 50

        // Data to hide in shadow1: [p1, p2]
        byte[] dataToHide = new byte[] {(byte)p1, (byte)p2};

        // Dummy carrier (at least 2 bytes)
        byte[] carrier = new byte[] {0, 0};

        // Embed
        byte[] embedded = LsbSteganography.embed(carrier, dataToHide);

        // Extract
        byte[] extracted = LsbSteganography.extract(embedded, 2);

        // Check
        assertEquals(p1, extracted[0] & 0xFF);
        assertEquals(p2, extracted[1] & 0xFF);
    }

    @Test
    public void testEmbedExtractMultipleBytes() {
        byte[] dataToHide = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
        byte[] carrier = new byte[8];
        byte[] embedded = LsbSteganography.embed(carrier, dataToHide);
        byte[] extracted = LsbSteganography.extract(embedded, dataToHide.length);
        assertArrayEquals(dataToHide, extracted);
    }

    @Test
    public void testEmbedExtractWithNonZeroCarrier() {
        byte[] dataToHide = new byte[] {(byte)0xAA, (byte)0x55, (byte)0xFF, (byte)0x00};
        byte[] carrier = new byte[] {(byte)0xF0, (byte)0x0F, (byte)0xCC, (byte)0x33};
        byte[] embedded = LsbSteganography.embed(carrier, dataToHide);
        byte[] extracted = LsbSteganography.extract(embedded, dataToHide.length);
        assertArrayEquals(dataToHide, extracted);
    }

    @Test
    public void testEmbedExtractAllLSBPlanes() {
        // Embed 8 bytes in 8-byte carrier, each byte will use a different LSB plane
        byte[] dataToHide = new byte[] {1,2,3,4,5,6,7,8};
        byte[] carrier = new byte[8];
        byte[] embedded = LsbSteganography.embed(carrier, dataToHide);
        byte[] extracted = LsbSteganography.extract(embedded, dataToHide.length);
        assertArrayEquals(dataToHide, extracted);
    }

    @Test
    public void testEmbedExtractRandomData() {
        Random rand = new Random(42);
        for (int trial = 0; trial < 10; trial++) {
            int len = 1 + rand.nextInt(32);
            byte[] dataToHide = new byte[len];
            byte[] carrier = new byte[len];
            rand.nextBytes(dataToHide);
            rand.nextBytes(carrier);
            byte[] embedded = LsbSteganography.embed(carrier, dataToHide);
            byte[] extracted = LsbSteganography.extract(embedded, len);
            assertArrayEquals("Failed at trial " + trial, dataToHide, extracted);
        }
    }

    @Test
    public void testExtractAllLSBsSet() {
        // Carrier with all LSBs set (0xFF)
        byte[] carrier = new byte[8];
        for (int i = 0; i < carrier.length; i++) carrier[i] = (byte)0xFF;
        // Should extract all 0xFF bytes
        byte[] expected = new byte[1];
        expected[0] = (byte)0xFF;
        byte[] extracted = LsbSteganography.extract(carrier, 1);
        assertArrayEquals(expected, extracted);
    }

    @Test
    public void testExtractAllLSBsClear() {
        // Carrier with all LSBs clear (0xFE)
        byte[] carrier = new byte[8];
        for (int i = 0; i < carrier.length; i++) carrier[i] = (byte)0xFE;
        // Should extract all 0x00 bytes
        byte[] expected = new byte[1];
        expected[0] = 0x00;
        byte[] extracted = LsbSteganography.extract(carrier, 1);
        assertArrayEquals(expected, extracted);
    }

    @Test
    public void testExtractAlternatingLSBs() {
        // Carrier with alternating LSBs: 0xAA (10101010), 0x55 (01010101)
        byte[] carrier = new byte[] {(byte)0xAA, (byte)0x55, (byte)0xAA, (byte)0x55, (byte)0xAA, (byte)0x55, (byte)0xAA, (byte)0x55};
        // The LSBs are: 0,1,0,1,0,1,0,1 -> should extract 0b01010101 = 0x55
        byte[] expected = new byte[] {0x55};
        byte[] extracted = LsbSteganography.extract(carrier, 1);
        assertArrayEquals(expected, extracted);
    }

    @Test
    public void testExtractFewerBytesThanCarrier() {
        // Carrier with LSBs: 1,0,1,0,1,0,1,0 (0xAA)
        byte[] carrier = new byte[] {(byte)0xFF, (byte)0xFE, (byte)0xFF, (byte)0xFE, (byte)0xFF, (byte)0xFE, (byte)0xFF, (byte)0xFE};
        // Only extract 1 byte
        byte[] expected = new byte[] {(byte)0xAA};
        byte[] extracted = LsbSteganography.extract(carrier, 1);
        assertArrayEquals(expected, extracted);
    }

    @Test
    public void testExtractWithNonZeroHigherBits() {
        // Carrier with LSBs set, higher bits random
        byte[] carrier = new byte[] {(byte)0x81, (byte)0xC3, (byte)0xE5, (byte)0xF7, (byte)0x89, (byte)0xAB, (byte)0xCD, (byte)0xEF};
        // LSBs: 1,1,1,1,1,1,1,1 -> should extract 0xFF
        byte[] expected = new byte[] {(byte)0xFF};
        byte[] extracted = LsbSteganography.extract(carrier, 1);
        assertArrayEquals(expected, extracted);
    }
} 