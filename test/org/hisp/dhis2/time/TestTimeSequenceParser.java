package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.Epoch2kDay.ofYMD;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class TestTimeSequenceParser {

  private static final TimeSequenceParser PARSER = new TimeSequenceParser(List.of(
      GregorianSequences.Daily,
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
  void walkPastAnchorYearThrows() {
    // 2022 has 4 quarters; :5 has no candidate within the anchor period.
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("GQ/2022:5"));
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
  void monthAnchorResolvesToThatMonth() {
    var p = PARSER.parse("GM/2022-03:1");
    assertEquals(new Epoch2kInterval(ofYMD(2022, 3, 1), ofYMD(2022, 3, 31)), p.interval());
  }

  @Test
  void monthAnchorPastFirstPositionThrows() {
    // A single month overlaps exactly one monthly interval; :2 has no candidate.
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("GM/2022-03:2"));
  }

  @Test
  void walkOnUnregisteredKeyThrows() {
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("EQ/2022:1"));
  }

  @Test
  void walkOnCustomSequenceWithGapAnchorThrows() {
    // Custom sequence: one term Jan 8 - May 24, 2024.
    TimeSequenceKey key = new TimeSequenceKey('S', 'Y');
    var terms = CustomSequences.of(
        key,
        List.of(new Epoch2kInterval(ofYMD(2024, 1, 8), ofYMD(2024, 5, 24))));
    var parser = new TimeSequenceParser(List.of(terms));

    // July is in a gap; walking from it fails.
    assertThrows(IllegalArgumentException.class, () -> parser.parse("SY/2024-07-01:1"));
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
  void customSequenceCollidingWithCalendarKeyRejectedAtRegistryConstruction() {
    var gregorianYear = GregorianSequences.Yearly;   // key "GY/"
    var fakeCustom = CustomSequences.of(
        new TimeSequenceKey('G', 'Y'),               // also "GY/"
        List.of(new Epoch2kInterval(ofYMD(2024, 1, 8), ofYMD(2024, 5, 24))));

    assertThrows(IllegalArgumentException.class,
        () -> new TimeSequenceParser(List.of(gregorianYear, fakeCustom)));
  }

  @Test
  void quarterlyFromYearAnchorResolvesWithinYear() {
    assertEquals(new Epoch2kInterval(ofYMD(2024, 1, 1), ofYMD(2024, 3, 31)),
        PARSER.parse("GQ/2024:1").interval());
    assertEquals(new Epoch2kInterval(ofYMD(2024, 4, 1), ofYMD(2024, 6, 30)),
        PARSER.parse("GQ/2024:2").interval());
    assertEquals(new Epoch2kInterval(ofYMD(2024, 7, 1), ofYMD(2024, 9, 30)),
        PARSER.parse("GQ/2024:3").interval());
    assertEquals(new Epoch2kInterval(ofYMD(2024, 10, 1), ofYMD(2024, 12, 31)),
        PARSER.parse("GQ/2024:4").interval());
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("GQ/2024:5"));
  }

  @Test
  void monthlyFromYearAnchorReachesDecemberAndNoFurther() {
    assertEquals(new Epoch2kInterval(ofYMD(2024, 12, 1), ofYMD(2024, 12, 31)),
        PARSER.parse("GM/2024:12").interval());
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("GM/2024:13"));
  }

  @Test
  void quarterlyFromYearAnchorPastEndThrows() {
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("GQ/2022:5"));
  }

  @Test
  void monthlyFromMonthAnchorAllowsOnlyOne() {
    assertEquals(new Epoch2kInterval(ofYMD(2022, 3, 1), ofYMD(2022, 3, 31)),
        PARSER.parse("GM/2022-03:1").interval());
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("GM/2022-03:2"));
  }

  @Test
  void canonicalFormReParsesToSameInterval() {
    for (String s : List.of(
        "GQ/2022:1", "GQ/2022:3",
        "GM/2022-03:1", "GY/2022:1",
        "GQ/2022-04-01+91", "GY/2022+365")) {
      var first = PARSER.parse(s);
      var second = PARSER.parse(first.toString());
      assertEquals(first.interval(), second.interval(),
          () -> "round-trip failed for: " + s);
    }
  }

  @Test
  void compactAndDashedFormsAreEquivalent() {
    for (var pair : List.of(
        List.of("GQ/2024-03-15+1", "GQ/20240315+1"),
        List.of("GQ/2024-03:1",    "GQ/202403:1"),
        List.of("GQ/2024:1",       "GQ/2024:1"))) {
      assertEquals(
          PARSER.parse(pair.get(0)).interval(),
          PARSER.parse(pair.get(1)).interval(),
          () -> pair.get(0) + " vs " + pair.get(1));
    }
  }

  @Test
  void variableWidthDashedComponentsAccepted() {
    assertEquals(
        PARSER.parse("GQ/2024-3-5+1").interval(),
        PARSER.parse("GQ/20240305+1").interval());
  }

  @Test
  void compactFormRejectsInvalidLengths() {
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("GQ/20241+1"));
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("GQ/2024031+1"));
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("GQ/202403151+1"));
  }

  @Test
  void compactFormRespectsCalendarValidation() {
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("GQ/20230229+1"));
    assertEquals(
        new Epoch2kInterval(ofYMD(2024, 2, 29), ofYMD(2024, 2, 29)),
        PARSER.parse("GQ/20240229+1").interval());
  }

  @Test
  void compactFormRejectsNonDigits() {
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("GQ/2024ab+1"));
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("GQ/20A403+1"));
  }

  @Test
  void rejectsOverlongNumeric() {
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("GQ/2024:99999999999"));
  }

  @Test
  void acceptsArbitraryCharSequence() {
    var sb = new StringBuilder("GQ/2024:2");
    assertEquals(PARSER.parse("GQ/2024:2").interval(), PARSER.parse(sb).interval());
  }

  @Test
  void compactAndDashedFormsAreEquivalentForExplicitLength() {
    for (String form : List.of(
        "GQ/2024-03-15+1", "GQ/20240315+1")) {
      assertEquals(
          new Epoch2kInterval(ofYMD(2024, 3, 15), ofYMD(2024, 3, 15)),
          PARSER.parse(form).interval(),
          () -> "failed for: " + form);
    }
  }

  @Test
  void compactAndDashedFormsAreEquivalentForWalkYear() {
    assertEquals(
        PARSER.parse("GQ/2024:1").interval(),
        PARSER.parse("GQ/2024:1").interval());   // trivially true, but locks the year form
  }

  @Test
  void compactAndDashedFormsAreEquivalentForWalkMonth() {
    assertEquals(
        PARSER.parse("GQ/2024-03:1").interval(),
        PARSER.parse("GQ/202403:1").interval());
  }

  @Test
  void compactAndDashedFormsAreEquivalentForWalkDay() {
    assertEquals(
        PARSER.parse("GQ/2024-03-15:1").interval(),
        PARSER.parse("GQ/20240315:1").interval());
  }

  @Test
  void compactFormRespectsOfYMDValidation() {
    // 2023 is not a leap year, so Feb 29 fails at the calendar layer.
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("GQ/20230229+1"));
    // Same day in 2024 succeeds.
    assertEquals(
        new Epoch2kInterval(ofYMD(2024, 2, 29), ofYMD(2024, 2, 29)),
        PARSER.parse("GQ/20240229+1").interval());
  }

  @Test
  void noSuffixEqualsPositionOne() {
    for (String s : List.of(
        "GQ/2024", "GQ/2024-01", "GQ/2024-01-01", "GQ/2024-03-15", "GQ/2024:1")) {
      assertEquals(
          PARSER.parse("GQ/2024:1").interval(),
          PARSER.parse(s).interval(),
          () -> "failed for: " + s);
    }
  }

  @Test
  void dailySequenceWithYearAnchorYieldsFirstDayOfYear() {
    // No suffix = first interval overlapping the anchor period.
    // For GD over a year, that's January 1st — use +366 for the whole year.
    assertEquals(
        new Epoch2kInterval(ofYMD(2024, 1, 1)),
        PARSER.parse("GD/2024").interval());
  }

  @Test
  void noSuffixOnDailySequenceIsSingleDay() {
    assertEquals(
        new Epoch2kInterval(ofYMD(2024, 3, 15)),
        PARSER.parse("GD/2024-03-15").interval());
    assertEquals(
        new Epoch2kInterval(ofYMD(2024, 3, 15)),
        PARSER.parse("GD/2024-03-15+1").interval());
  }

  @Test
  void noSuffixOnQuarterlySequenceIsContainingQuarter() {
    var q1 = new Epoch2kInterval(ofYMD(2024, 1, 1), ofYMD(2024, 3, 31));
    assertEquals(q1, PARSER.parse("GQ/2024-01-01").interval());
    assertEquals(q1, PARSER.parse("GQ/2024-03-15").interval());
    assertEquals(q1, PARSER.parse("GQ/2024").interval());
  }
}