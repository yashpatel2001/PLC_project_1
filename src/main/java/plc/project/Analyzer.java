package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        for(Ast.Global global : ast.getGlobals()) {
            visit(global);
        }
        for(Ast.Function func : ast.getFunctions()) {
            visit(func);
        }
        scope.lookupFunction("main",0);
        requireAssignable(Environment.Type.INTEGER,scope.lookupFunction("main",0).getReturnType());
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        if(ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            requireAssignable(Environment.getType(ast.getTypeName()),ast.getValue().get().getType());
        }
        //check if var is mutable?
        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName()), true,Environment.NIL));
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        List<Environment.Type> paramTypes = new ArrayList<>();
        Environment.Type returnType = Environment.Type.NIL;

        for(int i = 0;i < ast.getParameterTypeNames().size();i++) {
            paramTypes.add(Environment.getType(ast.getParameterTypeNames().get(i)));
        }
        if(ast.getReturnTypeName().isPresent()) {
            returnType = Environment.getType(ast.getReturnTypeName().get());
        }
        ast.setFunction(scope.defineFunction(ast.getName(),ast.getName(),paramTypes, returnType, args->Environment.NIL));
        try {
            scope = new Scope(scope);
            for (int i = 0; i < ast.getParameters().size(); i++) {
                scope.defineVariable(ast.getParameters().get(i), ast.getParameters().get(i), paramTypes.get(i), true, Environment.NIL);
            }
            function = ast;
            for (int i = 0; i < ast.getStatements().size(); i++) {
                visit(ast.getStatements().get(i));
            }
        } finally {
            scope = scope.getParent();
            function = null;
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        if(!ast.getExpression().getClass().equals(Ast.Expression.Function.class)) {
            throw new RuntimeException("Not a function");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        if(!ast.getTypeName().isPresent() && !ast.getValue().isPresent()) {
            throw new RuntimeException("Declaration must have type");
        }

        Environment.Type type = null;
        if(ast.getTypeName().isPresent()) {
            type = Environment.getType(ast.getTypeName().get());
        }

        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            if (type == null) {
                type = ast.getValue().get().getType();
            }
            requireAssignable(type, ast.getValue().get().getType());
        }
        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), type, true,Environment.NIL));
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        visit(ast.getReceiver());
        visit(ast.getValue());
        if(!ast.getReceiver().getClass().equals(Ast.Expression.Access.class)) {
            throw new RuntimeException("Not an Expression.Access.class");
        }
        requireAssignable(ast.getReceiver().getType(),ast.getValue().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        if(ast.getThenStatements().size() == 0) {
            throw new RuntimeException("Then Statements empty");
        }
        try{
            scope = new Scope(scope);
            for (Ast.Statement stmt : ast.getThenStatements()){
                visit(stmt);
            }
        }
        finally{
            scope = scope.getParent();
        }

        try{
            scope = new Scope(scope);
            for (Ast.Statement stmt : ast.getElseStatements()){
                visit(stmt);
            }
        }
        finally{
            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        visit(ast.getCondition());
        for(int i=0;i<ast.getCases().size()-1;i++) {
            visit(ast.getCases().get(i).getValue().get());
            if(ast.getCases().get(i).getValue().get().getType() !=ast.getCondition().getType()) {
                throw new RuntimeException();
            }
            try {
                scope=new Scope(scope);
                visit(ast.getCases().get(i));
            }
            finally {
                scope=scope.getParent();
            }
        }
        int size= ast.getCases().size()-1;
        if(ast.getCases().get(size).getValue().isPresent()) {
            throw new RuntimeException();
        }
        try {
            scope=new Scope(scope);
            visit(ast.getCases().get(size));
        }
        finally {
            scope=scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        try {
            for (int i = 0; i < ast.getStatements().size(); i++) {
                visit(ast.getStatements().get(i));
            }
        }catch(RuntimeException e) {
            throw new RuntimeException();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        try {
            scope = new Scope(scope);
            for(Ast.Statement stmt : ast.getStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        visit(ast.getValue());
        requireAssignable(function.getFunction().getReturnType(), ast.getValue().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if(ast.getLiteral() instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if (ast.getLiteral() instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        }
        else if (ast.getLiteral() instanceof String) {
            ast.setType(Environment.Type.STRING);
        }
        else if (ast.getLiteral() == null) {
            ast.setType(Environment.Type.NIL);
        }
        else if (ast.getLiteral() instanceof BigInteger) {
            BigInteger value = (BigInteger) ast.getLiteral();
            BigInteger min = new BigInteger(String.valueOf(Integer.MIN_VALUE));
            BigInteger max = new BigInteger(String.valueOf(Integer.MAX_VALUE));

            boolean OutOfRange = value.compareTo(max) > 0 || value.compareTo(min) < 0;
            if(OutOfRange) {
                throw new RuntimeException("Integer out of Range");
            }
            else {
                ast.setType(Environment.Type.INTEGER);
            }

        }
        else if (ast.getLiteral() instanceof BigDecimal) {
            double val = ((BigDecimal) ast.getLiteral()).doubleValue();
            if(val == Double.NEGATIVE_INFINITY || val == Double.POSITIVE_INFINITY) {
                throw new RuntimeException("Decimal out of Range");
            }
            else {
                ast.setType(Environment.Type.DECIMAL);
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        visit(ast.getExpression());
        if(!ast.getExpression().getClass().equals(Ast.Expression.Binary.class)) {
            throw new RuntimeException("Expression is not Binary");
        }
        ast.setType(ast.getExpression().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        String op = ast.getOperator();
        visit(ast.getLeft());
        visit(ast.getRight());


        switch (op) {
            case "&&":
            case "||":

                requireAssignable(Environment.Type.BOOLEAN, ast.getLeft().getType());
                requireAssignable(Environment.Type.BOOLEAN, ast.getRight().getType());
                ast.setType(Environment.Type.BOOLEAN);

                break;
            case ">":
            case "<":
            case ">=":
            case "<=":
            case "==":
            case "!=":

                requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
                requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());
                ast.setType(Environment.Type.BOOLEAN);
                break;
            case "+":
                if (ast.getRight().getType().equals(Environment.Type.STRING) || ast.getLeft().getType().equals(Environment.Type.STRING)) {
                    ast.setType(Environment.Type.STRING);
                } else if (ast.getLeft().getType().equals(Environment.Type.INTEGER) || ast.getLeft().getType().equals(Environment.Type.DECIMAL)) {
                    if (ast.getRight().getType().equals(ast.getLeft().getType())) {
                        ast.setType(ast.getLeft().getType());
                    } else {
                        throw new RuntimeException("Left is integer or Decimal but right is not the Same");
                    }
                } else {
                    throw new RuntimeException("Left is not integer or Decimal or String");
                }
                break;
            case "-":
            case "*":
            case "/":
                if (ast.getLeft().getType().equals(Environment.Type.INTEGER) || ast.getLeft().getType().equals(Environment.Type.DECIMAL)) {
                    if (ast.getRight().getType().equals(ast.getLeft().getType())) {
                        ast.setType(ast.getLeft().getType());
                    } else {
                        throw new RuntimeException("Left is integer or Decimal but right is not the Same");
                    }
                } else {
                    throw new RuntimeException("Left is not integer or Decimal or String");
                }
                break;
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        if(ast.getOffset().isPresent()) {
            if (ast.getOffset().get().getType() == Environment.Type.INTEGER) {
                visit(ast.getOffset().get());
                ast.setVariable(ast.getOffset().get().getType().getGlobal(ast.getName()));
            } else
                throw new RuntimeException();
        }
        else {
            ast.setVariable(scope.lookupVariable(ast.getName()));
        }
       return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        try {
            List<Ast.Expression> args = ast.getArguments();
            for (int i = 0; i < args.size(); i++) {
                visit(ast.getArguments().get(i));
            }
            Environment.Function lookup = scope.lookupFunction(ast.getName(), ast.getArguments().size());
            for(int i=0;i<args.size();i++) {
                requireAssignable(lookup.getParameterTypes().get(i),ast.getArguments().get(i).getType());
            }
            ast.setFunction(lookup);
            return null;
        }
        catch(RuntimeException e) {
            throw new RuntimeException();
        }
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        try {
            for (int i = 0; i < ast.getValues().size(); i++) {
                visit(ast.getValues().get(i));
            }
            for (int i = 0; i < ast.getValues().size(); i++) {
                requireAssignable(ast.getValues().get(i).getType(), ast.getType());
            }
            return null;
        }catch(RuntimeException e) {
            throw new RuntimeException();
        }
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if(target.equals(type)) {
            return;
        }
        if(target.equals(Environment.Type.ANY)) {
            return;
        }
        if(target.equals(Environment.Type.COMPARABLE)) {
            if(type.equals(Environment.Type.INTEGER) || type.equals(Environment.Type.DECIMAL) || type.equals(Environment.Type.CHARACTER) || type.equals(Environment.Type.STRING)) {
                return;
            }
            else {
                throw new RuntimeException("Not Assignable: Comparable with wrong type");
            }
        }
        throw new RuntimeException("Not Assignable");
    }

}
