package org.hisp.dhis2.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
