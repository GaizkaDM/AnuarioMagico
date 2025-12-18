"""
Rutas de administración del sistema.
Gestiona sincronizaciones, importaciones y tareas en segundo plano.

Autor: Gaizka
"""
from flask import Blueprint, jsonify, request
import threading
import os
from backend.services.ImageService import ImageService
from backend.services.sync_mysql import sync_sqlite_to_mysql, sync_mysql_to_sqlite
from backend.services.PersonajeService import PersonajeService
from backend.config import DB_FILE
from backend.logging_config import logger_backend

admin_bp = Blueprint('admin', __name__)
personaje_service = PersonajeService(DB_FILE)

@admin_bp.route('/admin/sync-images', methods=['POST'])
def trigger_image_sync():
    thread = threading.Thread(target=ImageService.cache_all_images_background)
    thread.daemon = True
    thread.start()
    return jsonify({"message": "Background image sync started", "status": "started"})

@admin_bp.route('/admin/sync-images/status', methods=['GET'])
def get_image_sync_status():
    return jsonify(ImageService.sync_status)

@admin_bp.route('/admin/sync-mysql', methods=['POST'])
def sync_mysql_push():
    try:
        sync_sqlite_to_mysql()
        return jsonify({"success": True, "message": "Push to MySQL completed sucessfully"})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@admin_bp.route('/admin/sync-pull', methods=['POST'])
def sync_mysql_pull():
    try:
        sync_mysql_to_sqlite()
        
        # Regenerar archivos de exportación (CSV, XML, BIN) para que coincidan con los nuevos datos
        try:
            personaje_service.export_service.exportar_todo()
            logger_backend.info("Export files regenerated after Sync Pull.")
        except Exception as ex:
            logger_backend.error(f"Error regenerating export files after sync: {ex}")
        
        # Auto-trigger background image sync to ensure offline availability
        thread = threading.Thread(target=ImageService.cache_all_images_background)
        thread.daemon = True
        thread.start()
        
        return jsonify({"success": True, "message": "Pull from MySQL completed. Export files updated. Image caching started in background."})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@admin_bp.route('/admin/import-csv', methods=['POST'])
def import_csv():
    """
    Endpoint masivo para importar personajes desde un archivo CSV.
    Espera un archivo en el form-data con key 'file'.
    Recibe un CSV con el modelo de personaje y los añade a la base de datos.
    Regenera los archivos CSV, XML y binario automáticamente.
    """
    try:
        if 'file' not in request.files:
            return jsonify({'error': 'No file part'}), 400
           
        file = request.files['file']
        if file.filename == '':
            return jsonify({'error': 'No selected file'}), 400
           
        if file and file.filename.endswith('.csv'):
            # Guardar temporalmente
            temp_path = 'temp_import.csv'
            # Asegurarse de guardar en el directorio actual o uno seguro
            abs_temp_path = os.path.abspath(temp_path)
            file.save(abs_temp_path)
           
            try:
                # Ejecutar importación
                stats = personaje_service.importar_personajes_desde_csv(abs_temp_path)
               
                # Limpieza
                if os.path.exists(abs_temp_path):
                    os.remove(abs_temp_path)
                   
                return jsonify({
                    'success': True,
                    'message': 'Import completed successfully',
                    'stats': stats
                })
            except Exception as e:
                # Asegurar limpieza en caso de error
                if os.path.exists(abs_temp_path):
                    os.remove(abs_temp_path)
                logger_backend.error(f"Service Error during import: {str(e)}", exc_info=True)
                raise e
        else:
            return jsonify({'error': 'Invalid file type. Must be CSV'}), 400
           
    except Exception as e:
        logger_backend.error(f"Import Error: {e}", exc_info=True)
        return jsonify({'error': str(e)}), 500
