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
    
    if MYSQL_CONFIG:
        logger_backend.info(f"--> Usando base de datos MySQL Remota: {MYSQL_CONFIG['host']}")
        db_uri = f"mysql+pymysql://{MYSQL_CONFIG['user']}:{MYSQL_CONFIG['password']}@{MYSQL_CONFIG['host']}:{MYSQL_CONFIG['port']}/{MYSQL_CONFIG['database']}"
        app.config['SQLALCHEMY_ENGINE_OPTIONS'] = {'pool_recycle': 280} # Solo para MySQL
    else:
        logger_backend.info(f"--> Usando base de datos SQLite Local: {DB_FILE}")
        db_uri = f'sqlite:///{DB_FILE}'
        if os.name == 'nt':
            db_uri = f"sqlite:///{DB_FILE.replace('\\', '/')}"
    
    app.config['SQLALCHEMY_DATABASE_URI'] = db_uri
    app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
    app.config['SQLALCHEMY_ENGINE_OPTIONS'] = {'pool_recycle': 280} # Evitar desconexiones por inactividad
    
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
        
    # Init DB
    with app.app_context():
        db.create_all()
        
    return app

app = create_app()

if __name__ == '__main__':
    app.run(port=8000, debug=True)
