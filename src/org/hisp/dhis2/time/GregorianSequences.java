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
  //
  // Target offsets (partitions()/contributesTo()) are derived from the source
  // offset when the source and target share an offset domain (day→day,
  // month→month); otherwise the target uses the family default (noOffset).

  public static TimeSequence daily() {
    return new DailyGregorianSequence(
        GD.noOffset(),
        Set.of(GW.noOffset(), GB.noOffset(), GM.noOffset(),
            GT.noOffset(), GQ.noOffset(), GS.noOffset(), GY.noOffset()),
        Set.of(GW.noOffset(), GB.noOffset(), GM.noOffset(),
            GT.noOffset(), GQ.noOffset(), GS.noOffset(), GY.noOffset()));
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
        Set.of(GB.offsetBy(day1)),
        Set.of(GB.offsetBy(day1),
            GM.noOffset(), GT.noOffset(), GQ.noOffset(),
            GS.noOffset(), GY.noOffset()));
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
        GB.offsetBy(day1),
        day1,
        Set.of(),
        Set.of(GM.noOffset(), GT.noOffset(), GQ.noOffset(),
            GS.noOffset(), GY.noOffset()));
  }

  public static TimeSequence monthly() {
    return new MonthlyGregorianSequence(
        GM.noOffset(),
        Set.of(GT.noOffset(), GQ.noOffset(), GS.noOffset(), GY.noOffset()),
        Set.of(GT.noOffset(), GQ.noOffset(), GS.noOffset(), GY.noOffset()));
  }

  public static TimeSequence biMonthly(Epoch2kDay.MonthOfYear month1) {
    return new BiMonthlyGregorianSequence(
        GT.offsetBy(month1),
        month1,
        Set.of(GY.offsetBy(month1)),
        Set.of(GQ.offsetBy(month1), GS.offsetBy(month1), GY.offsetBy(month1)));
  }

  public static TimeSequence sixMonthly(Epoch2kDay.MonthOfYear month1) {
    return new SixMonthlyGregorianSequence(
        GS.offsetBy(month1),
        month1,
        Set.of(GY.offsetBy(month1)),
        Set.of(GY.offsetBy(month1)));
  }

  public static TimeSequence quarterly(Epoch2kDay.MonthOfYear month1) {
    return new QuarterlyGregorianSequence(
        GQ.offsetBy(month1),
        month1,
        Set.of(GY.offsetBy(month1)),
        Set.of(GS.offsetBy(month1), GY.offsetBy(month1)));
  }

  public static TimeSequence fiscalSixMonthly(Epoch2kDay.MonthOfYear month1) {
    return new SixMonthlyGregorianSequence(
        FS.offsetBy(month1),
        month1,
        Set.of(FY.offsetBy(month1)),
        Set.of(FY.offsetBy(month1)));
  }

  public static TimeSequence fiscalMonthly(Epoch2kDay.MonthOfYear start) {
    return new MonthlyGregorianSequence(
        FM.offsetBy(start),
        Set.of(FQ.offsetBy(start), FY.offsetBy(start)),
        Set.of(FQ.offsetBy(start), FY.offsetBy(start)));
  }

  public static TimeSequence fiscalQuarterly(Epoch2kDay.MonthOfYear month1) {
    return new QuarterlyGregorianSequence(
        FQ.offsetBy(month1),
        month1,
        Set.of(FY.offsetBy(month1)),
        Set.of(FY.offsetBy(month1)));
  }

  public static TimeSequence yearly(Epoch2kDay.MonthOfYear month1) {
    return new YearlyGregorianSequence(
        GY.offsetBy(month1), month1, Set.of(), Set.of());
  }

  public static TimeSequence fiscalYearly(Epoch2kDay.MonthOfYear month1) {
    return new YearlyGregorianSequence(
        FY.offsetBy(month1), month1, Set.of(), Set.of());
  }

  // ── Implementation ─────────────────────────────────────────────────────────

  private record DailyGregorianSequence(
      TimeSequenceKey key, Set<TimeSequenceKey> partitions, Set<TimeSequenceKey> contributesTo)
      implements TimeSequence {

    DailyGregorianSequence {
      TimeSequence.checkKeys(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return new Epoch2kInterval(day);
    }
  }

  private record WeeklyGregorianSequence(
      TimeSequenceKey key,
      Epoch2kDay.DayOfWeek day1,
      Set<TimeSequenceKey> partitions,
      Set<TimeSequenceKey> contributesTo)
      implements TimeSequence {

    WeeklyGregorianSequence {
      if (key.offsetAsDay() != day1)
        throw new IllegalArgumentException(
            "key " + key + " implies day1 " + key.offsetAsDay() + " but record has " + day1);
      TimeSequence.checkKeys(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return day.containingWeek(day1);
    }
  }

  private record BiWeeklyGregorianSequence(
      TimeSequenceKey key,
      Epoch2kDay.DayOfWeek day1,
      Set<TimeSequenceKey> partitions,
      Set<TimeSequenceKey> contributesTo)
      implements TimeSequence {

    BiWeeklyGregorianSequence {
      if (key.offsetAsDay() != day1)
        throw new IllegalArgumentException(
            "key " + key + " implies day1 " + key.offsetAsDay() + " but record has " + day1);
      TimeSequence.checkKeys(partitions, contributesTo);
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

  private record MonthlyGregorianSequence(
      TimeSequenceKey key, Set<TimeSequenceKey> partitions, Set<TimeSequenceKey> contributesTo)
      implements TimeSequence {

    MonthlyGregorianSequence {
      TimeSequence.checkKeys(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return day.containingMonth();
    }
  }

  private record BiMonthlyGregorianSequence(
      TimeSequenceKey key,
      Epoch2kDay.MonthOfYear month1,
      Set<TimeSequenceKey> partitions,
      Set<TimeSequenceKey> contributesTo)
      implements TimeSequence {

    BiMonthlyGregorianSequence {
      if (key.offsetAsMonth() != month1)
        throw new IllegalArgumentException(
            "key " + key + " implies month1 " + key.offsetAsMonth() + " but record has " + month1);
      TimeSequence.checkKeys(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return day.containingBiMonth(month1);
    }
  }

  private record SixMonthlyGregorianSequence(
      TimeSequenceKey key,
      Epoch2kDay.MonthOfYear month1,
      Set<TimeSequenceKey> partitions,
      Set<TimeSequenceKey> contributesTo)
      implements TimeSequence {

    SixMonthlyGregorianSequence {
      if (key.offsetAsMonth() != month1)
        throw new IllegalArgumentException(
            "key " + key + " implies month1 " + key.offsetAsMonth() + " but record has " + month1);
      TimeSequence.checkKeys(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return day.containingSixMonth(month1);
    }
  }

  private record QuarterlyGregorianSequence(
      TimeSequenceKey key,
      Epoch2kDay.MonthOfYear month1,
      Set<TimeSequenceKey> partitions,
      Set<TimeSequenceKey> contributesTo)
      implements TimeSequence {

    QuarterlyGregorianSequence {
      if (key.offsetAsMonth() != month1)
        throw new IllegalArgumentException(
            "key " + key + " implies month1 " + key.offsetAsMonth() + " but record has " + month1);
      TimeSequence.checkKeys(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return day.containingQuarter(month1);
    }
  }

  private record YearlyGregorianSequence(
      TimeSequenceKey key,
      Epoch2kDay.MonthOfYear month1,
      Set<TimeSequenceKey> partitions,
      Set<TimeSequenceKey> contributesTo)
      implements TimeSequence {

    YearlyGregorianSequence {
      if (key.offsetAsMonth() != month1)
        throw new IllegalArgumentException(
            "key " + key + " implies month1 " + key.offsetAsMonth() + " but record has " + month1);
      TimeSequence.checkKeys(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return day.containingYear(month1);
    }
  }
}