"""
PersonajeService.py
Servicio de negocio para gestionar operaciones de personajes
Orquesta las llamadas a sync_sqlite y ExportService en el orden correcto

Author: Xiker
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



