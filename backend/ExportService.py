"""
ExportService.py
Servicio para exportar datos de personajes a diferentes formatos (CSV, XML, Binario)

Author: Xiker
"""

import csv
import xml.etree.ElementTree as ET
from xml.dom import minidom
import pickle
import json
from typing import List, Dict
from sync_sqlite import DaoSQLite


class ExportService:
    """Clase para exportar datos de personajes a diferentes formatos"""
    
    def __init__(self, dao: DaoSQLite):
        """
        Inicializa el servicio de exportación
        
        Args:
            dao: Instancia de DaoSQLite para acceder a los datos
            
        Author: Xiker
        """
        self.dao = dao
        self.csv_file = 'personajes.csv'
        self.xml_file = 'personajes.xml'
        self.bin_file = 'personajes.bin'
    
    def exportar_a_csv(self) -> bool:
        """
        Exporta todos los personajes a un archivo CSV
        
        Returns:
            True si se exportó correctamente, False en caso contrario
            
        Author: Xiker
        """
        try:
            personajes = self.dao.obtener_todos_personajes()
            
            if not personajes:
                print("⚠ No hay personajes para exportar a CSV")
                return False
            
            # Definir las columnas del CSV
            columnas = [
                'id', 'name', 'house', 'image', 'died', 'born', 'patronus',
                'gender', 'species', 'blood_status', 'role', 'wiki', 'slug',
                'alias_names', 'animagus', 'boggart', 'eye_color', 'family_member',
                'hair_color', 'height', 'jobs', 'nationality', 'romances',
                'skin_color', 'titles', 'wand', 'weight'
            ]
            
            with open(self.csv_file, 'w', newline='', encoding='utf-8') as csvfile:
                writer = csv.DictWriter(csvfile, fieldnames=columnas)
                writer.writeheader()
                
                for personaje in personajes:
                    # Convertir listas a strings para CSV
                    row = {}
                    for col in columnas:
                        valor = personaje.get(col, '')
                        
                        # Si es una lista, convertir a JSON string
                        if isinstance(valor, list):
                            row[col] = json.dumps(valor, ensure_ascii=False)
                        # Si es None, usar string vacío
                        elif valor is None:
                            row[col] = ''
                        # Si es bytes (image_blob), omitir
                        elif isinstance(valor, bytes):
                            row[col] = '[BINARY_DATA]'
                        else:
                            row[col] = str(valor)
                    
                    writer.writerow(row)
            
            print(f"✓ Exportados {len(personajes)} personajes a {self.csv_file}")
            return True
            
        except Exception as e:
            print(f"✗ Error al exportar a CSV: {str(e)}")
            return False
    
    def exportar_a_xml(self) -> bool:
        """
        Exporta todos los personajes a un archivo XML
        
        Returns:
            True si se exportó correctamente, False en caso contrario
            
        Author: Xiker
        """
        try:
            personajes = self.dao.obtener_todos_personajes()
            
            if not personajes:
                print("⚠ No hay personajes para exportar a XML")
                return False
            
            # Crear el elemento raíz
            root = ET.Element('personajes')
            
            for personaje in personajes:
                personaje_elem = ET.SubElement(root, 'personaje')
                
                for key, value in personaje.items():
                    # Omitir image_blob (datos binarios)
                    if key == 'image_blob':
                        continue
                    
                    campo_elem = ET.SubElement(personaje_elem, key)
                    
                    # Convertir el valor a string
                    if isinstance(value, list):
                        # Para listas, crear subelementos
                        for item in value:
                            item_elem = ET.SubElement(campo_elem, 'item')
                            if isinstance(item, dict):
                                item_elem.text = json.dumps(item, ensure_ascii=False)
                            else:
                                item_elem.text = str(item) if item else ''
                    elif value is None:
                        campo_elem.text = ''
                    elif isinstance(value, bytes):
                        campo_elem.text = '[BINARY_DATA]'
                    else:
                        campo_elem.text = str(value)
            
            # Convertir a string con formato bonito
            xml_str = minidom.parseString(ET.tostring(root, encoding='utf-8')).toprettyxml(indent="  ")
            
            # Guardar en archivo
            with open(self.xml_file, 'w', encoding='utf-8') as f:
                f.write(xml_str)
            
            print(f"✓ Exportados {len(personajes)} personajes a {self.xml_file}")
            return True
            
        except Exception as e:
            print(f"✗ Error al exportar a XML: {str(e)}")
            return False
    
    def exportar_a_binario(self) -> bool:
        """
        Exporta todos los personajes a un archivo binario usando el XML como fuente
        
        Returns:
            True si se exportó correctamente, False en caso contrario
            
        Author: Xiker
        """
        try:
            # Leer el archivo XML
            try:
                tree = ET.parse(self.xml_file)
                root = tree.getroot()
            except FileNotFoundError:
                print(f"✗ Error: No se encuentra el archivo {self.xml_file}")
                return False
            except ET.ParseError as e:
                print(f"✗ Error al parsear XML: {str(e)}")
                return False
            
            # Convertir XML a estructura de datos Python
            personajes = []
            
            for personaje_elem in root.findall('personaje'):
                personaje = {}
                
                for campo in personaje_elem:
                    campo_nombre = campo.tag
                    
                    # Si tiene subelementos (es una lista)
                    items = campo.findall('item')
                    if items:
                        personaje[campo_nombre] = [item.text or '' for item in items]
                    else:
                        personaje[campo_nombre] = campo.text or ''
                
                personajes.append(personaje)
            
            # Serializar a binario usando pickle
            with open(self.bin_file, 'wb') as f:
                pickle.dump(personajes, f)
            
            print(f"✓ Exportados {len(personajes)} personajes a {self.bin_file}")
            return True
            
        except Exception as e:
            print(f"✗ Error al exportar a binario: {str(e)}")
            return False
    
    def importar_desde_binario(self) -> List[Dict]:
        """
        Importa personajes desde el archivo binario
        
        Returns:
            Lista de personajes o lista vacía si hay error
            
        Author: Xiker
        """
        try:
            with open(self.bin_file, 'rb') as f:
                personajes = pickle.load(f)
            
            print(f"✓ Importados {len(personajes)} personajes desde {self.bin_file}")
            return personajes
            
        except FileNotFoundError:
            print(f"✗ Error: No se encuentra el archivo {self.bin_file}")
            return []
        except Exception as e:
            print(f"✗ Error al importar desde binario: {str(e)}")
            return []
    
    def exportar_todo(self) -> bool:
        """
        Ejecuta todas las exportaciones en secuencia: CSV -> XML -> Binario
        
        Returns:
            True si todas las exportaciones fueron exitosas
            
        Author: Xiker
        """
        print("\n=== Iniciando exportación completa ===")
        
        csv_ok = self.exportar_a_csv()
        xml_ok = self.exportar_a_xml()
        bin_ok = self.exportar_a_binario()
        
        print("=== Exportación completa finalizada ===\n")
        
        return csv_ok and xml_ok and bin_ok
