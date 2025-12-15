from flask import Flask, jsonify, request, Response
from flask_cors import CORS
from flask_sqlalchemy import SQLAlchemy
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
from werkzeug.security import generate_password_hash, check_password_hash
import uuid
# Importar funciones de sincronización del módulo hermano
import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from sync_mysql import sync_sqlite_to_mysql, sync_mysql_to_sqlite
from PersonajeService import PersonajeService

"""
Módulo principal de la aplicación Backend (Flask).
Encargado de gestionar la API REST, la conexión con PotterDB,
la base de datos SQLite local y las funciones de autenticación/sincronización.

@author: GaizkaFrost
@version: 1.0
@date: 2025-12-14
"""

__author__ = "GaizkaFrost"
__version__ = "1.0"
__status__ = "Development"

app = Flask(__name__)
CORS(app)  # Habilitar CORS para cliente JavaFX

from config import DB_FILE, MYSQL_CONFIG, MASTER_PASSWORD, POTTERDB_API

# Configuración SQLAlchemy
app.config['SQLALCHEMY_DATABASE_URI'] = f'sqlite:///{DB_FILE}'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
db = SQLAlchemy(app)

# --- LOGGING CONFIGURATION ---
import logging
from logging.handlers import RotatingFileHandler

def setup_logging():
    log_dir = os.path.join(BASE_DIR, 'logs')
    if not os.path.exists(log_dir):
        os.makedirs(log_dir)
        
    log_file = os.path.join(log_dir, 'backend.log')
    
    # Configure root logger
    logger = logging.getLogger()
    logger.setLevel(logging.INFO)
    
    # File handler (Rotating: 1MB, 3 backups)
    file_handler = RotatingFileHandler(log_file, maxBytes=1_000_000, backupCount=3)
    file_handler.setFormatter(logging.Formatter(
        '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    ))
    logger.addHandler(file_handler)
    
    # Console handler
    console_handler = logging.StreamHandler()
    console_handler.setFormatter(logging.Formatter(
        '%(asctime)s - %(levelname)s - %(message)s'
    ))
    logger.addHandler(console_handler)
    
    logging.info("Backend Logging Initialized")

setup_logging()
# -----------------------------

# Inicializar servicio de personajes (CRUD + Exportación)
personaje_service = PersonajeService(DB_FILE)

# Almacén de sesiones simple en memoria: token -> usuario
SESSIONS = {} 
# --- MODELOS SQLALCHEMY ---

class User(db.Model):
    __tablename__ = 'users'
    username = db.Column(db.String(255), primary_key=True)
    password_hash = db.Column(db.String(255))
    created_at = db.Column(db.String(100))

class Favorite(db.Model):
    __tablename__ = 'favorites'
    character_id = db.Column(db.String(255), primary_key=True)
    is_favorite = db.Column(db.Boolean, default=False)

class Character(db.Model):
    __tablename__ = 'characters'
    id = db.Column(db.String(255), primary_key=True)
    name = db.Column(db.String(255))
    house = db.Column(db.String(255))
    image = db.Column(db.Text)
    died = db.Column(db.String(255))
    born = db.Column(db.String(255))
    patronus = db.Column(db.String(255))
    gender = db.Column(db.String(50))
    species = db.Column(db.String(100))
    blood_status = db.Column(db.String(100))
    role = db.Column(db.Text)
    wiki = db.Column(db.Text)
    slug = db.Column(db.String(255))
    image_blob = db.Column(db.LargeBinary)
    alias_names = db.Column(db.Text)
    animagus = db.Column(db.Text)
    boggart = db.Column(db.Text)
    eye_color = db.Column(db.String(100))
    family_member = db.Column(db.Text)
    hair_color = db.Column(db.String(100))
    height = db.Column(db.String(50))
    jobs = db.Column(db.Text)
    nationality = db.Column(db.String(100))
    romances = db.Column(db.Text)
    skin_color = db.Column(db.String(100))
    titles = db.Column(db.Text)
    wand = db.Column(db.Text)
    weight = db.Column(db.String(50))

def init_db():
    """
    Inicializa la base de datos SQLite para favoritos y personajes.
    Crea las tablas 'favorites', 'users' y 'characters' si no existen.
    También realiza migraciones de columnas si es necesario.
    """
    with app.app_context():
        # Crea todas las tablas definidas en los modelos si no existen
        db.create_all()
        
        # Migración manual para columnas nuevas (por si la DB ya existía antes de SQLAlchemy)
        # SQLAlchemy create_all no actualiza tablas ya existentes, así que mantenemos esto por seguridad
        try:
            conn = sqlite3.connect(DB_FILE)
            cursor = conn.cursor()
            
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
                    try:
                        cursor.execute(f"ALTER TABLE characters ADD COLUMN {col_name} {col_type}")
                    except sqlite3.OperationalError:
                        pass # Probablemente ya existe
            
            conn.commit()
            conn.close()
        except Exception as e:
            print(f"Migration warning: {e}")


def get_favorite_status(character_id):
    """
    Obtiene el estado de favorito de un personaje.

    Args:
        character_id (str): El ID del personaje.

    Returns:
        bool: True si es favorito, False en caso contrario.
    """
    try:
        fav = db.session.get(Favorite, character_id)
        return bool(fav and fav.is_favorite)
    except Exception:
        return False


def set_favorite_status(character_id, is_favorite):
    """
    Establece el estado de favorito para un personaje.

    Args:
        character_id (str): El ID del personaje.
        is_favorite (bool): True para marcar como favorito, False para desmarcar.
    """
    try:
        fav = db.session.get(Favorite, character_id)
        
        if is_favorite:
            if not fav:
                fav = Favorite(character_id=character_id, is_favorite=True)
                db.session.add(fav)
            else:
                fav.is_favorite = True
        else:
            if fav:
                db.session.delete(fav)
                
        db.session.commit()
    except Exception as e:
        db.session.rollback()
        print(f"Error setting favorite status: {e}")
        raise e


def save_characters_to_db(characters_data):
    """
    Guarda una lista de personajes en la base de datos local SQLite usando SQLAlchemy.
    Actualiza la información existente sin sobrescribir el BLOB de imagen si ya existe.

    Args:
        characters_data (list): Lista de diccionarios con la información de los personajes.
    """
    try:
        for char_data in characters_data:
            # Buscar si existe
            char = db.session.get(Character, char_data['id'])
            
            # Preparar datos (serializar listas a JSON strings)
            # Nota: Asumimos que data ya viene 'limpia' del endpoint, pero debemos convertir listas a JSON
            def ensure_json(val):
                return json.dumps(val) if isinstance(val, (list, dict)) else (val or '')

            # Si ya existe, actualizamos todo MENOS el blob (si el nuevo blob es null)
            # En la lógica actual, char_data NO trae 'image_blob' normalmente (es None).
            # Solo si lo trajera explícitamente deberíamos actualizarlo.
            
            if not char:
                char = Character(id=char_data['id'])
                db.session.add(char)
            
            # Actualizar campos
            char.name = char_data['name']
            char.house = char_data['house']
            char.image = char_data['image']
            char.died = char_data['died']
            char.born = char_data['born']
            char.patronus = char_data['patronus']
            char.gender = char_data['gender']
            char.species = char_data['species']
            char.blood_status = char_data['blood_status']
            char.role = char_data['role']
            char.wiki = char_data['wiki']
            char.slug = char_data.get('slug', '')
            
            # Serializar campos complejos
            char.alias_names = ensure_json(char_data.get('alias_names', []))
            char.animagus = char_data.get('animagus', '')
            char.boggart = char_data.get('boggart', '')
            char.eye_color = char_data.get('eye_color', '')
            char.family_member = ensure_json(char_data.get('family_member', []))
            char.hair_color = char_data.get('hair_color', '')
            char.height = char_data.get('height', '')
            char.jobs = ensure_json(char_data.get('jobs', []))
            char.nationality = char_data.get('nationality', '')
            char.romances = ensure_json(char_data.get('romances', []))
            char.skin_color = char_data.get('skin_color', '')
            char.titles = ensure_json(char_data.get('titles', []))
            char.wand = ensure_json(char_data.get('wand', []))
            char.weight = char_data.get('weight', '')
            
            # Gestión de BLOB: Si viene en data, úsalo. Si no, respeta el existente.
            if 'image_blob' in char_data and char_data['image_blob']:
                 char.image_blob = char_data['image_blob']
            
        db.session.commit()
    except Exception as e:
        db.session.rollback()
        print(f"Error saving characters with SQLAlchemy: {e}")
        raise e


def load_characters_from_db():
    """
    Carga todos los personajes desde la base de datos local.
    Realiza un LEFT JOIN con la tabla de favoritos para eficiencia.
    Convierte los campos JSON almacenados nuevamente a listas.

    Returns:
        list: Lista de diccionarios con los personajes.
    """
    try:
        # Realizamos una consulta optimizada: SELECT characters.*, favorites.is_favorite FROM characters LEFT JOIN favorites
        # db.session.query devuelve tuplas (Character, Favorite)
        # Outerjoin asegura que traemos el personaje aunque no tenga entrada en favoritos
        results = db.session.query(Character, Favorite.is_favorite).\
            outerjoin(Favorite, Character.id == Favorite.character_id).all()
        
        characters = []
        for char, is_fav in results:
            char_dict = {c.name: getattr(char, c.name) for c in char.__table__.columns}
            
            # Quitar el blob del listado general
            if 'image_blob' in char_dict:
                del char_dict['image_blob']
            
            # Modificar URL de imagen
            char_dict['image'] = f"http://localhost:8000/characters/{char.id}/image"
            
            # Parsear campos JSON
            json_fields = ['alias_names', 'family_member', 'jobs', 'romances', 'titles', 'wand']
            for field in json_fields:
                val = char_dict.get(field)
                if val:
                    try:
                        char_dict[field] = json.loads(val)
                    except (json.JSONDecodeError, TypeError):
                        char_dict[field] = []
                else:
                    char_dict[field] = []

            # Añadir estado de favorito (True si is_fav es 1/True, False si es None o 0)
            char_dict['is_favorite'] = bool(is_fav)
            
            characters.append(char_dict)
            
        return characters
    except Exception as e:
        print(f"Error loading characters: {e}")
        return []



# --- AUTHENTICATION ENDPOINTS ---

@app.route('/auth/register', methods=['POST'])
def register():
    try:
        data = request.get_json()
        username = data.get('username')
        password = data.get('password')
        master_password = data.get('master_password')
        
        if not username or not password or not master_password:
            return jsonify({'error': 'Missing fields'}), 400
            
        if master_password != MASTER_PASSWORD:
            return jsonify({'error': 'Invalid Master Password'}), 403
            
        conn = sqlite3.connect(DB_FILE)
        cursor = conn.cursor()
        
        # Comprobar si el usuario existe
        cursor.execute('SELECT 1 FROM users WHERE username = ?', (username,))
        if cursor.fetchone():
            conn.close()
            return jsonify({'error': 'User already exists'}), 409
            
        # Crear usuario
        pw_hash = generate_password_hash(password)
        created_at = datetime.now().isoformat()
        
        cursor.execute('INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)', 
                       (username, pw_hash, created_at))
        conn.commit()
        conn.close()
        
        return jsonify({'success': True, 'message': 'User registered successfully'})
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/auth/login', methods=['POST'])
def login():
    try:
        data = request.get_json()
        username = data.get('username')
        password = data.get('password')
        
        if not username or not password:
            return jsonify({'error': 'Missing fields'}), 400
            
        conn = sqlite3.connect(DB_FILE)
        cursor = conn.cursor()
        
        cursor.execute('SELECT password_hash FROM users WHERE username = ?', (username,))
        row = cursor.fetchone()
        conn.close()
        
        if not row or not check_password_hash(row[0], password):
            return jsonify({'error': 'Invalid credentials'}), 401
            
        # Generar token simple
        token = str(uuid.uuid4())
        SESSIONS[token] = username
        
        return jsonify({
            'success': True, 
            'token': token,
            'username': username
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/auth/logout', methods=['POST'])
def logout():
    token = request.headers.get('Authorization')
    if token and token in SESSIONS:
        del SESSIONS[token]
    return jsonify({'success': True})


def generate_id(slug):
    """
    Genera un ID consistente basado en el slug de PotterDB.
    
    Args:
        slug (str): El slug del personaje.
        
    Returns:
        str: El ID generado o el propio slug.
    """
    if slug:
        return slug
    # Fallback: generate from hash if no slug
    return hashlib.md5(str(slug).encode()).hexdigest()[:16]


@app.route('/characters', methods=['GET'])
def get_characters():
    """
    Endpoint para obtener personajes.
    
    Flujo:
    1. Intenta cargar desde la DB local SQLite.
    2. Si está vacía, descarga desde la API de PotterDB (paginación).
    3. Filtra personajes (sin imagen, palabras prohibidas).
    4. Guarda en DB local.
    5. Devuelve la lista filtrada.
    """
    try:
        # 1. Intentar cargar de base de datos local
        local_characters = load_characters_from_db()
        
        if local_characters:
            print(f"✓ Returning {len(local_characters)} characters from local SQLite DB")
            
            # Aplicar filtrado si se solicita
            if request.args.get('filter') == 'favorites':
                local_characters = [c for c in local_characters if c['is_favorite']]
                
            return jsonify(local_characters)
        
        # 2. Si no hay datos, descargar de PotterDB API
        print("⟳ Local DB empty. Fetching fresh data from PotterDB API...")
        all_characters = []
        page = 1
        
        # Obtener todas las páginas de la API
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
            
            # Comprobar si hay más páginas
            meta = data.get('meta', {})
            pagination = meta.get('pagination', {})
            if page >= pagination.get('last', 1):
                break
            
            page += 1
        
        print(f"  Fetched {len(all_characters)} total characters from API")
        
        # Filtrar y transformar datos
        filtered_characters = []
        exclude_keywords = ['unidentified', 'unknown', 'student', 'girl', 'boy', 'man', 'woman', 'baby', 'child', 'spectator', 'team', 'gang', 'group', 'troll', 'portrait', 'house-elf', 'ghost', 'ghosts', 'champion', 'mentor', 'creature', 'abraxan', 'actor', 'announcer', 'cat', 'killer', 'enemy', 'antipodean', 'shopkeeper', 'ashwinder', 'reserve', 'augurey', 'aunt', 'mascot', 'grandmother', 'beggar', 'bespectacled', 'corridor', 'boa constrictor', 'ministry', 'hagrid\'s', 'enthusiast', 'fiancee', 'fianceé', 'friends', 'chief', 'victim', 'guard', 'guards', 'witch', 'wizard', 'waiter', 'deer', 'cousin', 'cousins', 'sister', 'sisters', 'brother', 'brothers', 'father', 'grandfather', 'parents', 'great-grandmother']
        
        for character in all_characters:
            attributes = character.get('attributes', {})
            name = attributes.get('name', '')
            image_url = attributes.get('image')
            
            name = attributes.get('name', '')
            image_url = attributes.get('image')
            
            # Filtro: debe tener imagen
            if not image_url:
                continue
            
            # Filtro: excluir palabras clave en el nombre
            if any(keyword.lower() in name.lower() for keyword in exclude_keywords):
                continue
            
            # Generar ID desde el slug
            slug = attributes.get('slug', '')
            character_id = generate_id(slug)
            
            # Construir objeto de respuesta completo
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
        
        # 3b. Generar archivos de exportación (CSV, XML, Binario)
        try:
            print("⟳ Generating export files from fresh data...")
            personaje_service.export_service.exportar_todo()
        except Exception as ex_error:
            print(f"⚠ Export warning: {ex_error}")
        
        # 4. Recargar desde DB para tener las URLs locales correctas
        return get_characters()
    
    except Exception as e:
        print(f"✗ Error: {str(e)}")
        return jsonify({"error": str(e)}), 500


@app.route('/characters/<character_id>/image', methods=['GET'])
def get_character_image(character_id):
    """
    Obtiene la imagen de un personaje.
    Sirve desde el campo BLOB si existe.
    Si no, descarga la imagen de la URL original, la convierte a JPEG,
    la guarda en el BLOB y la sirve.
    
    Args:
        character_id (str): ID del personaje.
        
    Returns:
        Response: Imagen en formato JPEG o JSON de error.
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
            
            # Convertir RGBA a RGB si es necesario (ej. para PNG/WebP con transparencia)
            try:
                img = Image.open(io.BytesIO(image_raw_data))
                
                # Convertir RGBA a RGB si es necesario (ej. para PNG/WebP con transparencia)
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
    Alterna o establece el estado de favorito de un personaje.
    
    Args:
        character_id (str): ID del personaje.
        
    Body JSON (opcional):
        { "is_favorite": boolean }
        
    Returns:
        JSON: Nuevo estado del personaje.
    """
    try:
        # Comprobar estado actual
        current_status = get_favorite_status(character_id)
        
        # Determinar nuevo estado
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
    """
    Endpoint de comprobación de estado (Health Check).
    Devuelve el estado de la DB y contadores de caché.
    """
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
    """
    Tarea en segundo plano para descargar y cachear todas las imágenes
    que faltan en la base de datos local.
    """
    print("⚡ Starting background image sync...")
    conn = sqlite3.connect(DB_FILE)
    cursor = conn.cursor()
    
    # Obtener todos los personajes con URL de imagen pero SIN blob
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
            # Descargar
            resp = requests.get(original_url, timeout=10)
            if resp.status_code == 200:
                raw_data = resp.content
                final_data = raw_data
                
                # Convertir
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
                
                # Guardar
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
    """
    Endpoint administrativo para iniciar la descarga de imágenes en segundo plano.
    Útil para soporte offline.
    """
    thread = threading.Thread(target=cache_all_images_background)
    thread.daemon = True
    thread.start()
    return jsonify({"message": "Background image sync started", "status": "started"})


@app.route('/admin/sync-mysql', methods=['POST'])
def sync_mysql_push():
    """
    Sincroniza los datos locales de SQLite a una base de datos MySQL externa (PUSH).
    Sincroniza: Personajes, Favoritos y Usuarios.
    """
    try:
        # Ejecutar la lógica encapsulada en sync_mysql.py
        # Nota: ejecutamos directamente la función importada para reutilizar lógica
        sync_sqlite_to_mysql()
        return jsonify({"success": True, "message": "Push to MySQL completed sucessfully"})
    except Exception as e:
        print(f"Sync Push Error: {e}")
        return jsonify({"error": str(e)}), 500

@app.route('/admin/sync-pull', methods=['POST'])
def sync_mysql_pull():
    """
    Descarga datos de MySQL a la base de datos local SQLite (PULL).
    Actualiza Usuarios, Favoritos y Personajes.
    """
    try:
        sync_mysql_to_sqlite()
        return jsonify({"success": True, "message": "Pull from MySQL completed successfully"})
    except Exception as e:
        print(f"Sync Pull Error: {e}")
        return jsonify({"error": str(e)}), 500


# --- CRUD ENDPOINTS (Integración con PersonajeService) ---

@app.route('/characters', methods=['POST'])
def add_character():
    """
    Añade un nuevo personaje manualmente.
    Dispara automáticamente la regeneración de archivos CSV/XML/Bin.
    Maneja campos JSON y serialización.
    """
    try:
        data = request.get_json()
        
        # Validación básica
        if not data.get('name'):
            return jsonify({'error': 'Name is required'}), 400
            
        # Generar ID si no viene
        if not data.get('id'):
            data['id'] = generate_id(data.get('slug') or data['name'])
            
        # Usar el servicio para añadir (esto actualiza SQLite y crea exports)
        success = personaje_service.añadir_personaje(data)
        
        if success:
            return jsonify({'success': True, 'message': 'Character added and files exported', 'id': data['id']}), 201
        else:
            return jsonify({'error': 'Failed to add character (check logs)'}), 500
            
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/characters/<character_id>', methods=['PUT', 'PATCH'])
def edit_character(character_id):
    """
    Edita un personaje existente.
    Dispara automáticamente la regeneración de archivos CSV/XML/Bin.
    """
    try:
        data = request.get_json()
        
        # Usar el servicio para editar
        success = personaje_service.editar_personaje(character_id, data)
        
        if success:
            return jsonify({'success': True, 'message': 'Character updated and files exported'}), 200
        else:
            return jsonify({'error': 'Failed to update character or ID not found'}), 404
            
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/characters/<character_id>', methods=['DELETE'])
def delete_character(character_id):
    """
    Elimina un personaje.
    Dispara automáticamente la regeneración de archivos CSV/XML/Bin.
    """
    try:
        # Usar el servicio para eliminar
        success = personaje_service.eliminar_personaje(character_id)
        
        if success:
            return jsonify({'success': True, 'message': 'Character deleted and files exported'}), 200
        else:
            # Podría ser que no existe, pero PersonajeService devuelve False en error genérico.
            # Asumimos 404/500 según contexto, aquí 404 es razonable para "no hecho".
            return jsonify({'error': 'Failed to delete character or ID not found'}), 404
            
    except Exception as e:
        return jsonify({'error': str(e)}), 500



# Endpoint legado para retrocompatibilidad
@app.route('/personajes', methods=['GET'])
def get_personajes():
    """Endpoint legado - redirige a /characters"""
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
    print("  - POST /admin/sync-mysql        : Push local to Cloud")
    print("  - POST /admin/sync-pull         : Pull Cloud to local")
    print("  - GET  /health                  : Health check")
    app.run(debug=True, port=8000, host='0.0.0.0')
