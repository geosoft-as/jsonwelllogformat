package no.geosoft.jwlf;

import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Date parser for the ISO 8601 format.
 * <p>
 * Java/SDK have several implementation of ISO 8601 parsers, but neither
 * comply to all aspects of the format so we do it properly here.
 * <p>
 * An ISO 8601 date/time representation has the following form:
 * <pre>
 *      &lt;date&gt;T&lt;time&gt;&lt;zone&gt;
 * </pre>
 * The 'T' separator between date and time is mandatory if time is present,
 * but often a space is used instead. This is not allowed by ISO 8601, but
 * we allow it here anyway.
 * <p>
 * &lt;date&gt; has the following form (hyphens are optional):
 * <pre>
 *   2018-11-24    ; Normal form
 *   2018-11       ; Year and month only, day = 01 implied
 *   2018          ; Year only, January 1 implied
 *   2018-W47      ; Week number. First day of week implied
 *   2018-W47-6    ; Week number and day of week
 *   2018-328      ; Ordinal date (day number of year)
 *   20            ; Century only. January 1, 00 assumed
 * </pre>
 * <p>
 * Note that the standard has opening for using years with more than four
 * digits, prefixed by +/- "if agreed by the communicating parties".
 * We don't support this here since we don't have a communicating partner
 * anyway.
 * <p>
 * &lt;time&gt; (optional) has the following form (colons are optional,
 * fraction delimiter can be dot or comma):
 * <pre>
 *                 ; Absent 00:00 implied
 *   13:42:15      ; Normal form
 *   13:42         ; Hour and minutes only. second 0 implied
 *   13            ; Hour only. minute 0 and second 0 implied
 *   13:42:15.5201 ; Fractional seconds
 *   13:42.123     ; Fractional minutes
 *   13.13389      ; Fractional hours
 * </pre>
 * <p>
 * &lt;zone&gt; (optional) has the following form (colons are optional):
 * <pre>
 *                 ; Absent. Time is in "local" time
 *   Z             ; UTC ("Zulu" time)
 *   +hh:mm        ; Number of hours:munutes ahead (+) or behind (-) UTC
 *   +hh           ; Number of hours ahead (+) or behind (-) UTC
 * </pre>
 * If time zone is not specified, "local" time is assumed. If "local" is to be
 * interpreted as "here", we could use the current time zone of the running JVM,
 * but this will give different results if the parser is executed in a different
 * time zone. It is better to interpret "local" as "don't know" and associate it
 * with UTC (being as good as anything) and at least get consistent results
 * wherever the class is used.
 * <p>
 * <b>NOTE:</b> The parser is <em>lenient</em> meaning that it may accept
 * sensible input that not necessarily follow the ISO 8601 strictly.
 * The class can therefore not be used to <em>validate</em> ISO 8601.
 *
 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
 */
public final class ISO8601DateParser
{
  /**
   * Private constructor to prevent client instantiation.
   */
  private ISO8601DateParser()
  {
    assert false : "This constructor should never be called";
  }

  /**
   * Parse the specified date string and update the given calendar instance
   * accordingly.
   *
   * @param dateString  Date string to parse. Non-null.
   * @param calendar    Calendar to populate. Non-null.
   * @throws ParseException  If the dateString cannot be parsed according to ISO 8601.
   */
  private static void parseDate(String dateString, Calendar calendar)
    throws ParseException
  {
    assert dateString != null : "dateString cannot be null";
    assert calendar != null : "calendar cannot be null";

    // Make into basic form
    String basicDateString = dateString.replaceAll("-", "");

    int length = basicDateString.length();
    boolean isWeekDate = basicDateString.indexOf('W') != -1;
    boolean isOrdinalDate = length == 7;

    //
    // Case 1: yyyyWww[d]
    //
    if (isWeekDate) {
      int year = Integer.parseInt(basicDateString.substring(0, 4));
      int weekOfYear = Integer.parseInt(basicDateString.substring(5, 7));
      int dayOfWeek = length == 7 ? Calendar.MONDAY : Integer.parseInt(basicDateString.substring(7));
      calendar.set(Calendar.YEAR, year);
      calendar.set(Calendar.WEEK_OF_YEAR, weekOfYear);
      calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);
    }

    //
    // Case 2: yyyyddd (ordinal date)
    //
    else if (isOrdinalDate) {
      int year = Integer.parseInt(basicDateString.substring(0, 4));
      int dayOfYear = Integer.parseInt(basicDateString.substring(4));
      calendar.set(Calendar.YEAR, year);
      calendar.set(Calendar.DAY_OF_YEAR, dayOfYear);
    }

    //
    // Case 3: yy (century only, January 1, year 0 assumed)
    //
    else if (length == 2) {
      int century = Integer.parseInt(basicDateString);
      calendar.set(century * 100, 0, 1);
    }

    //
    // Case 4: yyyy (year only, January 1 assumed)
    //
    else if (length == 4) {
      int year = Integer.parseInt(basicDateString);
      calendar.set(year, 0, 1);
    }

    //
    // Date case 5: yyyymm (year month only, First day in month assumed)
    //
    else if (length == 6) {
      int year = Integer.parseInt(basicDateString.substring(0, 4));
      int month = Integer.parseInt(basicDateString.substring(4, 6)) - 1;
      calendar.set(year, month, 1);
    }

    //
    // Date case 6: yyyymmdd (standard case)
    //
    else if (length == 8) {
      int year = Integer.parseInt(basicDateString.substring(0, 4));
      int month = Integer.parseInt(basicDateString.substring(4, 6)) - 1;
      int day = Integer.parseInt(basicDateString.substring(6));
      calendar.set(year, month, day);
    }

