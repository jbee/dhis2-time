package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.Epoch2kDay.ofYMD;
import static org.hisp.dhis2.time.GregorianSequences.*;
import static org.hisp.dhis2.time.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class QuarterlyGregorianSequenceTest {

  @Test
  void calendarQuarterQ1() {
    assertInterval(GQ_.interval(ofYMD(2024, 2, 15)), 2024, 1, 1, 2024, 3, 31);
  }

  @Test
  void calendarQuarterQ2() {
    assertInterval(GQ_.interval(ofYMD(2024, 5, 15)), 2024, 4, 1, 2024, 6, 30);
  }

  @Test
  void calendarQuarterQ3() {
    assertInterval(GQ_.interval(ofYMD(2024, 8, 15)), 2024, 7, 1, 2024, 9, 30);
  }

  @Test
  void calendarQuarterQ4() {
    assertInterval(GQ_.interval(ofYMD(2024, 11, 15)), 2024, 10, 1, 2024, 12, 31);
  }

  @Test
  void julyAnchoredQuarterContainsAugust() {
    var q = quarterly(Epoch2kDay.MonthOfYear.July);
    assertInterval(q.interval(ofYMD(2024, 8, 15)), 2024, 7, 1, 2024, 9, 30);
  }

  @Test
  void julyAnchoredQuarterForJanuaryBelongsToPreviousJulyStart() {
    var q = quarterly(Epoch2kDay.MonthOfYear.July);
    // Jan 2024 falls in Q3 of the fiscal year that started Jul 2023.
    assertInterval(q.interval(ofYMD(2024, 1, 15)), 2024, 1, 1, 2024, 3, 31);
  }

  @Test
  void julyAnchoredQuarterForJuneClosesPriorCycle() {
    var q = quarterly(Epoch2kDay.MonthOfYear.July);
    assertInterval(q.interval(ofYMD(2024, 6, 15)), 2024, 4, 1, 2024, 6, 30);
  }

  @Test
  void nextFromQ4CrossesToQ1OfFollowingYear() {
    assertInterval(
        GQ_.next(GQ_.interval(ofYMD(2024, 11, 15))), 2025, 1, 1, 2025, 3, 31);
  }

  @Test
  void nextAndPreviousAreInverses() {
    var i = GQ_.interval(ofYMD(2024, 5, 15));
    assertEquals(i, GQ_.previous(GQ_.next(i)));
    assertEquals(i, GQ_.next(GQ_.previous(i)));
  }

  @Test
  void partitionYearIntoFourQuarters() {
    var y = Epoch2kInterval.ofYear(2024);
    var parts = GQ_.partition(y);
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
    assertEquals(Set.of(GY.noOffset(), GS.noOffset()), GQ_.contributesTo());
  }

  @Test
  void quarterlyDeclaresNoSixMonthPartition() {
    // SixMonth is not in Quarterly.partitions() because Quarterly(April) cannot
    // tile Semester(February) — the family key doesn't encode offset, so the
    // declaration must hold for every (quarterly offset, semester offset) pair.
    assertFalse(GQ_.partitions(GS));
  }

  @Test
  void quarterlyPartitionIsStructuralNotFamilyGated() {
    // partition() does not consult partitions(). A specific pair whose boundaries
    // align tiles successfully regardless of the declared family set.
    var janJun = GS_.interval(ofYMD(2024, 3, 15));    // Jan 1 .. Jun 30
    var parts = GQ_.partition(janJun);
    assertEquals(2, parts.size());
    assertEquals(ofYMD(2024, 1, 1), parts.get(0).firstDay());
    assertEquals(ofYMD(2024, 4, 1), parts.get(1).firstDay());

    // A six-month whose start is not a quarter boundary fails structurally.
    var febJul = semester(Epoch2kDay.MonthOfYear.February).interval(ofYMD(2024, 5, 15));
    assertThrows(IllegalArgumentException.class, () -> GQ_.partition(febJul));
  }

  @Test
  void quarterlyStillContributesToSemester() {
    var s1 = GS_.interval(ofYMD(2024, 3, 15));   // Jan-Jun 2024
    var quarters = GQ_.contributorsTo(s1);
    assertEquals(2, quarters.size());
    assertEquals(ofYMD(2024, 1, 1), quarters.get(0).firstDay());
    assertEquals(ofYMD(2024, 4, 1), quarters.get(1).firstDay());
  }
}
