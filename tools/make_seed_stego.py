#!/usr/bin/env python3
"""
make_seed_stego.py
Embeds the private seed into web/assets/recovery_notice.png using red-channel LSB steganography.
Payload format:
  - Magic: b"PLMP" (4 bytes)
  - Length: 2 bytes unsigned big-endian integer
  - Payload: ASCII seed string
"""

import os
import sys
import hashlib
from PIL import Image, ImageDraw, ImageFont

MAGIC = b"PLMP"

def embed_seed(seed_str: str, output_path: str):
    payload = MAGIC + len(seed_str).to_bytes(2, "big") + seed_str.encode("ascii")
    
    # Generate bit sequence (MSB first for each byte)
    bits = []
    for byte in payload:
        for i in range(7, -1, -1):
            bits.append((byte >> i) & 1)
            
    width, height = 512, 280
    # Create dark retro terminal background
    img = Image.new("RGBA", (width, height), (18, 20, 24, 255))
    draw = ImageDraw.Draw(img)
    
    # Draw CRT scanline effect & borders
    for y in range(0, height, 4):
        draw.line([(0, y), (width, y)], fill=(12, 14, 18, 255), width=1)
        
    draw.rectangle([8, 8, width - 9, height - 9], outline=(54, 78, 64), width=2)
    draw.rectangle([12, 12, width - 13, height - 13], outline=(38, 54, 44), width=1)
    
    # Draw text
    seed_hash = hashlib.sha256(seed_str.encode("utf-8")).hexdigest()
    
    draw.text((24, 24), "========================================", fill=(96, 172, 116))
    draw.text((24, 40), "PALIMPSEST ARCHIVE RECOVERY NOTICE", fill=(120, 220, 140))
    draw.text((24, 56), "========================================", fill=(96, 172, 116))
    draw.text((24, 84), "IDENTIFIER : PARTITION-0xFF-CH0", fill=(160, 200, 170))
    draw.text((24, 104), f"CHECKSUM   : sha256:{seed_hash[:16]}...", fill=(140, 180, 150))
    draw.text((24, 124), "FORMAT     : MC 26.3 ARCHIVE DUMP", fill=(140, 180, 150))
    draw.text((24, 144), "STATUS     : ENCRYPTED CARRIER", fill=(140, 180, 150))
    draw.text((24, 180), "NOTICE: World seed embedded in carrier matrix.", fill=(180, 210, 190))
    draw.text((24, 200), "Extract red-channel LSB stream (PLMP header).", fill=(120, 160, 130))
    draw.text((24, 236), "[AUTHENTICATION: PENDING WORLD GENERATION]", fill=(96, 172, 116))
    
    # Embed bits into Red channel LSB
    pixels = img.load()
    total_pixels = width * height
    if len(bits) > total_pixels:
        raise ValueError(f"Payload too large: {len(bits)} bits exceeds image capacity {total_pixels}")
        
    bit_idx = 0
    for y in range(height):
        for x in range(width):
            if bit_idx < len(bits):
                r, g, b, a = pixels[x, y]
                r = (r & ~1) | bits[bit_idx]
                pixels[x, y] = (r, g, b, a)
                bit_idx += 1
            else:
                break
        if bit_idx >= len(bits):
            break
            
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    img.save(output_path, "PNG")
    print(f"Embedded {len(payload)} bytes ({len(bits)} bits) into {output_path}")

def extract_seed(image_path: str) -> str:
    img = Image.open(image_path).convert("RGBA")
    pixels = img.load()
    width, height = img.size
    
    # Read bits from red channel LSB
    raw_bytes = bytearray()
    curr_byte = 0
    bit_count = 0
    
    for y in range(height):
        for x in range(width):
            r, _, _, _ = pixels[x, y]
            bit = r & 1
            curr_byte = (curr_byte << 1) | bit
            bit_count += 1
            if bit_count == 8:
                raw_bytes.append(curr_byte)
                curr_byte = 0
                bit_count = 0
                # Check header once 6 bytes are read
                if len(raw_bytes) == 6:
                    if raw_bytes[:4] != MAGIC:
                        raise ValueError(f"Magic mismatch: expected {MAGIC}, got {raw_bytes[:4]}")
                if len(raw_bytes) >= 6:
                    expected_len = int.from_bytes(raw_bytes[4:6], "big")
                    if len(raw_bytes) == 6 + expected_len:
                        seed_str = raw_bytes[6:6 + expected_len].decode("ascii")
                        return seed_str
                        
    raise ValueError("Could not find complete payload in image")

def main():
    seed_file = os.path.join(os.path.dirname(__file__), "..", "private", "seed.txt")
    out_file = os.path.join(os.path.dirname(__file__), "..", "web", "assets", "recovery_notice.png")
    
    if len(sys.argv) > 1 and sys.argv[1] == "--decode":
        extracted = extract_seed(out_file)
        print(f"Extracted seed: {extracted}")
        return
        
    if not os.path.exists(seed_file):
        print(f"Error: {seed_file} does not exist", file=sys.stderr)
        sys.exit(1)
        
    with open(seed_file, "r", encoding="utf-8") as f:
        seed = f.read().strip()
        
    embed_seed(seed, out_file)
    decoded = extract_seed(out_file)
    assert decoded == seed, f"Stego round-trip verification failed: {decoded} != {seed}"
    print(f"Round-trip verification successful! Decoded seed matches: {decoded}")

if __name__ == "__main__":
    main()
