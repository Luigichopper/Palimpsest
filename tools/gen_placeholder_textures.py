#!/usr/bin/env python3
import os
from PIL import Image

def generate_textures():
    base_dir = "src/main/resources/assets/palimpsest/textures"
    os.makedirs(f"{base_dir}/block", exist_ok=True)
    os.makedirs(f"{base_dir}/item", exist_ok=True)
    
    # Placeholder terminal block texture
    term_img = Image.new("RGBA", (16, 16), (35, 40, 45, 255))
    for x in range(2, 14):
        term_img.putpixel((x, 2), (20, 25, 30, 255))
        term_img.putpixel((x, 13), (20, 25, 30, 255))
    for y in range(2, 14):
        term_img.putpixel((2, y), (20, 25, 30, 255))
        term_img.putpixel((13, y), (20, 25, 30, 255))
    # Green phosphor screen
    for x in range(4, 12):
        for y in range(4, 11):
            term_img.putpixel((x, y), (15, 60, 25, 255))
    term_img.save(f"{base_dir}/block/terminal.png")

    # Placeholder diff lens item texture
    lens_img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(4, 12):
        for y in range(4, 12):
            lens_img.putpixel((x, y), (70, 130, 180, 180))
    lens_img.save(f"{base_dir}/item/diff_lens.png")
    print("Generated placeholder textures.")

if __name__ == "__main__":
    generate_textures()
