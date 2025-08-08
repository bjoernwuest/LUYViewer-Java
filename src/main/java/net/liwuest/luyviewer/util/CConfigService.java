package net.liwuest.luyviewer.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service for handling configuration file operations.
 */
public final class CConfigService {
    private static CConfig config;

    /**
     * Gets the cached configuration or loads it if not already loaded.
     */
    public static synchronized CConfig getConfig() throws IOException {
        if (null == config) {
            Path configPath = Paths.get("config.jsonc");
            if (!Files.exists(configPath)) throw new IOException("Configuration file not found: config.jsonc");

            String content = Files.readString(configPath);
            ObjectMapper objectMapper = new ObjectMapper();
            // Enable comments in JSON
            JsonFactory factory = objectMapper.getFactory();
            factory.enable(JsonParser.Feature.ALLOW_COMMENTS);
            factory.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
            factory.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
            config = objectMapper.readValue(content, CConfig.class);
        }
        return config;
    }
}
