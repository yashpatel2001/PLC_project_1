package plc.project;

import java.io.PrintWriter;
import java.util.List;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                if(!object.toString().equals("print") && !object.toString().equals("."))
                     writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        print("public class Main {");
        newline(0);
        indent += 1;
        if(ast.getGlobals().size()>0) {
            for(int i = 0; i < ast.getGlobals().size(); i++) {
                newline(indent);
                print(ast.getGlobals().get(i));
            }
            newline(0);
        }
        newline(indent);
        print("public static void main(String[] args) {");
        newline(++indent);
        print("System.exit(new Main().main());");
        newline(--indent);
        print("}");

        for(int i = 0; i < ast.getFunctions().size(); i++) {
            newline(0);
            newline(indent);
            print(ast.getFunctions().get(i));
        }
        newline(0);
        newline(--indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        print(ast.getVariable().getType().getJvmName(), " ",ast.getVariable().getJvmName());
        if(!ast.getValue().isEmpty()){
            print(" = ", ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        print(ast.getFunction().getReturnType().getJvmName(), " ", ast.getFunction().getJvmName(), "(");
        if(ast.getParameters().size()>0) {
            for(int i = 0; i<ast.getParameters().size(); i++) {
                if(i!=0) {
                    print(", ");
                }
                print(ast.getFunction().getParameterTypes().get(i).getJvmName(), " ", ast.getParameters().get(i));
            }
        }
        print(") {");
        if(!ast.getStatements().isEmpty()){
            printStatements(ast.getStatements(),ast);
        }
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        print(ast.getExpression(),";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        print(ast.getVariable().getType().getJvmName(), " ", ast.getVariable().getJvmName());
        if(ast.getValue().isPresent()) {
            print(" = ", ast.getValue().get());
        }
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        print(ast.getReceiver(), " = ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if (", ast.getCondition(),") {");
        printStatements(ast.getThenStatements(),ast);
        print("}");

        if(ast.getElseStatements().size()>0){
            print(" else {");
            printStatements(ast.getElseStatements(), ast);
            print("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (", ast.getCondition(), ") {");

        printStatements(ast.getStatements(), ast);

        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if(ast.getType().equals(Environment.Type.NIL)) {
            print("nil");
        }
        if (ast.getType().equals(Environment.Type.CHARACTER)) {
            print("\'", ast.getLiteral(), "\'");
        }
        else if(ast.getType().equals(Environment.Type.STRING)) {
            print("\"", ast.getLiteral(), "\"");
        }
        else {
            print(ast.getLiteral());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(", ast.getExpression(), ")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        String jvmOp = ast.getOperator();
        String op = ast.getOperator();
        if (op.equals("||")){
            jvmOp ="||";
        }
        if(op.equals("&&")) {
            jvmOp = "&&";
        }
        print(ast.getLeft()," ", jvmOp," ",ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        if(ast.getOffset().isPresent()==true) {
            print(ast.getOffset().get(),".");
        }

        print(ast.getVariable().getJvmName());

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        if(ast.getName().isEmpty()==true) {
            print(ast.getName(), ".");
        }

        print(ast.getFunction().getJvmName(), "(");
        if(!ast.getArguments().isEmpty()){
            for(int i = 0; i < ast.getArguments().size(); i++) {
                if(i!=0) {
                    print(", ");
                }
                print(ast.getArguments().get(i));
            }
        }
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    private Void printStatements(List<Ast.Statement> statements, Ast ast) {
        if(!statements.isEmpty()) {
            newline(++indent);
            for(int i = 0; i< statements.size(); i++){
                if (i != 0) {
                    newline(indent);
                }
                print(statements.get(i));
            }
            newline(--indent);
        }
        return null;
    }

}
