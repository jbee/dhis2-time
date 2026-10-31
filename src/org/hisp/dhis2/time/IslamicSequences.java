package org.hisp.dhis2.time;

import java.util.Set;

public final class IslamicSequences {

  private IslamicSequences() {}

  public static final TimeFamilyKey Day = new TimeFamilyKey('I', 'D');
  public static final TimeFamilyKey Month = new TimeFamilyKey('I', 'M');
  public static final TimeFamilyKey Year = new TimeFamilyKey('I', 'Y');

  public static final TimeSequence Daily = daily();
  public static final TimeSequence Monthly = monthly();
  public static final TimeSequence Yearly = yearly();

  public static TimeSequence daily() {
    return new DailyIslamicSequence(
        new TimeSequenceKey('I', 'D'),
        Set.of(Month, Year),
        Set.of(Month, Year));
  }

  public static TimeSequence monthly() {
    return new MonthlyIslamicSequence(
        new TimeSequenceKey('I', 'M'),
        Set.of(Year),
        Set.of(Year));
  }

  public static TimeSequence yearly() {
    return new YearlyIslamicSequence(
        new TimeSequenceKey('I', 'Y'),
        Set.of(),
        Set.of());
  }

  public record DailyIslamicSequence(
      TimeSequenceKey key, Set<TimeFamilyKey> partitions, Set<TimeFamilyKey> contributesTo)
      implements TimeSequence {

    public DailyIslamicSequence {
      TimeSequence.checkFamilies(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return new Epoch2kInterval(day);
    }
  }

  public record MonthlyIslamicSequence(
      TimeSequenceKey key, Set<TimeFamilyKey> partitions, Set<TimeFamilyKey> contributesTo)
      implements TimeSequence {

    public MonthlyIslamicSequence {
      TimeSequence.checkFamilies(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return IslamicCalendar.toEpochIntervalOfIslamicMonthContaining(day);
    }
  }

  public record YearlyIslamicSequence(
      TimeSequenceKey key, Set<TimeFamilyKey> partitions, Set<TimeFamilyKey> contributesTo)
      implements TimeSequence {

    public YearlyIslamicSequence {
      TimeSequence.checkFamilies(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return IslamicCalendar.toEpochIntervalOfIslamicYearContaining(day);
    }
  }
}