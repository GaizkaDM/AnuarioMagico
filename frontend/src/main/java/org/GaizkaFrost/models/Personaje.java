package org.GaizkaFrost.models;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de datos que representa un personaje de Harry Potter.
 * Se utiliza para mapear la respuesta JSON del backend y vincular datos en la interfaz.
 *
 * @author Xiker
 * @author Gaizka
 * @author Diego
 */
public class Personaje {

    // --- CAMPOS PRINCIPALES ---
    private int id;
    private String apiId;
    private String nombre;
    private String casa;
    private String estado; // "Vivo" / "Muerto" o equivalente
    private String patronus;
    private String imagenUrl; // image_small
    private String imagenFull; // image_full

    // --- DETALLES ---
    private String born;
    private String died;
    private String gender;
    private String species;
    private String animagus;
    private String nationality;
    private String alias;
    private String titles;
    private String wand;
    private String slug;
    private String eyeColor;
    private String hairColor;
    private String skinColor;
    private String height;
    private String weight;
    private String boggart;
    private String romances;
    private String family;
    private String jobs;

    // --- JSON original de la API ---
    private String rawJson;
    private boolean favorite;

    private String bloodStatus;
    private String role;
    private String wiki;

    // ==========================
    // CONSTRUCTORES
    // ==========================

    public Personaje() {
        // constructor vacío requerido por JavaFX
    }

    public Personaje(String nombre, String casa, String estado, String patronus, String imagenUrl) {
        this.nombre = nombre;
        this.casa = casa;
        this.estado = estado;
        this.patronus = patronus;
        this.imagenUrl = imagenUrl;
    }

    public Personaje(String apiId, String nombre, String casa, String estado, String patronus, String imagenUrl) {
        this.apiId = apiId;
        this.nombre = nombre;
        this.casa = casa;
        this.estado = estado;
        this.patronus = patronus;
        this.imagenUrl = imagenUrl;
    }

    // ==========================
    // GETTERS y SETTERS
    // ==========================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCasa() {
        return casa;
    }

    public void setCasa(String casa) {
        this.casa = casa;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getPatronus() {
        return patronus;
    }

    public void setPatronus(String patronus) {
        this.patronus = patronus;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public String getImagenFull() {
        return imagenFull;
    }

    public void setImagenFull(String imagenFull) {
        this.imagenFull = imagenFull;
    }

    public String getBorn() {
        return born;
    }

    public void setBorn(String born) {
        this.born = born;
    }

    public String getDied() {
        return died;
    }

    public void setDied(String died) {
        this.died = died;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getAnimagus() {
        return animagus;
    }

    public void setAnimagus(String animagus) {
        this.animagus = animagus;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getTitles() {
        return titles;
    }

    public void setTitles(String titles) {
        this.titles = titles;
    }

    public String getWand() {
        return wand;
    }

    public void setWand(String wand) {
        this.wand = wand;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    // Blood status
    public String getBloodStatus() {
        return bloodStatus;
    }

    public void setBloodStatus(String bloodStatus) {
        this.bloodStatus = bloodStatus;
    }

    // Role
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // Wiki
    public String getWiki() {
        return wiki;
    }

    public void setWiki(String wiki) {
        this.wiki = wiki;
    }

    // Favorite status
    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public String getEyeColor() {
        return eyeColor;
    }

    public void setEyeColor(String eyeColor) {
        this.eyeColor = eyeColor;
    }

    public String getHairColor() {
        return hairColor;
    }

    public void setHairColor(String hairColor) {
        this.hairColor = hairColor;
    }

    public String getSkinColor() {
        return skinColor;
    }

    public void setSkinColor(String skinColor) {
        this.skinColor = skinColor;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getBoggart() {
        return boggart;
    }

    public void setBoggart(String boggart) {
        this.boggart = boggart;
    }

    public String getRomances() {
        return romances;
    }

    public void setRomances(String romances) {
        this.romances = romances;
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public String getJobs() {
        return jobs;
    }

    public void setJobs(String jobs) {
        this.jobs = jobs;
    }

    // ==========================
    // FACTORY METHODS & UTILS
    // ==========================

    public static Personaje fromJson(JsonObject obj) {
        String id = getStringOrEmpty(obj, "id");
        String nombre = getStringOrEmpty(obj, "name");
        String casa = getStringOrEmpty(obj, "house");
        String died = getStringOrEmpty(obj, "died");
        String estado = (died != null && !died.isEmpty()) ? "Fallecido" : "Vivo";
        String patronus = getStringOrEmpty(obj, "patronus");
        String imagen = getStringOrEmpty(obj, "image");

        Personaje p = new Personaje(id, nombre, casa, estado, patronus, imagen);

        if (obj.has("is_favorite") && !obj.get("is_favorite").isJsonNull()) {
            p.setFavorite(obj.get("is_favorite").getAsBoolean());
        }

        p.setBorn(getStringOrEmpty(obj, "born"));
        p.setDied(died);
        p.setGender(getStringOrEmpty(obj, "gender"));
        p.setSpecies(getStringOrEmpty(obj, "species"));
        p.setBloodStatus(getStringOrEmpty(obj, "blood_status"));
        p.setRole(getStringOrEmpty(obj, "role"));
        p.setWiki(getStringOrEmpty(obj, "wiki"));

        p.setAlias(getListAsString(obj, "alias_names"));
        p.setTitles(getListAsString(obj, "titles"));
        p.setWand(getListAsString(obj, "wand"));
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

        return p;
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