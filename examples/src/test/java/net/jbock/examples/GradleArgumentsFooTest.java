package net.jbock.examples;

import net.jbock.examples.fixture.ParserTestFixture;
import org.junit.jupiter.api.Test;

import java.util.Optional;

class GradleArgumentsFooTest {

    private final GradleArguments_FooParser parser = new GradleArguments_FooParser();

    private final ParserTestFixture<GradleArguments.Foo> f =
            ParserTestFixture.create(parser::parse);

    @Test
    void testParserForNestedClass() {
        f.assertThat("--bar=4")
                .has(GradleArguments.Foo::bar, Optional.of(4));
    }

    @Test
    void testPrint() {
        f.assertPrintsHelp(
                parser.createModel(),
                "\u001B[1mUSAGE\u001B[m",
                "  foo [OPTIONS]",
                "",
                "\u001B[1mOPTIONS\u001B[m",
                "  --bar BAR ",
                "");
    }
}
