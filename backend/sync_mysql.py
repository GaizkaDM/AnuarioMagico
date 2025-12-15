import sqlite3
import mysql.connector
from mysql.connector import Error
import sys
import os



"""
Módulo para la sincronización de la base de datos local SQLite con una base de datos MySQL remota.
Permite respaldar datos de personajes, favoritos y usuarios.

@author: GaizkaFrost
@version: 1.0
@date: 2025-12-14
"""

__author__ = "GaizkaFrost"
__version__ = "1.0"

from datetime import datetime
try:
    from config import DB_FILE, MYSQL_CONFIG
except ImportError:
    # Fallback si se ejecuta como script standalone sin contexto de paquete
    import sys
    sys.path.append(os.path.dirname(os.path.abspath(__file__)))
    from config import DB_FILE, MYSQL_CONFIG

# Asignar alias para compatibilidad interna
SQLITE_DB = DB_FILE

def get_sqlite_connection():
    """
    Establece conexión con la base de datos SQLite.

    Returns:
        Connection: Objeto de conexión SQLite o None si falla.
    """
    try:
        return sqlite3.connect(SQLITE_DB)
    except sqlite3.Error as e:
        print(f"Error connecting to SQLite: {e}")
        return None

def get_mysql_connection():
    """
    Establece conexión con la base de datos MySQL.

    Returns:
        Connection: Objeto de conexión MySQL o None si falla.
    """

        
    try:
        connection = mysql.connector.connect(**MYSQL_CONFIG)
        if connection.is_connected():
            return connection
    except Error as e:
        print(f"Error connecting to MySQL: {e}")
        return None

