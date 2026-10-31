package org.hisp.dhis2.time;

import java.util.Set;

public final class EthiopianSequences {

  private EthiopianSequences() {}

  // Ethiopian family keys — system 'E' distinguishes them from Gregorian 'G'/'F'
  public static final TimeFamilyKey Day = new TimeFamilyKey('E', 'D');
  public static final TimeFamilyKey Month = new TimeFamilyKey('E', 'M');
  public static final TimeFamilyKey Year = new TimeFamilyKey('E', 'Y');

  public static final TimeSequence Daily = daily();
  public static final TimeSequence Monthly = monthly();
  public static final TimeSequence Yearly = yearly();

  public static TimeSequence daily() {
    return new DailyEthiopianSequence(
        new TimeSequenceKey('E', 'D'),
        Set.of(Month, Year),
        Set.of(Month, Year));
  }

  public static TimeSequence monthly() {
    return new MonthlyEthiopianSequence(
        new TimeSequenceKey('E', 'M'),
        Set.of(Year),
        Set.of(Year));
  }

  public static TimeSequence yearly() {
    return new YearlyEthiopianSequence(
        new TimeSequenceKey('E', 'Y'),
        Set.of(),
        Set.of());
  }

  public record DailyEthiopianSequence(
      TimeSequenceKey key, Set<TimeFamilyKey> partitions, Set<TimeFamilyKey> aggregatesTo)
      implements TimeSequence {

    public DailyEthiopianSequence {
      TimeSequence.checkFamilies(partitions, aggregatesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return new Epoch2kInterval(day);
    }
  }

  public record MonthlyEthiopianSequence(
      TimeSequenceKey key, Set<TimeFamilyKey> partitions, Set<TimeFamilyKey> aggregatesTo)
      implements TimeSequence {

    public MonthlyEthiopianSequence {
      TimeSequence.checkFamilies(partitions, aggregatesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return EthiopianCalendar.monthInterval(day);
    }
  }

  public record YearlyEthiopianSequence(
      TimeSequenceKey key, Set<TimeFamilyKey> partitions, Set<TimeFamilyKey> aggregatesTo)
      implements TimeSequence {

    public YearlyEthiopianSequence {
      TimeSequence.checkFamilies(partitions, aggregatesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return EthiopianCalendar.yearInterval(day);
    }
  }
}