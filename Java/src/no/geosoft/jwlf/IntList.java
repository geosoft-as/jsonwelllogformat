package no.geosoft.jwlf;

import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * A List implementation wrapping a native int array.
 * <p>
 * Useful if the array becomes <em>very</em> large as this is both
 * a lot faster and requires less storage than List&lt;Integer&gt;.
 * <p>
 * <b>Speed:</b> IntList is about 5x faster than List&lt;Integer&gt;<br>
 * <b>Memory:</b> int is 4 bytes while Integer is 16 bytes: 4x.
 *
 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
 */
public final class IntList implements List<Integer>
{
  /** Use this to represent Integer null. */
  private static final int NO_VALUE = Integer.MIN_VALUE;

  /** Back-end array. */
  private int[] array_;

  /** Current size. [0,&gt;. */
  private int size_ = 0;

  /**
   * An ID defining the <em>state</em> of the list. Changed whenever the
   * <em>structure</em> of the list changes. Used with iterators derrived
   * from the class that requires a stable state during their operation.
   */
  private transient int stateId_;

  /**
   * Create a new Integer list with the specified capacity.
   *
   * @param capacity  Initial capacity. [1,&gt;.
   * @throws IllegalArgumentException  If capacity is &lt;1.
   */
  public IntList(int capacity)
  {
    if (capacity < 1)
      throw new IllegalArgumentException("Invalid capacity: " + capacity);

    array_ = new int[capacity];
  }

  /**
   * Create a new Double list with default capacity.
   */
  public IntList()
  {
    // A large initial capacity to indicate that this class
    // is mainly used with large collections.
    this(1000);
  }

