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

1.  Activar entorno virtual:
    ```powershell
    .venv\Scripts\activate
    ```
2.  Instalar dependencias:
    ```powershell
    pip install -r backend/requirements.txt
    ```
3.  Generar ejecutable (usando `.spec`):
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
