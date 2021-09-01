package net.jbock.context;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import net.jbock.processor.SourceElement;
import net.jbock.util.ExFailure;

import javax.inject.Inject;

import static com.squareup.javapoet.ParameterSpec.builder;
import static net.jbock.common.Constants.EITHER;
import static net.jbock.common.Constants.LIST_OF_STRING;

@ContextScope
public final class ParseMethod extends Cached<MethodSpec> {

    private final GeneratedTypes generatedTypes;
    private final SourceElement sourceElement;
    private final CreateModelMethod createModelMethod;
    private final HarvestMethod harvestMethod;
    private final ParserTypeFactory parserTypeFactory;

    @Inject
    ParseMethod(
            GeneratedTypes generatedTypes,
            SourceElement sourceElement,
            CreateModelMethod createModelMethod,
            HarvestMethod harvestMethod,
            ParserTypeFactory parserTypeFactory) {
        this.generatedTypes = generatedTypes;
        this.sourceElement = sourceElement;
        this.createModelMethod = createModelMethod;
        this.harvestMethod = harvestMethod;
        this.parserTypeFactory = parserTypeFactory;
    }

    @Override
    MethodSpec define() {

        ParameterSpec tokens = builder(LIST_OF_STRING, "tokens").build();

        CodeBlock.Builder code = CodeBlock.builder();

        ParserType parserType = parserTypeFactory.get();

        ParameterSpec e = builder(Exception.class, "e").build();
        ParameterSpec parser = parserType.asParam();
        code.addStatement("$T $N = $L", parserType.type(), parser, parserType.init());
        code.add("try {\n").indent()
                .addStatement("$N.parse($N)", parser, tokens)
                .addStatement("return $T.right($N($N))", EITHER,
                        harvestMethod.get(), parser)
                .unindent().add("} catch ($T $N) {\n", ExFailure.class, e).indent()
                .addStatement("return $T.left($N.toError($N()))",
                        EITHER, e, createModelMethod.get())
                .unindent().add("}\n");

        return MethodSpec.methodBuilder("parse")
                .addParameter(tokens)
                .returns(generatedTypes.parseResultType())
                .addCode(code.build())
                .addModifiers(sourceElement.accessModifiers())
                .build();
    }
}