    //
    // Date case 7: Unparsable
    //
    else {
      throw new ParseException("Invalid date format " + dateString, 0);
    }
  }

  /**
   * Parse the specified time string and update the given calendar instance
   * accordingly.
   *
   * @param timeString  Time string to parse. Non-null.
   * @param calendar    Calendar to populate. Non-null.
   * @throws ParseException  If the timeString cannot be parsed according to ISO 8601.
   */
  private static void parseTime(String timeString, Calendar calendar)
    throws ParseException
  {
    assert timeString != null : "timeString cannot be null";
    assert calendar != null : "calendar cannot be null";

    String basicTimeString = timeString.replace(":", "").replace(",", ".");

    // Divide into whole and fractional part
    int pos = basicTimeString.indexOf('.');
    String wholePart = pos != -1 ? basicTimeString.substring(0, pos) : basicTimeString;
    String fractionPart = pos != -1 ? basicTimeString.substring(pos + 1) : "0";

    double fraction = Double.parseDouble("0." + fractionPart);

    int length = wholePart.length();

    //
    // hh
    //
    if (length >= 2) {
      int hour = Integer.parseInt(wholePart.substring(0, 2));
      calendar.set(Calendar.HOUR_OF_DAY, hour);
    }

    //
    // ..mm
    //
    if (length > 2) {
      int minute = Integer.parseInt(wholePart.substring(2, 4));
      calendar.set(Calendar.MINUTE, minute);
    }
    else {
      fraction *= 60;
    }

    //
    // ....ss
    //
    if (length > 4) {
      int second = Integer.parseInt(wholePart.substring(4, 6));
      calendar.set(Calendar.SECOND, second);
    }
    else {
      fraction *= 60;
    }

    int millisecond = (int) Math.round(fraction * 1000);
    calendar.set(Calendar.MILLISECOND, millisecond);
  }

  /**
   * Parse the specified time zone string and update the given calendar instance
   * accordingly.
   *
   * @param zoneString  Zone string to parse. Non-null.
   * @param calendar    Calendar to populate. Non-null.
   */
  private static void parseZone(String zoneString, Calendar calendar)
  {
    assert zoneString != null : "<oneString cannot be null";
    assert calendar != null : "calendar cannot be null";

    // Without a time zone indicator, "local" time is assumed. If "local" is to be
    // interpreted as "here", we could use TimeZone.getDefault() (the time zone of
    // the current JVM), but this will give different results if executing the method
    // in a different time zone. It is better to interpret "local" as "don't know"
    // and associate them with UTC (being as good as anything) and at least get consistent
    // results wherever the method is executed.
    if (zoneString.isEmpty())
      calendar.setTimeZone(TimeZone.getTimeZone("UTC")); // TimeZone.getDefault());

    else if (zoneString.startsWith("Z"))
      calendar.setTimeZone(TimeZone.getTimeZone("UTC"));

    else {
      // We have removed the optional colon from the zone string, but the
      // TimeZone class needs this, so we reintroduce it here.
      // The getTimeZone() method doesn't throw exception on illegal syntax
      // so anything other than +/-hh[:mm] will end up as GMT.
      if (zoneString.length() == 5)
        zoneString = zoneString.substring(0, 3) + ':' + zoneString.substring(3, 5);
      calendar.setTimeZone(TimeZone.getTimeZone("GMT" + zoneString));
    }
  }

  /**
   * Parse the given string in ISO 8601 date/time format and return it as
   * a Date object.
   *
   * @param  text  Text string to parse. Non-null.
   * @return Corresponding date instance. Never null.
   * @throws IllegalArgumentException  If text is null.
   * @throws ParseException   If text is not a valid date time according to ISO 8601
   */
  public static Date parse(String text)
    throws ParseException
  {
    if (text == null)
      throw new IllegalArgumentException("text cannot be null");

    text = text.toUpperCase();

    // Initiate a calendar instance we can populate
    Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
    calendar.setMinimalDaysInFirstWeek(4);
    calendar.setFirstDayOfWeek(Calendar.MONDAY);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    calendar.setLenient(true); // Allow wrap-overs

    //
    // Split string into dateText, timeText and zoneText component
    //
    int p = text.indexOf('T');
    if (p == -1)
      p = text.indexOf(' '); // Not strictly ISO 8601, but common so we allow it

    String dateText = p != -1 ? text.substring(0, p) : text;
    String timeAndZoneText = p != -1 ? text.substring(p + 1) : "";

    p = timeAndZoneText.indexOf('Z');
    if (p == -1)
      p = timeAndZoneText.indexOf('+');
    if (p == -1)
      p = timeAndZoneText.indexOf('-');

    String timeText = p != -1 ? timeAndZoneText.substring(0, p) : timeAndZoneText;
    String zoneText = p != -1 ? timeAndZoneText.substring(p) : "";

    //
    // Parse the individual parts and update calendar accordingly
    //
    parseDate(dateText, calendar);
    parseTime(timeText, calendar);
    parseZone(zoneText, calendar);

    return calendar.getTime();
  }

  /**
   * Generate a ISO 8601 string representation of the specified date.
   *
   * @param date The date to create string representation of. UTC assumed. Non-null.
   * @return     String representing the date/time in the ISO 8601 format.
   *             Never null.
   * @throws IllegalArgumentException  If date is null.
   */
  public static String toString(Date date)
  {
    if (date == null)
      throw new IllegalArgumentException("date cannot be null");

    return DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC).format(Instant.ofEpochMilli(date.getTime()));
  }
}
