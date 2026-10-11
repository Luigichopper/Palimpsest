#!/usr/bin/env python3
"""
landmark_check.py
Validates landmarks.json schema, coordinates, and configuration bounds.
"""

import json
import os
import sys

def check_landmarks():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    landmarks_file = os.path.join(root, "src", "main", "resources", "data", "palimpsest", "landmarks.json")
    
    if not os.path.exists(landmarks_file):
        print(f"Error: {landmarks_file} does not exist", file=sys.stderr)
        return False
        
    with open(landmarks_file, "r", encoding="utf-8") as f:
        data = json.load(f)
        
    if "landmarks" not in data or not isinstance(data["landmarks"], list):
        print("Error: 'landmarks' array missing from landmarks.json", file=sys.stderr)
        return False
        
    valid_builders = {"cabin", "bunker", "pylon", "dock"}
    seen_ids = set()
    
    for idx, lm in enumerate(data["landmarks"]):
        for key in ["id", "anchorX", "anchorZ", "builder"]:
            if key not in lm:
                print(f"Error: landmark #{idx} missing required key '{key}'", file=sys.stderr)
                return False
                
        lid = lm["id"]
        if lid in seen_ids:
            print(f"Error: duplicate landmark ID '{lid}'", file=sys.stderr)
            return False
        seen_ids.add(lid)
        
        builder = lm["builder"].lower()
        if builder not in valid_builders:
            print(f"Error: landmark '{lid}' has unknown builder '{builder}'", file=sys.stderr)
            return False
            
        # Coordinate sanity checks
        x, z = lm["anchorX"], lm["anchorZ"]
        if not (-30000000 <= x <= 30000000 and -30000000 <= z <= 30000000):
            print(f"Error: landmark '{lid}' coordinates [{x}, {z}] out of bounds", file=sys.stderr)
            return False
            
    print(f"Landmarks check PASSED: {len(data['landmarks'])} landmarks validated successfully.")
    return True

if __name__ == "__main__":
    if not check_landmarks():
        sys.exit(1)
