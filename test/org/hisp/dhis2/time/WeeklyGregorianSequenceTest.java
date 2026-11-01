package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.GregorianSequences.*;
import static org.hisp.dhis2.time.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class WeeklyGregorianSequenceTest {

  @Test
  void mondayAnchoredWeekRunsMondayThroughSunday() {
    assertInterval(GW_.interval(Epoch2kDay.ofYMD(2024, 1, 1)), 2024, 1, 1, 2024, 1, 7);
  }

  @Test
  void midWeekDayMapsToSameWeek() {
    assertInterval(GW_.interval(Epoch2kDay.ofYMD(2024, 1, 3)), 2024, 1, 1, 2024, 1, 7);
    assertInterval(GW_.interval(Epoch2kDay.ofYMD(2024, 1, 7)), 2024, 1, 1, 2024, 1, 7);
  }

  @Test
  void sundayAnchoredWeekEndsOnSaturday() {
    var sunday = weekly(Epoch2kDay.DayOfWeek.Sunday);
    assertInterval(sunday.interval(Epoch2kDay.ofYMD(2024, 1, 1)), 2023, 12, 31, 2024, 1, 6);
  }

  @Test
  void everyDayIsContainedInItsWeek() {
    for (var d :
        new Epoch2kDay[] {
          Epoch2kDay.ofYMD(2000, 1, 1),
          Epoch2kDay.ofYMD(2024, 2, 29),
          Epoch2kDay.ofYMD(2024, 12, 31),
          Epoch2kDay.ofYMD(2025, 1, 1)
        }) {
      assertContains(GW_.interval(d), d);
    }
  }

  @Test
  void nextIsFollowingWeek() {
    assertInterval(
        GW_.next(GW_.interval(Epoch2kDay.ofYMD(2024, 1, 1))), 2024, 1, 8, 2024, 1, 14);
  }

  @Test
  void previousIsPrecedingWeek() {
    assertInterval(
        GW_.previous(GW_.interval(Epoch2kDay.ofYMD(2024, 1, 1))), 2023, 12, 25, 2023, 12, 31);
  }

  @Test
  void nextAndPreviousAreInverses() {
    var i = GW_.interval(Epoch2kDay.ofYMD(2024, 6, 15));
    assertEquals(i, GW_.previous(GW_.next(i)));
    assertEquals(i, GW_.next(GW_.previous(i)));
  }

  @Test
  void partitionBiWeekIntoTwoWeeks() {
    var biWeek = interval(2024, 1, 1, 2024, 1, 14);
    var parts = GW_.partition(biWeek);
    assertEquals(2, parts.size());
    assertInterval(parts.get(0), 2024, 1, 1, 2024, 1, 7);
    assertInterval(parts.get(1), 2024, 1, 8, 2024, 1, 14);
  }

  @Test
  void partitionMonthThrowsBecauseWeeksStraddleMonths() {
    var jan = interval(2024, 1, 1, 2024, 1, 31);
    assertThrows(IllegalArgumentException.class, () -> GW_.partition(jan));
  }

  @Test
  void membersOfMonthUsesMajorityForBoundaryWeeks() {
    // June 2024: Jun 1 is Saturday. Week containing Jun 1 runs May 27–Jun 2,
    // with only 2 of 7 days in June → excluded.
    var june = interval(2024, 6, 1, 2024, 6, 30);
    var weeks = GW_.contributorsTo(june);
    assertEquals(4, weeks.size());
    assertEquals(Epoch2kDay.ofYMD(2024, 6, 3), weeks.get(0).firstDay());
  }

  @Test
  void membersOfMonthIncludesBoundaryWeeksWithMajorityDays() {
    // Aug 2024: Aug 1 is Thursday. Week containing Aug 1 runs Jul 29–Aug 4,
    // with 4 of 7 days in Aug → included.
    var aug = interval(2024, 8, 1, 2024, 8, 31);
    var weeks = GW_.contributorsTo(aug);
    assertEquals(5, weeks.size());
    assertEquals(Epoch2kDay.ofYMD(2024, 7, 29), weeks.get(0).firstDay());
  }

  @Test
  void partitionsContainsOnlyBiWeek() {
    assertEquals(Set.of(GT.noOffset()), GW_.partitions());
  }

  @Test
  void aggregatesToContainsBiWeekMonthQuarterYear() {
    assertEquals(Set.of(GT.noOffset(), GM.noOffset(), GB.noOffset(), GQ.noOffset(), GS.noOffset(), GY.noOffset()), GW_.contributesTo());
  }
}
