package de.c.seiler.opticsannotation.processor;

import java.util.List;

import de.c.seiler.simpleoptics.Lens;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Wither;

@Value
@Wither
public class ClassInfo
{
  @NonNull
  String name;
  List<ClassInfo> typeParams;

  public static final Lens<ClassInfo, String> Name = new Lens<>(ClassInfo::getName, (a, b) -> a.withName(b));
  public static final Lens<ClassInfo, List<ClassInfo>> TypeParams = new Lens<>(ClassInfo::getTypeParams,
      (a, b) -> a.withTypeParams(b));

}
