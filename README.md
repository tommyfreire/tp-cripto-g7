# Visual Secret Sharing for BMP Images

This project implements a visual secret sharing scheme for BMP images, combining Shamir's Secret Sharing with LSB (Least Significant Bit) steganography. It allows you to split a secret BMP image into multiple "shadows" (shares), such that any subset of `k` out of `n` shadows can reconstruct the original image, but fewer than `k` cannot.

## Features

- **Secret Distribution:** Split a BMP image into `n` shadow images, embedding the shares into the LSBs of carrier BMP images.
- **Secret Recovery:** Reconstruct the original BMP image from any `k` shadow images.
- **Steganography:** Uses LSB steganography to hide share data within carrier images.
- **Shamir's Secret Sharing:** Ensures information-theoretic security; fewer than `k` shares reveal nothing about the secret.
- **Automatic 256 Avoidance:** The algorithm ensures no shadow pixel value is ever 256, as per the referenced paper, by dynamically adjusting coefficients.
- **Easy Scripted Usage:** Includes a script for easy running, cleaning, and compilation.

## Directory Structure (Updated)

- **Carrier images for distribution:** `resources/preSombras/`
- **Generated shadow images:** `resources/sombras/`
- **Default recovery location:** `resources/sombras/`

## How It Works

1. **Distribute Mode (`-d`):**
   - The secret image is permuted for extra security.
   - The permuted data is split using Shamir's Secret Sharing.
   - Each share is embedded into a carrier BMP image from `resources/preSombras/` using LSB steganography.
   - Shadow images are saved in `resources/sombras/` with metadata in their headers.
   - If any polynomial evaluation yields 256, the first nonzero coefficient is decremented and the process is retried until all values are in [0, 255].

2. **Recover Mode (`-r`):**
   - `k` shadow images are selected from `resources/sombras/`.
   - The embedded data is extracted and the original permuted secret is reconstructed using modular linear algebra.
   - The permutation is reversed to recover the original image.
   - The BMP header for the output is taken from the first available pre-shadow image in `resources/preSombras/`.

## Usage

### Using the Script (Recommended)

A script `visualsss.sh` is provided for easy running, cleaning, and compilation.

```sh
# Distribute secret (uses carriers from resources/preSombras/)
./visualsss.sh -d -secret resources/Alfred.bmp -k 3 -n 5

# Recover secret (uses shadows from resources/sombras/)
./visualsss.sh -r -secret resources/recuperado.bmp -k 3 -n 5

# Clean generated shadows and output files
./visualsss.sh clean

# Clean Java binaries in bin/
./visualsss.sh clean -b
```

- `-d`: Distribute mode (split secret)
- `-r`: Recover mode (reconstruct secret)
- `-secret <file>`: Path to the secret BMP file (for distribute) or output file (for recover)
- `-k <num>`: Minimum number of shares required to reconstruct the secret
- `-n <num>`: Total number of shares to create (optional for recovery, required for distribution)
- `-dir <directory>`: Directory containing carrier BMP images (for distribute) or shadow images (for recover)
However, the script will prompt for missing parameters and always compile the Java sources before running.
You do not need to specify carrier or shadow directories unless using custom locations.

### Manual Command Line

```sh
# Distribute secret
java -cp bin VisualSSS -d -secret resources/Alfred.bmp -k 3 -n 5 -dir resources/sombras

# Recover secret
java -cp bin VisualSSS -r -secret resources/recuperado.bmp -k 3 -n 5 -dir resources/sombras
```

## Running and Testing the Project

### 1. Compile the Project (if not using the script)

```sh
javac -d bin src/*.java
```

### 2. Prepare Images

- **Secret Image:**  Place your secret BMP image (e.g., `Alfred.bmp`) in `resources/`.
- **Carrier Images:**  Place at least `n` BMP images in `resources/preSombras/` (must be the same size as the secret image).

#### Requirements for carrier images
- **When k = 8:** All carrier images must have the same size (width and height, however we are actually using the same amount of pixels) as the secret image. If this condition is not met, the program will show an error and not continue. During recovery, all selected shadow images must also have the same size; otherwise, the program will show an error and abort.
- **When k ≠ 8:** Carrier images can have any size, but must be large enough to hide all the data needed using LSB replacement. This means the number of pixels must be at least equal to the amount of data to hide.

### 3. Distribute the Secret

```sh
./visualsss.sh -d -secret resources/Alfred.bmp -k 3 -n 5
```
- This will create `sombra1.bmp`, `sombra2.bmp`, ..., `sombra5.bmp` in `resources/sombras/`.
- The hiding method used is LSB replacement, starting from the first pixel of the carrier image.

### 4. Recover the Secret

```sh
./visualsss.sh -r -secret resources/recuperado.bmp -k 3 -n 5
```
- This will reconstruct the secret and save it as `resources/recuperado.bmp`.
- **Note:** For (8, n) schemes, all selected shadow images must have the same size (number of pixels). If not, the program will show an error and abort.

### 5. Clean Up

```sh
./visualsss.sh clean      # Remove generated shadows and output
./visualsss.sh clean -b   # Remove generated shadows and output and also Java binaries in bin/
```

### 6. Troubleshooting

- **Class Not Found:**  The script compiles automatically, but if running manually, ensure you use `-cp bin` and that `bin/VisualSSS.class` exists.
- **Image Size:**  Carrier images must be at least as large as the secret image.
- **Divisibility:**  The number of bytes in the secret image must be divisible by `k`.
- **No 256 Values:**  The algorithm ensures no shadow pixel value is ever 256.

### 7. Example Directory Structure

```
tp-cripto-g7/
  visualsss.sh
  src/
    *.java
  bin/
    *.class
  resources/
    Alfred.bmp
    preSombras/
      carrier1.bmp
      carrier2.bmp
      ...
    sombras/
      sombra1.bmp
      sombra2.bmp
      ...
```

## File Structure

- `src/VisualSSS.java`: Main entry point, argument parsing, orchestrates distribution and recovery.
- `src/SecretDistributor.java`: Handles splitting and embedding the secret, and ensures no 256 values in shadows.
- `src/SecretRecoverer.java`: Handles extracting and reconstructing the secret.
- `src/LsbSteganography.java`: LSB steganography utilities.
- `src/BmpImage.java`: BMP image reading/writing utilities.
- `src/PermutationTable.java`: Pseudo-random permutation for extra security.
- `visualsss.sh`: Script for easy running, cleaning, and compilation.

## Requirements

- Java 8 or higher
- Only supports 24-bit BMP images

## Notes

- The number of bytes in the secret image must be divisible by `k`.
- The carrier images must be at least as large as the secret image.
- The program stores metadata (seed, shadow ID, etc.) in reserved bytes of the BMP header.
- The script automates compilation and directory management for you.
- The algorithm dynamically adjusts coefficients to avoid 256 in shadow pixels, as per the referenced paper.

**Authors:**  
Grupo 7:  
Braun, Santos; Freire, Tomás; Vella, Mauro
