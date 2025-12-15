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

# Import Blueprints
from backend.routes.auth import auth_bp
from backend.routes.characters import characters_bp
from backend.routes.admin import admin_bp
from backend.models.User import User
from backend.models.Favorite import Favorite
from backend.models.Character import Character

def create_app():
    app = Flask(__name__)
    CORS(app)
    
    # Config
    # Ensure URI is compatible with Windows paths
    db_uri = f'sqlite:///{DB_FILE}'
    if os.name == 'nt':
        db_uri = f"sqlite:///{DB_FILE.replace('\\', '/')}"
    
    app.config['SQLALCHEMY_DATABASE_URI'] = db_uri
    app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
    
    # Init Extensions
    db.init_app(app)
    
    # Logging
    setup_logging(app)
    
    # Register Blueprints
    app.register_blueprint(auth_bp)
    app.register_blueprint(characters_bp)
    app.register_blueprint(admin_bp)
    
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

def setup_logging(app):
    log_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'logs')
    if not os.path.exists(log_dir):
        os.makedirs(log_dir)
        
    log_file = os.path.join(log_dir, 'backend.log')
    logger = logging.getLogger()
    logger.setLevel(logging.INFO)
    
    file_handler = RotatingFileHandler(log_file, maxBytes=1_000_000, backupCount=3)
    file_handler.setFormatter(logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s'))
    logger.addHandler(file_handler)
    
    console_handler = logging.StreamHandler()
    console_handler.setFormatter(logging.Formatter('%(asctime)s - %(levelname)s - %(message)s'))
    logger.addHandler(console_handler)

app = create_app()

if __name__ == '__main__':
    app.run(port=8000, debug=True)
