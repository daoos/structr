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
import org.structr.core.GraphObjectMap;
import org.structr.core.property.DoubleProperty;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 * @author Christian Morgner
 */
public class UTMToLatLonFunction extends Function<Object, Object> {

	private static final String ERROR_MESSAGE            = "Usage: ${utmToLatLon(latitude, longitude)}. Example: ${utmToLatLon('32U 395473 5686479')}";
	private static final Logger logger                   = Logger.getLogger(UTMToLatLonFunction.class.getName());
	private static final String UTMHemisphere            = "SSSSSSSSSSNNNNNNNNNNN";
	private static final String UTMzdlChars              = "CDEFGHJKLMNPQRSTUVWXX";
	public static final DoubleProperty latitudeProperty  = new DoubleProperty("latitude");
	public static final DoubleProperty longitudeProperty = new DoubleProperty("longitude");

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

			final String utmString = (String)sources[0];
			if (utmString != null) {

				final String[] parts = utmString.split("[\\s]+");

				if (parts.length < 3) {

					logger.log(Level.WARNING, "Unsupported UTM string: this implementation only supports the full UTM format with spaces, e.g. 32U 439596 5967780 or 32 N 439596 5967780.");

				} else if (parts.length == 3) {

					// full UTM string
					// 32U 439596 5967780
					final String zone = parts[0];
					final String east = parts[1];
					final String north = parts[2];

					return utmToLatLon(zone, getHemisphereFromZone(zone), east, north);

				} else if (parts.length == 4) {

					// full UTM string with hemisphere indication
					// 32 N 439596 5967780
					final String zone       = parts[0];
					final String hemisphere = parts[1];
					final String east       = parts[2];
					final String north      = parts[3];

					return utmToLatLon(zone, hemisphere, east, north);

				}

			} else {

				logger.log(Level.WARNING, "Invalid argument(s), cannot convert to double: {0}, {1}", new Object[] { sources[0], sources[1] });
			}
		}

		return "Unsupported UTM string";
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
		return "utm_to_lat_lon";
	}

	// ----- private methods -----

	private String getHemisphereFromZone(final String zone) {

		String band = null;

		switch (zone.length()) {

			case 3:
				// we can safely assume a format of "32U"
				band = zone.substring(2);
				break;

			case 2:
				// can be either single digit zone plus band
				// or double digit zone w/o band..
				if (zone.matches("\\d\\D")) {

					// single-digit zone plus band
					band = zone.substring(1);
				}
				break;
		}

		if (band != null) {

			final int pos = UTMzdlChars.indexOf(band);
			if (pos >= 0) {

				return UTMHemisphere.substring(pos, pos+1);
			}
		}

		logger.log(Level.WARNING, "Unable to determine hemisphere from UTM zone, assuming NORTHERN hemisphere.");

		return "N";
	}


	private GraphObjectMap utmToLatLon(final String zone, final String hemisphere, final String east, final String north) {

		final GraphObjectMap obj = new GraphObjectMap();

		// clean zone string (remove all non-digits)
		final String cleanedZone = zone.replaceAll("[\\D]+", "");
		final StringBuilder epsg = new StringBuilder("EPSG:32");

		switch (hemisphere) {

			case "N":
				epsg.append("6");
				break;

			case "S":
				epsg.append("7");
				break;
		}

		// append "0" to zone number of single-digit
		if (cleanedZone.length() == 1) {
			epsg.append("0");
		}

		// append zone number
		epsg.append(cleanedZone);

		try {

			final CoordinateReferenceSystem src = CRS.decode(epsg.toString());
			final CoordinateReferenceSystem dst = CRS.decode("EPSG:4326");
			final MathTransform transform       = CRS.findMathTransform(src, dst, true);
			final DirectPosition sourcePt       = new DirectPosition2D(getDoubleOrNull(east), getDoubleOrNull(north));
			final DirectPosition targetPt       = transform.transform(sourcePt, null);

			obj.put(latitudeProperty, targetPt.getOrdinate(0));
			obj.put(longitudeProperty, targetPt.getOrdinate(1));

		} catch (Throwable t) {

			t.printStackTrace();
		}

		return obj;
	}
}
