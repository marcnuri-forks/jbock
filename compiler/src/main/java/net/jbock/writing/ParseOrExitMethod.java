package net.jbock.writing;

import io.jbock.javapoet.ArrayTypeName;
import io.jbock.javapoet.CodeBlock;
import io.jbock.javapoet.MethodSpec;
import io.jbock.javapoet.ParameterSpec;
import jakarta.inject.Inject;
import net.jbock.contrib.StandardErrorHandler;
import net.jbock.util.AtFileError;
import net.jbock.util.ParseRequest;

import static io.jbock.javapoet.MethodSpec.methodBuilder;
import static io.jbock.javapoet.ParameterSpec.builder;
import static net.jbock.common.Constants.STRING;

@WritingScope
final class ParseOrExitMethod extends HasCommandRepresentation {

    private final GeneratedTypes generatedTypes;
    private final ParseMethod parseMethod;
    private final CreateModelMethod createModelMethod;

    @Inject
    ParseOrExitMethod(
            CommandRepresentation commandRepresentation,
            GeneratedTypes generatedTypes,
            ParseMethod parseMethod,
            CreateModelMethod createModelMethod) {
        super(commandRepresentation);
        this.generatedTypes = generatedTypes;
        this.parseMethod = parseMethod;
        this.createModelMethod = createModelMethod;
    }

    MethodSpec define() {

        ParameterSpec args = builder(ArrayTypeName.of(STRING), "args").build();
        ParameterSpec notSuccess = builder(generatedTypes.parseResultType(), "failure").build();
        ParameterSpec err = builder(AtFileError.class, "err").build();

        CodeBlock.Builder code = CodeBlock.builder();
        code.beginControlFlow("if ($1N.length > 0 && $2S.equals($1N[0]))", args, "--help")
                .add("$T.builder().build()\n", StandardErrorHandler.class).indent()
                .add(".printUsageDocumentation($N());\n", createModelMethod.get()).unindent()
                .addStatement("$T.exit(0)", System.class)
                .endControlFlow();

        code.add("return $T.from($N).expand()\n", ParseRequest.class, args).indent()
                .add(".mapLeft($1N -> $1N.addModel($2N()))\n", err, createModelMethod.get())
                .add(".flatMap(this::$N)\n", parseMethod.get())
                .add(".orElseThrow($N -> {\n", notSuccess).indent()
                .addStatement("$T.builder().build().printErrorMessage($N)",
                        StandardErrorHandler.class, notSuccess)
                .addStatement("$T.exit(1)", System.class)
                .addStatement("return new $T()", RuntimeException.class).unindent()
                .addStatement("})").unindent();
        return methodBuilder("parseOrExit").addParameter(args)
                .addModifiers(sourceElement().accessModifiers())
                .returns(generatedTypes.parseSuccessType())
                .addCode(code.build())
                .build();
    }
}
