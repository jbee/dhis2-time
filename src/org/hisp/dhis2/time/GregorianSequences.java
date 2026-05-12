package org.hisp.dhis2.time;

import java.util.function.LongSupplier;

public final class GregorianSequences {

  record DailyGregorianSequence(Code code) implements TimeSequence {

    @Override
    public IsoInterval today(LongSupplier nowUTC) {
      return new IsoInterval(IsoDay.today(nowUTC));
    }

    @Override
    public IsoInterval next(IsoInterval ref) {
      return new IsoInterval(ref.last().tomorrow());
    }

    @Override
    public IsoInterval previous(IsoInterval ref) {
      return new IsoInterval(ref.first().yesterday());
    }

    @Override
    public int lengthInDays() {
      return 1;
    }
  }

  /**
   * @param day1 the day considered the first day of the week
   */
  record WeeklyGregorianSequence(Code code, IsoDay.Weekday day1)
      implements TimeSequence {

    @Override
    public IsoInterval today(LongSupplier nowUTC) {
      return IsoDay.today(nowUTC).inWeek(day1);
    }

    @Override
    public IsoInterval next(IsoInterval ref) {
      return ref.last().tomorrow().inWeek(day1);
    }

    @Override
    public IsoInterval previous(IsoInterval ref) {
      return ref.first().yesterday().inWeek(day1);
    }

    @Override
    public int lengthInDays() {
      return 7;
    }
  }

  record BiWeeklyGregorianSequence(Code code, IsoDay.Weekday startingWeekday)
      implements TimeSequence {

    @Override
    public IsoInterval today(LongSupplier nowUTC) {
      // TODO interestingly the math has to take into account when (bi-)week 1 starts to check if we
      // are in bi-week 27 which can be only 1 week long
      return null;
    }

    @Override
    public IsoInterval next(IsoInterval ref) {
      return null;
    }

    @Override
    public IsoInterval previous(IsoInterval ref) {
      return null;
    }

    @Override
    public int lengthInDays() {
      // sometimes we have a 1 week-bi-week because there is a week 53
      return -14;
    }
  }

  /**
   * @param code for the sequence
   */
  record MonthlyGregorianSequence(Code code) implements TimeSequence {

    @Override
    public IsoInterval today(LongSupplier nowUTC) {
      return IsoDay.today(nowUTC).inMonth();
    }

    @Override
    public IsoInterval next(IsoInterval ref) {
      return ref.last().tomorrow().inMonth();
    }

    @Override
    public IsoInterval previous(IsoInterval ref) {
      return ref.first().yesterday().inMonth();
    }

    @Override
    public int lengthInDays() {
      return -30;
    }
  }

  record QuarterlyGregorianSequence(Code code, IsoDay.Month month1) implements TimeSequence {

    @Override
    public IsoInterval today(LongSupplier nowUTC) {
      return IsoDay.today(nowUTC).inQuarter(month1);
    }

    @Override
    public IsoInterval next(IsoInterval ref) {
      return ref.last().tomorrow().inQuarter(month1);
    }

    @Override
    public IsoInterval previous(IsoInterval ref) {
      return ref.first().yesterday().inQuarter(month1);
    }

    @Override
    public int lengthInDays() {
      return -3 * 30;
    }
  }

  /**
   * @param code
   * @param month1 the month considered the first month of the year(ly cycle)
   */
  record YearlyGregorianSequence(Code code, IsoDay.Month month1) implements TimeSequence {

    @Override
    public IsoInterval today(LongSupplier nowUTC) {
      return IsoDay.today(nowUTC).inYear(month1);
    }

    @Override
    public IsoInterval next(IsoInterval ref) {
      return ref.last().tomorrow().inYear(month1);
    }

    @Override
    public IsoInterval previous(IsoInterval ref) {
      return ref.first().yesterday().inYear(month1);
    }

    @Override
    public int lengthInDays() {
      return -365;
    }
  }
}
