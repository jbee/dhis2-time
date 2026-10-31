package org.hisp.dhis2.time;

import java.util.Set;

public final class GregorianSequences {

  private GregorianSequences() {}

  // Calendar Gregorian
  public static final TimeFamilyKey Day = new TimeFamilyKey('G', 'D');
  public static final TimeFamilyKey Week = new TimeFamilyKey('G', 'W');
  public static final TimeFamilyKey BiWeek = new TimeFamilyKey('G', 'B');
  public static final TimeFamilyKey Month = new TimeFamilyKey('G', 'M');
  public static final TimeFamilyKey Quarter = new TimeFamilyKey('G', 'Q');
  public static final TimeFamilyKey Year = new TimeFamilyKey('G', 'Y');
  // Fiscal Gregorian
  public static final TimeFamilyKey FiscalMonth = new TimeFamilyKey('F', 'M');
  public static final TimeFamilyKey FiscalQuarter = new TimeFamilyKey('F', 'Q');
  public static final TimeFamilyKey FiscalYear = new TimeFamilyKey('F', 'Y');

  // ── Typical calendar-Gregorian ──────────────────────────────────────

  public static final TimeSequence Daily =
      new DailyGregorianSequence(
          new TimeSequenceKey('G', 'D'),
          Set.of(Week, BiWeek, Month, Quarter, Year),
          Set.of(Week, BiWeek, Month, Quarter, Year));

  public static final TimeSequence Weekly =
      new WeeklyGregorianSequence(
          TimeSequenceKey.weekly('G', Epoch2kDay.DayOfWeek.Monday),
          Epoch2kDay.DayOfWeek.Monday,
          Set.of(BiWeek),
          Set.of(BiWeek, Month, Quarter, Year));

  public static final TimeSequence BiWeekly =
      new BiWeeklyGregorianSequence(
          TimeSequenceKey.biWeekly('G', Epoch2kDay.DayOfWeek.Monday),
          Epoch2kDay.DayOfWeek.Monday,
          Set.of(),
          Set.of(Month, Quarter, Year));

  public static final TimeSequence Monthly =
      new MonthlyGregorianSequence(
          new TimeSequenceKey('G', 'M'), Set.of(Quarter, Year), Set.of(Quarter, Year));

  public static final TimeSequence Quarterly =
      new QuarterlyGregorianSequence(
          new TimeSequenceKey('G', 'Q'),
          Epoch2kDay.MonthOfYear.January,
          Set.of(Year),
          Set.of(Year));

  public static final TimeSequence Yearly =
      new YearlyGregorianSequence(
          new TimeSequenceKey('G', 'Y'), Epoch2kDay.MonthOfYear.January, Set.of(), Set.of());

  // ── Typical fiscal (July-start) ─────────────────────────────────────

  public static final TimeSequence FiscalMonthlyJuly =
      new MonthlyGregorianSequence(
          TimeSequenceKey.monthly('F', Epoch2kDay.MonthOfYear.July),
          Set.of(FiscalQuarter, FiscalYear),
          Set.of(FiscalQuarter, FiscalYear));

  public static final TimeSequence FiscalQuarterlyJuly =
      new QuarterlyGregorianSequence(
          TimeSequenceKey.quarterly('F', Epoch2kDay.MonthOfYear.July),
          Epoch2kDay.MonthOfYear.July,
          Set.of(FiscalYear),
          Set.of(FiscalYear));

  public static final TimeSequence FiscalYearlyJuly =
      new YearlyGregorianSequence(
          TimeSequenceKey.yearly('F', Epoch2kDay.MonthOfYear.July),
          Epoch2kDay.MonthOfYear.July,
          Set.of(),
          Set.of());

  // ── Parameterised factories ─────────────────────────────────────────
  //
  // public static TimeSequence weekly(Epoch2kDay.DayOfWeek day1) { … }
  // public static TimeSequence biWeekly(Epoch2kDay.DayOfWeek day1) { … }
  // public static TimeSequence quarterly(Epoch2kDay.MonthOfYear month1) { … }
  // public static TimeSequence yearly(Epoch2kDay.MonthOfYear month1) { … }

  // ── Records ─────────────────────────────────────────────────────────

