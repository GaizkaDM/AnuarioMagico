from flask import Blueprint, request, jsonify, Response
import requests
import hashlib
from backend.models.Favorite import Favorite
from backend.models.Character import Character
from backend.extensions import db
from backend.services.PersonajeService import PersonajeService
from backend.services.ImageService import ImageService
from backend.config import POTTERDB_API, DB_FILE
from backend.logging_config import logger_backend

characters_bp = Blueprint('characters', __name__)
personaje_service = PersonajeService(DB_FILE)

import re

def slugify(text):
    if not text:
        return "character"
    # Convert to lowercase and replace non-alphanumeric with hyphens
    text = text.lower()
    text = re.sub(r'[^a-z0-9]+', '-', text)
    text = text.strip('-')
    return text if text else "character"

def generate_id(name, slug=None):
    """Generates a unique ID (slug) by checking the database and appending suffixes if needed."""
    # Use slug as base if provided (API cases), otherwise slugify the name (User cases)
    base_slug = slug if slug else slugify(name)
    
    candidate = base_slug
    counter = 1
    
    # Check for existence using SQLAlchemy model
    while Character.query.get(candidate):
        candidate = f"{base_slug}{counter}"
        counter += 1
        
    return candidate

@characters_bp.route('/characters', methods=['GET'])
def get_characters():
    try:
        local_characters = personaje_service.dao.obtener_todos_personajes()
        
        if local_characters:
            logger_backend.info(f"✓ Returning {len(local_characters)} characters from local SQLite DB")
            if request.args.get('filter') == 'favorites':
                # Fetch favorites ids
                favs = Favorite.query.filter_by(is_favorite=True).all()
                fav_ids = {f.character_id for f in favs}
                local_characters = [c for c in local_characters if c['id'] in fav_ids]
                
            # Add is_favorite flag (DaoSQLite returns dicts, need to enrich)
            # This is a bit inefficient, but maintains logic. Better would be join in DAO.
            # In refactoring, we use SQLAlchemy model for simpler querying?
            # Keeping consistent with original logic which used custom DAO + manual loading.
            # Actually, app.py had `load_characters_from_db` using SQLAlchemy query with join.
            # Let's reproduce THAT logic here using SQLAlchemy for better consistency with Models.
            
            # REPLACING DAO CALL WITH SQLALCHEMY JOIN LOGIC FROM APP.PY
            results = db.session.query(Character, Favorite.is_favorite).\
                outerjoin(Favorite, Character.id == Favorite.character_id).all()
            
            characters = []
            for char, is_fav in results:
                char_dict = {c.name: getattr(char, c.name) for c in char.__table__.columns}
                if 'image_blob' in char_dict: del char_dict['image_blob']
                char_dict['image'] = f"http://localhost:8000/characters/{char.id}/image"
                
                # Parse JSON fields
                import json
                json_fields = ['alias_names', 'family_member', 'jobs', 'romances', 'titles', 'wand']
                for field in json_fields:
                    val = char_dict.get(field)
                    try:
                        char_dict[field] = json.loads(val) if val else []
                    except:
                        char_dict[field] = []
                
                char_dict['is_favorite'] = bool(is_fav)
                characters.append(char_dict)
                
            if request.args.get('filter') == 'favorites':
                characters = [c for c in characters if c['is_favorite']]
                
            return jsonify(characters)
        
        # If empty, fetch from API (Logic copied from app.py)
        logger_backend.info("⟳ Local DB empty. Fetching fresh data from PotterDB API...")
        all_characters = []
        page = 1
        while True:
            logger_backend.debug(f"  Fetching page {page}...")
            response = requests.get(f"{POTTERDB_API}?page[number]={page}")
            if response.status_code != 200: return jsonify({"error": "Failed to fetch"}), 500
            data = response.json()
            chars = data.get('data', [])
            if not chars: break
            all_characters.extend(chars)
            meta = data.get('meta', {})
            if page >= meta.get('pagination', {}).get('last', 1): break
            page += 1
            
        # Filter and save
        exclude_keywords = ['unidentified', 'unknown', 'student', 'girl', 'boy', 'man', 'woman', 'baby', 'child', 'spectator', 'team', 'gang', 'group', 'troll', 'portrait', 'house-elf', 'ghost', 'ghosts', 'champion', 'mentor', 'creature', 'abraxan', 'actor', 'announcer', 'cat', 'killer', 'enemy', 'antipodean', 'shopkeeper', 'ashwinder', 'reserve', 'augurey', 'aunt', 'mascot', 'grandmother', 'beggar', 'bespectacled', 'corridor', 'boa constrictor', 'ministry', 'hagrid\'s', 'enthusiast', 'fiancee', 'fianceé', 'friends', 'chief', 'victim', 'guard', 'guards', 'witch', 'wizard', 'waiter', 'deer', 'cousin', 'cousins', 'sister', 'sisters', 'brother', 'brothers', 'father', 'grandfather', 'parents', 'great-grandmother']
        filtered_characters = []
        
        for char in all_characters:
            attr = char.get('attributes', {})
            name = attr.get('name', '')
            img = attr.get('image')
            if not img: continue
            if any(k.lower() in name.lower() for k in exclude_keywords): continue
            
            slug = attr.get('slug', '')
            char_id = generate_id(name, slug)
            
            char_obj = {
                'id': char_id, 'name': name, 'house': attr.get('house') or '', 'image': img,
                'died': attr.get('died') or '', 'born': attr.get('born') or '', 'patronus': attr.get('patronus') or '',
                'gender': attr.get('gender') or '', 'species': attr.get('species') or '',
                'blood_status': attr.get('blood_status') or '', 'role': attr.get('role') or '',
                'wiki': attr.get('wiki') or '', 'slug': slug,
                'alias_names': attr.get('alias_names', []), 'animagus': attr.get('animagus') or '',
                'boggart': attr.get('boggart') or '', 'eye_color': attr.get('eye_color') or '',
                'family_member': attr.get('family_members', []), 'hair_color': attr.get('hair_color') or '',
                'height': attr.get('height') or '', 'jobs': attr.get('jobs', []),
                'nationality': attr.get('nationality') or '', 'romances': attr.get('romances', []),
                'skin_color': attr.get('skin_color') or '', 'titles': attr.get('titles', []),
                'wand': attr.get('wands', []), 'weight': attr.get('weight') or ''
            }
            filtered_characters.append(char_obj)
            
        # Save via Service (Uses DAO) -> Actually app.py used `save_characters_to_db` (SQLAlchemy).
        # We should use `personaje_service.add_character` ? No, that does single insert + exports.
        # We need bulk insert. app.py had `save_characters_to_db`.
        # For Refactoring, I should reimplement `save_characters_to_db` somewhere.
        # Ideally in PersonajeService or a generic utils, but PersonajeService seems tied to legacy DAO.
        # I will inline simplified SQLAlchemy save logic here for now to avoid breaking.
        
        import json
        for c in filtered_characters:
            existing = Character.query.get(c['id'])
            if not existing:
                existing = Character(id=c['id'])
                db.session.add(existing)
            
            for k, v in c.items():
                if k == 'id': continue
                if isinstance(v, (dict, list)):
                    setattr(existing, k if k != 'family_member' else 'family_member', json.dumps(v)) # Mapping fix (key in dict is family_member, model is family_member)
                elif k == 'wand': # Key mapping check
                     setattr(existing, 'wand', json.dumps(v))
                else:
                    if hasattr(existing, k):
                        setattr(existing, k, v)
        db.session.commit()
        
        # Trigger exports
        personaje_service.export_service.exportar_todo()
        
        # Recursively call to get formatted list -> CAUTION: This creates a new request context/session potentially.
        # Better to return the list we just processed, but formatted correctly.
        # Re-querying the DB is safer to ensure correct format.
        local_characters = personaje_service.dao.obtener_todos_personajes()
        
        # Manually enrich (Copy-paste logic from above to avoid recursion, or refactor into helper)
        # Using a helper would be cleaner, but for minimal diff, let's re-query using the main logic flow?
        # Recursion here is risky if the transaction wasn't committed fully or connection pool is tight.
        # But db.session.commit() was called.
        
        # Let's return the recently saved list directly formatted.
        results = db.session.query(Character, Favorite.is_favorite).\
                outerjoin(Favorite, Character.id == Favorite.character_id).all()
            
        characters = []
        for char, is_fav in results:
            char_dict = {c.name: getattr(char, c.name) for c in char.__table__.columns}
            if 'image_blob' in char_dict: del char_dict['image_blob']
            char_dict['image'] = f"http://localhost:8000/characters/{char.id}/image"
            
            import json
            json_fields = ['alias_names', 'family_member', 'jobs', 'romances', 'titles', 'wand']
            for field in json_fields:
                val = char_dict.get(field)
                try:
                    char_dict[field] = json.loads(val) if val else []
                except:
                    char_dict[field] = []
            
            char_dict['is_favorite'] = bool(is_fav)
            characters.append(char_dict)
            
        return jsonify(characters)
        
    except Exception as e:
        logger_backend.error(f"Error in get_characters: {str(e)}", exc_info=True)
        return jsonify({"error": str(e)}), 500

