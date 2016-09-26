package de.c.seiler.opticsannotation.processor.generator;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic.Kind;

import de.c.seiler.simpleoptics.Lens;
import de.c.seiler.simpleoptics.OptionalLens;
import de.c.seiler.simpleoptics.OptionalView;
import de.c.seiler.simpleoptics.View;
import de.c.seiler.simpleoptics.primitive.DoubleLens;
import de.c.seiler.simpleoptics.primitive.DoubleView;
import de.c.seiler.simpleoptics.primitive.IntLens;
import de.c.seiler.simpleoptics.primitive.IntView;
import de.c.seiler.simpleoptics.primitive.LongLens;
import de.c.seiler.simpleoptics.primitive.LongView;
import de.c.seiler.simpleoptics.primitive.OptionalDoubleLens;
import de.c.seiler.simpleoptics.primitive.OptionalDoubleView;
import de.c.seiler.simpleoptics.primitive.OptionalIntLens;
import de.c.seiler.simpleoptics.primitive.OptionalIntView;
import de.c.seiler.simpleoptics.primitive.OptionalLongLens;
import de.c.seiler.simpleoptics.primitive.OptionalLongView;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import de.c.seiler.opticsannotation.processor.OpticInfo;
import de.c.seiler.opticsannotation.processor.OpticType;
import de.c.seiler.opticsannotation.processor.UtilityClassGenerator;
import de.c.seiler.opticsannotation.processor.util.Tree;

public class JavaPoetUtilityClassGenerator implements UtilityClassGenerator
{
  public EnumMap<OpticType, ClassName> opticTypeToClassName = new EnumMap<>(OpticType.class);

  private PrimaryFieldSpecGenerator primary;
  private SecondaryFieldSpecGenerator secondary;

  public JavaPoetUtilityClassGenerator()
  {
    opticTypeToClassName.put(OpticType.Lens, ClassName.get(Lens.class));
    opticTypeToClassName.put(OpticType.IntLens, ClassName.get(IntLens.class));
    opticTypeToClassName.put(OpticType.LongLens, ClassName.get(LongLens.class));
    opticTypeToClassName.put(OpticType.DoubleLens, ClassName.get(DoubleLens.class));
    opticTypeToClassName.put(OpticType.OptionalLens, ClassName.get(OptionalLens.class));
    opticTypeToClassName.put(OpticType.OptionalIntLens, ClassName.get(OptionalIntLens.class));
    opticTypeToClassName.put(OpticType.OptionalLongLens, ClassName.get(OptionalLongLens.class));
    opticTypeToClassName.put(OpticType.OptionalDoubleLens, ClassName.get(OptionalDoubleLens.class));
    opticTypeToClassName.put(OpticType.View, ClassName.get(View.class));
    opticTypeToClassName.put(OpticType.IntView, ClassName.get(IntView.class));
    opticTypeToClassName.put(OpticType.LongView, ClassName.get(LongView.class));
    opticTypeToClassName.put(OpticType.DoubleView, ClassName.get(DoubleView.class));
    opticTypeToClassName.put(OpticType.OptionalView, ClassName.get(OptionalView.class));
    opticTypeToClassName.put(OpticType.OptionalIntView, ClassName.get(OptionalIntView.class));
    opticTypeToClassName.put(OpticType.OptionalLongView, ClassName.get(OptionalLongView.class));
    opticTypeToClassName.put(OpticType.OptionalDoubleView, ClassName.get(OptionalDoubleView.class));

    this.primary = new PrimaryFieldSpecGenerator(this);
    this.secondary = new SecondaryFieldSpecGenerator(this);
  }
  //  public static final View<Tree<OpticInfo>, OpticInfo> Oi = new View<>(Tree<OpticInfo>::getValue);
  //  public static final View<Tree<OpticInfo>, AccessorInfo> Oi_Accessor = Oi.andThen(OpticInfo.Accessor);
  //  public static final View<Tree<OpticInfo>, String> Oi_Accessor_UtilityClass = Oi.andThen(OpticInfo.Accessor)
  //      .andThen(AccessorInfo.UtilityClass);

  @Override
  public void generateUtilityClasses(Messager messager, Filer filer, Map<String, List<Tree<OpticInfo>>> dependencies)
  {
    List<String> sortedUtilityClasses = sortByDependencies(dependencies);

    sortedUtilityClasses.forEach(utilityClass -> {
      int index = utilityClass.lastIndexOf(".");
      String pack = "";
      String name = utilityClass;

      if (index > 0)
      {
        pack = utilityClass.substring(0, index);
        name = utilityClass.substring(index + 1, utilityClass.toString().length());
      }
      List<Tree<OpticInfo>> ref = dependencies.get(utilityClass);

      List<FieldSpec> lenses = ref.stream()
          .flatMap(this::toFieldSpecs)
          .collect(Collectors.toList());

      writeFile(messager, filer, pack, name, lenses);

    });
  }

  void writeFile(Messager messager, Filer filer, String pack, String name, List<FieldSpec> lenses)
  {
    if (!lenses.isEmpty())
    {
      TypeSpec ul = TypeSpec.classBuilder(name)
          .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
          .addFields(lenses)
          .build();

      JavaFile jf = JavaFile.builder(pack, ul).build();
      try
      {
        jf.writeTo(filer);
      }
      catch (IOException e)
      {
        messager.printMessage(Kind.ERROR, e.toString());
      }
    }
  }

  private List<String> sortByDependencies(Map<String, List<Tree<OpticInfo>>> dependencies)
  {
    Map<SortedSet<String>, List<String>> depsPerUc = dependencies.keySet().stream()
        .collect(Collectors.groupingBy(ucn -> {
          List<Tree<OpticInfo>> lt = dependencies.get(ucn);
          SortedSet<String> ucns = lt.stream().collect(TreeSet::new,
              this::utilityClassesFromTree, TreeSet::addAll);
          return ucns;
        }));
    List<String> result = depsPerUc.keySet().stream()
        .sorted((a, b) -> a.size() - b.size())
        .flatMap(k -> depsPerUc.get(k).stream())
        .distinct()
        .collect(toList());

    return result;
  }

  void utilityClassesFromTree(TreeSet<String> a, Tree<OpticInfo> t)
  {
    TreeSet<String> uu = t.reduce(new TreeSet<String>(),
        (at, tt) -> {
          at.add(OpticInfo.Accessor_UtilityClass.get(tt));
          return at;
        },
        (x, y) -> {
          x.addAll(y);
          return x;
        });
    a.addAll(uu);
  }

  private Stream<FieldSpec> toFieldSpecs(Tree<OpticInfo> tree)
  {
    return Stream.concat(
        Stream.of(primary.toPrimaryFieldSpec(tree.getValue())),
        secondary.toSecondaryFieldSpecs(tree));
  }

}
