package no.geosoft.jwlf;

import java.util.ArrayList;
import java.util.Date;

/**
 * Class for holding a list of values of a specific type.
 * <p>
 * The class is non-generic for memory and performance reasons: A List&lt;Object&gt;
 * will contain overhead for every object, while the present implementation
 * degenerates to a primitive array.
 * <p>
 * In theory it should be possible to make a <em>generic</em> version of this class,
 * but this proves impossible to work with on the client side as the generic type is
 * not known, other than through the valueType variable.
 *
 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
 */
public final class DataArray
{
  /** Data array in case type of array is double. Null if not. */
  private final DoubleList doubleValues_;

  /** Data array in case type of array is float. Null if not. */
  private final FloatList floatValues_;

  /** Data array in case type of array is integer. Null if not. */
  private final IntList intValues_;

  /** Data array in case type of array is long. Null if not. */
  private final ArrayList<Long> longValues_;

  /** Data array in case type of array is boolean. Null if not. */
  private final ArrayList<Boolean> boolValues_;

  /** Data array in case type of array is string. Null if not. */
  private final ArrayList<String> stringValues_;

  /** Data array in case type of array is Date. Null if not. */
  private final ArrayList<Date> timeValues_;

  /** Data array in case type of array is Short. Null if not. */
  private final ArrayList<Short> shortValues_;

  /** Data array in case type of array is (signed) Byte. Null if not. */
  private final ArrayList<Byte> byteValues_;

  /**
   * Data array in case type is none of the above.
   * Never null, but not used unless all of the above is null.
   */
  private final ArrayList<Object> objectValues_;

  /**
   * Create a data array of the specific type.
   *
   * @param type  Type of the elements of the data array. Non-null.
   * @throws IllegalArgumentException  If type is null.
   */
  public DataArray(Class<?> type)
  {
    if (type == null)
      throw new IllegalArgumentException("yype cannot be null");

    //
    // Create the correct back-end list
    //
    floatValues_ = type == Float.class ? new FloatList() : null;
    intValues_ = type == Integer.class ? new IntList() : null;
    longValues_ = type == Long.class ? new ArrayList<Long>() : null;
    doubleValues_ = type == Double.class ? new DoubleList() : null;
    timeValues_ = type == Date.class ? new ArrayList<Date>() : null;
    shortValues_ = type == Short.class ? new ArrayList<Short>() : null;
    byteValues_ = type == Byte.class ? new ArrayList<Byte>() : null;
    stringValues_ = type == String.class ? new ArrayList<String>() : null;
    boolValues_ = type == Boolean.class ? new ArrayList<Boolean>() : null;
    objectValues_ = new ArrayList<Object>();
  }

  /**
   * Add an element to this data array.
   *
   * @param value  Value to add. It is assumed to contain the correct type (or null).
   *               No type checking is done for performance reasons.
   */
  public void add(Object value)
  {
    if (floatValues_ != null)
      floatValues_.add((Float) value);
    else if (intValues_ != null)
      intValues_.add((Integer) value);
    else if (longValues_ != null)
      longValues_.add((Long) value);
    else if (doubleValues_ != null)
      doubleValues_.add((Double) value);
    else if (timeValues_ != null)
      timeValues_.add((Date) value);
    else if (shortValues_ != null)
      shortValues_.add((Short) value);
    else if (byteValues_ != null)
      byteValues_.add((Byte) value);
    else if (stringValues_ != null)
      stringValues_.add((String) value);
    else if (boolValues_ != null)
      boolValues_.add((Boolean) value);
    else
      objectValues_.add(value);
  }

