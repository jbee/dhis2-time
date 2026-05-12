package org.hisp.dhis2.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestIsoInterval {

  @Test
  void testContains() {
    IsoInterval jan2000 = new IsoInterval(new IsoDay(0), new IsoDay(30));
    for (int i = 0; i< 31; i++)
      assertTrue(jan2000.contains(new IsoDay(i)));
    assertFalse(jan2000.contains(new IsoDay(-1)));
    assertFalse(jan2000.contains(new IsoDay(31)));
  }

  @Test
  void testOverlaps() {
    IsoInterval jan2000 = new IsoInterval(new IsoDay(0), new IsoDay(30));
    IsoInterval feb2000 = new IsoInterval(new IsoDay(31), new IsoDay(31+28));
    IsoInterval span2000 = new IsoInterval(new IsoDay(30), new IsoDay(50));
    assertFalse(jan2000.overlaps(feb2000));
    assertTrue(jan2000.overlaps(span2000));
    assertTrue(feb2000.overlaps(span2000));
    assertTrue(span2000.overlaps(jan2000));
    assertTrue(span2000.overlaps(feb2000));
  }
}
