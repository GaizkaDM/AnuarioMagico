package org.GaizkaFrost;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class HarryPotterAPI {

    private static final String API_URL = "http://localhost:8000/characters";

    public static boolean toggleFavorite(String characterId) throws Exception {
        URL url = new URL("http://localhost:8000/characters/" + characterId + "/favorite");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(5000);

        int responseCode = conn.getResponseCode();
        conn.disconnect();

        return responseCode == 200;
    }

    public static List<Personaje> fetchCharacters() throws Exception {
        List<Personaje> personajes = new ArrayList<>();

        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(120000); // 2 minutes for connection
        conn.setReadTimeout(120000); // 2 minutes for reading data

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Parse JSON
            Gson gson = new Gson();
            JsonArray jsonArray = gson.fromJson(response.toString(), JsonArray.class);

            for (JsonElement element : jsonArray) {
                JsonObject obj = element.getAsJsonObject();

                String id = getStringOrEmpty(obj, "id");
                String nombre = getStringOrEmpty(obj, "name");
                String casa = getStringOrEmpty(obj, "house");
                // If "died" field exists and has value, character is dead
                String died = getStringOrEmpty(obj, "died");
                String estado = (died != null && !died.isEmpty()) ? "Fallecido" : "Vivo";
                String patronus = getStringOrEmpty(obj, "patronus");
                String imagen = getStringOrEmpty(obj, "image");

                Personaje p = new Personaje(id, nombre, casa, estado, patronus, imagen);

                // Set favorite status
                if (obj.has("is_favorite") && !obj.get("is_favorite").isJsonNull()) {
                    p.setFavorite(obj.get("is_favorite").getAsBoolean());
                }

                // Campos adicionales
                p.setBorn(getStringOrEmpty(obj, "born"));
                p.setDied(died);
                p.setGender(getStringOrEmpty(obj, "gender"));
                p.setSpecies(getStringOrEmpty(obj, "species"));
                p.setBloodStatus(getStringOrEmpty(obj, "blood_status"));
                p.setRole(getStringOrEmpty(obj, "role"));
                p.setWiki(getStringOrEmpty(obj, "wiki"));

                // Nuevos campos
                p.setAlias(getListAsString(obj, "alias_names"));
                p.setTitles(getListAsString(obj, "titles"));
                p.setWand(getListAsString(obj, "wand")); // sometimes "wands" in api, check backend: app.py uses 'wand':
                                                         // attributes.get('wands', [])
                p.setRomances(getListAsString(obj, "romances"));
                p.setFamily(getListAsString(obj, "family_member"));
                p.setJobs(getListAsString(obj, "jobs"));

                p.setAnimagus(getStringOrEmpty(obj, "animagus"));
                p.setBoggart(getStringOrEmpty(obj, "boggart"));
                p.setEyeColor(getStringOrEmpty(obj, "eye_color"));
                p.setHairColor(getStringOrEmpty(obj, "hair_color"));
                p.setSkinColor(getStringOrEmpty(obj, "skin_color"));
                p.setHeight(getStringOrEmpty(obj, "height"));
                p.setWeight(getStringOrEmpty(obj, "weight"));
                p.setNationality(getStringOrEmpty(obj, "nationality"));

                personajes.add(p);
            }
        }

        conn.disconnect();
        return personajes;
    }

    private static String getStringOrEmpty(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return "";
    }

    private static String getListAsString(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            JsonElement el = obj.get(key);
            if (el.isJsonArray()) {
                JsonArray arr = el.getAsJsonArray();
                List<String> items = new ArrayList<>();
                for (JsonElement e : arr) {
                    items.add(e.getAsString());
                }
                return String.join(", ", items);
            } else if (el.isJsonPrimitive()) {
                return el.getAsString();
            }
        }
        return "";
    }
}
