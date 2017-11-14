package net.zerobuilder.examples.gradle;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import net.zerobuilder.examples.gradle.GradleMan_Parser.Option;
import net.zerobuilder.examples.gradle.GradleMan_Parser.OptionType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public final class GradleManTest {

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void testShortLongConflict() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Conflicting token: --message=goodbye");
    GradleMan_Parser.parse(new String[]{"-m", "hello", "--message=goodbye"});
  }

  @Test
  public void testMissingValue() {
    // there's nothing after -m
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Missing value: -m");
    GradleMan gradleMan = GradleMan_Parser.parse(new String[]{"-m"});
    //gradleMan.message();
  }

  @Test
  public void testLongShortConflict() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Conflicting token: -m");
    GradleMan_Parser.parse(new String[]{"--message=hello", "-m", "goodbye"});
  }

  @Test
  public void testLongLongConflict() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Conflicting token: --message=goodbye");
    GradleMan_Parser.parse(new String[]{"--message=hello", "--message=goodbye"});
  }

  @Test
  public void testDetachedLong() {
    GradleMan gradleMan = GradleMan_Parser.parse(
        new String[]{"--message", "hello"});
    assertThat(gradleMan.message(), is(Optional.of("hello")));
  }

  @Test
  public void testNull() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("null token");
    GradleMan_Parser.parse(new String[]{null});
  }

  @Test
  public void testInterestingTokens() {
    GradleMan gradleMan = GradleMan_Parser.parse(
        new String[]{"--message=hello", "-", "--", "->", "<=>", "", " "});
    assertThat(gradleMan.message(), is(Optional.of("hello")));
    assertThat(gradleMan.otherTokens().size(), is(6));
    assertThat(gradleMan.otherTokens().get(0), is("-"));
    assertThat(gradleMan.otherTokens().get(1), is("--"));
    assertThat(gradleMan.otherTokens().get(2), is("->"));
    assertThat(gradleMan.otherTokens().get(3), is("<=>"));
    assertThat(gradleMan.otherTokens().get(4), is(""));
    assertThat(gradleMan.otherTokens().get(5), is(" "));
  }

  @Test
  public void testEmptyVersusAbsent() {
    assertThat(GradleMan_Parser.parse(new String[]{"--message="}).message(), is(Optional.of("")));
    assertThat(GradleMan_Parser.parse(new String[0]).message(), is(Optional.empty()));
  }

  @Test
  public void testShortNonAtomic() {
    String[] args = {"-m", "hello"};
    GradleMan gradleMan = GradleMan_Parser.parse(args);
    assertThat(gradleMan.message(), is(Optional.of("hello")));
    assertThat(gradleMan.cmos(), is(false));
  }

  @Test
  public void testLongMessage() {
    GradleMan gradleMan = GradleMan_Parser.parse(new String[]{"--message=hello"});
    assertThat(gradleMan.message(), is(Optional.of("hello")));
    assertThat(gradleMan.cmos(), is(false));
  }

  @Test
  public void testShortAtomic() {
    GradleMan gradleMan = GradleMan_Parser.parse(new String[]{"-fbar.txt"});
    assertThat(gradleMan.file().size(), is(1));
    assertThat(gradleMan.file().get(0), is("bar.txt"));
  }

  @Test
  public void testLongShortAtomic() {
    GradleMan gradleMan = GradleMan_Parser.parse(new String[]{"--message=hello", "-fbar.txt"});
    assertThat(gradleMan.file().size(), is(1));
    assertThat(gradleMan.file().get(0), is("bar.txt"));
    assertThat(gradleMan.message(), is(Optional.of("hello")));
  }

  @Test
  public void testLongInvalid() {
    // --file is not declared
    GradleMan gradleMan = GradleMan_Parser.parse(new String[]{"--file=file"});
    assertThat(gradleMan.file().size(), is(0));
    assertThat(gradleMan.otherTokens().size(), is(1));
    assertThat(gradleMan.otherTokens().get(0), is("--file=file"));
  }

  @Test
  public void testLong() {
    GradleMan gradleMan = GradleMan_Parser.parse(new String[]{"--dir=dir"});
    assertThat(gradleMan.dir(), is(Optional.of("dir")));
  }

  @Test
  public void testFlag() {
    // -c is a flag; last token goes in the trash
    GradleMan gradleMan = GradleMan_Parser.parse(new String[]{"-c", "hello"});
    assertThat(gradleMan.cmos(), is(true));
  }

  @Test
  public void testNonsense() {
    // bogus options
    GradleMan gradleMan = GradleMan_Parser.parse(new String[]{"hello", "goodbye"});
    assertThat(gradleMan.otherTokens().size(), is(2));
  }

  @Test
  public void testFlagWithTrailingGarbage() {
    exception.expect(IllegalArgumentException.class);
    GradleMan_Parser.parse(new String[]{"-c1"});
  }

  @Test
  public void testDoubleFlag() {
    GradleMan gradleMan = GradleMan_Parser.parse(new String[]{"-cv"});
    assertThat(gradleMan.cmos(), is(true));
    assertThat(gradleMan.verbose(), is(true));
  }

  @Test
  public void testDoubleFlagWithAttachedOption() {
    GradleMan gradleMan = GradleMan_Parser.parse(new String[]{"-cvmhello"});
    assertThat(gradleMan.cmos(), is(true));
    assertThat(gradleMan.verbose(), is(true));
    assertThat(gradleMan.message(), is(Optional.of("hello")));
  }

  @Test
  public void testDoubleFlagWithDetachedOption() {
    GradleMan gradleMan = GradleMan_Parser.parse(new String[]{"-cvm", "hello"});
    assertThat(gradleMan.cmos(), is(true));
    assertThat(gradleMan.verbose(), is(true));
    assertThat(gradleMan.message(), is(Optional.of("hello")));
  }

  @Test
  public void testOptions() {
    Option[] options = Option.values();
    assertThat(options.length, is(6));
    assertThat(Arrays.stream(options)
            .filter(o -> o.type() != OptionType.FLAG)
            .map(Option::longName)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet()),
        is(new HashSet<>(asList("otherTokens", "message", "dir"))));
    assertThat(Arrays.stream(options)
            .filter(o -> o.type() != OptionType.FLAG)
            .map(Option::shortName)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet()),
        is(new HashSet<>(asList("f", "m"))));
    assertThat(Arrays.stream(options)
            .filter(o -> o.type() == OptionType.FLAG)
            .map(Option::shortName)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet()),
        is(new HashSet<>(asList("c", "v"))));
  }

  @Test
  public void testMessageOption() {
    assertThat(Option.MESSAGE.description().size(), is(2));
    assertThat(Option.MESSAGE.description().get(0), is("the message"));
    assertThat(Option.MESSAGE.description().get(1), is("message goes here"));
    assertThat(Option.MESSAGE.type(), is(OptionType.OPTIONAL));
    assertThat(Option.MESSAGE.longName(), is("message"));
    assertThat(Option.MESSAGE.shortName(), is("m"));
    assertThat(Option.MESSAGE.descriptionParameter(), is("MESSAGE"));
  }

  @Test
  public void testCmosOption() {
    assertThat(Option.CMOS.description().size(), is(1));
    assertThat(Option.CMOS.description().get(0), is("cmos flag"));
    assertThat(Option.CMOS.type(), is(OptionType.FLAG));
    assertThat(Option.CMOS.longName(), is(nullValue()));
    assertThat(Option.CMOS.shortName(), is("c"));
    assertThat(Option.CMOS.descriptionParameter(), is(nullValue()));
  }

  @Test
  public void testParserForNestedClass() {
    GradleMan.Foo foo = GradleMan_Foo_Parser.parse(new String[]{"--bar=4"});
    assertThat(foo.bar(), is(Optional.of("4")));
  }

  @Test
  public void testPrint() {
    Arrays.stream(Option.values())
        .map(o -> o.describe(4))
        .forEach(System.out::println);
  }
}