  public record DailyGregorianSequence(
      TimeSequenceKey key, Set<TimeFamilyKey> partitions, Set<TimeFamilyKey> aggregatesTo)
      implements TimeSequence {

    public DailyGregorianSequence {
      TimeSequence.checkFamilies(partitions, aggregatesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return new Epoch2kInterval(day);
    }
  }

  public record WeeklyGregorianSequence(
      TimeSequenceKey key,
      Epoch2kDay.DayOfWeek day1,
      Set<TimeFamilyKey> partitions,
      Set<TimeFamilyKey> aggregatesTo)
      implements TimeSequence {

    public WeeklyGregorianSequence {
      if (key.offset() == TimeSequenceKey.NO_OFFSET)
        throw new IllegalArgumentException("weekly key must carry a day offset: " + key);
      if (key.day1() != day1)
        throw new IllegalArgumentException(
            "key " + key + " implies day1 " + key.day1() + " but record has " + day1);
      TimeSequence.checkFamilies(partitions, aggregatesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return day.containingWeek(day1);
    }
  }

  public record BiWeeklyGregorianSequence(
      TimeSequenceKey key,
      Epoch2kDay.DayOfWeek day1,
      Set<TimeFamilyKey> partitions,
      Set<TimeFamilyKey> aggregatesTo)
      implements TimeSequence {

    public BiWeeklyGregorianSequence {
      if (key.offset() == TimeSequenceKey.NO_OFFSET)
        throw new IllegalArgumentException("bi-weekly key must carry a day offset: " + key);
      if (key.day1() != day1)
        throw new IllegalArgumentException(
            "key " + key + " implies day1 " + key.day1() + " but record has " + day1);
      TimeSequence.checkFamilies(partitions, aggregatesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      Epoch2kInterval week = day.containingWeek(day1);
      Epoch2kDay weekStart = week.first();

      // ISO-style week-year: week 1 of Y contains Jan 4 of Y.
      int year = weekStart.year();
      Epoch2kDay week1 = Epoch2kDay.ofWeek1In(year, day1);
      if (weekStart.before(week1)) {
        year--;
        week1 = Epoch2kDay.ofWeek1In(year, day1);
      } else {
        Epoch2kDay nextWeek1 = Epoch2kDay.ofWeek1In(year + 1, day1);
        if (weekStart.notBefore(nextWeek1)) {
          year++;
          week1 = nextWeek1;
        }
      }

      int weekNumber = ((weekStart.daysSince2k() - week1.daysSince2k()) / 7) + 1;
      int biWeekIndex = (weekNumber - 1) / 2;

      Epoch2kDay biWeekStart = week1.plusDays(biWeekIndex * 14);
      Epoch2kDay biWeekEnd = biWeekStart.plusDays(13);

      // 53-week years: the 27th bi-week is a single week.
      Epoch2kDay nextYearWeek1 = Epoch2kDay.ofWeek1In(year + 1, day1);
      if (biWeekEnd.notBefore(nextYearWeek1)) {
        biWeekEnd = nextYearWeek1.minusDays(1);
      }
      return new Epoch2kInterval(biWeekStart, biWeekEnd);
    }
  }

  public record MonthlyGregorianSequence(
      TimeSequenceKey key, Set<TimeFamilyKey> partitions, Set<TimeFamilyKey> aggregatesTo)
      implements TimeSequence {

    public MonthlyGregorianSequence {
      TimeSequence.checkFamilies(partitions, aggregatesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return day.containingMonth();
    }
  }

  public record QuarterlyGregorianSequence(
      TimeSequenceKey key,
      Epoch2kDay.MonthOfYear month1,
      Set<TimeFamilyKey> partitions,
      Set<TimeFamilyKey> aggregatesTo)
      implements TimeSequence {

    public QuarterlyGregorianSequence {
      Epoch2kDay.MonthOfYear keyMonth1 =
          key.offset() == TimeSequenceKey.NO_OFFSET
              ? Epoch2kDay.MonthOfYear.January
              : key.month1();
      if (keyMonth1 != month1)
        throw new IllegalArgumentException(
            "key " + key + " implies month1 " + keyMonth1 + " but record has " + month1);
      TimeSequence.checkFamilies(partitions, aggregatesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return day.containingQuarter(month1);
    }
  }

  public record YearlyGregorianSequence(
      TimeSequenceKey key,
      Epoch2kDay.MonthOfYear month1,
      Set<TimeFamilyKey> partitions,
      Set<TimeFamilyKey> aggregatesTo)
      implements TimeSequence {

    public YearlyGregorianSequence {
      Epoch2kDay.MonthOfYear keyMonth1 =
          key.offset() == TimeSequenceKey.NO_OFFSET
              ? Epoch2kDay.MonthOfYear.January
              : key.month1();
      if (keyMonth1 != month1)
        throw new IllegalArgumentException(
            "key " + key + " implies month1 " + keyMonth1 + " but record has " + month1);
      TimeSequence.checkFamilies(partitions, aggregatesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return day.containingYear(month1);
    }
  }
}