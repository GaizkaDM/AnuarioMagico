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
        
        # 2. Exportar a CSV
        logger_backend.debug("[2/4] Exportando a CSV...")
        if not self.export_service.exportar_a_csv():
            logger_backend.warning("⚠ Advertencia: Error al exportar a CSV")
        
        # 3. Exportar a XML
        logger_backend.debug("[3/4] Exportando a XML...")
        if not self.export_service.exportar_a_xml():
            logger_backend.warning("⚠ Advertencia: Error al exportar a XML")
        
        # 4. Exportar a Binario
        logger_backend.debug("[4/4] Exportando a Binario...")
        if not self.export_service.exportar_a_binario():
            logger_backend.warning("⚠ Advertencia: Error al exportar a Binario")
        
        logger_backend.info(f"✓ PERSONAJE AÑADIDO EXITOSAMENTE: {personaje.get('name')}")
        
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
        
        # 2. Exportar a CSV
        logger_backend.debug("[2/4] Exportando a CSV...")
        if not self.export_service.exportar_a_csv():
            logger_backend.warning("⚠ Advertencia: Error al exportar a CSV")
        
        # 3. Exportar a XML
        logger_backend.debug("[3/4] Exportando a XML...")
        if not self.export_service.exportar_a_xml():
            logger_backend.warning("⚠ Advertencia: Error al exportar a XML")
        
        # 4. Exportar a Binario
        logger_backend.debug("[4/4] Exportando a Binario...")
        if not self.export_service.exportar_a_binario():
            logger_backend.warning("⚠ Advertencia: Error al exportar a Binario")
        
        logger_backend.info(f"✓ PERSONAJE EDITADO EXITOSAMENTE: {personaje_id}")
        
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
        
        # 2. Exportar a CSV
        logger_backend.debug("[2/4] Exportando a CSV...")
        if not self.export_service.exportar_a_csv():
            logger_backend.warning("⚠ Advertencia: Error al exportar a CSV")
        
        # 3. Exportar a XML
        logger_backend.debug("[3/4] Exportando a XML...")
        if not self.export_service.exportar_a_xml():
            logger_backend.warning("⚠ Advertencia: Error al exportar a XML")
        
        # 4. Exportar a Binario
        logger_backend.debug("[4/4] Exportando a Binario...")
        if not self.export_service.exportar_a_binario():
            logger_backend.warning("⚠ Advertencia: Error al exportar a Binario")
        
        logger_backend.info(f"✓ PERSONAJE ELIMINADO EXITOSAMENTE: {personaje_id}")
        
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
