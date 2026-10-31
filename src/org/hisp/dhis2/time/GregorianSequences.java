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

  // ── Common constants ────────────────────────────────────────────────

  public static final TimeSequence Daily = daily();

  public static final TimeSequence Weekly = weekly(Epoch2kDay.DayOfWeek.Monday);
  public static final TimeSequence BiWeekly = biWeekly(Epoch2kDay.DayOfWeek.Monday);

  public static final TimeSequence Monthly = monthly();
  public static final TimeSequence Quarterly = quarterly(Epoch2kDay.MonthOfYear.January);
  public static final TimeSequence Yearly = yearly(Epoch2kDay.MonthOfYear.January);

  // ── Common fiscal constants (July-start) ────────────────────────────

  public static final TimeSequence FiscalMonthlyJuly = fiscalMonthly(Epoch2kDay.MonthOfYear.July);
  public static final TimeSequence FiscalQuarterlyJuly = fiscalQuarterly(Epoch2kDay.MonthOfYear.July);
  public static final TimeSequence FiscalYearlyJuly = fiscalYearly(Epoch2kDay.MonthOfYear.July);

  // ── Factories ───────────────────────────────────────────────────────
  //
  // Calendar-family factories use system 'G'. The key carries NO_OFFSET ('-')
  // when the start is January (the calendar default) and an explicit offset
  // otherwise (e.g. GQ4 for April quarters, GY7 for July-start calendar years).
  //
  // Fiscal-family factories use system 'F' and always carry an explicit
  // start-month offset (e.g. FQ7, FY7).
  //
  // Weeks are never fiscal — weekly/bi-weekly always use system 'G'.

  public static TimeSequence daily() {
    return new DailyGregorianSequence(
        new TimeSequenceKey('G', 'D'),
        Set.of(Week, BiWeek, Month, Quarter, Year),
        Set.of(Week, BiWeek, Month, Quarter, Year));
  }

  public static TimeSequence weekly(Epoch2kDay.DayOfWeek day1) {
    return new WeeklyGregorianSequence(
        TimeSequenceKey.weekly('G', day1),
        day1,
        Set.of(BiWeek),
        Set.of(BiWeek, Month, Quarter, Year));
  }

  public static TimeSequence biWeekly(Epoch2kDay.DayOfWeek day1) {
    return new BiWeeklyGregorianSequence(
        TimeSequenceKey.biWeekly('G', day1),
        day1,
        Set.of(),
        Set.of(Month, Quarter, Year));
  }

  public static TimeSequence monthly() {
    return new MonthlyGregorianSequence(
        new TimeSequenceKey('G', 'M'),
        Set.of(Quarter, Year),
        Set.of(Quarter, Year));
  }

  public static TimeSequence fiscalMonthly(Epoch2kDay.MonthOfYear start) {
    return new MonthlyGregorianSequence(
        TimeSequenceKey.monthly('F', start),
        Set.of(FiscalQuarter, FiscalYear),
        Set.of(FiscalQuarter, FiscalYear));
  }

  public static TimeSequence quarterly(Epoch2kDay.MonthOfYear month1) {
    TimeSequenceKey key =
        month1 == Epoch2kDay.MonthOfYear.January
            ? new TimeSequenceKey('G', 'Q')
            : TimeSequenceKey.quarterly('G', month1);
    return new QuarterlyGregorianSequence(key, month1, Set.of(Year), Set.of(Year));
  }

  public static TimeSequence fiscalQuarterly(Epoch2kDay.MonthOfYear month1) {
    return new QuarterlyGregorianSequence(
        TimeSequenceKey.quarterly('F', month1),
        month1,
        Set.of(FiscalYear),
        Set.of(FiscalYear));
  }

  public static TimeSequence yearly(Epoch2kDay.MonthOfYear month1) {
    TimeSequenceKey key =
        month1 == Epoch2kDay.MonthOfYear.January
            ? new TimeSequenceKey('G', 'Y')
            : TimeSequenceKey.yearly('G', month1);
    return new YearlyGregorianSequence(key, month1, Set.of(), Set.of());
  }

  public static TimeSequence fiscalYearly(Epoch2kDay.MonthOfYear month1) {
    return new YearlyGregorianSequence(
        TimeSequenceKey.yearly('F', month1),
        month1,
        Set.of(),
        Set.of());
  }

  // ── Implementation ─────────────────────────────────────────────────────────

  public record DailyGregorianSequence(
      TimeSequenceKey key, Set<TimeFamilyKey> partitions, Set<TimeFamilyKey> contributesTo)
      implements TimeSequence {

    public DailyGregorianSequence {
      TimeSequence.checkFamilies(partitions, contributesTo);
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
      Set<TimeFamilyKey> contributesTo)
      implements TimeSequence {

    public WeeklyGregorianSequence {
      if (key.offset() == TimeSequenceKey.NO_OFFSET)
        throw new IllegalArgumentException("weekly key must carry a day offset: " + key);
      if (key.day1() != day1)
        throw new IllegalArgumentException(
            "key " + key + " implies day1 " + key.day1() + " but record has " + day1);
      TimeSequence.checkFamilies(partitions, contributesTo);
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
      Set<TimeFamilyKey> contributesTo)
      implements TimeSequence {

    public BiWeeklyGregorianSequence {
      if (key.offset() == TimeSequenceKey.NO_OFFSET)
        throw new IllegalArgumentException("bi-weekly key must carry a day offset: " + key);
      if (key.day1() != day1)
        throw new IllegalArgumentException(
            "key " + key + " implies day1 " + key.day1() + " but record has " + day1);
      TimeSequence.checkFamilies(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      Epoch2kInterval week = day.containingWeek(day1);
      Epoch2kDay weekStart = week.firstDay();

      // ISO-style week-year: week 1 of Y contains Jan 4 of Y.
      int year = weekStart.year();
      Epoch2kDay week1 = Epoch2kDay.ofWeek1InYear(year, day1);
      if (weekStart.before(week1)) {
        year--;
        week1 = Epoch2kDay.ofWeek1InYear(year, day1);
      } else {
        Epoch2kDay nextWeek1 = Epoch2kDay.ofWeek1InYear(year + 1, day1);
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
      Epoch2kDay nextYearWeek1 = Epoch2kDay.ofWeek1InYear(year + 1, day1);
      if (biWeekEnd.notBefore(nextYearWeek1)) {
        biWeekEnd = nextYearWeek1.minusDays(1);
      }
      return new Epoch2kInterval(biWeekStart, biWeekEnd);
    }
  }

  public record MonthlyGregorianSequence(
      TimeSequenceKey key, Set<TimeFamilyKey> partitions, Set<TimeFamilyKey> contributesTo)
      implements TimeSequence {

    public MonthlyGregorianSequence {
      TimeSequence.checkFamilies(partitions, contributesTo);
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
      Set<TimeFamilyKey> contributesTo)
      implements TimeSequence {

    public QuarterlyGregorianSequence {
      Epoch2kDay.MonthOfYear keyMonth1 =
          key.offset() == TimeSequenceKey.NO_OFFSET
              ? Epoch2kDay.MonthOfYear.January
              : key.month1();
      if (keyMonth1 != month1)
        throw new IllegalArgumentException(
            "key " + key + " implies month1 " + keyMonth1 + " but record has " + month1);
      TimeSequence.checkFamilies(partitions, contributesTo);
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
      Set<TimeFamilyKey> contributesTo)
      implements TimeSequence {

    public YearlyGregorianSequence {
      Epoch2kDay.MonthOfYear keyMonth1 =
          key.offset() == TimeSequenceKey.NO_OFFSET
              ? Epoch2kDay.MonthOfYear.January
              : key.month1();
      if (keyMonth1 != month1)
        throw new IllegalArgumentException(
            "key " + key + " implies month1 " + keyMonth1 + " but record has " + month1);
      TimeSequence.checkFamilies(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return day.containingYear(month1);
    }
  }
}