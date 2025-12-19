"""
PersonajeService.py
Servicio para la lógica de negocio relacionada con Personajes.
Gestiona la obtención de datos desde la API externa y la sincronización con la base de datos local.

Autores: Xiker, Gaizka, Diego
"""

from typing import Dict, Optional
from typing import Dict, Optional
from backend.services.sync_sqlite import DaoSQLite
from backend.services.ExportService import ExportService
from backend.logging_config import logger_backend


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
        logger_backend.info(f"AÑADIENDO PERSONAJE: {personaje.get('name', 'Sin nombre')}")
        
        # 1. Actualizar SQLite
        logger_backend.debug("[1/4] Actualizando base de datos SQLite...")
        if not self.dao.añadir_personaje(personaje):
            logger_backend.error("✗ Error al añadir personaje a SQLite. Operación cancelada.")
            return False
            
        # Lanzar exportaciones en segundo plano para no bloquear al usuario
        def realizar_exportaciones():
            logger_backend.debug("🔄 Iniciando exportaciones en segundo plano...")
            # 2. Exportar a CSV
            if not self.export_service.exportar_a_csv():
                logger_backend.warning("⚠ Advertencia: Error al exportar a CSV")
            
            # 3. Exportar a XML
            if not self.export_service.exportar_a_xml():
                logger_backend.warning("⚠ Advertencia: Error al exportar a XML")
            
            # 4. Exportar a Binario
            if not self.export_service.exportar_a_binario():
                logger_backend.warning("⚠ Advertencia: Error al exportar a Binario")
            logger_backend.debug("✅ Exportaciones completadas en segundo plano.")

        import threading
        export_thread = threading.Thread(target=realizar_exportaciones)
        export_thread.daemon = True
        export_thread.start()
        
        logger_backend.info(f"✓ PERSONAJE AÑADIDO (Persistencia local OK): {personaje.get('name')}")
        
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
        logger_backend.info(f"EDITANDO PERSONAJE: {personaje_id}")
        
        # 1. Actualizar SQLite
        logger_backend.debug("[1/4] Actualizando base de datos SQLite...")
        if not self.dao.editar_personaje(personaje_id, datos_actualizados):
            logger_backend.error(f"✗ Error al editar personaje {personaje_id} en SQLite. Operación cancelada.")
            return False
        
        # Lanzar exportaciones en segundo plano
        import threading
        export_thread = threading.Thread(target=self.export_service.exportar_todo)
        export_thread.daemon = True
        export_thread.start()
        
        logger_backend.info(f"✓ PERSONAJE EDITADO (Persistencia local OK): {personaje_id}")
        
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
        logger_backend.info(f"ELIMINANDO PERSONAJE: {personaje_id}")
        
        # 1. Actualizar SQLite
        logger_backend.debug("[1/4] Eliminando de base de datos SQLite...")
        if not self.dao.eliminar_personaje(personaje_id):
            logger_backend.error(f"✗ Error al eliminar personaje {personaje_id} de SQLite. Operación cancelada.")
            return False
        
        # Lanzar exportaciones en segundo plano
        import threading
        export_thread = threading.Thread(target=self.export_service.exportar_todo)
        export_thread.daemon = True
        export_thread.start()
        
        logger_backend.info(f"✓ PERSONAJE ELIMINADO (Persistencia local OK): {personaje_id}")
        
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
        if not ruta_csv:
            logger_backend.error("No se ha proporcionado la ruta del CSV.")
            return {"total": 0, "exitosos": 0, "fallidos": 0}

        logger_backend.info(f"IMPORTACIÓN MASIVA DESDE CSV: {ruta_csv}")
        
        resultados = {"total": 0, "exitosos": 0, "fallidos": 0}
        
        # 1. Leer CSV
        logger_backend.debug("[1/3] Leyendo archivo CSV...")
        personajes = self.export_service.importar_desde_csv(ruta_csv)
        
        if not personajes:
            logger_backend.error("✗ No se pudieron leer personajes del CSV.")
            return resultados
            
        resultados["total"] = len(personajes)
        logger_backend.info(f"✓ Leídos {resultados['total']} personajes.")
        
        # 2. Insertar en SQLite
        logger_backend.debug("[2/3] Importando a base de datos SQLite...")
        for i, personaje in enumerate(personajes, 1):
            
            # --- VALIDACIÓN OBLIGATORIA ---
            # Nombre obligatorio
            if not personaje.get('name'):
                logger_backend.warning(f"  ✗ [Fila {i}] OMITIDO: Falta el nombre (campo obligatorio).")
                resultados["fallidos"] += 1
                continue
                
            # Imagen (blob) obligatoria y válida
            # Nota: ExportService ya convierte el blob. Si es None, es que no había o era inválido.
            if not personaje.get('image_blob'):
                logger_backend.warning(f"  ✗ [Fila {i}] OMITIDO: Peronaje '{personaje.get('name')}' no tiene imagen (blob) válida.")
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
                    logger_backend.debug(f"  ⚠ El ID {personaje['id']} ya existe. Intentando actualizar...")
                    if self.dao.editar_personaje(personaje['id'], personaje):
                        resultados["exitosos"] += 1
                        logger_backend.debug(f"  ✓ Personaje actualizado correctamente.")
                    else:
                        resultados["fallidos"] += 1
                else:
                    resultados["fallidos"] += 1
            
            if i % 10 == 0:
                logger_backend.debug(f"  Procesados {i}/{resultados['total']}...")

        # 3. Regenerar exportaciones
        logger_backend.debug("[3/3] Regenerando archivos de exportación...")
        if resultados["exitosos"] > 0:
            self.export_service.exportar_todo()
            logger_backend.info("✓ Archivos regenerados con exito.")
        else:
            logger_backend.warning("⚠ No se importaron nuevos datos, se omite regeneración de archivos.")
            
        logger_backend.info(f"RESUMEN DE IMPORTACIÓN - Total: {resultados['total']}, Exitosos: {resultados['exitosos']}, Fallidos: {resultados['fallidos']}")
        
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




