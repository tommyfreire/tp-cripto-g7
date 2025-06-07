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
        for (int i = 0; i < secretData.length; i++) {
            secretData[i] = (byte) (i * 2);
        }
        
        // Embed the secret data
        byte[] modifiedImage = LsbSteganography.embed(originalImage, secretData);
        
        // Verify that only LSBs were modified
        for (int i = 0; i < originalImage.length; i++) {
            // Compare all bits except the LSB
            assertEquals("Only LSB should be different", 
                originalImage[i] & 0xFE, 
                modifiedImage[i] & 0xFE);
        }
        
        // Extract the hidden data
        byte[] extractedData = LsbSteganography.extract(modifiedImage, secretData.length);
        // Verify the extracted data matches the original secret
        assertArrayEquals("Extracted data should match original secret", 
            secretData, 
            extractedData);
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
                    
                    // Fill with random data
                    for (int i = 0; i < carrier.length; i++) {
                        carrier[i] = (byte) (Math.random() * 256);
                    }
                    for (int i = 0; i < secret.length; i++) {
                        secret[i] = (byte) (Math.random() * 256);
                    }
                    
                    byte[] modified = LsbSteganography.embed(carrier, secret);
                    byte[] extracted = LsbSteganography.extract(modified, secret.length);
                    
                    assertArrayEquals("Data should be preserved for carrier size " + carrierSize + 
                        " and secret size " + secretSize, secret, extracted);
                }
            }
        }
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testEmbedTooMuchData() {
        byte[] carrier = new byte[100];
        byte[] secret = new byte[101]; // Too much data for the carrier
        LsbSteganography.embed(carrier, secret);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testExtractTooMuchData() {
        byte[] carrier = new byte[100];
        LsbSteganography.extract(carrier, 101); // Try to extract more data than possible
    }
}