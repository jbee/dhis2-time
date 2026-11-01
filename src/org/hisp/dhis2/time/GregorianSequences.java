package org.hisp.dhis2.time;

import java.util.Set;

public final class GregorianSequences {

  private GregorianSequences() {}

  // Calendar Gregorian
  public static final TimeFamilyKey GD = new TimeFamilyKey('G', 'D');
  public static final TimeFamilyKey GW = new TimeFamilyKey('G', 'W');
  public static final TimeFamilyKey GB = new TimeFamilyKey('G', 'B');
  public static final TimeFamilyKey GM = new TimeFamilyKey('G', 'M');
  public static final TimeFamilyKey GT = new TimeFamilyKey('G', 'T');
  public static final TimeFamilyKey GS = new TimeFamilyKey('G', 'S');
  public static final TimeFamilyKey GQ = new TimeFamilyKey('G', 'Q');
  public static final TimeFamilyKey GY = new TimeFamilyKey('G', 'Y');
  // Fiscal Gregorian
  public static final TimeFamilyKey FM = new TimeFamilyKey('F', 'M');
  public static final TimeFamilyKey FS = new TimeFamilyKey('F', 'S');
  public static final TimeFamilyKey FQ = new TimeFamilyKey('F', 'Q');
  public static final TimeFamilyKey FY = new TimeFamilyKey('F', 'Y');

  // ── Common constants ────────────────────────────────────────────────

  public static final TimeSequence GD_ = daily();

  public static final TimeSequence GW_ = weekly(Epoch2kDay.DayOfWeek.Monday);
  public static final TimeSequence GB_ = biWeekly(Epoch2kDay.DayOfWeek.Monday);

  public static final TimeSequence GM_ = monthly();
  public static final TimeSequence GT_ = biMonthly(Epoch2kDay.MonthOfYear.January);
  public static final TimeSequence GS_ = sixMonthly(Epoch2kDay.MonthOfYear.January);
  public static final TimeSequence GQ_ = quarterly(Epoch2kDay.MonthOfYear.January);
  public static final TimeSequence GY_ = yearly(Epoch2kDay.MonthOfYear.January);

  // ── Common fiscal constants (July-start) ────────────────────────────

