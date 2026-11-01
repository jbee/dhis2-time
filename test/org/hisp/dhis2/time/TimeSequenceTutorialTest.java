package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.Epoch2kDay.ofYMD;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * A progressive tour of the period-string syntax. Each test is self-contained
 * and introduces one feature, building on the ones introduced above. Read
 * top-to-bottom as a tutorial: start with a single day, end with custom
 * sequences and canonical round-trips.
 *
 * <p>The general form is {@code <key>/<date><suffix>}. The key (2 or 3 chars,
 * optionally followed by an offset) identifies the sequence; the date is
 * always Gregorian; the suffix is either an explicit length ({@code +N}) or a
 * position ({@code :N}), or absent (meaning position 1).
 */
class TimeSequenceTutorialTest {

  // ── 1. The simplest period: a single day ──────────────────────────

  @Test
  void aSingleDay() {
    var parser = new TimeSequenceParser(List.of(GregorianSequences.GD_));

    // "GD/" is the daily sequence. A date with no suffix resolves to the
    // single day denoted by that date.
    var input = parser.parse("GD/2024-03-15");

    assertEquals(new Epoch2kInterval(ofYMD(2024, 3, 15)), input.interval());
  }

  // ── 2. The no-suffix form on a coarser sequence ───────────────────

  @Test
  void theSuffixIsOptionalForASingleInterval() {
    var parser = new TimeSequenceParser(List.of(GregorianSequences.GQ_));

    // "GQ/" is the quarterly sequence. The no-suffix form means "the first
    // quarterly interval containing (or following) the given day".
    var q1 = parser.parse("GQ/2024-02-15").interval();

    assertEquals(ofYMD(2024, 1, 1), q1.firstDay());
    assertEquals(ofYMD(2024, 3, 31), q1.lastDay());
  }

  // ── 3. Short date forms ───────────────────────────────────────────

  @Test
  void yearOnlyDate() {
    var parser = new TimeSequenceParser(List.of(GregorianSequences.GQ_));

    // A year-only date means "the whole year". The no-suffix form then picks
    // the first quarterly interval that overlaps it.
    var q1 = parser.parse("GQ/2024").interval();

    assertEquals(ofYMD(2024, 1, 1), q1.firstDay());
    assertEquals(ofYMD(2024, 3, 31), q1.lastDay());
  }

  @Test
  void monthOnlyDate() {
    var parser = new TimeSequenceParser(List.of(GregorianSequences.GQ_));

    // "2024-04" means April 2024 — a month is enough to disambiguate a quarter.
    var q2 = parser.parse("GQ/2024-04").interval();

    assertEquals(ofYMD(2024, 4, 1), q2.firstDay());
    assertEquals(ofYMD(2024, 6, 30), q2.lastDay());
  }

  // ── 4. Explicit length: "+N" ──────────────────────────────────────

  @Test
  void explicitLength() {
    var parser = new TimeSequenceParser(List.of(GregorianSequences.GQ_));

    // "+91" says: a 91-day interval starting on the given day. The parser
    // does not check that this matches a quarter — it just builds the interval.
    var interval = parser.parse("GQ/2024-01-01+91").interval();

    assertEquals(ofYMD(2024, 1, 1), interval.firstDay());
    assertEquals(ofYMD(2024, 3, 31), interval.lastDay());
  }

  @Test
  void explicitLengthCanBeAnyLength() {
    var parser = new TimeSequenceParser(List.of(GregorianSequences.GQ_));

    // The +N form is free-form: any positive length works.
    assertEquals(1, parser.parse("GQ/2024-01-01+1").interval().lengthInDays());
    assertEquals(366, parser.parse("GQ/2024-01-01+366").interval().lengthInDays());
  }

  // ── 5. Position within the anchor period: ":N" ────────────────────

  @Test
  void positionWithinYear() {
    var parser = new TimeSequenceParser(List.of(GregorianSequences.GQ_));

    // "2024:1" means: the 1st GQ interval overlapping 2024 — that's Q1.
    // "2024:4" is Q4. The position is bounded by the anchor's period.
    var q1 = parser.parse("GQ/2024:1").interval();
    var q4 = parser.parse("GQ/2024:4").interval();

    assertEquals(ofYMD(2024, 1, 1), q1.firstDay());
    assertEquals(ofYMD(2024, 10, 1), q4.firstDay());
  }

