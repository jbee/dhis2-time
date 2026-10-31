package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.Epoch2kDay.ofYMD;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TestEpoch2kInterval {

  @Test
  void testContains() {
    Epoch2kInterval jan2000 = new Epoch2kInterval(new Epoch2kDay(0), new Epoch2kDay(30));
    for (int i = 0; i < 31; i++) assertTrue(jan2000.contains(new Epoch2kDay(i)));
    assertFalse(jan2000.contains(new Epoch2kDay(-1)));
    assertFalse(jan2000.contains(new Epoch2kDay(31)));
  }

  @Test
  void testOverlaps() {
    Epoch2kInterval jan2000 = new Epoch2kInterval(new Epoch2kDay(0), new Epoch2kDay(30));
    Epoch2kInterval feb2000 = new Epoch2kInterval(new Epoch2kDay(31), new Epoch2kDay(31 + 28));
    Epoch2kInterval span2000 = new Epoch2kInterval(new Epoch2kDay(30), new Epoch2kDay(50));
    assertFalse(jan2000.overlaps(feb2000));
    assertTrue(jan2000.overlaps(span2000));
    assertTrue(feb2000.overlaps(span2000));
    assertTrue(span2000.overlaps(jan2000));
    assertTrue(span2000.overlaps(feb2000));
  }

  // ── ofYear / ofYearRange ────────────────────────────────────────────

  @Test
  void ofYearSpansJanuaryToDecember() {
    var leap = Epoch2kInterval.ofYear(2024);
    assertEquals(ofYMD(2024, 1, 1), leap.firstDay());
    assertEquals(ofYMD(2024, 12, 31), leap.lastDay());
    assertEquals(366, leap.lengthInDays());

    var nonLeap = Epoch2kInterval.ofYear(2023);
    assertEquals(365, nonLeap.lengthInDays());
  }

  @Test
  void ofYearRangeSpansConsecutiveYears() {
    var i = Epoch2kInterval.ofYearRange(2024, 2025);
    assertEquals(ofYMD(2024, 1, 1), i.firstDay());
    assertEquals(ofYMD(2025, 12, 31), i.lastDay());
    assertEquals(366 + 365, i.lengthInDays());
  }

  @Test
  void ofYearRangeSingleYearEqualsOfYear() {
    assertEquals(Epoch2kInterval.ofYear(2024), Epoch2kInterval.ofYearRange(2024, 2024));
  }

  @Test
  void ofYearRangeReversedYearsThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> Epoch2kInterval.ofYearRange(2025, 2024));
  }

  // ── Constructor validation ──────────────────────────────────────────

  @Test
  void constructorRejectsNegativeLength() {
    assertThrows(IllegalArgumentException.class,
        () -> new Epoch2kInterval(ofYMD(2024, 3, 15), ofYMD(2024, 3, 14)));
  }

  @Test
  void singleDayIntervalHasLengthOne() {
    var d = ofYMD(2024, 3, 15);
    var i = new Epoch2kInterval(d, d);
    assertEquals(1, i.lengthInDays());
    assertTrue(i.contains(d));
  }

  // ── lengthInDays ────────────────────────────────────────────────────

  @Test
  void lengthInDaysOfVariousSpans() {
    assertEquals(1,
        new Epoch2kInterval(ofYMD(2024, 1, 1), ofYMD(2024, 1, 1)).lengthInDays());
    assertEquals(31,
        new Epoch2kInterval(ofYMD(2024, 1, 1), ofYMD(2024, 1, 31)).lengthInDays());
    assertEquals(7,
        new Epoch2kInterval(ofYMD(2024, 3, 11), ofYMD(2024, 3, 17)).lengthInDays());
  }

  // ── toString ────────────────────────────────────────────────────────

  @Test
  void toStringCombinesFirstDayAndLength() {
    assertEquals("2024-01-01+366", Epoch2kInterval.ofYear(2024).toString());
    assertEquals("2023-01-01+365", Epoch2kInterval.ofYear(2023).toString());
  }

  // ── Boundary cases ──────────────────────────────────────────────────

  @Test
  void containsIsInclusiveOnBothEnds() {
    var i = new Epoch2kInterval(ofYMD(2024, 3, 11), ofYMD(2024, 3, 17));
    assertTrue(i.contains(ofYMD(2024, 3, 11)));
    assertTrue(i.contains(ofYMD(2024, 3, 14)));
    assertTrue(i.contains(ofYMD(2024, 3, 17)));
    assertFalse(i.contains(ofYMD(2024, 3, 10)));
    assertFalse(i.contains(ofYMD(2024, 3, 18)));
  }

  @Test
  void overlapsIncludesEdgeTouching() {
    var i1 = new Epoch2kInterval(ofYMD(2024, 3, 11), ofYMD(2024, 3, 17));
    var i2 = new Epoch2kInterval(ofYMD(2024, 3, 17), ofYMD(2024, 3, 23));
    assertTrue(i1.overlaps(i2)); // share 3/17
    assertTrue(i2.overlaps(i1));

    var i3 = new Epoch2kInterval(ofYMD(2024, 3, 18), ofYMD(2024, 3, 24));
    assertFalse(i1.overlaps(i3)); // adjacent, no overlap
    assertFalse(i3.overlaps(i1));
  }

  @Test
  void overlapsIsSymmetric() {
    var a = new Epoch2kInterval(ofYMD(2024, 1, 1), ofYMD(2024, 6, 30));
    var b = new Epoch2kInterval(ofYMD(2024, 3, 1), ofYMD(2024, 9, 30));
    var c = new Epoch2kInterval(ofYMD(2024, 7, 1), ofYMD(2024, 12, 31));
    assertTrue(a.overlaps(b));
    assertEquals(a.overlaps(b), b.overlaps(a));
    assertFalse(a.overlaps(c));
    assertEquals(a.overlaps(c), c.overlaps(a));
  }
}