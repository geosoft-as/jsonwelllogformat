package no.geosoft.jwlf;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * A class capable of formatting (i.e write as text) numbers so that
 * they get uniform appearance and can be presented together, typically
 * in a column with decimal symbol aligned etc.
 *
 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
 */
public final class Formatter
{
  /** Default number of significant digits. */
  private static final int N_SIGNIFICANT_DEFAULT = 7;

  /** Default number of decimals. */
  private static final int N_DECIMALS_DEFAULT = 2;

  /** The smallest value before switching to scientific notation. */
  private static final double MIN_NON_SCIENTIFIC = 0.0001;

  /** The largest value before switching to scientific notation. */
  private static final double MAX_NON_SCIENTIFIC = 9999999.0;

  /** The format to use. Non-null. */
  private final DecimalFormat format_;

  /**
   * Return the number of digits in the specified number.
   *
   * <b>NOTE:</b> This method is a copy from cc.util.NumberUtil and
   * should be kept in sync with that one.
   *
   * @param v  Number to count digits in.
   * @return   Number of digits in v. [1,&gt;.
   */
  public static int countDigits(long v)
  {
    // Twice as fast as (Math.abs(v) + "").length()
    return v == 0 ? 1 : (int) Math.log10(Math.abs(v)) + 1;
  }

  /**
   * Return number of <em>significant decimals</em> in the specified
   * floating point value.
   * <p>
   * <b>NOTE:</b> This method is a copy from cc.util.NumberUtil and
   * should be kept in sync with that one.
   *
   * <p>
   * Example:
   * <pre>
   *   0.991 = 3
   *   0.9901 = 4
   *   0.99001 = 5
   *   0.990001 = 6
   *   0.9900001 = 2
   * </pre>
   * However, if the whole part is large this will influence the result
   * as there is a maximum total number of significant digits:
   * <pre>
   *   12345678.991 = 3
   *   12345678.9901 = 4
   *   12345678.99001 = 4   // ideally 2 but we don't capture this
   *   12345678.990001 = 4  // ideally 2 but we don't capture this
   * </pre>
   *
   * @param d  Number to check.
   * @return   Number of significant decimals. [0,&gt;.
   */
  private static int countDecimals(double d)
  {
    if (!Double.isFinite(d))
      return 0;

    // We strip away sign and the whole part as we care about
    // the decimals only. Left is something like 0.12....
    d = Math.abs(d);
    long wholePart = Math.round(d);
    int nSignificant = countDigits(wholePart);
    double fractionPart = Math.abs(d - wholePart);

    // We start with the fraction, say 0.12345678 and loop
    // over it 10x at the time, so for this example we will get:
    //   0.12345678
    //   1.2345678
    //   12.345678
    //   123.45678
    //   :
    int nDecimals = 0;
    int order = 1;
    while (true) {

      // This is the full floating point number and the integer part
      // (rounded) i.e. 12.789 and 13 etc.
      double floating = fractionPart * order;
      long whole = Math.round(floating);

      // We find the difference of the two, like 0.211 and find
      // what is the fraction of this with the whole.
      double difference = Math.abs(whole - floating);
      double fraction = whole != 0.0 ? difference / whole : difference;

      // If this fraction is very small then the fractional part of the
      // number no longer contributes significantly to the whole and we
      // conclude that the rest is not significant decimals of this number.
      if (fraction < 0.0001)
        break;

      // If we reach maximum number of signigicant digit the computer
      // can adequately represent we stop. We use 12 as a conservative limit.
      // 1234567890.12345678 is 1234567890.12 and nDecimals are 2.
      if (nSignificant >= 12)
        break;

      order *= 10;
      nDecimals++;
      nSignificant++;
    }

    return nDecimals;
  }

