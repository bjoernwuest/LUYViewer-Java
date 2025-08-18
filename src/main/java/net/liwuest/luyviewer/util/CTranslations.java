package net.liwuest.luyviewer.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.liwuest.luyviewer.LUYViewer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

public final class CTranslations {
  @JsonProperty(value = "Label_Ready", defaultValue = "Ready") public String Label_Ready;
  @JsonProperty(value = "Button_Download", defaultValue = "Download from LUY") public String Button_Download;
  @JsonProperty(value = "Label_Downloading", defaultValue = "Loading data...") public String Label_Downloading;
  @JsonProperty(value = "Label_DownloadError", defaultValue = "Error loading data: %s") public String Label_DownloadError;
  @JsonProperty(value = "Label_SelectDatafile", defaultValue = "Select data file:") public String Label_SelectDatafile;
  @JsonProperty(value = "Unknown_Placeholder", defaultValue = "<UNKNOWN>") public String Unknown_Placeholder;
  @JsonProperty(value = "Button_Cancel", defaultValue = "Cancel") public String Button_Cancel;
  @JsonProperty(value = "Label_EnterCredentials", defaultValue = "Enter your credentials to download from LUY") public String Label_EnterCredentials;
  @JsonProperty(value = "Label_Username", defaultValue = "Username:") public String Label_Username;
  @JsonProperty(value = "Label_Password", defaultValue = "Password:") public String Label_Password;
  @JsonProperty(value = "Title_InputRequired", defaultValue = "Input Required") public String Title_InputRequired;
  @JsonProperty(value = "Label_EnterUsernamePassword", defaultValue = "Please enter both username and password") public String Label_EnterUsernamePassword;
  @JsonProperty(value = "Label_DownloadSuccess", defaultValue = "Download completed successfully") public String Label_DownloadSuccess;
  @JsonProperty(value = "Title_DownloadError", defaultValue = "Download Error") public String Title_DownloadError;
  @JsonProperty(value = "Button_Filter", defaultValue = "Filter") public String Button_Filter;
  @JsonProperty(value = "Tooltip_FilterValid", defaultValue = "Filter is valid") public String Tooltip_FilterValid;
  @JsonProperty(value = "Tooltip_FilterInvalid", defaultValue = "Filter is not set or invalid") public String Tooltip_FilterInvalid;
  @JsonProperty(value = "Title_FilterEdit", defaultValue = "Edit Filter") public String Title_FilterEdit;
  @JsonProperty(value = "Button_Save", defaultValue = "Save") public String Button_Save;
  @JsonProperty(value = "Label_Group", defaultValue = "Group:") public String Label_Group;
  @JsonProperty(value = "Button_AddRule", defaultValue = "Add Rule") public String Button_AddRule;
  @JsonProperty(value = "Button_AddGroup", defaultValue = "Add Group") public String Button_AddGroup;
  @JsonProperty(value = "Label_Rule", defaultValue = "Rule:") public String Label_Rule;
  @JsonProperty(value = "Button_Remove", defaultValue = "Remove") public String Button_Remove;
  @JsonProperty(value = "Button_Export2Excel", defaultValue = "Export to Excel") public String Button_Export2Excel;
  @JsonProperty(value = "Title_Export2Excel", defaultValue = "Export table as Excel file") public String Title_Export2Excel;
  @JsonProperty(value = "Button_DownloadFromS3", defaultValue = "Get files from S3") public String Button_DownloadFromS3;
  @JsonProperty(value ="Label_SelectDatasetToDownload", defaultValue = "Select data set to download") public String Label_SelectDatasetToDownload;
  @JsonProperty(value = "Label_S3ListingFailed", defaultValue = "Failed to list files from S3: %s") public String Label_S3ListingFailed;

  @JsonProperty(value = "Status_Initial", defaultValue = "Select data file or download one from LUY") public String Status_Initial;
  @JsonProperty(value = "Status_LoadLUYDataFile", defaultValue = "Loading data from LUY file...") public String Status_LoadingDataFromLUYFile;
  @JsonProperty(value = "Status_Ready", defaultValue = "Ready") public String Status_Ready;
  @JsonProperty(value = "Status_DownloadError", defaultValue = "Error downloading data: %s") public String Status_DownloadError;
  @JsonProperty(value = "Status_BuildDataView", defaultValue = "Prepare your view on data") public String Status_BuildDataView;

  @JsonProperty(value = "Operation_Equals", defaultValue = "equals") public String Operation_Equals;
  @JsonProperty(value = "Operation_Any", defaultValue = "any") public String Operation_Any;
  @JsonProperty(value = "Operation_Contains", defaultValue = "contains") public String Operation_Contains;
  @JsonProperty(value = "Operation_StartsWith", defaultValue = "starts with") public String Operation_StartsWith;
  @JsonProperty(value = "Operation_EndsWith", defaultValue = "ends with") public String Operation_EndsWith;
  @JsonProperty(value = "Operation_Button_Complex", defaultValue = "complex filter") public String Operation_Button_Complex;
  @JsonProperty(value = "Operation_ComplexAny", defaultValue = "match any") public String Operation_ComplexAny;
  @JsonProperty(value = "Operation_ComplexAll", defaultValue = "match all") public String Operation_ComplexAll;

  private CTranslations() {}
  public final static CTranslations INSTANCE;
  static {
    CTranslations t = null;
    String configFileName = "texts_";
    try {
      try { configFileName += CConfigService.getConfig().language; } catch (IOException Ignored) { configFileName += "en"; }
      configFileName += ".json";
      Path configFile = Paths.get(configFileName);
      if (!Files.exists(configFile)) { configFile = Paths.get("texts_en.json"); }

      String content = Files.readString(configFile);
      ObjectMapper objectMapper = new ObjectMapper();
      // Enable comments in JSON
      JsonFactory factory = objectMapper.getFactory();
      factory.enable(JsonParser.Feature.ALLOW_COMMENTS);
      factory.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
      factory.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
      t = objectMapper.readValue(content, CTranslations.class);
    } catch (IOException Ex) {
      LUYViewer.LOGGER.log(Level.SEVERE, "Could not load language file '" + configFileName + "'", Ex);
      System.exit(-1);
    }
    INSTANCE = t;
  }
}
