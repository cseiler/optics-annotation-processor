package de.c.seiler.opticsannotation.processor;

import de.c.seiler.simpleoptics.Lens;

import lombok.Value;
import lombok.experimental.Wither;

@Value
@Wither
public class OpticInfo
{
  AccessorInfo accessor;
  OpticType type;
  public static final Lens<OpticInfo, AccessorInfo> Accessor = new Lens<>(OpticInfo::getAccessor,
      (a, b) -> a.withAccessor(b));
  public static final Lens<OpticInfo, ClassInfo> Accessor_Base = Accessor.andThen(AccessorInfo.Base);
  public static final Lens<OpticInfo, String> Accessor_Base_Name = Accessor.andThen(AccessorInfo.Base).andThen(ClassInfo.Name);
  public static final Lens<OpticInfo, String> Accessor_UtilityClass = Accessor.andThen(AccessorInfo.UtilityClass);
  public static final Lens<OpticInfo, ClassInfo> Accessor_Target = Accessor.andThen(AccessorInfo.Target);
  public static final Lens<OpticInfo, String> Accessor_Target_Name = Accessor.andThen(AccessorInfo.Target).andThen(ClassInfo.Name);
  public static final Lens<OpticInfo, String> Accessor_TargetName = Accessor.andThen(AccessorInfo.TargetName);

}
