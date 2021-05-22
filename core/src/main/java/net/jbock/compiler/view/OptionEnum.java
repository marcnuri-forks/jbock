package net.jbock.compiler.view;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import dagger.Reusable;
import net.jbock.compiler.EnumName;
import net.jbock.compiler.GeneratedTypes;
import net.jbock.compiler.parameter.AbstractParameter;
import net.jbock.convert.ConvertedParameter;
import net.jbock.qualifier.AllParameters;

import javax.inject.Inject;
import javax.lang.model.util.Elements;
import java.util.List;

import static com.squareup.javapoet.ParameterSpec.builder;
import static com.squareup.javapoet.TypeSpec.anonymousClassBuilder;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.jbock.compiler.Constants.STRING_ARRAY;

/**
 * Defines the *_Parser.Option enum.
 *
 * @see GeneratedClass
 */
@Reusable
public class OptionEnum {

  private final AllParameters context;
  private final GeneratedTypes generatedTypes;
  private final FieldSpec descriptionField;
  private final Elements elements;

  @Inject
  OptionEnum(
      AllParameters context,
      GeneratedTypes generatedTypes,
      Elements elements) {
    this.context = context;
    this.generatedTypes = generatedTypes;
    this.elements = elements;
    this.descriptionField = FieldSpec.builder(STRING_ARRAY, "description").build();
  }

  TypeSpec define() {
    List<ConvertedParameter<? extends AbstractParameter>> parameters = context.parameters();
    TypeSpec.Builder spec = TypeSpec.enumBuilder(generatedTypes.optionType());
    for (ConvertedParameter<? extends AbstractParameter> param : parameters) {
      EnumName enumName = param.enumName();
      String enumConstant = enumName.enumConstant();
      CodeBlock description = descriptionBlock(param.parameter().description(elements));
      TypeSpec optionSpec = anonymousClassBuilder(description).build();
      spec.addEnumConstant(enumConstant, optionSpec);
    }
    return spec.addModifiers(PRIVATE)
        .addField(descriptionField)
        .addMethod(privateConstructor())
        .build();
  }

  private CodeBlock descriptionBlock(List<String> lines) {
    CodeBlock.Builder code = CodeBlock.builder();
    for (int i = 0; i < lines.size(); i++) {
      code.add("$S", lines.get(i));
      if (i != lines.size() - 1) {
        code.add(",\n");
      }
    }
    return code.build();

  }

  private MethodSpec privateConstructor() {
    ParameterSpec description = builder(ArrayTypeName.of(String.class), "description").build();
    return MethodSpec.constructorBuilder()
        .addStatement("this.$N = $N", descriptionField, description)
        .addParameter(description)
        .varargs(true)
        .build();
  }
}