  @Test
  void positionClippedToAnchorPeriod() {
    var parser = new TimeSequenceParser(List.of(GregorianSequences.GQ_));

    // A year contains exactly 4 quarters. :5 has no candidate — the walk does
    // not continue into the next year.
    var ex = assertThrows(IllegalArgumentException.class, () -> parser.parse("GQ/2024:5"));
    assertTrue(ex.getMessage().contains("exceeds the 4 interval(s)"));
  }

  @Test
  void positionWithinMonth() {
    var parser = new TimeSequenceParser(List.of(GregorianSequences.GM_));

    // Anchored at a month, a monthly sequence has exactly one overlapping
    // interval. :1 works; :2 has no candidate.
    var march = parser.parse("GM/2024-03:1").interval();
    assertEquals(ofYMD(2024, 3, 1), march.firstDay());

    assertThrows(IllegalArgumentException.class, () -> parser.parse("GM/2024-03:2"));
  }

  // ── 6. Compact date forms ─────────────────────────────────────────

  @Test
  void compactDateForms() {
    var parser = new TimeSequenceParser(List.of(GregorianSequences.GQ_));

    // YYYYMMDD works identically to YYYY-MM-DD; YYYYMM to YYYY-MM.
    assertEquals(
        parser.parse("GQ/2024-04-15:1").interval(),
        parser.parse("GQ/20240415:1").interval());

    assertEquals(
        parser.parse("GQ/2024-04:1").interval(),
        parser.parse("GQ/202404:1").interval());
  }

  // ── 7. Multiple sequences in one parser ───────────────────────────

  @Test
  void multipleKeysResolvedByPrefix() {
    var parser = new TimeSequenceParser(List.of(
        GregorianSequences.GY_,
        GregorianSequences.GQ_,
        GregorianSequences.GM_,
        GregorianSequences.GD_));

    // Same date, different key → different interval granularity.
    var year    = parser.parse("GY/2024-06-15").interval();
    var quarter = parser.parse("GQ/2024-06-15").interval();
    var month   = parser.parse("GM/2024-06-15").interval();
    var day     = parser.parse("GD/2024-06-15").interval();

    assertEquals(ofYMD(2024, 1, 1),  year.firstDay());
    assertEquals(ofYMD(2024, 4, 1),  quarter.firstDay());
    assertEquals(ofYMD(2024, 6, 1),  month.firstDay());
    assertEquals(ofYMD(2024, 6, 15), day.firstDay());
  }

  // ── 8. Same interval via different input forms ────────────────────

  @Test
  void sameIntervalViaDifferentPaths() {
    var parser = new TimeSequenceParser(List.of(GregorianSequences.GQ_));

    // Q1 2024 can be written five different ways. They all resolve identically.
    var viaNoSuffix     = parser.parse("GQ/2024-01-01").interval();
    var viaYear         = parser.parse("GQ/2024").interval();
    var viaPosition     = parser.parse("GQ/2024:1").interval();
    var viaExplicit     = parser.parse("GQ/2024-01-01+91").interval();
    var viaAnyDayInQ1   = parser.parse("GQ/2024-02-15").interval();

    assertEquals(viaNoSuffix, viaYear);
    assertEquals(viaNoSuffix, viaPosition);
    assertEquals(viaNoSuffix, viaExplicit);
    assertEquals(viaNoSuffix, viaAnyDayInQ1);
  }

  // ── 9. Canonical form ─────────────────────────────────────────────

  @Test
  void canonicalFormIsAlwaysExplicitLength() {
    var parser = new TimeSequenceParser(List.of(GregorianSequences.GQ_));

    // Whatever the input form, toString returns the canonical +N form.
    assertEquals("GQ/2024-01-01+91", parser.parse("GQ/2024-01-01+91").toString());
    assertEquals("GQ/2024-01-01+91", parser.parse("GQ/2024:1").toString());
    assertEquals("GQ/2024-01-01+91", parser.parse("GQ/2024").toString());
    assertEquals("GQ/2024-01-01+91", parser.parse("GQ/2024-01-01").toString());
  }

