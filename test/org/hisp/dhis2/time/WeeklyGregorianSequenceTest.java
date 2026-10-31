package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.GregorianSequences.*;
import static org.hisp.dhis2.time.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class WeeklyGregorianSequenceTest {

  @Test
  void mondayAnchoredWeekRunsMondayThroughSunday() {
    assertInterval(Weekly.interval(Epoch2kDay.of(2024, 1, 1)), 2024, 1, 1, 2024, 1, 7);
  }

  @Test
  void midWeekDayMapsToSameWeek() {
    assertInterval(Weekly.interval(Epoch2kDay.of(2024, 1, 3)), 2024, 1, 1, 2024, 1, 7);
    assertInterval(Weekly.interval(Epoch2kDay.of(2024, 1, 7)), 2024, 1, 1, 2024, 1, 7);
  }

  @Test
  void sundayAnchoredWeekEndsOnSaturday() {
    var sunday = weekly(Epoch2kDay.DayOfWeek.Sunday);
    assertInterval(sunday.interval(Epoch2kDay.of(2024, 1, 1)), 2023, 12, 31, 2024, 1, 6);
  }

  @Test
  void everyDayIsContainedInItsWeek() {
    for (var d :
        new Epoch2kDay[] {
          Epoch2kDay.of(2000, 1, 1),
          Epoch2kDay.of(2024, 2, 29),
          Epoch2kDay.of(2024, 12, 31),
          Epoch2kDay.of(2025, 1, 1)
        }) {
      assertContains(Weekly.interval(d), d);
    }
  }

  @Test
  void nextIsFollowingWeek() {
    assertInterval(
        Weekly.next(Weekly.interval(Epoch2kDay.of(2024, 1, 1))), 2024, 1, 8, 2024, 1, 14);
  }

  @Test
  void previousIsPrecedingWeek() {
    assertInterval(
        Weekly.previous(Weekly.interval(Epoch2kDay.of(2024, 1, 1))), 2023, 12, 25, 2023, 12, 31);
  }

  @Test
  void nextAndPreviousAreInverses() {
    var i = Weekly.interval(Epoch2kDay.of(2024, 6, 15));
    assertEquals(i, Weekly.previous(Weekly.next(i)));
    assertEquals(i, Weekly.next(Weekly.previous(i)));
  }

  @Test
  void partitionBiWeekIntoTwoWeeks() {
    var biWeek = interval(2024, 1, 1, 2024, 1, 14);
    var parts = Weekly.partition(biWeek);
    assertEquals(2, parts.size());
    assertInterval(parts.get(0), 2024, 1, 1, 2024, 1, 7);
    assertInterval(parts.get(1), 2024, 1, 8, 2024, 1, 14);
  }

  @Test
  void partitionMonthThrowsBecauseWeeksStraddleMonths() {
    var jan = interval(2024, 1, 1, 2024, 1, 31);
    assertThrows(IllegalArgumentException.class, () -> Weekly.partition(jan));
  }

  @Test
  void membersOfMonthUsesMajorityForBoundaryWeeks() {
    // June 2024: Jun 1 is Saturday. Week containing Jun 1 runs May 27–Jun 2,
    // with only 2 of 7 days in June → excluded.
    var june = interval(2024, 6, 1, 2024, 6, 30);
    var weeks = Weekly.membersOf(june);
    assertEquals(4, weeks.size());
    assertEquals(Epoch2kDay.of(2024, 6, 3), weeks.get(0).first());
  }

  @Test
  void membersOfMonthIncludesBoundaryWeeksWithMajorityDays() {
    // Aug 2024: Aug 1 is Thursday. Week containing Aug 1 runs Jul 29–Aug 4,
    // with 4 of 7 days in Aug → included.
    var aug = interval(2024, 8, 1, 2024, 8, 31);
    var weeks = Weekly.membersOf(aug);
    assertEquals(5, weeks.size());
    assertEquals(Epoch2kDay.of(2024, 7, 29), weeks.get(0).first());
  }

  @Test
  void partitionsContainsOnlyBiWeek() {
    assertEquals(Set.of(BiWeek), Weekly.partitions());
  }

  @Test
  void aggregatesToContainsBiWeekMonthQuarterYear() {
    assertEquals(Set.of(BiWeek, Month, Quarter, Year), Weekly.aggregatesTo());
  }

  @Test
  void compactConstructorRejectsKeyWithoutDayOffset() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new GregorianSequences.WeeklyGregorianSequence(
                new TimeSequenceKey('G', 'W'),
                Epoch2kDay.DayOfWeek.Monday,
                Set.of(BiWeek),
                Set.of(BiWeek)));
  }

  @Test
  void compactConstructorRejectsKeyAndRecordDayMismatch() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new GregorianSequences.WeeklyGregorianSequence(
                TimeSequenceKey.weekly('G', Epoch2kDay.DayOfWeek.Monday),
                Epoch2kDay.DayOfWeek.Tuesday,
                Set.of(BiWeek),
                Set.of(BiWeek)));
  }
}
