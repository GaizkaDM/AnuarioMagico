package org.GaizkaFrost.services;

import org.GaizkaFrost.models.Personaje;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase de utilidad para interactuar con la API Backend.
 * Maneja solicitudes HTTP para autenticación, obtención de personajes y gestión
 * de favoritos.
 *
 * @author Gaizka
 * @author Xiker
 * @author Diego
 */
public class HarryPotterAPI {

    private static final Logger logger = LoggerFactory.getLogger(HarryPotterAPI.class);
    private static final String API_URL = "http://127.0.0.1:8000/characters";
    private static final String AUTH_URL = "http://127.0.0.1:8000/auth";

    // Token de sesión para autenticación
    private static String currentToken = null;
    private static String currentUsername = null;

    // ==========================================
    // GESTIÓN DE SESIÓN Y TOKEN
    // ==========================================

    /**
     * Establece el token de sesión actual.
     */
    public static void setToken(String token) {
        setToken(token, null);
    }

    /**
     * Establece el token y nombre de usuario de la sesión actual.
     */
    public static void setToken(String token, String username) {
        currentToken = token;
        currentUsername = username;
        logger.info("Token saved: {}",
                (token != null ? token.substring(0, Math.min(8, token.length())) + "..." : "null"));
        if (username != null) {
            logger.info("User: {}", username);
        }
    }

    /**
     * Obtiene el token de sesión actual.
     */
    public static String getToken() {
        return currentToken;
    }

    /**
     * Obtiene el nombre de usuario de la sesión actual.
     */
    public static String getUsername() {
        return currentUsername;
    }

    /**
     * Limpia el token de sesión.
     */
    public static void clearToken() {
        currentToken = null;
        currentUsername = null;
        logger.info("Token eliminated");
    }

    /**
     * Verifica si hay una sesión activa.
     */
    public static boolean isLoggedIn() {
        return currentToken != null && !currentToken.isEmpty();
    }

    // ==========================================
    // AUTENTICACIÓN
    // ==========================================

    /**
     * Realiza el inicio de sesión del usuario.
     */
    public static String login(String username, String password) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("username", username);
        json.addProperty("password", password);

        HttpURLConnection conn = createConnection(AUTH_URL + "/login", "POST");
        sendJson(conn, json);

