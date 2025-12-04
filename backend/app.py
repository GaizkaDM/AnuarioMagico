from flask import Flask, jsonify, request
from flask_cors import CORS
import requests
import sqlite3
import hashlib
from datetime import datetime, timedelta

app = Flask(__name__)
CORS(app)  # Enable CORS for JavaFX client

# PotterDB API base URL
POTTERDB_API = "https://api.potterdb.com/v1/characters"

# Database file
DB_FILE = 'favorites.db'

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
            slug TEXT
        )
    ''')
    
    conn.commit()
    conn.close()


def get_favorite_status(character_id):
    """Get favorite status for a character"""
    conn = sqlite3.connect(DB_FILE)
    cursor = conn.cursor()
    cursor.execute('SELECT is_favorite FROM favorites WHERE character_id = ?', (character_id,))
    result = cursor.fetchone()
    conn.close()
    return result[0] == 1 if result else False


def set_favorite_status(character_id, is_favorite):
    """Set favorite status for a character"""
    conn = sqlite3.connect(DB_FILE)
    cursor = conn.cursor()
    cursor.execute('''
        INSERT OR REPLACE INTO favorites (character_id, is_favorite)
        VALUES (?, ?)
    ''', (character_id, 1 if is_favorite else 0))
    conn.commit()
    conn.close()


def save_characters_to_db(characters):
    """Save a list of characters to the database"""
    conn = sqlite3.connect(DB_FILE)
    cursor = conn.cursor()
    
    for char in characters:
        cursor.execute('''
            INSERT OR REPLACE INTO characters (
                id, name, house, image, died, born, patronus, 
                gender, species, blood_status, role, wiki, slug
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (
            char['id'], char['name'], char['house'], char['image'], 
            char['died'], char['born'], char['patronus'],
            char['gender'], char['species'], char['blood_status'], 
            char['role'], char['wiki'], char.get('slug', '')
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
        exclude_keywords = ['Unidentified', 'Unknown', 'Student']
        
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
                'slug': slug
            }
            filtered_characters.append(char_obj)
        
        # 3. Guardar en base de datos local
        save_characters_to_db(filtered_characters)
        print(f"✓ Saved {len(filtered_characters)} characters to SQLite DB")
        
        # 4. Añadir estado de favoritos para la respuesta
        final_characters = []
        filter_favorites = request.args.get('filter') == 'favorites'
        
        for char in filtered_characters:
            is_fav = get_favorite_status(char['id'])
            char['is_favorite'] = is_fav
            
            if filter_favorites:
                if is_fav:
                    final_characters.append(char)
            else:
                final_characters.append(char)
            
        return jsonify(final_characters)
    
    except Exception as e:
        print(f"✗ Error: {str(e)}")
        return jsonify({"error": str(e)}), 500


@app.route('/characters/<character_id>/favorite', methods=['POST'])
def toggle_favorite(character_id):
    """
    Toggle favorite status for a character
    
    Expected JSON body: {"is_favorite": true/false}
    """
    try:
        data = request.get_json()
        is_favorite = data.get('is_favorite', False)
        
        set_favorite_status(character_id, is_favorite)
        
        return jsonify({
            'success': True,
            'character_id': character_id,
            'is_favorite': is_favorite
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
        conn.close()
        db_status = "connected"
    except Exception as e:
        db_status = f"error: {str(e)}"
        count = 0

    return jsonify({
        "status": "ok",
        "message": "Backend is running on port 8000",
        "db_status": db_status,
        "cached_characters": count
    })


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
    print("  - POST /characters/<id>/favorite : Toggle favorite status")
    print("  - GET  /health                  : Health check")
    print("  - GET  /personajes              : Legacy endpoint (compatibility)")
    app.run(debug=True, port=8000, host='0.0.0.0')
