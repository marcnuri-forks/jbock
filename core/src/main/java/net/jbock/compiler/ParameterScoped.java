package net.jbock.compiler;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.jbock.Command;
import net.jbock.compiler.parameter.Parameter;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

public class ParameterScoped {

  private final ParameterContext parameterContext;

  public ParameterScoped(ParameterContext parameterContext) {
    this.parameterContext = parameterContext;
  }

  public final ExecutableElement sourceMethod() {
    return parameterContext.sourceMethod;
  }

  public final TypeElement sourceElement() {
    return parameterContext.sourceElement;
  }

  public final TypeTool tool() {
    return parameterContext.tool;
  }

  public final ClassName optionType() {
    return parameterContext.optionType;
  }

  final ImmutableList<Parameter> alreadyCreated() {
    return parameterContext.alreadyCreated;
  }

  final String[] description() {
    return parameterContext.description;
  }

  final String bundleKey() {
    return parameterContext.bundleKey;
  }

  public final EnumName enumName() {
    return parameterContext.enumName;
  }

  public final TypeMirror returnType() {
    return sourceMethod().getReturnType();
  }

  public final ParameterSpec constructorParam(TypeMirror constructorParamType) {
    return ParameterSpec.builder(TypeName.get(constructorParamType), enumName().camel()).build();
  }

  public final TypeMirror boxedReturnType() {
    PrimitiveType primitive = returnType().accept(TypeTool.AS_PRIMITIVE, null);
    return primitive == null ? returnType() : tool().types().boxedClass(primitive).asType();
  }

  public final Types types() {
    return tool().types();
  }

  public ParameterContext parameterContext() {
    return parameterContext;
  }

  boolean isHelpEnabled() {
    return !parameterContext.sourceElement.getAnnotation(Command.class).helpDisabled();
  }
}
