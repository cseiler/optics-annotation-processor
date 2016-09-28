package de.c.seiler.opticsannotation.processor;

import static de.c.seiler.opticsannotation.processor.util.Strings.isPrimitiveBoolean;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import de.c.seiler.opticsannotation.annotation.Optics;

import de.c.seiler.opticsannotation.processor.util.Strings;
import lombok.Value;
import lombok.experimental.Wither;

public class AccessorsProcessorImpl implements AccessorsProcessor
{
  private static final String NON_NULL = "NonNull";
  private static final String GETTER = "Getter";
  private static final String SETTER = "Setter";
  private static final String WITHER = "Wither";
  private static final String DATA = "Data";
  private static final String VALUE = "Value";

  @Override
  public List<AccessorInfo> buildAccessors(Types typeUtils, Elements elementUtils, String utilityClass,
      String[] excludedFields, TypeElement element)
  {
    List<String> exFields = Arrays.asList(excludedFields);
    List<AccessorInfo> result = Collections.emptyList();

    //find all fields
    List<? extends Element> ee = element.getEnclosedElements();

    List<? extends Element> fields = ee.stream()
        .filter(e0 -> e0.getKind().equals(ElementKind.FIELD))
        .filter(el -> !exFields.contains(el.getSimpleName().toString()))
        .collect(toList());
    List<? extends AnnotationMirror> classAnnotations = elementUtils.getAllAnnotationMirrors(element)
        .stream()
        .filter(am -> !am.getAnnotationType().asElement().toString().equals(Optics.class.getName()))
        .collect(Collectors.toList());

    List<? extends Element> methods = ee.stream()
        .filter(e0 -> e0.getKind().equals(ElementKind.METHOD))
        .collect(toList());

    result = fields.stream()
        .map(f -> fieldToElementInfo(classAnnotations, f))
        .filter(p -> p != null)
        .map(ei -> elementInfoToAccessors(utilityClass, typeUtils, element, methods, ei))
        .filter(p -> p != null)
        .collect(toList());
    return result;
  }

  private AccessorInfo elementInfoToAccessors(String utilityClass, Types typeUtils, TypeElement enclosingElementType,
      List<? extends Element> methods,
      ElementInfo ei)
  {
    String name = ei.getName();
    String cname = Strings.capitalize(name);

    //for each field look for getters (special case lombok)
    //for each field look for suitable (!) setters (special case lombok)

    AccessorInfo result = null;
    String target = ei.getClassName();
    result = new AccessorInfo(utilityClass, enclosingElementType.toString(), cname,
        target, !ei.isNonNull(),
        null, null, null);

    //getter
    if (!isPrimitiveBoolean(target))
    {
      String getterName = "get" + cname;
      boolean found = findGetter(typeUtils, methods, ei, getterName);
      if (found)
        result = result.withGetterName(getterName);
    }
    else
    {
      String getterName = "is" + cname;
      boolean found = findGetter(typeUtils, methods, ei, getterName);
      if (found)
        result = result.withGetterName(getterName);
    }
    //setter
    String setterName = "set" + cname;
    boolean found = findSetter(typeUtils, methods, ei, setterName);
    if (found)
      result = result.withSetterName(setterName);
    //with
    String withName = "with" + cname;
    String methName = findWither(typeUtils, enclosingElementType, methods, ei, setterName, withName);
    if (methName != null)
      result = result.withWithName(methName);

    if (!isValid(result))
      return null;
    return result;
  }

  private boolean isValid(AccessorInfo result)
  {
    return result.getGetterName() != null;
  }

  private String findWither(Types typeUtils, TypeElement enclosingElementType, List<? extends Element> methods,
      ElementInfo ei,
      String setterName, String withName)
  {
    //shortcut lombok
    if (ei.isLombokWither())
      return withName;

    boolean found = methods.stream().anyMatch(m -> withMatch(typeUtils, enclosingElementType, ei, withName, m));
    if (found)
      return withName;

    found = methods.stream().anyMatch(m -> withMatch(typeUtils, enclosingElementType, ei, setterName, m));
    if (found)
      return setterName;

    return null;

  }

  private boolean withMatch(Types typeUtils, TypeElement enclosingElementType, ElementInfo ei, String withName,
      Element m)
  {
    boolean nameMatch = withName.equals(m.getSimpleName().toString());
    if (nameMatch)
    {
      ExecutableElement ee = (ExecutableElement) m;
      List<? extends VariableElement> params = ee.getParameters();
      if (params.size() == 1)
      {
        VariableElement p = params.get(0);
        boolean paramMatch = ei.getClassName().equals(typeMirrorToClass(typeUtils, p.asType()));
        if (paramMatch)
        {
          boolean returnTypeMatch = enclosingElementType.asType().equals(ee.getReturnType());
          if(returnTypeMatch)
            return m.getModifiers().contains(Modifier.PUBLIC);
          return false;
        }
      }
    }
    return false;
  }

  private boolean findSetter(Types typeUtils, List<? extends Element> methods, ElementInfo ei, String setterName)
  {
    //shortcut lombok
    if (ei.isLombokSetter())
      return true;

    boolean found = methods.stream().anyMatch(m -> {
      boolean nameMatch = setterName.equals(m.getSimpleName().toString());
      if (nameMatch)
      {
        ExecutableElement ee = (ExecutableElement) m;
        List<? extends VariableElement> params = ee.getParameters();
        if (params.size() == 1)
        {
          VariableElement p = params.get(0);
          boolean paramMatch = ei.getClassName().equals(typeMirrorToClass(typeUtils, p.asType()));
          if (paramMatch)
          {
            boolean returnTypeMatch = typeMirrorToClass(typeUtils, ee.getReturnType()) == null;
            if (returnTypeMatch)
              return m.getModifiers().contains(Modifier.PUBLIC);
            return false;
          }
        }
      }
      return false;
    });
    return found;
  }

