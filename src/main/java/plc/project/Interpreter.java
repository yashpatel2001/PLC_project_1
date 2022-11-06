package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        for(Ast.Global global: ast.getGlobals()) {
            visit(global);
        }
        for(Ast.Function function: ast.getFunctions()){
            visit(function);
        }try {
            return scope.lookupFunction("main", 0).invoke(new ArrayList<>());
        }catch(Exception e) {
            try {
                throw new Exception();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(),true, visit(ast.getValue().get()));
        }
        else {
            scope.defineVariable(ast.getName(),false, Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
         Scope capt=scope;
         scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            Scope curr = scope;
            scope = new Scope(capt);

            for (int i = 0; i < ast.getParameters().size(); i++) {
                scope.defineVariable(ast.getParameters().get(i),true, args.get(i));
            }

            try {
                for (Ast.Statement stmt : ast.getStatements()) {
                    visit(stmt);
                }
            }
            catch(Return e) {
                return e.value;
            }
            finally {
                scope = curr;
            }
            return Environment.NIL;
        });

        return Environment.NIL;

    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), true, visit(ast.getValue().get()));
        }
        else {
            scope.defineVariable(ast.getName(), false,Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        if(ast.getReceiver().getClass().equals(Ast.Expression.Access.class)) {
            if(!scope.lookupVariable(((Ast.Expression.Access) ast.getReceiver()).getName()).getMutable())
                throw new RuntimeException();
            if(((Ast.Expression.Access) ast.getReceiver()).getOffset().isPresent())
            {
                Scope scope1=null;
                scope1=scope;

                Object list= null;
                //Object index=((Ast.Expression.Access) ast.getReceiver()).getOffset().get();
                Environment.PlcObject s= visit(((Ast.Expression.Access) ast.getReceiver()).getOffset().get());
                Object l=s.getValue();
                Integer i= Integer.valueOf(String.valueOf((BigInteger)l));
                Environment.PlcObject find= scope1.lookupVariable(((Ast.Expression.Access) ast.getReceiver()).getName()).getValue();
                list=find.getValue();
                //Object list_val = scope1.getValue();
                //List<BigInteger> bigIntArrayList= (List<BigInteger>) list_val;
                //scope1.lookupVariable(((Ast.Expression.Access) ast.getReceiver()).getName()).setValue();

                //Environment.PlcObject scopeFind = scope.lookupVariable(((Ast.Expression.Access) ast.getReceiver()).getName()).getValue();
               //Object list_val=scopeFind.getValue();

                List<BigInteger> list_assignment= (List<BigInteger>) list;

               // scope1.lookupVariable(((Ast.Expression.Access) ast.getReceiver()).getName()).setValue();

               // Object list_val= s.getValue();


                scope=scope1;
                return Environment.NIL;



            }
            else {
                scope.lookupVariable(((Ast.Expression.Access) ast.getReceiver()).getName()).setValue(visit(ast.getValue()));
            }
        }
        else
            throw new RuntimeException();
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        if(requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Statement stmt : ast.getThenStatements()) {
                    visit(stmt);
                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        else if(!requireType(Boolean.class,visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Statement stmt : ast.getElseStatements()) {
                    visit(stmt);
                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {

        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {

        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while(requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                ast.getStatements().forEach(this::visit);
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        Environment.PlcObject object =visit(ast.getValue());
        throw new Return(object);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
       Environment.PlcObject object_created= Environment.create(ast.getLiteral());
       if(ast.getLiteral()==null) {
           Environment.PlcObject object=Environment.create(Environment.NIL.getValue());
           return object;
       }
       return object_created;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        Environment.PlcObject obj = Environment.create(visit(ast.getExpression()).getValue());
        return obj;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        String op = ast.getOperator();
        if(op.equals("&&")) {
            Environment.PlcObject obj;
            if (requireType(Boolean.class, visit(ast.getLeft()))) {
                if (requireType(Boolean.class, visit(ast.getRight()))) {
                    obj = Environment.create(true);
                } else {
                    obj = Environment.create(false);
                }
            }
            else {
                obj = Environment.create(false);
            }
            return obj;
        }
        else if(op.equals("||")) {
            Environment.PlcObject obj;
            if (requireType(Boolean.class, visit(ast.getLeft()))) {
                obj = Environment.create(true);

            } else if (requireType(Boolean.class, visit(ast.getRight()))) {
                obj = Environment.create(true);
            } else {
                obj = Environment.create(false);
            }
            return obj;
        }
        else if(op.equals("<")) {
            Environment.PlcObject left = visit(ast.getLeft());
            return Environment.create(requireType(Comparable.class, left).compareTo(requireType(left.getValue().getClass(),visit(ast.getRight())))<0);
        }
        else if (op.equals("<=")) {
            Environment.PlcObject left = visit(ast.getLeft());

            return Environment.create(requireType(Comparable.class,left).compareTo(requireType(left.getValue().getClass(),visit(ast.getRight())))<=0);

        }
        else if (op.equals(">")) {
            Environment.PlcObject left = visit(ast.getLeft());

            return Environment.create(requireType(Comparable.class,left).compareTo(requireType(left.getValue().getClass(),visit(ast.getRight())))>0);


        }
        else if (op.equals(">=")) {
            Environment.PlcObject left = visit(ast.getLeft());

            return Environment.create(requireType(Comparable.class,left).compareTo(requireType(left.getValue().getClass(),visit(ast.getRight())))>=0);

        }
        else if (op.equals("==")) {
            Environment.PlcObject obj;
            if (ast.getLeft().equals(ast.getRight())) {
                obj = Environment.create(true);
            }
            else {
                obj = Environment.create(false);
            }
            return obj;
        }
        else if (op.equals("!=")) {
            Environment.PlcObject obj;
            if (ast.getLeft().equals(ast.getRight())) {
                obj = Environment.create(false);
            }
            else {
                obj = Environment.create(true);
            }
            return obj;
        }
        else if (op.equals("+")) {
            Environment.PlcObject left = visit(ast.getLeft());
            Environment.PlcObject sum;
            if(left.getValue() instanceof String) {
                return Environment.create(""+left.getValue() + visit(ast.getRight()).getValue());
            }
            else if(left.getValue() instanceof BigInteger) {
                sum = Environment.create(((BigInteger) left.getValue()).add(requireType(BigInteger.class,visit(ast.getRight()))));

            }
            else if(left.getValue() instanceof BigDecimal) {
                sum = Environment.create(((BigDecimal) left.getValue()).add(requireType(BigDecimal.class,visit(ast.getRight()))));
            }
            else {
                throw new RuntimeException();
            }
            return sum;

        }
        else if (op.equals("-")) {

            Environment.PlcObject right = visit(ast.getRight());
            Environment.PlcObject left = visit(ast.getLeft());
            Environment.PlcObject sum;
            if(left.getValue() instanceof BigInteger) {
                sum = Environment.create(((BigInteger) left.getValue()).subtract(requireType(BigInteger.class,visit(ast.getRight()))));
            }
            else if(left.getValue() instanceof BigDecimal) {
                sum = Environment.create(((BigDecimal) left.getValue()).subtract(requireType(BigDecimal.class,right)));
            }
            else {
                throw new RuntimeException();
            }
            return sum;
        }
        else if (op.equals("*")){
            Environment.PlcObject right = visit(ast.getRight());
            Environment.PlcObject left = visit(ast.getLeft());
            Environment.PlcObject sum;
            if(left.getValue() instanceof BigInteger) {
                sum = Environment.create(((BigInteger) left.getValue()).multiply(requireType(BigInteger.class,visit(ast.getRight()))));
            }
            else if(left.getValue() instanceof BigDecimal) {
                sum = Environment.create(((BigDecimal) left.getValue()).multiply(requireType(BigDecimal.class,right)));
            }
            else {
                throw new RuntimeException();
            }
            return sum;
        }
        else if (op.equals("/")) {
            Environment.PlcObject right = visit(ast.getRight());
            Environment.PlcObject left = visit(ast.getLeft());


            if(right.getValue().toString().equals("0")) {
                throw new RuntimeException();
            }

            Environment.PlcObject sum;
            if (left.getValue() instanceof BigInteger) {
                sum = Environment.create(((BigInteger) left.getValue()).divide(requireType(BigInteger.class,visit(ast.getRight()))));
            }
            else if(left.getValue() instanceof BigDecimal) {
                sum = Environment.create(((BigDecimal) left.getValue()).divide(requireType(BigDecimal.class,right),BigDecimal.ROUND_HALF_EVEN));
            }
            else {
                throw new RuntimeException();
            }
            return sum;
        }
        throw new RuntimeException();

    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        Environment.PlcObject object_access=null;

        if(ast.getOffset().isPresent()) {
           Environment.PlcObject access= visit(ast.getOffset().get());
           if(access.getValue().getClass()==BigInteger.class) {
               Environment.PlcObject scope1 = scope.lookupVariable(ast.getName()).getValue();
               Object list_val = scope1.getValue();
               List<BigInteger> bigIntArrayList= (List<BigInteger>) list_val;
               Integer index= Integer.valueOf(String.valueOf((BigInteger) access.getValue()));
               BigInteger i= bigIntArrayList.get((int) index);
               Environment.PlcObject list_ret = Environment.create(i);
               return list_ret;

           }
           else {
               throw new Return(access);
           }
        }
        else {
            Environment.PlcObject scope1 = scope.lookupVariable(ast.getName()).getValue();
            return scope1;
        }

    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
            if(ast.getArguments().size()==0) {
                return Environment.create(ast.getName());
            }
            List<Environment.PlcObject> list= new ArrayList<>();

            for(int i=0;i<ast.getArguments().size();i++) {
                list.add(visit(ast.getArguments().get(i)));
            }
            return scope.lookupFunction(ast.getName(),ast.getArguments().size()).invoke(list);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        List<Object> arrayList=new ArrayList<Object>();
        for(int i=0;i<ast.getValues().size();i++) {
            arrayList.add(visit(ast.getValues().get(i)).getValue());
        }
        Environment.PlcObject object=Environment.create(arrayList);
        return object;
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
