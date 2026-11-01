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

  public static Map<BridgeFormat, TimeSequenceKey> calendarMapping() {
    return Map.ofEntries(
        Map.entry(YEARLY,             GY.noOffset()),
        Map.entry(YEARLY_FISCAL,      FY.noOffset()),
        Map.entry(MONTHLY,            GM.noOffset()),
        Map.entry(QUARTERLY,          GQ.noOffset()),
        Map.entry(QUARTERLY_FISCAL,   FQ.noOffset()),
        Map.entry(SEMESTER,           GS.noOffset()),
        Map.entry(SEMESTER_FISCAL,    FH.noOffset()),
        Map.entry(BIMESTER,           GB.noOffset()),
        Map.entry(WEEKLY,             GW.noOffset()),
        Map.entry(FORTNIGHTLY,        GF.noOffset()),
        Map.entry(DAILY,              GD.noOffset()));
  }

  public static Map<BridgeFormat, TimeSequenceKey> fiscalMapping() {
    return Map.ofEntries(
        Map.entry(YEARLY,             FY.noOffset()),
        Map.entry(YEARLY_FISCAL,      FY.noOffset()),
        Map.entry(MONTHLY,            FM.noOffset()),
        Map.entry(QUARTERLY,          FQ.noOffset()),
        Map.entry(QUARTERLY_FISCAL,   FQ.noOffset()),
        Map.entry(SEMESTER,           FH.noOffset()),
        Map.entry(SEMESTER_FISCAL,    FH.noOffset()),
        Map.entry(BIMESTER,           GB.noOffset()),
        Map.entry(WEEKLY,             GW.noOffset()),
        Map.entry(FORTNIGHTLY,        GF.noOffset()),
        Map.entry(DAILY,              GD.noOffset()));
  }
}