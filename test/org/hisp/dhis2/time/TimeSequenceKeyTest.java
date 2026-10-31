package org.hisp.dhis2.time;

import static org.hisp.dhis2.time.Epoch2kDay.DayOfWeek.*;
import static org.hisp.dhis2.time.Epoch2kDay.MonthOfYear.*;
import static org.junit.jupiter.api.Assertions.*;

import org.hisp.dhis2.time.Epoch2kDay.DayOfWeek;
import org.hisp.dhis2.time.Epoch2kDay.MonthOfYear;
import org.junit.jupiter.api.Test;

class TimeSequenceKeyTest {

  @Test
  void weeklyFactoryEncodesMondayAsDigit1() {
    assertEquals('1', TimeSequenceKey.weekly('G', Monday).offset());
  }

  @Test
  void weeklyFactoryEncodesSundayAsDigit7() {
    assertEquals('7', TimeSequenceKey.weekly('G', Sunday).offset());
  }

  @Test
  void yearlyFactoryEncodesJanuaryAsDigit1() {
    assertEquals('1', TimeSequenceKey.yearly('G', January).offset());
  }

  @Test
  void yearlyFactoryEncodesSeptemberAsDigit9() {
    assertEquals('9', TimeSequenceKey.yearly('G', September).offset());
  }

  @Test
  void yearlyFactoryEncodesOctoberAsLowercase_a() {
    assertEquals('a', TimeSequenceKey.yearly('G', October).offset());
  }

  @Test
  void yearlyFactoryEncodesDecemberAsLowercase_c() {
    assertEquals('c', TimeSequenceKey.yearly('G', December).offset());
  }

  @Test
  void allDaysRoundTrip() {
    for (var d : DayOfWeek.values()) {
      assertEquals(d, TimeSequenceKey.weekly('G', d).day1());
      assertEquals(d, TimeSequenceKey.biWeekly('G', d).day1());
    }
  }

  @Test
  void allMonthsRoundTrip() {
    for (var m : MonthOfYear.values()) {
      assertEquals(m, TimeSequenceKey.monthly('F', m).month1());
      assertEquals(m, TimeSequenceKey.quarterly('G', m).month1());
      assertEquals(m, TimeSequenceKey.yearly('G', m).month1());
    }
  }

  @Test
  void twoArgConstructorUsesNoOffset() {
    assertEquals(TimeSequenceKey.NO_OFFSET, new TimeSequenceKey('G', 'M').offset());
  }

  @Test
  void toStringIncludesTrailingSlashAndOffset() {
    assertEquals("GW1/", TimeSequenceKey.weekly('G', Monday).toString());
    assertEquals("FY7/", TimeSequenceKey.yearly('F', July).toString());
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
  void day1OnNonWeekIntervalThrows() {
    assertThrows(IllegalStateException.class, () -> new TimeSequenceKey('G', 'M').day1());
  }

  @Test
  void month1OnNonMonthIntervalThrows() {
    assertThrows(IllegalStateException.class, () -> new TimeSequenceKey('G', 'D').month1());
  }

  @Test
  void day1OnKeyWithoutOffsetThrows() {
    assertThrows(IllegalArgumentException.class, () -> new TimeSequenceKey('G', 'W').day1());
  }

  @Test
  void familyStripsOffset() {
    assertEquals(new TimeFamilyKey('G', 'W'),
        TimeSequenceKey.weekly('G', Monday).family());
  }

  @Test
  void familyKeyToStringHasNoSlash() {
    assertEquals("G-W".replace("-", ""), new TimeFamilyKey('G', 'W').toString());
  }
}