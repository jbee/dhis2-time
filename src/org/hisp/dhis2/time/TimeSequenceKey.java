package org.hisp.dhis2.time;

public record TimeSequenceKey(char system, char interval, char offset) {

  public static final char NO_OFFSET = '-';

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

  public boolean isOffset() {
    return offset != NO_OFFSET;
  }

  public int offsetAsInt() {
    char c = offset;
    if (c >= '0' && c <= '9') return c - '0';
    if (c >= 'a' && c <= 'c') return c - 'a';
    // should be unreachable as we do not accept chars outside above ranges
    throw new IllegalArgumentException("Bad offset: " + c);
  }

  public Epoch2kDay.DayOfWeek offsetAsDay() {
    char c = offset;
    if (c == NO_OFFSET) return Epoch2kDay.DayOfWeek.Monday;
    if (c < '1' || c > '7') throw new IllegalArgumentException("Bad day offset: " + c);
    return Epoch2kDay.DayOfWeek.values()[c - '1'];
  }

  public Epoch2kDay.MonthOfYear offsetAsMonth() {
    char c = offset;
    if (c == NO_OFFSET) return Epoch2kDay.MonthOfYear.January;
    if (c >= '1' && c <= '9') return Epoch2kDay.MonthOfYear.values()[c - '1'];
    if (c >= 'a' && c <= 'c') return Epoch2kDay.MonthOfYear.values()[9 + (c - 'a')];
    throw new IllegalArgumentException("Bad month offset: " + c);
  }

  // ── Encoding (private) ─────────────────────────────────────────────

  static char offsetChar(int offset) {
    if (offset < 0)
      throw new IllegalArgumentException(
          "offset must be 0-9, a-z, or '-', got: (underflow) (" + offset + ")");
    if (offset < 10) return (char) ('0'+offset);
    if (offset < 36) return (char) ('a'+(offset-10));
    throw new IllegalArgumentException(
        "offset must be 0-9, a-z, or '-', got: (overflow) (" + offset + ")");
  }

  static char offsetChar(Epoch2kDay.DayOfWeek d) {
    int n = d.ordinal();
    if (n == 0) return NO_OFFSET;
    return (char) ('1' + n);
  }

  static char offsetChar(Epoch2kDay.MonthOfYear m) {
    int n = m.ordinal();
    if (n == 0) return NO_OFFSET;
    return n < 9 ? (char) ('1' + n) : (char) ('a' + (n - 9));
  }

  /**
   * The key in string form, terminated by {@code '/'} so it reads as a prefix of the full period
   * string: {@code GW1/2024-01-01+7}.
   */
  @Override
  public String toString() {
    // yields the canonical name
    return offset == NO_OFFSET
        ? "" + system + interval + '/'
        : "" + system + interval + offset + '/';
  }

  public TimeFamilyKey family() {
    return new TimeFamilyKey(system, interval);
  }
}
