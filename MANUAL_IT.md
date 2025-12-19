# Manual Técnico - Anuario Hogwarts (IT & DevOps)

Este documento detalla la arquitectura, el proceso de despliegue, la configuración y el mantenimiento de la aplicación **Anuario Hogwarts**.

## 1. Arquitectura del Sistema

La aplicación sigue una arquitectura híbrida **Frontend-Backend Desacoplado** corriendo localmente en la máquina del cliente (Desktop Application).

### Componentes
1.  **Frontend (JavaFX)**:
    *   Interfaz Gráfica de Usuario.
    *   Actúa como "Lanzador" del backend.
    *   Se comunica con el backend vía HTTP REST (localhost:8000).
    *   Gestión de procesos: Inicia el subproceso Python al arrancar y lo mata (`taskkill`) al cerrar.

2.  **Backend (Python/Flask)**:
    *   Empaquetado como ejecutable independiente (`backend_server.exe`) usando PyInstaller.
    *   Expone una API REST en el puerto `8000`.
    *   Maneja la lógica de negocio, persistencia local y sincronización remota.

3.  **Capa de Datos (Offline-First)**:
    *   **Local (SQLite)**: Archivo `data/anuario.db`. Almacena personajes, imágenes (BLOBs) y usuarios. Permite funcionamiento sin internet.
    *   **Remota (MySQL)**: Base de datos centralizada para sincronización entre equipos.
    *   **Fuente Externa**: API pública `PotterDB` (se usa como fallback si la BD local está vacía y no hay MySQL).

---

## 2. Requisitos del Sistema (Cliente)

Para ejecutar la aplicación en los equipos finales:

*   **Sistema Operativo**: Windows 10/11 (x64).
*   **Java Runtime**: JRE 17 o superior instalado y configurado en el PATH.
*   **Red**: Permisos para conectar a `localhost:8000` (interno) y salida a Internet (para PotterDB) o Intranet (para MySQL).
*   **No requiere Python**: El backend es autocontenido.

---

## 3. Guía de Compilación (Build)

Si necesitas regenerar los ejecutables desde el código fuente.

### 3.1. Compilar Backend (Python)
Requisitos: Python 3.10+, `pip`, `virtualenv`.

1.  Crear entorno virtual:
    ```powershell
    python -m venv .venv
    ```
2.  Activar entorno virtual:
    ```powershell
    .venv\Scripts\activate
    ```
3.  Instalar dependencias:
    ```powershell
    pip install -r backend/requirements.txt
    ```
4.  Generar ejecutable (usando `.spec`):
    ```powershell
    pyinstaller --clean --noconfirm backend_server.spec
    ```
    *Resultado*: Genera `dist/backend_server.exe`.

### 3.2. Compilar Frontend (Java)
Requisitos: Maven 3.8+, JDK 21.

1.  Limpiar y empaquetar:
    ```powershell
    mvn clean package
    ```
    *Resultado*: Genera `target/AnuarioMagico-1.0-SNAPSHOT.jar` (y carpetas de librerías si no es FatJAR).

---

## 4. Despliegue y Estructura de Directorios

Para entregar la aplicación a producción, crea una carpeta contenedora con la siguiente estructura **obligatoria**:

```text
/AnuarioApp/
├── AnuarioMagico.jar         # El frontend compilado
├── backend_server.exe        # El backend compilado (copiar desde dist/)
├── .env                      # Archivo de configuración (Ver sección 5)
└── data/                     # Carpeta para la BD local (se crea sola si no existe)
```

**Nota Importante**: El `backend_server.exe` debe estar en la **misma carpeta** que el JAR o en una subcarpeta `dist/` relativa al JAR. El lanzador buscará en ambas ubicaciones.

---

## 5. Configuración (.env)

El archivo `.env` es crítico para definir la conexión a la base de datos central MySQL.

**Ejemplo de archivo `.env`:**

```ini
# Configuración MySQL Remoto
DB_HOST=192.168.39.6      # IP del servidor MySQL
DB_PORT=3306
DB_NAME=AnuarioMagico
DB_USER=usuario_remoto
DB_PASSWORD=contraseña_segura

# (Opcional) Master Password para creación de usuarios locales
MASTER_PASSWORD=HogwartsMaster
```

> **Modo Offline**: Si no se incluye el archivo `.env`, la aplicación funcionará en modo "Isla", usando solo SQLite y descargando datos de PotterDB.

---

## 6. Resolución de Problemas (Troubleshooting)

### A. La aplicación no carga datos (Lista vacía)
1.  Verificar si existe conexión a MySQL (Revisar logs en consola).
2.  Verificar si la descarga inicial de PotterDB está en curso (puede tardar 1 minuto la primera vez).
3.  Revisar archivo `logs/backend.log` (si está habilitado) o la salida de consola.

