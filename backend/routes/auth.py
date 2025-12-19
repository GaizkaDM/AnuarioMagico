"""
Rutas de autenticación (Login/Register).
Maneja el registro de usuarios con clave maestra y la generación de tokens de sesión.

Autores: Xiker, Gaizka, Diego
"""
from flask import Blueprint, request, jsonify
from werkzeug.security import generate_password_hash, check_password_hash
import sqlite3
from datetime import datetime
import uuid
from backend.extensions import db, SESSIONS
from backend.models.User import User
from backend.config import MASTER_PASSWORD, DB_FILE

auth_bp = Blueprint('auth', __name__)

@auth_bp.route('/auth/register', methods=['POST'])
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
            
        # Check if user exists (using SQLAlchemy logic now for consistency, or keep raw SQL if preferred)
        # Converting to SQLAlchemy as we have the model
        if User.query.filter_by(username=username).first():
             return jsonify({'error': 'User already exists'}), 409

        # Create user
        pw_hash = generate_password_hash(password)
        created_at = datetime.now().isoformat()
        
        new_user = User(username=username, password_hash=pw_hash, created_at=created_at)
        db.session.add(new_user)
        db.session.commit()
        
        return jsonify({'success': True, 'message': 'User registered successfully'})
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@auth_bp.route('/auth/login', methods=['POST'])
def login():
    try:
        data = request.get_json()
        username = data.get('username')
        password = data.get('password')
        
        if not username or not password:
            return jsonify({'error': 'Missing fields'}), 400
            
        user = User.query.filter_by(username=username).first()
        
        if not user or not check_password_hash(user.password_hash, password):
            return jsonify({'error': 'Invalid credentials'}), 401
            
        # Generate simple token
        token = str(uuid.uuid4())
        SESSIONS[token] = username
        
        return jsonify({
            'success': True, 
            'token': token,
            'username': username
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@auth_bp.route('/auth/logout', methods=['POST'])
def logout():
    token = request.headers.get('Authorization')
    if token and token in SESSIONS:
        del SESSIONS[token]
    return jsonify({'success': True})
