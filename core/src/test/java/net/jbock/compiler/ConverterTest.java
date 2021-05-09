package net.jbock.compiler;

import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static java.util.Collections.singletonList;
import static net.jbock.compiler.ProcessorTest.fromSource;

class ConverterTest {

  @Test
  void converterImplementsBothFunctionAndSupplier() {
    JavaFileObject javaFile = fromSource(
        "@Converter",
        "class MapMap implements Function<String, String>, Supplier<Function<String, String>> {",
        "  public String apply(String s) { return null; }",
        "  public Function<String, String> get() { return null; }",
        "}",
        "",
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract String foo();",
        "",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("converter should implement Function<String, ?> or Supplier<Function<String, ?>> but not both");
  }

  @Test
  void converterDoesNotImplementFunction() {
    JavaFileObject javaFile = fromSource(
        "@Converter",
        "class MapMap {}",
        "",
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract String foo();",
        "",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("converter should implement Function<String, ?> or Supplier<Function<String, ?>>");
  }

  @Test
  void missingConverterAnnotation() {
    JavaFileObject javaFile = fromSource(
        "class MapMap implements Function<String, String> {",
        "  public String apply(String s) { return null; }",
        "}",
        "",
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract String foo();",
        "",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("converter must be an inner class of the command class, " +
            "or carry the @Converter annotation");
  }

  @Test
  void validArrayMapperSupplier() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = ArrayMapper.class)",
        "  abstract Optional<int[]> foo();",
        "",
        "  @Converter",
        "  static class ArrayMapper implements Supplier<Function<String, int[]>> {",
        "    public Function<String, int[]> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void validArrayMapper() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = ArrayMapper.class)",
        "  abstract Optional<int[]> foo();",
        "",
        "  static class ArrayMapper implements Function<String, int[]> {",
        "    public int[] apply(String s) { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void invalidFlagMapper() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = FlagMapper.class)",
        "  abstract Boolean flag();",
        "",
        "  @Converter",
        "  static class FlagMapper implements Supplier<Function<String, Boolean>> {",
        "    public Function<String, Boolean> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void validBooleanList() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Parameters(converter = BooleanMapper.class)",
        "  abstract List<Boolean> booleanList();",
        "",
        "  @Converter",
        "  static class BooleanMapper implements Supplier<Function<String, Boolean>> {",
        "    public Function<String, Boolean> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void parametersInvalidNotList() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Parameters(converter = MyConverter.class)",
        "  abstract Integer something();",
        "",
        "  @Converter",
        "  static class MyConverter implements Function<String, Integer> {",
        "    public Integer apply() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("use @Parameter here");
  }

  @Test
  void parametersInvalidNotListOptional() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Parameters(converter = MyConverter.class)",
        "  abstract Optional<Integer> something();",
        "",
        "  @Converter",
        "  static class MyConverter implements Function<String, Integer> {",
        "    public Integer apply() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("use @Parameter here");
  }

