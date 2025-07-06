import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

abstract class ASTNode {}
class AssignNode extends ASTNode {
    String varName;
    ExprNode expr;
    AssignNode(String varName, ExprNode expr) {
        this.varName = varName;
        this.expr = expr;
    }
}
class PrintNode extends ASTNode {
    ExprNode expr;
    PrintNode(ExprNode expr) {
        this.expr = expr;
    }
}
class InputNode extends ASTNode {
    String varName;
    InputNode(String varName) {
        this.varName = varName;
    }
}
class IfNode extends ASTNode {
    ExprNode condition;
    ASTNode[] thenBlock;
    ASTNode[] elseBlock;
    IfNode(ExprNode condition, ASTNode[] thenBlock, ASTNode[] elseBlock) {
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }
}
class ForNode extends ASTNode {
    String varName;
    int start, end;
    ASTNode[] body;
    ForNode(String varName, int start, int end, ASTNode[] body) {
        this.varName = varName;
        this.start = start;
        this.end = end;
        this.body = body;
    }
}
class ClearNode extends ASTNode {}

abstract class ExprNode {}
class NumberNode extends ExprNode {
    Number value;
    NumberNode(Number value) {
        this.value = value;
    }
}
class StringNode extends ExprNode {
    String value;
    StringNode(String value) {
        this.value = value;
    }
}
class VarNode extends ExprNode {
    String varName;
    VarNode(String varName) {
        this.varName = varName;
    }
}
class ArrayNode extends ExprNode {
    ArrayList<ExprNode> elements;
    ArrayNode(ArrayList<ExprNode> elements) {
        this.elements = elements;
    }
}
class BinaryOpNode extends ExprNode {
    String operator;
    ExprNode left, right;
    BinaryOpNode(String operator, ExprNode left, ExprNode right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }
}

class SayakScriptInterpreter {
    private Map<String, Object> variables = new HashMap<>();
    private String[] tokens;
    private int pos;
    private Scanner scanner = new Scanner(System.in);

    void execute(String code) {
        tokens = tokenize(code);
        pos = 0;
        while (pos < tokens.length && !tokens[pos].isEmpty()) {
            ASTNode stmt = parseStatement();
            if (stmt != null) interpret(stmt);
        }
    }