        if (conn.getResponseCode() == 200) {
            String response = readResponse(conn);
            JsonObject res = new Gson().fromJson(response, JsonObject.class);
            return res.has("token") ? res.get("token").getAsString() : null;
        }
        return null;
    }

    /**
     * Registra un nuevo usuario en el sistema.
     */
    public static boolean register(String username, String password, String masterPassword) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("username", username);
        json.addProperty("password", password);
        json.addProperty("master_password", masterPassword);

        HttpURLConnection conn = createConnection(AUTH_URL + "/register", "POST");
        sendJson(conn, json);
        return conn.getResponseCode() == 200;
    }

    /**
     * Alterna el estado de favorito de un personaje.
     */
    public static boolean toggleFavorite(String characterId) throws Exception {
        HttpURLConnection conn = createConnection("http://localhost:8000/characters/" + characterId + "/favorite",
                "POST");
        return conn.getResponseCode() == 200;
    }

    /**
     * Sincroniza datos desde MySQL (Pull).
     */
    public static boolean syncPull() {
        return executeSyncRequest("http://localhost:8000/admin/sync-pull");
    }

    /**
     * Sincroniza datos hacia MySQL (Push).
     */
    public static boolean syncPush() {
        return executeSyncRequest("http://localhost:8000/admin/sync-mysql");
    }

    private static boolean executeSyncRequest(String url) {
        try {
            HttpURLConnection conn = createConnection(url, "POST");
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            logger.error("Sync Error ({}): {}", url, e.getMessage());
            return false;
        }
    }

    /**
     * Sincronización completa (Push + Pull).
     */
    public static boolean fullSync() {
        logger.info("Starting Full Sync...");
        boolean pushOk = syncPush();
        logger.info("Push status: {}", pushOk);
        boolean pullOk = syncPull();
        logger.info("Pull status: {}", pullOk);
        return pushOk && pullOk;
    }

    /**
     * Obtiene el estado de la sincronización de imágenes en segundo plano.
     */
    public static JsonObject getImageSyncStatus() {
        try {
            HttpURLConnection conn = createConnection("http://localhost:8000/admin/sync-images/status", "GET");
            if (conn.getResponseCode() == 200) {
                String response = readResponse(conn);
                return new Gson().fromJson(response, JsonObject.class);
            }
        } catch (Exception e) {
            logger.error("Error checking sync status: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Obtiene la lista de personajes.
     */
    public static List<Personaje> fetchCharacters() throws Exception {
        List<Personaje> personajes = new ArrayList<>();
        HttpURLConnection conn = createConnection(API_URL, "GET");
        conn.setConnectTimeout(120000); // 2 minutos para conexión inicial (DB fría)
        conn.setReadTimeout(120000); // 2 minutos para lectura

        if (conn.getResponseCode() == 200) {
            String response = readResponse(conn);
            JsonArray jsonArray = new Gson().fromJson(response, JsonArray.class);
            for (JsonElement element : jsonArray) {
                personajes.add(Personaje.fromJson(element.getAsJsonObject()));
            }
        }
        conn.disconnect();
        return personajes;
    }

    // ==========================================
    // CRUD DE PERSONAJES
    // ==========================================

    /**
     * Añade un nuevo personaje.
     * Requiere sesión iniciada (verificación en frontend).
     * 
     * @param personaje El personaje a añadir (como JsonObject).
     * @return El ID del personaje si se añadió correctamente, null en caso
     *         contrario.
     */
    public static String addCharacter(JsonObject personaje) throws Exception {
        if (!isLoggedIn()) {
            logger.error("Error: No session active");
            return null;
        }

        HttpURLConnection conn = createConnection(API_URL, "POST");
        sendJson(conn, personaje);

        int responseCode = conn.getResponseCode();
        if (responseCode == 201 || responseCode == 200) {
            String response = readResponse(conn);
            JsonObject res = new Gson().fromJson(response, JsonObject.class);
            conn.disconnect();
            return res.has("id") ? res.get("id").getAsString() : null;
        }
        conn.disconnect();
        return null;
    }

    /**
     * Sube una imagen al servidor para un personaje específico.
     * 
     * @param characterId El ID del personaje.
     * @param imageFile   El archivo de imagen a subir.
     * @return true si se subió correctamente.
     */
    public static boolean uploadImage(String characterId, java.io.File imageFile) throws Exception {
        if (!isLoggedIn())
            return false;

        String boundary = "---" + System.currentTimeMillis();
        String url = API_URL + "/" + characterId + "/upload-image";
        HttpURLConnection conn = createConnection(url, "POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setDoOutput(true);

        try (java.io.OutputStream os = conn.getOutputStream();
                java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.OutputStreamWriter(os, "UTF-8"),
                        true)) {

            writer.append("--" + boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"image\"; filename=\"" + imageFile.getName() + "\"")
                    .append("\r\n");
            writer.append("Content-Type: image/jpeg").append("\r\n");
            writer.append("\r\n").flush();

            try (java.io.FileInputStream fis = new java.io.FileInputStream(imageFile)) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, n);
                }
            }
            os.flush();
            writer.append("\r\n").flush();
            writer.append("--" + boundary + "--").append("\r\n").flush();
        }

        return conn.getResponseCode() == 200;
    }

    /**
     * Edita un personaje existente.
     * Requiere sesión iniciada (verificación en frontend).
     * 
     * @param characterId       El ID del personaje a editar.
     * @param datosActualizados Los datos a actualizar (como JsonObject).
     * @return true si se editó correctamente, false en caso contrario.
     */
    public static boolean editCharacter(String characterId, JsonObject datosActualizados) throws Exception {
        if (!isLoggedIn()) {
            logger.error("Error: No session active");
            return false;
        }

        String url = API_URL + "/" + characterId;
        HttpURLConnection conn = createConnection(url, "PUT");
        sendJson(conn, datosActualizados);

        int responseCode = conn.getResponseCode();
        conn.disconnect();
        return responseCode == 200;
    }

    /**
     * Elimina un personaje.
     * Requiere sesión iniciada (verificación en frontend).
     * 
     * @param characterId El ID del personaje a eliminar.
     * @return true si se eliminó correctamente, false en caso contrario.
     */
    public static boolean deleteCharacter(String characterId) throws Exception {
        if (!isLoggedIn()) {
            logger.error("Error: No session active");
            return false;
        }

        String url = API_URL + "/" + characterId;
        HttpURLConnection conn = createConnection(url, "DELETE");

        int responseCode = conn.getResponseCode();
        conn.disconnect();
        return responseCode == 200;
    }

    // ==========================================
    // MÉTODOS AUXILIARES PRIVADOS (HELPERS)
    // ==========================================

    private static HttpURLConnection createConnection(String urlStr, String method) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(10000); // Default 10s
        conn.setReadTimeout(10000); // Default 10s

        // Incluir token de autenticación si existe
        if (currentToken != null && !currentToken.isEmpty()) {
            conn.setRequestProperty("Authorization", currentToken);
        }

        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method)) {
            conn.setDoOutput(true);
        }
        return conn;
    }

    private static void sendJson(HttpURLConnection conn, JsonObject json) throws Exception {
        conn.setRequestProperty("Content-Type", "application/json");
        try (java.io.OutputStream os = conn.getOutputStream()) {
            byte[] input = json.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }
    }

    private static String readResponse(HttpURLConnection conn) throws Exception {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

}
