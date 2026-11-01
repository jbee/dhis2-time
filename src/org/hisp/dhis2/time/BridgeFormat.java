package org.hisp.dhis2.time;

import java.util.Map;

import static org.hisp.dhis2.time.GregorianSequences.FH;
import static org.hisp.dhis2.time.GregorianSequences.FM;
import static org.hisp.dhis2.time.GregorianSequences.FQ;
import static org.hisp.dhis2.time.GregorianSequences.FY;
import static org.hisp.dhis2.time.GregorianSequences.GB;
import static org.hisp.dhis2.time.GregorianSequences.GD;
import static org.hisp.dhis2.time.GregorianSequences.GF;
import static org.hisp.dhis2.time.GregorianSequences.GM;
import static org.hisp.dhis2.time.GregorianSequences.GQ;
import static org.hisp.dhis2.time.GregorianSequences.GS;
import static org.hisp.dhis2.time.GregorianSequences.GW;
import static org.hisp.dhis2.time.GregorianSequences.GY;

/**
 * The syntactic categories of legacy DHIS2 period identifiers. Each value
 * corresponds to one suffix grammar in the legacy format, regardless of the
 * offset (month or day) the actual period carries. Offsets are resolved at
 * parse time from the string content.
 *
 * <p>Used as the key type for {@link BridgeParser}'s mapping from
 * kind to {@link TimeSequenceKey}. The mapping tells the parser which concrete
 * sequence family a legacy string of that kind belongs to; the parser fills in
 * the offset from the parsed components.
 */
public enum BridgeFormat {
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
  DAILY;

  /**
   * Mapping for calendar-mode parsing: bare formats ({@code 2024}, {@code 202403},
   * {@code 2024Q1}, {@code 2024S1}) resolve to calendar families; fiscal-marked
   * formats ({@code 2024July}, {@code 2024NovQ1}, {@code 2024AprilS1}) always
   * resolve to fiscal families regardless of this mapping.
   */
  public static Map<BridgeFormat, TimeFamilyKey> calendarMapping() {
    return Map.ofEntries(
        Map.entry(YEARLY,             GY),
        Map.entry(YEARLY_FISCAL,      FY),
        Map.entry(MONTHLY,            GM),
        Map.entry(QUARTERLY,          GQ),
        Map.entry(QUARTERLY_FISCAL,   FQ),
        Map.entry(SEMESTER,           GS),
        Map.entry(SEMESTER_FISCAL,    FH),
        Map.entry(BIMESTER,           GB),
        Map.entry(WEEKLY,             GW),
        Map.entry(FORTNIGHTLY,        GF),
        Map.entry(DAILY,              GD));
  }

  /**
   * Mapping for fiscal-mode parsing. Differs from {@link #calendarMapping()} only
   * on the four formats with no explicit fiscal marker in the string: those
   * resolve to fiscal families here instead of calendar.
   */
  public static Map<BridgeFormat, TimeFamilyKey> fiscalMapping() {
    return Map.ofEntries(
        Map.entry(YEARLY,             FY),
        Map.entry(YEARLY_FISCAL,      FY),
        Map.entry(MONTHLY,            FM),
        Map.entry(QUARTERLY,          FQ),
        Map.entry(QUARTERLY_FISCAL,   FQ),
        Map.entry(SEMESTER,           FH),
        Map.entry(SEMESTER_FISCAL,    FH),
        Map.entry(BIMESTER,           GB),
        Map.entry(WEEKLY,             GW),
        Map.entry(FORTNIGHTLY,        GF),
        Map.entry(DAILY,              GD));
  }
}