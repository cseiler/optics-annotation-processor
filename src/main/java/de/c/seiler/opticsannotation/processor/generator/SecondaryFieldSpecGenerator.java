package de.c.seiler.opticsannotation.processor.generator;

import static de.c.seiler.opticsannotation.processor.util.Strings.isPrimitive;
import static de.c.seiler.opticsannotation.processor.util.Strings.isPrimitiveBoolean;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import de.c.seiler.opticsannotation.processor.ClassInfo;
import de.c.seiler.opticsannotation.processor.OpticInfo;
import de.c.seiler.opticsannotation.processor.OpticType;
import de.c.seiler.opticsannotation.processor.util.Tree;
import de.c.seiler.opticsannotation.processor.util.Trees;
import lombok.Value;

public class SecondaryFieldSpecGenerator
{
  private static final CharSequence DELIMITER = "_";

  //  public static final View<Tree<OpticInfo>, OpticInfo> Oi = new View<>(Tree<OpticInfo>::getValue);
  //  public static final View<Tree<OpticInfo>, AccessorInfo> Oi_Accessor = Oi.andThen(OpticInfo.Accessor);
  //  public static final View<Tree<OpticInfo>, String> Oi_Accessor_UtilityClass = Oi.andThen(OpticInfo.Accessor)
  //      .andThen(AccessorInfo.UtilityClass);
  private static final String CONNECT = ".andThen(";
  private static final String START = "$N";
  private static final String LOCAL = "$L)";
  private static final String REMOTE = "$L.$L)";

  private JavaPoetUtilityClassGenerator parent;

  public SecondaryFieldSpecGenerator(JavaPoetUtilityClassGenerator parent)
  {
    this.parent = parent;
  }

  public Stream<FieldSpec> toSecondaryFieldSpecs(Tree<OpticInfo> tree)
  {
    List<List<OpticInfo>> paths = Trees.treePaths(tree);
    Stream<FieldSpec> secondaries = paths.stream()
        .filter(l -> l.size() > 1) //remove primaries
        .map(this::toSecondaryFieldSpec);
    return secondaries;
  }

  private FieldSpec toSecondaryFieldSpec(List<OpticInfo> l)
  {
    OpticType opticType = l.stream().map(OpticInfo::getType).reduce(OpticType::weaker)
        .orElseThrow(IllegalArgumentException::new);
    String fieldName = l.stream().map(oi -> oi.getAccessor().getTargetName()).collect(Collectors.joining(DELIMITER));

    OpticInfo first = l.get(0);
    OpticInfo last = l.get(l.size() - 1);
    ClassName opticClassName = parent.opticTypeToClassName.get(opticType);
    TypeName baseTypName = buildTypeName(OpticInfo.Accessor_Base.get(first));
    ClassInfo targetClass = OpticInfo.Accessor_Target.get(last);
    ParameterizedTypeName ptn = buildParameterizedTypeName(opticClassName, baseTypName, targetClass);

    FormatAndParams fsAndP = buildFormatStringAndParams(l, first);
    FieldSpec result = FieldSpec
        .builder(ptn, fieldName, Modifier.PUBLIC, Modifier.STATIC)
        .initializer(fsAndP.getFormatString(), fsAndP.getParams().toArray())
        .build();
    return result;
  }

  ParameterizedTypeName buildParameterizedTypeName(ClassName opticClassName, TypeName baseTypName,
      ClassInfo targetClass)
  {
    ParameterizedTypeName ptn;
    if (isPrimitive(targetClass.getName()))
      ptn = ParameterizedTypeName.get(opticClassName, baseTypName);
    else if (isPrimitiveBoolean(targetClass.getName()))
      ptn = ParameterizedTypeName.get(opticClassName, baseTypName, ClassName.get(Boolean.class));
    else
      ptn = ParameterizedTypeName.get(opticClassName, baseTypName, buildTypeName(targetClass));
    return ptn;
  }

  private FormatAndParams buildFormatStringAndParams(List<OpticInfo> l, OpticInfo first)
  {
    StringBuilder formatString = new StringBuilder();
    ArrayList<String> params = new ArrayList<>();
    formatString.append(START);
    params.add(OpticInfo.Accessor_TargetName.get(first));

    String uc = OpticInfo.Accessor_UtilityClass.get(first);

    List<OpticInfo> ll = l.subList(1, l.size());
    for (OpticInfo opticInfo : ll)
    {
      formatString.append(CONNECT);
      String remoteUc = OpticInfo.Accessor_UtilityClass.get(opticInfo);
      if (remoteUc.equals(uc))
        formatString.append(LOCAL);
      else
      {
        formatString.append(REMOTE);
        params.add(remoteUc);
      }
      params.add(OpticInfo.Accessor_TargetName.get(opticInfo));
    }
    return new FormatAndParams(formatString.toString(), params);
  }

  @Value
  private static class FormatAndParams
  {
    String formatString;
    ArrayList<String> params;
  }

  private TypeName buildTypeName(ClassInfo base)
  {
    ClassName baseClassName = ClassName.bestGuess(base.getName());
    List<ClassInfo> tp = base.getTypeParams();
    if (tp.isEmpty())
      return baseClassName;

    TypeName[] typeNames = tp.stream()
        .map(t -> buildTypeName(t))
        .collect(toList())
        .toArray(new TypeName[0]);
    return ParameterizedTypeName.get(baseClassName, typeNames);
  }

}
