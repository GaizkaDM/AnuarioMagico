"""
Servicio de gestión de imágenes.
Se encarga de descargar, procesar y almacenar imágenes de personajes en formato binario (BLOB).

Autores: Gaizka, Xiker, Diego
"""
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

import concurrent.futures
import time

class ImageService:
    # Store sync status in memory
    sync_status = {
        "running": False,
        "current": 0,
        "total": 0,
        "errors": 0
    }

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
            raw_data = None
            
            # Handle local file URLs (file:/...)
            if url.startswith('file:'):
                from urllib.parse import urlparse, unquote
                import os
                
                # Parse URI
                parsed = urlparse(url)
                file_path = unquote(parsed.path)
                
                # Windows path fix: /C:/Path -> C:/Path
                if os.name == 'nt' and file_path.startswith('/') and len(file_path) > 2 and file_path[2] == ':':
                    file_path = file_path[1:]
                    
                if os.path.exists(file_path):
                    with open(file_path, 'rb') as f:
                        raw_data = f.read()
                else:
                    print(f"File not found: {file_path}")
                    return None
            else:
                # Handle HTTP/HTTPS
                resp = requests.get(url, timeout=10)
                if resp.status_code != 200:
                    return None
                raw_data = resp.content
                
            if not raw_data:
                return None

            return ImageService.process_image_data(raw_data)
        except Exception as e:
            print(f"Image processing error: {e}")
            return None

    @staticmethod
    def process_image_data(raw_data):
        """Procesa datos binarios de imagen: convierte a RGB, ajusta calidad y devuelve JPEG."""
        try:
            img = Image.open(io.BytesIO(raw_data))
            
            if img.mode in ('RGBA', 'LA'):
                bg = Image.new('RGB', img.size, (255, 255, 255))
                # Unmasking if possible, else standard paste
                try:
                    bg.paste(img, mask=img.split()[-1])
                except:
                    bg.paste(img)
                img = bg
            elif img.mode != 'RGB':
                img = img.convert('RGB')
                
            buf = io.BytesIO()
            img.save(buf, format='JPEG', quality=85)
            return buf.getvalue()
        except Exception as e:
            print(f"Error processing image data: {e}")
            return None

    @staticmethod
    def cache_all_images_background():
        print("⚡ Starting background image sync (Parallel)...")
        ImageService.sync_status["running"] = True
        ImageService.sync_status["current"] = 0
        ImageService.sync_status["errors"] = 0
        
        conn = sqlite3.connect(DB_FILE)
        cursor = conn.cursor()
        
        cursor.execute("SELECT id, image FROM characters WHERE image IS NOT NULL AND image_blob IS NULL")
        rows = cursor.fetchall()
        
        total = len(rows)
        ImageService.sync_status["total"] = total
        print(f"⚡ Found {total} images to cache.")
        
        # Close main connection to avoid locking if we use other connections
        conn.close()

        count = 0
        errors = 0
        
        # Helper function for parallel execution
        def download_task(row_data):
            c_id, url = row_data
            if not url: return None
            try:
                processed = ImageService.download_and_process_image(url)
                return (c_id, processed) if processed else None
            except Exception as ex:
                print(f"Error downloading {c_id}: {ex}")
                return None

        # Use ThreadPoolExecutor for parallel downloads
        max_workers = 10
        with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as executor:
            # Submit all tasks
            future_to_char = {executor.submit(download_task, row): row for row in rows}
            
            # Process as they complete
            for future in concurrent.futures.as_completed(future_to_char):
                result = future.result()
                
                if result:
                    char_id, blob = result
                    try:
                        # Quick update in DB
                        # Re-open connection for this write to be safe/simple
                        # (Ideally we could batch updates, but immediate feedback is nice)
                        t_conn = sqlite3.connect(DB_FILE)
                        t_cur = t_conn.cursor()
                        t_cur.execute("UPDATE characters SET image_blob = ? WHERE id = ?", (blob, char_id))
                        t_conn.commit()
                        t_conn.close()
                        count += 1
                    except Exception as db_err:
                        print(f"DB Error saving image {char_id}: {db_err}")
                        errors += 1
                else:
                    errors += 1
                
                # Update status
                ImageService.sync_status["current"] = count
                ImageService.sync_status["errors"] = errors
                
                if count % 5 == 0:
                    print(f"  ⚡ PROGRESS: {count}/{total} (Errors: {errors})")

        ImageService.sync_status["running"] = False
        print(f"⚡ Background sync complete. Cached: {count}, Errors: {errors}")
