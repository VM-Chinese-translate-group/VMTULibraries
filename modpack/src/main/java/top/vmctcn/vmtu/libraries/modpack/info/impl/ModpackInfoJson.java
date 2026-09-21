package top.vmctcn.vmtu.libraries.modpack.info.impl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import top.vmctcn.vmtu.libraries.modpack.info.api.ModpackInfo;

import java.io.Reader;
import java.util.Collections;

/** JSON compatibility layer for the current and legacy information files. */
public final class ModpackInfoJson {
    private ModpackInfoJson() {}

    public static ModpackInfo read(Gson gson, Reader reader) {
        JsonElement rootElement = gson.fromJson(reader, JsonElement.class);
        if (rootElement == null || !rootElement.isJsonObject()) {
            throw new JsonParseException("Patch pack information must be a JSON object");
        }
        JsonObject root = rootElement.getAsJsonObject();
        if (root.has("modpack")) return readLegacy(gson, root);

        ModpackInfo result = gson.fromJson(root, ModpackInfo.class);
        // Accept the explicit spelling used by some early schema drafts.
        if (result.version == null && root.has("translationVersion")) {
            result.version = string(root, "translationVersion");
        }
        validateCurrent(root, result);
        result.getModpack();
        return result;
    }

    private static ModpackInfo readLegacy(Gson gson, JsonObject root) {
        LegacyInfo legacy = gson.fromJson(root, LegacyInfo.class);
        if (legacy == null || legacy.modpack == null) {
            throw new JsonParseException("Legacy modpackinfo.json has no modpack object");
        }
        ModpackInfo result = new ModpackInfo();
        result.name = legacy.modpack.name;
        result.modpackVersion = legacy.modpack.version;
        result.modpackVersionRange = exactRange(legacy.modpack.version);
        if (legacy.modpack.translation != null) {
            ModpackInfo.Translation translation = legacy.modpack.translation;
            result.id = translation.id;
            result.url = translation.url;
            result.language = translation.language;
            result.version = translation.version;
        }
        result.authors = Collections.emptyList();
        result.getModpack();
        return result;
    }

    private static void validateCurrent(JsonObject root, ModpackInfo info) {
        if (!root.has("formatVersion") || root.get("formatVersion").isJsonNull()) {
            throw new JsonParseException("patchpackinfo.json is missing required field: formatVersion");
        }
        if (info.formatVersion != 1) {
            throw new JsonParseException("Unsupported patchpackinfo formatVersion: " + info.formatVersion);
        }
        if (isBlank(info.name)) {
            throw new JsonParseException("patchpackinfo.json is missing required field: name");
        }
        if (isBlank(info.modpackVersionRange)) {
            throw new JsonParseException("patchpackinfo.json is missing required field: modpackVersionRange");
        }
    }

    private static String exactRange(String version) {
        return isBlank(version) ? null : "[" + version + "]";
    }

    private static String string(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class LegacyInfo {
        private ModpackInfo.Modpack modpack;
    }
}
