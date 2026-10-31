package org.hisp.dhis2.time;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Parses the string form of a period:
 * <ul>
 *   <li>{@code <key>/<date>+<length>} — explicit interval</li>
 *   <li>{@code <key>/<date>:<n>} — 1-based position in the linear sequence.
 *       {@code :1} is the anchor interval, {@code :n} is {@code n-1} calls to
 *       {@code next()}. The walk may cross natural boundaries (e.g.
 *       {@code GQ/2022:5} is Q1 2023).</li>
 * </ul>
 *
 * <p>The date is always Gregorian — it is what allows a valid {@link Epoch2kInterval}
 * to be computed without knowing which calendar the key belongs to. The key prefix
 * may be 2 or 3 chars regardless of which interval form follows; the 2-char form
 * implies {@link TimeSequenceKey#NO_OFFSET}.
 *
 * <p>Canonical form is always the {@code +} form, produced by
 * {@link TimeSequenceInput#toString()}.
 */
public final class TimeSequenceParser {

  private final Map<TimeSequenceKey, TimeSequence> registry;

  /**
   * @param sequences the sequences this parser can resolve keys for; duplicate keys
   *     are rejected at construction so lookups are unambiguous
   */
  public TimeSequenceParser(Collection<? extends TimeSequence> sequences) {
    Map<TimeSequenceKey, TimeSequence> map = new HashMap<>();
    for (TimeSequence s : sequences) {
      TimeSequence existing = map.putIfAbsent(s.key(), s);
      if (existing != null)
        throw new IllegalArgumentException(
            "duplicate key " + s.key() + " in registry: " + existing + " and " + s);
    }
    this.registry = Map.copyOf(map);
  }

  public TimeSequenceInput parse(String s) {
    int slash = s.indexOf('/');
    if (slash != 2 && slash != 3)
      throw new IllegalArgumentException(
          "expected 2- or 3-char key prefix followed by '/', got: " + s);

    TimeSequenceKey key = slash == 2
        ? new TimeSequenceKey(s.charAt(0), s.charAt(1))
        : new TimeSequenceKey(s.charAt(0), s.charAt(1), s.charAt(2));

    String tail = s.substring(slash + 1);
    int plus = tail.indexOf('+');
    int colon = tail.indexOf(':');

    if (plus >= 0 && colon >= 0)
      throw new IllegalArgumentException("cannot combine '+' and ':' in: " + s);
    if (plus >= 0)
      return parseExplicit(key, tail.substring(0, plus), tail.substring(plus + 1), s);
    if (colon >= 0)
      return parseWalk(key, tail.substring(0, colon), tail.substring(colon + 1), s);
    throw new IllegalArgumentException("missing '+' or ':' in: " + s);
  }

  private TimeSequenceInput parseExplicit(
      TimeSequenceKey key, String datePart, String lengthPart, String whole) {
    Epoch2kDay first = parseDate(datePart, whole);
    int length = parsePositive(lengthPart, "length", whole);
    return new TimeSequenceInput(key, new Epoch2kInterval(first, first.plusDays(length - 1)));
  }

  private TimeSequenceInput parseWalk(
      TimeSequenceKey key, String datePart, String nPart, String whole) {
    TimeSequence sequence = registry.get(key);
    if (sequence == null)
      throw new IllegalArgumentException(
          "no sequence registered for key " + key + " (in: " + whole + ")");

    Epoch2kDay anchor = parseDate(datePart, whole);
    int position = parsePositive(nPart, "position", whole);

    Epoch2kInterval current;
    try {
      current = sequence.interval(anchor);
      for (int i = 1; i < position; i++) current = sequence.next(current);
    } catch (IllegalArgumentException | NoSuchElementException e) {
      throw new IllegalArgumentException(
          "cannot resolve " + whole + " against " + key + ": " + e.getMessage(), e);
    }
    return new TimeSequenceInput(key, current);
  }

  private static Epoch2kDay parseDate(String s, String whole) {
    String[] parts = s.split("-", -1);
    if (parts.length < 1 || parts.length > 3)
      throw new IllegalArgumentException("bad date '" + s + "' in: " + whole);

    int year = parseYear(parts[0], whole);
    int month = parts.length >= 2 ? parseNumeric(parts[1], "month", whole) : 1;
    int day = parts.length == 3 ? parseNumeric(parts[2], "day", whole) : 1;
    try {
      return Epoch2kDay.ofYMD(year, month, day);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(e.getMessage() + " (in: " + whole + ")", e);
    }
  }

  private static int parseYear(String s, String whole) {
    if (s.length() != 4 || !allDigits(s))
      throw new IllegalArgumentException("year must be 4 digits: '" + s + "' in: " + whole);
    int y = Integer.parseInt(s);
    if (y < 1901 || y > 2099)
      throw new IllegalArgumentException("year out of range 1901-2099: " + y + " in: " + whole);
    return y;
  }

  private static int parseNumeric(String s, String label, String whole) {
    if (s.isEmpty() || !allDigits(s))
      throw new IllegalArgumentException(label + " must be numeric: '" + s + "' in: " + whole);
    return Integer.parseInt(s);
  }

  private static int parsePositive(String s, String label, String whole) {
    int n = parseNumeric(s, label, whole);
    if (n < 1)
      throw new IllegalArgumentException(label + " must be >= 1, got: " + n + " in: " + whole);
    return n;
  }

  private static boolean allDigits(String s) {
    return s.chars().allMatch(Character::isDigit);
  }
}