package org.hisp.dhis2.time;

import java.util.Set;

public final class EthiopianSequences {

  private EthiopianSequences() {}

  // Ethiopian family tags — system 'E' distinguishes them from Gregorian 'G'/'F'
  public static final TimeFamilyTag ED = new TimeFamilyTag('E', 'D');
  public static final TimeFamilyTag EM = new TimeFamilyTag('E', 'M');
  public static final TimeFamilyTag EY = new TimeFamilyTag('E', 'Y');

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
      TimeSequenceTag tag, Set<TimeSequenceTag> partitions, Set<TimeSequenceTag> contributesTo)
      implements TimeSequence {

    DailyEthiopianSequence {
      TimeSequence.checkTags(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return new Epoch2kInterval(day);
    }
  }

  private record MonthlyEthiopianSequence(
      TimeSequenceTag tag, Set<TimeSequenceTag> partitions, Set<TimeSequenceTag> contributesTo)
      implements TimeSequence {

    MonthlyEthiopianSequence {
      TimeSequence.checkTags(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return EthiopianCalendar.toEpochIntervalOfEthiopianMonthContaining(day);
    }
  }

  private record YearlyEthiopianSequence(
      TimeSequenceTag tag, Set<TimeSequenceTag> partitions, Set<TimeSequenceTag> contributesTo)
      implements TimeSequence {

    YearlyEthiopianSequence {
      TimeSequence.checkTags(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return EthiopianCalendar.toEpochIntervalOfEthiopianYearContaining(day);
    }
  }
}