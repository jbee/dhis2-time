package org.hisp.dhis2.time;

import org.hisp.dhis2.time.Epoch2kDay.DayOfWeek;
import org.hisp.dhis2.time.Epoch2kDay.MonthOfYear;

import java.util.Set;

/**
 * For Gregorian calendar:
 * <pre>
 *   D = Daily
 *   W = Weekly
 *   F = Fortnightly (a.k.a. bi-weekly)
 *   M = Monthly
 *   B = Bimester (a.k.a. bi-monthly)
 *   S = Semester (a.k.a. six-monthly)
 *   Q = Quarterly
 *   Y = Yearly
 * </pre>
 * For Fiscal Gregorian calendar:
 * <pre>
 *   H = Half-Year (a.k.a. six-monthly)
 * </pre>
 */
public final class GregorianSequences {

  // Interval codes are the first letter of the interval's name in that family's
  // vocabulary: M=Month, Q=Quarter, Y=Year in both families (same word), but
  // S=Semester (calendar) vs H=HalfYear (fiscal) because the names differ.
  // This reflects that interval code is a naming choice, not a property of the
  // interval's shape — the same six-month cycle appears under two names because
  // two domains call it different things.

  private GregorianSequences() {}

  // Calendar Gregorian
  public static final TimeFamilyTag GD = new TimeFamilyTag('G', 'D');
  public static final TimeFamilyTag GW = new TimeFamilyTag('G', 'W');
  public static final TimeFamilyTag GF = new TimeFamilyTag('G', 'F');
  public static final TimeFamilyTag GM = new TimeFamilyTag('G', 'M');
  public static final TimeFamilyTag GB = new TimeFamilyTag('G', 'B');
  public static final TimeFamilyTag GQ = new TimeFamilyTag('G', 'Q');
  public static final TimeFamilyTag GS = new TimeFamilyTag('G', 'S');
  public static final TimeFamilyTag GY = new TimeFamilyTag('G', 'Y');
  // Fiscal Gregorian
  public static final TimeFamilyTag FM = new TimeFamilyTag('F', 'M');
  public static final TimeFamilyTag FQ = new TimeFamilyTag('F', 'Q');
  public static final TimeFamilyTag FH = new TimeFamilyTag('F', 'H');
  public static final TimeFamilyTag FY = new TimeFamilyTag('F', 'Y');

  // ── Common constants ────────────────────────────────────────────────

  public static final TimeSequence GD_ = daily();

  public static final TimeSequence GW_ = weekly(DayOfWeek.Monday);
  public static final TimeSequence GF_ = fortnightly(DayOfWeek.Monday);

  public static final TimeSequence GM_ = monthly();
  public static final TimeSequence GB_ = bimester(MonthOfYear.January);
  public static final TimeSequence GS_ = semester(MonthOfYear.January);
  public static final TimeSequence GQ_ = quarterly(MonthOfYear.January);
  public static final TimeSequence GY_ = yearly(MonthOfYear.January);

  // ── Common fiscal constants (July-start) ────────────────────────────

  public static final TimeSequence FM7 = fiscalMonthly(MonthOfYear.July);
  public static final TimeSequence FH4 = fiscalHalfYear(MonthOfYear.April);
  public static final TimeSequence FHb = fiscalHalfYear(MonthOfYear.November);
  public static final TimeSequence FQ7 = fiscalQuarterly(MonthOfYear.July);
  public static final TimeSequence FQb = fiscalQuarterly(MonthOfYear.November);
  public static final TimeSequence FY7 = fiscalYearly(MonthOfYear.July);

  // ── Factories ───────────────────────────────────────────────────────

  public static TimeSequence daily() {
    return new DailyGregorianSequence(
        GD.noOffset(),
        Set.of(
            GW.noOffset(),
            GF.noOffset(),
            GM.noOffset(),
            GB.noOffset(),
            GQ.noOffset(),
            GS.noOffset(),
            GY.noOffset()),
        Set.of(
            GW.noOffset(),
            GF.noOffset(),
            GM.noOffset(),
            GB.noOffset(),
            GQ.noOffset(),
            GS.noOffset(),
            GY.noOffset()));
  }

