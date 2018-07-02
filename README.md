# Rulesets [![Build Status](https://travis-ci.org/plankp/Rulesets.svg?branch=master)](https://travis-ci.org/plankp/Rulesets) [![codecov](https://codecov.io/gh/plankp/Rulesets/branch/master/graph/badge.svg)](https://codecov.io/gh/plankp/Rulesets) [![Maintainability](https://api.codeclimate.com/v1/badges/f0611bc0867b632690d6/maintainability)](https://codeclimate.com/github/plankp/Rulesets/maintainability)

Yes, it is called _**Rulesets**_, not _**Ruleset**_

## Build Instructions

Install jdk 8 or higher, Use `gradlew`

## What is this?

It is similar to a lexer and parser generator in many ways, except you also
use it to match an sequence of (or singular) basic Java objects. You can check
if any object responds to a selector (has field or method with the specified
name). Checking if the object is subclass or parent class is also allowed. The objects that support equality-based matches are limited
to:

*  Null
*  Integers
*  Doubles
*  Characters / List of Characters
*  Strings
*  Object Array / Collection
*  Epsilon, a made up object detonating the end of data sequence

In addition, Integer, Doubles, Characters and Strings support relative comparisons.

Essentially, this is a DSL that matches data in a regex-like syntax

## How do you use this?

You could use the compiler provided by the program, or interface this project as a library:

#### Invoking compiler:

After you built the project and copied out the `.jar` files and the `.bat` or shell script files:

```
% rulesets -h
Rulesets compiler:
  compiler [options] [ruleset file]
options:
  -h | --help              Prints this message
  -n | --class-name <name> Changes generated class name
                           default: 'CompiledRulesets'
  -d | --directory <name>  Places generated file in specifed directory
                           default: current directory
  -l | --with-logger       Generated class has logging built-in
                           default: without logger
If no ruleset file is specified, the compiler will read from stdin:
  cat foo.rset bar.rset | compiler -n demo/JointRules -d demo

Link to copyright notices of used libraries:
  https://github.com/plankp/Rulesets/blob/master/COPYING
```

#### Interfacing:

Rulesets:

```
# Matching numbers (an over-simplified lexer)

fragment digit
  = %0-%9+
,

fragment alhex
  = (&digit | %a-%f | %A-%F)+
,

rule number
  = n:(%0 (%x &alhex)? | %1-%9 &digit?)
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
    // and save it as with class name you gave .class
    //
    // The output file is dependent on
    //   com.ymcmp.rset.rt.*
    //   com.ymcmp.rset.lib.*
    final byte[] bytes = Ruleset.toEvalMap(parser.parse().toBytecode(generatedClassName));

    final ByteClassLoader bcl = new ByteClassLoader();
    final Class<?> cl = bcl.loadFromBytes(generatedClassName, bytes);

    // cl implements Rulesets, so this cast is safe
    // You could use cl.getConstructor(Extensions.class).newInstance(ext) for loading custom extensions
    final Rulesets rulesets = (Rulesets) cl.getConstructor().newInstance();

    // In this example, we will be testing 0x1234
    final Object[] test = new Object[]{ '0', 'x', '1', '2', '3', '4' };
    rulesets.forEachRule((name, rule) -> {
        final Object result = rule.apply(test);
        // name: name of the matching rule
        // result: result of { ... } after rule definition
    });
} catch (Exception ex) {
    // Handle these...
}
```