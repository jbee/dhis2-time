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
    return new DailyEthiopianSequence(ED.noOffset(), Set.of(EM, EY), Set.of(EM, EY));
  }

  public static TimeSequence monthly() {
    return new MonthlyEthiopianSequence(EM.noOffset(), Set.of(EY), Set.of(EY));
  }

  public static TimeSequence yearly() {
    return new YearlyEthiopianSequence(EY.noOffset(), Set.of(), Set.of());
  }

  public record DailyEthiopianSequence(
      TimeSequenceKey key, Set<TimeFamilyKey> partitions, Set<TimeFamilyKey> contributesTo)
      implements TimeSequence {

    public DailyEthiopianSequence {
      TimeSequence.checkFamilies(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return new Epoch2kInterval(day);
    }
  }

  public record MonthlyEthiopianSequence(
      TimeSequenceKey key, Set<TimeFamilyKey> partitions, Set<TimeFamilyKey> contributesTo)
      implements TimeSequence {

    public MonthlyEthiopianSequence {
      TimeSequence.checkFamilies(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return EthiopianCalendar.toEpochIntervalOfEthiopianMonthContaining(day);
    }
  }

  public record YearlyEthiopianSequence(
      TimeSequenceKey key, Set<TimeFamilyKey> partitions, Set<TimeFamilyKey> contributesTo)
      implements TimeSequence {

    public YearlyEthiopianSequence {
      TimeSequence.checkFamilies(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return EthiopianCalendar.toEpochIntervalOfEthiopianYearContaining(day);
    }
  }
}