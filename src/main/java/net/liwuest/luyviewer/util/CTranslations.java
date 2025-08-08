package net.liwuest.luyviewer.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class CTranslations {
  @JsonProperty(value = "Label_Ready", defaultValue = "Ready") public String Label_Ready;
  @JsonProperty(value = "Button_Download", defaultValue = "Download from LUY") public String Button_Download;
  @JsonProperty(value = "Label_Downloading", defaultValue = "Loading data...") public String Label_Downloading;
  @JsonProperty(value = "Label_DownloadError", defaultValue = "Error loading data: %s") public String Label_DownloadError;
  @JsonProperty(value = "Label_SelectDatafile", defaultValue = "Select data file:") public String Label_SelectDatafile;

  private CTranslations() {}
  public final static CTranslations INSTANCE;
  static {
    CTranslations t = null;
    try {
      String configFileName = "texts_";
      try { configFileName += CConfigService.getConfig().language; } catch (IOException Ignored) { configFileName += "en"; }
      configFileName += ".json";
      Path configFile = Paths.get(configFileName);
      if (!Files.exists(configFile)) { configFileName = "texts_en.json"; }

      String content = Files.readString(configFile);
      ObjectMapper objectMapper = new ObjectMapper();
      // Enable comments in JSON
      JsonFactory factory = objectMapper.getFactory();
      factory.enable(JsonParser.Feature.ALLOW_COMMENTS);
      factory.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
      factory.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
      t = objectMapper.readValue(content, CTranslations.class);
    } catch (IOException Ex) {
      Ex.printStackTrace();
      System.exit(-1);
    }
    INSTANCE = t;
  }
}
