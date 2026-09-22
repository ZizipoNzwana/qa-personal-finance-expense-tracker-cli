package com.financetracker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@DisplayName("Main.tokenize: interactive REPL command line parsing")
class MainTokenizeTest {

    @Test
    @DisplayName("splits a simple space-separated command")
    void splitsSimpleCommand() {
        assertArrayEquals(
                new String[]{"add-expense", "--amount", "50", "--category", "food"},
                Main.tokenize("add-expense --amount 50 --category food"));
    }

    @Test
    @DisplayName("keeps a double-quoted description as a single token")
    void keepsDoubleQuotedTokenTogether() {
        assertArrayEquals(
                new String[]{"add-expense", "--amount", "12", "--description", "coffee with a friend"},
                Main.tokenize("add-expense --amount 12 --description \"coffee with a friend\""));
    }

    @Test
    @DisplayName("keeps a single-quoted description as a single token")
    void keepsSingleQuotedTokenTogether() {
        assertArrayEquals(
                new String[]{"--description", "team lunch"},
                Main.tokenize("--description 'team lunch'"));
    }

    @Test
    @DisplayName("collapses repeated whitespace")
    void collapsesRepeatedWhitespace() {
        assertArrayEquals(
                new String[]{"list"},
                Main.tokenize("   list   "));
    }
}