@characters_bp.route('/characters', methods=['POST'])
def add_character():
    try:
        data = request.get_json()
        if not data: return jsonify({"error": "No data"}), 400
        
        # Generate UNIQUE ID if missing
        if 'id' not in data:
            data['id'] = generate_id(data.get('name', 'character'), data.get('slug'))
            # Ensure slug matches the final ID if it was missing or generated
            if 'slug' not in data or not data['slug']:
                data['slug'] = data['id']
            
        success = personaje_service.añadir_personaje(data)
        if success:
            return jsonify({"success": True, "id": data['id']}), 201
        return jsonify({"error": "Failed to add"}), 500
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@characters_bp.route('/characters/<character_id>/upload-image', methods=['POST'])
def upload_character_image(character_id):
    try:
        if 'image' not in request.files:
            return jsonify({"error": "No image part"}), 400
        file = request.files['image']
        if file.filename == '':
            return jsonify({"error": "No selected file"}), 400
            
        raw_data = file.read()
        processed_data = ImageService.process_image_data(raw_data)
        
        if not processed_data:
            return jsonify({"error": "Invalid image data"}), 400
            
        success = personaje_service.editar_personaje(character_id, {"image_blob": processed_data})
        if success:
            logger_backend.info(f"✓ Image blob updated for character {character_id}")
            return jsonify({"success": True})
        return jsonify({"error": "Failed to update character image"}), 500
    except Exception as e:
        logger_backend.error(f"Upload error: {e}")
        return jsonify({"error": str(e)}), 500

