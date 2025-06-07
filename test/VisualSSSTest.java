import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class VisualSSSTest {
    
    private static final String RESOURCES_DIR = "resources";
    private static final String PRE_SHADOWS_DIR = RESOURCES_DIR + "/preSombras";
    private static final String SHADOWS_DIR = RESOURCES_DIR + "/sombras";
    private static final String SECRET_PATH = RESOURCES_DIR + "/Facundo.bmp";
    
    @Test
    public void testSeedDistributeEqualsSeedRecoverer() {
        
    }

    @Test
    public void testSecretRecoveryWithDifferentKShadows() throws Exception {
        // Test parameters
        int k = 3;  // Minimum shadows needed
        int n = 5;  // Total number of shadows
        
        // Create shadows directory if it doesn't exist
        new File(SHADOWS_DIR).mkdirs();
        
        // Distribute the secret
        String[] distributeArgs = {
            "-d",
            "-secret", SECRET_PATH,
            "-k", String.valueOf(k),
            "-n", String.valueOf(n),
            "-dir", SHADOWS_DIR
        };
        VisualSSS.main(distributeArgs);
        
        // Verify shadows were created with correct names
        File shadowsDir = new File(SHADOWS_DIR);
        File[] shadows = shadowsDir.listFiles((d, name) -> name.startsWith("sombra") && name.endsWith(".bmp"));
        assertNotNull("Shadows should exist", shadows);
        assertTrue("Should have enough shadows", shadows.length >= k);
        
        // Test recovery with different combinations of k shadows
        for (int i = 0; i < 10; i++) {
            // Create a temporary directory for this test
            String tempDir = "test/temp_" + i;
            new File(tempDir).mkdirs();
            
            // Shuffle and take k shadows
            Arrays.sort(shadows, (a, b) -> (int)(Math.random() * 3 - 1));
            for (int j = 0; j < k; j++) {
                Files.copy(shadows[j].toPath(), 
                          Paths.get(tempDir + "/sombra" + j + ".bmp"));
            }
            
            // Try to recover the secret
            String recoveredPath = "test/recovered_" + i + ".bmp";
            String[] recoverArgs = {
                "-r",
                "-secret", recoveredPath,
                "-k", String.valueOf(k),
                "-n", String.valueOf(n),
                "-dir", tempDir
            };
            VisualSSS.main(recoverArgs);
            
            // Verify the recovered secret matches the original
            BmpImage original = new BmpImage(SECRET_PATH);
            BmpImage recovered = new BmpImage(recoveredPath);
            assertArrayEquals("Recovered secret should match original", 
                            original.getPixelData(), 
                            recovered.getPixelData());
            
            // Clean up
            new File(tempDir).delete();
            new File(recoveredPath).delete();
        }
        
        // Clean up shadow files
        for (File file : shadows) {
            file.delete();
        }
        new File("resources/permutado_secreto.bin").delete();
    }
    
    @Test
    public void testSecretRecoveryWithInsufficientShadows() throws Exception {
        // Test parameters
        int k = 3;  // Minimum shadows needed
        int n = 5;  // Total number of shadows
        
        // Create shadows directory if it doesn't exist
        new File(SHADOWS_DIR).mkdirs();
        
        // Distribute the secret
        String[] distributeArgs = {
            "-d",
            "-secret", SECRET_PATH,
            "-k", String.valueOf(k),
            "-n", String.valueOf(n),
            "-dir", SHADOWS_DIR
        };
        VisualSSS.main(distributeArgs);
        
        // Try to recover with k-1 shadows (should fail)
        String tempDir = "test/temp_insufficient";
        new File(tempDir).mkdirs();
        
        File[] shadows = new File(SHADOWS_DIR).listFiles((d, name) -> name.startsWith("sombra") && name.endsWith(".bmp"));
        for (int i = 0; i < k-1; i++) {
            Files.copy(shadows[i].toPath(), 
                      Paths.get(tempDir + "/sombra" + i + ".bmp"));
        }
        
        String recoveredPath = "test/recovered_insufficient.bmp";
        String[] recoverArgs = {
            "-r",
            "-secret", recoveredPath,
            "-k", String.valueOf(k),
            "-n", String.valueOf(n),
            "-dir", tempDir
        };
        
        try {
            VisualSSS.main(recoverArgs);
            fail("Should have failed with insufficient shadows");
        } catch (Exception e) {
            // Expected exception
        }
        
        // Clean up
        new File(tempDir).delete();
        new File(recoveredPath).delete();
        for (File file : shadows) {
            file.delete();
        }
        new File("resources/permutado_secreto.bin").delete();
    }
    
    @Test
    public void testShadowImagePreservation() throws Exception {
        // Test parameters
        int k = 3;
        int n = 5;
        
        // Create shadows directory if it doesn't exist
        new File(SHADOWS_DIR).mkdirs();
        
        // Distribute the secret
        String[] distributeArgs = {
            "-d",
            "-secret", SECRET_PATH,
            "-k", String.valueOf(k),
            "-n", String.valueOf(n),
            "-dir", SHADOWS_DIR
        };
        VisualSSS.main(distributeArgs);
        
        // Get one of the pre-shadow images
        File preShadowDir = new File(PRE_SHADOWS_DIR);
        File[] preShadows = preShadowDir.listFiles((d, name) -> name.toLowerCase().endsWith(".bmp"));
        assertNotNull("Pre-shadows should exist", preShadows);
        assertTrue("Should have pre-shadows", preShadows.length > 0);
        
        // Get the corresponding shadow
        File shadowDir = new File(SHADOWS_DIR);
        File[] shadows = shadowDir.listFiles((d, name) -> name.startsWith("sombra") && name.endsWith(".bmp"));
        assertNotNull("Shadows should exist", shadows);
        assertTrue("Should have shadows", shadows.length > 0);
        
        // Compare a pre-shadow with its corresponding shadow
        BmpImage preShadow = new BmpImage(preShadows[0].getAbsolutePath());
        BmpImage shadow = new BmpImage(shadows[0].getAbsolutePath());
        
        // Verify that only LSBs are different
        byte[] preShadowData = preShadow.getPixelData();
        byte[] shadowData = shadow.getPixelData();
        
        assertEquals("Shadow should have same size as pre-shadow", 
                    preShadowData.length, 
                    shadowData.length);
        
        for (int i = 0; i < preShadowData.length; i++) {
            // Compare all bits except the LSB
            assertEquals("Only LSB should be different", 
                        preShadowData[i] & 0xFE, 
                        shadowData[i] & 0xFE);
        }
        
        // Clean up
        for (File file : shadows) {
            file.delete();
        }
        new File("resources/permutado_secreto.bin").delete();
    }
}
