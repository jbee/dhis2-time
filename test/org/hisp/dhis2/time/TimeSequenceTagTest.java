package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.Epoch2kDay.DayOfWeek.*;
import static org.hisp.dhis2.time.Epoch2kDay.MonthOfYear.*;
import static org.junit.jupiter.api.Assertions.*;

import org.hisp.dhis2.time.Epoch2kDay.DayOfWeek;
import org.hisp.dhis2.time.Epoch2kDay.MonthOfYear;
import org.junit.jupiter.api.Test;

class TimeSequenceTagTest {

  @Test
  void allDaysRoundTrip() {
    for (var d : DayOfWeek.values()) {
      assertEquals(d, GregorianSequences.GW.offsetBy(d).offsetAsDay());
      assertEquals(d, GregorianSequences.GF.offsetBy(d).offsetAsDay());
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
    assertEquals(TimeSequenceTag.NO_OFFSET, new TimeSequenceTag('G', 'M').offset());
  }

  @Test
  void toStringIncludesTrailingSlashAndOffset() {
    assertEquals("GW/", GregorianSequences.weekly(Monday).tag().toString());
    assertEquals("FY7/", GregorianSequences.fiscalYearly(July).tag().toString());
  }

  @Test
  void toStringOmitsDashOffset() {
    assertEquals("GM/", new TimeSequenceTag('G', 'M').toString());
  }

  @Test
  void rejectsLowercaseSystem() {
    assertThrows(IllegalArgumentException.class, () -> new TimeSequenceTag('g', 'M', '-'));
  }

  @Test
  void rejectsLowercaseInterval() {
    assertThrows(IllegalArgumentException.class, () -> new TimeSequenceTag('G', 'm', '-'));
  }

  @Test
  void rejectsUppercaseOffset() {
    assertThrows(IllegalArgumentException.class, () -> new TimeSequenceTag('G', 'M', 'A'));
  }

  @Test
  void rejectsPunctuationOffset() {
    assertThrows(IllegalArgumentException.class, () -> new TimeSequenceTag('G', 'M', '!'));
  }

  @Test
  void acceptsDigitLowercaseAndDashOffsets() {
    new TimeSequenceTag('G', 'M', '5');
    new TimeSequenceTag('G', 'M', 'z');
    new TimeSequenceTag('G', 'M', '-');
  }

  @Test
  void familyTagToStringHasNoSlash() {
    assertEquals("G-W".replace("-", ""), new TimeFamilyTag('G', 'W').toString());
  }
}
