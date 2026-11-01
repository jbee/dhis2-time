package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.Epoch2kDay.DayOfWeek.*;
import static org.hisp.dhis2.time.Epoch2kDay.MonthOfYear.*;
import static org.junit.jupiter.api.Assertions.*;

import org.hisp.dhis2.time.Epoch2kDay.DayOfWeek;
import org.hisp.dhis2.time.Epoch2kDay.MonthOfYear;
import org.junit.jupiter.api.Test;

class TimeSequenceKeyTest {

  @Test
  void allDaysRoundTrip() {
    for (var d : DayOfWeek.values()) {
      assertEquals(d, GregorianSequences.GW.offsetBy(d).offsetAsDay());
      assertEquals(d, GregorianSequences.GB.offsetBy(d).offsetAsDay());
    }
  }

  @Test
  void allMonthsRoundTrip() {
    for (var m : MonthOfYear.values()) {
      assertEquals(m, GregorianSequences.GM.offsetBy(m).offsetAsMonth());
      assertEquals(m, GregorianSequences.GQ.offsetBy(m).offsetAsMonth());
      assertEquals(m, GregorianSequences.GY.offsetBy(m).offsetAsMonth());
    }
  }

  @Test
  void twoArgConstructorUsesNoOffset() {
    assertEquals(TimeSequenceKey.NO_OFFSET, new TimeSequenceKey('G', 'M').offset());
  }

  @Test
  void toStringIncludesTrailingSlashAndOffset() {
    assertEquals("GW/", GregorianSequences.weekly(Monday).key().toString());
    assertEquals("FY7/", GregorianSequences.fiscalYearly(July).key().toString());
  }

  @Test
  void toStringOmitsDashOffset() {
    assertEquals("GM/", new TimeSequenceKey('G', 'M').toString());
  }

  @Test
  void rejectsLowercaseSystem() {
    assertThrows(IllegalArgumentException.class, () -> new TimeSequenceKey('g', 'M', '-'));
  }

  @Test
  void rejectsLowercaseInterval() {
    assertThrows(IllegalArgumentException.class, () -> new TimeSequenceKey('G', 'm', '-'));
  }

  @Test
  void rejectsUppercaseOffset() {
    assertThrows(IllegalArgumentException.class, () -> new TimeSequenceKey('G', 'M', 'A'));
  }

  @Test
  void rejectsPunctuationOffset() {
    assertThrows(IllegalArgumentException.class, () -> new TimeSequenceKey('G', 'M', '!'));
  }

  @Test
  void acceptsDigitLowercaseAndDashOffsets() {
    new TimeSequenceKey('G', 'M', '5');
    new TimeSequenceKey('G', 'M', 'z');
    new TimeSequenceKey('G', 'M', '-');
  }

  @Test
  void familyKeyToStringHasNoSlash() {
    assertEquals("G-W".replace("-", ""), new TimeFamilyKey('G', 'W').toString());
  }
}
