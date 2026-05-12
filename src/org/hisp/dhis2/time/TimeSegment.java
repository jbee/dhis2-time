package org.hisp.dhis2.time;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marker for 1-based time numbers originating from segmenting time into segments where the first
 * segment usually is called 1st or just 1 like the 1st month in a year is 1, the 1st day in a week
 * or month is 1.
 *
 * <p>Purely for readability so number don't get confused with {@link TimeOffset}s.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface TimeSegment {}
