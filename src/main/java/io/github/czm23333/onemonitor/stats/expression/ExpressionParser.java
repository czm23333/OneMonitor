package io.github.czm23333.onemonitor.stats.expression;

import io.github.czm23333.onemonitor.stats.expression.exception.IllegalExpressionException;
import io.github.czm23333.onemonitor.stats.expression.node.*;
import it.unimi.dsi.fastutil.Pair;

import java.nio.CharBuffer;
import java.util.*;

public class ExpressionParser {
    private static final Set<Character> OP_CHARS = Set.of('+', '-', '*', '/', '"', '.', ',', '(', ')', '[', ']', '{',
            '}');

    public static Node parse(String str) {
        return parse(CharBuffer.wrap(str), true);
    }

    private static Node parse(CharBuffer expression, boolean root) {
        ArrayDeque<Pair<Node, Boolean>> stack = new ArrayDeque<>();
        loop:
        while (expression.hasRemaining()) {
            expression.mark();
            char c = expression.get();
            switch (c) {
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                    expression.reset();
                    completeNode(stack, parseNumber(expression));
                }
                case '+' -> {
                    if (!stack.isEmpty() && stack.getFirst().right())
                        incompleteNode(stack, new AddNode(Objects.requireNonNull(stack.pollFirst()).left(), null));
                    else completeNode(stack, parseNumber(expression));
                }
                case '-' -> {
                    if (!stack.isEmpty() && stack.getFirst().right())
                        incompleteNode(stack, new SubtractNode(Objects.requireNonNull(stack.pollFirst()).left(), null));
                    else {
                        NumberNode temp = parseNumber(expression);
                        temp.num = -temp.num;
                        completeNode(stack, temp);
                    }
                }
                case '*' -> {
                    if (!stack.isEmpty() && stack.getFirst().right()) {
                        Node last = Objects.requireNonNull(stack.pollLast()).left();
                        if (last instanceof AddNode addNode) {
                            MultiplyNode temp = new MultiplyNode(addNode.getRight(), null);
                            addNode.setRight(null);
                            incompleteNode(stack, addNode);
                            incompleteNode(stack, temp);
                        } else if (last instanceof SubtractNode subtractNode) {
                            MultiplyNode temp = new MultiplyNode(subtractNode.getRight(), null);
                            subtractNode.setRight(null);
                            incompleteNode(stack, subtractNode);
                            incompleteNode(stack, temp);
                        } else incompleteNode(stack, new MultiplyNode(last, null));
                    } else throw new IllegalExpressionException("Incomplete expression");
                }
                case '/' -> {
                    if (!stack.isEmpty() && stack.getFirst().right()) {
                        Node last = Objects.requireNonNull(stack.pollLast()).left();
                        if (last instanceof AddNode addNode) {
                            DivideNode temp = new DivideNode(addNode.getRight(), null);
                            addNode.setRight(null);
                            incompleteNode(stack, addNode);
                            incompleteNode(stack, temp);
                        } else if (last instanceof SubtractNode subtractNode) {
                            DivideNode temp = new DivideNode(subtractNode.getRight(), null);
                            subtractNode.setRight(null);
                            incompleteNode(stack, subtractNode);
                            incompleteNode(stack, temp);
                        } else incompleteNode(stack, new DivideNode(last, null));
                    } else throw new IllegalExpressionException("Incomplete expression");
                }
                case '(' -> completeNode(stack, new SealedNode(parse(expression, false)));
                case ')' -> {
                    if (root) throw new IllegalExpressionException("Parentheses don't match");
                    else break loop;
                }
                case '[' -> {
                    Node temp = parse(expression, false);
                    if (!stack.isEmpty() && stack.getFirst().right()) {
                        Node last = Objects.requireNonNull(stack.pollLast()).left();
                        if (last instanceof BiNode biNode) {
                            GetAttributeNode node = new GetAttributeNode(biNode.getRight(), temp);
                            biNode.setRight(null);
                            incompleteNode(stack, biNode);
                            completeNode(stack, node);
                        } else completeNode(stack, new GetAttributeNode(last, temp));
                    } else completeNode(stack, new GetAttributeNode(temp));
                }
                case ']' -> {
                    if (root) throw new IllegalExpressionException("Square brackets don't match");
                    else break loop;
                }
                case '"' -> completeNode(stack, parseString(expression));
                case '.' -> {
                    if (!stack.isEmpty() && stack.getFirst().right()) {
                        Node last = Objects.requireNonNull(stack.pollLast()).left();
                        if (last instanceof BiNode biNode) {
                            SpreadNode node = new SpreadNode(biNode.getRight());
                            biNode.setRight(null);
                            incompleteNode(stack, biNode);
                            completeNode(stack, node);
                        } else completeNode(stack, new SpreadNode(last));
                    } else throw new IllegalExpressionException("Illegal spread");
                }
                case ',' -> {
                    if (root) throw new IllegalExpressionException("Unexpected comma");
                    else break loop;
                }
                case '{' -> completeNode(stack, parseList(expression));
                case '}' -> {
                    if (root) throw new IllegalExpressionException("Curly brackets don't match");
                    else break loop;
                }
                default -> {
                    if (FunctionManager.ID_BEGIN_CHARS.contains(c)) {
                        expression.reset();
                        completeNode(stack, parseFunctionCall(expression));
                        break;
                    }
                    if (!Character.isSpaceChar(c)) throw new IllegalExpressionException("Unexpected char: " + c);
                }
            }
        }
        if (stack.isEmpty()) return null;
        if (stack.size() != 1 || !stack.getFirst().right())
            throw new IllegalExpressionException("Incomplete expression");
        return stack.getFirst().left();
    }

    private static void incompleteNode(ArrayDeque<Pair<Node, Boolean>> stack, Node node) {
        stack.addFirst(Pair.of(node, false));
    }

    private static void completeNode(ArrayDeque<Pair<Node, Boolean>> stack, Node node) {
        if (stack.isEmpty() || stack.getFirst().right()) stack.addFirst(Pair.of(node, true));
        else {
            BiNode last = (BiNode) Objects.requireNonNull(stack.pollFirst()).first();
            last.setRight(node);
            completeNode(stack, last);
        }
    }

    private static NumberNode parseNumber(CharBuffer expression) {
        StringBuilder builder = new StringBuilder();
        while (expression.hasRemaining()) {
            expression.mark();
            char c = expression.get();
            if (OP_CHARS.contains(c)) {
                expression.reset();
                break;
            } else builder.append(c);
        }
        try {
            return new NumberNode(Double.parseDouble(builder.toString()));
        } catch (NumberFormatException e) {
            throw new IllegalExpressionException("Illegal number: " + builder, e);
        }
    }

    private static StringNode parseString(CharBuffer expression) {
        StringBuilder builder = new StringBuilder();
        while (expression.hasRemaining()) {
            expression.mark();
            char c = expression.get();
            if (c == '\\') if (expression.hasRemaining()) {
                char nxt = expression.get();
                if (nxt == '\\') builder.append('\\');
                else if (nxt == '"') builder.append('"');
                else throw new IllegalExpressionException("Unknown escape char: " + nxt);
            } else throw new IllegalExpressionException("Incomplete escape char");
            else if (c == '"') break;
            else builder.append(c);
        }
        return new StringNode(builder.toString());
    }

    private static CallNode parseFunctionCall(CharBuffer expression) {
        StringBuilder builder = new StringBuilder();
        while (expression.hasRemaining()) {
            expression.mark();
            char c = expression.get();
            if (c == '(') break;
            else builder.append(c);
        }
        String id = builder.toString();
        List<Node> parameter = new ArrayList<>();
        boolean flag = false;
        while (expression.hasRemaining()) {
            Node para = parse(expression, false);
            if (para != null) parameter.add(para);
            expression.reset();
            char c = expression.get();
            if (c == ')') {
                flag = true;
                break;
            } else if (c != ',') throw new IllegalExpressionException("Illegal parameter list");
        }
        if (!flag) throw new IllegalExpressionException("Incomplete parameter list");
        return new CallNode(id, parameter);
    }

    private static ListNode parseList(CharBuffer expression) {
        List<Node> element = new ArrayList<>();
        boolean flag = false;
        while (expression.hasRemaining()) {
            Node elem = parse(expression, false);
            if (elem != null) element.add(elem);
            expression.reset();
            char c = expression.get();
            if (c == '}') {
                flag = true;
                break;
            } else if (c != ',') throw new IllegalExpressionException("Illegal element list");
        }
        if (!flag) throw new IllegalExpressionException("Incomplete element list");
        return new ListNode(element);
    }
}