#!/usr/bin/env python3
import hashlib
import json
import secrets
from pathlib import Path

def generate_seed_hash(seed_file="private/seed.txt", output_file="src/main/resources/palimpsest_seed.json", salt=None):
    seed_path = Path(seed_file)
    if not seed_path.exists():
        raise FileNotFoundError(f"Seed file not found: {seed_file}")
    
    seed = seed_path.read_text(encoding="utf-8").strip()
    if salt is None:
        salt = secrets.token_hex(16)
    
    data_to_hash = (salt + seed).encode("utf-8")
    seed_hash = hashlib.sha256(data_to_hash).hexdigest()
    
    payload = {
        "salt": salt,
        "hash": seed_hash
    }
    
    out_path = Path(output_file)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    print(f"Generated {output_file} from {seed_file} (salt={salt})")

if __name__ == "__main__":
    generate_seed_hash()
