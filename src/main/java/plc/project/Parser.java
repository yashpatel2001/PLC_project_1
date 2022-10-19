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
            List<Ast.Global> global = new ArrayList<>();
            List<Ast.Function> func = new ArrayList<>();
            boolean passed = false;
            while(tokens.has(0))
            {
                if(peek("LIST") || peek("VAR") || peek("VAL"))
                {
                    if(passed)
                        throw new ParseException("Globals after functions", tokens.get(0).getIndex());
                    global.add(parseGlobal());
                }
                else if(peek("FUN"))
                {
                    func.add(parseFunction());
                    passed = true;
                }
            }
        return new Ast.Source(global,func);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {

        Ast.Global val = null;

        if(peek("LIST"))
            val = parseList();
        else if(peek("VAR"))
            val = parseMutable();
        else if(peek("VAL"))
            val = parseImmutable();
        if(!match(";"))
            throw new ParseException("Missing semicolon",0);

        return val;
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        String name = "";
        List<Ast.Expression> list = new ArrayList<>();
        match("LIST");
        if(match(Token.Type.IDENTIFIER)) {
            name = tokens.get(-1).getLiteral();
            if(match("=")) {
                if(match("[")) {
                    list.add(parseExpression());
                    while(match(","))
                        list.add(parseExpression());
                    if(match("]"))
                        return new Ast.Global(name, true, Optional.of(new Ast.Expression.PlcList(list)));
                }
                else {
                    throw new ParseException("No array bracket", tokens.get(0).getIndex());
                }
            }
            else {
                throw new ParseException("No equal sign", tokens.get(0).getIndex());
            }
        }
        else {
            throw new ParseException("No identifier found", tokens.get(0).getIndex());
        }

        throw new ParseException("No array bracket", tokens.get(0).getIndex());
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        String name = "";
        Optional<Ast.Expression> value = Optional.empty();
        match("VAR");
        if(match(Token.Type.IDENTIFIER)) {
            name = tokens.get(-1).getLiteral();
            if(match("=")) {
                if(!peek(";"))
                    value = Optional.of(parseExpression());
            }
        }
        else {
            throw new ParseException("No identifier found", tokens.get(0).getIndex());
        }
        return new Ast.Global(name,true,value);
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        String name = "";
        Optional<Ast.Expression> value = Optional.empty();
        match("VAL");
        if(match(Token.Type.IDENTIFIER)) {
            name = tokens.get(-1).getLiteral();
            if(match("=")) {
                if(!peek(";"))
                    value = Optional.of(parseExpression());
                else
                    throw new ParseException("No value after equal",tokens.get(0).getIndex());
            }
            else
                throw new ParseException("Invalid token after identifier", tokens.get(0).getIndex());
        }
        else {
            throw new ParseException("No identifier found", tokens.get(0).getIndex());
        }
        return new Ast.Global(name,false,value);
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
       match("FUN");
       String name = "";
       List<String> parameters = new ArrayList<>();
       List<Ast.Statement> statements = new ArrayList<>();

       if(peek(Token.Type.IDENTIFIER)) {
           name = tokens.get(0).getLiteral();
           match(Token.Type.IDENTIFIER);
       }
       else
           throw new ParseException("No function name found", tokens.get(0).getIndex());
       if(!match("("))
           throw new ParseException("No opening parenthesis", tokens.get(0).getIndex());
       if(peek(Token.Type.IDENTIFIER)) {
           parameters.add(tokens.get(0).getLiteral());
           match(Token.Type.IDENTIFIER);
           while(match(",")) {
               if(match(Token.Type.IDENTIFIER)) {
                   parameters.add(tokens.get(0).getLiteral());
               }
               else {
                   throw new ParseException("not identifier or dangling comma", tokens.get(0).getIndex());
               }
           }
       }
        if(match(Token.Type.IDENTIFIER))
            throw new ParseException("identifier without a comma before it", tokens.get(0).getIndex());

        if(!match(")"))
            throw new ParseException("no closing parenthesis", tokens.get(0).getIndex());

        if(!match("DO"))
            throw new ParseException("no DO ", tokens.get(0).getIndex());

        while(tokens.has(0) && !peek("END"))
            statements.add(parseStatement());

        if(!match("END"))
            throw new ParseException("no DO ", tokens.get(0).getIndex());
        return new Ast.Function(name, parameters, statements);
    }


    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        List<Ast.Statement> parseStatements=new ArrayList<Ast.Statement>();
        while(tokens.has(0)) {
            parseStatements.add(parseStatement());
            if(peek("END") || peek("ELSE")|| peek("DEFAULT")) {
                return parseStatements;
            }
        }
        return parseStatements;
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {

        if(match("LET")) {
            return parseDeclarationStatement();
        }
        else if(match("SWITCH")) {
            return parseSwitchStatement();
        }
        else if(match("IF")) {
            return parseIfStatement();
        }
        else if(match("WHILE")) {
            return parseWhileStatement();
        }
        else if(match("RETURN")) {
            return parseReturnStatement();
        }
        else {
            //down below is the rule expression ( '=' expression )?;
            // checks if there is an expression on the left hand side
            Ast.Expression lhs = parseExpression();
            // peeks & advances if there is an equal sign then checks if there is no semicolon
            if (!match("=")) {
                if (!match(";")) {
                    throw new ParseException("no semicolon after expression", (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
                }
                //returns just the lhs
                return new Ast.Statement.Expression(lhs);
            } else {
                // sets expression to write expression if equal sign exists
                Ast.Expression rhs = parseExpression();
                if (!match(";")) {
                    throw new ParseException("no semicolon after expression", (tokens.get(-1).getIndex()) + tokens.get(-1).getLiteral().length());
                }
                // returns Assignment expression
                else {
                    return new Ast.Statement.Assignment(lhs, rhs);
                }
            }
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        if(peek(Token.Type.IDENTIFIER)) {
            match(Token.Type.IDENTIFIER);
            String name_identifier = tokens.get(-1).getLiteral();
            if(peek(";")) {
                match(";");
                return new Ast.Statement.Declaration(name_identifier, Optional.empty());
            }
            else {
                if (peek("=")) {
                    match("=");
                    try {
                        Ast.Expression parse_expr = parseExpression();
                        if (peek(";")) {
                            match(";");
                            return new Ast.Statement.Declaration(name_identifier, Optional.of(parse_expr));
                        } else {
                            if(tokens.has(0)) {
                                throw new ParseException("no expression ahead", (tokens.get(0).getIndex()));
                            }
                            else {
                                throw new ParseException("no expression ahead", (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
                            }
                        }
                    } catch (ParseException p) {
                        if(tokens.has(0)) {
                            throw new ParseException("no expression ahead", (tokens.get(0).getIndex()));
                        }
                        else {
                            throw new ParseException("no expression ahead", (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
                        }
                    }

                }
            }
        }
        else {
            if(tokens.has(0)) {
                throw new ParseException("no expression ahead", (tokens.get(0).getIndex()));
            }
            else {
                throw new ParseException("no expression ahead", (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
            }
        }
        throw new ParseException("no expression ahead",(tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        try {
            Ast.Expression expr=parseExpression();
            match("DO");
            List<Ast.Statement> thenExpr=parseBlock();
            List<Ast.Statement> elseExpr=new ArrayList<Ast.Statement>();
            if(peek("ELSE")) {
                match("ELSE");
                while (!peek("END")) {
                    elseExpr.add(parseStatement());
                }
                match("END");
                return new Ast.Statement.If(expr, thenExpr, elseExpr);
            }
            else{
                match("END");
                return new Ast.Statement.If(expr, thenExpr, elseExpr);
            }
        }
        catch (ParseException p) {
            if(tokens.has(0)) {
                throw new ParseException("no expression ahead", (tokens.get(0).getIndex()));
            }
            else {
                throw new ParseException("no expression ahead", (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
            }
        }
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        try {
            Ast.Expression expression_start = parseExpression();
            List<Ast.Statement.Case> caseList = new ArrayList<Ast.Statement.Case>();
            while (peek("CASE"))
                caseList.add(parseCaseStatement());
            if(match("DEFAULT")){
                Optional<Ast.Expression> def = Optional.empty();
                caseList.add(new Ast.Statement.Case(def,parseBlock()));
                if(match("END"))
                    return new Ast.Statement.Switch(expression_start,caseList);
            }
        }
        catch(ParseException p)  {
            if(tokens.has(0)) {
                throw new ParseException("no expression ahead", (tokens.get(0).getIndex()));
            }
            else {
                throw new ParseException("no expression ahead", (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
            }
        }
        throw new ParseException("no expression ahead", (tokens.get(0).getIndex()));
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule. 
     * This method should only be called if the next tokens start the case or 
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        match("CASE");
        Optional<Ast.Expression> expression = Optional.of(parseExpression());
        if(match(":")) {
            List<Ast.Statement> statements = parseBlock();
            return new Ast.Statement.Case(expression,statements);
        }
        throw new ParseException("Invalid case syntax", (tokens.get(0).getIndex()));
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        if (match("DO"))
            throw new ParseException("No condition", tokens.get(0).getIndex());
        Ast.Expression expr=parseExpression();
        List<Ast.Statement> statements = new ArrayList<>();
        if (!match("DO"))
            throw new ParseException("No DO after the condition", tokens.get(0).getIndex());

        match("DO");
        List<Ast.Statement> listStatement= parseBlock();
        match("END");
        return new Ast.Statement.While(expr,listStatement);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        try {
            Ast.Expression parse_expr = parseExpression();
            match(";");
            return new Ast.Statement.Return(parse_expr);
        }
        catch(ParseException p) {
            if(tokens.has(0)) {
                throw new ParseException("no expression ahead", (tokens.get(0).getIndex()));
            }
            else {
                throw new ParseException("no expression ahead", (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
            }
        }
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        try {
            return parseLogicalExpression();
        }
        catch(ParseException p) {
            throw new ParseException("no expression returned", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
        }
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
       try {
           //checks if there is an expression on left handside of binary expression
           Ast.Expression logical_expr = parseComparisonExpression();
           while (match("&&") || match("||")) {
               String op = tokens.get(-1).getLiteral();
               Ast.Expression rhs = parseComparisonExpression();
               logical_expr = new Ast.Expression.Binary(op, logical_expr, rhs);
           }
           //returns Binary expression with any operators if it has any
           return logical_expr;
       }
       ///throws exception if any part of the logical expression rule is violated
       catch(ParseException p) {
           throw new ParseException("logical expression is violated",(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length()));
       }
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        //checks if left expression exist in try-catch
       try {
           Ast.Expression compare_expr=parseAdditiveExpression();
           //keep checking for operator, appending to the binary expression
           while(match("<")|| match(">") || match("==")||match("!=")) {
                 String op=tokens.get(-1).getLiteral();
                 Ast.Expression rhs= parseAdditiveExpression();
                 compare_expr=new Ast.Expression.Binary(op,compare_expr,rhs);
           }
           //returns the binary expression with operators, or without if not present
           return compare_expr;
       }
       //throws parse exception if any part of binary expr. is violated
       catch(ParseException p) {
           throw new ParseException("logical expression is violated",(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length()));
       }
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        // checks if additive expression exists on left hand side
        try {
            Ast.Expression additive_expr=parseMultiplicativeExpression();
            // builds binary expression with operators if they exist
            while(match("+")|| match("-")) {
                String op=tokens.get(-1).getLiteral();
                Ast.Expression rhs= parseMultiplicativeExpression();
                additive_expr=new Ast.Expression.Binary(op,additive_expr,rhs);
            }
            //returns binary expression
            return additive_expr;
        }
        //throws parseexception of  previous token + previous token length
        catch(ParseException p) {
            throw new ParseException("additive expression is violated",(tokens.get(-1).getIndex())+tokens.get(-1).getLiteral().length());
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
            throw new ParseException("multiplicative expression is violated",(tokens.get(-1).getIndex()+ tokens.get(-1).getLiteral().length()));
        }
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
          // if-else statements for the various primary expressions and there returned expressions based on peek-match of tokens
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
              // removes the single quotes and escape characters. remove the double escape within string to be parsed as a single escape, not literal
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
              // removes the double quotes and escape characters. reasoning for why is mentioned above. these are just the rules for the grammar
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
                  else {
                      Ast.Expression expression_first=parseExpression();
                      arguments.add(expression_first);
                      while(peek(",")) {
                          match(",");
                          Ast.Expression added_expr=parseExpression();
                          arguments.add(added_expr);
                      }
                      return new Ast.Expression.Function(identifier,arguments);

                  }

              }
              else if(peek("[")) {
                    match("[");
                    try{
                        Ast.Expression list_expr= parseExpression();
                        peek("]");
                        if(!match("]")) {
                            throw new ParseException("no closing parentheses or expression inside group", tokens.get(-1).getIndex()+ tokens.get(-1).getLiteral().length());
                        }
                        return new Ast.Expression.Access(Optional.of(list_expr),identifier);
                    }
                    catch(ParseException p) {
                        throw new ParseException("no closing parentheses or expression inside list", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
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
                    if(!match(")")) {
                        throw new ParseException("no closing parentheses or expression inside group", tokens.get(-1).getIndex()+ tokens.get(-1).getLiteral().length());
                    }

                    return new Ast.Expression.Group(expr);
                }
                catch(ParseException p) {
                    throw new ParseException("no closing parentheses or expression inside group", tokens.get(-1).getIndex()+ tokens.get(-1).getLiteral().length());
                }
          }
          throw new ParseException("not a primary expression", tokens.get(-1).getIndex()+ tokens.get(-1).getLiteral().length());
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
