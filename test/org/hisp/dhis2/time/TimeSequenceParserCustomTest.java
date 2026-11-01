package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.Epoch2kDay.ofYMD;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Parser tests against a table-driven custom sequence — a school-term table
 * with four terms spanning two academic years. The emphasis is on behaviour
 * that only custom sequences exhibit: gap anchors, boundary anchors, and the
 * interval-relative semantics of the {@code :n} walk.
 */
class TimeSequenceParserCustomTest {

  // ── School-term table ──────────────────────────────────────────────

  private static final Epoch2kInterval SPRING_2024 =
      new Epoch2kInterval(ofYMD(2024, 1, 8), ofYMD(2024, 5, 24));   // 138 days

  private static final Epoch2kInterval AUTUMN_2024 =
      new Epoch2kInterval(ofYMD(2024, 9, 2), ofYMD(2024, 12, 20));  // 110 days

  private static final Epoch2kInterval SPRING_2025 =
      new Epoch2kInterval(ofYMD(2025, 1, 6), ofYMD(2025, 5, 23));   // 138 days

  private static final Epoch2kInterval AUTUMN_2025 =
      new Epoch2kInterval(ofYMD(2025, 9, 1), ofYMD(2025, 12, 19));  // 110 days

  private static final TimeSequence SCHOOL_TERMS = CustomSequences.of(
      new TimeSequenceKey('S', 'T'),
      List.of(SPRING_2024, AUTUMN_2024, SPRING_2025, AUTUMN_2025));

  private static final TimeSequenceParser PARSER =
      new TimeSequenceParser(List.of(SCHOOL_TERMS));

  private static Epoch2kInterval parseInterval(String s) {
    return PARSER.parse(s).interval();
  }

  // ── Explicit-length form ──────────────────────────────────────────

  @Test
  void explicitFormReturnsTheSpecifiedInterval() {
    var p = PARSER.parse("ST/2024-01-08+138");
    assertEquals(SCHOOL_TERMS.key(), p.key());
    assertEquals(SPRING_2024, p.interval());
  }

  @Test
  void explicitFormWorksForAnyTableInterval() {
    assertEquals(AUTUMN_2024, parseInterval("ST/2024-09-02+110"));
    assertEquals(SPRING_2025, parseInterval("ST/2025-01-06+138"));
    assertEquals(AUTUMN_2025, parseInterval("ST/2025-09-01+110"));
  }

  // ── Walk form (1-based, linear over the table) ────────────────────

  @Test
  void walkOneIsTheTermContainingTheAnchor() {
    assertEquals(SPRING_2024, parseInterval("ST/2024-01-08:1"));
    assertEquals(SPRING_2024, parseInterval("ST/2024-03-15:1"));   // mid-term anchor
    assertEquals(SPRING_2024, parseInterval("ST/2024-05-24:1"));   // last-day anchor
  }

  @Test
  void eachYearAnchorResolvesItsOwnTerms() {
    assertEquals(SPRING_2024, parseInterval("ST/2024:1"));
    assertEquals(AUTUMN_2024, parseInterval("ST/2024:2"));
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("ST/2024:3"));

