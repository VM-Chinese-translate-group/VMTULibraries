package top.vmctcn.vmtu.libraries.modpack.info.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import top.vmctcn.vmtu.libraries.common.CommonContexts;
import top.vmctcn.vmtu.libraries.common.LogMarkers;
import top.vmctcn.vmtu.libraries.modpack.info.impl.DefaultModpackInfo;
import top.vmctcn.vmtu.libraries.modpack.info.impl.ModpackInfoJson;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ModpackInfoHelper {
    public static final String PATCHPACK_INFO_FILE = "patchpackinfo.json";
    public static final String LEGACY_MODPACK_INFO_FILE = "modpackinfo.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModpackInfo modpackInfo;
    private static Path modpackInfoPath;

    private ModpackInfoHelper() {}

    /**
     * Reads patchpackinfo.json from the instance root, falling back to the
     * deprecated modpackinfo.json only when the new file does not exist.
     */
    public static void readModpackInfo(boolean generateExampleModpackInfo) {
        Path gameDir = CommonContexts.getGameInfo().getGameDir();
        Path patchpackInfoPath = gameDir.resolve(PATCHPACK_INFO_FILE);
        Path legacyModpackInfoPath = gameDir.resolve(LEGACY_MODPACK_INFO_FILE);
        Path existingPath = Files.exists(patchpackInfoPath)
                ? patchpackInfoPath
                : (Files.exists(legacyModpackInfoPath) ? legacyModpackInfoPath : null);

        if (existingPath == null) {
            modpackInfo = null;
            modpackInfoPath = null;
            if (generateExampleModpackInfo) {
                CommonContexts.LOGGER.warn(LogMarkers.MODPACK,
                        "Neither {} nor {} exists, generating default {}.",
                        PATCHPACK_INFO_FILE, LEGACY_MODPACK_INFO_FILE, PATCHPACK_INFO_FILE);
                generateDefaultModpackInfo(patchpackInfoPath);
            } else {
                CommonContexts.LOGGER.warn(LogMarkers.MODPACK,
                        "Neither {} nor {} exists, skip it.", PATCHPACK_INFO_FILE, LEGACY_MODPACK_INFO_FILE);
            }
            return;
        }

        try {
            modpackInfo = readModpackInfo(existingPath);
            modpackInfoPath = existingPath;
        } catch (Exception e) {
            modpackInfo = null;
            modpackInfoPath = null;
            if (generateExampleModpackInfo) {
                CommonContexts.LOGGER.warn(LogMarkers.MODPACK,
                        "Error reading {}, generating default {}.",
                        existingPath.getFileName(), PATCHPACK_INFO_FILE, e);
                generateDefaultModpackInfo(patchpackInfoPath);
            } else {
                CommonContexts.LOGGER.warn(LogMarkers.MODPACK,
                        "Error reading {}, skip it.", existingPath.getFileName(), e);
            }
        }
    }

    /** Reads either schema from an arbitrary file, including an extracted ZIP entry. */
    public static ModpackInfo readModpackInfo(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parseModpackInfo(reader);
        }
    }

    /** Parses either schema through the same public API. */
    public static ModpackInfo parseModpackInfo(Reader reader) {
        return ModpackInfoJson.read(GSON, reader);
    }

    /** Writes only the current flat patchpackinfo.json schema. */
    public static void writeModpackInfo(Path path, ModpackInfo info) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            GSON.toJson(info, writer);
        }
    }

    /**
     * Updates the exact modpack version and always writes patchpackinfo.json.
     * When legacy metadata was loaded this performs a non-destructive migration;
     * the old file is left untouched.
     */
    public static void syncModpackVersion(String newVersion) {
        if (newVersion == null || newVersion.isEmpty()) return;
        if (modpackInfo == null) {
            CommonContexts.LOGGER.error(LogMarkers.MODPACK, "Cannot update modpack version: modpackInfo is null");
            return;
        }

        String oldVersion = modpackInfo.getModpackVersion();
        modpackInfo.setModpackVersion(newVersion);
        Path outputPath = CommonContexts.getGameInfo().getGameDir().resolve(PATCHPACK_INFO_FILE);
        try {
            writeModpackInfo(outputPath, modpackInfo);
            modpackInfoPath = outputPath;
            CommonContexts.LOGGER.info(LogMarkers.MODPACK,
                    "Modpack version updated from {} to {} in {}", oldVersion, newVersion, PATCHPACK_INFO_FILE);
        } catch (IOException e) {
            CommonContexts.LOGGER.error(LogMarkers.MODPACK, "Failed to update modpack version to {}", newVersion, e);
        }
    }

    private static void generateDefaultModpackInfo(Path patchpackInfoPath) {
        modpackInfo = new DefaultModpackInfo();
        try {
            writeModpackInfo(patchpackInfoPath, modpackInfo);
            modpackInfoPath = patchpackInfoPath;
            CommonContexts.LOGGER.info(LogMarkers.MODPACK, "Default {} generated.", PATCHPACK_INFO_FILE);
        } catch (IOException e) {
            CommonContexts.LOGGER.error(LogMarkers.MODPACK, "Failed to generate default {}", PATCHPACK_INFO_FILE, e);
        }
    }

    public static ModpackInfo getModpackInfo() {
        return modpackInfo == null ? new DefaultModpackInfo() : modpackInfo;
    }

    public static Path getModpackInfoPath() {
        return modpackInfoPath;
    }

    public static boolean isExampleModpackInfo() {
        return "example".equals(getModpackInfo().getId());
    }
}
