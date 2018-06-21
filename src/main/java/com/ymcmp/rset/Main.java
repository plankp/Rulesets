/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset;

import java.io.Reader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.nio.file.Paths;
import java.nio.file.Files;

import com.ymcmp.rset.tree.RulesetGroup;

public class Main {

    private static final String DEFAULT_CLASS_NAME = "CompiledRulesets";

    public static void main(final String[] args) throws IOException {
        String generatedClassName = DEFAULT_CLASS_NAME;
        String generatedPathName  = ".";
        String inputFile = null;
        boolean genWithLogger = false;
        for (int i = 0; i < args.length; ++i) {
            final String arg = args[i];
            switch (arg) {
                case "-h":
                case "--help":
                    System.out.println(
                            "Rulesets compiler:\n" +
                            "  compiler [options] [ruleset file]" +
                            "options:\n" +
                            "  -h | --help              Prints this message\n" +
                            "  -n | --class-name <name> Changes generated class name\n" +
                            "                           default: 'CompiledRulesets'\n" +
                            "  -d | --directory <name>  Places generated file in specifed directory\n" +
                            "                           default: current directory\n" +
                            "  -l | --with-logger       Generated class has logging built-in\n" +
                            "                           default: without logger\n" +
                            "If no ruleset file is specified, the compiler will read from stdin:\n" +
                            "  cat foo.rset bar.rset | compiler -n demo/JointRules -d demo\n" +
                            "\n" +
                            "Link to copyright notices of used libraries:\n" +
                            "  https://github.com/plankp/Rulesets/blob/master/COPYING");
                    break;
                case "-n":
                case "--class-name":
                    // User will provide something like com/ymcmp/Crules
                    // Change generatedClassName here
                    generatedClassName = args[++i].replace('.', '/');
                    break;
                case "-d":
                case "--directory":
                    // This only affects where the compiled file is placed
                    // I guess technically you could specify -d /dev/null !?
                    generatedPathName = args[++i];
                    break;
                case "-l":
                case "--with-logger":
                    genWithLogger = true;
                    break;
                default:
                    inputFile = arg;
                    break;
            }
        }

        try {
            final Reader reader = inputFile == null
                    ? new InputStreamReader(System.in)
                    : Files.newBufferedReader(Paths.get(inputFile));
            try (final RsetLexer lexer = new RsetLexer(reader)) {
                final RsetParser parser = new RsetParser(lexer);
                final RulesetGroup tree = parser.parse();

                final String[] arr = generatedClassName.split(".*/", 2);
                String fileName = DEFAULT_CLASS_NAME;
                switch (arr.length) {
                    case 1: fileName = arr[0]; break;
                    case 2: fileName = arr[1]; break;
                    default:
                        System.out.println("Unreadable class name, using default name instead");
                        generatedClassName = DEFAULT_CLASS_NAME;
                        break;
                }

                final byte[] bytes = tree.toBytecode(generatedClassName, inputFile, genWithLogger);

                // Create directories
                Files.write(Files.createDirectories(Paths.get(generatedPathName)).resolve(fileName + ".class"), bytes);
            }
        } catch (IOException ex) {
            throw ex;
        }
    }
}