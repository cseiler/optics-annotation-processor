package de.c.seiler.opticsannotation.processor;

import java.util.stream.Stream;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public interface AccessorsProcessor
{

  Stream<AccessorInfo> buildAccessors(Types typeUtils, Elements elementUtils, String utilityClass,
      String[] excludedFields, TypeElement te);

}