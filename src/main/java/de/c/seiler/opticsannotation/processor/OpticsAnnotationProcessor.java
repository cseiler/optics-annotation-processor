package de.c.seiler.opticsannotation.processor;

import static de.c.seiler.opticsannotation.processor.util.Strings.isPrimitive;
import static de.c.seiler.opticsannotation.processor.util.Strings.isPrimitiveBoolean;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

import de.c.seiler.opticsannotation.annotation.Optics;
import de.c.seiler.opticsannotation.processor.OpticType.Base;
import de.c.seiler.opticsannotation.processor.OpticType.Mode;
import de.c.seiler.opticsannotation.processor.OpticType.Target;
import de.c.seiler.opticsannotation.processor.generator.JavaPoetUtilityClassGenerator;
import de.c.seiler.opticsannotation.processor.util.Tree;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes(value = {"de.c.seiler.opticsannotation.annotation.Optics"})
public class OpticsAnnotationProcessor extends AbstractProcessor
{
  private static String DEFAULTSUFFIX = "Optics";
  private Types typeUtils;
  private Elements elementUtils;
  private Filer filer;
  private Messager messager;
  private AccessorsProcessor gsProc;
  private UtilityClassGenerator utilityClassGenerator;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv)
  {
    super.init(processingEnv);
    typeUtils = processingEnv.getTypeUtils();
    elementUtils = processingEnv.getElementUtils();
    filer = processingEnv.getFiler();
    messager = processingEnv.getMessager();
    gsProc = new AccessorsProcessorImpl();
    utilityClassGenerator = new JavaPoetUtilityClassGenerator();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
  {
    try
    {
      List<AccessorInfo> accessors = analyseCode(roundEnv);
      List<OpticInfo> opticInfos = analyseAccessors(accessors);
      Map<String, List<Tree<OpticInfo>>> dependencies = buildDependencyTree(opticInfos);

      utilityClassGenerator.generateUtilityClasses(messager, filer, dependencies);
    }
    catch (Throwable t)
    {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      t.printStackTrace(new PrintWriter(baos));

      messager.printMessage(Kind.ERROR, "Something went horribly wrong " + t.toString() + " > " + baos.toString());
    }
    return true;
  }

  private List<OpticInfo> analyseAccessors(List<AccessorInfo> accessors)
  {
    List<OpticInfo> result = accessors.stream().map(this::accessorToOpticInfo).collect(toList());
    return result;
  }

  private OpticInfo accessorToOpticInfo(AccessorInfo a)
  {
    String target = a.getTarget().getName();
    OpticInfo result = new OpticInfo(a, null);

    Base base;
    if (!isPrimitiveBoolean(target) && (a.getWithName() != null || a.getSetterName() != null))
      base = OpticType.Base.Lens;
    else
      base = OpticType.Base.View;
    Mode mode;
    if (a.isNullable() && !isPrimitive(target) && !isPrimitiveBoolean(target))
      mode = OpticType.Mode.Optional;
    else
      mode = OpticType.Mode.Mandatory;
    Target otarget;
    if (isPrimitive(target))
    {
      if (java.lang.Integer.TYPE.toString().equals(target))
        otarget = OpticType.Target.Int;
      else if (java.lang.Long.TYPE.toString().equals(target))
        otarget = OpticType.Target.Long;
      else if (java.lang.Double.TYPE.toString().equals(target))
        otarget = OpticType.Target.Double;
      else
        throw new IllegalArgumentException("unexpected type " + target);
    }
    else
      otarget = OpticType.Target.Object;
    OpticType ot = OpticType.find(mode, otarget, base);

    result = result.withType(ot);
    return result;
  }

  private Map<String, List<Tree<OpticInfo>>> buildDependencyTree(List<OpticInfo> opticInfos)
  {
    Map<ClassInfo, List<OpticInfo>> byBase = opticInfos.stream()
        .collect(groupingBy(OpticInfo.Accessor_Base::get));
    Map<String, List<OpticInfo>> byUtilityClass = opticInfos.stream()
        .collect(groupingBy(OpticInfo.Accessor_UtilityClass::get));

    Map<String, List<Tree<OpticInfo>>> result = new HashMap<>();
    byUtilityClass.entrySet().forEach(e -> {
      List<OpticInfo> ref = e.getValue();
      List<Tree<OpticInfo>> l = ref.stream()
          .map(rr -> refs(byBase, rr)).collect(toList());
      result.put(e.getKey(), l);
    });
    return result;
  }

  private Tree<OpticInfo> refs(Map<ClassInfo, List<OpticInfo>> byBase, OpticInfo r)
  {
    ClassInfo t = OpticInfo.Accessor_Target.get(r);
    List<OpticInfo> targets = byBase.getOrDefault(t, new ArrayList<>());
    Tree<OpticInfo> result = new Tree<OpticInfo>(r);
    if (targets.isEmpty())
      return result;
    result = result.withLeaves(targets.stream().map(t1 -> refs(byBase, t1)).collect(toList()));
    return result;
  }

  private List<AccessorInfo> analyseCode(RoundEnvironment roundEnv)
  {
    Set<? extends Element> ae = roundEnv.getElementsAnnotatedWith(Optics.class);

    return ae.stream().filter(e -> validateAndEmitErrors(messager, e)).flatMap(element -> {
      TypeElement te = (TypeElement) element;
      Optics annotation = te.getAnnotation(Optics.class);

      //determine UtilityClass name
      String presetUtilityClass = annotation.utilityClass();
      String[] excludedFields = annotation.exclude();
      String utilityClass = buildUtilityClassName(te.getQualifiedName(), presetUtilityClass);

      return gsProc.buildAccessors(typeUtils, elementUtils, utilityClass, excludedFields,
          te);
    }).collect(Collectors.toList());
  }

  private String buildUtilityClassName(Name typeName, String presetUtilityClass)
  {
    String result = presetUtilityClass;
    if (presetUtilityClass == null || presetUtilityClass.isEmpty())
      result = typeName.toString() + DEFAULTSUFFIX;
    return result;
  }

  private boolean validateAndEmitErrors(Messager messager, Element element)
  {
    boolean result = true;

    if (element.getKind() != ElementKind.CLASS)
    {
      messager.printMessage(Kind.ERROR, "Only class annotations allowed.", element);
      result = false;
    }

    return result;
  }

}
