package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.CustomSequences.of;
import static org.hisp.dhis2.time.Epoch2kDay.ofYMD;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CustomSequencesTest {

  // School terms 2024: Spring (Jan 8 – May 24), Autumn (Sep 2 – Dec 20).
  private static final Epoch2kInterval SPRING =
      new Epoch2kInterval(ofYMD(2024, 1, 8), ofYMD(2024, 5, 24));
  private static final Epoch2kInterval AUTUMN =
      new Epoch2kInterval(ofYMD(2024, 9, 2), ofYMD(2024, 12, 20));

  private static final TimeSequence TERMS =
      of(
          new TimeSequenceKey('S', 'Y'),
          List.of(SPRING, AUTUMN),
          Set.of(),
          Set.of(GregorianSequences.GY.noOffset()));

  // ── interval ───────────────────────────────────────────────────────

  @Test
  void intervalInsideSpringTerm() {
    assertEquals(SPRING, TERMS.interval(ofYMD(2024, 3, 15)));
  }

  @Test
  void intervalOnFirstDayOfAutumnTerm() {
    assertEquals(AUTUMN, TERMS.interval(ofYMD(2024, 9, 2)));
  }

  @Test
  void intervalOnLastDayOfAutumnTerm() {
    assertEquals(AUTUMN, TERMS.interval(ofYMD(2024, 12, 20)));
  }

  @Test
  void intervalInSummerGapThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> TERMS.interval(ofYMD(2024, 7, 1)));
  }

  @Test
  void intervalBeforeFirstTermThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> TERMS.interval(ofYMD(2023, 12, 31)));
  }

  @Test
  void intervalAfterLastTermThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> TERMS.interval(ofYMD(2025, 1, 1)));
  }

  // ── next / previous ────────────────────────────────────────────────

  @Test
  void nextFromSpringIsAutumn() {
    assertEquals(AUTUMN, TERMS.next(SPRING));
  }

  @Test
  void previousFromAutumnIsSpring() {
    assertEquals(SPRING, TERMS.previous(AUTUMN));
  }

  @Test
  void nextFromLastTermThrows() {
    assertThrows(NoSuchElementException.class, () -> TERMS.next(AUTUMN));
  }

  @Test
  void previousFromFirstTermThrows() {
    assertThrows(NoSuchElementException.class, () -> TERMS.previous(SPRING));
  }

  @Test
  void nextFromIntervalNotInTableFindsNextTableEntry() {
    // A hand-crafted reference between Spring and Autumn.
    var between = new Epoch2kInterval(ofYMD(2024, 6, 15), ofYMD(2024, 7, 15));
    assertEquals(AUTUMN, TERMS.next(between));
  }

  @Test
  void nextAndPreviousAreNotInversesAcrossGaps() {
    // previous(next(spring)) == spring, but next(previous(autumn)) == autumn — both hold.
    assertEquals(SPRING, TERMS.previous(TERMS.next(SPRING)));
    assertEquals(AUTUMN, TERMS.next(TERMS.previous(AUTUMN)));
  }

  // ── membersOf ──────────────────────────────────────────────────────

  @Test
  void membersOfGregorianYearReturnsBothTerms() {
    var year = Epoch2kInterval.ofYear(2024);
    assertEquals(List.of(SPRING, AUTUMN), TERMS.contributorsTo(year));
  }

  @Test
  void membersOfGregorianYearIgnoresGaps() {
    // The default implementation would throw when the day walk hits the summer
    // gap; the override iterates the table instead and succeeds.
    var year = Epoch2kInterval.ofYear(2024);
    assertDoesNotThrow(() -> TERMS.contributorsTo(year));
  }

  @Test
  void membersOfYearOutsideAnyTermReturnsEmpty() {
    var year = Epoch2kInterval.ofYear(2022);
    assertTrue(TERMS.contributorsTo(year).isEmpty());
  }

  // ── partition ──────────────────────────────────────────────────────

  @Test
  void partitionSingleTermReturnsItself() {
    assertEquals(List.of(SPRING), TERMS.partition(SPRING));
  }

  @Test
  void partitionAdjacentTermsReturnsBoth() {
    var first = new Epoch2kInterval(ofYMD(2024, 1, 1), ofYMD(2024, 3, 31));
    var second = new Epoch2kInterval(ofYMD(2024, 4, 1), ofYMD(2024, 6, 30));
    var seq = of(new TimeSequenceKey('S', 'T'), List.of(first, second));
    assertEquals(List.of(first, second),
        seq.partition(new Epoch2kInterval(first.firstDay(), second.lastDay())));
  }

  @Test
  void partitionRangeSpanningGapThrows() {
    var span = new Epoch2kInterval(SPRING.firstDay(), AUTUMN.lastDay());
    assertThrows(IllegalArgumentException.class, () -> TERMS.partition(span));
  }

  // ── Construction validation ────────────────────────────────────────

  @Test
  void emptyIntervalListRejected() {
    TimeSequenceKey key = new TimeSequenceKey('S', 'Y');
    assertThrows(IllegalArgumentException.class, () -> of(key, List.of()));
  }

  @Test
  void overlappingIntervalsRejected() {
    var a = new Epoch2kInterval(ofYMD(2024, 1, 1), ofYMD(2024, 3, 31));
    var b = new Epoch2kInterval(ofYMD(2024, 3, 15), ofYMD(2024, 6, 30));
    TimeSequenceKey key = new TimeSequenceKey('S', 'Y');
    assertThrows(IllegalArgumentException.class,
        () -> of(key, List.of(a, b)));
  }

  @Test
  void constructionSortsUnorderedInput() {
    // Pass Autumn first, then Spring — the constructor sorts.
    TimeSequenceKey key = new TimeSequenceKey('S', 'Y');
    var seq = of(key, List.of(AUTUMN, SPRING));
    assertEquals(SPRING, seq.next(new Epoch2kInterval(ofYMD(2023, 1, 1), ofYMD(2023, 12, 31))));
  }

  @Test
  void adjacentIntervalsAllowed() {
    var a = new Epoch2kInterval(ofYMD(2024, 1, 1), ofYMD(2024, 3, 31));
    var b = new Epoch2kInterval(ofYMD(2024, 4, 1), ofYMD(2024, 6, 30));
    TimeSequenceKey key = new TimeSequenceKey('S', 'Y');
    assertDoesNotThrow(() -> of(key, List.of(a, b)));
  }

  // ── Multi-year and integration ────────────────────────────────────

  @Test
  void nextCrossesYearBoundaryWhenTableDoes() {
    var autumn2024 = AUTUMN;
    var spring2025 = new Epoch2kInterval(ofYMD(2025, 1, 6), ofYMD(2025, 5, 23));
    TimeSequenceKey key = new TimeSequenceKey('S', 'Y');
    var seq = of(key, List.of(SPRING, autumn2024, spring2025));
    assertEquals(spring2025, seq.next(autumn2024));
  }

  @Test
  void aggregatesToGregorianYearIsConsistentWithMembership() {
    // Every term is fully inside one Gregorian year, so Gregorian Year is a
    // declared aggregation target, and membersOf agrees.
    var year = Epoch2kInterval.ofYear(2024);
    for (var member : TERMS.contributorsTo(year)) {
      assertTrue(year.contains(member.firstDay()));
      assertTrue(year.contains(member.lastDay()));
    }
  }

  @Test
  void customSequenceCollidingWithCalendarKeyRejectedAtRegistryConstruction() {
    var gregorianYear = GregorianSequences.GY_;   // key "GY/"
    var fakeCustom = CustomSequences.of(
        GregorianSequences.GY.noOffset(),         // also "GY/"
        List.of(new Epoch2kInterval(ofYMD(2024, 1, 8), ofYMD(2024, 5, 24))));

    assertThrows(IllegalArgumentException.class,
        () -> new TimeSliceParser(List.of(gregorianYear, fakeCustom)));
  }
}