  /**
   * Weekly intervals starting on {@code day1}.
   *
   * @param day1 the first day of each week; {@link DayOfWeek#Monday} gives ISO 8601 weeks
   */
  public static TimeSequence weekly(DayOfWeek day1) {
    return new WeeklyGregorianSequence(
        GW.offsetBy(day1),
        day1,
        Set.of(GF.offsetBy(day1)),
        Set.of(
            GF.offsetBy(day1),
            GM.noOffset(),
            GB.noOffset(),
            GQ.noOffset(),
            GS.noOffset(),
            GY.noOffset()));
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
  public static TimeSequence fortnightly(DayOfWeek day1) {
    return new FortnightlyGregorianSequence(
        GF.offsetBy(day1),
        day1,
        Set.of(),
        Set.of(GM.noOffset(), GB.noOffset(), GQ.noOffset(), GS.noOffset(), GY.noOffset()));
  }

  public static TimeSequence monthly() {
    return new MonthlyGregorianSequence(
        GM.noOffset(),
        Set.of(GB.noOffset(), GQ.noOffset(), GS.noOffset(), GY.noOffset()),
        Set.of(GB.noOffset(), GQ.noOffset(), GS.noOffset(), GY.noOffset()));
  }

  public static TimeSequence bimester(MonthOfYear month1) {
    return new BimesterGregorianSequence(
        GB.offsetBy(month1), month1,
        Set.of(GS.offsetBy(month1), GY.offsetBy(month1)),
        Set.of(GQ.offsetBy(month1), GS.offsetBy(month1), GY.offsetBy(month1)));
  }

  public static TimeSequence semester(MonthOfYear month1) {
    return new SemesterGregorianSequence(
        GS.offsetBy(month1), month1, Set.of(GY.offsetBy(month1)), Set.of(GY.offsetBy(month1)));
  }

  public static TimeSequence quarterly(MonthOfYear month1) {
    return new QuarterlyGregorianSequence(
        GQ.offsetBy(month1), month1,
        Set.of(GS.offsetBy(month1), GY.offsetBy(month1)),
        Set.of(GS.offsetBy(month1), GY.offsetBy(month1)));
  }

  public static TimeSequence fiscalHalfYear(MonthOfYear month1) {
    return new SemesterGregorianSequence(
        FH.offsetBy(month1), month1, Set.of(FY.offsetBy(month1)), Set.of(FY.offsetBy(month1)));
  }

  public static TimeSequence fiscalMonthly(MonthOfYear start) {
    return new MonthlyGregorianSequence(
        FM.offsetBy(start),
        Set.of(FH.offsetBy(start), FQ.offsetBy(start), FY.offsetBy(start)),
        Set.of(FH.offsetBy(start), FQ.offsetBy(start), FY.offsetBy(start)));
  }

  public static TimeSequence fiscalQuarterly(MonthOfYear month1) {
    return new QuarterlyGregorianSequence(
        FQ.offsetBy(month1), month1,
        Set.of(FH.offsetBy(month1), FY.offsetBy(month1)),
        Set.of(FH.offsetBy(month1), FY.offsetBy(month1)));
  }

  public static TimeSequence yearly(MonthOfYear month1) {
    return new YearlyGregorianSequence(GY.offsetBy(month1), month1, Set.of(), Set.of());
  }

  public static TimeSequence fiscalYearly(MonthOfYear month1) {
    return new YearlyGregorianSequence(FY.offsetBy(month1), month1, Set.of(), Set.of());
  }

  // ── Implementation ─────────────────────────────────────────────────────────

  private record DailyGregorianSequence(
      TimeSequenceTag tag, Set<TimeSequenceTag> partitions, Set<TimeSequenceTag> contributesTo)
      implements TimeSequence {

    DailyGregorianSequence {
      TimeSequence.checkTags(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return new Epoch2kInterval(day);
    }

    @Override
    public String format(Epoch2kInterval interval) {
      return formatFirstDayBasedSequence(this, interval);
    }
  }

  private record WeeklyGregorianSequence(
      TimeSequenceTag tag,
      DayOfWeek day1,
      Set<TimeSequenceTag> partitions,
      Set<TimeSequenceTag> contributesTo)
      implements TimeSequence {

    WeeklyGregorianSequence {
      if (tag.offsetAsDay() != day1)
        throw new IllegalArgumentException(
            "tag " + tag + " implies day1 " + tag.offsetAsDay() + " but record has " + day1);
      TimeSequence.checkTags(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return day.containingWeek(day1);
    }

    @Override
    public String format(Epoch2kInterval interval) {
      return formatFirstDayBasedSequence(this, interval);
    }
  }

  private record FortnightlyGregorianSequence(
      TimeSequenceTag tag,
      DayOfWeek day1,
      Set<TimeSequenceTag> partitions,
      Set<TimeSequenceTag> contributesTo)
      implements TimeSequence {

    FortnightlyGregorianSequence {
      if (tag.offsetAsDay() != day1)
        throw new IllegalArgumentException(
            "tag " + tag + " implies day1 " + tag.offsetAsDay() + " but record has " + day1);
      TimeSequence.checkTags(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return day.containingFortnight(day1);
    }

    @Override
    public String format(Epoch2kInterval interval) {
      return formatFirstDayBasedSequence(this, interval);
    }
  }

  private record MonthlyGregorianSequence(
      TimeSequenceTag tag, Set<TimeSequenceTag> partitions, Set<TimeSequenceTag> contributesTo)
      implements TimeSequence {

    MonthlyGregorianSequence {
      TimeSequence.checkTags(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return day.containingMonth();
    }

    @Override
    public String format(Epoch2kInterval interval) {
      return formatMonthBasedSequence(this, interval);
    }
  }

  private record BimesterGregorianSequence(
      TimeSequenceTag tag,
      MonthOfYear month1,
      Set<TimeSequenceTag> partitions,
      Set<TimeSequenceTag> contributesTo)
      implements TimeSequence {

    BimesterGregorianSequence {
      if (tag.offsetAsMonth() != month1)
        throw new IllegalArgumentException(
            "tag " + tag + " implies month1 " + tag.offsetAsMonth() + " but record has " + month1);
      TimeSequence.checkTags(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return day.containingBimester(month1);
    }

    @Override
    public String format(Epoch2kInterval interval) {
      return formatMonthBasedSequence(this, interval, 2);
    }
  }

  private record SemesterGregorianSequence(
      TimeSequenceTag tag,
      MonthOfYear month1,
      Set<TimeSequenceTag> partitions,
      Set<TimeSequenceTag> contributesTo)
      implements TimeSequence {

    SemesterGregorianSequence {
      if (tag.offsetAsMonth() != month1)
        throw new IllegalArgumentException(
            "tag " + tag + " implies month1 " + tag.offsetAsMonth() + " but record has " + month1);
      TimeSequence.checkTags(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return day.containingSemester(month1);
    }

    @Override
    public String format(Epoch2kInterval interval) {
      return formatMonthBasedSequence(this, interval, 6);
    }
  }

  private record QuarterlyGregorianSequence(
      TimeSequenceTag tag,
      MonthOfYear month1,
      Set<TimeSequenceTag> partitions,
      Set<TimeSequenceTag> contributesTo)
      implements TimeSequence {

    QuarterlyGregorianSequence {
      if (tag.offsetAsMonth() != month1)
        throw new IllegalArgumentException(
            "tag " + tag + " implies month1 " + tag.offsetAsMonth() + " but record has " + month1);
      TimeSequence.checkTags(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return day.containingQuarter(month1);
    }

    @Override
    public String format(Epoch2kInterval interval) {
      return formatMonthBasedSequence(this, interval, 3);
    }
  }

  private record YearlyGregorianSequence(
      TimeSequenceTag tag,
      MonthOfYear month1,
      Set<TimeSequenceTag> partitions,
      Set<TimeSequenceTag> contributesTo)
      implements TimeSequence {

    YearlyGregorianSequence {
      if (tag.offsetAsMonth() != month1)
        throw new IllegalArgumentException(
            "tag " + tag + " implies month1 " + tag.offsetAsMonth() + " but record has " + month1);
      TimeSequence.checkTags(partitions, contributesTo);
    }

    @Override
    public Epoch2kInterval interval(Epoch2kDay day) {
      return day.containingYear(month1);
    }

    @Override
    public String format(Epoch2kInterval interval) {
      return formatYearBasedSequence(this, interval);
    }
  }

  private static String formatYearBasedSequence(TimeSequence ts, Epoch2kInterval interval) {
    var slice = new TimeSlice(ts.tag(), interval);
    if (!TimeSequence.isMemberOf(ts, interval)) return slice.toString();
    if (ts.tag().offsetAsMonth() == MonthOfYear.January) {
      return slice.formatYYYY();
    }
    return slice.formatYYYY_MM_DD();
  }

  private static String formatMonthBasedSequence(TimeSequence ts, Epoch2kInterval interval) {
    TimeSlice slice = new TimeSlice(ts.tag(), interval);
    if (!TimeSequence.isMemberOf(ts, interval)) return slice.toString();
    return slice.formatYYYY_MM();
  }

  private static String formatMonthBasedSequence(
      TimeSequence ts, Epoch2kInterval interval, int monthsPerPosition) {
    var tag = ts.tag();
    var slice = new TimeSlice(tag, interval);
    if (!TimeSequence.isMemberOf(ts, interval)) return slice.toString();
    if (tag.offsetAsMonth() == MonthOfYear.January) {
      int position = interval.firstDay().monthOffset() / monthsPerPosition + 1;
      return slice.formatYYYYxN(position);
    }
    return slice.formatYYYY_MM_DD();
  }

  private static String formatFirstDayBasedSequence(TimeSequence ts, Epoch2kInterval interval) {
    TimeSlice slice = new TimeSlice(ts.tag(), interval);
    if (!TimeSequence.isMemberOf(ts, interval)) return slice.toString();
    return slice.formatYYYY_MM_DD();
  }
}
