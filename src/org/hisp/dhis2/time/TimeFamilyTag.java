package org.hisp.dhis2.time;

public record TimeFamilyTag(char system, char interval) {

  public TimeFamilyTag {
    if (system < 'A' || system > 'Z')
      throw new IllegalArgumentException(
          "system must be A-Z, got: '" + system + "' (" + (int) system + ")");
    if (interval < 'A' || interval > 'Z')
      throw new IllegalArgumentException(
          "interval must be A-Z, got: '" + interval + "' (" + (int) interval + ")");
  }

  public TimeSequenceTag noOffset() {
    return new TimeSequenceTag(system, interval);
  }

  public TimeSequenceTag offsetBy(int offset) {
    return new TimeSequenceTag(system, interval, TimeSequenceTag.offsetChar(offset));
  }

  public TimeSequenceTag offsetBy(Epoch2kDay.DayOfWeek day1) {
    return new TimeSequenceTag(system, interval, TimeSequenceTag.offsetChar(day1));
  }

  public TimeSequenceTag offsetBy(Epoch2kDay.MonthOfYear month1) {
    return new TimeSequenceTag(system, interval, TimeSequenceTag.offsetChar(month1));
  }

  @Override
  public String toString() {
    // yields the canonical name
    return "" + system + interval;
  }
}
