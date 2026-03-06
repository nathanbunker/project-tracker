package org.openimmunizationsoftware.pt;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SoftwareVersion {
  public static final String VERSION = loadVersion();

  private static String loadVersion() {
    Properties properties = new Properties();
    try (InputStream inputStream = SoftwareVersion.class.getClassLoader().getResourceAsStream("version.properties")) {
      if (inputStream != null) {
        properties.load(inputStream);
        String version = properties.getProperty("app.version");
        if (version != null && !version.trim().isEmpty()) {
          return version.trim();
        }
      }
    } catch (IOException e) {
      // Fall through to default when resource cannot be read.
    }
    return "unknown";
  }
}
