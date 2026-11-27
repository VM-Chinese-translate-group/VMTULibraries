package top.vmctcn.vmtu.libraries.resourcepack.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import top.vmctcn.vmtu.libraries.common.CommonContexts;
import top.vmctcn.vmtu.libraries.common.LogMarkers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class AssetUtil {
    private static final List<MirrorSources> mirrors = MirrorSources.getAllMirrors();

    public static void download(String url, Path localFile) throws IOException, URISyntaxException {
        CommonContexts.LOGGER.info(LogMarkers.RESOURCEPACK, "Downloading: {} -> {}", url, localFile);
        FileUtils.copyURLToFile(new URI(url).toURL(), localFile.toFile(),
                (int) TimeUnit.SECONDS.toMillis(3), (int) TimeUnit.SECONDS.toMillis(33));
        CommonContexts.LOGGER.debug(LogMarkers.RESOURCEPACK, "Downloaded: {} -> {}", url, localFile);
    }

    public static String getString(String url) throws IOException, URISyntaxException {
        return IOUtils.toString(new URI(url).toURL(), StandardCharsets.UTF_8);
    }

    public static String getFastestResourcePackUrl() {
        return getFastestUrl() + "resourcepack/";
    }

    public static String getFastestUrl() {
        List<Callable<String>> probes = new ArrayList<>();
        for (MirrorSources mirror : mirrors) {
            probes.add(() -> testUrlConnection(mirror.getMirrorUrl()));
        }
        ExecutorService executor = Executors.newFixedThreadPool(Math.max(mirrors.size(), 1));
        String fastest = MirrorProbeSelector.firstSuccessful(probes, executor, 10, TimeUnit.SECONDS);
        if (fastest != null) {
            CommonContexts.LOGGER.info(LogMarkers.RESOURCEPACK, "Using fastest url: {}", fastest);
            return fastest;
        }
        CommonContexts.LOGGER.info(LogMarkers.RESOURCEPACK, "No successful mirror probe, using VMCT_ESA");
        return MirrorSources.VMCT_ESA.getMirrorUrl();
    }

    private static String testUrlConnection(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url + "resourcepack/metadata.json").openConnection();
        try {
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                return url;
            }
            CommonContexts.LOGGER.debug(LogMarkers.RESOURCEPACK, "URL unreachable: {}, code: {}", url, code);
            throw new IOException("URL unreachable: " + url);
        } finally {
            conn.disconnect();
        }
    }
}