@characters_bp.route('/characters/<character_id>/image', methods=['GET'])
def get_character_image(character_id):
    try:
        row = ImageService.get_image_from_db(character_id)
        if not row: return jsonify({'error': 'Character not found'}), 404
        
        blob_data, original_url = row
        if blob_data: return Response(blob_data, mimetype='image/jpeg')
        if not original_url: return jsonify({'error': 'No image URL'}), 404
        
        final_data = ImageService.download_and_process_image(original_url)
        if final_data:
            ImageService.cache_image(character_id, final_data)
            return Response(final_data, mimetype='image/jpeg')
        return jsonify({'error': 'Failed to download'}), 502
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@characters_bp.route('/characters/<character_id>/favorite', methods=['POST'])
def toggle_favorite(character_id):
    try:
        fav = Favorite.query.get(character_id)
        data = request.get_json(silent=True)
        
        new_status = True
        if data and 'is_favorite' in data:
            new_status = data['is_favorite']
        elif fav:
            new_status = not fav.is_favorite
            
        if new_status:
            if not fav:
                fav = Favorite(character_id=character_id, is_favorite=True)
                db.session.add(fav)
            else:
                fav.is_favorite = True
        else:
            if fav:
                db.session.delete(fav)
        
        db.session.commit()
        return jsonify({'success': True, 'character_id': character_id, 'is_favorite': new_status})
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@characters_bp.route('/characters/<character_id>', methods=['PUT', 'DELETE'])
def manage_character(character_id):
    if request.method == 'PUT':
        # EDIT
        try:
            data = request.get_json()
            if not data: return jsonify({"error": "No data"}), 400
            
            success = personaje_service.editar_personaje(character_id, data)
            if success:
                return jsonify({"success": True})
            return jsonify({"error": "Failed to update"}), 500
        except Exception as e:
            logger_backend.error(f"Error updating character {character_id}: {str(e)}", exc_info=True)
            return jsonify({"error": str(e)}), 500

    elif request.method == 'DELETE':
        # DELETE
        try:
            success = personaje_service.eliminar_personaje(character_id)
            if success:
                return jsonify({"success": True})
            return jsonify({"error": "Failed to delete"}), 500
        except Exception as e:
            logger_backend.error(f"Error deleting character {character_id}: {str(e)}", exc_info=True)
            return jsonify({"error": str(e)}), 500
