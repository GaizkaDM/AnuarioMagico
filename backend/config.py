
import os

# Directorio base del proyecto (un nivel arriba de config.py)
BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))

# Archivo de base de datos SQLite (ruta absoluta)
DB_FILE = os.path.join(BASE_DIR, 'data', 'anuario.db')

# Configuración de base de datos MySQL
MYSQL_CONFIG = {
    'host': '127.0.0.1',
    'port': 3309,
    'database': 'hogwarts',
    'user': 'appuser',
    'password': 'appPass123'
}

# Configuración de Autenticación
MASTER_PASSWORD = "HogwartsMaster"

# URL base de la API PotterDB
POTTERDB_API = "https://api.potterdb.com/v1/characters"
