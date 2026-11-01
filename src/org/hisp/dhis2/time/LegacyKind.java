package org.hisp.dhis2.time;

/**
 * The syntactic categories of legacy DHIS2 period identifiers. Each value
 * corresponds to one suffix grammar in the legacy format, regardless of the
 * offset (month or day) the actual period carries. Offsets are resolved at
 * parse time from the string content.
 *
 * <p>Used as the key type for {@link LegacyPeriodParser}'s mapping from
 * kind to {@link TimeSequenceKey}. The mapping tells the parser which concrete
 * sequence family a legacy string of that kind belongs to; the parser fills in
 * the offset from the parsed components.
 */
public enum LegacyKind {
  YEARLY,
  YEARLY_FISCAL,
  MONTHLY,
  QUARTERLY,
  QUARTERLY_FISCAL,
  SEMESTER,
  SEMESTER_FISCAL,
  BIMESTER,
  WEEKLY,
  FORTNIGHTLY,
  DAILY
}