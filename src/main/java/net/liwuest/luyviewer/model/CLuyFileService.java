package net.liwuest.luyviewer.model;

import io.minio.*;
import net.liwuest.luyviewer.LUYViewer;
import net.liwuest.luyviewer.util.CConfig;
import net.liwuest.luyviewer.util.CConfigService;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import io.minio.messages.Item;

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

    /**
     * Gibt die S3-Paare zurück, die lokal fehlen.
     */
    public static Set<String> getMissingS3Pairs(CConfig config) throws Exception {
      MinioClient minioClient = MinioClient.builder().endpoint(config.s3_url).credentials(config.s3_access_key, config.s3_secret_key).build();
      String bucket = config.s3_bucket;
      String prefix = config.s3_folder.endsWith("/") ? config.s3_folder : config.s3_folder + "/";
      Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder().bucket(bucket).prefix(prefix).recursive(true).build());
      HashSet<String> bases = new HashSet<>();
      ArrayList<String> files = new ArrayList<>();
      for (Result<Item> result : results) {
        String name = result.get().objectName();
        if (name.endsWith("_data.json") || name.endsWith("_metamodel.json")) {
          String base = name.replaceAll("(_data|_metamodel)\\.json$", "");
          files.add(name);
          bases.add(base);
        }
      }
      // Nur Paare, die _data und _metamodel haben
      HashSet<String> pairs = new HashSet<>();
      for (String base : bases) {
        boolean hasData = files.contains(base + "_data.json");
        boolean hasMeta = files.contains(base + "_metamodel.json");
        if (hasData && hasMeta) pairs.add(base);
      }
        pairs.removeAll(listFiles());
      return pairs.stream().map(p -> p.startsWith(prefix) ? p.substring(prefix.length()) : p).collect(Collectors.toSet());
    }

    /**
     * Lädt ein _data und _metamodel Paar aus S3 in das lokale data/-Verzeichnis.
     */
    public static void downloadS3FilePair(CConfig config, String baseName) throws Exception {
        MinioClient minioClient = MinioClient.builder().endpoint(config.s3_url).credentials(config.s3_access_key, config.s3_secret_key).build();
        String bucket = config.s3_bucket;
      String prefix = config.s3_folder.endsWith("/") ? config.s3_folder : config.s3_folder + "/";
        String dataKey = prefix.isEmpty() ? baseName + "_data.json" : prefix + baseName + "_data.json";
        String metaKey = prefix.isEmpty() ? baseName + "_metamodel.json" : prefix + baseName + "_metamodel.json";
        Files.createDirectories(Paths.get("data"));
        try (GetObjectResponse dataStream = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(dataKey).build()); FileOutputStream outData = new FileOutputStream("data/" + baseName + "_data.json")) {
            dataStream.transferTo(outData);
        }
        try (GetObjectResponse metaStream = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(metaKey).build()); FileOutputStream outMeta = new FileOutputStream("data/" + baseName + "_metamodel.json")) {
            metaStream.transferTo(outMeta);
        }
    }
}