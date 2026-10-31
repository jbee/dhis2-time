package org.hisp.dhis2.time;

public record TimeFamilyKey(char system, char interval) {

  public TimeFamilyKey {
    if (system < 'A' || system > 'Z')
      throw new IllegalArgumentException(
          "system must be A-Z, got: '" + system + "' (" + (int) system + ")");
    if (interval < 'A' || interval > 'Z')
      throw new IllegalArgumentException(
          "interval must be A-Z, got: '" + interval + "' (" + (int) interval + ")");
  }

  @Override
  public String toString() {
    return "" + system + interval;
  }
}
