package no.geosoft.jwlf;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

/**
 * A collection of useful utilities for JsonLog's.
 *
 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
 */
public final class JsonLogUtil
{
  /**
   * A simple way to keep track of latency within a system or a pipeline
   * is to add time stamp or latency curves to the log. This method will
   * add the specified latency curve to the given log and compute latency
   * from similar curves added earlier.
   * <p>
   * The curve name should have a numeric suffix, like TIME_T8 etc. or
   * such a suffix will be added.
   * <p>
   * The first curve of this pattern added will contain a timestamp (long
   * values of milliseconds since Epoch) while later curves added will contain
   * the latency (in milliseconds) since the <em>previous</em> curve was added.
   * <p>
   * Curve names may not be a consecutive sequence. TIME_T0 can be followed
   * by TIME_T4 and so on.
   *
   * @param jsonLog           Log to add latency curve to. Non-null.
   * @param curveName         Name of curve to add. A numeric suffix is added if
   *                          the name doesn't contain one already.
   * @param curveDescription  Curve description, typically describing the
   *                          performed task responsible for the latency. May be null.
   * @param isTotalLatency    True to make a grand total of latency curves added
   *                          earlier, false to make it a regular latency curve.
   * @return                  The added curve. Never null.
   * @throws IllegalArgumentException  If jsonLog or curveName is null.
   */
  public static JsonCurve addLatencyCurve(JsonLog jsonLog, String curveName,
                                          String curveDescription,
                                          boolean isTotalLatency)
  {
    if (jsonLog == null)
      throw new IllegalArgumentException("jsonLog cannot be null");

    if (curveName == null)
      throw new IllegalArgumentException("curveName cannot be null");

    //
    // Split curveName into base name and numeric suffix.
    // If it doesn't end in a number, suffix will be null.
    //
    String baseName = curveName;
    String suffixString = null;

    Pattern pattern = Pattern.compile("([a-zA-Z_\\s]*)(.*)$");
    Matcher matcher = pattern.matcher(curveName);
    if (matcher.find()) {
      baseName = matcher.group(1);
      suffixString = matcher.group(2);
    }

    //
    // Determine suffix. Start with the one provided (or 0), but check
    // if this exists and pick the next available one.
    //
    int suffix = 0;
    try {
      suffix = Integer.parseInt(suffixString);
    }
    catch (NumberFormatException exception) {
      suffix = 0;
    }
    while (true) {
      String name = baseName + suffix;
      if (jsonLog.findCurve(name) == null)
        break;
      suffix++;
    }

    //
    // Create the new curve
    //
    String newCurveName = isTotalLatency ? baseName : baseName + suffix;
    JsonCurve newLatencyCurve = new JsonCurve(newCurveName, curveDescription, "Time", "ms", Long.class, 1);

    //
    // Find all existing latency curves. Since latency curves
    // may not be consecutive we search a wide range.
    //
    List<JsonCurve> latencyCurves = new ArrayList<>();
    suffix = 0;
    while (suffix < 9999) {
      String name = baseName + suffix;
      JsonCurve curve = jsonLog.findCurve(name);
      if (curve != null)
        latencyCurves.add(curve);
      suffix++;
    }

    //
    // Time right now.
    //
    long now = System.currentTimeMillis();

    //
    // If this is the first latency curve, we populate with this number,
    // otherwise we subtract all numbers proir to this one.
    //
    for (int i = 0; i < jsonLog.getNValues(); i++) {

      // Pick the time now and subtract value from the other latency curves
      Long totalLatency = now;

      for (JsonCurve latencyCurve : latencyCurves) {
        Object value = latencyCurve.getValue(i);
        Long latency = (Long) no.geosoft.jwlf.Util.getAsType(value, Long.class);

        // In the total latency case we only want to subtract the
        // initial time stamp.
        if (isTotalLatency && latency != null && latency < 10000000L)
          latency = 0L;

        totalLatency = latency != null && totalLatency != null ? totalLatency - latency : null;
      }

      newLatencyCurve.addValue(totalLatency);
    }

    jsonLog.addCurve(newLatencyCurve);
    return newLatencyCurve;
  }
}
