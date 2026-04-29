package com.kasirpendem.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class AppConfig {
    private static final Properties PROPERTIES = new Properties();
    private static final Path CONFIG_FILE_PATH = Path.of("src/main/resources/app.properties");

    static {
        try (InputStream in = AppConfig.class.getResourceAsStream("/app.properties")) {
            if (in == null) {
                throw new IllegalStateException("File app.properties tidak ditemukan");
            }
            PROPERTIES.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Gagal membaca konfigurasi", e);
        }
    }

    private AppConfig() {
    }

    public static String get(String key) {
        return PROPERTIES.getProperty(key);
    }

    public static int getInt(String key) {
        return Integer.parseInt(PROPERTIES.getProperty(key));
    }

    public static synchronized void set(String key, String value) {
        PROPERTIES.setProperty(key, value);
    }

    public static synchronized void save() {
        try (OutputStream out = Files.newOutputStream(CONFIG_FILE_PATH)) {
            PROPERTIES.store(out, "Kasir Pendem app config");
        } catch (IOException e) {
            throw new RuntimeException("Gagal menyimpan konfigurasi", e);
        }
    }
}