  @Test
  void canonicalRoundTrip() {
    var parser = new TimeSequenceParser(List.of(GregorianSequences.GQ_));

    // parse → toString → parse yields the same interval, for any input form.
    for (String s : List.of(
        "GQ/2024", "GQ/2024:1", "GQ/2024-04", "GQ/2024-04-15",
        "GQ/2024:3", "GQ/2024-01-01+91", "GQ/202404")) {
      var first = parser.parse(s);
      var second = parser.parse(first.toString());
      assertEquals(first.interval(), second.interval(), () -> "round-trip failed for: " + s);
    }
  }

  // ── 10. Custom sequences ──────────────────────────────────────────

  @Test
  void customSequenceWithGaps() {
    // School terms 2024: Spring and Autumn, with a summer gap between them.
    var spring = new Epoch2kInterval(ofYMD(2024, 1, 8), ofYMD(2024, 5, 24));
    var autumn = new Epoch2kInterval(ofYMD(2024, 9, 2), ofYMD(2024, 12, 20));
    var school = CustomSequences.of(
        new TimeSequenceKey('S', 'T'), List.of(spring, autumn));
    var parser = new TimeSequenceParser(List.of(school));

    // Same :N syntax as calendars; the position refers to the n-th term
    // overlapping the anchor period.
    assertEquals(spring, parser.parse("ST/2024:1").interval());
    assertEquals(autumn, parser.parse("ST/2024:2").interval());

    // The no-suffix form still means "first interval overlapping the date".
    assertEquals(spring, parser.parse("ST/2024-03-15").interval());
    assertEquals(autumn, parser.parse("ST/2024-09-15").interval());
  }

  @Test
  void customSequenceGapDaysRejected() {
    var spring = new Epoch2kInterval(ofYMD(2024, 1, 8), ofYMD(2024, 5, 24));
    var autumn = new Epoch2kInterval(ofYMD(2024, 9, 2), ofYMD(2024, 12, 20));
    var school = CustomSequences.of(
        new TimeSequenceKey('S', 'T'), List.of(spring, autumn));
    var parser = new TimeSequenceParser(List.of(school));

    // July 2024 is in the gap; no term overlaps it.
    assertThrows(IllegalArgumentException.class, () -> parser.parse("ST/2024-07:1"));
    assertThrows(IllegalArgumentException.class, () -> parser.parse("ST/2024-07-15"));
  }

  // ── 11. Errors ────────────────────────────────────────────────────

  @Test
  void unknownKeyRejected() {
    var parser = new TimeSequenceParser(List.of(GregorianSequences.GQ_));

    // GY is a valid key globally, but not registered with this parser.
    assertThrows(IllegalArgumentException.class, () -> parser.parse("GY/2024:1"));
  }

  @Test
  void structuralErrorsRejected() {
    var parser = new TimeSequenceParser(List.of(GregorianSequences.GQ_));

    // Missing "/" between key and date.
    assertThrows(IllegalArgumentException.class, () -> parser.parse("GQ2024"));
    // Both suffix forms at once.
    assertThrows(IllegalArgumentException.class, () -> parser.parse("GQ/2024+1:1"));
    // Neither suffix nor a date.
    assertThrows(IllegalArgumentException.class, () -> parser.parse("GQ/"));
  }

  @Test
  void contentErrorsRejected() {
    var parser = new TimeSequenceParser(List.of(GregorianSequences.GQ_));

    // February 29 in a non-leap year.
    assertThrows(IllegalArgumentException.class, () -> parser.parse("GQ/2023-02-29"));
    // Month 13 doesn't exist.
    assertThrows(IllegalArgumentException.class, () -> parser.parse("GQ/2024-13:1"));
    // Position beyond the anchor period's capacity.
    assertThrows(IllegalArgumentException.class, () -> parser.parse("GQ/2024:5"));
  }
}