    assertEquals(SPRING_2025, parseInterval("ST/2025:1"));
    assertEquals(AUTUMN_2025, parseInterval("ST/2025:2"));
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("ST/2025:3"));
  }

  @Test
  void positionPastTermsWithinAnchorYearThrows() {
    // 2025 has only 2 terms; :3 has no candidate.
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("ST/2025:3"));
  }

  @Test
  void walkPastLastTermFromLastTermThrows() {
    // Anchoring at the last term: :1 succeeds, :2 has no next.
    assertEquals(AUTUMN_2025, parseInterval("ST/2025-09-01:1"));
    assertThrows(IllegalArgumentException.class,
        () -> PARSER.parse("ST/2025-09-01:2"));
  }

  // ── Walk is interval-relative, not day-relative ───────────────────

  @Test
  void differentDayAnchorsWithinSameTermResolveSame() {
    // Jan 8, Mar 15, and May 24 are all inside Spring 2024.
    // :1 from any of them must resolve to that term.
    assertEquals(SPRING_2024, parseInterval("ST/2024-01-08:1"));
    assertEquals(SPRING_2024, parseInterval("ST/2024-03-15:1"));
    assertEquals(SPRING_2024, parseInterval("ST/2024-05-24:1"));
  }

  @Test
  void dayAnchorCannotWalkPastContainingTerm() {
    // A single day overlaps exactly one term; :2 has no candidate.
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("ST/2024-03-15:2"));
  }

  // ── Gap anchors ───────────────────────────────────────────────────

  @Test
  void yearAnchorResolvesTermsWithinTheYear() {
    assertEquals(SPRING_2024, parseInterval("ST/2024:1"));
    assertEquals(AUTUMN_2024, parseInterval("ST/2024:2"));
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("ST/2024:3"));
  }

  @Test
  void dayAnchorResolvesToContainingTerm() {
    assertEquals(SPRING_2024, parseInterval("ST/2024-03-15:1"));
    // Only one term overlaps a single day; :2 has no candidate.
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("ST/2024-03-15:2"));
  }

  @Test
  void dayAnchorOutsideAnyTermThrows() {
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("ST/2024-07-01:1"));
  }

  @Test
  void dayAnchorInGapThrows() {
    // July 1, 2024 — no term contains that day.
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("ST/2024-07-01:1"));
  }

  @Test
  void preRangeAnchorThrows() {
    // 2023 contains no term — the walk is scoped to the anchor's period.
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("ST/2023:1"));
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("ST/2023-01-01:1"));
  }

  @Test
  void anchorInGapThrows() {
    // July 1 is between Spring and Autumn 2024 — no term contains it.
    assertThrows(IllegalArgumentException.class,
        () -> PARSER.parse("ST/2024-07-01:1"));
    assertThrows(IllegalArgumentException.class,
        () -> PARSER.parse("ST/2024-07-01:2"));
  }

  @Test
  void dayAfterTermEndsIsInTheGap() {
    // Spring 2024 ends May 24; May 25 is uncovered.
    assertThrows(IllegalArgumentException.class,
        () -> PARSER.parse("ST/2024-05-25:1"));
  }

  @Test
  void anchorBeforeFirstTermThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> PARSER.parse("ST/2023-12-31:1"));
  }

  @Test
  void anchorAfterLastTermThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> PARSER.parse("ST/2026-01-01:1"));
  }

  // ── Boundary anchors (inclusive) ──────────────────────────────────

  @Test
  void lastDayOfTermIsStillInTheTerm() {
    assertEquals(SPRING_2024, parseInterval("ST/2024-05-24:1"));
    assertEquals(AUTUMN_2024, parseInterval("ST/2024-12-20:1"));
    assertEquals(AUTUMN_2025, parseInterval("ST/2025-12-19:1"));
  }

  // ── Position validation ───────────────────────────────────────────

  @Test
  void walkZeroRejected() {
    assertThrows(IllegalArgumentException.class,
        () -> PARSER.parse("ST/2024-01-08:0"));
  }

  @Test
  void walkNegativeRejected() {
    assertThrows(IllegalArgumentException.class,
        () -> PARSER.parse("ST/2024-01-08:-1"));
  }

  // ── Registry interaction ──────────────────────────────────────────

  @Test
  void unregisteredKeyThrows() {
    // "SU/" is not in this parser's registry — only "ST/" is.
    assertThrows(IllegalArgumentException.class,
        () -> PARSER.parse("SU/2024-01-08:1"));
  }

  @Test
  void walkResolvesToCanonicalExplicitForm() {
    // Year anchors: n-th term within 2024 / 2025.
    assertEquals("ST/2024-01-08+138", PARSER.parse("ST/2024:1").toString());
    assertEquals("ST/2024-09-02+110", PARSER.parse("ST/2024:2").toString());
    assertEquals("ST/2025-01-06+138", PARSER.parse("ST/2025:1").toString());
    assertEquals("ST/2025-09-01+110", PARSER.parse("ST/2025:2").toString());
  }

  @Test
  void canonicalFormReParsesToSameInterval() {
    for (String s : List.of(
        "ST/2024:1", "ST/2024:2", "ST/2025:1", "ST/2025:2",
        "ST/2024-10-15:1",                       // day anchor, :1 only
        "ST/2024-01-08+138", "ST/2024-09-02+110")) {
      var first = PARSER.parse(s);
      var second = PARSER.parse(first.toString());
      assertEquals(first.interval(), second.interval(),
          () -> "round-trip failed for: " + s);
    }
  }

  @Test
  void canonicalFormIsIndependentOfWalkPath() {
    // Autumn 2024 reachable as :2 from 2024, or :1 from a day inside the term.
    var viaYear = PARSER.parse("ST/2024:2").toString();
    var viaDay  = PARSER.parse("ST/2024-10-15:1").toString();
    assertEquals(viaYear, viaDay);
  }

  @Test
  void sameTermReachableByDifferentWalks() {
    assertEquals(AUTUMN_2024, parseInterval("ST/2024:2"));
    assertEquals(AUTUMN_2024, parseInterval("ST/2024-09-02:1"));
    assertEquals(AUTUMN_2024, parseInterval("ST/2024-10-15:1"));
  }

  @Test
  void walkTwoIsTheNextTermRegardlessOfGap() {
    // Gap is invisible to the walk: :2 within 2024 is the second term, regardless
    // of the summer break between them.
    assertEquals(AUTUMN_2024, parseInterval("ST/2024:2"));
  }

  @Test
  void eachYearAnchorReachesItsOwnTerms() {
    assertEquals(SPRING_2024, parseInterval("ST/2024:1"));
    assertEquals(AUTUMN_2024, parseInterval("ST/2024:2"));
    assertEquals(SPRING_2025, parseInterval("ST/2025:1"));
    assertEquals(AUTUMN_2025, parseInterval("ST/2025:2"));
  }

  @Test
  void differentDayAnchorsWithinSameTermProduceSameResolution() {
    // Day anchors with :1 resolve to the containing term.
    assertEquals(SPRING_2024, parseInterval("ST/2024-01-08:1"));
    assertEquals(SPRING_2024, parseInterval("ST/2024-03-15:1"));
    assertEquals(SPRING_2024, parseInterval("ST/2024-05-24:1"));
  }

  @Test
  void customSequenceAndCalendarCoexistInRegistry() {
    var combined = new TimeSequenceParser(List.of(
        SCHOOL_TERMS,
        GregorianSequences.GQ_,
        GregorianSequences.GY_));

    assertEquals(SPRING_2024, combined.parse("ST/2024:1").interval());
    assertEquals(AUTUMN_2024, combined.parse("ST/2024:2").interval());

    assertEquals(new Epoch2kInterval(ofYMD(2024, 1, 1), ofYMD(2024, 3, 31)),
        combined.parse("GQ/2024:1").interval());

    assertEquals("ST/2024-09-02+110", combined.parse("ST/2024:2").toString());
    assertEquals("GQ/2024-07-01+92", combined.parse("GQ/2024:3").toString());
  }

  @Test
  void yearAnchorBeforeFirstTermThrows() {
    // 2023 contains no term — the walk is scoped to the anchor's period.
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("ST/2023:1"));
  }

  @Test
  void monthAnchorInGapThrows() {
    // July 2024 — no term overlaps that month.
    assertThrows(IllegalArgumentException.class, () -> PARSER.parse("ST/2024-07:1"));
  }

  @Test
  void noSuffixOnCustomSequenceResolvesViaIntervalOrAfter() {
    assertEquals(SPRING_2024, parseInterval("ST/2024"));
    assertEquals(SPRING_2024, parseInterval("ST/2024-03-15"));
    assertEquals(AUTUMN_2024, parseInterval("ST/2024-09-15"));
  }
}