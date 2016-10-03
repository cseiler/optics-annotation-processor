package de.c.seiler.opticsannotation.processor;

import de.c.seiler.simpleoptics.Lens;
import lombok.Value;
import lombok.experimental.Wither;

@Value
@Wither
public class AccessorInfo
{ 
  
  String utilityClass;
  ClassInfo base;
  String targetName;
  ClassInfo target;
  boolean nullable;
  String getterName;
  String setterName;
  String withName;
  
  public static final Lens<AccessorInfo, ClassInfo> Base = new Lens<>(AccessorInfo::getBase, (a,b) ->a.withBase(b));
  public static final Lens<AccessorInfo, String> Base_Name = Base.andThen(ClassInfo.Name);
  public static final Lens<AccessorInfo, String> UtilityClass = new Lens<>(AccessorInfo::getUtilityClass, (a,b) ->a.withUtilityClass(b));
  public static final Lens<AccessorInfo, ClassInfo> Target = new Lens<>(AccessorInfo::getTarget, (a,b) ->a.withTarget(b));
  public static final Lens<AccessorInfo, String> Target_Name = Target.andThen(ClassInfo.Name);
  public static final Lens<AccessorInfo, String> TargetName = new Lens<>(AccessorInfo::getTargetName, (a,b) ->a.withTargetName(b));


}
