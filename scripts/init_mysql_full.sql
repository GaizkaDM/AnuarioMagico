-- Script de Inicialización de Base de Datos MySQL
-- Uso: Ejecutar este script en el servidor MySQL para crear la base de datos y las tablas necesarias.
-- Se adapta al esquema utilizado por la aplicación Anuario Mágico.

-- 1. Crear Base de Datos
CREATE DATABASE IF NOT EXISTS hogwarts CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hogwarts;

-- 2. Tabla de Usuarios (users)
DROP TABLE IF EXISTS users;
CREATE TABLE IF NOT EXISTS users (
    username VARCHAR(255) PRIMARY KEY,
    password_hash VARCHAR(255),
    created_at VARCHAR(100)
);

-- 3. Tabla de Favoritos (favorites)
DROP TABLE IF EXISTS favorites;
CREATE TABLE IF NOT EXISTS favorites (
    character_id VARCHAR(255) PRIMARY KEY,
    is_favorite BOOLEAN DEFAULT FALSE,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 4. Tabla de Personajes (characters)
DROP TABLE IF EXISTS characters;
CREATE TABLE IF NOT EXISTS characters (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255),
    house VARCHAR(255),
    image TEXT,
    died TEXT,
    born TEXT,
    patronus TEXT,
    gender VARCHAR(50),
    species VARCHAR(100),
    blood_status VARCHAR(100),
    role TEXT,
    wiki TEXT,
    slug TEXT,
    image_blob MEDIUMBLOB,
    alias_names TEXT,
    animagus TEXT,
    boggart TEXT,
    eye_color TEXT,
    family_member TEXT,
    hair_color TEXT,
    height TEXT,
    jobs TEXT,
    nationality TEXT,
    romances TEXT,
    skin_color TEXT,
    titles TEXT,
    wand TEXT,
    weight TEXT,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Verificación final
SHOW TABLES;
DESCRIBE users;
DESCRIBE favorites;
DESCRIBE characters;
