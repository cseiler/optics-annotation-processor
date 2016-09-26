package de.c.seiler.opticsannotation.processor;

import java.util.List;
import java.util.Map;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;

import de.c.seiler.opticsannotation.processor.util.Tree;

public interface UtilityClassGenerator
{

  void generateUtilityClasses(Messager messager, Filer filer, Map<String, List<Tree<OpticInfo>>> dependencies);

}
