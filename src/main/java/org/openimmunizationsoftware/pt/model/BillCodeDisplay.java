package org.openimmunizationsoftware.pt.model;

import java.util.regex.Pattern;

public final class BillCodeDisplay {

  private static final Pattern HEX_COLOR = Pattern.compile("^#?[0-9A-Fa-f]{6}$");

  private BillCodeDisplay() {
  }

  public static String normalizeDisplayColor(String value) {
    if (value == null || value.trim().length() == 0) {
      return null;
    }
    String color = value.trim();
    if (!HEX_COLOR.matcher(color).matches()) {
      throw new IllegalArgumentException("Display color must be a 6-digit hex color, such as #336699 or 336699.");
    }
    if (!color.startsWith("#")) {
      color = "#" + color;
    }
    return color.toUpperCase();
  }

  public static String normalizeDisplayColorOrNull(String value) {
    try {
      return normalizeDisplayColor(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public static String getDisplayDotLabel(BillCode billCode) {
    if (billCode == null) {
      return "";
    }
    if (hasText(billCode.getDisplayLabel())) {
      return billCode.getDisplayLabel().trim();
    }
    if (hasText(billCode.getBillLabel())) {
      return billCode.getBillLabel().trim();
    }
    return billCode.getBillCode() == null ? "" : billCode.getBillCode().trim();
  }

  private static boolean hasText(String value) {
    return value != null && value.trim().length() > 0;
  }
}
