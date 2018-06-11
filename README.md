# Rulesets

Yes, it is called _**Rulesets**_, not _**Ruleset**_

## Build Instructions

Install jdk 8 or higher, Use `gradlew`

## What is this?

Rulesets:

```
# Matching numbers (an over-simplified lexer)

rule digit
  = '0'-'9'+

rule alhex
  = (&digit | a-f | A-F)+

rule number
  = n:('0' (x &alhex)? | '1'-'9' &digit?)
{
    'Yes: ' ~ (*_concat *n)
}
```

Interfacing the rules with Java:

```java
try (final RsetLexer lexer = new RsetLexer(/* A Reader that contains the above code */)) {
    final RsetParser parser = new RsetParser(lexer);
    final Map<String, Ruleset> env = Ruleset.toEvalMap(parser.parse().toRulesetStream());
    final Extensions ext = new Extensions();

    // Replace with your own sequence of data, using 0x1234 as example
    Ruleset.evaluate(env, ext, "0", "x", "1", "2", "3", "4").forEach((name, opt) -> {
        opt.ifPresent(result -> {
            // name: name of the matching rule
            // result: result of { ... } after rule definition
            //
            // result is never null, uses java.util.Optional
        });
    });
} catch (java.io.IOException ex) {
    // Handle this exception
}
```