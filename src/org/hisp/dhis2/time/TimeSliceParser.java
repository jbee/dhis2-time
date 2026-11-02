package org.hisp.dhis2.time;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses the string form of a period:
 * <ul>
 *   <li>{@code <tag>/<date>+<length>} — explicit interval</li>
 *   <li>{@code <tag>/<date>:<n>} — 1-based position among the intervals of the
 *       sequence that overlap the anchor's calendar period. The period is
 *       determined by the anchor's precision: {@code YYYY} denotes the whole
 *       year, {@code YYYY-MM} the month, {@code YYYY-MM-DD} the single day.</li>
 * </ul>
 *
 * <p>When no {@code +} or {@code :} suffix is given, the period is the first
 * interval of the sequence that overlaps the anchor period — equivalent to
 * writing {@code :1}. For {@code GD/2024-03-15} this yields the single day
 * 2024-03-15 (GD's intervals are one day); for {@code GQ/2024-01-01} it yields
 * Q1 2024; for {@code ST/2024} it yields Spring 2024 (the first term
 * overlapping 2024, resolved via {@link TimeSequence#intervalOrNext}).
 *
 * <p>The date is always Gregorian — it is what allows a valid
 * {@link Epoch2kInterval} to be computed without knowing which calendar the
 * tag belongs to. The tag prefix may be 2 or 3 chars regardless of which
 * interval form follows; the 2-char form implies
 * {@link TimeSequenceTag#NO_OFFSET}.
 *
 * <p>Canonical form is always the {@code +} form, produced by
 * {@link TimeSlice#toString()}.
 *
 * <p>Parsing works directly on the input {@link CharSequence}: no intermediate
 * substrings are allocated on the success path. The whole input is passed
 * through to error messages, so failures include the original string.
 */
public final class TimeSliceParser {

  private final Map<TimeSequenceTag, TimeSequence> registry;

  public TimeSliceParser(Collection<? extends TimeSequence> sequences) {
    Map<TimeSequenceTag, TimeSequence> map = new HashMap<>();
    for (TimeSequence s : sequences) {
      TimeSequence existing = map.putIfAbsent(s.tag(), s);
      if (existing != null)
        throw new IllegalArgumentException(
            "duplicate tag " + s.tag() + " in registry: " + existing + " and " + s);
    }
    this.registry = Map.copyOf(map);
  }

  public TimeSlice parse(CharSequence s) {
    int slash = findChar(s, '/', 0);
    if (slash != 2 && slash != 3)
      throw new IllegalArgumentException(
          "expected 2- or 3-char tag prefix followed by '/', got: " + s);

    TimeSequenceTag tag = slash == 2
        ? new TimeSequenceTag(s.charAt(0), s.charAt(1))
        : new TimeSequenceTag(s.charAt(0), s.charAt(1), s.charAt(2));

    int tailStart = slash + 1;
    int plus = findChar(s, '+', tailStart);
    int colon = findChar(s, ':', tailStart);

    if (plus >= 0 && colon >= 0)
      throw new IllegalArgumentException("cannot combine '+' and ':' in: " + s);
    if (plus >= 0)
      return parseExplicit(tag, s, tailStart, plus, plus + 1, s.length());
    if (colon >= 0)
      return parseAtPosition(tag, s, tailStart, colon,
          parsePositive(s, colon + 1, s.length() - colon - 1, "position"));
    return parseAtPosition(tag, s, tailStart, s.length(), 1);
  }

  // ── :n form ────────────────────────────────────────────────────────

  /**
   * The shared resolution step for {@code :n} and the no-suffix form. {@code :n}
   * passes its parsed position; the no-suffix form passes {@code 1}.
   */
  private TimeSlice parseAtPosition(
      TimeSequenceTag tag, CharSequence s,
      int dateStart, int dateEnd, int position) {
    TimeSequence sequence = registry.get(tag);
    if (sequence == null)
      throw new IllegalArgumentException(
          "no sequence registered for tag " + tag + " (in: " + s + ")");

    Epoch2kInterval period = toPeriod(parseDateParts(s, dateStart, dateEnd - dateStart), s);
    List<Epoch2kInterval> overlapping = overlapping(sequence, period);
    if (position > overlapping.size())
      throw new IllegalArgumentException(
          "position " + position + " exceeds the " + overlapping.size()
              + " interval(s) of " + tag + " overlapping " + period
              + " (in: " + s + ")");

    return new TimeSlice(tag, overlapping.get(position - 1));
  }

  // ── +length form ───────────────────────────────────────────────────

  private TimeSlice parseExplicit(
      TimeSequenceTag tag, CharSequence s,
      int dateStart, int dateEnd, int lengthStart, int lengthEnd) {
    Epoch2kDay first = toEpochDay(parseDateParts(s, dateStart, dateEnd - dateStart), s);
    int length = parsePositive(s, lengthStart, lengthEnd - lengthStart, "length");
    return new TimeSlice(tag, new Epoch2kInterval(first, first.plusDays(length - 1)));
  }

  // ── Date parsing ───────────────────────────────────────────────────

  /**
   * @param precision 1 = year, 2 = month, 3 = day
   */
  private record DateParts(int year, int month, int day, int precision) {}

  private static DateParts parseDateParts(CharSequence s, int offset, int length) {
    if (length < 4)
      throw new IllegalArgumentException("date too short in: " + s);

    int firstDash = -1, secondDash = -1, dashCount = 0;
    for (int i = offset; i < offset + length; i++) {
      if (s.charAt(i) == '-') {
        if (dashCount == 0) firstDash = i;
        else if (dashCount == 1) secondDash = i;
        dashCount++;
      }
    }

    if (dashCount > 2)
      throw new IllegalArgumentException("too many '-' in date in: " + s);

    if (dashCount == 0) {
      return switch (length) {
        case 4 -> new DateParts(parseYear(s, offset), 1, 1, 1);
        case 6 -> new DateParts(
            parseYear(s, offset),
            parseNumeric(s, offset + 4, 2, "month"), 1, 2);
        case 8 -> new DateParts(
            parseYear(s, offset),
            parseNumeric(s, offset + 4, 2, "month"),
            parseNumeric(s, offset + 6, 2, "day"), 3);
        default -> throw new IllegalArgumentException(
            "compact date must be 4 (YYYY), 6 (YYYYMM), or 8 (YYYYMMDD) digits, got "
                + length + " in: " + s);
      };
    }

    if (firstDash != offset + 4)
      throw new IllegalArgumentException("year must be exactly 4 digits in: " + s);

    int year = parseYear(s, offset);
    int monthEnd = (dashCount == 2) ? secondDash : offset + length;
    int month = parseNumeric(s, firstDash + 1, monthEnd - firstDash - 1, "month");

    if (dashCount == 1) return new DateParts(year, month, 1, 2);

    int day = parseNumeric(s, secondDash + 1, offset + length - secondDash - 1, "day");
    return new DateParts(year, month, day, 3);
  }

  private static Epoch2kDay toEpochDay(DateParts p, CharSequence whole) {
    try {
      return Epoch2kDay.ofYMD(p.year, p.month, p.day);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(e.getMessage() + " (in: " + whole + ")", e);
    }
  }

  private static Epoch2kInterval toPeriod(DateParts p, CharSequence whole) {
    Epoch2kDay first = toEpochDay(p, whole);
    return switch (p.precision) {
      case 1 -> new Epoch2kInterval(first, first.plusYears(1).minusDays(1));
      case 2 -> new Epoch2kInterval(first, first.plusMonths(1).minusDays(1));
      default -> new Epoch2kInterval(first);
    };
  }

  // ── Primitive scanners ─────────────────────────────────────────────

  private static int findChar(CharSequence s, char target, int from) {
    for (int i = from; i < s.length(); i++)
      if (s.charAt(i) == target) return i;
    return -1;
  }

  private static int parseYear(CharSequence s, int offset) {
    int y = parseNumeric(s, offset, 4, "year");
    if (y < 1901 || y > 2099)
      throw new IllegalArgumentException("year out of range 1901-2099: " + y + " in: " + s);
    return y;
  }

  private static int parseNumeric(CharSequence s, int offset, int length, String label) {
    if (length <= 0)
      throw new IllegalArgumentException(label + " is empty in: " + s);
    int result = 0;
    for (int i = offset; i < offset + length; i++) {
      char c = s.charAt(i);
      if (c < '0' || c > '9')
        throw new IllegalArgumentException(
            label + " must be digits, found '" + c + "' in: " + s);
      int digit = c - '0';
      if (result > (Integer.MAX_VALUE - digit) / 10)
        throw new IllegalArgumentException(label + " overflow in: " + s);
      result = result * 10 + digit;
    }
    return result;
  }

  private static int parsePositive(CharSequence s, int offset, int length, String label) {
    int n = parseNumeric(s, offset, length, label);
    if (n < 1)
      throw new IllegalArgumentException(label + " must be >= 1, got: " + n + " in: " + s);
    return n;
  }

  // ── Overlap enumeration (unchanged) ────────────────────────────────

  private static List<Epoch2kInterval> overlapping(
      TimeSequence sequence, Epoch2kInterval period) {
    Epoch2kInterval current;
    try {
      current = sequence.intervalOrNext(period.firstDay());
    } catch (IllegalArgumentException e) {
      return List.of();
    }

    List<Epoch2kInterval> result = new ArrayList<>();
    while (current.firstDay().notAfter(period.lastDay())) {
      result.add(current);
      try {
        current = sequence.next(current);
      } catch (RuntimeException e) {
        break;
      }
    }
    return List.copyOf(result);
  }
}