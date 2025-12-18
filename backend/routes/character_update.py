from flask import Blueprint, request, jsonify
from backend.services.PersonajeService import PersonajeService

character_update_bp = Blueprint('character_update', __name__)
service = PersonajeService()

@character_update_bp.route('/<character_id>', methods=['PUT', 'PATCH'])
def update_character(character_id):
    """
    Actualiza la información de un personaje.
    """
    try:
        data = request.json
        if not data:
            return jsonify({'error': 'No data provided'}), 400

        # Validar campos esenciales si fuera necesario, o dejar que el servicio maneje
        
        success = service.editar_personaje(character_id, data)
        
        if success:
            # Obtener el personaje actualizado para devolverlo
            updated_character = service.obtener_personaje(character_id)
            return jsonify({
                'message': 'Character updated successfully',
                'character': updated_character
            }), 200
        else:
            return jsonify({'error': 'Failed to update character or character not found'}), 404

    except Exception as e:
        return jsonify({'error': str(e)}), 500

@character_update_bp.route('/', methods=['POST'])
def add_character():
    """
    Añade un nuevo personaje.
    """
    try:
        data = request.json
        if not data:
            return jsonify({'error': 'No data provided'}), 400
            
        # Validación mínima
        if 'name' not in data:
             return jsonify({'error': 'Name is required'}), 400
             
        # Generar UUID si no existe
        if 'id' not in data:
            import uuid
            data['id'] = str(uuid.uuid4())
        
        success = service.añadir_personaje(data)
        
        if success:
             return jsonify({'message': 'Character added successfully', 'id': data['id']}), 201
        else:
             return jsonify({'error': 'Failed to add character'}), 500
             
    except Exception as e:
        return jsonify({'error': str(e)}), 500
