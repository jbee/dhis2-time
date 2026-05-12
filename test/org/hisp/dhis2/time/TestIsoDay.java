package org.hisp.dhis2.time;

import org.junit.jupiter.api.Test;

import static java.lang.Integer.parseInt;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TestIsoDay {

  @Test
  void testYear() {
    assertDayEquals("2000-01-01", new IsoDay(0));
    assertDayEquals("2000-01-02", new IsoDay(1));
    assertDayEquals("2000-01-31", new IsoDay(30));
    assertDayEquals("2000-02-29", new IsoDay(30+29));
    assertDayEquals("2000-02-01", new IsoDay(31));
    assertDayEquals("2000-12-31", new IsoDay(365));
    assertDayEquals("2001-01-01", new IsoDay(366));
    assertDayEquals("2001-12-31", new IsoDay(365+365));
    assertDayEquals("2002-01-01", new IsoDay(366+365));
  }

  @Test
  void testEpoch() {
    assertDayEquals("1970-01-02", IsoDay.today(() -> 1));
  }

  private void assertDayEquals(String expected, IsoDay actual) {
    IsoDay.OffsetYMD offset = actual.offset();
    assertEquals(expected, offset.toString());
    String[] yearMonthDay = expected.split("-");
    assertEquals(parseInt(yearMonthDay[0]), offset.years());
    assertEquals(parseInt(yearMonthDay[1])-1, offset.months());
    assertEquals(parseInt(yearMonthDay[2])-1, offset.days());
  }
}
