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
    return new DailyIslamicSequence(ID.noOffset(), Set.of(IM, IY), Set.of(IM, IY));
  }

  public static TimeSequence monthly() {
    return new MonthlyIslamicSequence(IM.noOffset(), Set.of(IY), Set.of(IY));
  }

  public static TimeSequence yearly() {
    return new YearlyIslamicSequence(IY.noOffset(), Set.of(), Set.of());
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