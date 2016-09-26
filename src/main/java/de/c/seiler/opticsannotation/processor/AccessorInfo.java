package de.c.seiler.opticsannotation.processor;

import de.c.seiler.simpleoptics.Lens;
import lombok.Value;
import lombok.experimental.Wither;

@Value
@Wither
public class AccessorInfo
{ 
  
  String utilityClass;
  String base;
  String targetName;
  String target;
  boolean nullable;
  String getterName;
  String setterName;
  String withName;
  
  public static final Lens<AccessorInfo, String> Base = new Lens<>(AccessorInfo::getBase, (a,b) ->a.withBase(b));
  public static final Lens<AccessorInfo, String> UtilityClass = new Lens<>(AccessorInfo::getUtilityClass, (a,b) ->a.withUtilityClass(b));
  public static final Lens<AccessorInfo, String> Target = new Lens<>(AccessorInfo::getTarget, (a,b) ->a.withTarget(b));
  public static final Lens<AccessorInfo, String> TargetName = new Lens<>(AccessorInfo::getTargetName, (a,b) ->a.withTargetName(b));


}
