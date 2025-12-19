package org.GaizkaFrost;

import java.io.File;
import java.io.IOException;

/**
 * Clase lanzadora para la aplicación.
 * Necesaria para crear un JAR ejecutable con dependencias de JavaFX
 * correctamente.
 *
 * @author Gaizka
 * @author Diego
 * @author Xiker
 */
public class Lanzador {

    private static Process backendProcess;

    /**
     * Método principal que delega la ejecución a App.main.
     *
     * @param args Argumentos de la línea de comandos.
     */
    public static void main(String[] args) {
        startBackend();
        App.main(args);
    }

    private static void startBackend() {
        try {
            // LIMPIEZA PREVIA: Matar cualquier instancia huérfana anterior
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                try {
                    new ProcessBuilder("taskkill", "/F", "/IM", "backend_server.exe").start().waitFor();
                    System.out.println("Limpieza: Procesos antiguos cerrados.");
                } catch (Exception ignored) {
                }
            }

            String[] possiblePaths = {
                    "backend_server.exe",
                    "dist/backend_server.exe",
                    "backend/dist/backend_server.exe"
            };

            File backendExe = null;
            for (String path : possiblePaths) {
                File f = new File(path);
                if (f.exists() && !f.isDirectory()) {
                    backendExe = f;
                    break;
                }
            }

            if (backendExe != null) {
                System.out.println("Iniciando backend desde: " + backendExe.getAbsolutePath());
                ProcessBuilder pb = new ProcessBuilder(backendExe.getAbsolutePath());
                pb.inheritIO(); // Para ver logs en consola si se ejecuta desde consola
                backendProcess = pb.start();

                // Hook para cerrar el backend cuando se cierre Java
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    if (backendProcess != null && backendProcess.isAlive()) {
                        System.out.println("Cerrando backend...");
                        try {
                            // Intentar matar el árbol de procesos en Windows (Backend + Subprocesos)
                            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                                long pid = backendProcess.pid();
                                System.out.println("Matando proceso backend PID: " + pid);
                                Runtime.getRuntime().exec("taskkill /F /T /PID " + pid);
                            }
                            // Fallback estándar (kill -9 o SIGKILL)
                            backendProcess.destroyForcibly();
                        } catch (Exception e) {
                            e.printStackTrace();
                            backendProcess.destroyForcibly();
                        }
                    }
                }));

                // Dar un momento para que inicie
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                System.out.println("ADVERTENCIA: No se encontró backend_server.exe. " +
                        "Asegúrate de ejecutar la app Python manualmente o colocar el exe en la misma carpeta.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}