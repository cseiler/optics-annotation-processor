package de.c.seiler.opticsannotation.processor;

import java.util.Arrays;

public enum OpticType
{

  Lens(Mode.Mandatory, Target.Object, Base.Lens),
  IntLens(Mode.Mandatory, Target.Int, Base.Lens),
  LongLens(Mode.Mandatory, Target.Long, Base.Lens),
  DoubleLens(Mode.Mandatory, Target.Double, Base.Lens),
  OptionalLens(Mode.Optional, Target.Object, Base.Lens),
  OptionalIntLens(Mode.Optional, Target.Int, Base.Lens),
  OptionalLongLens(Mode.Optional, Target.Long, Base.Lens),
  OptionalDoubleLens(Mode.Optional, Target.Double, Base.Lens),
  View(Mode.Mandatory, Target.Object, Base.View),
  IntView(Mode.Mandatory, Target.Int, Base.View),
  LongView(Mode.Mandatory, Target.Long, Base.View),
  DoubleView(Mode.Mandatory, Target.Double, Base.View),
  OptionalView(Mode.Optional, Target.Object, Base.View),
  OptionalIntView(Mode.Optional, Target.Int, Base.View),
  OptionalLongView(Mode.Optional, Target.Long, Base.View),
  OptionalDoubleView(Mode.Optional, Target.Double, Base.View);

  final Mode mode;
  final Target target;
  final Base base;

  private OpticType(OpticType.Mode mode, OpticType.Target target, OpticType.Base base)
  {
    this.mode = mode;
    this.target = target;
    this.base = base;
  }

  public static OpticType find(OpticType.Mode m, OpticType.Target t, OpticType.Base b)
  {
    OpticType result = Arrays.stream(OpticType.values()).filter(o -> o.mode == m && o.target == t && o.base == b)
        .findAny().orElseThrow(IllegalArgumentException::new);
    return result;
  }

  public static OpticType findOptionalOptic(OpticType ot)
  {
    OpticType result = find(Mode.Optional, ot.target, ot.base);
    return result ;
  }

  public static OpticType findMandatoryOptic(OpticType ot)
  {
    OpticType result = find(Mode.Mandatory, ot.target, ot.base);
    return result ;
  }

  public static OpticType weaker(OpticType a, OpticType b)
  {
    Mode mode = a.mode.compareTo(b.mode) < 0?b.mode:a.mode;
    Target target = a.target.compareTo(b.target) < 0?b.target:a.target;
    Base base = a.base.compareTo(b.base) < 0?b.base:a.base;
    OpticType result = find(mode, target, base);
    return result;
  }

  public boolean istMandatory()
  {
    return this.mode == Mode.Mandatory;
  }
  
  public boolean isPrimitive()
  {
    return this.target!=Target.Object;
  }
  
  public enum Mode
  {
    Mandatory, Optional
  };

  public enum Target
  {
    Object,
    Int,
    Long,
    Double
  };

  public enum Base
  {
    Lens,
    View
  }

  
}
