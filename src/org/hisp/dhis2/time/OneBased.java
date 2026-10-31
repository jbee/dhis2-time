package org.hisp.dhis2.time;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marker for numeric values where {@code 1} is the first element — human-facing
 * numbering ("1st month", "15th day"). In this package it appears on the entry
 * points that accept user-shaped coordinates, e.g. {@code ofYMD(year, 1, 1)}
 * for January 1st.
 *
 * <p>Contrast with {@link ZeroBased}.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface OneBased {}