### B. Procesos "backend_server.exe" huérfanos
*   El lanzador Java intenta matar los procesos al cerrar, pero si el equipo se apaga forzosamente, pueden quedar vivos.
*   **Solución**: El `Lanzador.java` incluye una rutina de limpieza al inicio. Simplemente **vuelve a abrir la aplicación** y esta matará cualquier instancia antigua automáticamente antes de iniciar una nueva.

### C. Error "Port 8000 already in use"
*   Indica que el backend anterior no se cerró o hay otra aplicación usando el puerto.
*   **Solución**: Abrir Administrador de Tareas -> Detalles -> Finalizar tarea `backend_server.exe` o reiniciar la app (que ejecutará el limpieza).

### D. No se ven las imágenes
*   Las imágenes se guardan como BLOB en SQLite.
*   Si la base de datos MySQL remota no tiene los BLOBs sincronizados, al hacer "Pull" pueden faltar imágenes.
*   Asegurar hacer "Push" desde una máquina con las imágenes completas primero.

---

## 7. Scripts de Base de Datos

### Inicialización MySQL (Servidor)
Ejecutar el siguiente script en el servidor MySQL para preparar el entorno (aunque el backend intenta crearlas automáticamente):

```sql
CREATE DATABASE IF NOT EXISTS AnuarioMagico CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE AnuarioMagico;
-- Las tablas se crearán automáticamente con SQLAlchemy/Pymysql al primer "Sync".
```

---

## 8. Trazabilidad del Dato Mágico

Este informe detalla la travesía de la información desde los mundos externos hasta su cristalización en múltiples formatos dentro del "Anuario Mágico".

### 8.1. Consumo de la API Potter (Extracción Mágica Inicial)

*   **¿Cómo realizasteis el conjuro de invocación?** 
    La invocación se realizó mediante arcanos `HTTP GET` utilizando la librería `requests` de Python. Como "potenciador" principal, utilizamos **Postman**. Este grimorio nos ofreció la ventaja de previsualizar las estructuras JSON antes de programar una sola línea de código, permitiéndonos probar la autenticación y los filtros de forma instantánea.
*   **Esencia de la información:** 
    La API devuelve un flujo de "esencias" en formato JSON (objetos con atributos como `name`, `house`, `patronus`, etc.). Antes de transformarlos, realizamos un **filtrado selectivo**: descartamos a todos los personajes sin imagen (pues un anuario sin rostro no es mágico) y filtramos personajes "genéricos" (como "unidentified student" o "boy") mediante una lista de palabras prohibidas, asegurando que solo los magos con nombre propio entraran en nuestro castillo.
*   **Criaturas y problemas:** 
    Nos enfrentamos a la **Paginación Rebelde** (el flujo no llega todo de golpe). Lo solucionamos con un bucle `while` que consultaba los metadatos de la API para saber cuántas páginas quedaban. También lidiamos con **atributos vacíos**, aplicando un hechizo de normalización para que los valores `null` se convirtieran en cadenas vacías o valores por defecto.

### 8.2. CSV: El Formato Plano de los Magos Archivistas

*   **Ventajas:** 
    Su sencillez es su mayor poder. Es universal, se puede leer con cualquier pergamino (Excel, Notepad) y es extremadamente ligero para intercambios rápidos.
*   **Búsquedas y actualizaciones:** 
    Si bien es adecuado para lecturas secuenciales, el CSV flaquea en actualizaciones selectivas (hay que reescribir gran parte del archivo). No obstante, para búsquedas sencillas por columnas, cumple su cometido de archivero.
*   **Organización y Purificación:** 
    Las columnas se organizaron de forma estricta (ID, Nombre, Casa, etc.). Los datos complejos (listas como `alias_names` o `jobs`) fueron purificados convirtiéndolos en cadenas de texto formateadas como JSON dentro de una sola celda, evitando así romper la estructura plana del CSV.
*   **Limitaciones:** 
    La mayor limitación fue la representación de **artefactos visuales**. El CSV no puede contener imágenes; solo pudimos guardar una referencia textual (`[BINARY_DATA]`) indicando que el objeto existía pero no podía ser visto en ese plano.

### 8.3. XML: El Grimorio Jerárquico

*   **¿Por qué XML?** 
    Porque el mundo mágico no es plano, es jerárquico. Mientras el CSV aplana todo, el XML permite representar la "familia" o las "varitas" como sub-elementos naturales.
*   **Ventajas de estructura:** 
    Ofrece una legibilidad superior para datos anidados. Permite definir esquemas donde un `<personaje>` contiene etiquetas propias para cada atributo, facilitando que otros sistemas entiendan la estructura sin confusiones.
