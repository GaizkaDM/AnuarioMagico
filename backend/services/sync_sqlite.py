"""
sync_sqlite.py
Data Access Object para gestionar operaciones CRUD en la base de datos SQLite

Author: Xiker
"""

import sqlite3
import json
from typing import Dict, List, Optional
import json
from typing import Dict, List, Optional
from backend.services.sync_mysql import get_sqlite_connection
from backend.logging_config import logger_backend


class DaoSQLite:
    """Clase para gestionar operaciones de base de datos SQLite"""
    
    def __init__(self, db_file: str = 'favorites.db'):
        """
        Inicializa el DAO con la ruta de la base de datos
        
        Args:
            db_file: Ruta al archivo de base de datos SQLite
            
        Author: Xiker
        """
        self.db_file = db_file
    
    def _get_connection(self) -> sqlite3.Connection:
        """
        Crea y retorna una conexión a la base de datos using sync_mysql utility
        
        Author: Xiker
        """
        # Nota: get_sqlite_connection utiliza la configuración global (SQLITE_DB)
        # ignorando self.db_file si es diferente.
        conn = get_sqlite_connection()
        if conn:
            conn.row_factory = sqlite3.Row
            return conn
        raise sqlite3.Error(f"No se pudo conectar a la base de datos")
    
    def añadir_personaje(self, personaje: Dict) -> bool:
        """
        Añade un nuevo personaje a la base de datos
        
        Args:
            personaje: Diccionario con los datos del personaje
            
        Returns:
            True si se añadió correctamente, False en caso contrario
            
        Author: Xiker
        """
        try:
            conn = self._get_connection()
            cursor = conn.cursor()
            
            # Preparar datos JSON
            alias_names = json.dumps(personaje.get('alias_names', []))
            family_member = json.dumps(personaje.get('family_member', []))
            jobs = json.dumps(personaje.get('jobs', []))
            romances = json.dumps(personaje.get('romances', []))
            titles = json.dumps(personaje.get('titles', []))
            wand = json.dumps(personaje.get('wand', []))
            
            cursor.execute('''
                INSERT INTO characters (
                    id, name, house, image, died, born, patronus, 
                    gender, species, blood_status, role, wiki, slug, image_blob,
                    alias_names, animagus, boggart, eye_color, family_member,
                    hair_color, height, jobs, nationality, romances,
                    skin_color, titles, wand, weight
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (
                personaje.get('id'),
                personaje.get('name'),
                personaje.get('house', ''),
                personaje.get('image', ''),
                personaje.get('died', ''),
                personaje.get('born', ''),
                personaje.get('patronus', ''),
                personaje.get('gender', ''),
                personaje.get('species', ''),
                personaje.get('blood_status', ''),
                personaje.get('role', ''),
                personaje.get('wiki', ''),
                personaje.get('slug', ''),
                personaje.get('image_blob'),
                alias_names,
                personaje.get('animagus', ''),
                personaje.get('boggart', ''),
                personaje.get('eye_color', ''),
                family_member,
                personaje.get('hair_color', ''),
                personaje.get('height', ''),
                jobs,
                personaje.get('nationality', ''),
                romances,
                personaje.get('skin_color', ''),
                titles,
                wand,
                personaje.get('weight', '')
            ))
            
            conn.commit()
            conn.close()
            logger_backend.debug(f"✓ Personaje '{personaje.get('name')}' añadido a SQLite")
            return True
            
        except sqlite3.IntegrityError as e:
            logger_backend.warning(f"✗ Error: El personaje con ID '{personaje.get('id')}' ya existe")
            return False
        except Exception as e:
            logger_backend.error(f"✗ Error al añadir personaje: {str(e)}", exc_info=True)
            return False
    
    def editar_personaje(self, personaje_id: str, datos_actualizados: Dict) -> bool:
        """
        Edita un personaje existente en la base de datos
        
        Args:
            personaje_id: ID del personaje a editar
            datos_actualizados: Diccionario con los campos a actualizar
            
        Returns:
            True si se editó correctamente, False en caso contrario
            
        Author: Xiker
        """
        try:
            conn = self._get_connection()
            cursor = conn.cursor()
            
            # Verificar que el personaje existe
            cursor.execute('SELECT id FROM characters WHERE id = ?', (personaje_id,))
            if not cursor.fetchone():
                logger_backend.error(f"✗ Error: No existe personaje con ID '{personaje_id}'")
                conn.close()
                return False
            
            # Construir la query dinámicamente según los campos proporcionados
            campos_actualizar = []
            valores = []
            
            # Campos simples
            campos_simples = [
                'name', 'house', 'image', 'died', 'born', 'patronus',
                'gender', 'species', 'blood_status', 'role', 'wiki', 'slug',
                'animagus', 'boggart', 'eye_color', 'hair_color', 'height',
                'nationality', 'skin_color', 'weight', 'image_blob'
            ]
            
            for campo in campos_simples:
                if campo in datos_actualizados:
                    campos_actualizar.append(f"{campo} = ?")
                    valores.append(datos_actualizados[campo])
            
            # Campos JSON
            campos_json = ['alias_names', 'family_member', 'jobs', 'romances', 'titles', 'wand']
            for campo in campos_json:
                if campo in datos_actualizados:
                    campos_actualizar.append(f"{campo} = ?")
                    valores.append(json.dumps(datos_actualizados[campo]))
            
            if not campos_actualizar:
                logger_backend.warning("⚠ No hay campos para actualizar")
                conn.close()
                return False
            
            # Añadir el ID al final de los valores
            valores.append(personaje_id)
            
            query = f"UPDATE characters SET {', '.join(campos_actualizar)} WHERE id = ?"
            cursor.execute(query, valores)
            
            conn.commit()
            conn.close()
            logger_backend.debug(f"✓ Personaje '{personaje_id}' editado en SQLite")
            return True
            
        except Exception as e:
            logger_backend.error(f"✗ Error al editar personaje: {str(e)}", exc_info=True)
            return False
    
    def eliminar_personaje(self, personaje_id: str) -> bool:
        """
        Elimina un personaje de la base de datos
        
        Args:
            personaje_id: ID del personaje a eliminar
            
        Returns:
            True si se eliminó correctamente, False en caso contrario
            
        Author: Xiker
        """
        try:
            conn = self._get_connection()
            cursor = conn.cursor()
            
            # Verificar que el personaje existe
            cursor.execute('SELECT id FROM characters WHERE id = ?', (personaje_id,))
            if not cursor.fetchone():
                logger_backend.error(f"✗ Error: No existe personaje con ID '{personaje_id}'")
                conn.close()
                return False
            
            # Eliminar de la tabla de personajes
            cursor.execute('DELETE FROM characters WHERE id = ?', (personaje_id,))
            
            # Eliminar también de favoritos si existe
            cursor.execute('DELETE FROM favorites WHERE character_id = ?', (personaje_id,))
            
            conn.commit()
            conn.close()
            logger_backend.debug(f"✓ Personaje '{personaje_id}' eliminado de SQLite")
            return True
            
        except Exception as e:
            logger_backend.error(f"✗ Error al eliminar personaje: {str(e)}", exc_info=True)
            return False
    
    def obtener_personaje(self, personaje_id: str) -> Optional[Dict]:
        """
        Obtiene un personaje por su ID
        
        Args:
            personaje_id: ID del personaje
            
        Returns:
            Diccionario con los datos del personaje o None si no existe
            
        Author: Xiker
        """
        try:
            conn = self._get_connection()
            cursor = conn.cursor()
            
            cursor.execute('SELECT * FROM characters WHERE id = ?', (personaje_id,))
            row = cursor.fetchone()
            conn.close()
            
            if not row:
                return None
            
            personaje = dict(row)
            
            # Parsear campos JSON
            json_fields = ['alias_names', 'family_member', 'jobs', 'romances', 'titles', 'wand']
            for field in json_fields:
                if field in personaje and personaje[field]:
                    try:
                        personaje[field] = json.loads(personaje[field])
                    except json.JSONDecodeError:
                        personaje[field] = []
                else:
                    personaje[field] = []
            
            return personaje
            
        except Exception as e:
            logger_backend.error(f"✗ Error al obtener personaje: {str(e)}", exc_info=True)
            return None
    
    def obtener_todos_personajes(self) -> List[Dict]:
        """
        Obtiene todos los personajes de la base de datos
        
        Returns:
            Lista de diccionarios con los datos de todos los personajes
            
        Author: Xiker
        """
        try:
            conn = self._get_connection()
            cursor = conn.cursor()
            
            cursor.execute('SELECT * FROM characters')
            rows = cursor.fetchall()
            conn.close()
            
            personajes = []
            for row in rows:
                personaje = dict(row)
                
                # Parsear campos JSON
                json_fields = ['alias_names', 'family_member', 'jobs', 'romances', 'titles', 'wand']
                for field in json_fields:
                    if field in personaje and personaje[field]:
                        try:
                            personaje[field] = json.loads(personaje[field])
                        except json.JSONDecodeError:
                            personaje[field] = []
                    else:
                        personaje[field] = []
                
                personajes.append(personaje)
            
            return personajes
            
        except Exception as e:
            logger_backend.error(f"✗ Error al obtener personajes: {str(e)}", exc_info=True)
            return []
