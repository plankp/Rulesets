/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.visitor;

import java.util.List;

import com.ymcmp.rset.ASMUtils;

import com.ymcmp.rset.tree.*;

import com.ymcmp.lexparse.tree.Visitor;
import com.ymcmp.lexparse.tree.ParseTree;

public abstract class BaseVisitor extends Visitor<Void> implements ASMUtils {

    public final Void visitKaryRule(final KaryRule r) {
        switch (r.type) {
            case SUBSCRIPT: visitRuleSubscript(r.rules); break;
            case JOIN:      visitRuleJoin(r.rules); break;
            case ARRAY:     visitRuleArray(r.rules); break;
            case CALL:      visitRuleCall(r.rules); break;
            case AND:       visitRuleAnd(r.rules); break;
            case OR:        visitRuleOr(r.rules); break;
            case ASSIGN:    visitRuleAssign(r.rules); break;
            case IGNORE:    visitRuleIgnore(r.rules); break;
            case SEQ:       visitRuleSeq(r.rules); break;
            case SWITCH:    visitRuleSwitch(r.rules); break;
            case GROUP:     visitRuleGroup(r.rules); break;
            default: return methodNotFound(r);
        }
        return null;
    }

    public void visitRuleSubscript(final List<ParseTree> rules) {
        throw new UnsupportedOperationException();
    }

    public void visitRuleJoin(final List<ParseTree> rules) {
        throw new UnsupportedOperationException();
    }

    public void visitRuleArray(final List<ParseTree> rules) {
        throw new UnsupportedOperationException();
    }

    public void visitRuleCall(final List<ParseTree> rules) {
        throw new UnsupportedOperationException();
    }

    public void visitRuleAnd(final List<ParseTree> rules) {
        throw new UnsupportedOperationException();
    }

    public void visitRuleOr(final List<ParseTree> rules) {
        throw new UnsupportedOperationException();
    }

    public void visitRuleAssign(final List<ParseTree> rules) {
        throw new UnsupportedOperationException();
    }

    public void visitRuleIgnore(final List<ParseTree> rules) {
        throw new UnsupportedOperationException();
    }

    public void visitRuleSeq(final List<ParseTree> rules) {
        throw new UnsupportedOperationException();
    }

    public void visitRuleSwitch(final List<ParseTree> rules) {
        throw new UnsupportedOperationException();
    }

    public void visitRuleGroup(final List<ParseTree> rules) {
        throw new UnsupportedOperationException();
    }
}