  private boolean findGetter(Types typeUtils, List<? extends Element> methods, ElementInfo ei, String getterName)
  {
    //shortcut lombok
    if (ei.isLombokGetter())
      return true;

    boolean found = methods.stream().anyMatch(m -> {
      boolean nameMatch = getterName.equals(m.getSimpleName().toString());
      if (nameMatch)
      {
        ExecutableElement ee = (ExecutableElement) m;
        boolean returnTypeMatch = ei.getClassName().equals(typeMirrorToClass(typeUtils, ee.getReturnType()));
        if (returnTypeMatch)
        {
          boolean noParams = ee.getParameters().isEmpty();
          if (noParams)
            return m.getModifiers().contains(Modifier.PUBLIC);
          return false;
        }
      }
      return false;
    });
    return found;
  }

  @Value
  @Wither
  private static class ElementInfo
  {
    Element element;
    String name;
    String className;
    boolean nonNull;
    boolean lombokGetter;
    boolean lombokSetter;
    boolean lombokWither;
  }

  private String typeMirrorToClass(Types typeUtils, TypeMirror tm)
  {
    Element e = typeUtils.asElement(tm);
    TypeKind kind = tm.getKind();
    String result = null;
    switch (kind)
    {
      case DECLARED:
        result = toClassDeclaredType(e);
        break;
      case INT:
        result = java.lang.Integer.TYPE.toString();
        break;
      case LONG:
        result = java.lang.Long.TYPE.toString();
        break;
      case DOUBLE:
        result = java.lang.Double.TYPE.toString();
        break;
      case BOOLEAN:
        result = java.lang.Boolean.TYPE.toString();
        break;
      default:
        break;
    }
    return result;
  }

  private String toClassDeclaredType(final Element t)
  {
    String result;

    DeclaredType dt = (DeclaredType) t.asType();
    TypeElement te = (TypeElement) dt.asElement();
    Name qn = te.getQualifiedName();
    result = qn.toString();
    //    try
    //    {
    //      result = Class.forName(qn.toString());
    //    }
    //    catch (ClassNotFoundException e)
    //    {
    //      e.printStackTrace();
    //      result = null;
    //    }
    return result;
  }

  private ElementInfo fieldToElementInfo(List<? extends AnnotationMirror> classAnnotations, Element field)
  {
    Name sn = field.getSimpleName();
    TypeMirror ft = field.asType();
    TypeKind kind = ft.getKind();

    ElementInfo result = new ElementInfo(field, sn.toString(), null, false, false, false, false);
    switch (kind)
    {
      case DECLARED:
        result = toNameClassDeclaredType(classAnnotations, field, result);
        break;
      case INT:
        result = result.withClassName(java.lang.Integer.TYPE.getName());
        result = fieldAnnotations(classAnnotations, field, result);
        break;
      case LONG:
        result = result.withClassName(java.lang.Long.TYPE.getName());
        result = fieldAnnotations(classAnnotations, field, result);
        break;
      case DOUBLE:
        result = result.withClassName(java.lang.Double.TYPE.getName());
        result = fieldAnnotations(classAnnotations, field, result);
        break;
      case BOOLEAN:
        result = result.withClassName(java.lang.Boolean.TYPE.getName());
        result = fieldAnnotations(classAnnotations, field, result);
        break;
      default:
        result = null;
        break;
    }
    return result;
  }

  private ElementInfo toNameClassDeclaredType(List<? extends AnnotationMirror> classAnnotations, final Element field,
      ElementInfo sofar)
  {

    DeclaredType dt = (DeclaredType) field.asType();
    TypeElement te = (TypeElement) dt.asElement();
    Name qn = te.getQualifiedName();
    ElementInfo result = sofar;
    result = result.withClassName(qn.toString());
    result = fieldAnnotations(classAnnotations, field, result);
    return result;
  }

  private ElementInfo fieldAnnotations(List<? extends AnnotationMirror> classAnnotations, final Element field,
      ElementInfo result)
  {
    List<AnnotationMirror> annotations = new ArrayList<>(field.getAnnotationMirrors());
    annotations.addAll(classAnnotations);
    if (!annotations.isEmpty())
    {
      List<Name> asimple = annotations.stream().map(a -> a.getAnnotationType().asElement().getSimpleName())
          .collect(toList());
      boolean nonNullA = asimple.stream()
          .anyMatch(n -> n.toString().contains(NON_NULL));
      result = result.withNonNull(nonNullA);
      boolean lGetter = asimple.stream()
          .anyMatch(n -> n.toString().contains(GETTER) || n.toString().contains(DATA) || n.toString().contains(VALUE));
      result = result.withLombokGetter(lGetter);
      boolean lSetter = asimple.stream()
          .anyMatch(n -> n.toString().contains(SETTER) || n.toString().contains(DATA));
      result = result.withLombokSetter(lSetter);
      boolean lWither = asimple.stream()
          .anyMatch(n -> n.toString().contains(WITHER));
      result = result.withLombokWither(lWither);
    }
    return result;
  }

}