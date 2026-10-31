package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.EthiopianCalendar.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TestEthiopianCalendar {

  @Test
  void anchor2000_01_01IsTahsas22_1992() {
    Epoch2kDay d = new Epoch2kDay(0);
    assertEquals(1992, year(d));
    assertEquals(4, month(d));       // Tahsas
    assertEquals(22, dayOfMonth(d));
  }

  @Test
  void newYear1992Is1999_09_12() {
    assertEquals(-111, newYearOffset(1992));
  }

  @Test
  void newYear1993Is2000_09_11() {
    // 1992 is not leap (1992 % 4 == 0), so New Year 1993 is the 11th.
    assertEquals(254, newYearOffset(1993));
  }

  @Test
  void newYear1995Is2002_09_11() {
    // 1995 is leap (1995 % 4 == 3), but the leap day is in Pagume 1995,
    // so Meskerem 1995 itself is still the 11th.
    assertEquals(984, newYearOffset(1995));
  }

  @Test
  void newYear1996Is2003_09_12() {
    // 1995 was leap, so New Year 1996 shifts to the 12th.
    assertEquals(1350, newYearOffset(1996));
  }

  @Test
  void leapYearsFollowMod4Equals3() {
    assertTrue(isLeapYear(1995));
    assertTrue(isLeapYear(1999));
    assertFalse(isLeapYear(1992));
    assertFalse(isLeapYear(1993));
    assertFalse(isLeapYear(1994));
    assertFalse(isLeapYear(1996));
  }

  @Test
  void pagumeHas5Or6Days() {
    assertEquals(5, daysInMonth(1992, 13));
    assertEquals(6, daysInMonth(1995, 13));
    assertEquals(30, daysInMonth(1995, 1));
    assertEquals(30, daysInMonth(1995, 12));
  }

  @Test
  void roundTripKnownAnchors() {
    assertRoundTrip(1992, 4, 22, new Epoch2kDay(0));
    assertRoundTrip(2017, 1, 1, Epoch2kDay.ofYMD(2024, 9, 11));
    assertRoundTrip(2018, 11, 5, Epoch2kDay.ofYMD(2026, 7, 12));
    assertRoundTrip(1992, 1, 1, Epoch2kDay.ofYMD(1999, 9, 12));
  }

  @Test
  void everyDayOf1995RoundTrips() {
    int start = newYearOffset(1995);
    int end = newYearOffset(1996);
    assertEquals(366, end - start);
    for (int i = start; i < end; i++) {
      Epoch2kDay d = new Epoch2kDay(i);
      int y = year(d), m = month(d), dom = dayOfMonth(d);
      assertEquals(d, day(y, m, dom), "round-trip failed for day " + i);
    }
  }

  @Test
  void monthIntervalForTahsas1992() {
    var i = monthInterval(new Epoch2kDay(0));
    assertEquals(day(1992, 4, 1), i.first());
    assertEquals(day(1992, 4, 30), i.last());
    assertEquals(30, i.lengthInDays());
  }

  @Test
  void monthIntervalForPagumeInLeapYear() {
    // Pagume 1995 (month 13) has 6 days.
    var i = monthInterval(day(1995, 13, 3));
    assertEquals(day(1995, 13, 1), i.first());
    assertEquals(day(1995, 13, 6), i.last());
    assertEquals(6, i.lengthInDays());
  }

  @Test
  void yearIntervalFor1992Has365Days() {
    var i = yearInterval(new Epoch2kDay(0));
    assertEquals(day(1992, 1, 1), i.first());
    assertEquals(day(1992, 13, 5), i.last());
    assertEquals(365, i.lengthInDays());
  }

  @Test
  void yearIntervalFor1995Has366Days() {
    var i = yearInterval(day(1995, 6, 15));
    assertEquals(day(1995, 1, 1), i.first());
    assertEquals(day(1995, 13, 6), i.last());
    assertEquals(366, i.lengthInDays());
  }

  private static void assertRoundTrip(int y, int m, int d, Epoch2kDay expected) {
    Epoch2kDay actual = day(y, m, d);
    assertEquals(expected, actual, () -> y + "-" + m + "-" + d);
    assertEquals(y, year(actual));
    assertEquals(m, month(actual));
    assertEquals(d, dayOfMonth(actual));
  }
}