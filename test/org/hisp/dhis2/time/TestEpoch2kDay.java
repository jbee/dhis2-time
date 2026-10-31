package org.hisp.dhis2.time;

import static java.lang.Integer.parseInt;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TestEpoch2kDay {

  @Test
  void testYear() {
    assertDayEquals("2000-01-01", new Epoch2kDay(0));
    assertDayEquals("2000-01-02", new Epoch2kDay(1));
    assertDayEquals("2000-01-31", new Epoch2kDay(30));
    assertDayEquals("2000-02-29", new Epoch2kDay(30+29));
    assertDayEquals("2000-02-01", new Epoch2kDay(31));
    assertDayEquals("2000-12-31", new Epoch2kDay(365));
    assertDayEquals("2001-01-01", new Epoch2kDay(366));
    assertDayEquals("2001-12-31", new Epoch2kDay(365+365));
    assertDayEquals("2002-01-01", new Epoch2kDay(366+365));
  }

  @Test
  void testEpoch() {
    assertDayEquals("1970-01-02", Epoch2kDay.today(() -> 1));
  }

  private void assertDayEquals(String expected, Epoch2kDay actual) {
    Epoch2kDay.OffsetYMD offset = actual.offset();
    assertEquals(expected, offset.toString());
    String[] yearMonthDay = expected.split("-");
    assertEquals(parseInt(yearMonthDay[0]), offset.years());
    assertEquals(parseInt(yearMonthDay[1])-1, offset.months());
    assertEquals(parseInt(yearMonthDay[2])-1, offset.days());
  }
}
