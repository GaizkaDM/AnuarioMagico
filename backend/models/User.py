"""
Modelo de Usuario para la autenticación y gestión de sesiones.
Representa a los usuarios que pueden acceder a la aplicación.

Autores: Xiker, Gaizka, Diego
"""
from backend.extensions import db

class User(db.Model):
    """
    Clase que representa la tabla 'users' en la base de datos.
    Almacena credenciales y fecha de creación.
    """
    __tablename__ = 'users'
    username = db.Column(db.String(255), primary_key=True)
    password_hash = db.Column(db.String(255))
    created_at = db.Column(db.String(100))
