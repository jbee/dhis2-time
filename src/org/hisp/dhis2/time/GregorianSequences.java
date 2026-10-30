package org.hisp.dhis2.time;

import java.util.function.LongSupplier;

public final class GregorianSequences {

  record DailyGregorianSequence(Code code) implements TimeSequence {

    @Override
    public Epoch2kInterval today(LongSupplier nowUTC) {
      return new Epoch2kInterval(Epoch2kDay.today(nowUTC));
    }

    @Override
    public Epoch2kInterval next(Epoch2kInterval ref) {
      return new Epoch2kInterval(ref.last().tomorrow());
    }

    @Override
    public Epoch2kInterval previous(Epoch2kInterval ref) {
      return new Epoch2kInterval(ref.first().yesterday());
    }

    @Override
    public int lengthInDays() {
      return 1;
    }
  }

  /**
   * @param day1 the day considered the first day of the week
   */
  record WeeklyGregorianSequence(Code code, Epoch2kDay.Weekday day1)
      implements TimeSequence {

    @Override
    public Epoch2kInterval today(LongSupplier nowUTC) {
      return Epoch2kDay.today(nowUTC).containingWeek(day1);
    }

    @Override
    public Epoch2kInterval next(Epoch2kInterval ref) {
      return ref.last().tomorrow().containingWeek(day1);
    }

    @Override
    public Epoch2kInterval previous(Epoch2kInterval ref) {
      return ref.first().yesterday().containingWeek(day1);
    }

    @Override
    public int lengthInDays() {
      return 7;
    }
  }

  record BiWeeklyGregorianSequence(Code code, Epoch2kDay.Weekday day1)
      implements TimeSequence {

    @Override
    public Epoch2kInterval today(LongSupplier nowUTC) {
      // TODO interestingly the math has to take into account when (bi-)week 1 starts to check if we
      // are in bi-week 27 which can be only 1 week long
      return null;
    }

    @Override
    public Epoch2kInterval next(Epoch2kInterval ref) {
      return null;
    }

    @Override
    public Epoch2kInterval previous(Epoch2kInterval ref) {
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
    public Epoch2kInterval today(LongSupplier nowUTC) {
      return Epoch2kDay.today(nowUTC).containingMonth();
    }

    @Override
    public Epoch2kInterval next(Epoch2kInterval ref) {
      return ref.last().tomorrow().containingMonth();
    }

    @Override
    public Epoch2kInterval previous(Epoch2kInterval ref) {
      return ref.first().yesterday().containingMonth();
    }

    @Override
    public int lengthInDays() {
      return -30;
    }
  }

  record QuarterlyGregorianSequence(Code code, Epoch2kDay.Month month1) implements TimeSequence {

    @Override
    public Epoch2kInterval today(LongSupplier nowUTC) {
      return Epoch2kDay.today(nowUTC).containingQuarter(month1);
    }

    @Override
    public Epoch2kInterval next(Epoch2kInterval ref) {
      return ref.last().tomorrow().containingQuarter(month1);
    }

    @Override
    public Epoch2kInterval previous(Epoch2kInterval ref) {
      return ref.first().yesterday().containingQuarter(month1);
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
  record YearlyGregorianSequence(Code code, Epoch2kDay.Month month1) implements TimeSequence {

    @Override
    public Epoch2kInterval today(LongSupplier nowUTC) {
      return Epoch2kDay.today(nowUTC).containingYear(month1);
    }

    @Override
    public Epoch2kInterval next(Epoch2kInterval ref) {
      return ref.last().tomorrow().containingYear(month1);
    }

    @Override
    public Epoch2kInterval previous(Epoch2kInterval ref) {
      return ref.first().yesterday().containingYear(month1);
    }

    @Override
    public int lengthInDays() {
      return -365;
    }
  }
}
