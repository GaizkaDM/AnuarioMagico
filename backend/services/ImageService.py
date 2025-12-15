import requests
import io
from PIL import Image
import requests
import io
from PIL import Image
import sqlite3
# Adjust path to find config in parent directory or use absolute import
import sys
import os
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
from config import DB_FILE

class ImageService:
    @staticmethod
    def get_image_from_db(character_id):
        conn = sqlite3.connect(DB_FILE)
        cursor = conn.cursor()
        cursor.execute('SELECT image_blob, image FROM characters WHERE id = ?', (character_id,))
        row = cursor.fetchone()
        conn.close()
        return row

    @staticmethod
    def cache_image(character_id, image_data):
        conn = sqlite3.connect(DB_FILE)
        cursor = conn.cursor()
        cursor.execute('UPDATE characters SET image_blob = ? WHERE id = ?', (image_data, character_id))
        conn.commit()
        conn.close()

    @staticmethod
    def download_and_process_image(url):
        if not url:
            return None
            
        try:
            resp = requests.get(url, timeout=10)
            if resp.status_code != 200:
                return None
                
            raw_data = resp.content
            img = Image.open(io.BytesIO(raw_data))
            
            if img.mode in ('RGBA', 'LA'):
                bg = Image.new('RGB', img.size, (255, 255, 255))
                bg.paste(img, mask=img.split()[-1])
                img = bg
            elif img.mode != 'RGB':
                img = img.convert('RGB')
                
            buf = io.BytesIO()
            img.save(buf, format='JPEG', quality=85)
            return buf.getvalue()
        except Exception as e:
            print(f"Image processing error: {e}")
            return None

    @staticmethod
    def cache_all_images_background():
        print("⚡ Starting background image sync...")
        conn = sqlite3.connect(DB_FILE)
        cursor = conn.cursor()
        
        cursor.execute("SELECT id, image FROM characters WHERE image IS NOT NULL AND image_blob IS NULL")
        rows = cursor.fetchall()
        
        total = len(rows)
        print(f"⚡ Found {total} images to cache.")
        
        count = 0
        errors = 0
        
        for row in rows:
            char_id, original_url = row
            if not original_url: continue
                
            try:
                processed_data = ImageService.download_and_process_image(original_url)
                if processed_data:
                    t_conn = sqlite3.connect(DB_FILE)
                    t_cur = t_conn.cursor()
                    t_cur.execute("UPDATE characters SET image_blob = ? WHERE id = ?", (processed_data, char_id))
                    t_conn.commit()
                    t_conn.close()
                    count += 1
                    if count % 10 == 0:
                        print(f"  ⚡ Cached {count}/{total} images...")
                else:
                    errors += 1
            except Exception as e:
                errors += 1
                print(f"  ✗ Failed to download {char_id}: {e}")
                
        conn.close()
        print(f"⚡ Background sync complete. Cached: {count}, Errors: {errors}")
