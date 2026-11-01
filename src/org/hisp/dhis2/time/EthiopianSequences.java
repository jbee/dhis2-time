package org.hisp.dhis2.time;

import java.util.Set;

public final class EthiopianSequences {

  private EthiopianSequences() {}

  // Ethiopian family keys — system 'E' distinguishes them from Gregorian 'G'/'F'
  public static final TimeFamilyKey ED = new TimeFamilyKey('E', 'D');
  public static final TimeFamilyKey EM = new TimeFamilyKey('E', 'M');
  public static final TimeFamilyKey EY = new TimeFamilyKey('E', 'Y');

  public static final TimeSequence ED_ = daily();
  public static final TimeSequence EM_ = monthly();
  public static final TimeSequence EY_ = yearly();

  public static TimeSequence daily() {
    return new DailyEthiopianSequence(
        ED.noOffset(),
        Set.of(EM.noOffset(), EY.noOffset()),
        Set.of(EM.noOffset(), EY.noOffset()));
  }

  public static TimeSequence monthly() {
    return new MonthlyEthiopianSequence(
        EM.noOffset(),
        Set.of(EY.noOffset()),
        Set.of(EY.noOffset()));
  }

  public static TimeSequence yearly() {
    return new YearlyEthiopianSequence(
        EY.noOffset(), Set.of(), Set.of());
  }

  private record DailyEthiopianSequence(
      TimeSequenceKey key, Set<TimeSequenceKey> partitions, Set<TimeSequenceKey> contributesTo)
      implements TimeSequence {

    DailyEthiopianSequence {
      TimeSequence.checkKeys(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return new Epoch2kInterval(day);
    }
  }

  private record MonthlyEthiopianSequence(
      TimeSequenceKey key, Set<TimeSequenceKey> partitions, Set<TimeSequenceKey> contributesTo)
      implements TimeSequence {

    MonthlyEthiopianSequence {
      TimeSequence.checkKeys(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return EthiopianCalendar.toEpochIntervalOfEthiopianMonthContaining(day);
    }
  }

  private record YearlyEthiopianSequence(
      TimeSequenceKey key, Set<TimeSequenceKey> partitions, Set<TimeSequenceKey> contributesTo)
      implements TimeSequence {

    YearlyEthiopianSequence {
      TimeSequence.checkKeys(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return EthiopianCalendar.toEpochIntervalOfEthiopianYearContaining(day);
    }
  }
}