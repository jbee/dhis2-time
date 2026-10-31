package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.Epoch2kDay.ofYMD;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class TestTimeSequenceParser {

  private static final TimeSequenceParser PARSER = new TimeSequenceParser(List.of(
      GregorianSequences.Quarterly,
      GregorianSequences.Monthly,
      GregorianSequences.Yearly,
      GregorianSequences.Weekly));

  // ── Explicit-length form ──────────────────────────────────────────

  @Test
  void parsesFullDateWithExplicitLength() {
    var p = PARSER.parse("GQ/2022-04-01+91");
    assertEquals(GregorianSequences.Quarterly.key(), p.key());
    assertEquals(new Epoch2kInterval(ofYMD(2022, 4, 1), ofYMD(2022, 6, 30)), p.interval());
  }

  @Test
  void yearOnlyExpandsToJanuaryFirst() {
    var p = PARSER.parse("GY/2022+365");
    assertEquals(new Epoch2kInterval(ofYMD(2022, 1, 1), ofYMD(2022, 12, 31)), p.interval());
  }

  @Test
  void yearMonthExpandsToFirstOfMonth() {
    var p = PARSER.parse("GM/2022-03+31");
    assertEquals(new Epoch2kInterval(ofYMD(2022, 3, 1), ofYMD(2022, 3, 31)), p.interval());
  }

  @Test
  void leadingZerosAccepted() {
    assertEquals(PARSER.parse("GY/2022-1+31").interval(),
        PARSER.parse("GY/2022-01+31").interval());
    assertEquals(PARSER.parse("GY/2022-1-5+27").interval(),
        PARSER.parse("GY/2022-01-05+27").interval());
  }

  @Test
  void twoCharKeyFormWorks() {
    // "GQ/" (no offset) — slash at position 2
    var p = PARSER.parse("GQ/2022+90");
    assertEquals('Q', p.key().interval());
    assertEquals(TimeSequenceKey.NO_OFFSET, p.key().offset());
  }

  @Test
  void threeCharKeyFormWorks() {
    // "GQ4/" (April start) — slash at position 3
    var p = PARSER.parse("GQ4/2022-04-01+92");
    assertEquals('4', p.key().offset());
  }

  // ── Walk form (1-based) ──────────────────────────────────────────

  @Test
  void walkOneIsBaseInterval() {
    var p = PARSER.parse("GQ/2022:1");
    assertEquals(new Epoch2kInterval(ofYMD(2022, 1, 1), ofYMD(2022, 3, 31)), p.interval());
  }

  @Test
  void walkThreeIsThirdQuarter() {
    var p = PARSER.parse("GQ/2022:3");
    assertEquals(new Epoch2kInterval(ofYMD(2022, 7, 1), ofYMD(2022, 9, 30)), p.interval());
  }

  @Test
  void walkTwelveIsDecemberMonth() {
    var p = PARSER.parse("GM/2022:12");
    assertEquals(new Epoch2kInterval(ofYMD(2022, 12, 1), ofYMD(2022, 12, 31)), p.interval());
  }

  @Test
  void walkPastBlockEndContinuesLinearly() {
    // :5 from Q1 2022 lands on Q1 2023 (quarter 5 in linear terms).
    var p = PARSER.parse("GQ/2022:5");
    assertEquals(new Epoch2kInterval(ofYMD(2023, 1, 1), ofYMD(2023, 3, 31)), p.interval());
  }

  @Test
  void walkZeroRejected() {
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("GQ/2022:0"));
  }

  @Test
  void walkNegativeRejected() {
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("GQ/2022:-1"));
  }

  @Test
  void walkWithMonthShortFormAnchor() {
    // GM/2022-03 anchors at Mar 1; :2 walks once -> April
    var p = PARSER.parse("GM/2022-03:2");
    assertEquals(new Epoch2kInterval(ofYMD(2022, 4, 1), ofYMD(2022, 4, 30)), p.interval());
  }

  @Test
  void walkOnUnregisteredKeyThrows() {
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("EQ/2022:1"));
  }

  @Test
  void walkOnCustomSequenceWithGapAnchorThrows() {
    // Custom sequence: one term Jan 8 - May 24, 2024.
    var terms = CustomSequences.of(
        TimeSequenceKey.custom('S'),
        List.of(new Epoch2kInterval(ofYMD(2024, 1, 8), ofYMD(2024, 5, 24))));
    var parser = new TimeSequenceParser(List.of(terms));

    // July is in a gap; walking from it fails.
    assertThrows(IllegalArgumentException.class, () -> parser.parse("CS/2024-07-01:1"));
  }

  // ── Registry construction ────────────────────────────────────────

  @Test
  void duplicateKeysRejectedAtConstruction() {
    assertThrows(IllegalArgumentException.class, () -> new TimeSequenceParser(List.of(
        GregorianSequences.Quarterly,
        GregorianSequences.Quarterly)));
  }

  // ── Error cases ──────────────────────────────────────────────────

  @Test
  void missingSeparatorThrows() {
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("GQ/2022"));
  }

  @Test
  void mixingPlusAndColonThrows() {
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("GQ/2022:2+92"));
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("GQ/2022+92:2"));
  }

  @Test
  void invalidKeyPrefixThrows() {
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("/2022:1"));
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("GQQQQ/2022:1"));
  }

  @Test
  void invalidDateComponentsThrow() {
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("GY/2022-13+30"));
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("GY/2022-02-30+1"));
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("GY/2023-02-29+1"));
  }

  @Test
  void zeroLengthThrows() {
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("GY/2022+0"));
  }

  @Test
  void emptyInputThrows() {
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse(""));
  }

  // ── Canonical form round-trip ────────────────────────────────────

  @Test
  void toStringProducesCanonicalForm() {
    assertEquals("GQ/2022-01-01+90", PARSER.parse("GQ/2022:1").toString());
    assertEquals("GQ/2022-07-01+92", PARSER.parse("GQ/2022:3").toString());
    assertEquals("GY/2022-01-01+365", PARSER.parse("GY/2022+365").toString());
  }

  @Test
  void canonicalFormReParsesToSameInterval() {
    for (String s : List.of(
        "GQ/2022:1", "GQ/2022:3", "GM/2022-03:2", "GY/2022:2",
        "GQ/2022-04-01+91", "GY/2022+365")) {
      var first = PARSER.parse(s);
      var second = PARSER.parse(first.toString());
      assertEquals(first.interval(), second.interval(),
          () -> "round-trip failed for: " + s);
    }
  }
}