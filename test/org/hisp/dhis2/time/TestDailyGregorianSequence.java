package org.hisp.dhis2.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestDailyGregorianSequence {

  @Test
  void testNext() {
    TimeSequence daily = GregorianSequences.Daily;
    Epoch2kInterval epochDay1 = daily.today(() -> 0);
    assertEquals("1970-01-01+1", epochDay1.toString());
    Epoch2kInterval epochDay2 = daily.next(epochDay1);
    Epoch2kInterval epochDay3 = daily.next(epochDay2);
    assertEquals("1970-01-02+1", epochDay2.toString());
    assertEquals("1970-01-03+1", epochDay3.toString());
  }

  @Test
  void testPrevious() {
    TimeSequence daily = GregorianSequences.Daily;
    Epoch2kInterval epochDay1 = daily.today(() -> 0);
    assertEquals("1970-01-01+1", epochDay1.toString());
    Epoch2kInterval epochDay2 = daily.previous(epochDay1);
    Epoch2kInterval epochDay3 = daily.previous(epochDay2);
    assertEquals("1969-12-31+1", epochDay2.toString());
    assertEquals("1969-12-30+1", epochDay3.toString());
  }
}
