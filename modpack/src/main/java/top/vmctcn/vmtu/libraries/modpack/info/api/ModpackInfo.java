package top.vmctcn.vmtu.libraries.modpack.info.api;

import java.util.Collections;
import java.util.List;

/**
 * Normalized patch pack information for both patchpackinfo.json and the
 * legacy modpackinfo.json format.
 */
public class ModpackInfo {
    public int formatVersion = 1;
    public String id;
    public String name;
    public String description;
    public String language;
    /** The translation/patch version. */
    public String version;
    /** The exact version of the modpack this file was generated for. */
    public String modpackVersion;
    public String modpackVersionRange;
    public List<String> authors;
    public String url;
    public Diff diff;

    /* Kept only as a source-compatible view for users of the old API. */
    @Deprecated
    public transient Modpack modpack;

    public int getFormatVersion() { return formatVersion; }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getLanguage() { return language; }
    public String getVersion() { return version; }
    public String getTranslationVersion() { return version; }
    public String getModpackVersion() { return modpackVersion; }
    public String getModpackVersionRange() { return modpackVersionRange; }
    public String getUrl() { return url; }
    public Diff getDiff() { return diff; }

    public List<String> getAuthors() {
        return authors == null ? Collections.<String>emptyList() : Collections.unmodifiableList(authors);
    }

    public void setModpackVersion(String modpackVersion) {
        this.modpackVersion = modpackVersion;
        if (modpack != null) modpack.version = modpackVersion;
    }

    /** Compatibility view for existing users of the nested v1 API. */
    public Modpack getModpack() {
        if (modpack == null) {
            modpack = new Modpack();
            modpack.name = name;
            modpack.version = modpackVersion;
            modpack.translation = new Translation();
            modpack.translation.id = id;
            modpack.translation.url = url;
            modpack.translation.language = language;
            modpack.translation.version = version;
        }
        return modpack;
    }

    public static class Diff {
        public List<String> delete;
        public List<Rename> rename;

        public List<String> getDelete() {
            return delete == null ? Collections.<String>emptyList() : Collections.unmodifiableList(delete);
        }

        public List<Rename> getRename() {
            return rename == null ? Collections.<Rename>emptyList() : Collections.unmodifiableList(rename);
        }
    }

    public static class Rename {
        public String from;
        public String to;
        public String getFrom() { return from; }
        public String getTo() { return to; }
    }

    public static class Modpack {
        public String name;
        public String version;
        public Translation translation;
        public String getName() { return name; }
        public String getVersion() { return version; }
        public Translation getTranslation() { return translation; }
    }

    public static class Translation {
        public String id;
        public String url;
        public String language;
        public String version;
        @Deprecated public String updateCheckUrl;
        @Deprecated public String resourcePackName;
        public String getId() { return id; }
        public String getUrl() { return url; }
        public String getLanguage() { return language; }
        public String getVersion() { return version; }
        @Deprecated public String getUpdateCheckUrl() { return updateCheckUrl; }
        @Deprecated public String getResourcePackName() { return resourcePackName; }
    }
}