def create_mysql_tables(cursor):
    """
    Crea las tablas necesarias en MySQL si no existen.
    
    Args:
        cursor: Cursor de la conexión MySQL.
    """
    
    # Tabla de Personajes
    # El manejo de BLOB en MySQL podría necesitar MEDIUMBLOB o LONGBLOB para imágenes
    cursor.execute("DROP TABLE IF EXISTS characters")
    cursor.execute("""
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
    )
    """)
    cursor.execute("DESCRIBE characters")
    print("DEBUG: MySQL Table Schema:", cursor.fetchall())
    
    # Tabla de Favoritos
    cursor.execute("DROP TABLE IF EXISTS favorites")
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS favorites (
        character_id VARCHAR(255) PRIMARY KEY,
        is_favorite BOOLEAN DEFAULT FALSE,
        last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    )
    """)
    print("[OK] MySQL tables checked/created.")

def sync_sqlite_to_mysql():
    """
    Función principal de sincronización.
    Lee datos de SQLite y los inserta/actualiza en MySQL.
    """
    print("Starting sync: SQLite -> MySQL...")
    
    sqlite_conn = get_sqlite_connection()
    mysql_conn = get_mysql_connection()
    
    if not sqlite_conn or not mysql_conn:
        print("✗ Connection failed. Aborting sync.")
        return

    try:
        sqlite_cursor = sqlite_conn.cursor()
        mysql_cursor = mysql_conn.cursor()
        
        # 1. Crear Tablas
        create_mysql_tables(mysql_cursor)
        
        # 2. Sincronizar Personajes
        print("  Syncing characters...")
        sqlite_cursor.execute("SELECT * FROM characters")
        characters = sqlite_cursor.fetchall()
        
        # Obtener nombres de columnas de SQLite para mapear dinámicamente
        col_names = [description[0] for description in sqlite_cursor.description]
        print("DEBUG: SQLite Columns:", col_names)
        
        count = 0
        for char in characters:
            char_dict = dict(zip(col_names, char))
            
            # Preparar consulta
            columns = ', '.join(char_dict.keys())
            placeholders = ', '.join(['%s'] * len(char_dict))
            
            # Construir cláusula ON DUPLICATE UPDATE
            # MySQL 8.0.20+ prefiere sintaxis 'AS new'
            update_clause = ', '.join([f"{k}=new.{k}" for k in char_dict.keys() if k != 'id'])
            
            sql = f"""
            INSERT INTO characters ({columns}) 
            VALUES ({placeholders}) AS new
            ON DUPLICATE KEY UPDATE {update_clause}
            """
            
            mysql_cursor.execute(sql, list(char_dict.values()))
            count += 1
            
        print(f"  [OK] Synced {count} characters to MySQL.")
        
        # 3. Sincronizar Favoritos
        print("  Syncing favorites...")
        sqlite_cursor.execute("SELECT * FROM favorites")
        favorites = sqlite_cursor.fetchall()
        
        fav_count = 0
        for fav in favorites:
            c_id, is_fav = fav
            mysql_cursor.execute("""
                INSERT INTO favorites (character_id, is_favorite) 
                VALUES (%s, %s) AS new
                ON DUPLICATE KEY UPDATE is_favorite=new.is_favorite
            """, (c_id, is_fav))
            fav_count += 1
            
        print(f"  [OK] Synced {fav_count} favorites to MySQL.")
        
        mysql_conn.commit()
        print("[OK] Synchronization complete!")
        
    except Error as e:
        print(f"[ERROR] MySQL Error: {e}")
    except sqlite3.Error as e:
        print(f"[ERROR] SQLite Error: {e}")
    finally:
        if mysql_conn: mysql_conn.close()

def sync_mysql_to_sqlite():
    """
    Descarga cambios de MySQL y actualiza la base de datos local SQLite (Pull).
    Estrategia 'Smart Merge': Actualiza lo local con lo remoto.
    """
    print("Starting sync: MySQL -> SQLite (Pull)...")
    
    sqlite_conn = get_sqlite_connection()
    mysql_conn = get_mysql_connection()
    
    if not sqlite_conn or not mysql_conn:
        print("✗ Connection failed. Aborting pull.")
        return

    try:
        sqlite_cursor = sqlite_conn.cursor()
        mysql_cursor = mysql_conn.cursor(dictionary=True)
        
        # 1. Pull Users
        print("  Pulling users...")
        mysql_cursor.execute("SELECT * FROM users")
        users = mysql_cursor.fetchall()
        
        for u in users:
            sqlite_cursor.execute("""
                INSERT OR REPLACE INTO users (username, password_hash, created_at)
                VALUES (?, ?, ?)
            """, (u['username'], u['password_hash'], u['created_at']))
            
        print(f"  [OK] Pulled {len(users)} users.")

        # 2. Pull Favorites
        print("  Pulling favorites...")
        mysql_cursor.execute("SELECT * FROM favorites")
        favorites = mysql_cursor.fetchall()
        
        for fav in favorites:
            char_id = fav['character_id']
            is_fav = fav['is_favorite'] # 1 or 0
            
            if is_fav:
                sqlite_cursor.execute("""
                    INSERT OR REPLACE INTO favorites (character_id, is_favorite)
                    VALUES (?, 1)
                """, (char_id,))
            else:
                # Si en MySQL dice que no es favorito, lo quitamos de local
                sqlite_cursor.execute("DELETE FROM favorites WHERE character_id = ?", (char_id,))
                
        print(f"  [OK] Pulled {len(favorites)} favorites.")

        # 3. Pull Characters
        # Solo actualizamos metadatos para no machacar BLOBs locales si en la nube están vacíos
        # (Asumiendo que la nube puede no tener las imágenes pesadas)
        print("  Pulling characters...")
        mysql_cursor.execute("SELECT * FROM characters")
        characters = mysql_cursor.fetchall()
        
        count = 0
        for char in characters:
            # Recuperar blob actual local si existe, para protegerlo
            sqlite_cursor.execute("SELECT image_blob FROM characters WHERE id = ?", (char['id'],))
            row = sqlite_cursor.fetchone()
            local_blob = row[0] if row else None
            
            # Usar blob remoto solo si existe, si no, mantener local
            blob_to_save = char['image_blob']
            if not blob_to_save:
                blob_to_save = local_blob

            # Construir query dinámicamente o mapear campos conocidos
            # Mapeamos explícitamente para seguridad
            sqlite_cursor.execute("""
                INSERT OR REPLACE INTO characters (
                    id, name, house, image, died, born, patronus, 
                    gender, species, blood_status, role, wiki, slug, 
                    image_blob, alias_names, animagus, boggart, eye_color, 
                    family_member, hair_color, height, jobs, nationality, 
                    romances, skin_color, titles, wand, weight
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                char['id'], char['name'], char['house'], char['image'], 
                char['died'], char['born'], char['patronus'],
                char['gender'], char['species'], char['blood_status'], 
                char['role'], char['wiki'], char['slug'], 
                blob_to_save, # BLOB gestionado
                char['alias_names'], char['animagus'], char['boggart'], char['eye_color'], 
                char['family_member'], char['hair_color'], char['height'], char['jobs'], char['nationality'], 
                char['romances'], char['skin_color'], char['titles'], char['wand'], char['weight']
            ))
            count += 1
            
        print(f"  [OK] Pulled {count} characters.")
        
        sqlite_conn.commit()
        print("[OK] Pull complete!")
        
    except Error as e:
        print(f"[ERROR] MySQL Error: {e}")
    except sqlite3.Error as e:
        print(f"[ERROR] SQLite Error: {e}")
    finally:
        if sqlite_conn: sqlite_conn.close()
        if mysql_conn: mysql_conn.close()

if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "pull":
        sync_mysql_to_sqlite()
    else:
        sync_sqlite_to_mysql()
