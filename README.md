# Harry Potter Anuario - Aplicación Completa

MVP de aplicación de escritorio para un Anuario de Harry Potter con arquitectura híbrida Python + JavaFX.

## 🎯 Características

- ✅ **Backend Python Flask** con persistencia SQLite para favoritos
- ✅ **Frontend JavaFX** con interfaz completa
- ✅ **Integración con PotterDB API** para datos actualizados
- ✅ **Sistema de favoritos persistente** - los favoritos se guardan localmente
- ✅ **Filtrado avanzado** por casa, estado (vivo/fallecido) y búsqueda por nombre
- ✅ **Vista detallada** de personajes con toda su información
- ✅ **Carga de imágenes** desde URLs de PotterDB

## 🏗️ Arquitectura

```
┌─────────────────┐         HTTP          ┌─────────────────┐
│                 │      localhost:8000    │                 │
│  JavaFX Client  │◄─────────────────────►│  Flask Backend  │
│   (Frontend)    │      JSON REST API     │    (Python)     │
│                 │                        │                 │
└─────────────────┘                        └────────┬────────┘
                                                    │
                                            ┌───────▼────────┐
                                            │  SQLite DB     │
                                            │  (Favorites)   │
                                            └────────────────┘
                                                    │
                                            ┌───────▼────────┐
                                            │  PotterDB API  │
                                            │  (External)    │
                                            └────────────────┘
```

## 📁 Estructura del Proyecto

```
PruebaAnuariio/
├── backend/
│   ├── app.py              # Servidor Flask (puerto 8000)
│   ├── requirements.txt    # Dependencias Python
│   └── favorites.db        # Base de datos SQLite (generada automáticamente)
├── frontend/
│   ├── pom.xml            # Configuración Maven
│   └── src/main/
│       ├── java/org/GaizkaFrost/
│       │   ├── App.java             # Aplicación principal JavaFX
│       │   ├── Lanzador.java        # Launcher para módulos
│       │   ├── Controlador.java     # Controlador vista principal
│       │   ├── DetailController.java # Controlador vista detalle
│       │   ├── Personaje.java       # Modelo de datos
│       │   └── HarryPotterAPI.java  # Cliente HTTP para backend
│       └── resources/
│           └── fxml/                # Archivos FXML de interfaz
├── .gitignore
└── README.md
```

## 🔌 API REST Backend

### Endpoints Disponibles

#### `GET /characters`
Obtiene todos los personajes filtrados de PotterDB.

**Respuesta:**
```json
[
  {
    "id": "harry-potter",
    "name": "Harry Potter",
    "house": "Gryffindor",
    "image": "https://...",
    "died": "",
    "born": "1980-07-31",
    "patronus": "Stag",
    "is_favorite": false,
    "gender": "Male",
    "species": "Human",
    "blood_status": "Half-blood",
    "role": "Student",
    "wiki": "https://..."
  }
]
```

**Filtros aplicados automáticamente:**
- Solo personajes con imagen
- Excluye nombres con: "Unidentified", "Unknown", "Student"

#### `POST /characters/<id>/favorite`
Marca/desmarca un personaje como favorito.

**Request Body:**
```json
{
  "is_favorite": true
}
```

**Respuesta:**
```json
{
  "success": true,
  "character_id": "harry-potter",
  "is_favorite": true
}
```

#### `GET /health`
Verifica el estado del servidor.

**Respuesta:**
```json
{
  "status": "ok",
  "message": "Backend is running on port 8000"
}
```

#### `GET /personajes` *(Legacy)*
Endpoint de compatibilidad - redirige a `/characters`.

## 📋 Requisitos

### Backend (Python)
- Python 3.8+
- pip (gestor de paquetes)

### Frontend (JavaFX)
- Java 11 o superior
- Maven 3.6+
- JavaFX 11 (incluido en las dependencias de Maven)

## 🚀 Instalación y Ejecución

### 1️⃣ Backend (Python)

```bash
# Navegar a la carpeta backend
cd backend

# Crear entorno virtual (recomendado)
python -m venv venv

# Activar entorno virtual
# Windows:
venv\Scripts\activate
# Linux/Mac:
source venv/bin/activate

# Instalar dependencias
pip install -r requirements.txt

# Ejecutar servidor
python app.py
```

**El backend estará disponible en:** `http://localhost:8000`

**Salida esperada:**
```
Initializing database...
Starting Harry Potter Yearbook Backend...
Backend will be available at: http://localhost:8000
Endpoints:
  - GET  /characters              : Get all filtered characters
  - POST /characters/<id>/favorite : Toggle favorite status
  - GET  /health                  : Health check
  - GET  /personajes              : Legacy endpoint (compatibility)
 * Running on http://0.0.0.0:8000
```

