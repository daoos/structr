/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.geo;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.CRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 * @author Christian Morgner
 */
public class LatLonToUTMFunction extends Function<Object, Object> {

	private static final String ERROR_MESSAGE = "Usage: ${latLonToUTM(latitude, longitude)}. Example: ${latLonToUTM(41.3445, 7.35)}";
	private static final Logger logger        = Logger.getLogger(LatLonToUTMFunction.class.getName());
	private static final String UTMzdlChars   = "CDEFGHJKLMNPQRSTUVWXX";

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

			final Double lat = getDoubleOrNull(sources[0]);
			final Double lon = getDoubleOrNull(sources[1]);

			if (lat != null && lon != null) {

				try {

					final StringBuilder epsg = new StringBuilder("EPSG:32");
					final int utmZone        = getUTMZone(lat, lon);

					if (lat < 0.0) {

						// southern hemisphere
						epsg.append("7");

					} else {

						// northern hemisphere
						epsg.append("6");
					}

					if (utmZone < 10) {
						epsg.append("0");
					}

					epsg.append(utmZone);


					final CoordinateReferenceSystem src = CRS.decode("EPSG:4326");
					final CoordinateReferenceSystem dst = CRS.decode(epsg.toString());
					final MathTransform transform       = CRS.findMathTransform(src, dst, true);
					final DirectPosition sourcePt       = new DirectPosition2D(lat, lon);
					final DirectPosition targetPt       = transform.transform(sourcePt, null);
					final String code                   = dst.getName().getCode();
					final int pos                       = code.lastIndexOf(" ") + 1;
					final String zoneName               = code.substring(pos, code.length() - 1);
					final String band                   = getLatitudeBand(lat, lon);
					final StringBuilder buf             = new StringBuilder();

					buf.append(zoneName);
					buf.append(band);
					buf.append(" ");
					buf.append((int)Math.rint(targetPt.getOrdinate(0)));
					buf.append(" ");
					buf.append((int)Math.rint(targetPt.getOrdinate(1)));

					// return result
					return buf.toString();

				} catch (Throwable t) {

					t.printStackTrace();
				}

			} else {

				logger.log(Level.WARNING, "Invalid argument(s), cannot convert to double: {0}, {1}", new Object[] { sources[0], sources[1] });
			}
		}

		return usage(ctx != null ? ctx.isJavaScriptContext() : false);
	}

	@Override
	public String usage(final boolean inJavaScriptContext) {
		return ERROR_MESSAGE;
	}

	@Override
	public String shortDescription() {
		return "Converts the given latitude/longitude coordinates into an UTM string.";
	}

	@Override
	public String getName() {
		return "lat_lon_to_utm";
	}

	// ----- private methods -----
	private int getUTMZone(final double lat, final double lon) {

		int zone = Double.valueOf(Math.floor((lon + 180.0) / 6.0)).intValue() + 1;

		if (lat >= 56.0 && lat < 64.0 && lon >= 3.0 && lon < 12.0) {
			zone = 32;
		}

		if (lat >= 72.0 && lat < 84.0) {
			if (lon >= 0.0 && lon < 9.0) {
				zone = 31;
			}

		} else if (lon >= 9.0 && lon < 21.0) {
			zone = 33;

		} else if (lon >= 21.0 && lon < 33.0) {

			zone = 35;

		} else if (lon >= 33.0 && lon < 42.0) {
			zone = 37;
		}

		return zone;
	}

	private String getLatitudeBand(final double lat, final double lon) {

		if (lat >= -80.0 && lat <= 84.0) {

			final double band = Math.floor((lat + 80.0) / 8.0);
			final int index   = Double.valueOf(band).intValue();

			return UTMzdlChars.substring(index, index+1);
		}

		return null;
	}
}
