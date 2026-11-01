package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.GregorianSequences.*;
import java.util.Map;

public class BridgeMappings {

  public static Map<BridgeFormat, TimeSequenceKey> calendarMapping() {
    return Map.ofEntries(
        Map.entry(BridgeFormat.YEARLY,             GY.noOffset()),
        Map.entry(BridgeFormat.YEARLY_FISCAL,      FY.noOffset()),
        Map.entry(BridgeFormat.MONTHLY,            GM.noOffset()),
        Map.entry(BridgeFormat.QUARTERLY,          GQ.noOffset()),
        Map.entry(BridgeFormat.QUARTERLY_FISCAL,   FQ.noOffset()),
        Map.entry(BridgeFormat.SEMESTER,           GS.noOffset()),
        Map.entry(BridgeFormat.SEMESTER_FISCAL,    FH.noOffset()),
        Map.entry(BridgeFormat.BIMESTER,           GB.noOffset()),
        Map.entry(BridgeFormat.WEEKLY,             GW.noOffset()),
        Map.entry(BridgeFormat.FORTNIGHTLY,        GF.noOffset()),
        Map.entry(BridgeFormat.DAILY,              GD.noOffset()));
  }

  public static Map<BridgeFormat, TimeSequenceKey> fiscalMapping() {
    return Map.ofEntries(
        Map.entry(BridgeFormat.YEARLY,             FY.noOffset()),
        Map.entry(BridgeFormat.YEARLY_FISCAL,      FY.noOffset()),
        Map.entry(BridgeFormat.MONTHLY,            FM.noOffset()),
        Map.entry(BridgeFormat.QUARTERLY,          FQ.noOffset()),
        Map.entry(BridgeFormat.QUARTERLY_FISCAL,   FQ.noOffset()),
        Map.entry(BridgeFormat.SEMESTER,           FH.noOffset()),
        Map.entry(BridgeFormat.SEMESTER_FISCAL,    FH.noOffset()),
        Map.entry(BridgeFormat.BIMESTER,           GB.noOffset()),
        Map.entry(BridgeFormat.WEEKLY,             GW.noOffset()),
        Map.entry(BridgeFormat.FORTNIGHTLY,        GF.noOffset()),
        Map.entry(BridgeFormat.DAILY,              GD.noOffset()));
  }
}
