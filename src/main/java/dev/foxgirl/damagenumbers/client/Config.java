package dev.foxgirl.damagenumbers.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import dev.foxgirl.damagenumbers.DamageNumbers;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {

    public boolean isEnabled = true;
    public boolean isPlayerDamageShown = false;
    public Color colorSm = new Color(0xFF787878); // gray
    public Color colorMd = new Color(0xFFFF7878); // red
    public Color colorLg = new Color(0xFFFF3C3C); // bright red

    public void readConfig(@NotNull PathProvider provider) {
        Path path = provider.getConfigFilePath();
        if (!Files.exists(path)) {
            writeConfig(provider);
            return;
        }

        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            var gson = new Gson();
            var config = gson.fromJson(reader, Config.class);
            if (config != null) {
                isEnabled = config.isEnabled;
                isPlayerDamageShown = config.isPlayerDamageShown;
                colorSm = config.colorSm;
                colorMd = config.colorMd;
                colorLg = config.colorLg;
            }
        } catch (JsonSyntaxException | IOException e) {
            DamageNumbers.LOGGER.error("Failed to read config file", e);
            backupAndResetConfig(provider);
        }
    }

    public void writeConfig(@NotNull PathProvider provider) {
        Path path = provider.getConfigTempPath();
        try {
            Files.createDirectories(path.getParent());

            try (var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                var gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(this, writer);
            }

            Files.move(path, provider.getConfigFilePath());
        } catch (IOException e) {
            DamageNumbers.LOGGER.error("Failed to write config file", e);
        }
    }

    private void backupAndResetConfig(@NotNull PathProvider provider) {
        Path originalPath = provider.getConfigFilePath();
        Path backupPath = Path.of(originalPath.toString() + ".backup");

        try {
            Files.move(originalPath, backupPath);
            DamageNumbers.LOGGER.info("Backed up corrupted config file to {}", backupPath);
            writeConfig(provider);
        } catch (IOException e) {
            DamageNumbers.LOGGER.error("Failed to backup and reset config", e);
        }
    }

    public interface PathProvider {
        @NotNull Path getConfigFilePath();
        @NotNull Path getConfigTempPath();
    }
}