import sqlite3
import requests
import time
import sys
import os

# Robust path handling
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DB_PATH = os.path.join(BASE_DIR, 'data', 'anuario.db')

# 1. Clear current character data to force re-fetch with new filters
print("1️⃣  Clearing local database...")
try:
    print(f"   Using DB: {DB_PATH}")
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("DELETE FROM characters") # Keep table, remove rows
    conn.commit()
    conn.close()
    print("   ✓ Database cleared.")
except Exception as e:
    print(f"   ✗ Error clearing DB: {e}")
    sys.exit(1)

# 2. Trigger fetch from PotterDB (this will apply the new 'exclude' filters)
print("\n2️⃣  Fetching fresh characters from PotterDB (this may take a moment)...")
try:
    # Timeout increased because it fetches many pages
    resp = requests.get("http://localhost:8000/characters", timeout=60)
    if resp.status_code == 200:
        data = resp.json()
        print(f"   ✓ Loaded {len(data)} characters after filtering.")
    else:
        print(f"   ✗ Failed to load characters: {resp.status_code}")
        sys.exit(1)
except Exception as e:
    print(f"   ✗ Error connecting to backend: {e}")
    print("     Make sure app.py is running!")
    sys.exit(1)

# 3. Trigger Image Sync
print("\n3️⃣  Triggering background image download...")
try:
    resp = requests.post("http://localhost:8000/admin/sync-images")
    if resp.status_code == 200:
        print("   ✓ Background sync started!")
        print("   Check the backend console for progress (Downloading...).")
    else:
        print(f"   ✗ Failed to trigger sync: {resp.status_code}")
except Exception as e:
    print(f"   ✗ Error triggering sync: {e}")
