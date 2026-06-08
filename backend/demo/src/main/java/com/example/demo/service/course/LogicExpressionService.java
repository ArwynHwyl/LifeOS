package com.example.demo.service.course;

import com.example.demo.service.exception.ValidationException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class LogicExpressionService {

    private static final int MAX_EQUIVALENCE_VARIABLES = 8;

    public Node parse(String source) {
        if (source == null || source.isBlank()) {
            throw new ValidationException("logic expression is required");
        }
        Parser parser = new Parser(tokenize(source));
        Node node = parser.parse();
        if (parser.hasRemaining()) {
            throw new ValidationException("logic expression contains trailing tokens");
        }
        return node;
    }

    public boolean evaluate(Node node, Map<String, Boolean> env) {
        if (node instanceof ConstNode constant) {
            return constant.value();
        }
        if (node instanceof VarNode variable) {
            return Boolean.TRUE.equals(env.get(variable.name()));
        }
        if (node instanceof NotNode not) {
            return !evaluate(not.value(), env);
        }
        BinNode bin = (BinNode) node;
        boolean left = evaluate(bin.left(), env);
        boolean right = evaluate(bin.right(), env);
        return switch (bin.op()) {
            case AND -> left && right;
            case OR -> left || right;
            case XOR -> left != right;
            case IMP -> !left || right;
            case IFF -> left == right;
        };
    }

    public Set<String> variables(Node node) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        collectVariables(node, names);
        return names;
    }

    public boolean equivalent(Node left, Node right) {
        List<String> names = new ArrayList<>(variables(left));
        for (String name : variables(right)) {
            if (!names.contains(name)) {
                names.add(name);
            }
        }
        names.sort(String::compareTo);
        if (names.size() > MAX_EQUIVALENCE_VARIABLES) {
            throw new ValidationException("logic expression can contain at most " + MAX_EQUIVALENCE_VARIABLES + " variables");
        }
        int rowCount = 1 << names.size();
        for (int mask = 0; mask < rowCount; mask++) {
            java.util.HashMap<String, Boolean> env = new java.util.HashMap<>();
            for (int index = 0; index < names.size(); index++) {
                env.put(names.get(index), (mask & (1 << (names.size() - 1 - index))) != 0);
            }
            if (evaluate(left, env) != evaluate(right, env)) {
                return false;
            }
        }
        return true;
    }

    public String normalize(Node node) {
        if (node instanceof ConstNode constant) {
            return constant.value() ? "T" : "F";
        }
        if (node instanceof VarNode variable) {
            return variable.name();
        }
        if (node instanceof NotNode not) {
            String inner = normalize(not.value());
            return "NOT".equals(not.value().precedenceName()) || "ATOM".equals(not.value().precedenceName())
                    ? "¬" + inner
                    : "¬(" + inner + ")";
        }
        BinNode bin = (BinNode) node;
        String left = wrap(bin.left(), bin.op().precedence(), false);
        String right = wrap(bin.right(), bin.op().precedence(), bin.op() == BinOp.IMP);
        return left + " " + bin.op().glyph() + " " + right;
    }

    private String wrap(Node node, int parentPrecedence, boolean rightImp) {
        String normalized = normalize(node);
        if (node.precedence() < parentPrecedence || (rightImp && node.precedence() == parentPrecedence)) {
            return "(" + normalized + ")";
        }
        return normalized;
    }

    private void collectVariables(Node node, Set<String> names) {
        if (node instanceof VarNode variable) {
            names.add(variable.name());
        } else if (node instanceof NotNode not) {
            collectVariables(not.value(), names);
        } else if (node instanceof BinNode bin) {
            collectVariables(bin.left(), names);
            collectVariables(bin.right(), names);
        }
    }

    private List<Token> tokenize(String source) {
        List<Token> tokens = new ArrayList<>();
        int index = 0;
        while (index < source.length()) {
            char current = source.charAt(index);
            if (Character.isWhitespace(current)) {
                index++;
                continue;
            }
            if (source.startsWith("<->", index)) {
                tokens.add(new Token(TokenKind.IFF, null));
                index += 3;
                continue;
            }
            if (source.startsWith("->", index)) {
                tokens.add(new Token(TokenKind.IMP, null));
                index += 2;
                continue;
            }
            switch (current) {
                case '↔' -> tokens.add(new Token(TokenKind.IFF, null));
                case '→' -> tokens.add(new Token(TokenKind.IMP, null));
                case '¬', '!', '~' -> tokens.add(new Token(TokenKind.NOT, null));
                case '∧', '&', '*', '.' -> tokens.add(new Token(TokenKind.AND, null));
                case '∨', '|', '+' -> tokens.add(new Token(TokenKind.OR, null));
                case '⊕', '^' -> tokens.add(new Token(TokenKind.XOR, null));
                case '(' -> tokens.add(new Token(TokenKind.LP, null));
                case ')' -> tokens.add(new Token(TokenKind.RP, null));
                case 'T', '1', '⊤' -> tokens.add(new Token(TokenKind.CONST, "true"));
                case 'F', '0', '⊥' -> tokens.add(new Token(TokenKind.CONST, "false"));
                default -> {
                    if (current >= 'A' && current <= 'Z' || current >= 'a' && current <= 'z') {
                        tokens.add(new Token(TokenKind.VAR, String.valueOf(Character.toUpperCase(current))));
                    } else {
                        throw new ValidationException("logic expression contains unsupported character: " + current);
                    }
                }
            }
            index++;
        }
        return tokens;
    }

    public sealed interface Node permits ConstNode, VarNode, NotNode, BinNode {
        int precedence();

        default String precedenceName() {
            return precedence() == 7 ? "ATOM" : precedence() == 6 ? "NOT" : "BIN";
        }
    }

    public record ConstNode(boolean value) implements Node {
        @Override
        public int precedence() {
            return 7;
        }
    }

    public record VarNode(String name) implements Node {
        @Override
        public int precedence() {
            return 7;
        }
    }

    public record NotNode(Node value) implements Node {
        @Override
        public int precedence() {
            return 6;
        }
    }

    public record BinNode(BinOp op, Node left, Node right) implements Node {
        @Override
        public int precedence() {
            return op.precedence();
        }
    }

    public enum BinOp {
        IFF(1, "↔"),
        IMP(2, "→"),
        OR(3, "∨"),
        XOR(4, "⊕"),
        AND(5, "∧");

        private final int precedence;
        private final String glyph;

        BinOp(int precedence, String glyph) {
            this.precedence = precedence;
            this.glyph = glyph;
        }

        int precedence() {
            return precedence;
        }

        String glyph() {
            return glyph;
        }
    }

    private enum TokenKind {
        VAR, CONST, NOT, AND, OR, XOR, IMP, IFF, LP, RP
    }

    private record Token(TokenKind kind, String value) {
    }

    private static class Parser {
        private final List<Token> tokens;
        private int position;

        Parser(List<Token> tokens) {
            this.tokens = tokens;
        }

        Node parse() {
            return iff();
        }

        boolean hasRemaining() {
            return position < tokens.size();
        }

        private Token peek() {
            return position < tokens.size() ? tokens.get(position) : null;
        }

        private Token eat(TokenKind kind) {
            Token token = peek();
            if (token == null || token.kind() != kind) {
                throw new ValidationException("logic expression expected " + kind);
            }
            position++;
            return token;
        }

        private Node atom() {
            Token token = peek();
            if (token == null) {
                throw new ValidationException("logic expression ended unexpectedly");
            }
            return switch (token.kind()) {
                case LP -> {
                    position++;
                    Node node = iff();
                    eat(TokenKind.RP);
                    yield node;
                }
                case NOT -> {
                    position++;
                    yield new NotNode(atom());
                }
                case VAR -> {
                    position++;
                    yield new VarNode(token.value());
                }
                case CONST -> {
                    position++;
                    yield new ConstNode(Boolean.parseBoolean(token.value()));
                }
                default -> throw new ValidationException("logic expression contains unexpected token: " + token.kind());
            };
        }

        private Node and() {
            Node left = atom();
            while (peek() != null && peek().kind() == TokenKind.AND) {
                position++;
                left = new BinNode(BinOp.AND, left, atom());
            }
            return left;
        }

        private Node xor() {
            Node left = and();
            while (peek() != null && peek().kind() == TokenKind.XOR) {
                position++;
                left = new BinNode(BinOp.XOR, left, and());
            }
            return left;
        }

        private Node or() {
            Node left = xor();
            while (peek() != null && peek().kind() == TokenKind.OR) {
                position++;
                left = new BinNode(BinOp.OR, left, xor());
            }
            return left;
        }

        private Node imp() {
            Node left = or();
            if (peek() != null && peek().kind() == TokenKind.IMP) {
                position++;
                return new BinNode(BinOp.IMP, left, imp());
            }
            return left;
        }

        private Node iff() {
            Node left = imp();
            while (peek() != null && peek().kind() == TokenKind.IFF) {
                position++;
                left = new BinNode(BinOp.IFF, left, imp());
            }
            return left;
        }
    }
}
