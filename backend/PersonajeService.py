"""
PersonajeService.py
Servicio de negocio para gestionar operaciones de personajes
Orquesta las llamadas a sync_sqlite y ExportService en el orden correcto

Author: Xiker
"""

from typing import Dict, Optional
from sync_sqlite import DaoSQLite
from ExportService import ExportService


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
    
    def importar_personajes_desde_csv(self, ruta_csv: str) -> Dict[str, int]:
        """
        Importa personajes masivamente desde un CSV y regenera los archivos
        
        Orden de ejecución:
        1. Leer CSV
        2. Insertar/Actualizar en SQLite
        3. Regenerar todas las exportaciones (CSV, XML, Binario) UNA sola vez
        
        Args:
            ruta_csv: Ruta al archivo CSV fuente
            
        Returns:
            Diccionario con estadísticas {total, exitosos, fallidos}
            
        Author: Xiker
        """
        print(f"\n{'='*60}")
        print(f"IMPORTACIÓN MASIVA DESDE CSV: {ruta_csv}")
        print(f"{'='*60}")
        
        resultados = {"total": 0, "exitosos": 0, "fallidos": 0}
        
        # 1. Leer CSV
        print("\n[1/3] Leyendo archivo CSV...")
        personajes = self.export_service.importar_desde_csv(ruta_csv)
        
        if not personajes:
            print("✗ No se pudieron leer personajes del CSV.")
            return resultados
            
        resultados["total"] = len(personajes)
        print(f"✓ Leídos {resultados['total']} personajes.")
        
        # 2. Insertar en SQLite
        print("\n[2/3] Importando a base de datos SQLite...")
        for i, personaje in enumerate(personajes, 1):
            
            # --- VALIDACIÓN OBLIGATORIA ---
            # Nombre obligatorio
            if not personaje.get('name'):
                print(f"  ✗ [Fila {i}] OMITIDO: Falta el nombre (campo obligatorio).")
                resultados["fallidos"] += 1
                continue
                
            # Imagen (blob) obligatoria y válida
            # Nota: ExportService ya convierte el blob. Si es None, es que no había o era inválido.
            if not personaje.get('image_blob'):
                print(f"  ✗ [Fila {i}] OMITIDO: Peronaje '{personaje.get('name')}' no tiene imagen (blob) válida.")
                resultados["fallidos"] += 1
                continue
            # -------------------------------

            # Intentar añadir. Si ya existe, podríamos intentar editar?
            # Por ahora asumimos que añadir intentará insertar y fallará si existe ID
            # Pero DaoSQLite.añadir_personaje captura IntegrityError y retorna False
            
            # Nota: Si se quiere 'Upsert' (actualizar si existe), habría que modificar la lógica
            # aquí o en el DAO. Dado el requerimiento "añade todos", intentamos añadir.
            if self.dao.añadir_personaje(personaje):
                resultados["exitosos"] += 1
            else:
                # Si falla (ej: ya existe), intentamos editar para asegurar que tenemos la versión del CSV?
                # El usuario dijo "añade todos". Si ya existe, update parece razonable para "importar".
                # Intentamos editar si tiene ID
                if 'id' in personaje and personaje['id']:
                    print(f"  ⚠ El ID {personaje['id']} ya existe. Intentando actualizar...")
                    if self.dao.editar_personaje(personaje['id'], personaje):
                        resultados["exitosos"] += 1
                        print(f"  ✓ Personaje actualizado correctamente.")
                    else:
                        resultados["fallidos"] += 1
                else:
                    resultados["fallidos"] += 1
            
            if i % 10 == 0:
                print(f"  Procesados {i}/{resultados['total']}...")

        # 3. Regenerar exportaciones
        print("\n[3/3] Regenerando archivos de exportación...")
        if resultados["exitosos"] > 0:
            self.export_service.exportar_todo()
            print("✓ Archivos regenerados con exito.")
        else:
            print("⚠ No se importaron nuevos datos, se omite regeneración de archivos.")
            
        print(f"\n{'='*60}")
        print(f"RESUMEN DE IMPORTACIÓN")
        print(f"Total leídos: {resultados['total']}")
        print(f"Importados/Actualizados: {resultados['exitosos']}")
        print(f"Fallidos: {resultados['fallidos']}")
        print(f"{'='*60}\n")
        
        return resultados
    
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