*   **Etiquetas y Atributos:** 
    Decidimos usar la etiqueta raíz `<personajes>` y elementos `<personaje>`. Para los campos que son listas (como títulos), empleamos sub-etiquetas `<item>`, encapsulando cada unidad de información en su propio nivel jerárquico.
*   **Incoherencias:** 
    Se resolvieron estableciendo el XML como el puente de validación: todo dato que pasara del CSV al XML debía seguir un esquema de etiquetas rígido antes de ser cristalizado en binario.

### 8.4. Binarios Serializables: Cristalización Mágica de Objetos

*   **Utilidad:** 
    La serialización (usando el módulo `pickle`) permite "congelar" un objeto de Python exactamente como está en memoria y guardarlo en un archivo `.bin`.
*   **Preservación:** 
    A diferencia de CSV o XML, el binario preserva la **naturaleza del objeto**. Si un dato es una lista o un diccionario complejo, al recuperarlo ("deserializarlo") vuelve a ser el mismo objeto listo para usar, sin necesidad de parsear texto.
*   **Rendimiento:** 
    Es el hechizo más rápido. La carga desde un archivo binario es casi instantánea comparada con el tiempo que tarda el sistema en interpretar y validar un archivo XML o CSV.
*   **Dificultades:** 
    Su mayor problema es la **opacidad**. Un mago no puede leer un archivo binario a simple vista. Además, para actualizar un solo personaje, hemos de "descongelar" todo el archivo, modificarlo y volver a "congelarlo" por completo.

### 8.5. Imágenes como BLOBs: Artefactos Visuales Encantados

*   **Gestión de Descargas:** 
    Utilizamos el servicio `ImageService` para invocar a las URLs de las imágenes. Procesamos cada pergamino visual usando la librería `PIL` (Pillow) para normalizar su tamaño y formato (JPEG) antes de guardarlo.
*   **¿Por qué BLOBs?** 
    Guardar las imágenes dentro de la base de datos (Binary Large Objects) garantiza que el anuario sea **autosuficiente**. Si el servidor de imágenes original desaparece, nuestro anuario conserva el recuerdo visual para siempre.
*   **Portabilidad:** 
    El uso de BLOBs permite que el proyecto sea portable entre mundos: basta con mover el archivo de la base de datos para que todas las imágenes viajen con él, sin preocuparse por rutas de archivos externas rotas.
*   **Verificación:** 
    Se verificó mediante rutas específicas de nuestra API que recuperan el BLOB y lo sirven al frontend con el "mimetype" correcto (`image/jpeg`), permitiendo que la interfaz JavaFX las pinte en pantalla sin errores.

### 8.6. Arquitectura General (Mapa del Castillo de Datos)

*   **Flujo de Datos:** 
    `API Externa (PotterDB) -> Flask Backend (Transformación) -> SQLite (Persistencia) -> ExportService (CSV/XML/Bin) -> JavaFX UI`.
*   **Capas:** 
    Las capas se comunican mediante un modelo de servicios. El **PersonajeService** actúa como el Gran Comedor, coordinando al **DaoSQLite** (bodegas de datos) y al **ExportService** (sala de copias).
*   **Integridad:** 
    Garantizada por una cadena de exportación automática: cada vez que se añade o edita un personaje, se dispara un evento que actualiza en cascada el CSV, el XML y el Binario.
*   **Interfaz de Usuario:** 
    Diseñada en JavaFX buscando una experiencia inmersiva, con transiciones suaves y una galería visual que rinde homenaje a los cuadros de Hogwarts.
*   **Patrones empleados:**
    *   **DAO (Data Access Object):** Para separar la lógica de conexión a la base de datos de la lógica de negocio.
    *   **Service Pattern:** Encapsulando la lógica de exportación e imágenes en clases especializadas.
    *   **MVC (Modelo-Vista-Controlador):** Separando los datos (Models) de la lógica del servidor (Flask/Controllers) y la UI (JavaFX/Fxml).

#### Plano del Castillo de Datos (Diagrama Conceptual)

```text
[ API EXTERNA: PotterDB ]
           |
           v
[ FLASK BACKEND: Puertas del Castillo ] <---- [ POSTMAN: El Grimorio de Pruebas ]
           |
    +------+------+
    |             |
[ SQLite/MySQL: [ SERVICIOS: Salas de Hechizos ]
  Mazmorras ]         | (Exporta a...)
    |           +-----+-----+-----+
    |           |     |     |     |
    v           v     v     v     v
[ UI: Torre ] [CSV] [XML] [BIN] [BLOBs]
```