    private String[] tokenize(String code) {
        ArrayList<String> tokenList = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean inQuotes = false;
        char quoteType = '\0';

        code = code.trim();
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if ((c == '"' || c == '\'') && !inQuotes) {
                inQuotes = true;
                quoteType = c;
                if (currentToken.length() > 0) {
                    splitAndAddTokens(tokenList, currentToken.toString());
                    currentToken = new StringBuilder();
                }
                currentToken.append(c);
            } else if (inQuotes && c == quoteType) {
                inQuotes = false;
                currentToken.append(c);
                tokenList.add(currentToken.toString());
                currentToken = new StringBuilder();
            } else if (inQuotes) {
                currentToken.append(c);
            } else if ("{}()[],;".indexOf(c) != -1) {
                if (currentToken.length() > 0) {
                    splitAndAddTokens(tokenList, currentToken.toString());
                    currentToken = new StringBuilder();
                }
                tokenList.add(String.valueOf(c));
            } else if ("+-*/><=".indexOf(c) != -1) {
                if (currentToken.length() > 0) {
                    String prev = currentToken.toString();
                    if (i + 1 < code.length() && "=".indexOf(code.charAt(i + 1)) != -1) {
                        splitAndAddTokens(tokenList, prev);
                        currentToken = new StringBuilder().append(c).append(code.charAt(i + 1));
                        i++;
                        tokenList.add(currentToken.toString());
                        currentToken = new StringBuilder();
                    } else {
                        splitAndAddTokens(tokenList, prev);
                        tokenList.add(String.valueOf(c));
                        currentToken = new StringBuilder();
                    }
                } else {
                    if (i + 1 < code.length() && "=".indexOf(code.charAt(i + 1)) != -1) {
                        currentToken.append(c).append(code.charAt(i + 1));
                        i++;
                        tokenList.add(currentToken.toString());
                        currentToken = new StringBuilder();
                    } else {
                        tokenList.add(String.valueOf(c));
                    }
                }
            } else if (Character.isWhitespace(c)) {
                if (currentToken.length() > 0) {
                    splitAndAddTokens(tokenList, currentToken.toString());
                    currentToken = new StringBuilder();
                }
            } else {
                currentToken.append(c);
            }
        }
        if (currentToken.length() > 0) {
            splitAndAddTokens(tokenList, currentToken.toString());
        }
        return tokenList.toArray(new String[0]);
    }

    private void splitAndAddTokens(ArrayList<String> tokenList, String token) {
        if (token.contains("=") && !token.equals("=") && !token.equals("==")) {
            int idx = token.indexOf("=");
            String left = token.substring(0, idx);
            if (!left.isEmpty()) tokenList.add(left);
            tokenList.add("=");
            String right = token.substring(idx + 1);
            if (!right.isEmpty()) tokenList.add(right);
        } else {
            tokenList.add(token);
        }
    }

    private ASTNode parseStatement() {
        if (pos >= tokens.length || tokens[pos].isEmpty()) return null;
        String token = tokens[pos];
        if (token.equals("le") && pos + 1 < tokens.length && tokens[pos + 1].equals("sayak")) {
            return parseAssignment();
        } else if (token.equals("dekho")) {
            return parsePrint();
        } else if (token.equals("bolo")) {
            return parseInput();
        } else if (token.equals("agar")) {
            return parseIf();
        } else if (token.equals("for") && pos + 1 < tokens.length && tokens[pos + 1].equals("sayak")) {
            return parseFor();
        } else if (token.equals("cls")) {
            pos++;
            if (pos >= tokens.length || !tokens[pos].equals(";")) throw new RuntimeException("Missing semicolon after cls");
            pos++;
            return new ClearNode();
        } else if (token.contains("=")) {
            throw new RuntimeException("Wrong syntax: Missing 'le sayak'. Correct syntax: 'le sayak " + token + ";'");
        }
        pos++;
        return null;
    }

    private AssignNode parseAssignment() {
        pos += 2; // Skip "le sayak"
        if (pos >= tokens.length) throw new RuntimeException("Incomplete assignment");
        String varName = tokens[pos++];
        if (pos >= tokens.length || !tokens[pos].equals("=")) throw new RuntimeException("Missing '=' in assignment");
        pos++; // Skip "="
        ExprNode expr = parseExpression();
        if (pos >= tokens.length || !tokens[pos].equals(";")) throw new RuntimeException("Missing semicolon after assignment");
        pos++; // Skip ";"
        return new AssignNode(varName, expr);
    }

    private PrintNode parsePrint() {
        pos++; // Skip "dekho"
        if (pos >= tokens.length || !tokens[pos].equals("(")) throw new RuntimeException("Missing '(' after dekho");
        pos++; // Skip "("
        ExprNode expr = parseExpression();
        if (pos >= tokens.length || !tokens[pos].equals(")")) throw new RuntimeException("Missing ')' after dekho expression");
        pos++; // Skip ")"
        if (pos >= tokens.length || !tokens[pos].equals(";")) throw new RuntimeException("Missing semicolon after dekho");
        pos++; // Skip ";"
        return new PrintNode(expr);
    }

    private InputNode parseInput() {
        pos++; // Skip "bolo"
        if (pos >= tokens.length) throw new RuntimeException("Missing variable for input");
        String varName = tokens[pos++];
        if (pos >= tokens.length || !tokens[pos].equals(";")) throw new RuntimeException("Missing semicolon after bolo");
        pos++; // Skip ";"
        return new InputNode(varName);
    }

    private IfNode parseIf() {
        pos++; // Skip "agar"
        if (pos >= tokens.length || !tokens[pos].equals("(")) throw new RuntimeException("Missing '(' in agar");
        pos++; // Skip "("
        ExprNode condition = parseExpression();
        if (pos >= tokens.length || !tokens[pos].equals(")")) throw new RuntimeException("Missing ')' in agar");
        pos++; // Skip ")"
        if (pos >= tokens.length || !tokens[pos].equals("{")) throw new RuntimeException("Missing '{' in agar");
        pos++; // Skip "{"
        ArrayList<ASTNode> thenBlock = new ArrayList<>();
        while (pos < tokens.length && !tokens[pos].equals("}")) {
            ASTNode stmt = parseStatement();
            if (stmt != null) thenBlock.add(stmt);
        }
        if (pos >= tokens.length) throw new RuntimeException("Missing '}' in agar");
        pos++; // Skip "}"
        ArrayList<ASTNode> elseBlock = new ArrayList<>();
        if (pos < tokens.length && tokens[pos].equals("warna")) {
            pos++; // Skip "warna"
            if (pos >= tokens.length || !tokens[pos].equals("{")) throw new RuntimeException("Missing '{' after warna");
            pos++; // Skip "{"
            while (pos < tokens.length && !tokens[pos].equals("}")) {
                ASTNode stmt = parseStatement();
                if (stmt != null) elseBlock.add(stmt);
            }
            if (pos >= tokens.length) throw new RuntimeException("Missing '}' after warna");
            pos++; // Skip "}"
        }
        return new IfNode(condition, thenBlock.toArray(new ASTNode[0]), elseBlock.toArray(new ASTNode[0]));
    }

    private ForNode parseFor() {
        pos += 2; // Skip "for sayak"
        if (pos >= tokens.length) throw new RuntimeException("Missing variable in for loop");
        String varName = tokens[pos++];
        if (pos >= tokens.length || !tokens[pos].equals("in")) throw new RuntimeException("Missing 'in' in for loop");
        pos++; // Skip "in"
        if (pos >= tokens.length || !tokens[pos].equals("range")) throw new RuntimeException("Missing 'range' in for loop");
        pos++; // Skip "range"
        if (pos >= tokens.length || !tokens[pos].equals("(")) throw new RuntimeException("Missing '(' in range");
        pos++; // Skip "("
        if (pos >= tokens.length || !tokens[pos].matches("\\d+")) throw new RuntimeException("Missing start number in range");
        int start = Integer.parseInt(tokens[pos++]);
        if (pos >= tokens.length || !tokens[pos].equals("to")) throw new RuntimeException("Missing 'to' in range");
        pos++; // Skip "to"
        if (pos >= tokens.length || !tokens[pos].matches("\\d+")) throw new RuntimeException("Missing end number in range");
        int end = Integer.parseInt(tokens[pos++]);
        if (pos >= tokens.length || !tokens[pos].equals(")")) throw new RuntimeException("Missing ')' in range");
        pos++; // Skip ")"
        if (pos >= tokens.length || !tokens[pos].equals("{")) throw new RuntimeException("Missing '{' in for loop");
        pos++; // Skip "{"
        ArrayList<ASTNode> body = new ArrayList<>();
        while (pos < tokens.length && !tokens[pos].equals("}")) {
            ASTNode stmt = parseStatement();
            if (stmt != null) body.add(stmt);
        }
        if (pos >= tokens.length) throw new RuntimeException("Missing '}' in for loop");
        pos++; // Skip "}"
        return new ForNode(varName, start, end, body.toArray(new ASTNode[0]));
    }

    private ExprNode parseExpression() {
        return parseComparison();
    }

    private ExprNode parseComparison() {
        ExprNode left = parseAddition();
        while (pos < tokens.length && (tokens[pos].equals(">") || tokens[pos].equals("<") ||
                tokens[pos].equals(">=") || tokens[pos].equals("<=") || tokens[pos].equals("=="))) {
            String op = tokens[pos++];
            ExprNode right = parseAddition();
            left = new BinaryOpNode(op, left, right);
        }
        return left;
    }

    private ExprNode parseAddition() {
        ExprNode left = parseMultiplication();
        while (pos < tokens.length && tokens[pos].equals("+")) {
            String op = tokens[pos++];
            ExprNode right = parseMultiplication();
            left = new BinaryOpNode(op, left, right);
        }
        return left;
    }

    private ExprNode parseMultiplication() {
        ExprNode left = parseFactor();
        while (pos < tokens.length && tokens[pos].equals("*")) {
            String op = tokens[pos++];
            ExprNode right = parseFactor();
            left = new BinaryOpNode(op, left, right);
        }
        return left;
    }

    private ExprNode parseFactor() {
        if (pos >= tokens.length) throw new RuntimeException("Incomplete expression");
        String token = tokens[pos++];
        if (token.equals("[")) {
            ArrayList<ExprNode> elements = new ArrayList<>();
            if (pos < tokens.length && !tokens[pos].equals("]")) {
                do {
                    elements.add(parseExpression());
                    if (pos < tokens.length && tokens[pos].equals(",")) {
                        pos++; // Skip comma
                    } else {
                        break;
                    }
                } while (pos < tokens.length && !tokens[pos].equals("]"));
            }
            if (pos >= tokens.length || !tokens[pos].equals("]")) {
                throw new RuntimeException("Missing ']' in array literal");
            }
            pos++; // Skip "]"
            return new ArrayNode(elements);
        } else if (token.matches("\\d+")) {
            return new NumberNode(Integer.parseInt(token));
        } else if (token.matches("\\d+\\.\\d+")) {
            return new NumberNode(Float.parseFloat(token));
        } else if ((token.startsWith("\"") && token.endsWith("\"")) || (token.startsWith("'") && token.endsWith("'")) && token.length() > 1) {
            return new StringNode(token.substring(1, token.length() - 1));
        }
        return new VarNode(token);
    }

    private void interpret(ASTNode node) {
        if (node instanceof AssignNode) {
            AssignNode assign = (AssignNode) node;
            variables.put(assign.varName, eval(assign.expr));
        } else if (node instanceof PrintNode) {
            PrintNode print = (PrintNode) node;
            Object value = eval(print.expr);
            if (value != null) System.out.println(value);
            else throw new RuntimeException("Expression cannot be evaluated");
        } else if (node instanceof InputNode) {
            InputNode input = (InputNode) node;
            System.out.print("Enter input for " + input.varName + ": ");
            String in = scanner.nextLine();
            try {
                if (in.contains(".")) {
                    variables.put(input.varName, Float.parseFloat(in));
                } else {
                    variables.put(input.varName, Integer.parseInt(in));
                }
            } catch (NumberFormatException e) {
                variables.put(input.varName, in);
            }
        } else if (node instanceof IfNode) {
            IfNode ifNode = (IfNode) node;
            Object condValue = eval(ifNode.condition);
            if (!(condValue instanceof Number)) throw new RuntimeException("Condition must be numeric (0 or 1)");
            if (((Number) condValue).floatValue() != 0) {
                for (ASTNode stmt : ifNode.thenBlock) interpret(stmt);
            } else {
                for (ASTNode stmt : ifNode.elseBlock) interpret(stmt);
            }
        } else if (node instanceof ForNode) {
            ForNode forNode = (ForNode) node;
            for (int i = forNode.start; i <= forNode.end; i++) {
                variables.put(forNode.varName, i);
                for (ASTNode stmt : forNode.body) {
                    interpret(stmt);
                }
            }
        } else if (node instanceof ClearNode) {
            try {
                if (System.getProperty("os.name").contains("Windows")) {
                    new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                } else {
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                }
            } catch (Exception e) {
                System.out.println("Error clearing screen: " + e.getMessage());
            }
        }
    }

    private Object eval(ExprNode expr) {
        if (expr instanceof NumberNode) {
            return ((NumberNode) expr).value;
        } else if (expr instanceof StringNode) {
            return ((StringNode) expr).value;
        } else if (expr instanceof VarNode) {
            Object value = variables.get(((VarNode) expr).varName);
            if (value == null) throw new RuntimeException("Variable '" + ((VarNode) expr).varName + "' not defined");
            return value;
        } else if (expr instanceof ArrayNode) {
            ArrayNode arrayNode = (ArrayNode) expr;
            ArrayList<Object> evaluatedElements = new ArrayList<>();
            for (ExprNode element : arrayNode.elements) {
                evaluatedElements.add(eval(element));
            }
            return evaluatedElements;
        } else if (expr instanceof BinaryOpNode) {
            BinaryOpNode binOp = (BinaryOpNode) expr;
            Object left = eval(binOp.left);
            Object right = eval(binOp.right);
            if (binOp.operator.equals("+")) {
                if (left instanceof Integer && right instanceof Integer) {
                    return (Integer) left + (Integer) right;
                } else if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).floatValue() + ((Number) right).floatValue();
                } else {
                    return String.valueOf(left) + String.valueOf(right);
                }
            } else if (binOp.operator.equals("*")) {
                if (left instanceof Integer && right instanceof Integer) {
                    return (Integer) left * (Integer) right;
                } else if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).floatValue() * ((Number) right).floatValue();
                } else {
                    throw new RuntimeException("Multiplication (*) requires numbers");
                }
            }
            if (!(left instanceof Number) || !(right instanceof Number)) {
                throw new RuntimeException("Comparison operators require numbers");
            }
            float leftFloat = ((Number) left).floatValue();
            float rightFloat = ((Number) right).floatValue();
            switch (binOp.operator) {
                case ">": return leftFloat > rightFloat ? 1 : 0;
                case "<": return leftFloat < rightFloat ? 1 : 0;
                case ">=": return leftFloat >= rightFloat ? 1 : 0;
                case "<=": return leftFloat <= rightFloat ? 1 : 0;
                case "==": return leftFloat == rightFloat ? 1 : 0;
                default: throw new RuntimeException("Unknown operator: " + binOp.operator);
            }
        }
        return null;
    }

    public static void main(String[] args) {
        SayakScriptInterpreter interpreter = new SayakScriptInterpreter();
        Scanner scanner = new Scanner(System.in);

        System.out.println("Welcome to SayakScript v5.7 - The Array Ace!");
        System.out.println("Created by Sayak Biswas");
        System.out.println("-------------------------------------------------------");
        System.out.println("Hey there, coder! SayakScript now rocks arrays!");
        System.out.println("Version: 5.7 (April 07, 2025)");
        System.out.println("New in v5.7:");
        System.out.println("  - Arrays: 'le sayak arr=[1, 2, 3];'");
        System.out.println("Features:");
        System.out.println("  - Variables: 'le sayak x=10;' (int), 'le sayak y=10.1;' (float), 'le sayak z=\"sayak\";' (str)");
        System.out.println("  - Print: 'dekho(\"hello world\");'");
        System.out.println("  - Input: 'bolo x;' (reads numbers or strings)");
        System.out.println("  - Conditionals: 'agar(x>0){...}warna{...}'");
        System.out.println("  - Loop: 'for sayak i in range(1 to 10) {dekho(i);}'");
        System.out.println("  - Operators: +, *, >, <, >=, <=, == (Python-style '+'!)");
        System.out.println("  - Clear: 'cls;'");
        System.out.println("Try this: 'le sayak arr=[1, 2, 3]; dekho(arr);'");
        System.out.println("Type 'exit' to stop!");
        System.out.println("-------------------------------------------------------");

        while (true) {
            System.out.print("SayakScript> ");
            String input = scanner.nextLine();
            if (input.equals("exit")) break;
            try {
                interpreter.execute(input);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        scanner.close();
    }
}