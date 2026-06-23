package org.openimmunizationsoftware.pt.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class BillCodeDisplayTest {

  @Test
  public void normalizeDisplayColorAcceptsHexWithOrWithoutHash() {
    assertEquals("#336699", BillCodeDisplay.normalizeDisplayColor("#336699"));
    assertEquals("#336699", BillCodeDisplay.normalizeDisplayColor("336699"));
    assertEquals("#ABCDEF", BillCodeDisplay.normalizeDisplayColor("#ABCDEF"));
    assertEquals("#ABCDEF", BillCodeDisplay.normalizeDisplayColor("abcdef"));
  }

  @Test
  public void normalizeDisplayColorReturnsNullForBlank() {
    assertNull(BillCodeDisplay.normalizeDisplayColor(null));
    assertNull(BillCodeDisplay.normalizeDisplayColor(""));
    assertNull(BillCodeDisplay.normalizeDisplayColor("   "));
  }

  @Test
  public void normalizeDisplayColorRejectsInvalidValues() {
    assertInvalid("#12345");
    assertInvalid("#1234567");
    assertInvalid("blue");
    assertInvalid("rgb(1,2,3)");
    assertInvalid("12 456");
  }

  @Test
  public void getDisplayDotLabelUsesConfiguredFallbackOrder() {
    BillCode billCode = new BillCode();
    billCode.setBillCode("ABC");
    billCode.setBillLabel("Bill Label");
    billCode.setDisplayLabel("Display Label");
    assertEquals("Display Label", BillCodeDisplay.getDisplayDotLabel(billCode));

    billCode.setDisplayLabel(" ");
    assertEquals("Bill Label", BillCodeDisplay.getDisplayDotLabel(billCode));

    billCode.setBillLabel("");
    assertEquals("ABC", BillCodeDisplay.getDisplayDotLabel(billCode));
  }

  private void assertInvalid(String value) {
    try {
      BillCodeDisplay.normalizeDisplayColor(value);
    } catch (IllegalArgumentException e) {
      return;
    }
    throw new AssertionError("Expected invalid color: " + value);
  }
}
