package de.c.seiler.opticsannotation.processor;

import java.util.List;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public interface AccessorsProcessor
{

  List<AccessorInfo> buildAccessors(Types typeUtils, Elements elementUtils, String utilityClass,
      String[] excludedFields, TypeElement te);

}