# Visual Secret Sharing for BMP Images

This project implements a visual secret sharing scheme for BMP images, combining Shamir's Secret Sharing with LSB (Least Significant Bit) steganography. It allows you to split a secret BMP image into multiple "shadows" (shares), such that any subset of `k` out of `n` shadows can reconstruct the original image, but fewer than `k` cannot.

## Features

- **Secret Distribution:** Split a BMP image into `n` shadow images, embedding the shares into the LSBs of carrier BMP images.
- **Secret Recovery:** Reconstruct the original BMP image from any `k` shadow images.
- **Steganography:** Uses LSB steganography to hide share data within carrier images.
- **Shamir's Secret Sharing:** Ensures information-theoretic security; fewer than `k` shares reveal nothing about the secret.

## How It Works

1. **Distribute Mode (`-d`):**
   - The secret image is permuted for extra security.
   - The permuted data is split using Shamir's Secret Sharing.
   - Each share is embedded into a carrier BMP image using LSB steganography.
   - Shadow images are saved with metadata in their headers.

2. **Recover Mode (`-r`):**
   - `k` shadow images are selected.
   - The embedded data is extracted and the original permuted secret is reconstructed using modular linear algebra.
   - The permutation is reversed to recover the original image.

## Usage

### Command Line

```sh
# Distribute secret
java VisualSSS -d -secret <secret.bmp> -k <threshold> -n <num_shadows> -dir <carrier_images_dir>

# Recover secret
java VisualSSS -r -secret <output.bmp> -k <threshold> -n <num_shadows> -dir <shadows_dir>
```

- `-d`: Distribute mode (split secret)
- `-r`: Recover mode (reconstruct secret)
- `-secret <file>`: Path to the secret BMP file (for distribute) or output file (for recover)
- `-k <num>`: Minimum number of shares required to reconstruct the secret
- `-n <num>`: Total number of shares to create (optional for recovery, required for distribution)
- `-dir <directory>`: Directory containing carrier BMP images (for distribute) or shadow images (for recover)

### Example

```sh
# Distribute a secret image into 5 shares, requiring any 3 to recover
java VisualSSS -d -secret secret.bmp -k 3 -n 5 -dir resources/

# Recover the secret image from any 3 shadow images
java VisualSSS -r -secret recovered.bmp -k 3 -n 5 -dir shadows/
```

## Running and Testing the Project

### 1. Compile the Project

Open a terminal in your project root (where `src/` is located) and run:

```sh
javac -d bin src/*.java
```

- This compiles all Java files in `src/` and puts the `.class` files in a new `bin/` directory.

### 2. Prepare Test Images

- **Secret Image:**  
  You need a BMP image (e.g., `secret.bmp`) that you want to split.
- **Carrier Images:**  
  You need at least `n` BMP images (e.g., `carrier1.bmp`, `carrier2.bmp`, ...) in a directory (e.g., `resources/`).  
  These should be the same size as the secret image.

### 3. Distribute the Secret

Suppose you want to split `secret.bmp` into 5 shares, with a threshold of 3, using carrier images in `resources/`:

```sh
java -cp bin VisualSSS -d -secret secret.bmp -k 3 -n 5 -dir resources/
```

- This will create `sombra1.bmp`, `sombra2.bmp`, ..., `sombra5.bmp` in the `resources/` directory.

### 4. Test Recovery

To recover the secret from any 3 of the shadow images (e.g., after copying 3 of them to a directory called `shadows/`):

```sh
java -cp bin VisualSSS -r -secret recovered.bmp -k 3 -n 5 -dir shadows/
```

- This will reconstruct the secret and save it as `recovered.bmp`.

### 5. Check the Result

- Open `recovered.bmp` with any image viewer.
- It should look identical to your original `secret.bmp`.

### 6. Troubleshooting

- **Class Not Found:**  
  Make sure you use `-cp bin` and that `bin/VisualSSS.class` exists.
- **BMP Format:**  
  Only 24-bit BMP images are supported.
- **Image Size:**  
  Carrier images must be at least as large as the secret image.
- **Divisibility:**  
  The number of bytes in the secret image must be divisible by `k`.

### 7. Example Directory Structure

```
tp-cripto-g7/
  src/
    *.java
  bin/
    *.class
  secret.bmp
  carriers/
    carrier1.bmp
    carrier2.bmp
    carrier3.bmp
    carrier4.bmp
    carrier5.bmp
  shadows/
    sombra1.bmp
    sombra2.bmp
    sombra3.bmp
```

## File Structure

- `src/VisualSSS.java`: Main entry point, argument parsing, orchestrates distribution and recovery.
- `src/SecretDistributor.java`: Handles splitting and embedding the secret.
- `src/SecretRecoverer.java`: Handles extracting and reconstructing the secret.
- `src/LsbSteganography.java`: LSB steganography utilities.
- `src/BmpImage.java`: BMP image reading/writing utilities.
- `src/PermutationTable.java`: Pseudo-random permutation for extra security.

## Requirements

- Java 8 or higher
- Only supports 24-bit BMP images

## Notes

- The number of bytes in the secret image must be divisible by `k`.
- The carrier images must be at least as large as the secret image.
- The program stores metadata (seed, shadow ID, etc.) in reserved bytes of the BMP header.

**Authors:**  
Grupo 7:
Braun, Santos; Freire, Tomás; Vella, Mauro
