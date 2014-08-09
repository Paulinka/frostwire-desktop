/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.search.extractors.js;

import static com.frostwire.search.extractors.js.JavaFunctions.isalpha;
import static com.frostwire.search.extractors.js.JavaFunctions.isdigit;
import static com.frostwire.search.extractors.js.JavaFunctions.join;
import static com.frostwire.search.extractors.js.JavaFunctions.len;
import static com.frostwire.search.extractors.js.JavaFunctions.list;
import static com.frostwire.search.extractors.js.JavaFunctions.reverse;
import static com.frostwire.search.extractors.js.JavaFunctions.slice;
import static com.frostwire.search.extractors.js.JavaFunctions.escape;
import static com.frostwire.search.extractors.js.JavaFunctions.mscpy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class JsFunction<T> {

    private final JsContext ctx;
    private final LambdaN initial_function;

    public JsFunction(String jscode, String funcname) {
        this.ctx = new JsContext(jscode);
        this.initial_function = extract_function(ctx, funcname);
    }

    @SuppressWarnings("unchecked")
    public T eval(Object[] args) {
        try {
            return (T) initial_function.eval(args);
        } finally {
            // at this point we know that jscode is no longer necessary
            ctx.free();
        }
    }

    public T eval(Object s) {
        return eval(new Object[] { s });
    }

    private static Object interpret_statement(final JsContext ctx, String stmt, final Map<String, Object> local_vars, final int allow_recursion) {
        if (allow_recursion < 0) {
            throw new JsError("Recursion limit reached");
        }

        if (stmt.startsWith("var ")) {
            stmt = stmt.substring("var ".length());
        }

        final Matcher ass_m = Pattern.compile("^(?<out>[a-z]+)(\\[(?<index>.+?)\\])?=(?<expr>.*)$").matcher(stmt);
        Lambda1 assign;
        String expr;
        if (ass_m.find()) {
            if (ass_m.group("index") != null) {

                final Object lvar = local_vars.get(ass_m.group("out"));
                final Object idx = interpret_expression(ctx, ass_m.group("index"), local_vars, allow_recursion);
                assert idx instanceof Integer;

                assign = new Lambda1() {
                    @Override
                    public Object eval(Object val) {
                        ((Object[]) lvar)[(Integer) idx] = val;
                        return val;
                    }
                };
                expr = ass_m.group("expr");
            } else {

                final String var = ass_m.group("out");

                assign = new Lambda1() {
                    @Override
                    public Object eval(Object val) {
                        local_vars.put(var, val);
                        return val;
                    }
                };
                expr = ass_m.group("expr");
            }
        } else if (stmt.startsWith("return ")) {
            assign = new Lambda1() {
                @Override
                public Object eval(Object v) {
                    return v;
                }
            };
            expr = stmt.substring("return ".length());
        } else {
            throw new JsError(String.format("Cannot determine left side of statement in %s", stmt));
        }

        Object v = interpret_expression(ctx, expr, local_vars, allow_recursion);
        return assign.eval(v);
    }

    private static Object interpret_expression(final JsContext ctx, String expr, Map<String, Object> local_vars, int allow_recursion) {
        if (isdigit(expr)) {
            return Integer.valueOf(expr);
        }

        if (isalpha(expr)) {
            return local_vars.get(expr);
        }

        Matcher m = Pattern.compile("^(?<in>[a-z]+)\\.(?<member>.*)$").matcher(expr);
        if (m.find()) {
            String member = m.group("member");
            Object val = local_vars.get(m.group("in"));
            if (member.equals("split(\"\")")) {
                return list((String) val);
            }
            if (member.equals("join(\"\")")) {
                return join((Object[]) val);
            }
            if (member.equals("length")) {
                return len(val);
            }
            if (member.equals("reverse()")) {
                return reverse(val);
            }
            Matcher slice_m = Pattern.compile("slice\\((?<idx>.*)\\)").matcher(member);
            if (slice_m.find()) {
                Object idx = interpret_expression(ctx, slice_m.group("idx"), local_vars, allow_recursion - 1);
                return slice(val, (Integer) idx);
            }
        }

        m = Pattern.compile("^(?<in>[a-z]+)\\[(?<idx>.+)\\]$").matcher(expr);
        if (m.find()) {
            Object val = local_vars.get(m.group("in"));
            Object idx = interpret_expression(ctx, m.group("idx"), local_vars, allow_recursion - 1);
            return ((Object[]) val)[(Integer) idx];
        }

        m = Pattern.compile("^(?<a>.+?)(?<op>[%])(?<b>.+?)$").matcher(expr);
        if (m.find()) {
            Object a = interpret_expression(ctx, m.group("a"), local_vars, allow_recursion);
            Object b = interpret_expression(ctx, m.group("b"), local_vars, allow_recursion);
            return (Integer) a % (Integer) b;
        }

        m = Pattern.compile("^(?<func>[a-zA-Z]+)\\((?<args>[a-z0-9,]+)\\)$").matcher(expr);
        if (m.find()) {
            String fname = m.group("func");
            if (!ctx.functions.containsKey(fname) && ctx.jscode.length() > 0) {
                ctx.functions.put(fname, extract_function(ctx, fname));
            }
            List<Object> argvals = new ArrayList<Object>();
            for (String v : m.group("args").split(",")) {
                if (isdigit(v)) {
                    argvals.add(Integer.valueOf(v));
                } else {
                    argvals.add(local_vars.get(v));
                }
            }
            return ctx.functions.get(fname).eval(argvals.toArray());
        }
        throw new JsError(String.format("Unsupported JS expression %s", expr));
    }

    private static LambdaN extract_function(final JsContext ctx, String funcname) {
        String func_mRegex = String.format("(function %1$s|[\\{;]%1$s\\p{Space}*=\\p{Space}*function)", escape(funcname)) + "\\((?<args>[a-z,]+)\\)\\{(?<code>[^\\}]+)\\}";
        final Matcher func_m = Pattern.compile(func_mRegex).matcher(ctx.jscode);
        if (!func_m.find()) {
            throw new JsError("Could not find JS function " + funcname);
        }

        final String[] argnames = mscpy(func_m.group("args").split(","));

        return build_function(ctx, argnames, func_m.group("code"));
    }

    private static LambdaN build_function(final JsContext ctx, final String[] argnames, String code) {
        final String[] stmts = mscpy(code.split(";"));

        return new LambdaN() {
            @Override
            public Object eval(Object[] args) {
                Map<String, Object> local_vars = new HashMap<String, Object>();
                for (int i = 0; i < argnames.length; i++) {
                    local_vars.put(argnames[i], args[i]);
                }
                Object res = null;
                for (String stmt : stmts) {
                    res = interpret_statement(ctx, stmt, local_vars, 20);
                }
                return res;
            }
        };
    }
}
