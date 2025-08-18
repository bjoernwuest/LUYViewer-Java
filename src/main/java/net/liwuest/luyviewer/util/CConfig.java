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
  @JsonProperty("s3_url") public String s3_url;
  @JsonProperty("s3_access_key") public String s3_access_key;
  @JsonProperty("s3_secret_key") public String s3_secret_key;
  @JsonProperty("s3_bucket") public String s3_bucket;
  @JsonProperty("s3_folder") public String s3_folder;

  public CConfig() {}
}
