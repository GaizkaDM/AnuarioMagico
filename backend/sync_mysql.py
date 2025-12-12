import sqlite3
import mysql.connector
from mysql.connector import Error
import sys
import os

# Configuration
SQLITE_DB = 'favorites.db'
MYSQL_CONFIG = {
    'host': '127.0.0.1',
    'port': 3309,
    'database': 'hogwarts',
    'user': 'appuser',
    'password': 'appPass123'
}

def get_sqlite_connection():
    """Connect to SQLite database"""
    try:
        return sqlite3.connect(SQLITE_DB)
    except sqlite3.Error as e:
        print(f"Error connecting to SQLite: {e}")
        return None

def get_mysql_connection():
    """Connect to MySQL database"""
    try:
        connection = mysql.connector.connect(**MYSQL_CONFIG)
        if connection.is_connected():
            return connection
    except Error as e:
        print(f"Error connecting to MySQL: {e}")
        return None

def create_mysql_tables(cursor):
    """Create tables in MySQL if they don't exist"""
    
    # Characters Table
    # MySQL BLOB handling might need MEDIUMBLOB or LONGBLOB for images
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
    
    # Favorites Table
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
    """Sync data from local SQLite to MySQL"""
    print("Starting sync: SQLite -> MySQL...")
    
    sqlite_conn = get_sqlite_connection()
    mysql_conn = get_mysql_connection()
    
    if not sqlite_conn or not mysql_conn:
        print("✗ Connection failed. Aborting sync.")
        return

    try:
        sqlite_cursor = sqlite_conn.cursor()
        mysql_cursor = mysql_conn.cursor()
        
        # 1. Create Tables
        create_mysql_tables(mysql_cursor)
        
        # 2. Sync Characters
        print("  Syncing characters...")
        sqlite_cursor.execute("SELECT * FROM characters")
        characters = sqlite_cursor.fetchall()
        
        # Get column names from SQLite to map dynamically
        col_names = [description[0] for description in sqlite_cursor.description]
        print("DEBUG: SQLite Columns:", col_names)
        
        count = 0
        for char in characters:
            char_dict = dict(zip(col_names, char))
            
            # Prepare query
            columns = ', '.join(char_dict.keys())
            placeholders = ', '.join(['%s'] * len(char_dict))
            
            # Construct ON DUPLICATE UPDATE clause
            # MySQL 8.0.20+ prefers 'AS new' syntax
            update_clause = ', '.join([f"{k}=new.{k}" for k in char_dict.keys() if k != 'id'])
            
            sql = f"""
            INSERT INTO characters ({columns}) 
            VALUES ({placeholders}) AS new
            ON DUPLICATE KEY UPDATE {update_clause}
            """
            
            mysql_cursor.execute(sql, list(char_dict.values()))
            count += 1
            
        print(f"  [OK] Synced {count} characters to MySQL.")
        
        # 3. Sync Favorites
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
        if sqlite_conn: sqlite_conn.close()
        if mysql_conn: mysql_conn.close()

if __name__ == "__main__":
    sync_sqlite_to_mysql()
