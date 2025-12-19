"""
Script de Verificación de Base de Datos Local.
Comprueba la existencia del archivo de base de datos SQLite 'anuario.db'
y lista las tablas que contiene para verificar su integridad.

Autores: Gaizka, Xiker, Diego
"""
import sqlite3
import os

db_path = os.path.join('data', 'anuario.db')
print(f"Checking DB at: {os.path.abspath(db_path)}")

if not os.path.exists(db_path):
    print("DB file does not exist!")
else:
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
        tables = cursor.fetchall()
        print("Tables:", tables)
        conn.close()
    except Exception as e:
        print(f"Error: {e}")
