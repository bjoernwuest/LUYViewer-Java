package net.liwuest.luyviewer.model;

import net.liwuest.luyviewer.LUYViewer;
import net.liwuest.luyviewer.util.CConfig;
import net.liwuest.luyviewer.util.CConfigService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for handling LUY file operations.
 */
public class CLuyFileService {
    /**
     * Lists the unique prefixes of the file names present in the "data" directory,
     * separated by an underscore character ('_'). Each file name in the directory
     * is split by the underscore, and the first part is collected into a set.
     *
     * @return a set of unique prefixes of file names in the "data" directory.
     * @throws IOException if an I/O error occurs when accessing the directory or files.
     */
    public static Set<String> listFiles() throws IOException {
        Files.createDirectories(Paths.get("data"));
        return Files.list(Paths.get("data")).map(p -> p.getFileName().toString().split("_")[0]).collect(Collectors.toSet());
    }

    /**
     * Downloads a LUY file by its ID from the configured LUY host with authentication.
     */
    public static String downloadFile(String username, String password) throws IOException, InterruptedException {
        // Ensure "data" directory exists
        java.nio.file.Files.createDirectories(Paths.get("data"));

        // Get current time for consistent naming
        String fileName = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss"));

        // Create auth header
        String auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

        // Download and save metamodel file
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        CConfig config = CConfigService.getConfig();
        if (null == config.luyHost || config.luyHost.isEmpty()) throw new IOException("LUY host not configured");

        LUYViewer.LOGGER.info("Proxy used: " + System.getProperty("http.proxyHost") + ":" + System.getProperty("http.proxyPort"));

        LUYViewer.LOGGER.info("Download metadata from : " + URI.create(config.luyHost + "/api/metamodel").toString());
        HttpResponse<String> metamodelResponse = httpClient.send(HttpRequest.newBuilder().uri(URI.create(config.luyHost + "/api/metamodel")).timeout(Duration.ofMinutes(2)).header("Accept", "application/json").header("User-Agent", "LUYViewer-Java/1.0.0").header("Authorization", "Basic " + auth).GET().build(), HttpResponse.BodyHandlers.ofString());
        if (200 == metamodelResponse.statusCode()) {
            LUYViewer.LOGGER.info("Download data from : " + URI.create(config.luyHost + "/api/data").toString());
            HttpResponse<String> dataResponse = httpClient.send(HttpRequest.newBuilder().uri(URI.create(config.luyHost + "/api/data")).timeout(Duration.ofMinutes(10)).header("Accept", "application/json").header("User-Agent", "LUYViewer-Java/1.0.0").header("Authorization", "Basic " + auth).GET().build(), HttpResponse.BodyHandlers.ofString());
            if (200 == dataResponse.statusCode()) {
                LUYViewer.LOGGER.info("Saving data file: " + fileName + "_metamodel.json / " + fileName + "_data.json");
                java.nio.file.Files.writeString(Paths.get("data", fileName + "_metamodel.json"), metamodelResponse.body());
                java.nio.file.Files.writeString(Paths.get("data", fileName + "_data.json"), dataResponse.body());
                return fileName;
            } else throw new IOException("Failed to download data file: HTTP " + dataResponse.statusCode()); // FIXME: translation
        } else throw new IOException("Failed to download metamodel file: HTTP " + metamodelResponse.statusCode()); // FIXME: translation
    }
}