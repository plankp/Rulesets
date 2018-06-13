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
    'Yes: ' ~ (?_concat ?n)
}
```

Interfacing the rules with Java:

```java
final String generatedClassName = /* Something */;
try (final RsetLexer lexer = new RsetLexer(/* A Reader that contains the above code */)) {
    final RsetParser parser = new RsetParser(lexer);

    // The alternative is to write the bytes to a file
    // and save it as what ever class name you gave .class
    //
    // The output file is dependent on
    //   com.ymcmp.rset.rt.*
    //   com.ymcmp.rset.lib.*
    final byte[] bytes = Ruleset.toEvalMap(parser.parse().toBytecode(generatedClassName));

    final ByteClassLoader bcl = new ByteClassLoader();
    final Class<?> cl = bcl.loadFromBytes(generatedClassName, bytes);

    // cl implements Rulesets, so this cast is safe
    final Rulesets rulesets = (Rulesets) cl.getConstructor().newInstance();

    // In this example, we will be testing 0x1234
    final Object[] test = new Object[]{ "0", "x", "1", "2", "3", "4" };
    rulesets.forEachRule((name, rule) -> {
        final Object result = rule.apply(test);
        // name: name of the matching rule
        // result: result of { ... } after rule definition
    });
} catch (Exception ex) {
    // Handle these...
}
```