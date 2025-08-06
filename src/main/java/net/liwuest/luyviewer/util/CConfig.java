package net.liwuest.luyviewer.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration model for the LUYViewer application.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CConfig {
  @JsonProperty("luy_host") public String luyHost;
  @JsonProperty("language") public String language;

  public CConfig() {}
}
