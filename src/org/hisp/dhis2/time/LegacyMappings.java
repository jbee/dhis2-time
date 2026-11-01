package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.GregorianSequences.*;
import java.util.Map;

public class LegacyMappings {

  public static Map<LegacyKind, TimeSequenceKey> calendarMapping() {
    return Map.ofEntries(
        Map.entry(LegacyKind.YEARLY,             GY.noOffset()),
        Map.entry(LegacyKind.YEARLY_FISCAL,      FY.noOffset()),
        Map.entry(LegacyKind.MONTHLY,            GM.noOffset()),
        Map.entry(LegacyKind.QUARTERLY,          GQ.noOffset()),
        Map.entry(LegacyKind.QUARTERLY_FISCAL,   FQ.noOffset()),
        Map.entry(LegacyKind.SEMESTER,           GS.noOffset()),
        Map.entry(LegacyKind.SEMESTER_FISCAL,    FH.noOffset()),
        Map.entry(LegacyKind.BIMESTER,           GB.noOffset()),
        Map.entry(LegacyKind.WEEKLY,             GW.noOffset()),
        Map.entry(LegacyKind.FORTNIGHTLY,        GF.noOffset()),
        Map.entry(LegacyKind.DAILY,              GD.noOffset()));
  }

  public static Map<LegacyKind, TimeSequenceKey> fiscalMapping() {
    return Map.ofEntries(
        Map.entry(LegacyKind.YEARLY,             FY.noOffset()),
        Map.entry(LegacyKind.YEARLY_FISCAL,      FY.noOffset()),
        Map.entry(LegacyKind.MONTHLY,            FM.noOffset()),
        Map.entry(LegacyKind.QUARTERLY,          FQ.noOffset()),
        Map.entry(LegacyKind.QUARTERLY_FISCAL,   FQ.noOffset()),
        Map.entry(LegacyKind.SEMESTER,           FH.noOffset()),
        Map.entry(LegacyKind.SEMESTER_FISCAL,    FH.noOffset()),
        Map.entry(LegacyKind.BIMESTER,           GB.noOffset()),
        Map.entry(LegacyKind.WEEKLY,             GW.noOffset()),
        Map.entry(LegacyKind.FORTNIGHTLY,        GF.noOffset()),
        Map.entry(LegacyKind.DAILY,              GD.noOffset()));
  }
}
