"""
Extensiones compartidas de Flask.
Inicializa instancias globales como SQLAlchemy para evitar importaciones circulares.

Autores: Xiker, Gaizka
"""
from flask_sqlalchemy import SQLAlchemy

db = SQLAlchemy()
SESSIONS = {}