  /**
   * Set an element of this data array.
   *
   * @param index  Index to set value at. [0,n&gt;.
   *               No bounds checking for performance reasons.
   * @param value  Value to set. It is assumed to contain the correct type (or null).
   *               No type checking is done for performance reasons.
   */
  public void set(int index, Object value)
  {
    if (floatValues_ != null)
      floatValues_.set(index, (Float) value);
    else if (intValues_ != null)
      intValues_.set(index, (Integer) value);
    else if (longValues_ != null)
      longValues_.set(index, (Long) value);
    else if (doubleValues_ != null)
      doubleValues_.set(index, (Double) value);
    else if (timeValues_ != null)
      timeValues_.set(index, (Date) value);
    else if (shortValues_ != null)
      shortValues_.set(index, (Short) value);
    else if (byteValues_ != null)
      byteValues_.set(index, (Byte) value);
    else if (stringValues_ != null)
      stringValues_.set(index, (String) value);
    else if (boolValues_ != null)
      boolValues_.set(index, (Boolean) value);
    else
      objectValues_.set(index, value);
  }

  /**
   * Return value at the specified index.
   *
   * @param index  Index to get value at. [0,n&gt;.
   *               No bounds checking for performance reasons.
   * @return  The requested value. Null if no-value.
   */
  public Object get(int index)
  {
    if (floatValues_ != null)
      return floatValues_.get(index);
    else if (intValues_ != null)
      return intValues_.get(index);
    else if (longValues_ != null)
      return longValues_.get(index);
    else if (doubleValues_ != null)
      return doubleValues_.get(index);
    else if (timeValues_ != null)
      return timeValues_.get(index);
    else if (shortValues_ != null)
      return shortValues_.get(index);
    else if (byteValues_ != null)
      return byteValues_.get(index);
    else if (stringValues_ != null)
      return stringValues_.get(index);
    else if (boolValues_ != null)
      return boolValues_.get(index);
    else
      return objectValues_.get(index);
  }

  /**
   * Return number of elements in this array.
   *
   * @return  Number of elements in this array. [0,&gt;.
   */
  public int size()
  {
    if (floatValues_ != null)
      return floatValues_.size();
    else if (intValues_ != null)
      return intValues_.size();
    else if (longValues_ != null)
      return longValues_.size();
    else if (doubleValues_ != null)
      return doubleValues_.size();
    else if (timeValues_ != null)
      return timeValues_.size();
    else if (shortValues_ != null)
      return shortValues_.size();
    else if (byteValues_ != null)
      return byteValues_.size();
    else if (stringValues_ != null)
      return stringValues_.size();
    else if (boolValues_ != null)
      return boolValues_.size();
    else
      return objectValues_.size();
  }

  /**
   * Clear content of this data array.
   */
  public void clear()
  {
    if (floatValues_ != null)
      floatValues_.clear();
    else if (intValues_ != null)
      intValues_.clear();
    else if (longValues_ != null)
      longValues_.clear();
    else if (doubleValues_ != null)
      doubleValues_.clear();
    else if (timeValues_ != null)
      timeValues_.clear();
    else if (shortValues_ != null)
      shortValues_.clear();
    else if (byteValues_ != null)
      byteValues_.clear();
    else if (stringValues_ != null)
      stringValues_.clear();
    else if (boolValues_ != null)
      boolValues_.clear();
    else
      objectValues_.clear();
  }

  /**
   * Set capacity of the back-end list to its actual size.
   * Typically done to save memory after the array is
   * completely populated.
   */
  public void trim()
  {
    if (floatValues_ != null)
      floatValues_.trimToSize();
    else if (intValues_ != null)
      intValues_.trimToSize();
    else if (longValues_ != null)
      longValues_.trimToSize();
    else if (doubleValues_ != null)
      doubleValues_.trimToSize();
    else if (timeValues_ != null)
      timeValues_.trimToSize();
    else if (shortValues_ != null)
      shortValues_.trimToSize();
    else if (byteValues_ != null)
      byteValues_.trimToSize();
    else if (stringValues_ != null)
      stringValues_.trimToSize();
    else if (boolValues_ != null)
      boolValues_.trimToSize();
    else
      objectValues_.trimToSize();
  }
}
