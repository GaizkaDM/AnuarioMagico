
"""
Configuración global de la aplicación Flask.
Define rutas de base de datos, claves secretas y URLs de APIs externas.

Autores: Gaizka, Diego
"""
import os

# Directorio base del proyecto (un nivel arriba de config.py)
BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))

# Archivo de base de datos SQLite (ruta absoluta)
DB_PATH = os.path.join(BASE_DIR, 'data')
if not os.path.exists(DB_PATH):
    os.makedirs(DB_PATH)

DB_FILE = os.path.join(DB_PATH, 'anuario.db')

# Configuración de base de datos
from dotenv import load_dotenv
load_dotenv()  # Cargar variables del .env

MYSQL_CONFIG = {
    'host': os.getenv('DB_HOST', '127.0.0.1'),
    'port': int(os.getenv('DB_PORT', 3306)),
    'database': os.getenv('DB_NAME', 'hogwarts'),
    'user': os.getenv('DB_USER', 'root'),
    'password': os.getenv('DB_PASSWORD', '')
}

# Configuración de Autenticación
MASTER_PASSWORD = "HogwartsMaster"

# URL base de la API PotterDB
POTTERDB_API = "https://api.potterdb.com/v1/characters"
