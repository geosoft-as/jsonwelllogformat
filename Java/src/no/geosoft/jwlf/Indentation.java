package no.geosoft.jwlf;

/**
 * Class for holding a space indentation commonly used at beginning
 * of pretty printed lines.
 *
 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
 */
public final class Indentation
{
  /** Current indentation level. [0,&gt;. */
  private final int level_;

  /** Indentation unit. Number of characters for the indentation. [0,&gt;. */
  private final int unit_;

  /** The actual indentation unit string. Cached for speed. */
  private final String indent_;

  /**
   * Create an indentation instance of the specified unit,
   * and initial indentation.
   *
   * @param unit   Number of characters per indentation. [0,&gt;.
   * @param level  Current indentation level. [0,&gt;.
   * @throws IllegalArgumentException  If unit or level is invalid.
   */
  public Indentation(int unit, int level)
  {
    if (unit < 0)
      throw new IllegalArgumentException("Invalid unit: " + unit);

    if (level < 0)
      throw new IllegalArgumentException("Invalid level: " + level);

    unit_ = unit;
    level_ = level;
    indent_ = Util.getSpaces(level_ * unit_);
  }

  /**
   * Create an indentation instance of the specified unit,
   *
   * @param unit  Number of characters per indentation. [0,&gt;.
   */
  public Indentation(int unit)
  {
    this(unit, 0);
  }

  /**
   * Create a new indentation instance indented one level to the right.
   *
   * @return  The requested indentation instance. Never null.
   */
  public Indentation push()
  {
    return new Indentation(unit_, level_ + 1);
  }

  /**
   * Create a new indentation instance indented one level to the left.
   *
   * @return  The requested indentation instance. Never null.
   * @throws IllegalStateException  If already at lowest level.
   */
  public Indentation pop()
  {
    if (level_ == 0)
      throw new IllegalStateException("Already at lowest level");

    return new Indentation(unit_, level_ - 1);
  }

  /** {@inheritDoc} */
  @Override
  public String toString()
  {
    return indent_;
  }
}
