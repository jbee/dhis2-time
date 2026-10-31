package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.IslamicCalendar.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TestIslamicCalendar {

  @Test
  void goldAnchorRamadan1445Is2024_03_11() {
    // 1 Ramadan 1445 = 2024-03-11 (KACST, confirmed by the tabulated data)
    assertEquals(Epoch2kDay.ofYMD(2024, 3, 11), day(1445, 9, 1));
  }

  @Test
  void muharram1440Starts2018_09_11() {
    // 1 Muharram 1440 = 2018-09-11
    assertEquals(Epoch2kDay.ofYMD(2018, 9, 11), day(1440, 1, 1));
  }

  @Test
  void year1450Ends2029_05_13AndSentinelIs2029_05_14() {
    var range = IslamicCalendar.supportedRange();
    assertEquals(Epoch2kDay.ofYMD(2018, 9, 11), range.first());
    assertEquals(Epoch2kDay.ofYMD(2029, 5, 13), range.last());
    assertEquals(Epoch2kDay.ofYMD(2029, 5, 14), range.last().tomorrow());
  }

  @Test
  void yearJustPastSupportedRangeThrows() {
    var sentinel = IslamicCalendar.supportedRange().last().tomorrow();
    assertThrows(IllegalArgumentException.class, () -> year(sentinel));
    assertThrows(IllegalArgumentException.class, () -> year(sentinel.tomorrow()));
  }

  @Test
  void yearAndMonthAndDayOfMonthRoundTripForKnownDates() {
    assertRoundTrip(1440, 1, 1, Epoch2kDay.ofYMD(2018, 9, 11));
    assertRoundTrip(1445, 9, 1, Epoch2kDay.ofYMD(2024, 3, 11));

    var lastDay = IslamicCalendar.supportedRange().last();
    assertEquals(1450, IslamicCalendar.year(lastDay));
    assertEquals(12, IslamicCalendar.month(lastDay));
    assertEquals(29, IslamicCalendar.dayOfMonth(lastDay));
    assertRoundTrip(1450, 12, 29, lastDay);
  }

  @Test
  void everyDayOf1441RoundTrips() {
    // 1441 is a 355-day (leap) year in the Umm al-Qura table.
    assertEquals(355, daysInYear(1441));
    Epoch2kDay start = day(1441, 1, 1);
    Epoch2kDay end = day(1441, 12, daysInMonth(1441, 12));
    for (Epoch2kDay d = start; d.notAfter(end); d = d.tomorrow()) {
      int y = year(d), m = month(d), dom = dayOfMonth(d);
      assertEquals(d, day(y, m, dom), "round-trip failed for " + d);
    }
  }

  @Test
  void leapYearsInRangeAreExactlyThoseWith355Days() {
    // 1441, 1443, 1447, 1448 have 355 days; the rest have 354.
    assertTrue(isLeapYear(1441));
    assertTrue(isLeapYear(1443));
    assertTrue(isLeapYear(1447));
    assertTrue(isLeapYear(1448));
    assertFalse(isLeapYear(1440));
    assertFalse(isLeapYear(1442));
    assertFalse(isLeapYear(1444));
    assertFalse(isLeapYear(1445));
    assertFalse(isLeapYear(1446));
    assertFalse(isLeapYear(1449));
    assertFalse(isLeapYear(1450));
  }

  @Test
  void ramadan1446Has29DaysAndRamadan1447Has30() {
    // Directly from the Umm al-Qura table; no arithmetic rule would tell you this.
    assertEquals(29, daysInMonth(1446, 9));
    assertEquals(30, daysInMonth(1447, 9));
  }

  @Test
  void monthIntervalForRamadan1445() {
    var i = monthInterval(day(1445, 9, 1));
    assertEquals(day(1445, 9, 1), i.first());
    assertEquals(day(1445, 9, 30), i.last());
    assertEquals(30, i.lengthInDays());
  }

  @Test
  void yearIntervalFor1445Has354Days() {
    var i = yearInterval(day(1445, 6, 15));
    assertEquals(day(1445, 1, 1), i.first());
    assertEquals(day(1445, 12, 30), i.last());
    assertEquals(354, i.lengthInDays());
  }

  @Test
  void yearIntervalFor1447Has355Days() {
    var i = yearInterval(day(1447, 6, 15));
    assertEquals(355, i.lengthInDays());
    assertEquals(day(1447, 12, daysInMonth(1447, 12)), i.last());
  }

  @Test
  void unsupportedYearThrows() {
    assertThrows(IllegalArgumentException.class, () -> daysInMonth(1439, 1));
    assertThrows(IllegalArgumentException.class, () -> daysInMonth(1451, 1));
  }

  private static void assertRoundTrip(int y, int m, int d, Epoch2kDay expected) {
    Epoch2kDay actual = day(y, m, d);
    assertEquals(expected, actual, () -> y + "-" + m + "-" + d);
    assertEquals(y, year(actual));
    assertEquals(m, month(actual));
    assertEquals(d, dayOfMonth(actual));
  }
}