package org.hisp.dhis2.time;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marker for numeric values where {@code 0} is the first element — array-index-style
 * numbering. For dates, this covers both 0-based indices within a segment
 * ({@code dayOfYear() == 0} for January 1st) and offsets from a fixpoint
 * ({@code daysSince2k() == 0} for 2000-01-01).
 *
 * <p>Contrast with {@link OneBased}, which marks human-facing 1-based values.
 * Values that are neither (absolute years, counts) carry no annotation.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface ZeroBased {}