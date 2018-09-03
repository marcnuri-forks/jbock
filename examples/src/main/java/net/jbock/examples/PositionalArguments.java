package net.jbock.examples;

import java.util.List;
import java.util.Optional;
import net.jbock.CommandLineArguments;
import net.jbock.Positional;

@CommandLineArguments(allowEscape = true)
abstract class PositionalArguments {

  @Positional
  abstract String source();

  @Positional
  abstract String dest();

  @Positional
  abstract Optional<String> optString();

  @Positional
  abstract List<String> otherTokens();
}
