package org.hisp.dhis2.time;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marker for numeric values that are 0-based offsets. Purely for readability so number don't get
 * confused with {@link TimeSegment}s.
 *
 * <h4>Time-Zones and Daylight-saving</h4>
 *
 * Offsets are relative to an absolute fixpoint in time. Usually the epoch (fixpoint declared zero)
 * used in computing is 1970-01-01 00:00:00 UTC. As such offsets are <b>not</b> subject to
 * time-zones or summer/winter time complications. Only expressing the point in time an offset
 * points to in a conversation is usually adding in that layer, as humans cannot grasp numeric
 * offsets intuitively.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface TimeOffset {}
