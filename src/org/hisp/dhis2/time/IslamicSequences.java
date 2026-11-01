package org.hisp.dhis2.time;

import java.util.Set;

public final class IslamicSequences {

  private IslamicSequences() {}

  public static final TimeFamilyKey ID = new TimeFamilyKey('I', 'D');
  public static final TimeFamilyKey IM = new TimeFamilyKey('I', 'M');
  public static final TimeFamilyKey IY = new TimeFamilyKey('I', 'Y');

  public static final TimeSequence ID_ = daily();
  public static final TimeSequence IM_ = monthly();
  public static final TimeSequence IY_ = yearly();

  public static TimeSequence daily() {
    return new DailyIslamicSequence(
        ID.noOffset(),
        Set.of(IM.noOffset(), IY.noOffset()),
        Set.of(IM.noOffset(), IY.noOffset()));
  }

  public static TimeSequence monthly() {
    return new MonthlyIslamicSequence(
        IM.noOffset(),
        Set.of(IY.noOffset()),
        Set.of(IY.noOffset()));
  }

  public static TimeSequence yearly() {
    return new YearlyIslamicSequence(
        IY.noOffset(), Set.of(), Set.of());
  }

  private record DailyIslamicSequence(
      TimeSequenceKey key, Set<TimeSequenceKey> partitions, Set<TimeSequenceKey> contributesTo)
      implements TimeSequence {

    DailyIslamicSequence {
      TimeSequence.checkKeys(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return new Epoch2kInterval(day);
    }
  }

  private record MonthlyIslamicSequence(
      TimeSequenceKey key, Set<TimeSequenceKey> partitions, Set<TimeSequenceKey> contributesTo)
      implements TimeSequence {

    MonthlyIslamicSequence {
      TimeSequence.checkKeys(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return IslamicCalendar.toEpochIntervalOfIslamicMonthContaining(day);
    }
  }

  private record YearlyIslamicSequence(
      TimeSequenceKey key, Set<TimeSequenceKey> partitions, Set<TimeSequenceKey> contributesTo)
      implements TimeSequence {

    YearlyIslamicSequence {
      TimeSequence.checkKeys(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return IslamicCalendar.toEpochIntervalOfIslamicYearContaining(day);
    }
  }
}