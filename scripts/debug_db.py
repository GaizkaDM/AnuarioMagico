import sqlite3
import os

db_path = r'c:\Users\GaizkaClase\Desktop\AnuarioMagico\data\anuario.db'

if not os.path.exists(db_path):
    print(f"Database not found at {db_path}")
else:
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        # Check pending images
        cursor.execute("SELECT COUNT(*) FROM characters WHERE image IS NOT NULL AND image != '' AND image_blob IS NULL")
        pending = cursor.fetchone()[0]
        print(f"Pending images: {pending}")
        
        # Check total characters
        cursor.execute("SELECT COUNT(*) FROM characters")
        total = cursor.fetchone()[0]
        print(f"Total characters: {total}")

        # Check latest timestamp if exists
        # cursor.execute("SELECT MAX(last_updated) FROM characters")
        # print(f"Latest update: {cursor.fetchone()[0]}")
        
        conn.close()
    except Exception as e:
        print(f"Error querying database: {e}")
