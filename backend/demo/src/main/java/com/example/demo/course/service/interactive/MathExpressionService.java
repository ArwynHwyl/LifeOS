package com.example.demo.course.service.interactive;

import com.example.demo.shared.exception.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MathExpressionService {

    public double evaluate(String expression, Map<String, Double> variables) {
        Parser parser = new Parser(tokenize(expression), variables == null ? Map.of() : variables);
        double value = parser.expression();
        if (parser.hasRemaining()) {
            throw new ValidationException("expression contains trailing tokens");
        }
        if (!Double.isFinite(value)) {
            throw new ValidationException("expression result is invalid");
        }
        return value;
    }

    private List<Token> tokenize(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new ValidationException("expression is required");
        }
        List<Token> tokens = new ArrayList<>();
        int index = 0;
        while (index < expression.length()) {
            char current = expression.charAt(index);
            if (Character.isWhitespace(current)) {
                index++;
                continue;
            }
            if (Character.isDigit(current) || current == '.') {
                int end = index + 1;
                while (end < expression.length() && (Character.isDigit(expression.charAt(end)) || expression.charAt(end) == '.')) {
                    end++;
                }
                try {
                    tokens.add(new Token(TokenKind.NUMBER, expression.substring(index, end), Double.parseDouble(expression.substring(index, end))));
                } catch (NumberFormatException ex) {
                    throw new ValidationException("expression contains invalid number");
                }
                index = end;
                continue;
            }
            if (Character.isLetter(current) || current == '_') {
                int end = index + 1;
                while (end < expression.length() && (Character.isLetterOrDigit(expression.charAt(end)) || expression.charAt(end) == '_')) {
                    end++;
                }
                tokens.add(new Token(TokenKind.IDENT, expression.substring(index, end), 0));
                index = end;
                continue;
            }
            if ("+-*/^(),".indexOf(current) >= 0) {
                tokens.add(new Token(TokenKind.SYMBOL, String.valueOf(current), 0));
                index++;
                continue;
            }
            throw new ValidationException("expression contains unsupported character");
        }
        return tokens;
    }

    private enum TokenKind {
        NUMBER, IDENT, SYMBOL
    }

    private record Token(TokenKind kind, String text, double number) {
    }

    private static class Parser {
        private final List<Token> tokens;
        private final Map<String, Double> variables;
        private int position;

        Parser(List<Token> tokens, Map<String, Double> variables) {
            this.tokens = tokens;
            this.variables = variables;
        }

        boolean hasRemaining() {
            return position < tokens.size();
        }

        double expression() {
            double value = term();
            while (match("+") || match("-")) {
                String op = previous().text();
                double right = term();
                value = "+".equals(op) ? value + right : value - right;
            }
            return value;
        }

        double term() {
            double value = power();
            while (match("*") || match("/")) {
                String op = previous().text();
                double right = power();
                value = "*".equals(op) ? value * right : value / right;
            }
            return value;
        }

        double power() {
            double value = unary();
            if (match("^")) {
                value = Math.pow(value, power());
            }
            return value;
        }

        double unary() {
            if (match("-")) {
                return -unary();
            }
            return primary();
        }

        double primary() {
            Token token = peek();
            if (token == null) {
                throw new ValidationException("expression ended unexpectedly");
            }
            if (token.kind() == TokenKind.NUMBER) {
                position++;
                return token.number();
            }
            if (match("(")) {
                double value = expression();
                consume(")");
                return value;
            }
            if (token.kind() == TokenKind.IDENT) {
                position++;
                String name = token.text();
                if (match("(")) {
                    List<Double> args = new ArrayList<>();
                    if (!check(")")) {
                        do {
                            args.add(expression());
                        } while (match(","));
                    }
                    consume(")");
                    return call(name, args);
                }
                if ("pi".equals(name)) return Math.PI;
                if ("e".equals(name)) return Math.E;
                Double value = variables.get(name);
                if (value == null) {
                    throw new ValidationException("missing expression variable: " + name);
                }
                return value;
            }
            throw new ValidationException("expression contains unexpected token");
        }

        private double call(String name, List<Double> args) {
            return switch (name) {
                case "sin" -> one(name, args, Math::sin);
                case "cos" -> one(name, args, Math::cos);
                case "tan" -> one(name, args, Math::tan);
                case "sqrt" -> one(name, args, Math::sqrt);
                case "abs" -> one(name, args, Math::abs);
                case "log" -> one(name, args, Math::log10);
                case "ln" -> one(name, args, Math::log);
                case "exp" -> one(name, args, Math::exp);
                case "min" -> two(name, args, Math::min);
                case "max" -> two(name, args, Math::max);
                case "pow" -> two(name, args, Math::pow);
                default -> throw new ValidationException("unsupported function: " + name);
            };
        }

        private double one(String name, List<Double> args, java.util.function.DoubleUnaryOperator fn) {
            if (args.size() != 1) throw new ValidationException(name + " requires 1 argument");
            return fn.applyAsDouble(args.get(0));
        }

        private double two(String name, List<Double> args, java.util.function.DoubleBinaryOperator fn) {
            if (args.size() != 2) throw new ValidationException(name + " requires 2 arguments");
            return fn.applyAsDouble(args.get(0), args.get(1));
        }

        private boolean match(String text) {
            if (!check(text)) return false;
            position++;
            return true;
        }

        private boolean check(String text) {
            Token token = peek();
            return token != null && token.kind() == TokenKind.SYMBOL && text.equals(token.text());
        }

        private void consume(String text) {
            if (!match(text)) {
                throw new ValidationException("expression expected " + text);
            }
        }

        private Token peek() {
            return position < tokens.size() ? tokens.get(position) : null;
        }

        private Token previous() {
            return tokens.get(position - 1);
        }
    }
}

