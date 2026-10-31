package org.hisp.dhis2.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestSupport {

  private TestSupport() {}

  static Epoch2kInterval interval(int y1, int m1, int d1, int y2, int m2, int d2) {
    return new Epoch2kInterval(Epoch2kDay.ofYMD(y1, m1, d1), Epoch2kDay.ofYMD(y2, m2, d2));
  }

  static void assertInterval(Epoch2kInterval i, int y1, int m1, int d1, int y2, int m2, int d2) {
    assertEquals(Epoch2kDay.ofYMD(y1, m1, d1), i.first(), "first");
    assertEquals(Epoch2kDay.ofYMD(y2, m2, d2), i.last(), "last");
  }

  static void assertContains(Epoch2kInterval i, Epoch2kDay d) {
    assertTrue(i.contains(d), () -> i + " does not contain " + d);
  }
}
