from flask import Flask, jsonify, request, Response
from flask_cors import CORS
import requests
import sqlite3
import hashlib
from datetime import datetime, timedelta
import io
import threading
import json
from PIL import Image
import mysql.connector
from mysql.connector import Error

app = Flask(__name__)
CORS(app)  # Enable CORS for JavaFX client

# PotterDB API base URL
POTTERDB_API = "https://api.potterdb.com/v1/characters"

# Database file
DB_FILE = 'favorites.db'

# MySQL Configuration
MYSQL_CONFIG = {
    'host': '127.0.0.1',
    'port': 3309,
    'database': 'hogwarts',
    'user': 'appuser',
    'password': 'appPass123'
}

def init_db():
    """Initialize SQLite database for favorites and characters"""
    conn = sqlite3.connect(DB_FILE)
    cursor = conn.cursor()
    
    # Tabla de favoritos
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS favorites (
            character_id TEXT PRIMARY KEY,
            is_favorite INTEGER DEFAULT 0
        )
    ''')
    
    # Tabla de personajes (caché persistente)
    # Tabla de personajes (caché persistente)
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS characters (
            id TEXT PRIMARY KEY,
            name TEXT,
            house TEXT,
            image TEXT,
            died TEXT,
            born TEXT,
            patronus TEXT,
            gender TEXT,
            species TEXT,
            blood_status TEXT,
            role TEXT,
            wiki TEXT,
            slug TEXT,
            image_blob BLOB,
            alias_names TEXT,
            animagus TEXT,
            boggart TEXT,
            eye_color TEXT,
            family_member TEXT,
            hair_color TEXT,
            height TEXT,
            jobs TEXT,
            nationality TEXT,
            romances TEXT,
            skin_color TEXT,
            titles TEXT,
            wand TEXT,
            weight TEXT
        )
    ''')
    
    # Migration: Check for new columns and add them if missing
    cursor.execute("PRAGMA table_info(characters)")
    existing_columns = [info[1] for info in cursor.fetchall()]
    
    columns_to_add = {
        'image_blob': 'BLOB',
        'alias_names': 'TEXT',
        'animagus': 'TEXT',
        'boggart': 'TEXT',
        'eye_color': 'TEXT',
        'family_member': 'TEXT',
        'hair_color': 'TEXT',
        'height': 'TEXT',
        'jobs': 'TEXT',
        'nationality': 'TEXT',
        'romances': 'TEXT',
        'skin_color': 'TEXT',
        'titles': 'TEXT',
        'wand': 'TEXT',
        'weight': 'TEXT'
    }
    
    for col_name, col_type in columns_to_add.items():
        if col_name not in existing_columns:
            print(f"⚠ Migrating database: Adding {col_name} column...")
            cursor.execute(f"ALTER TABLE characters ADD COLUMN {col_name} {col_type}")
    
    conn.commit()
    conn.close()


def get_favorite_status(character_id):
    """Get favorite status for a character"""
    conn = sqlite3.connect(DB_FILE)
    cursor = conn.cursor()
    cursor.execute('SELECT 1 FROM favorites WHERE character_id = ?', (character_id,))
    result = cursor.fetchone()
    conn.close()
    return True if result else False


def set_favorite_status(character_id, is_favorite):
    """Set favorite status for a character"""
    conn = sqlite3.connect(DB_FILE)
    cursor = conn.cursor()
    
    if is_favorite:
        cursor.execute('''
            INSERT OR IGNORE INTO favorites (character_id, is_favorite)
            VALUES (?, 1)
        ''', (character_id,))
    else:
        cursor.execute('DELETE FROM favorites WHERE character_id = ?', (character_id,))
        
    conn.commit()
    conn.close()