  public static final TimeSequence FM7 = fiscalMonthly(Epoch2kDay.MonthOfYear.July);
  public static final TimeSequence FS4 = fiscalSixMonthly(Epoch2kDay.MonthOfYear.April);
  public static final TimeSequence FSa = fiscalSixMonthly(Epoch2kDay.MonthOfYear.November);
  public static final TimeSequence FQ7 = fiscalQuarterly(Epoch2kDay.MonthOfYear.July);
  public static final TimeSequence FY7 = fiscalYearly(Epoch2kDay.MonthOfYear.July);

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
        GD.noOffset(),
        Set.of(GW, GB, GM, GT, GQ, GS, GY),
        Set.of(GW, GB, GM, GT, GQ, GS, GY));
  }

  /**
   * Weekly intervals starting on {@code day1}.
   *
   * @param day1 the first day of each week; {@link Epoch2kDay.DayOfWeek#Monday}
   *     gives ISO 8601 weeks
   */
  public static TimeSequence weekly(Epoch2kDay.DayOfWeek day1) {
    return new WeeklyGregorianSequence(
        GW.offsetBy(day1),
        day1,
        Set.of(GB),
        Set.of(GB, GM, GT, GQ, GS, GY));
  }

  /**
   * Bi-weekly intervals starting on {@code day1}.
   *
   * <p>Weeks are grouped two at a time within the week-year. The week-year is resolved by majority
   * of days (see {@link Epoch2kDay#ofWeek1InYear}); for Monday-start weeks this is ISO 8601
   * week-year numbering. In a 53-week year the 27th bi-week is a single week.
   *
   * @param day1 the first day of each week
   */
  public static TimeSequence biWeekly(Epoch2kDay.DayOfWeek day1) {
    return new BiWeeklyGregorianSequence(
        GB.offsetBy(day1), day1, Set.of(), Set.of(GM, GT, GQ, GS, GY));
  }

  public static TimeSequence monthly() {
    return new MonthlyGregorianSequence(
        GM.noOffset(), Set.of(GT, GQ, GS, GY), Set.of(GT, GQ, GS, GY));
  }

  public static TimeSequence biMonthly(Epoch2kDay.MonthOfYear month1) {
    return new BiMonthlyGregorianSequence(
        GT.offsetBy(month1), month1, Set.of(GY), Set.of(GQ, GS, GY));
  }

  public static TimeSequence sixMonthly(Epoch2kDay.MonthOfYear month1) {
    return new SixMonthlyGregorianSequence(GS.offsetBy(month1), month1, Set.of(GY), Set.of(GY));
  }

  public static TimeSequence quarterly(Epoch2kDay.MonthOfYear month1) {
    return new QuarterlyGregorianSequence(GQ.offsetBy(month1), month1, Set.of(GY), Set.of(GS, GY));
  }

  public static TimeSequence fiscalSixMonthly(Epoch2kDay.MonthOfYear month1) {
    return new SixMonthlyGregorianSequence(FS.offsetBy(month1), month1, Set.of(FY), Set.of(FY));
  }

  public static TimeSequence fiscalMonthly(Epoch2kDay.MonthOfYear start) {
    return new MonthlyGregorianSequence(FM.offsetBy(start), Set.of(FQ, FY), Set.of(FQ, FY));
  }

  public static TimeSequence fiscalQuarterly(Epoch2kDay.MonthOfYear month1) {
    return new QuarterlyGregorianSequence(FQ.offsetBy(month1), month1, Set.of(FY), Set.of(FY));
  }

  public static TimeSequence yearly(Epoch2kDay.MonthOfYear month1) {
    return new YearlyGregorianSequence(GY.offsetBy(month1), month1, Set.of(), Set.of());
  }

  public static TimeSequence fiscalYearly(Epoch2kDay.MonthOfYear month1) {
    return new YearlyGregorianSequence(FY.offsetBy(month1), month1, Set.of(), Set.of());
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
      if (key.offsetAsDay() != day1)
        throw new IllegalArgumentException(
            "key " + key + " implies day1 " + key.offsetAsDay() + " but record has " + day1);
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
      if (key.offsetAsDay() != day1)
        throw new IllegalArgumentException(
            "key " + key + " implies day1 " + key.offsetAsDay() + " but record has " + day1);
      TimeSequence.checkFamilies(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      Epoch2kInterval week = day.containingWeek(day1);
      Epoch2kDay weekStart = week.firstDay();

      // Week-year by majority of days: a week belongs to the year containing more
      // than half its days. For Monday-start weeks this reduces to "the week
      // containing Jan 4". ofWeek1InYear implements the general form.
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

  public record BiMonthlyGregorianSequence(
      TimeSequenceKey key,
      Epoch2kDay.MonthOfYear month1,
      Set<TimeFamilyKey> partitions,
      Set<TimeFamilyKey> contributesTo)
      implements TimeSequence {

    public BiMonthlyGregorianSequence {
      Epoch2kDay.MonthOfYear keyMonth1 = key.offsetAsMonth();
      if (keyMonth1 != month1)
        throw new IllegalArgumentException(
            "key " + key + " implies month1 " + keyMonth1 + " but record has " + month1);
      TimeSequence.checkFamilies(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return day.containingBiMonth(month1);
    }
  }

  public record SixMonthlyGregorianSequence(
      TimeSequenceKey key,
      Epoch2kDay.MonthOfYear month1,
      Set<TimeFamilyKey> partitions,
      Set<TimeFamilyKey> contributesTo)
      implements TimeSequence {

    public SixMonthlyGregorianSequence {
      Epoch2kDay.MonthOfYear keyMonth1 = key.offsetAsMonth();
      if (keyMonth1 != month1)
        throw new IllegalArgumentException(
            "key " + key + " implies month1 " + keyMonth1 + " but record has " + month1);
      TimeSequence.checkFamilies(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return day.containingSixMonth(month1);
    }
  }

  public record QuarterlyGregorianSequence(
      TimeSequenceKey key,
      Epoch2kDay.MonthOfYear month1,
      Set<TimeFamilyKey> partitions,
      Set<TimeFamilyKey> contributesTo)
      implements TimeSequence {

    public QuarterlyGregorianSequence {
      Epoch2kDay.MonthOfYear keyMonth1 = key.offsetAsMonth();
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
      Epoch2kDay.MonthOfYear keyMonth1 = key.offsetAsMonth();
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