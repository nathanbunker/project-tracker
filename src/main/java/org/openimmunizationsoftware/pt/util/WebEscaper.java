package org.openimmunizationsoftware.pt.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public final class WebEscaper {

  private WebEscaper() {
    // Utility class
  }

  public static String escapeHtml(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  public static String urlEncode(String value) {
    if (value == null) {
      return "";
    }
    try {
      return URLEncoder.encode(value, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }
}