  @Test
  void parameterInvalidList() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Parameter(index = 0, converter = MyConverter.class)",
        "  abstract List<Integer> something();",
        "",
        "  @Converter",
        "  static class MyConverter implements Function<String, Integer> {",
        "    public Integer apply() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("use @Parameters here");
  }

  @Test
  void invalidBounds() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Parameter(index = 1, converter = BoundMapper.class)",
        "  abstract String a();",
        "",
        "  @Converter",
        "  static class BoundMapper<E extends Integer> implements Supplier<Function<E, E>> {",
        "    public Function<E, E> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("converter");
  }

  @Test
  void validBounds() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Parameter(index = 1, converter = BoundMapper.class)",
        "  abstract String a();",
        "",
        "  @Converter",
        "  static class BoundMapper implements Katz<String> {",
        "    public Function<String, String> get() { return null; }",
        "  }",
        "",
        "  interface Katz<OR> extends Supplier<Function<OR, OR>> { }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("converter should implement Function<String, ?> or Supplier<Function<String, ?>>");
  }

  @Test
  void converterInvalidPrivateConstructor() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract Integer number();",
        "",
        "  @Converter",
        "  static class MapMap implements Supplier<Function<String, Integer>> {",
        "",
        "    private MapMap() {}",
        "",
        "    public Function<String, Integer> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("constructor");
  }

  @Test
  void converterInvalidNoDefaultConstructor() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract Integer number();",
        "",
        "  @Converter",
        "  static class MapMap implements Supplier<Function<String, Integer>> {",
        "",
        "    MapMap(int i) {}",
        "",
        "    public Function<String, Integer> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("converter missing default constructor");
  }

  @Test
  void converterInvalidConstructorException() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract Integer number();",
        "",
        "  @Converter",
        "  static class MapMap implements Supplier<Function<String, Integer>> {",
        "",
        "    MapMap() throws IllegalStateException {}",
        "",
        "    public Function<String, Integer> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("converter missing default constructor");
  }

  @Test
  void converterInvalidNonstaticInnerClass() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract Integer number();",
        "",
        "  @Converter",
        "  class MapMap implements Supplier<Function<String, Integer>> {",
        "    public Function<String, Integer> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("converter must be static or top-level");
  }

  @Test
  void converterInvalidNotStringFunction() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract Integer number();",
        "",
        "  @Converter",
        "  static class MapMap implements Supplier<Function<Integer, Integer>> {",
        "    public Function<Integer, Integer> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("converter should implement Function<String, ?>");
  }

  @Test
  void converterInvalidReturnsString() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract Integer number();",
        "",
        "  @Converter",
        "  static class MapMap implements Supplier<Function<String, String>> {",
        "    public Function<String, String> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("converter should implement Function<String, Integer>");
  }

  @Test
  void converterInvalidReturnsStringOptional() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract java.util.OptionalInt number();",
        "",
        "  @Converter",
        "  static class MapMap implements Supplier<Function<String, String>> {",
        "    public Function<String, String> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("converter should implement Function<String, Integer>");
  }

  @Test
  void converterInvalidReturnsStringList() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract List<Integer> number();",
        "",
        "  @Converter",
        "  static class MapMap implements Supplier<Function<String, String>> {",
        "    public Function<String, String> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("converter should implement Function<String, Integer>");
  }

  @Test
  void rawType() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract java.util.Set<java.util.Set> things();",
        "",
        "  @Converter",
        "  static class MapMap implements Supplier<Function<String, java.util.Set<java.util.Set>>> {",
        "    public Function<String, java.util.Set<java.util.Set>> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void converterValidTypevars() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract Supplier<String> string();",
        "",
        "  @Converter",
        "  static class MapMap implements Supplier<Function<String, Supplier<String>>> {",
        "    public Function<String, Supplier<String>> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void converterValidNestedTypevars() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract Supplier<Optional<String>> string();",
        "",
        "  @Converter",
        "  static class MapMap implements Supplier<Function<String, Supplier<Optional<String>>>> {",
        "    public Function<String, Supplier<Optional<String>>> get() { return null; }",
        "  }",
        "}");

    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void converterValidExtendsFunction() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract Integer number();",
        "",
        "  @Converter",
        "  static class MapMap implements Supplier<StringFunction<String, Integer>> {",
        "    public StringFunction<String, Integer> get() { return null; }",
        "  }",
        "",
        "  interface StringFunction<V, X> extends Function<V, X> {}",
        "",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("converter should implement Function<String, ?> or Supplier<Function<String, ?>>");
  }

  @Test
  void converterInvalidStringFunction() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract Integer number();",
        "",
        "  @Converter",
        "  static class MapMap implements Supplier<StringFunction<Integer>> {",
        "    public StringFunction<Integer> get() { return null; }",
        "  }",
        "",
        "  interface StringFunction<R> extends Function<Long, R> {}",
        "",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("converter should implement Function<String, ?> or Supplier<Function<String, ?>>");
  }

  @Test
  void testConverterTypeSudokuInvalid() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract List<List<Integer>> number();",
        "",
        "  @Converter",
        "  static class MapMap<E extends List<List<Integer>>> implements FooSupplier<E> { public Foo<E> get() { return null; } }",
        "  interface FooSupplier<K> extends Supplier<Foo<K>> { }",
        "  interface Foo<X> extends Function<String, List<List<X>>> { }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("found type parameters in converter class declaration");
  }

  @Test
  void testSudokuHard() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract java.util.ArrayList<List<List<List<List<List<List<Set<Set<Set<Set<Set<Set<java.util.Collection<Integer>>>>>>>>>>>>>> numbers();",
        "",
        "  @Converter",
        "  static class MapMap implements Supplier<Function<String, java.util.ArrayList<List<List<List<List<List<List<Set<Set<Set<Set<Set<Set<java.util.Collection<Integer>>>>>>>>>>>>>>>> {",
        "    public Foo1<Set<Set<Set<Set<Set<Set<java.util.Collection<Integer>>>>>>>> get() { return null; }",
        "  }",
        "  interface Foo1<A> extends Foo2<List<A>> { }",
        "  interface Foo2<B> extends Foo3<List<B>> { }",
        "  interface Foo3<C> extends Foo4<List<C>> { }",
        "  interface Foo4<D> extends Foo5<List<D>> { }",
        "  interface Foo5<E> extends Foo6<List<E>> { }",
        "  interface Foo6<F> extends Foo7<List<F>> { }",
        "  interface Foo7<G> extends Foo8<java.util.ArrayList<G>> { }",
        "  interface Foo8<H> extends Function<String, H> { }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void converterInvalidRawFunctionSupplier() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract Integer number();",
        "",
        "  @Converter",
        "  static class MapMap implements Supplier<Function> {",
        "    public Function get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("raw type in converter class");
  }

  @Test
  void converterInvalidRawSupplier() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract Integer number();",
        "",
        "  @Converter",
        "  static class MapMap implements Supplier {",
        "    public Object get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("raw type in converter class");
  }

  @Test
  void converterInvalidRawFunction() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract Integer number();",
        "",
        "  @Converter",
        "  static class MapMap implements Function {",
        "    public Object apply(Object o) { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("raw type in converter class");
  }

  @Test
  void converterValid() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract List<java.util.OptionalInt> numbers();",
        "",
        "  @Converter",
        "  static class MapMap implements Supplier<Function<String, java.util.OptionalInt>> {",
        "    public Function<String, java.util.OptionalInt> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void converterValidByte() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract Byte number();",
        "",
        "  @Converter",
        "  static class MapMap implements Supplier<Function<String, Byte>> {",
        "    public Function<String, Byte> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void converterValidBytePrimitive() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract byte number();",
        "",
        "  @Converter",
        "  static class MapMap implements Supplier<Function<String, Byte>> {",
        "    public Function<String, Byte> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void converterValidOptionalInteger() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract Optional<Integer> number();",
        "",
        "  @Converter",
        "  static class MapMap implements Supplier<Function<String, Integer>> {",
        "    public Function<String, Integer> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void implicitMapperOptionalInt() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract java.util.OptionalInt b();",
        "",
        "  @Converter",
        "  static class MapMap implements Supplier<Function<String, Integer>> {",
        "    public Function<String, Integer> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void converterOptionalInt() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract java.util.OptionalInt b();",
        "",
        "  @Converter",
        "  static class MapMap implements Supplier<Function<String, java.util.OptionalInt>> {",
        "    public Function<String, java.util.OptionalInt> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void converterOptionalInteger() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract Optional<Integer> b();",
        "",
        "  @Converter",
        "  static class MapMap implements Supplier<Function<String, Optional<Integer>>> {",
        "    public Function<String, Optional<Integer>> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  void oneOptionalInt() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract java.util.OptionalInt b();",
        "",
        "  @Converter",
        "  static class MapMap implements Supplier<Function<String, Integer>> {",
        "    public Function<String, Integer> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }


  @Test
  void converterValidListOfSet() {
    JavaFileObject javaFile = fromSource(
        "@Command",
        "abstract class Arguments {",
        "",
        "  @Option(names = \"--x\", converter = MapMap.class)",
        "  abstract List<Set<Integer>> sets();",
        "",
        "  @Converter",
        "  static class MapMap implements Supplier<Function<String, Set<Integer>>> {",
        "    public Function<String, Set<Integer>> get() { return null; }",
        "  }",
        "}");
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }
}
