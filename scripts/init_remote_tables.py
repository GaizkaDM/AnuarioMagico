
"""
Script de Inicialización de Tablas Remotas.
Este script se conecta a la base de datos MySQL configurada en el archivo .env
y crea las tablas necesarias (users, favorites, characters) si no existen.

Autores: Gaizka, Xiker, Diego
"""
import os
import pymysql
from dotenv import load_dotenv

# Cargar variables de entorno del archivo .env
load_dotenv()

host = os.getenv('DB_HOST')
port = int(os.getenv('DB_PORT', 3306))
user = os.getenv('DB_USER')
password = os.getenv('DB_PASSWORD')
db_name = os.getenv('DB_NAME')

if not db_name:
    print("Error: DB_NAME no está definido en el archivo .env")
    exit(1)

print(f"--- Inicializando TABLAS en MySQL Remoto: {host}:{port} ({db_name}) ---")

# Definición de Tablas (Idéntico a init_mysql_full.sql pero sin DROPs peligrosos)
# Usamos CREATE TABLE IF NOT EXISTS para que sea seguro ejecutarlo varias veces.

tables_sql = [
    # 1. Tabla de Usuarios
    """
    CREATE TABLE IF NOT EXISTS users (
        username VARCHAR(255) PRIMARY KEY,
        password_hash VARCHAR(255),
        created_at VARCHAR(100)
    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    """,
    
    # 2. Tabla de Favoritos
    """
    CREATE TABLE IF NOT EXISTS favorites (
        character_id VARCHAR(255) PRIMARY KEY,
        is_favorite BOOLEAN DEFAULT FALSE,
        last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    """,
    
    # 3. Tabla de Personajes
    """
    CREATE TABLE IF NOT EXISTS characters (
        id VARCHAR(255) PRIMARY KEY,
        name VARCHAR(255),
        house VARCHAR(255),
        image TEXT,
        died TEXT,
        born TEXT,
        patronus TEXT,
        gender VARCHAR(50),
        species VARCHAR(100),
        blood_status VARCHAR(100),
        role TEXT,
        wiki TEXT,
        slug TEXT,
        image_blob MEDIUMBLOB,
        alias_names TEXT,
        animagus TEXT,
        boggart TEXT,
        eye_color TEXT,
        family_member TEXT,
        hair_color TEXT,
        height TEXT,
        jobs TEXT,
        nationality TEXT,
        romances TEXT,
        skin_color TEXT,
        titles TEXT,
        wand TEXT,
        weight TEXT,
        last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    """
]

try:
    print(f"Conectando a la base de datos '{db_name}'...")
    connection = pymysql.connect(
        host=host,
        port=port,
        user=user,
        password=password,
        database=db_name,
        cursorclass=pymysql.cursors.DictCursor
    )
    
    with connection:
        with connection.cursor() as cursor:
            for i, sql in enumerate(tables_sql, 1):
                try:
                    # Extraer el nombre de la tabla para el log (crudo pero efectivo)
                    table_name = sql.split("TABLE IF NOT EXISTS")[1].split("(")[0].strip()
                    print(f"[{i}/3] Verificando/Creando tabla '{table_name}'...")
                    cursor.execute(sql)
                except Exception as table_error:
                    print(f"Error creando tabla {i}: {table_error}")
                    raise table_error
            
            connection.commit()
            print("✅ ¡Todas las tablas han sido verificadas/creadas correctamente!")
            
            # Mostrar resumen
            cursor.execute("SHOW TABLES")
            result = cursor.fetchall()
            print("\nTablas actuales en la base de datos:")
            for row in result:
                print(f" - {list(row.values())[0]}")

except pymysql.err.OperationalError as e:
    if e.args[0] == 1049:
        print(f"❌ Error: La base de datos '{db_name}' NO existe.")
        print("   Ejecuta primero: python scripts/create_remote_db.py")
    else:
        print(f"❌ Error de conexión: {e}")
except Exception as e:
    print(f"❌ Error inesperado: {e}")