  /**
   * Create a common formatter for the specified set of numbers.
   *
   * @param values              Representative values to use for creating the formatter.
   *                            Null to create a generic formatter independent of any
   *                            actual values.
   * @param nSignificantDigits  Number of significant digits. Defaults to 7 if null.
   *                            Ignored if nDecimals is specified.
   * @param nDecimals           Number of decimals. If null, decide by significan digits.
   * @param locale              Locale to present numbers in. Null for default.
   */
  public Formatter(double[] values, Integer nSignificantDigits, Integer nDecimals, Locale locale)
  {
    int nActualSignificantDigits = nSignificantDigits != null ? nSignificantDigits : N_SIGNIFICANT_DEFAULT;
    int nActualDecimals = N_DECIMALS_DEFAULT;
    Locale actualLocale = locale != null ? locale : Locale.ROOT;

    double maxValue = 0.0;

    boolean isScientific = false;

    // Maximum number of decimals needed to represent the values provided
    int nMaxDecimalsNeeded = 0;

    //
    // Loop over all the representative values to find the maximum
    // and to check if we should use scientific notation.
    //
    if (values != null) {
      for (int i = 0; i < values.length; i++) {

        double value = values[i];

        // Leave non-printable characters
        if (Double.isNaN(value) || Double.isInfinite(value))
          continue;

        // Work with the absolute value only
        value = Math.abs(value);

        //
        // Check if we should go scientific
        //
        if (value > MAX_NON_SCIENTIFIC || value != 0.0 && value < MIN_NON_SCIENTIFIC) {
          isScientific = true;
          nActualDecimals = nActualSignificantDigits - 1;
          break;
        }

        // Keep track of maximum numeric value of the lot
        if (value > maxValue)
          maxValue = value;

        // Find how many decimals is needed to represent this value correctly
        int nDecimalsNeeded = countDecimals(value);
        if (nDecimalsNeeded > nMaxDecimalsNeeded)
          nMaxDecimalsNeeded = nDecimalsNeeded;
      }
    }

    //
    // Determine n decimals for the non-scietific case
    //
    if (!isScientific) {
      long wholePart = Math.round(maxValue);
      int length = ("" + wholePart).length();
      nActualDecimals = Math.max(nActualSignificantDigits - length, 0);

      // If there are values provided, and they need fewer decimals
      // than computed, we reduce this
      if (values != null && values.length > 0 && nMaxDecimalsNeeded < nActualDecimals)
        nActualDecimals = nMaxDecimalsNeeded;
    }

    // Override n decimals on users request
    if (nDecimals != null)
      nActualDecimals = nDecimals;

    //
    // Create the format string
    //
    StringBuilder formatString = new StringBuilder();
    if (isScientific) {
      formatString.append("0.E0");
      for (int i = 0; i < nActualDecimals; i++)
        formatString.insert(2, '0');
    }
    else {
      formatString.append(nActualDecimals > 0 ? "0." : "0");
      for (int i = 0; i < nActualDecimals; i++)
        formatString.append('0');
    }

    //
    // Create the actual decimal format
    //
    DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(actualLocale);
    format_ = new DecimalFormat(formatString.toString(), formatSymbols);
  }

  /**
   * Create a common formatter for the specified set of numbers.
   *
   * @param values              Representative values to use for creating the formatter.
   *                            Null to create a generic formatter independent of any
   *                            actual values.
   */
  public Formatter(double[] values)
  {
    this(values, null, null, null);
  }

  /**
   * Create a default number formatter,
   */
  public Formatter()
  {
    this(null, null, null, null);
  }

  /**
   * Format the specified value according to the formatting defined
   * by this formatter.
   *
   * @param value  Value to format,
   * @return       Text representation of the value according to the format.
   */
  public String format(double value)
  {
    // Handle the non-printable characters
    if (Double.isNaN(value) || Double.isInfinite(value))
      return "";

    // 0.0 easily gets lost if written with many decimals like 0.00000.
    // Consequently we write this as either 0.0 or 0
    if (value == 0.0)
      return format_.getMaximumFractionDigits() == 0 ? "0" : "0.0";

    return format_.format(value);
  }

  /**
   * Return the back-end decimal format of this formatter.
   *
   * @return  The back-end decimal format of this formatter. Never null.
   */
  public DecimalFormat getFormat()
  {
    return format_;
  }


  /** {@inheritDoc} */
  @Override
  public String toString()
  {
    StringBuilder s = new StringBuilder();
    s.append("99");
    s.append(format_.getDecimalFormatSymbols().getDecimalSeparator());
    for (int i = 0; i < format_.getMinimumFractionDigits(); i++)
      s.append('9');
    return s.toString();
  }
}
