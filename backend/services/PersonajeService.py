"""
PersonajeService.py
Servicio para la lógica de negocio relacionada con Personajes.
Gestiona la obtención de datos desde la API externa y la sincronización con la base de datos local.

Autores: Xiker
"""

from typing import Dict, Optional
from typing import Dict, Optional
from backend.services.sync_sqlite import DaoSQLite
from backend.services.ExportService import ExportService


class PersonajeService:
    """Servicio de negocio para operaciones CRUD de personajes con exportación automática"""
    
    def __init__(self, db_file: str = 'favorites.db'):
        """
        Inicializa el servicio de personajes
        
        Args:
            db_file: Ruta al archivo de base de datos SQLite
            
        Author: Xiker
        """
        self.dao = DaoSQLite(db_file)
        self.export_service = ExportService(self.dao)
    
    def añadir_personaje(self, personaje: Dict) -> bool:
        """
        Añade un nuevo personaje y actualiza todos los archivos de exportación
        
        Orden de ejecución:
        1. Actualizar SQLite
        2. Exportar a CSV
        3. Exportar a XML
        4. Exportar a Binario (usando XML)
        
        Args:
            personaje: Diccionario con los datos del personaje
            
        Returns:
            True si todas las operaciones fueron exitosas
            
        Author: Xiker
        """
        print(f"\n{'='*60}")
        print(f"AÑADIENDO PERSONAJE: {personaje.get('name', 'Sin nombre')}")
        print(f"{'='*60}")
        
        # 1. Actualizar SQLite
        print("\n[1/4] Actualizando base de datos SQLite...")
        if not self.dao.añadir_personaje(personaje):
            print("✗ Error al añadir personaje a SQLite. Operación cancelada.")
            return False
        
        # 2. Exportar a CSV
        print("\n[2/4] Exportando a CSV...")
        if not self.export_service.exportar_a_csv():
            print("⚠ Advertencia: Error al exportar a CSV")
        
        # 3. Exportar a XML
        print("\n[3/4] Exportando a XML...")
        if not self.export_service.exportar_a_xml():
            print("⚠ Advertencia: Error al exportar a XML")
        
        # 4. Exportar a Binario
        print("\n[4/4] Exportando a Binario...")
        if not self.export_service.exportar_a_binario():
            print("⚠ Advertencia: Error al exportar a Binario")
        
        print(f"\n{'='*60}")
        print(f"✓ PERSONAJE AÑADIDO EXITOSAMENTE")
        print(f"{'='*60}\n")
        
        return True
    
    def editar_personaje(self, personaje_id: str, datos_actualizados: Dict) -> bool:
        """
        Edita un personaje existente y actualiza todos los archivos de exportación
        
        Orden de ejecución:
        1. Actualizar SQLite
        2. Exportar a CSV
        3. Exportar a XML
        4. Exportar a Binario (usando XML)
        
        Args:
            personaje_id: ID del personaje a editar
            datos_actualizados: Diccionario con los campos a actualizar
            
        Returns:
            True si todas las operaciones fueron exitosas
            
        Author: Xiker
        """
        print(f"\n{'='*60}")
        print(f"EDITANDO PERSONAJE: {personaje_id}")
        print(f"{'='*60}")
        
        # 1. Actualizar SQLite
        print("\n[1/4] Actualizando base de datos SQLite...")
        if not self.dao.editar_personaje(personaje_id, datos_actualizados):
            print("✗ Error al editar personaje en SQLite. Operación cancelada.")
            return False
        
        # 2. Exportar a CSV
        print("\n[2/4] Exportando a CSV...")
        if not self.export_service.exportar_a_csv():
            print("⚠ Advertencia: Error al exportar a CSV")
        
        # 3. Exportar a XML
        print("\n[3/4] Exportando a XML...")
        if not self.export_service.exportar_a_xml():
            print("⚠ Advertencia: Error al exportar a XML")
        
        # 4. Exportar a Binario
        print("\n[4/4] Exportando a Binario...")
        if not self.export_service.exportar_a_binario():
            print("⚠ Advertencia: Error al exportar a Binario")
        
        print(f"\n{'='*60}")
        print(f"✓ PERSONAJE EDITADO EXITOSAMENTE")
        print(f"{'='*60}\n")
        
        return True
    
    def eliminar_personaje(self, personaje_id: str) -> bool:
        """
        Elimina un personaje y actualiza todos los archivos de exportación
        
        Orden de ejecución:
        1. Actualizar SQLite (eliminar)
        2. Exportar a CSV
        3. Exportar a XML
        4. Exportar a Binario (usando XML)
        
        Args:
            personaje_id: ID del personaje a eliminar
            
        Returns:
            True si todas las operaciones fueron exitosas
            
        Author: Xiker
        """
        print(f"\n{'='*60}")
        print(f"ELIMINANDO PERSONAJE: {personaje_id}")
        print(f"{'='*60}")
        
        # 1. Actualizar SQLite
        print("\n[1/4] Eliminando de base de datos SQLite...")
        if not self.dao.eliminar_personaje(personaje_id):
            print("✗ Error al eliminar personaje de SQLite. Operación cancelada.")
            return False
        
        # 2. Exportar a CSV
        print("\n[2/4] Exportando a CSV...")
        if not self.export_service.exportar_a_csv():
            print("⚠ Advertencia: Error al exportar a CSV")
        
        # 3. Exportar a XML
        print("\n[3/4] Exportando a XML...")
        if not self.export_service.exportar_a_xml():
            print("⚠ Advertencia: Error al exportar a XML")
        
        # 4. Exportar a Binario
        print("\n[4/4] Exportando a Binario...")
        if not self.export_service.exportar_a_binario():
            print("⚠ Advertencia: Error al exportar a Binario")
        
        print(f"\n{'='*60}")
        print(f"✓ PERSONAJE ELIMINADO EXITOSAMENTE")
        print(f"{'='*60}\n")
        
        return True
    
    def obtener_personaje(self, personaje_id: str) -> Optional[Dict]:
        """
        Obtiene un personaje por su ID
        
        Args:
            personaje_id: ID del personaje
            
        Returns:
            Diccionario con los datos del personaje o None si no existe
            
        Author: Xiker
        """
        return self.dao.obtener_personaje(personaje_id)
    
    def listar_personajes(self):
        """
        Lista todos los personajes
        
        Returns:
            Lista de diccionarios con los datos de todos los personajes
            
        Author: Xiker
        """
        return self.dao.obtener_todos_personajes()



    def importar_personajes_desde_csv(self, csv_path: str) -> Dict:
        """
        Importa personajes masivamente desde un archivo CSV.
        
        Args:
            csv_path: Ruta al archivo CSV
            
        Returns:
            Diccionario con estadísticas de la importación
        """
        import csv
        import json
        
        stats = {
            "total": 0,
            "added": 0,
            "failed": 0,
            "errors": []
        }
        
        print(f"\n{'='*60}")
        print(f"IMPORTANDO PERSONAJES DESDE CSV")
        print(f"{'='*60}")
        
        try:
            with open(csv_path, 'r', encoding='utf-8') as f:
                reader = csv.DictReader(f)
                
                # Campos que necesitan ser parseados de JSON str a lista/dict
                json_fields = ['alias_names', 'family_member', 'jobs', 'romances', 'titles', 'wand']
                
                for row in reader:
                    stats['total'] += 1
                    try:
                        personaje = dict(row)
                        
                        # Limpiar y convertir campos JSON
                        for field in json_fields:
                            if field in personaje and personaje[field]:
                                try:
                                    # Intentar parsear si parece JSON (empieza con [ o {)
                                    val = personaje[field].strip()
                                    if val.startswith('[') or val.startswith('{'):
                                        personaje[field] = json.loads(val)
                                    else:
                                        # Si no es JSON pero esperamos lista, ponerlo en lista si no está vacío
                                        if val:
                                            personaje[field] = [val]
                                        else:
                                            personaje[field] = []
                                except json.JSONDecodeError:
                                    # Fallback: tratar como string simple en lista
                                    personaje[field] = [personaje[field]]
                            else:
                                personaje[field] = []
                        
                        # Añadir a SQLite
                        if self.dao.añadir_personaje(personaje):
                            stats['added'] += 1
                        else:
                            stats['failed'] += 1
                            stats['errors'].append(f"Fila {stats['total']}: Error al guardar en BD (ID: {personaje.get('id', 'N/A')})")
                            
                    except Exception as e:
                        stats['failed'] += 1
                        stats['errors'].append(f"Fila {stats['total']}: {str(e)}")
            
            # Regenerar exportaciones (solo una vez al final)
            if stats['added'] > 0:
                print("\n[INFO] Regenerando archivos de exportación...")
                self.export_service.exportar_a_csv()
                self.export_service.exportar_a_xml()
                self.export_service.exportar_a_binario()
            
            print(f"\nResultados de importación:")
            print(f"  Total procesados: {stats['total']}")
            print(f"  Añadidos: {stats['added']}")
            print(f"  Fallidos: {stats['failed']}")
            
            return stats
            
        except Exception as e:
            print(f"Error fatal durante importación CSV: {str(e)}")
            raise e
