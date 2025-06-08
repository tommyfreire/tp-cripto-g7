import static org.junit.Assert.*;

import org.junit.Test;

public class LsbSteganographyTest {
    
    @Test
    public void testEmbedAndExtractPreservesOriginalImage() {
        // Create a test image with known pixel values
        byte[] originalImage = new byte[1000];
        for (int i = 0; i < originalImage.length; i++) {
            originalImage[i] = (byte) (i % 256);
        }
        
        // Create some data to hide
        byte[] secretData = new byte[100];
        boolean[] isBorder = new boolean[100];
        for (int i = 0; i < secretData.length; i++) {
            secretData[i] = (byte) (i * 2);
            isBorder[i] = false; // No border values in this test
        }
        
        // Embed the secret data
        byte[] modifiedImage = LsbSteganography.embed(originalImage, secretData, isBorder);
        
        // Verify that only LSBs were modified
        for (int i = 0; i < originalImage.length; i++) {
            // Compare all bits except the LSB and MSB (which might be used for border info)
            assertEquals("Only LSB and MSB should be different", 
                originalImage[i] & 0x7E, 
                modifiedImage[i] & 0x7E);
        }
        
        // Extract the hidden data
        boolean[] extractedIsBorder = new boolean[secretData.length];
        byte[] extractedData = LsbSteganography.extract(modifiedImage, secretData.length, extractedIsBorder);
        
        // Verify the extracted data matches the original secret
        assertArrayEquals("Extracted data should match original secret", 
            secretData, 
            extractedData);
        assertArrayEquals("Border flags should match", isBorder, extractedIsBorder);
    }
    
    @Test
    public void testEmbedAndExtractWithBorderValues() {
        // Create a test image
        byte[] originalImage = new byte[1000];
        for (int i = 0; i < originalImage.length; i++) {
            originalImage[i] = (byte) (i % 256);
        }
        
        // Create data with some border values (255)
        byte[] secretData = new byte[100];
        boolean[] isBorder = new boolean[100];
        for (int i = 0; i < secretData.length; i++) {
            if (i % 10 == 0) { // Every 10th value is 255
                secretData[i] = (byte) 255;
                isBorder[i] = true; // Mark as border value
            } else {
                secretData[i] = (byte) (i * 2);
                isBorder[i] = false;
            }
        }
        
        // Embed the secret data
        byte[] modifiedImage = LsbSteganography.embed(originalImage, secretData, isBorder);
        
        // Extract the hidden data
        boolean[] extractedIsBorder = new boolean[secretData.length];
        byte[] extractedData = LsbSteganography.extract(modifiedImage, secretData.length, extractedIsBorder);
        
        // Verify the extracted data matches the original secret
        assertArrayEquals("Extracted data should match original secret", 
            secretData, 
            extractedData);
        assertArrayEquals("Border flags should match", isBorder, extractedIsBorder);
    }
    
