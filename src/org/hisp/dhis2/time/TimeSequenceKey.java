package org.hisp.dhis2.time;

public record TimeSequenceKey(char system, char interval, char offset) {

  public static final char NO_OFFSET = '-';

  // ── Typed factories (the only public way to set an offset) ─────────

  public static TimeSequenceKey weekly(char system, Epoch2kDay.DayOfWeek day1) {
    return new TimeSequenceKey(system, 'W', dayChar(day1));
  }

  public static TimeSequenceKey biWeekly(char system, Epoch2kDay.DayOfWeek day1) {
    return new TimeSequenceKey(system, 'B', dayChar(day1));
  }

  public static TimeSequenceKey monthly(char system, Epoch2kDay.MonthOfYear fiscalStart) {
    return new TimeSequenceKey(system, 'M', monthChar(fiscalStart));
  }

  public static TimeSequenceKey quarterly(char system, Epoch2kDay.MonthOfYear month1) {
    return new TimeSequenceKey(system, 'Q', monthChar(month1));
  }

  public static TimeSequenceKey yearly(char system, Epoch2kDay.MonthOfYear month1) {
    return new TimeSequenceKey(system, 'Y', monthChar(month1));
  }

  // ── Constructors ─────────────────────────────────────────────────────

  public TimeSequenceKey(char system, char interval) {
    this(system, interval, NO_OFFSET);
  }

  public TimeSequenceKey {
    if (!isValidLetter(system))
      throw new IllegalArgumentException(
          "system must be A-Z, got: '" + system + "' (" + (int) system + ")");
    if (!isValidLetter(interval))
      throw new IllegalArgumentException(
          "interval must be A-Z, got: '" + interval + "' (" + (int) interval + ")");
    if (!isValidOffset(offset))
      throw new IllegalArgumentException(
          "offset must be 0-9, a-z, or '-', got: '" + offset + "' (" + (int) offset + ")");
  }

  // ── Validation ─────────────────────────────────────────────────────

  /** {@code system} and {@code interval} are uppercase letters, distinguishing them from the offset. */
  private static boolean isValidLetter(char c) {
    return c >= 'A' && c <= 'Z';
  }

  /**
   * Allowed range for {@code offset}: {@code 0-9}, {@code a-z}, or the literal
   * {@link #NO_OFFSET} ({@code '-'}). Wider than the current Gregorian encoding needs
   * ({@code 1-9} + {@code a-c}) so other calendar systems and sequence types can
   * introduce their own offset alphabets without changing this validation.
   *
   * <p>Rejects uppercase letters (reserved for {@code system}/{@code interval}),
   * punctuation other than {@code '-'}, and whitespace.
   */
  private static boolean isValidOffset(char c) {
    return c == NO_OFFSET || (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z');
  }

  // ── Typed accessors ────────────────────────────────────────────────

  public Epoch2kDay.DayOfWeek day1() {
    if (interval != 'W' && interval != 'B')
      throw new IllegalStateException(interval + " has no day offset");
    return dayFromChar(offset);
  }

  public Epoch2kDay.MonthOfYear month1() {
    if (interval != 'M' && interval != 'Q' && interval != 'Y')
      throw new IllegalStateException(interval + " has no month offset");
    return monthFromChar(offset);
  }

  // ── Encoding (private) ─────────────────────────────────────────────

  private static char dayChar(Epoch2kDay.DayOfWeek d) {
    return (char) ('1' + d.ordinal());
  }

  private static char monthChar(Epoch2kDay.MonthOfYear m) {
    int n = m.ordinal();
    return n < 9 ? (char) ('1' + n) : (char) ('a' + (n - 9));
  }

  private static Epoch2kDay.DayOfWeek dayFromChar(char c) {
    if (c < '1' || c > '7') throw new IllegalArgumentException("Bad day offset: " + c);
    return Epoch2kDay.DayOfWeek.values()[c - '1'];
  }

  private static Epoch2kDay.MonthOfYear monthFromChar(char c) {
    if (c >= '1' && c <= '9') return Epoch2kDay.MonthOfYear.values()[c - '1'];
    if (c >= 'a' && c <= 'c') return Epoch2kDay.MonthOfYear.values()[9 + (c - 'a')];
    throw new IllegalArgumentException("Bad month offset: " + c);
  }

  /**
   * The key in string form, terminated by {@code '/'} so it reads as a prefix of the full period
   * string: {@code GW1/2024-01-01+7}.
   */
  @Override
  public String toString() {
    return offset == NO_OFFSET
        ? "" + system + interval + '/'
        : "" + system + interval + offset + '/';
  }

  public TimeFamilyKey family() {
    return new TimeFamilyKey(system, interval);
  }
}
