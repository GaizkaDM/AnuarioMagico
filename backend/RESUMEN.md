# Sistema de Gestión de Personajes

## 📁 Archivos Creados

### 1. **sync_sqlite.py**
Capa de acceso a datos para SQLite con los siguientes métodos:
- `añadir_personaje(personaje)` - Inserta un nuevo personaje en la BD
- `editar_personaje(personaje_id, datos_actualizados)` - Actualiza un personaje existente
- `eliminar_personaje(personaje_id)` - Elimina un personaje
- `obtener_personaje(personaje_id)` - Consulta un personaje por ID
- `obtener_todos_personajes()` - Lista todos los personajes

### 2. **ExportService.py**
Servicio de exportación con los siguientes métodos:
- `exportar_a_csv()` - Exporta todos los personajes a CSV
- `exportar_a_xml()` - Exporta todos los personajes a XML
- `exportar_a_binario()` - Exporta usando el XML como fuente a formato binario (pickle)

### 3. **PersonajeService.py**
Servicio de negocio que orquesta las operaciones. Cada método ejecuta en este orden:
1. Actualiza SQLite
2. Exporta a CSV
3. Exporta a XML
4. Exporta a Binario (desde XML)

**Métodos principales:**
- `añadir_personaje(personaje)` - Añade y exporta
- `editar_personaje(personaje_id, datos_actualizados)` - Edita y exporta
- `eliminar_personaje(personaje_id)` - Elimina y exporta

## 🔄 Flujo de Ejecución

```
PersonajeService.añadir_personaje()
    ├─► sync_sqlite.añadir_personaje()        [1/4]
    ├─► ExportService.exportar_a_csv()      [2/4]
    ├─► ExportService.exportar_a_xml()      [3/4]
    └─► ExportService.exportar_a_binario()  [4/4]
```

## 💻 Ejemplo de Uso

```python
from PersonajeService import PersonajeService

# Crear servicio
servicio = PersonajeService()

# Añadir personaje
personaje = {
    'id': 'harry-potter',
    'name': 'Harry Potter',
    'house': 'Gryffindor',
    'patronus': 'Ciervo'
}
servicio.añadir_personaje(personaje)

# Editar personaje
servicio.editar_personaje('harry-potter', {'house': 'Slytherin'})

# Eliminar personaje
servicio.eliminar_personaje('harry-potter')
```

## 📊 Archivos Generados

Cada operación genera automáticamente:
- `personajes.csv` - Formato CSV
- `personajes.xml` - Formato XML
- `personajes.bin` - Formato binario (generado desde XML)