    @Test
    public void testEmbedAndExtractWithDifferentSizes() {
        // Test with various sizes of carrier and secret data
        int[] carrierSizes = {100, 1000, 10000};
        int[] secretSizes = {10, 100, 1000};
        
        for (int carrierSize : carrierSizes) {
            for (int secretSize : secretSizes) {
                if (secretSize * 8 <= carrierSize * 8) { // Only test if there's enough space
                    byte[] carrier = new byte[carrierSize];
                    byte[] secret = new byte[secretSize];
                    boolean[] isBorder = new boolean[secretSize];
                    
                    // Fill with random data
                    for (int i = 0; i < carrier.length; i++) {
                        carrier[i] = (byte) (Math.random() * 256);
                    }
                    for (int i = 0; i < secret.length; i++) {
                        if (Math.random() < 0.1) { // 10% chance of being 255
                            secret[i] = (byte) 255;
                            isBorder[i] = true;
                        } else {
                            secret[i] = (byte) (Math.random() * 255);
                            isBorder[i] = false;
                        }
                    }
                    
                    byte[] modified = LsbSteganography.embed(carrier, secret, isBorder);
                    boolean[] extractedIsBorder = new boolean[secret.length];
                    byte[] extracted = LsbSteganography.extract(modified, secret.length, extractedIsBorder);
                    
                    assertArrayEquals("Data should be preserved for carrier size " + carrierSize + 
                        " and secret size " + secretSize, secret, extracted);
                    assertArrayEquals("Border flags should match", isBorder, extractedIsBorder);
                }
            }
        }
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testEmbedTooMuchData() {
        byte[] carrier = new byte[100];
        byte[] secret = new byte[101]; // Too much data for the carrier
        boolean[] isBorder = new boolean[101];
        LsbSteganography.embed(carrier, secret, isBorder);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testExtractTooMuchData() {
        byte[] carrier = new byte[100];
        boolean[] isBorder = new boolean[101];
        LsbSteganography.extract(carrier, 101, isBorder); // Try to extract more data than possible
    }

    @Test
    public void testEmbedAndExtractWithEmptyData() {
        byte[] carrier = new byte[100];
        byte[] secret = new byte[0];
        boolean[] isBorder = new boolean[0];
        
        byte[] modified = LsbSteganography.embed(carrier, secret, isBorder);
        boolean[] extractedIsBorder = new boolean[0];
        byte[] extracted = LsbSteganography.extract(modified, 0, extractedIsBorder);
        
        assertArrayEquals("Empty data should be preserved", secret, extracted);
        assertArrayEquals("Empty border flags should match", isBorder, extractedIsBorder);
    }

    @Test
    public void testEmbedAndExtractWithAllBorderValues() {
        byte[] carrier = new byte[100];
        byte[] secret = new byte[10];
        boolean[] isBorder = new boolean[10];
        
        // Set all values to 255 and mark all as border
        for (int i = 0; i < secret.length; i++) {
            secret[i] = (byte) 255;
            isBorder[i] = true;
        }
        
        byte[] modified = LsbSteganography.embed(carrier, secret, isBorder);
        boolean[] extractedIsBorder = new boolean[secret.length];
        byte[] extracted = LsbSteganography.extract(modified, secret.length, extractedIsBorder);
        
        assertArrayEquals("All border values should be preserved", secret, extracted);
        assertArrayEquals("All border flags should be true", isBorder, extractedIsBorder);
    }

    @Test
    public void testEmbedAndExtractWithAlternatingBorderValues() {
        byte[] carrier = new byte[100];
        byte[] secret = new byte[10];
        boolean[] isBorder = new boolean[10];
        
        // Alternate between border and non-border values
        for (int i = 0; i < secret.length; i++) {
            if (i % 2 == 0) {
                secret[i] = (byte) 255;
                isBorder[i] = true;
            } else {
                secret[i] = (byte) (i * 2);
                isBorder[i] = false;
            }
        }
        
        byte[] modified = LsbSteganography.embed(carrier, secret, isBorder);
        boolean[] extractedIsBorder = new boolean[secret.length];
        byte[] extracted = LsbSteganography.extract(modified, secret.length, extractedIsBorder);
        
        assertArrayEquals("Alternating border values should be preserved", secret, extracted);
        assertArrayEquals("Alternating border flags should match", isBorder, extractedIsBorder);
    }

    @Test
    public void testEmbedAndExtractWithMaxCapacity() {
        // Test embedding data that uses exactly all available bits
        int carrierSize = 100;
        int secretSize = carrierSize; // Each byte of carrier can store 1 bit of secret data
        byte[] carrier = new byte[carrierSize];
        byte[] secret = new byte[secretSize];
        boolean[] isBorder = new boolean[secretSize];
        
        for (int i = 0; i < secret.length; i++) {
            secret[i] = (byte) (i % 256);
            isBorder[i] = (i % 2 == 0);
        }
        
        byte[] modified = LsbSteganography.embed(carrier, secret, isBorder);
        boolean[] extractedIsBorder = new boolean[secret.length];
        byte[] extracted = LsbSteganography.extract(modified, secret.length, extractedIsBorder);
        
        assertArrayEquals("Data should be preserved at max capacity", secret, extracted);
        assertArrayEquals("Border flags should match at max capacity", isBorder, extractedIsBorder);
    }

    @Test
    public void testEmbedAndExtractWithZeroValues() {
        byte[] carrier = new byte[100];
        byte[] secret = new byte[10];
        boolean[] isBorder = new boolean[10];
        
        // Set all values to 0
        for (int i = 0; i < secret.length; i++) {
            secret[i] = 0;
            isBorder[i] = false;
        }
        
        byte[] modified = LsbSteganography.embed(carrier, secret, isBorder);
        boolean[] extractedIsBorder = new boolean[secret.length];
        byte[] extracted = LsbSteganography.extract(modified, secret.length, extractedIsBorder);
        
        assertArrayEquals("Zero values should be preserved", secret, extracted);
        assertArrayEquals("Border flags should match for zero values", isBorder, extractedIsBorder);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmbedWithNullCarrier() {
        byte[] secret = new byte[10];
        boolean[] isBorder = new boolean[10];
        LsbSteganography.embed(null, secret, isBorder);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmbedWithNullSecret() {
        byte[] carrier = new byte[100];
        boolean[] isBorder = new boolean[10];
        LsbSteganography.embed(carrier, null, isBorder);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmbedWithNullBorder() {
        byte[] carrier = new byte[100];
        byte[] secret = new byte[10];
        LsbSteganography.embed(carrier, secret, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractWithNullCarrier() {
        boolean[] isBorder = new boolean[10];
        LsbSteganography.extract(null, 10, isBorder);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractWithNullBorder() {
        byte[] carrier = new byte[100];
        LsbSteganography.extract(carrier, 10, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractWithNegativeSize() {
        byte[] carrier = new byte[100];
        boolean[] isBorder = new boolean[10];
        LsbSteganography.extract(carrier, -1, isBorder);
    }

    @Test
    public void testEmbedSingleBorderValue() {
        byte[] carrier = new byte[8];
        byte[] secret = new byte[1];
        boolean[] isBorder = new boolean[1];
        
        secret[0] = (byte) 255;
        isBorder[0] = true;
        
        byte[] modified = LsbSteganography.embed(carrier, secret, isBorder);
        boolean[] extractedIsBorder = new boolean[1];
        byte[] extracted = LsbSteganography.extract(modified, 1, extractedIsBorder);
        
        assertEquals("Single border value should be preserved", (byte) 255, extracted[0]);
        assertTrue("Single border flag should be true", extractedIsBorder[0]);
    }

    @Test
    public void testEmbedSingleNonBorderValue() {
        byte[] carrier = new byte[8];
        byte[] secret = new byte[1];
        boolean[] isBorder = new boolean[1];
        
        secret[0] = (byte) 42;
        isBorder[0] = false;
        
        byte[] modified = LsbSteganography.embed(carrier, secret, isBorder);
        boolean[] extractedIsBorder = new boolean[1];
        byte[] extracted = LsbSteganography.extract(modified, 1, extractedIsBorder);
        
        assertEquals("Single non-border value should be preserved", (byte) 42, extracted[0]);
        assertFalse("Single non-border flag should be false", extractedIsBorder[0]);
    }

    @Test
    public void testEmbedTwoConsecutiveBorderValues() {
        byte[] carrier = new byte[16];
        byte[] secret = new byte[2];
        boolean[] isBorder = new boolean[2];
        
        secret[0] = (byte) 255;
        secret[1] = (byte) 255;
        isBorder[0] = true;
        isBorder[1] = true;
        
        byte[] modified = LsbSteganography.embed(carrier, secret, isBorder);
        boolean[] extractedIsBorder = new boolean[2];
        byte[] extracted = LsbSteganography.extract(modified, 2, extractedIsBorder);
        
        assertArrayEquals("Two consecutive border values should be preserved", secret, extracted);
        assertArrayEquals("Two consecutive border flags should be true", isBorder, extractedIsBorder);
    }

    @Test
    public void testEmbedBorderValueFollowedByNonBorder() {
        byte[] carrier = new byte[16];
        byte[] secret = new byte[2];
        boolean[] isBorder = new boolean[2];
        
        secret[0] = (byte) 255;
        secret[1] = (byte) 42;
        isBorder[0] = true;
        isBorder[1] = false;
        
        byte[] modified = LsbSteganography.embed(carrier, secret, isBorder);
        boolean[] extractedIsBorder = new boolean[2];
        byte[] extracted = LsbSteganography.extract(modified, 2, extractedIsBorder);
        
        assertArrayEquals("Border followed by non-border should be preserved", secret, extracted);
        assertArrayEquals("Border flags should match", isBorder, extractedIsBorder);
    }

    @Test
    public void testEmbedNonBorderValueFollowedByBorder() {
        byte[] carrier = new byte[16];
        byte[] secret = new byte[2];
        boolean[] isBorder = new boolean[2];
        
        secret[0] = (byte) 42;
        secret[1] = (byte) 255;
        isBorder[0] = false;
        isBorder[1] = true;
        
        byte[] modified = LsbSteganography.embed(carrier, secret, isBorder);
        boolean[] extractedIsBorder = new boolean[2];
        byte[] extracted = LsbSteganography.extract(modified, 2, extractedIsBorder);
        
        assertArrayEquals("Non-border followed by border should be preserved", secret, extracted);
        assertArrayEquals("Border flags should match", isBorder, extractedIsBorder);
    }

    @Test
    public void testEmbedWithZeroCarrier() {
        byte[] carrier = new byte[0];
        byte[] secret = new byte[0];
        boolean[] isBorder = new boolean[0];
        
        byte[] modified = LsbSteganography.embed(carrier, secret, isBorder);
        boolean[] extractedIsBorder = new boolean[0];
        byte[] extracted = LsbSteganography.extract(modified, 0, extractedIsBorder);
        
        assertArrayEquals("Empty arrays should be preserved", secret, extracted);
        assertArrayEquals("Empty border flags should match", isBorder, extractedIsBorder);
    }

    @Test
    public void testEmbedWithSingleByteCarrier() {
        byte[] carrier = new byte[1];
        byte[] secret = new byte[1];
        boolean[] isBorder = new boolean[1];
        
        secret[0] = (byte) 42;
        isBorder[0] = false;
        
        byte[] modified = LsbSteganography.embed(carrier, secret, isBorder);
        boolean[] extractedIsBorder = new boolean[1];
        byte[] extracted = LsbSteganography.extract(modified, 1, extractedIsBorder);
        
        assertEquals("Single byte should be preserved", (byte) 42, extracted[0]);
        assertFalse("Single byte border flag should be false", extractedIsBorder[0]);
    }

    @Test
    public void testEmbedWithAllOnesCarrier() {
        byte[] carrier = new byte[8];
        for (int i = 0; i < carrier.length; i++) {
            carrier[i] = (byte) 0xFF;
        }
        
        byte[] secret = new byte[1];
        boolean[] isBorder = new boolean[1];
        
        secret[0] = (byte) 42;
        isBorder[0] = false;
        
        byte[] modified = LsbSteganography.embed(carrier, secret, isBorder);
        boolean[] extractedIsBorder = new boolean[1];
        byte[] extracted = LsbSteganography.extract(modified, 1, extractedIsBorder);
        
        assertEquals("Value should be preserved in all-ones carrier", (byte) 42, extracted[0]);
        assertFalse("Border flag should be false in all-ones carrier", extractedIsBorder[0]);
    }

    @Test
    public void testEmbedWithAllZerosCarrier() {
        byte[] carrier = new byte[8];
        for (int i = 0; i < carrier.length; i++) {
            carrier[i] = 0;
        }
        
        byte[] secret = new byte[1];
        boolean[] isBorder = new boolean[1];
        
        secret[0] = (byte) 42;
        isBorder[0] = false;
        
        byte[] modified = LsbSteganography.embed(carrier, secret, isBorder);
        boolean[] extractedIsBorder = new boolean[1];
        byte[] extracted = LsbSteganography.extract(modified, 1, extractedIsBorder);
        
        assertEquals("Value should be preserved in all-zeros carrier", (byte) 42, extracted[0]);
        assertFalse("Border flag should be false in all-zeros carrier", extractedIsBorder[0]);
    }

    @Test
    public void testEmbedWithAlternatingBitsCarrier() {
        byte[] carrier = new byte[8];
        for (int i = 0; i < carrier.length; i++) {
            carrier[i] = (byte) 0xAA; // 10101010
        }
        
        byte[] secret = new byte[1];
        boolean[] isBorder = new boolean[1];
        
        secret[0] = (byte) 42;
        isBorder[0] = false;
        
        byte[] modified = LsbSteganography.embed(carrier, secret, isBorder);
        boolean[] extractedIsBorder = new boolean[1];
        byte[] extracted = LsbSteganography.extract(modified, 1, extractedIsBorder);
        
        assertEquals("Value should be preserved in alternating bits carrier", (byte) 42, extracted[0]);
        assertFalse("Border flag should be false in alternating bits carrier", extractedIsBorder[0]);
    }

    @Test
    public void testEmbedWithBorderValueInMiddle() {
        byte[] carrier = new byte[24];
        byte[] secret = new byte[3];
        boolean[] isBorder = new boolean[3];
        
        secret[0] = (byte) 42;
        secret[1] = (byte) 255;
        secret[2] = (byte) 42;
        isBorder[0] = false;
        isBorder[1] = true;
        isBorder[2] = false;
        
        byte[] modified = LsbSteganography.embed(carrier, secret, isBorder);
        boolean[] extractedIsBorder = new boolean[3];
        byte[] extracted = LsbSteganography.extract(modified, 3, extractedIsBorder);
        
        assertArrayEquals("Values should be preserved with border in middle", secret, extracted);
        assertArrayEquals("Border flags should match with border in middle", isBorder, extractedIsBorder);
    }
}