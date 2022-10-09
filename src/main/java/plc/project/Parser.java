package plc.project;

import javax.swing.text.html.Option;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
          Ast.Expression lhs=parseExpression();
          if(!match("=")) {
              if(!match(";")){
                  throw new ParseException("no semicolon after expression", (tokens.get(-1).getIndex()+1));
              }
              return new Ast.Statement.Expression(lhs);
          }
          else {
              Ast.Expression rhs=parseExpression();
              if(!match(";")){
                  throw new ParseException("no semicolon after expression", (tokens.get(-1).getIndex())+1);
              }
              else {
                  return new Ast.Statement.Assignment(lhs, rhs);
              }
          }

    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule. 
     * This method should only be called if the next tokens start the case or 
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        try {
            return parseLogicalExpression();
        }
        catch(ParseException p) {
            throw new ParseException("no expression returned", tokens.get(0).getIndex());
        }
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
       try {
           Ast.Expression logical_expr = parseComparisonExpression();
           while (match("&&") || match("||")) {
               String op = tokens.get(-1).getLiteral();
               Ast.Expression rhs = parseComparisonExpression();
               logical_expr = new Ast.Expression.Binary(op, logical_expr, rhs);
           }
           return logical_expr;
       }
       catch(ParseException p) {
           throw new ParseException("logical expression is violated",(tokens.get(0).getIndex()));
       }
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
       try {
           Ast.Expression compare_expr=parseAdditiveExpression();
           while(match("<")|| match(">") || match("==")||match("!=")) {
                 String op=tokens.get(-1).getLiteral();
                 Ast.Expression rhs= parseAdditiveExpression();
                 compare_expr=new Ast.Expression.Binary(op,compare_expr,rhs);
           }
           return compare_expr;
       }
       catch(ParseException p) {
           throw new ParseException("logical expression is violated",(tokens.get(0).getIndex()));
       }
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        try {
            Ast.Expression additive_expr=parseMultiplicativeExpression();
            while(match("+")|| match("-")) {
                String op=tokens.get(-1).getLiteral();
                Ast.Expression rhs= parseMultiplicativeExpression();
                additive_expr=new Ast.Expression.Binary(op,additive_expr,rhs);
            }
            return additive_expr;
        }
        catch(ParseException p) {
            throw new ParseException("additive expression is violated",(tokens.get(0).getIndex()));
        }
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        try {
            Ast.Expression multiplicative_expr=parsePrimaryExpression();
            while(match("*")|| match("-")) {
                String op=tokens.get(-1).getLiteral();
                Ast.Expression rhs= parseMultiplicativeExpression();
                multiplicative_expr=new Ast.Expression.Binary(op,multiplicative_expr,rhs);
            }
            return multiplicative_expr;
        }
        catch(ParseException p) {
            throw new ParseException("multiplicative expression is violated",(tokens.get(0).getIndex()));
        }
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
          if(peek("NIL")) {
              match("NIL");
              return new Ast.Expression.Literal(null);
          }
          else if(peek("TRUE")) {
              match("TRUE");
              Boolean true_statement=true;
              return new Ast.Expression.Literal(true_statement);
          }
          else if(peek("FALSE")) {
              match("FALSE");
              Boolean false_statement=false;
              return new Ast.Expression.Literal(false_statement);
          }
          else if(peek(Token.Type.INTEGER)) {
              BigInteger integer=new BigInteger(tokens.get(0).getLiteral());
              match(Token.Type.INTEGER);
              return new Ast.Expression.Literal(integer);
          }
          else if(peek(Token.Type.DECIMAL)) {
              BigDecimal decimal=new BigDecimal(tokens.get(0).getLiteral());
              match(Token.Type.DECIMAL);
              return new Ast.Expression.Literal(decimal);
          }
          else if(peek(Token.Type.CHARACTER)) {
              String s=tokens.get(0).getLiteral();
              match(Token.Type.CHARACTER);
              s = s.replaceAll("\'","");
              s=s.replace("\\b","\b");
              s=s.replace("\\n","\n");
              s=s.replace("\\r","\r");
              s=s.replace("\\t","\t");
              s=s.replace("\\\\","\\");
              s=s.replace("\\\"","\"");
              s=s.replace("\\\'","\'");
              return new Ast.Expression.Literal(s.charAt(0));
          }
          else if(peek(Token.Type.STRING)) {
              String s=tokens.get(0).getLiteral();
              match(Token.Type.STRING);
              s = s.replaceAll("\"","");
              s=s.replace("\\b","\b");
              s=s.replace("\\n","\n");
              s=s.replace("\\r","\r");
              s=s.replace("\\t","\t");
              s=s.replace("\\\\","\\");
              s=s.replace("\\\"","\"");
              s=s.replace("\\\'","\'");
              return new Ast.Expression.Literal(s);
          }
          else if(peek(Token.Type.IDENTIFIER)) {
              String identifier=tokens.get(0).getLiteral();
              match(Token.Type.IDENTIFIER);
              if(peek("(")) {
                   match("(");
                   List<Ast.Expression>arguments=new ArrayList<Ast.Expression>();
                   if(peek(")")) {
                       match(")");
                       return new Ast.Expression.Function(identifier,arguments);
                   }
                   //insert else
              }
              else if(peek("[")) {
                    match("[");
                    try{
                        Ast.Expression list_expr= parseExpression();
                        peek("]");
                        match("]");
                        return new Ast.Expression.Access(Optional.of(list_expr),identifier);
                    }
                    catch(ParseException p) {
                        throw new ParseException("no closing parentheses or expression inside list", tokens.get(0).getIndex());
                    }
              }
              else {
                  return new Ast.Expression.Access(Optional.empty(),identifier);
              }
          }
          else if(peek("(")) {
               match("(");
                try {
                    Ast.Expression expr=parseExpression();
                    peek(")");
                    match(")");
                    return new Ast.Expression.Group(expr);
                }
                catch(ParseException p) {
                    throw new ParseException("no closing parentheses or expression inside group", tokens.get(0).getIndex());
                }
          }
          throw new ParseException("not a primary expression", tokens.get(0).getIndex());
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
       for(int i=0;i< patterns.length;i++){
           if(!tokens.has(i)) {
               return false;
           }
           else if (patterns[i] instanceof Token.Type) {
               if(patterns[i] != tokens.get(i).getType()) {
                   return false;
               }
           }
           else if(patterns[i] instanceof String){
               if(!patterns[i].equals(tokens.get(i).getLiteral())) {
                   return false;
               }
           }
           else {
               throw new AssertionError("Invalid pattern object: "+ patterns[i].getClass());
           }
       }
       return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek=peek(patterns);
        if(peek) {
            for(int i=0;i< patterns.length;i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