def save_characters_to_db(characters):
    """Save a list of characters to the database"""
    conn = sqlite3.connect(DB_FILE)
    cursor = conn.cursor()
    
    for char in characters:
        # Check if we already have the blob, don't overwrite it with NULL if we do
        cursor.execute('SELECT image_blob FROM characters WHERE id = ?', (char['id'],))
        existing_blob = cursor.fetchone()
        
        # We only update non-blob fields, or insert if new. 
        # But for simplicity, we can use the ON CONFLICT clause or a check.
        # Here we just re-insert. Ideally we should merge.
        # A simpler way without losing blob:
        
        image_blob = None
        if existing_blob and existing_blob[0]:
            image_blob = existing_blob[0]

        cursor.execute('''
            INSERT OR REPLACE INTO characters (
                id, name, house, image, died, born, patronus, 
                gender, species, blood_status, role, wiki, slug, image_blob,
                alias_names, animagus, boggart, eye_color, family_member,
                hair_color, height, jobs, nationality, romances,
                skin_color, titles, wand, weight
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (
            char['id'], char['name'], char['house'], char['image'], 
            char['died'], char['born'], char['patronus'],
            char['gender'], char['species'], char['blood_status'], 
            char['role'], char['wiki'], char.get('slug', ''),
            image_blob,
            json.dumps(char.get('alias_names', [])),
            char.get('animagus', ''),
            char.get('boggart', ''),
            char.get('eye_color', ''),
            json.dumps(char.get('family_member', [])),
            char.get('hair_color', ''),
            char.get('height', ''),
            json.dumps(char.get('jobs', [])),
            char.get('nationality', ''),
            json.dumps(char.get('romances', [])),
            char.get('skin_color', ''),
            json.dumps(char.get('titles', [])),
            json.dumps(char.get('wand', [])),
            char.get('weight', '')
        ))
        
    conn.commit()
    conn.close()


def load_characters_from_db():
    """Load all characters from the database"""
    conn = sqlite3.connect(DB_FILE)
    conn.row_factory = sqlite3.Row  # Para acceder por nombre de columna
    cursor = conn.cursor()
    
    cursor.execute('SELECT * FROM characters')
    rows = cursor.fetchall()
    
    characters = []
    for row in rows:
        char_dict = dict(row)
        # Quitar el blob del listado general para no saturar la respuesta JSON
        if 'image_blob' in char_dict:
            del char_dict['image_blob']
            
        # Modificar URL de imagen para apuntar a local
        char_dict['image'] = f"http://localhost:8000/characters/{char_dict['id']}/image"
            
        # Parse JSON fields back to lists
        json_fields = ['alias_names', 'family_member', 'jobs', 'romances', 'titles', 'wand']
        for field in json_fields:
            if field in char_dict and char_dict[field]:
                try:
                    char_dict[field] = json.loads(char_dict[field])
                except json.JSONDecodeError:
                    char_dict[field] = []
            else:
                 char_dict[field] = []

        # Añadir estado de favorito
        char_dict['is_favorite'] = get_favorite_status(char_dict['id'])
        characters.append(char_dict)
        
    conn.close()
    return characters


def generate_id(slug):
    """Generate a consistent ID from PotterDB slug"""
    if slug:
        return slug
    # Fallback: generate from hash if no slug
    return hashlib.md5(str(slug).encode()).hexdigest()[:16]


@app.route('/characters', methods=['GET'])
def get_characters():
    """
    Fetch characters. Tries local DB first, then API.
    """
    try:
        # 1. Intentar cargar de base de datos local
        local_characters = load_characters_from_db()
        
        if local_characters:
            print(f"✓ Returning {len(local_characters)} characters from local SQLite DB")
            
            # Apply filtering if requested
            if request.args.get('filter') == 'favorites':
                local_characters = [c for c in local_characters if c['is_favorite']]
                
            return jsonify(local_characters)
        
        # 2. Si no hay datos, descargar de PotterDB API
        print("⟳ Local DB empty. Fetching fresh data from PotterDB API...")
        all_characters = []
        page = 1
        
        # Fetch all pages from the API
        while True:
            print(f"  Fetching page {page}...")
            response = requests.get(f"{POTTERDB_API}?page[number]={page}")
            
            if response.status_code != 200:
                return jsonify({"error": "Failed to fetch from PotterDB"}), 500
            
            data = response.json()
            characters = data.get('data', [])
            
            if not characters:
                break
            
            all_characters.extend(characters)
            
            # Check if there are more pages
            meta = data.get('meta', {})
            pagination = meta.get('pagination', {})
            if page >= pagination.get('last', 1):
                break
            
            page += 1
        
        print(f"  Fetched {len(all_characters)} total characters from API")
        
        # Filter and transform data
        filtered_characters = []
        exclude_keywords = ['unidentified', 'unknown', 'student', 'girl', 'boy', 'man', 'woman', 'baby', 'child', 'spectator', 'team', 'gang', 'group', 'troll', 'portrait', 'house-elf', 'ghost', 'ghosts', 'champion', 'mentor', 'creature', 'abraxan', 'actor', 'announcer', 'cat', 'killer', 'enemy', 'antipodean', 'shopkeeper', 'ashwinder', 'reserve', 'augurey', 'aunt', 'mascot', 'grandmother', 'beggar', 'bespectacled', 'corridor', 'boa constrictor', 'ministry', 'hagrid\'s', 'enthusiast', 'fiancee', 'fianceé', 'friends', 'chief', 'victim', 'guard', 'guards', 'witch', 'wizard', 'waiter', 'deer', 'cousin', 'cousins', 'sister', 'sisters', 'brother', 'brothers', 'father', 'grandfather', 'parents', 'great-grandmother']
        
        for character in all_characters:
            attributes = character.get('attributes', {})
            name = attributes.get('name', '')
            image_url = attributes.get('image')
            
            # Filter: must have image
            if not image_url:
                continue
            
            # Filter: exclude keywords in name
            if any(keyword.lower() in name.lower() for keyword in exclude_keywords):
                continue
            
            # Generate ID from slug
            slug = attributes.get('slug', '')
            character_id = generate_id(slug)
            
            # Build complete response object
            # Build complete response object
            char_obj = {
                'id': character_id,
                'name': name,
                'house': attributes.get('house') or '',
                'image': image_url,
                'died': attributes.get('died') or '',
                'born': attributes.get('born') or '',
                'patronus': attributes.get('patronus') or '',
                'gender': attributes.get('gender') or '',
                'species': attributes.get('species') or '',
                'blood_status': attributes.get('blood_status') or '',
                'role': attributes.get('role') or '',
                'wiki': attributes.get('wiki') or '',
                'slug': slug,
                'alias_names': attributes.get('alias_names', []),
                'animagus': attributes.get('animagus') or '',
                'boggart': attributes.get('boggart') or '',
                'eye_color': attributes.get('eye_color') or '',
                'family_member': attributes.get('family_members', []),
                'hair_color': attributes.get('hair_color') or '',
                'height': attributes.get('height') or '',
                'jobs': attributes.get('jobs', []),
                'nationality': attributes.get('nationality') or '',
                'romances': attributes.get('romances', []),
                'skin_color': attributes.get('skin_color') or '',
                'titles': attributes.get('titles', []),
                'wand': attributes.get('wands', []),
                'weight': attributes.get('weight') or ''
            }
            filtered_characters.append(char_obj)
        
        # 3. Guardar en base de datos local
        save_characters_to_db(filtered_characters)
        print(f"✓ Saved {len(filtered_characters)} characters to SQLite DB")
        
        # 4. Recargar desde DB para tener las URLs locales correctas
        return get_characters()
    
    except Exception as e:
        print(f"✗ Error: {str(e)}")
        return jsonify({"error": str(e)}), 500


@app.route('/characters/<character_id>/image', methods=['GET'])
def get_character_image(character_id):
    """
    Get character image. 
    Serve from BLOB if exists, otherwise download, cache and serve.
    """
    try:
        conn = sqlite3.connect(DB_FILE)
        cursor = conn.cursor()
        
        # Buscar blob y url original
        cursor.execute('SELECT image_blob, image FROM characters WHERE id = ?', (character_id,))
        row = cursor.fetchone()
        
        if not row:
            conn.close()
            return jsonify({'error': 'Character not found'}), 404
            
        blob_data, original_url = row
        
        # Si tenemos blob, servirlo
        if blob_data:
            conn.close()
            return Response(blob_data, mimetype='image/jpeg')
            
        # Si no hay blob, descargar
        if not original_url:
            conn.close()
            return jsonify({'error': 'No image URL for this character'}), 404
            
        print(f"⬇ Downloading image for {character_id}...")
        image_response = requests.get(original_url)
        
        if image_response.status_code == 200:
            image_raw_data = image_response.content
            
            # Convert to JPEG using Pillow
            try:
                img = Image.open(io.BytesIO(image_raw_data))
                
                # Convert RGBA to RGB if necessary (e.g. for PNG/WebP with transparency)
                if img.mode in ('RGBA', 'LA'):
                    background = Image.new('RGB', img.size, (255, 255, 255))
                    background.paste(img, mask=img.split()[-1])
                    img = background
                elif img.mode != 'RGB':
                    img = img.convert('RGB')
                
                output_buffer = io.BytesIO()
                img.save(output_buffer, format='JPEG', quality=90)
                final_image_data = output_buffer.getvalue()
                
                print(f"✓ Converted image to JPEG for {character_id}")
            except Exception as e:
                print(f"⚠ Failed to convert image: {e}. Saving raw data which might fail in JavaFX 11.")
                final_image_data = image_raw_data

            # Guardar en blob
            cursor.execute('UPDATE characters SET image_blob = ? WHERE id = ?', (final_image_data, character_id))
            conn.commit()
            print(f"✓ Image cached for {character_id}")
            
            conn.close()
            return Response(final_image_data, mimetype='image/jpeg')
        else:
            conn.close()
            return jsonify({'error': 'Failed to download image'}), 502

    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/characters/<character_id>/favorite', methods=['POST'])
def toggle_favorite(character_id):
    """
    Toggle favorite status for a character.
    If 'is_favorite' is provided in JSON, set to that value.
    Otherwise, flip current status.
    """
    try:
        # Check current status
        current_status = get_favorite_status(character_id)
        
        # Determine new status
        data = request.get_json(silent=True)
        if data and 'is_favorite' in data:
            new_status = data['is_favorite']
        else:
            new_status = not current_status
        
        set_favorite_status(character_id, new_status)
        
        return jsonify({
            'success': True,
            'character_id': character_id,
            'is_favorite': new_status,
            'action': 'toggled' if data is None or 'is_favorite' not in data else 'set'
        })
    
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/health', methods=['GET'])
def health():
    """Health check endpoint"""
    try:
        conn = sqlite3.connect(DB_FILE)
        cursor = conn.cursor()
        cursor.execute('SELECT COUNT(*) FROM characters')
        count = cursor.fetchone()[0]
        
        cursor.execute('SELECT COUNT(*) FROM characters WHERE image_blob IS NOT NULL')
        cached_images = cursor.fetchone()[0]
        
        conn.close()
        db_status = "connected"
    except Exception as e:
        db_status = f"error: {str(e)}"
        count = 0
        cached_images = 0

    return jsonify({
        "status": "ok",
        "message": "Backend is running on port 8000",
        "db_status": db_status,
        "cached_characters": count,
        "cached_images": cached_images
    })


def cache_all_images_background():
    """Background task to download and cache all images"""
    print("⚡ Starting background image sync...")
    conn = sqlite3.connect(DB_FILE)
    cursor = conn.cursor()
    
    # Get all characters with an image URL but NO blob
    cursor.execute("SELECT id, image FROM characters WHERE image IS NOT NULL AND image_blob IS NULL")
    rows = cursor.fetchall()
    conn.close() 
    
    total = len(rows)
    print(f"⚡ Found {total} images to cache.")
    
    count = 0
    errors = 0
    
    for row in rows:
        char_id, original_url = row
        if not original_url: 
            continue
            
        try:
            # Download
            resp = requests.get(original_url, timeout=10)
            if resp.status_code == 200:
                raw_data = resp.content
                final_data = raw_data
                
                # Convert
                try:
                    img = Image.open(io.BytesIO(raw_data))
                    if img.mode in ('RGBA', 'LA'):
                        bg = Image.new('RGB', img.size, (255, 255, 255))
                        bg.paste(img, mask=img.split()[-1])
                        img = bg
                    elif img.mode != 'RGB':
                        img = img.convert('RGB')
                        
                    buf = io.BytesIO()
                    img.save(buf, format='JPEG', quality=85)
                    final_data = buf.getvalue()
                except Exception as e:
                    print(f"  ⚠ Conversion failed for {char_id}: {e}")
                
                # Save
                t_conn = sqlite3.connect(DB_FILE)
                t_cur = t_conn.cursor()
                t_cur.execute("UPDATE characters SET image_blob = ? WHERE id = ?", (final_data, char_id))
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
            
    print(f"⚡ Background sync complete. Cached: {count}, Errors: {errors}")


@app.route('/admin/sync-images', methods=['POST'])
def trigger_image_sync():
    """Trigger background image download for offline support"""
    thread = threading.Thread(target=cache_all_images_background)
    thread.daemon = True
    thread.start()
    return jsonify({"message": "Background image sync started", "status": "started"})


@app.route('/admin/sync-mysql', methods=['POST'])
def sync_mysql():
    """Trigger synchronization to MySQL"""
    try:
        # SQLite connection
        sqlite_conn = sqlite3.connect(DB_FILE)
        sqlite_conn.row_factory = sqlite3.Row
        sqlite_cursor = sqlite_conn.cursor()
        
        # MySQL connection
        try:
            mysql_conn = mysql.connector.connect(**MYSQL_CONFIG)
        except Error as e:
            return jsonify({"error": f"MySQL Connection failed: {str(e)}"}), 502
            
        mysql_cursor = mysql_conn.cursor()
        
        # 1. Create Tables
        mysql_cursor.execute("""
        CREATE TABLE IF NOT EXISTS characters (
            id VARCHAR(255) PRIMARY KEY,
            name VARCHAR(255),
            house VARCHAR(255),
            image TEXT,
            died TEXT,
            born TEXT,
            patronus TEXT,
            gender VARCHAR(50),
            species VARCHAR(100),
            blood_status VARCHAR(100),
            role TEXT,
            wiki TEXT,
            slug TEXT,
            image_blob MEDIUMBLOB,
            alias_names TEXT,
            animagus TEXT,
            boggart TEXT,
            eye_color TEXT,
            family_member TEXT,
            hair_color TEXT,
            height TEXT,
            jobs TEXT,
            nationality TEXT,
            romances TEXT,
            skin_color TEXT,
            titles TEXT,
            wand TEXT,
            weight TEXT,
            last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        )
        """)
        
        mysql_cursor.execute("""
        CREATE TABLE IF NOT EXISTS favorites (
            character_id VARCHAR(255) PRIMARY KEY,
            last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        )
        """)
        
        # 2. Sync Characters
        # Clear existing data to ensure we match the local filter exactly
        mysql_cursor.execute("DELETE FROM characters")
        
        sqlite_cursor.execute("SELECT * FROM characters")
        characters = sqlite_cursor.fetchall()
        
        count = 0
        for char in characters:
            char_dict = dict(char)
            # Remove keys that might not exist in target if schema differs, but here we aligned them.
            # Convert dict to keys/values for SQL
            columns = ', '.join(char_dict.keys())
            placeholders = ', '.join(['%s'] * len(char_dict))
            update_clause = ', '.join([f"{k}=new.{k}" for k in char_dict.keys() if k != 'id'])
            
            sql = f"INSERT INTO characters ({columns}) VALUES ({placeholders}) AS new ON DUPLICATE KEY UPDATE {update_clause}"
            mysql_cursor.execute(sql, list(char_dict.values()))
            count += 1
            
        # 3. Sync Favorites
        # Truncate favorites in MySQL first to mirror deletions
        mysql_cursor.execute("DELETE FROM favorites")
        
        sqlite_cursor.execute("SELECT character_id FROM favorites")
        favorites = sqlite_cursor.fetchall()
        
        fav_count = 0
        for fav in favorites:
            # fav is Row object, access by index or key
            c_id = fav['character_id']
            
            mysql_cursor.execute("""
                INSERT IGNORE INTO favorites (character_id, is_favorite) 
                VALUES (%s, 1)
            """, (c_id,))
            fav_count += 1
            
        mysql_conn.commit()
        mysql_conn.close()
        sqlite_conn.close()
        
        return jsonify({
            "success": True, 
            "message": f"Synced {count} characters and {fav_count} favorites to MySQL",
            "mysql_status": "connected"
        })
        
    except Exception as e:
        print(f"Sync error: {e}")
        return jsonify({"error": str(e)}), 500



# Legacy endpoint for backwards compatibility
@app.route('/personajes', methods=['GET'])
def get_personajes():
    """Legacy endpoint - redirects to /characters"""
    return get_characters()


if __name__ == '__main__':
    print("Initializing database...")
    init_db()
    print("Starting Harry Potter Yearbook Backend...")
    print("Backend will be available at: http://localhost:8000")
    print("Endpoints:")
    print("  - GET  /characters              : Get all filtered characters")
    print("  - GET  /characters/<id>/image   : Get character image (lazy cache)")
    print("  - POST /characters/<id>/favorite : Toggle favorite status")
    print("  - POST /admin/sync-images       : Trigger full image cache (OFFLINE MODE)")
    print("  - GET  /health                  : Health check")
    app.run(debug=True, port=8000, host='0.0.0.0')
