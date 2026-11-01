package org.hisp.dhis2.time;

import org.junit.jupiter.api.Test;

import static org.hisp.dhis2.time.Epoch2kDay.ofYMD;
import static org.hisp.dhis2.time.GregorianSequences.FH4;
import static org.hisp.dhis2.time.GregorianSequences.FY;
import static org.hisp.dhis2.time.GregorianSequences.GS_;
import static org.hisp.dhis2.time.GregorianSequences.GY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemesterGregorianSequenceTest {

  @Test
  void semesterTilesYear() {
    var year = Epoch2kInterval.ofYear(2024);
    var parts = GS_.partition(year);
    assertEquals(2, parts.size());
    assertEquals(ofYMD(2024, 1, 1), parts.get(0).firstDay());
    assertEquals(ofYMD(2024, 7, 1), parts.get(1).firstDay());
  }

  @Test
  void fiscalSemesterAggregatesToFiscalYear() {
    assertTrue(FH4.contributesTo(FY));
    assertTrue(FH4.partitions(FY));
    assertFalse(FH4.contributesTo(GY));
  }

  @Test
  void semesterAprilStartsApril() {
    var s1 = FH4.interval(ofYMD(2024, 5, 15));
    assertEquals(ofYMD(2024, 4, 1), s1.firstDay());
    assertEquals(ofYMD(2024, 9, 30), s1.lastDay());
  }

  @Test
  void nextAndPreviousInverseForSemester() {
    var i = GS_.interval(ofYMD(2024, 8, 15));
    assertEquals(i, GS_.next(GS_.previous(i)));
    assertEquals(i, GS_.previous(GS_.next(i)));
  }
}
