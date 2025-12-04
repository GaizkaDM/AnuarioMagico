package org.GaizkaFrost;

/**
 * Modelo completo que representa un personaje según tu SQL y tu interfaz.
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
}
