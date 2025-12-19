"""
Punto de entrada principal de la aplicación Flask.
Configura la aplicación, registra blueprints y gestiona el contexto global.

Autores: Xiker, Gaizka
"""
from flask import Flask, jsonify
from flask_cors import CORS
import os
import sys
import sqlite3
import logging
from logging.handlers import RotatingFileHandler

# Add project root to sys.path
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

# Import Extensions and Config
from backend.config import DB_FILE
from backend.extensions import db
from backend.logging_config import logger_backend

# Import Blueprints
from backend.routes.auth import auth_bp
from backend.routes.characters import characters_bp
from backend.routes.admin import admin_bp
from backend.routes.character_update import character_update_bp
from backend.models.User import User
from backend.models.Favorite import Favorite
from backend.models.Character import Character

def create_app():
    app = Flask(__name__)
    CORS(app)
    
    # Config
    from backend.config import MYSQL_CONFIG, DB_FILE
    
    # Configuración de Base de Datos: SIEMPRE SQLite Local para la app principal (Offline First)
    logger_backend.info(f"--> Usando base de datos SQLite Local: {DB_FILE}")
    db_uri = f'sqlite:///{DB_FILE}'
    if os.name == 'nt':
        db_uri = f"sqlite:///{DB_FILE.replace('\\', '/')}"
    
    app.config['SQLALCHEMY_DATABASE_URI'] = db_uri
    app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
    
    # Pool configuration para SQLite (optimización de conexiones locales)
    app.config['SQLALCHEMY_ENGINE_OPTIONS'] = {
        'pool_pre_ping': True,
        'pool_recycle': 3600
    }
    
    # Init Extensions
    db.init_app(app)
    
    # Register Blueprints
    app.register_blueprint(auth_bp)
    app.register_blueprint(characters_bp)
    app.register_blueprint(admin_bp)
    app.register_blueprint(character_update_bp, url_prefix='/api/characters')
    
    # Health Check Endpoint
    @app.route('/health', methods=['GET'])
    def health():
        try:
            conn = sqlite3.connect(DB_FILE)
            cursor = conn.cursor()
            cursor.execute('SELECT COUNT(*) FROM characters')
            count = cursor.fetchone()[0]
            conn.close()
            db_status = "connected"
        except Exception as e:
            logger_backend.error(f"Health check database error: {str(e)}", exc_info=True)
            db_status = f"error: {str(e)}"
            count = 0
            
        return jsonify({
            "status": "ok",
            "message": "Backend is running (Refactored)",
            "db_status": db_status,
            "cached_characters": count
        })
        
    return app

app = create_app()

if __name__ == '__main__':
    # Asegurar que el directorio de datos existe
    if not os.path.exists(os.path.dirname(DB_FILE)):
        os.makedirs(os.path.dirname(DB_FILE))
        
    with app.app_context():
        # Crear tablas SQLite si no existen (Fresh Install)
        db.create_all()
        logger_backend.info(f"✓ Local database checked at: {DB_FILE}")
        
    logger_backend.info("🚀 Starting Flask Backend...")
    # debug=False para producción/exe
    app.run(host='0.0.0.0', port=8000, debug=False)
