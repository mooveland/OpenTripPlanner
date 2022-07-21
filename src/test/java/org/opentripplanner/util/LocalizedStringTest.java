package org.opentripplanner.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.LocalizedString;
import org.opentripplanner.transit.model.basic.TranslatedString;

class LocalizedStringTest {

  @Test
  public void locale() {
    assertEquals(
      "corner of First and Second",
      new LocalizedString(
        "corner",
        TranslatedString.getI18NString("First", "de", "erste"),
        TranslatedString.getI18NString("Second", "de", "zweite")
      )
        .toString()
    );
  }

  @Test
  public void localeWithTranslation() {
    assertEquals(
      "Kreuzung Erste mit Zweite",
      new LocalizedString(
        "corner",
        TranslatedString.getI18NString("First", "de", "Erste"),
        TranslatedString.getI18NString("Second", "de", "Zweite")
      )
        .toString(Locale.GERMANY)
    );
  }

  @Test
  public void localeWithoutTranslation() {
    assertEquals(
      "corner of First and Second",
      new LocalizedString(
        "corner",
        TranslatedString.getI18NString("First", "de", "erste"),
        TranslatedString.getI18NString("Second", "de", "zweite")
      )
        .toString(Locale.CHINESE)
    );
  }

  @Test
  public void localeWithoutParams() {
    assertEquals("Destination", new LocalizedString("destination").toString());
  }
}