### 2️⃣ Frontend (JavaFX)

**En una nueva terminal/ventana:**

```bash
# Navegar a la carpeta frontend
cd frontend

# Compilar y ejecutar con Maven
mvn clean javafx:run
```

**Nota:** La primera vez, Maven descargará todas las dependencias (JavaFX, Gson, etc.), esto puede tardar unos minutos.

## 🎮 Uso de la Aplicación

### Vista Principal

1. **Cargar Personajes:** Click en "Cargar Personajes" para obtener datos del backend
2. **Buscar:** Escribe en el campo de búsqueda para filtrar por nombre
3. **Filtrar por Casa:** Usa el ComboBox de casas (Gryffindor, Slytherin, etc.)
4. **Filtrar por Estado:** Selecciona "Vivo" o "Fallecido"
5. **Limpiar Filtros:** Click en "Limpiar" para resetear todos los filtros
6. **Ver Detalles:** Selecciona un personaje y click en "Ver Detalles"
7. **Favoritos:** Click en la estrella para marcar/desmarcar favoritos (se guardan permanentemente)

### Vista Detallada

- Muestra toda la información del personaje (casa, fecha de nacimiento, patronus, etc.)
- Imagen grande del personaje
- Enlace a Wikipedia (si está disponible)
- Botón "Volver" para regresar a la vista principal

## 💾 Persistencia de Datos

Los **favoritos se guardan permanentemente** en una base de datos SQLite (`backend/favorites.db`).

- Al marcar/desmarcar un favorito, se guarda inmediatamente en la base de datos
- Los favoritos persisten entre reinicios de la aplicación
- Si eliminas `favorites.db`, se creará una nueva vacía al iniciar el backend

## 🔧 Solución de Problemas

### Backend no disponible
```bash
# Verifica que app.py esté ejecutándose
# Comprueba que el puerto 8000 no esté ocupado
netstat -ano | findstr :8000   # Windows
lsof -i :8000                   # Linux/Mac

# Revisa que todas las dependencias estén instaladas
pip install -r requirements.txt
```

### Error de compilación JavaFX
```bash
# Verifica Java 11 o superior
java -version

# Verifica Maven
mvn -version

# Limpia y recompila
mvn clean install
mvn javafx:run
```

### Frontend no conecta con Backend
1. Verifica que el backend esté corriendo en `http://localhost:8000`
2. Prueba el endpoint manualmente:
   ```bash
   curl http://localhost:8000/health
   curl http://localhost:8000/characters
   ```
3. Revisa la consola del frontend para errores de conexión

### Imágenes no cargan
- Verifica tu conexión a Internet
- Las imágenes se cargan desde URLs externas de PotterDB
- Si una imagen específica no carga, puede ser un problema con la URL en PotterDB

## 🧪 Pruebas Rápidas

### Probar Backend
```bash
# Health check
curl http://localhost:8000/health

# Obtener personajes
curl http://localhost:8000/characters

# Marcar favorito (requiere ID válido)
curl -X POST http://localhost:8000/characters/harry-potter/favorite \
  -H "Content-Type: application/json" \
  -d '{"is_favorite": true}'
```

### Probar Frontend
1. Ejecuta `mvn clean javafx:run`
2. Click en "Cargar Personajes"
3. Verifica que se carguen personajes con imágenes
4. Prueba los filtros y la búsqueda
5. Marca algunos favoritos (estrella)
6. Cierra y vuelve a abrir - los favoritos deben estar guardados

## 🔜 Próximos Pasos (Roadmap)

- [ ] Diseño visual estilo Hogwarts (colores de casas)
- [ ] Despliegue a producción (backend en servidor remoto)
- [ ] Exportación de personajes a PDF
- [ ] Sistema de anuarios personalizados
- [ ] Modo offline (caché local de personajes)
- [ ] Filtro por múltiples casas simultáneamente
- [ ] Vista de favoritos exclusiva
- [ ] Animaciones y transiciones mejoradas

## 📝 Tecnologías Utilizadas

### Backend
- **Flask** - Framework web Python
- **Flask-CORS** - Manejo de CORS para permitir conexiones desde JavaFX
- **Requests** - Cliente HTTP para consumir PotterDB API
- **SQLite3** - Base de datos ligera para favoritos

### Frontend
- **JavaFX 11** - Framework de interfaz gráfica
- **Gson** - Parseo de JSON
- **Maven** - Gestión de dependencias y build

### API Externa
- **PotterDB** - https://api.potterdb.com/ (datos de Harry Potter)

## 👨‍💻 Autor

Desarrollado como MVP para prueba de concepto de arquitectura híbrida Python + JavaFX.

---

## 📄 Licencia

Este proyecto es de código abierto y está disponible bajo la licencia MIT.