  /**
   * Ensure that the backing listh as enough capacity for the
   * specified number of elements.
   *
   * @param size  Number of elements. [0,&gt;.
   */
  private void ensureCapacity(int size)
  {
    assert size >= 0 : "Invalid size: " + size;

    int oldCapacity = array_.length;

    // If the backing array is to small, we increase it by 50%
    if (size > oldCapacity) {
      int newCapacity = (oldCapacity * 3) / 2 + 1;
      array_ = Arrays.copyOf(array_, newCapacity);
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean add(Integer value)
  {
    stateId_++;
    ensureCapacity(size_ + 1);
    array_[size_++] = value != null ? value : NO_VALUE;
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public void add(int index, Integer value)
  {
    if (index < 0 || index > size_)
      throw new IndexOutOfBoundsException("" + index);

    stateId_++;
    ensureCapacity(size_ + 1);
    System.arraycopy(array_, index, array_, index + 1, size_ - index);

    array_[index] = value != null ? value : NO_VALUE;

    size_++;
  }

  /** {@inheritDoc} */
  @Override
  public boolean addAll(Collection<? extends Integer> values)
  {
    if (values == null)
      throw new IllegalArgumentException("values cannot be null");

    if (values.isEmpty())
      return false;

    stateId_++;
    ensureCapacity(size_ + values.size());
    for (Integer value : values)
      array_[size_++] = value != null ? value : NO_VALUE;

    return true;
  }

  /** {@inheritDoc} */
  @Override
  public boolean addAll(int index, Collection<? extends Integer> values)
  {
    if (index < 0 || index > size_)
      throw new IndexOutOfBoundsException("" + index);

    if (values == null)
      throw new IllegalArgumentException("values cannot be null");

    if (values.isEmpty())
      return false;

    stateId_++;

    int size = values.size();

    ensureCapacity(size_ + size);

    System.arraycopy(array_, index, array_, index + size, size_ - index);

    int i = index;
    for (Integer value : values)
      array_[i++] = value != null ? value : NO_VALUE;

    size_ += size;

    return true;
  }

  /** {@inheritDoc} */
  @Override
  public void clear()
  {
    stateId_++;
    size_ = 0;
  }

  /** {@inheritDoc} */
  @Override
  public boolean contains(Object value)
  {
    return indexOf(value) != -1;
  }

  /** {@inheritDoc} */
  @Override
  public boolean containsAll(Collection<?> collection)
  {
    if (collection == null)
      throw new IllegalArgumentException("collection cannot be null");

    for (Object value : collection)
      if (!contains(value))
        return false;

    return true;
  }

  /** {@inheritDoc} */
  @Override
  public Integer get(int index)
  {
    if (index < 0 || index >= size_)
      throw new IndexOutOfBoundsException("" + index);

    int v = array_[index];
    return v == NO_VALUE ? null : v;
  }






  /** {@inheritDoc} */
  @Override
  public int indexOf(Object value)
  {
    if (value != null && !(value instanceof Integer))
      return -1;

    if (value == null) {
      for (int index = 0; index < size_; index++)
        if (array_[index] == NO_VALUE)
          return index;
    }

    else {
      int v = (Integer) value;

      for (int index = 0; index < size_; index++)
        if (v == array_[index])
          return index;
    }

    // Not found
    return -1;
  }

  /** {@inheritDoc} */
  @Override
  public int lastIndexOf(Object value)
  {
    if (value != null && !(value instanceof Integer))
      return -1;

    if (value == null) {
      for (int index = size_ - 1; index >= 0; index--)
        if (array_[index] == NO_VALUE)
          return index;
    }

    else {
      int v = (Integer) value;
      for (int index = size_ - 1; index >= 0; index--)
        if (v == array_[index])
          return index;
    }

    // Not found
    return -1;
  }

  /** {@inheritDoc} */
  @Override
  public int size()
  {
    return size_;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isEmpty()
  {
    return size_ == 0;
  }

  /** {@inheritDoc} */
  @Override
  public ListIterator<Integer> listIterator()
  {
    return new IntegerListIterator(0);
  }

  /** {@inheritDoc} */
  @Override
  public ListIterator<Integer> listIterator(int index)
  {
    if (index < 0 || index >= size_)
      throw new IndexOutOfBoundsException("" + index);

    return new IntegerListIterator(index);
  }

  /** {@inheritDoc} */
  @Override
  public Iterator<Integer> iterator()
  {
    return listIterator();
  }

  /** {@inheritDoc} */
  @Override
  public Integer remove(int index)
  {
    if (index < 0 || index >= size_)
      throw new IndexOutOfBoundsException("" + index);

    stateId_++;

    int value = array_[index];

    int nMoved = size_ - index - 1;
    if (nMoved > 0)
      System.arraycopy(array_, index + 1, array_, index, nMoved);

    size_--;

    return value != NO_VALUE ? value : null;
  }

  /** {@inheritDoc} */
  @Override
  public boolean remove(Object value)
  {
    int index = indexOf(value);

    if (index == -1)
      return false;

    remove(index);
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public boolean removeAll(Collection<?> collection)
  {
    throw new UnsupportedOperationException("Not supported. Use java.util.ArrayList instead");
  }

  /** {@inheritDoc} */
  @Override
  public boolean retainAll(Collection<?> collection)
  {
    throw new UnsupportedOperationException("Not supported. Use java.util.ArrayList instead");
  }

  /** {@inheritDoc} */
  @Override
  public Integer set(int index, Integer value)
  {
    if (index < 0 || index >= size_)
      throw new IndexOutOfBoundsException("" + index);

    int oldValue = array_[index];
    int newValue = value != null ? value : NO_VALUE;

    array_[index] = newValue;

    return oldValue != NO_VALUE ? oldValue : null;
  }

  /** {@inheritDoc} */
  @Override
  public List<Integer> subList(int fromIndex, int toINdex)
  {
    throw new UnsupportedOperationException("Not supported. Use java.util.ArrayList instead");
  }

  /** {@inheritDoc} */
  @Override
  public Object[] toArray()
  {
    Object[] array = new Object[size_];
    for (int i = 0; i < size_; i++) {
      int value = array_[i];
      array[i] = value != NO_VALUE ? value : null;
    }

    return array;
  }

  /** {@inheritDoc} */
  @Override
  public <Integer> Integer[] toArray(Integer[] array)
  {
    throw new UnsupportedOperationException("Use toArray() instead");
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode()
  {
    return Arrays.hashCode(array_);
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(Object object)
  {
    if (object == this)
      return true;

    if (!(object instanceof List))
      return false;

    ListIterator<Integer> e1 = listIterator();
    ListIterator<?> e2 = ((List) object).listIterator();

    while (e1.hasNext() && e2.hasNext()) {
      Integer o1 = e1.next();
      Object o2 = e2.next();

      if (!(o1 == null ? o2 == null : o1.equals(o2)))
        return false;
    }

    return !(e1.hasNext() || e2.hasNext());
  }

  /**
   * Trim the back-end array to the actual size of the list.
   * Typically done to save space when the list will grow no longer.
   */
  public void trimToSize()
  {
    if (size_ < array_.length)
      array_ = Arrays.copyOf(array_, size_);
  }

  /** {@inheritDoc} */
  @Override
  public String toString()
  {
    StringBuilder s = new StringBuilder();
    s.append("IntList(" + size_ + "/" + array_.length + ") = [");
    for (int i = 0; i < 5; i++) {
      if (i == size_)
        break;
      if (i > 0)
        s.append(", ");
      s.append(array_[i]);
    }

    if (size_ > 5) {
      s.append(", ...");
      s.append(" (" + (size_ - 5) + " more)");
    }

    s.append("]");

    return s.toString();
  }

  /**
   * An Integer list iterator.
   *
   * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
   */
  private class IntegerListIterator implements ListIterator<Integer>
  {
    /** Index of next element to return. */
    private int cursor_;

    /** Index of last element returned. -1 if no such element. */
    private int lastIndex_ = -1;

    /**
     * The initial state of the list. Used to detect changes of state
     * during the lifetime of the iterator. Such changes are not allowed
     * (as the iterator will get confused) and therefore causes a
     * ConcurrentModificationException.
     */
    private int initialStateId_ = stateId_;

    /**
     * Create an iterator starting at the given index.
     *
     * @param index  Start index. [0,&gt;.
     */
    private IntegerListIterator(int index)
    {
      assert index >= 0 : "Invalid index: " + index;
      cursor_ = index;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasNext()
    {
      return cursor_ != size_;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasPrevious()
    {
      return cursor_ != 0;
    }

    /** {@inheritDoc} */
    @Override
    public int nextIndex()
    {
      return cursor_;
    }

    /** {@inheritDoc} */
    @Override
    public int previousIndex()
    {
      return cursor_ - 1;
    }

    /** {@inheritDoc} */
    @Override
    public Integer next()
    {
      // Check that the state (i.e. structure) of the list has
      // not been changed during the operation of the iterator
      if (stateId_ != initialStateId_)
        throw new ConcurrentModificationException();

      int index = cursor_;

      if (index >= size_)
        throw new NoSuchElementException();

      if (index >= array_.length)
        throw new ConcurrentModificationException();

      cursor_ = index + 1;
      lastIndex_ = index;

      int value = array_[index];
      return value != NO_VALUE ? value : null;
    }

    /** {@inheritDoc} */
    @Override
    public Integer previous()
    {
      // Check that the state (i.e. structure) of the list has
      // not been changed during the operation of the iterator
      if (stateId_ != initialStateId_)
        throw new ConcurrentModificationException();

      int index = cursor_ - 1;
      if (index < 0)
        throw new NoSuchElementException();

      if (index >= array_.length)
        throw new ConcurrentModificationException();

      cursor_ = index;
      lastIndex_ = index;

      int value = array_[index];
      return value != NO_VALUE ? value : null;
    }

    /** {@inheritDoc} */
    @Override
    public void set(Integer value)
    {
      if (lastIndex_ == -1)
        throw new IllegalStateException();

      // Check that the state (i.e. structure) of the list has
      // not been changed during the operation of the iterator
      if (stateId_ != initialStateId_)
        throw new ConcurrentModificationException();

      try {
        IntList.this.set(lastIndex_, value);
      }
      catch (IndexOutOfBoundsException exception) {
        throw new ConcurrentModificationException();
      }
    }

    /** {@inheritDoc} */
    @Override
    public void add(Integer value)
    {
      // Check that the state (i.e. structure) of the list has
      // not been changed during the operation of the iterator
      if (stateId_ != initialStateId_)
        throw new ConcurrentModificationException();

      try {
        int index = cursor_;
        IntList.this.add(index, value);
        cursor_ = index + 1;
        lastIndex_ = -1;

        // The iterator is now in sync with the state so we update the ID
        initialStateId_ = stateId_;
      }
      catch (IndexOutOfBoundsException exception) {
        throw new ConcurrentModificationException();
      }
    }

    /** {@inheritDoc} */
    @Override
    public void remove()
    {
      if (lastIndex_ == -1)
        throw new IllegalStateException();

      // Check that the state (i.e. structure) of the list has
      // not been changed during the operation of the iterator
      if (stateId_ != initialStateId_)
        throw new ConcurrentModificationException();

      try {
        IntList.this.remove(lastIndex_);
        cursor_ = lastIndex_;
        lastIndex_ = -1;

        // The iterator is now in sync with the state so we update the ID
        initialStateId_ = stateId_;
      }
      catch (IndexOutOfBoundsException exception) {
        throw new ConcurrentModificationException();
      }
    }
  }
}
