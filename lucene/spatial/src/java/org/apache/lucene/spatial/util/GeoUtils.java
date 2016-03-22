/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.spatial.util;

import java.util.ArrayList;

import org.apache.lucene.util.SloppyMath;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.PI;
import static java.lang.Math.abs;

import static org.apache.lucene.util.SloppyMath.asin;
import static org.apache.lucene.util.SloppyMath.cos;
import static org.apache.lucene.util.SloppyMath.TO_DEGREES;
import static org.apache.lucene.util.SloppyMath.TO_RADIANS;
import static org.apache.lucene.spatial.util.GeoEncodingUtils.TOLERANCE;
import static org.apache.lucene.spatial.util.GeoProjectionUtils.MAX_LAT_RADIANS;
import static org.apache.lucene.spatial.util.GeoProjectionUtils.MAX_LON_RADIANS;
import static org.apache.lucene.spatial.util.GeoProjectionUtils.MIN_LAT_RADIANS;
import static org.apache.lucene.spatial.util.GeoProjectionUtils.MIN_LON_RADIANS;
import static org.apache.lucene.spatial.util.GeoProjectionUtils.SEMIMAJOR_AXIS;

/**
 * Basic reusable geo-spatial utility methods
 *
 * @lucene.experimental
 */
public final class GeoUtils {
  /** Minimum longitude value. */
  public static final double MIN_LON_INCL = -180.0D;

  /** Maximum longitude value. */
  public static final double MAX_LON_INCL = 180.0D;

  /** Minimum latitude value. */
  public static final double MIN_LAT_INCL = -90.0D;

  /** Maximum latitude value. */
  public static final double MAX_LAT_INCL = 90.0D;

  // No instance:
  private GeoUtils() {
  }

  /** validates latitude value is within standard +/-90 coordinate bounds */
  public static boolean isValidLat(double lat) {
    return Double.isNaN(lat) == false && lat >= MIN_LAT_INCL && lat <= MAX_LAT_INCL;
  }

  /** validates longitude value is within standard +/-180 coordinate bounds */
  public static boolean isValidLon(double lon) {
    return Double.isNaN(lon) == false && lon >= MIN_LON_INCL && lon <= MAX_LON_INCL;
  }

  /** Puts longitude in range of -180 to +180. */
  public static double normalizeLon(double lon_deg) {
    if (lon_deg >= -180 && lon_deg <= 180) {
      return lon_deg; //common case, and avoids slight double precision shifting
    }
    double off = (lon_deg + 180) % 360;
    if (off < 0) {
      return 180 + off;
    } else if (off == 0 && lon_deg > 0) {
      return 180;
    } else {
      return -180 + off;
    }
  }

  /** Puts latitude in range of -90 to 90. */
  public static double normalizeLat(double lat_deg) {
    if (lat_deg >= -90 && lat_deg <= 90) {
      return lat_deg; //common case, and avoids slight double precision shifting
    }
    double off = abs((lat_deg + 90) % 360);
    return (off <= 180 ? off : 360-off) - 90;
  }

  /** Compute Bounding Box for a circle using WGS-84 parameters */
  public static GeoRect circleToBBox(final double centerLat, final double centerLon, final double radiusMeters) {
    final double radLat = TO_RADIANS * centerLat;
    final double radLon = TO_RADIANS * centerLon;
    double radDistance = radiusMeters / SEMIMAJOR_AXIS;
    double minLat = radLat - radDistance;
    double maxLat = radLat + radDistance;
    double minLon;
    double maxLon;

    if (minLat > MIN_LAT_RADIANS && maxLat < MAX_LAT_RADIANS) {
      double deltaLon = asin(sloppySin(radDistance) / cos(radLat));
      minLon = radLon - deltaLon;
      if (minLon < MIN_LON_RADIANS) {
        minLon += 2d * PI;
      }
      maxLon = radLon + deltaLon;
      if (maxLon > MAX_LON_RADIANS) {
        maxLon -= 2d * PI;
      }
    } else {
      // a pole is within the distance
      minLat = max(minLat, MIN_LAT_RADIANS);
      maxLat = min(maxLat, MAX_LAT_RADIANS);
      minLon = MIN_LON_RADIANS;
      maxLon = MAX_LON_RADIANS;
    }

    return new GeoRect(TO_DEGREES * minLat, TO_DEGREES * maxLat, TO_DEGREES * minLon, TO_DEGREES * maxLon);
  }

  /** Compute Bounding Box for a polygon using WGS-84 parameters */
  public static GeoRect polyToBBox(double[] polyLats, double[] polyLons) {
    if (polyLons.length != polyLats.length) {
      throw new IllegalArgumentException("polyLons and polyLats must be equal length");
    }

    double minLon = Double.POSITIVE_INFINITY;
    double maxLon = Double.NEGATIVE_INFINITY;
    double minLat = Double.POSITIVE_INFINITY;
    double maxLat = Double.NEGATIVE_INFINITY;

    for (int i=0;i<polyLats.length;i++) {
      if (GeoUtils.isValidLon(polyLons[i]) == false) {
        throw new IllegalArgumentException("invalid polyLons[" + i + "]=" + polyLons[i]);
      }
      if (GeoUtils.isValidLat(polyLats[i]) == false) {
        throw new IllegalArgumentException("invalid polyLats[" + i + "]=" + polyLats[i]);
      }
      minLon = min(polyLons[i], minLon);
      maxLon = max(polyLons[i], maxLon);
      minLat = min(polyLats[i], minLat);
      maxLat = max(polyLats[i], maxLat);
    }
    // expand bounding box by TOLERANCE factor to handle round-off error
    return new GeoRect(max(minLat - TOLERANCE, MIN_LAT_INCL), min(maxLat + TOLERANCE, MAX_LAT_INCL),
                       max(minLon - TOLERANCE, MIN_LON_INCL), min(maxLon + TOLERANCE, MAX_LON_INCL));
  }
  
  // some sloppyish stuff, do we really need this to be done in a sloppy way?
  // unless it is performance sensitive, we should try to remove.
  static final double PIO2 = Math.PI / 2D;

  /**
   * Returns the trigonometric sine of an angle converted as a cos operation.
   * <p>
   * Note that this is not quite right... e.g. sin(0) != 0
   * <p>
   * Special cases:
   * <ul>
   *  <li>If the argument is {@code NaN} or an infinity, then the result is {@code NaN}.
   * </ul>
   * @param a an angle, in radians.
   * @return the sine of the argument.
   * @see Math#sin(double)
   */
  // TODO: deprecate/remove this? at least its no longer public.
  static double sloppySin(double a) {
    return cos(a - PIO2);
  }
  

  /**
   * Returns the trigonometric tangent of an angle converted in terms of a sin/cos operation.
   * <p>
   * Note that this is probably not quite right (untested)
   * <p>
   * Special cases:
   * <ul>
   *  <li>If the argument is {@code NaN} or an infinity, then the result is {@code NaN}.
   * </ul>
   * @param a an angle, in radians.
   * @return the tangent of the argument.
   * @see Math#sin(double) aand Math#cos(double)
   */
  // TODO: deprecate/remove this? at least its no longer public.
  static double sloppyTan(double a) {
    return sloppySin(a) / cos(a);
  }
}
