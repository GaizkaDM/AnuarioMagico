"""
Script de Creación de Base de Datos Remota.
Intenta conectar al servidor MySQL especificado en el archivo .env y crear
la base de datos (schema) vacía si aún no existe.

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
# Nombre de la base de datos a crear (desde .env)
new_db_name = os.getenv('DB_NAME', 'AnuarioMagico') 

print(f"Conectando a {host}:{port} con usuario '{user}'...")

try:
    # Conectamos sin especificar DB para poder crearla
    connection = pymysql.connect(
        host=host,
        port=port,
        user=user,
        password=password,
        charset='utf8mb4',
        cursorclass=pymysql.cursors.DictCursor
    )
    
    try:
        with connection.cursor() as cursor:
            # Crear la base de datos
            print(f"Intentando crear la base de datos '{new_db_name}'...")
            sql = f"CREATE DATABASE IF NOT EXISTS {new_db_name} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
            cursor.execute(sql)
            print(f"¡Éxito! Base de datos '{new_db_name}' asegurada.")
            
            # Verificar
            cursor.execute("SHOW DATABASES")
            result = cursor.fetchall()
            databases = [d['Database'] for d in result]
            print(f"Bases de datos existentes: {databases}")
            
            if new_db_name in databases:
                print(f"La base de datos '{new_db_name}' está lista para usarse.")
            else:
                print("Error: No se ve la base de datos creada en la lista.")

    finally:
        connection.close()

except Exception as e:
    print(f"Error al conectar o crear la base de datos: {e}")
