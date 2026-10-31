package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.GregorianSequences.*;
import static org.hisp.dhis2.time.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class QuarterlyGregorianSequenceTest {

  @Test
  void calendarQuarterQ1() {
    assertInterval(Quarterly.interval(Epoch2kDay.ofYMD(2024, 2, 15)), 2024, 1, 1, 2024, 3, 31);
  }

  @Test
  void calendarQuarterQ2() {
    assertInterval(Quarterly.interval(Epoch2kDay.ofYMD(2024, 5, 15)), 2024, 4, 1, 2024, 6, 30);
  }

  @Test
  void calendarQuarterQ3() {
    assertInterval(Quarterly.interval(Epoch2kDay.ofYMD(2024, 8, 15)), 2024, 7, 1, 2024, 9, 30);
  }

  @Test
  void calendarQuarterQ4() {
    assertInterval(Quarterly.interval(Epoch2kDay.ofYMD(2024, 11, 15)), 2024, 10, 1, 2024, 12, 31);
  }

  @Test
  void julyAnchoredQuarterContainsAugust() {
    var q = quarterly(Epoch2kDay.MonthOfYear.July);
    assertInterval(q.interval(Epoch2kDay.ofYMD(2024, 8, 15)), 2024, 7, 1, 2024, 9, 30);
  }

  @Test
  void julyAnchoredQuarterForJanuaryBelongsToPreviousJulyStart() {
    var q = quarterly(Epoch2kDay.MonthOfYear.July);
    // Jan 2024 falls in Q3 of the fiscal year that started Jul 2023.
    assertInterval(q.interval(Epoch2kDay.ofYMD(2024, 1, 15)), 2024, 1, 1, 2024, 3, 31);
  }

  @Test
  void julyAnchoredQuarterForJuneClosesPriorCycle() {
    var q = quarterly(Epoch2kDay.MonthOfYear.July);
    assertInterval(q.interval(Epoch2kDay.ofYMD(2024, 6, 15)), 2024, 4, 1, 2024, 6, 30);
  }

  @Test
  void nextFromQ4CrossesToQ1OfFollowingYear() {
    assertInterval(
        Quarterly.next(Quarterly.interval(Epoch2kDay.ofYMD(2024, 11, 15))), 2025, 1, 1, 2025, 3, 31);
  }

  @Test
  void nextAndPreviousAreInverses() {
    var i = Quarterly.interval(Epoch2kDay.ofYMD(2024, 5, 15));
    assertEquals(i, Quarterly.previous(Quarterly.next(i)));
    assertEquals(i, Quarterly.next(Quarterly.previous(i)));
  }

  @Test
  void partitionYearIntoFourQuarters() {
    var y = Epoch2kInterval.ofYear(2024);
    var parts = Quarterly.partition(y);
    assertEquals(4, parts.size());
    assertInterval(parts.get(0), 2024, 1, 1, 2024, 3, 31);
    assertInterval(parts.get(3), 2024, 10, 1, 2024, 12, 31);
  }

  @Test
  void factoryUsesNoOffsetForJanuary() {
    assertEquals('-', quarterly(Epoch2kDay.MonthOfYear.January).key().offset());
  }

  @Test
  void factoryEncodesNonJanuaryStartAsDigit() {
    assertEquals('4', quarterly(Epoch2kDay.MonthOfYear.April).key().offset());
  }

  @Test
  void fiscalUsesSystemF() {
    var q = fiscalQuarterly(Epoch2kDay.MonthOfYear.July);
    assertEquals('F', q.key().system());
    assertEquals('7', q.key().offset());
  }

  @Test
  void aggregatesToContainsYear() {
    assertEquals(Set.of(Year, SixMonth), Quarterly.contributesTo());
  }

  @Test
  void compactConstructorRejectsKeyAndRecordMonthMismatch() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new GregorianSequences.QuarterlyGregorianSequence(
                TimeSequenceKey.quarterly('G', Epoch2kDay.MonthOfYear.July),
                Epoch2kDay.MonthOfYear.April,
                Set.of(Year),
                Set.of(Year)));
  }
}
