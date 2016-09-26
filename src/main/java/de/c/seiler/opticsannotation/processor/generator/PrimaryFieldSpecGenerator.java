package de.c.seiler.opticsannotation.processor.generator;

import static de.c.seiler.opticsannotation.processor.util.Strings.isPrimitive;
import static de.c.seiler.opticsannotation.processor.util.Strings.isPrimitiveBoolean;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;

import de.c.seiler.opticsannotation.processor.AccessorInfo;
import de.c.seiler.opticsannotation.processor.OpticInfo;
import de.c.seiler.opticsannotation.processor.OpticType;
import de.c.seiler.opticsannotation.processor.util.Strings;
import lombok.Value;

public class PrimaryFieldSpecGenerator
{
  private static final String SETTER = "new $T<>($T::$L, (a,b) -> {a.$L(b); return a;})";
  private static final String OPTIONAL_SETTER = "new $T<>(new $T<>($T::$L, (a,b) -> {a.$L(b); return a;}))";

  private static final String WITHER = "new $T<>($T::$L, (a,b) -> a.$L(b))";
  private static final String OPTIONAL_WITHER = "new $T<>(new $T<>($T::$L, (a,b) -> a.$L(b)))";

  private static final String VIEW = "new $T<>($T::$L)";
  private static final String OPTIONAL_VIEW = "new $T<>(new $T<>($T::$L))";
  private JavaPoetUtilityClassGenerator parent;

  public PrimaryFieldSpecGenerator(JavaPoetUtilityClassGenerator parent)
  {
    this.parent = parent;
  }
  
  public FieldSpec toPrimaryFieldSpec(OpticInfo oi)
  {
    FieldSpec result;
    OpticType ot = oi.getType();
    AccessorInfo ai = oi.getAccessor();
    switch (ot)
    {
      case Lens:
      case IntLens:
      case LongLens:
      case DoubleLens:
        result = toPrimaryLensFieldSpec(ai, ot);
        break;
      case OptionalLens:
        result = toPrimaryOptionalLensFieldSpec(ai, ot);
        break;
      case View:
      case IntView:
      case LongView:
      case DoubleView:
        result = toPrimaryViewFieldSpec(ai, ot);
        break;
      case OptionalView:
        result = toPrimaryOptionalViewFieldSpec(ai, ot);
        break;
      case OptionalIntLens:
      case OptionalLongLens:
      case OptionalDoubleLens:
      case OptionalIntView:
      case OptionalLongView:
      case OptionalDoubleView:
      default:
        throw new IllegalArgumentException("unexpected type " + ot);
    }
    return result;
  }

  private FieldSpec toPrimaryLensFieldSpec(AccessorInfo gs, OpticType ot)
  {
    ClassName base = ClassName.bestGuess(gs.getBase());
    OpticClassInfo oi = buildOpticClassInfo(base, gs.getTarget(), ot);

    String wthName;
    FieldSpec result;
    if ((wthName = gs.getWithName()) != null)
      result = FieldSpec
          .builder(oi.getOpticFor(), gs.getTargetName(), Modifier.PUBLIC, Modifier.STATIC)
          .initializer(CodeBlock.of(WITHER, oi.getOptic(), base, gs.getGetterName(), wthName))
          .build();
    else
      result = FieldSpec
          .builder(oi.getOpticFor(), gs.getTargetName(), Modifier.PUBLIC, Modifier.STATIC)
          .initializer(CodeBlock.of(SETTER, oi.getOpticFor(), base, gs.getGetterName(), gs.getSetterName()))
          .build();
    return result;
  }

  private FieldSpec toPrimaryOptionalLensFieldSpec(AccessorInfo gs, OpticType ot)
  {
    ClassName base = ClassName.bestGuess(gs.getBase());
    OpticClassInfo oi = buildOpticClassInfoForOptional(base, gs.getTarget(), ot);
    String wthName;
    FieldSpec result;
    if ((wthName = gs.getWithName()) != null)
      result = FieldSpec
          .builder(oi.getOpticFor(), gs.getTargetName(), Modifier.PUBLIC, Modifier.STATIC)
          .initializer(CodeBlock.of(OPTIONAL_WITHER, oi.getOoptic(), oi.getOptic(), base, gs.getGetterName(), wthName))
          .build();
    else
      result = FieldSpec
          .builder(oi.getOpticFor(), gs.getTargetName(), Modifier.PUBLIC, Modifier.STATIC)
          .initializer(CodeBlock.of(OPTIONAL_SETTER, oi.getOoptic(), oi.getOptic(), base, gs.getGetterName(),
              gs.getSetterName()))
          .build();
    return result;
  }

  private FieldSpec toPrimaryViewFieldSpec(AccessorInfo gs, OpticType ot)
  {
    ClassName base = ClassName.bestGuess(gs.getBase());
    OpticClassInfo oi = buildOpticClassInfo(base, gs.getTarget(), ot);
    FieldSpec result = FieldSpec
        .builder(oi.getOpticFor(), gs.getTargetName(), Modifier.PUBLIC, Modifier.STATIC)
        .initializer(CodeBlock.of(VIEW, oi.getOptic(), base, gs.getGetterName()))
        .build();
    return result;
  }

  private FieldSpec toPrimaryOptionalViewFieldSpec(AccessorInfo gs, OpticType ot)
  {
    ClassName base = ClassName.bestGuess(gs.getBase());
    OpticClassInfo oi = buildOpticClassInfoForOptional(base, gs.getTarget(), ot);
    FieldSpec result = FieldSpec
        .builder(oi.getOpticFor(), gs.getTargetName(), Modifier.PUBLIC, Modifier.STATIC)
        .initializer(CodeBlock.of(OPTIONAL_VIEW, oi.getOoptic(), oi.getOptic(), base, gs.getGetterName()))
        .build();
    return result;
  }

  @Value
  private static class OpticClassInfo
  {
    ClassName ooptic;
    ClassName optic;
    ParameterizedTypeName opticFor;
  }

  private OpticClassInfo buildOpticClassInfo(ClassName base, String target, OpticType ot)
  {
    ClassName ooptic;
    ClassName optic;
    ParameterizedTypeName opticFor;

    optic = parent.opticTypeToClassName.get(ot);
    ooptic = parent.opticTypeToClassName.get(OpticType.findOptionalOptic(ot));
    if (isPrimitive(target))
      opticFor = ParameterizedTypeName.get(optic, base);
    else if(isPrimitiveBoolean(target))
      opticFor = ParameterizedTypeName.get(optic, base, ClassName.get(Boolean.class));      
    else
      opticFor = ParameterizedTypeName.get(optic, base, ClassName.bestGuess(target));
    OpticClassInfo of = new OpticClassInfo(ooptic, optic, opticFor);
    return of;
  }

  private OpticClassInfo buildOpticClassInfoForOptional(ClassName base, String target, OpticType ot)
  {
    ClassName ooptic;
    ClassName optic;
    ParameterizedTypeName opticFor;

    optic = parent.opticTypeToClassName.get(OpticType.findMandatoryOptic(ot));
    ooptic = parent.opticTypeToClassName.get(OpticType.findOptionalOptic(ot));

    if (Strings.isPrimitive(target))
      opticFor = ParameterizedTypeName.get(ooptic, base);
    else if(isPrimitiveBoolean(target))
      opticFor = ParameterizedTypeName.get(ooptic, base, ClassName.get(Boolean.class));      
    else
      opticFor = ParameterizedTypeName.get(ooptic, base, ClassName.bestGuess(target));

    OpticClassInfo of = new OpticClassInfo(ooptic, optic, opticFor);
    return of;
  }

}
