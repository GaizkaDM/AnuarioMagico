"""
Modelo de Favorito para gestionar los personajes marcados por el usuario.

Autores: Diego, Gaizka, Xiker
"""
from backend.extensions import db

class Favorite(db.Model):
    """
    Clase que representa la tabla 'favorites'.
    Vincula un ID de personaje con su estado de favorito.
    """
    __tablename__ = 'favorites'
    character_id = db.Column(db.String(255), primary_key=True)
    is_favorite = db.Column(db.Boolean, default=False)
