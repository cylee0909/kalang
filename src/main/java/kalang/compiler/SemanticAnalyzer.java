package kalang.compiler;

import kalang.util.MathType;
import kalang.util.AstUtil;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import kalang.ast.AssignExpr;
import kalang.ast.AstNode;
import kalang.ast.AstVisitor;
import kalang.ast.BinaryExpr;
import kalang.ast.CastExpr;
import kalang.ast.CatchBlock;
import kalang.ast.ClassNode;
import kalang.ast.ConstExpr;
import kalang.ast.ElementExpr;
import kalang.ast.ExprNode;
import kalang.ast.FieldExpr;
import kalang.ast.IfStmt;
import kalang.ast.InvocationExpr;
import kalang.ast.ThisExpr;
import kalang.ast.LoopStmt;
import kalang.ast.MethodNode;
import kalang.ast.MultiStmtExpr;
import kalang.ast.NewArrayExpr;
import kalang.ast.ParameterExpr;
import kalang.ast.ReturnStmt;
import kalang.ast.Statement;
import kalang.ast.TryStmt;
import kalang.ast.UnaryExpr;
import kalang.ast.VarDeclStmt;
import kalang.ast.VarExpr;
import kalang.ast.VarObject;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import kalang.ast.AnnotationNode;
import kalang.ast.Annotationable;
import kalang.ast.ArrayLengthExpr;
import kalang.ast.ClassReference;
import kalang.ast.ErrorousExpr;
import kalang.ast.FieldNode;
import kalang.ast.IncrementExpr;
import kalang.ast.LocalVarNode;
import kalang.ast.NewObjectExpr;
import kalang.ast.ParameterNode;
import kalang.ast.ThrowStmt;
import kalang.ast.UnknownFieldExpr;
import kalang.ast.UnknownInvocationExpr;
import kalang.core.ClassType;
import kalang.core.PrimitiveType;
import kalang.core.Type;
import kalang.core.Types;
import static kalang.util.AstUtil.getMethodsByName;
import static kalang.util.AstUtil.getParameterTypes;
import static kalang.util.AstUtil.matchTypes;
import kalang.util.BoxUtil;

/**
 *  The semantic analyzer class infers and checks the componentType of expressions. It may transform the abstract syntax tree.
 * 
 * @author Kason Yang <i@kasonyang.com>
 */
public class SemanticAnalyzer extends AstVisitor<Type> {
   
    private HashMap<String, VarObject> fields;

    private AstLoader astLoader;

    private ClassNode clazz;

   private  MethodNode method;

    private List<String> methodDeclared;

    private boolean returned;

    private SemanticErrorReporter err;

    private CompileErrorHandler errHandler;

    private Stack<Map<Type,AstNode>> exceptionStack = new Stack();
    private CompilationUnit source;

    public SemanticAnalyzer(CompilationUnit source,AstLoader astLoader) {
        this.astLoader = astLoader;
        this.source = source;
        errHandler = (error) -> {
                System.err.println(error.toString());
        };
    }
    
    private Type getDefaultType(){
        return Types.ROOT_TYPE;
    }

    public void setAstSemanticErrorHandler(CompileErrorHandler handler) {
        errHandler = handler;
    }

    /**
     * checks whether an expression as assignable to another expression , and transform the expression if needed.
     * 
     * @param expr
     * @param from
     * @param to
     * @param node
     * @return the assignable expression when assignable ,or null
     */
    @Nullable
    private ExprNode checkAssign(ExprNode expr, Type from, Type to, AstNode node) {
        expr = BoxUtil.assign(expr, from, to);
        if (expr == null) {
            err.failedToCast(node, from.getName(), to.getName());
            return null;
        }
        return expr;
    }

    @Nullable
    public ClassNode loadAst(String name, AstNode node) {
        ClassNode ast = this.astLoader.getAst(name);
        if (ast == null) {
            err.classNotFound(node, name);
            return null;
        }
        return ast;
    }

    public void check(ClassNode clz) {
        err = new SemanticErrorReporter(clz,source ,new SemanticErrorReporter.AstSemanticReporterCallback() {
            @Override
            public void handleAstSemanticError(SemanticError error) {
                errHandler.handleCompileError(error);
            }
        });
        this.fields = new HashMap();
        this.methodDeclared = new LinkedList();
        for (VarObject f : clz.fields) {
            this.fields.put(f.name, f);
        }
        this.clazz = clz;
        visit(clazz);
        if (clazz.interfaces.size() > 0) {
            for (ClassNode itfNode : clazz.interfaces) {
                if (itfNode == null) {
                    continue;
                }
                List<MethodNode> unImps = AstUtil.getUnimplementedMethod(clazz, itfNode);
                if (unImps.size() > 0) {
                    err.notImplementedMethods(clazz, itfNode, unImps);
                }
            }
        }
    }

    @Override
    public Type visit(AstNode node) {
        if(node==null) return null;
        if (node instanceof Statement && returned) {
            err.fail("unable to reach statement", SemanticError.LACKS_OF_STATEMENT, node);
            return null;
        }
        if(node instanceof Annotationable){
            validateAnnotation(((Annotationable)node).getAnnotations());
        }
        Object ret = super.visit(node);
        if (ret instanceof Type) {
            return  (Type) ret;
        }else if(node instanceof ExprNode){
            return ((ExprNode)node).getType();
        }
        return null;
    }

    @Override
    public Type visitCastExpr(CastExpr node) {
        Type et = visit(node.getExpr());
        if(!node.getToType().isSubTypeOf(et)
                && !node.getToType().equals(et)
                ){
            err.failedToCast(node, et.getName(), node.getToType().getName());
        }
        return null;
    }

    @Override
    public Type visitAssignExpr(AssignExpr node) {
        Type ft = visit(node.getFrom());
        Type tt = visit(node.getTo());
        if(!requireNoneVoid(ft, node)) return getDefaultType();
        if(!requireNoneVoid(tt, node)) return getDefaultType();
        if(!ft.equals(tt)){
            ExprNode from = checkAssign(node.getFrom(), ft, tt, node); 
            if(from==null) return getDefaultType();            
            node.setFrom(from);
        }
        return tt;
    }
    
    @Nullable
    public static PrimitiveType getPrimitiveType(Type t){
        if(t instanceof PrimitiveType){
            return (PrimitiveType) t;
        }else if(t instanceof ClassType){
            return Types.getPrimitiveType((ClassType)t);
        }else{
            return null;
        }
    }

    public static PrimitiveType getMathType(Type t1, Type t2, String op) {
        PrimitiveType pt1= getPrimitiveType(t1);
        PrimitiveType pt2 = getPrimitiveType(t2);
        if(pt1==null){
            throw new IllegalArgumentException(t1.getName());
        }
        if(pt2==null) throw new IllegalArgumentException(t2.getName());
        String ret = MathType.getType(pt1.getName(), pt2.getName(), op);
        return Types.getPrimitiveType(ret);
    }

    @Override
    public Type visitBinaryExpr(BinaryExpr node) {
        Type t1 = visit(node.getExpr1());
        Type t2 = visit(node.getExpr2());
        String op = node.getOperation();
        Type t;
        switch (op) {
            case "==":
            case "!=":
                if (Types.isNumber(t1)) {
                    if (!Types.isNumber(t2)) {
                        err.failedToCast(node, t2.getName(), Types.INT_CLASS_TYPE.getName());
                        return getDefaultType();
                    }
                } else {
                    //TODO pass anything.may be Object needed?
                }
                t = Types.BOOLEAN_TYPE;
                break;
            case "+":
                if(isNumber(t1) && isNumber(t2)){
                    t = getMathType(t1, t2, op);
                }else{
                    node.setExpr1(checkAssign(node.getExpr1(), t1, Types.STRING_CLASS_TYPE, node));
                    node.setExpr2(checkAssign(node.getExpr2(), t2, Types.STRING_CLASS_TYPE, node));
                    t =Types.STRING_CLASS_TYPE;
                }
                break;
            case "-":
            case "*":
            case "/":
            case "%":
                if(!requireNumber(node, t1)) return getDefaultType();
                if(!requireNumber(node, t2)) return getDefaultType();
                t = (getMathType(t1, t2, op));
                break;
            case ">=":
            case "<=":
            case ">":
            case "<":
                if(!requireNumber(node, t1)) return getDefaultType();
                if(!requireNumber(node, t2)) return getDefaultType();
                t = Types.BOOLEAN_TYPE;
                break;
            case "&&":
            case "||":
                if(!requireBoolean(node, t1)) return getDefaultType();
                if(!requireBoolean(node, t2)) return getDefaultType();
                t = Types.BOOLEAN_TYPE;
                break;
            case "&":
            case "|":
            case "^":
            case BinaryExpr.OP_SHIFT_LEFT:
            case BinaryExpr.OP_SHIFT_RIGHT:
                if(!requireNumber(node, t1)) return getDefaultType();
                if(!requireNumber(node, t2)) return getDefaultType();
                t = getPrimitiveType(Types.getHigherType(t1, t2));
                break;
            default:
                err.fail("unsupport operation:" + op, SemanticError.UNSUPPORTED, node);
                return getDefaultType();
        }
        return t;
    }

    @Override
    public Type visitElementExpr(ElementExpr node) {
        super.visitElementExpr(node);
        requireArray(node, node.getArrayExpr().getType());
        return null;
    }

//    @Override
//    public Type visitFieldExpr(FieldExpr node) {
//        super.visitFieldExpr(node);
//        if (isStatic(method.modifier)) {
//            FieldNode field = node.getField();
//            requireStatic(field.modifier, node);
//        }
//        return null;
//    }

    @Override
    public Type visitInvocationExpr(InvocationExpr node) {
        super.visitInvocationExpr(node);
       MethodNode invokeMethod = node.getMethod();
//        boolean inStaticMethod = Modifier.isStatic(node.getMethod().modifier) && Modifier.isStatic(this.method.modifier);
//        if (inStaticMethod) {
//            if(!requireStatic(invokeMethod.modifier, node)) return getDefaultType();
//        }
        for(Type et:invokeMethod.exceptionTypes){
            this.exceptionStack.peek().put(et,node);
        }
        return invokeMethod.type;
    }

    @Override
    public Type visitUnaryExpr(UnaryExpr node) {
        super.visitUnaryExpr(node);
        String op = node.getOperation();
        Type et = node.getExpr().getType();
        switch(op){
            case UnaryExpr.OPERATION_LOGIC_NOT:
                requireBoolean(node, et);
                break;
            case UnaryExpr.OPERATION_NEG:
            case UnaryExpr.OPERATION_POS:
            case UnaryExpr.OPERATION_NOT:
                requireNumber(node, et);
        }
        return et;
    }

    private void caughException(Type type) {
        Map<Type, AstNode> exceptions = this.exceptionStack.peek();
        Type[] exTypes = exceptions.keySet().toArray(new Type[0]);
        for (Type e : exTypes) {
                if (
                        e.equals(type)
                        || e.isSubTypeOf(type)
                        ) {
                    exceptions.remove(e);
                }
        }
    }

    @Override
    public Type visitTryStmt(TryStmt node) {
        this.exceptionStack.add(new HashMap<>());        
        visit(node.getExecStmt());
        boolean tryReturned = this.returned;
        for(CatchBlock cs:node.getCatchStmts()){
            this.returned = false;
            visit(cs);
            tryReturned = tryReturned && this.returned;
        }
        Map<Type, AstNode> uncaught = this.exceptionStack.pop();
        if (uncaught.size() > 0) {
            this.exceptionStack.peek().putAll(uncaught);
        }
        returned = false;
        visit(node.getFinallyStmt());
        this.returned = tryReturned || returned;
        return null;
    }

    @Override
    public Type visitCatchBlock(CatchBlock node) {
        this.caughException(node.catchVar.type);
        return super.visitCatchBlock(node);
    }

    @Override
    public Type visitFieldNode(FieldNode fieldNode) {
        return checkVarDecl(fieldNode);
    }

    @Override
    public Type visitLocalVarNode(LocalVarNode localVarNode) {
        return checkVarDecl(localVarNode);
    }

    private Type checkVarDecl(VarObject var) {
        Type retType = null;
        if(var.initExpr!=null){
            retType = visit(var.initExpr);
            if(!requireNoneVoid(retType, var)) return getDefaultType();
        }
        if (var.type == null) {
            if(retType!=null){
                var.type = retType;
            }else{
                var.type = Types.ROOT_TYPE;
            }
        }
        if(retType!=null){
            var.initExpr = checkAssign(var.initExpr, retType, var.type, var);
        }
        return var.type;
    }

    @Override
    public Type visitIfStmt(IfStmt node) {
        if(!requireBoolean(node.getConditionExpr())) return getDefaultType();
        if (node.getTrueBody() != null) {
            visit(node.getTrueBody());
        }
        boolean returnedOld = returned;
        returned = false;
        if (node.getFalseBody() != null) {
            visit(node.getFalseBody());
        } else {
            returned = false;
        }
        returned = returnedOld && returned;
        return null;
    }

    @Override
    public Type visitLoopStmt(LoopStmt node) {
        if (node.preConditionExpr != null) {
            requireBoolean(node.preConditionExpr);
        }
        if (node.initStmts != null) {
            visitAll(node.initStmts);
        }
        if (node.loopBody != null) {
            visit(node.loopBody);
        }
        if (node.postConditionExpr != null) {
            requireBoolean(node.postConditionExpr);
        }
        return null;
    }

    @Override
    public Type visitMethodNode(MethodNode node) {
        String mStr = AstUtil.getMethodDescriptor(node);
        if (methodDeclared.contains(mStr)) {
            err.unsupported("declare method duplicately:"+mStr, node);
            return getDefaultType();
        }
        methodDeclared.add(mStr);
        method = node;
        returned = false;
        this.exceptionStack.push(new HashMap<>());
        super.visitMethodNode(node);
        if (method.exceptionTypes != null) {
            for (Type e : method.exceptionTypes) {
                this.caughException(e);
            }
        }
        Map<Type, AstNode> uncaught = this.exceptionStack.pop();
        for(Type k:uncaught.keySet()){
            err.uncaughtException(uncaught.get(k),k.getName());
        }
        boolean needReturn = (
            node.type != null
            && !node.type.equals(Types.VOID_TYPE)
        );
        if (node.body != null && needReturn && !returned) {
            err.fail("Missing return statement in method:" + mStr, SemanticError.LACKS_OF_STATEMENT, node);
        }
        return null;
    }

    @Override
    public Type visitReturnStmt(ReturnStmt node) {
        Type retType = method.type;
        //this.checkCastable(visit(node.expr),retType,node)
        if (node.expr != null) {
            Type exType = visit(node.expr);
            node.expr = this.checkAssign(node.expr, exType, retType, node);
        }
        returned = true;
        return null;
    }

    boolean requireNumber(AstNode node, Type t) {
        if (!isNumber(t)) {
            err.failedToCast(node, t.getName(),Types.INT_CLASS_TYPE.getName() );
            return false;
        }
        return true;
    }

    boolean requireBoolean(ExprNode node) {
        visit(node);
        return requireBoolean(node, node.getType());
    }

    boolean requireBoolean(AstNode node, Type t) {
        if (!Types.isBoolean(t)) {
            err.failedToCast(node, t.getName(), Types.BOOLEAN_CLASS_TYPE.getName());
            return false;
        }
        return true;
    }

    boolean isArray(Type t) {
        return t.isArray();
    }

    boolean requireArray(AstNode node, Type t) {
        if (!isArray(t)) {
            err.failedToCast(node, t.getName(), "array");
            return false;
        }
        return true;
    }

    public boolean isStatic(int modifier) {
        return AstUtil.isStatic(modifier);
    }

    boolean requireStatic(Integer modifier, AstNode node) {
        boolean isStatic = isStatic(modifier);
        if (!isStatic) {
            err.fail("couldn't refer non-static member in static context", SemanticError.UNSUPPORTED, node);
            return false;
        }
        return true;
    }

    boolean requireNoneVoid(Type type, AstNode node) {
        if (type == null
                || type == Types.VOID_TYPE
                || type == Types.VOID_CLASS_TYPE
                ){
            err.unsupported("use void type as value", node);
            return false;
        }
        return true;
    }

    private boolean isNumber(Type t1) {
        return Types.isNumber(t1);
    }

    public AstLoader getAstLoader() {
        return astLoader;
    }

    @Override
    public Type visitParameterNode(ParameterNode parameterNode) {
        return checkVarDecl(parameterNode);
    }

    @Override
    public Type visitThrowStmt(ThrowStmt node) {
        Type ret = super.visitThrowStmt(node);
        returned = true;
        return ret;
    }

    @Override
    public Type visitUnknownInvocationExpr(UnknownInvocationExpr node) {
        String type = "";
        AstNode target = node.getTarget();
        if(target instanceof ExprNode){
            type =((ExprNode) target).getType().getName();
        }else if(target instanceof ClassReference){
            type = ((ClassReference)target).getReferencedClassNode().name;
        }
        err.methodNotFound(node,type,node.getMethodName(),AstUtil.getExprTypes(node.getArguments()));
        return getDefaultType();
    }

    @Override
    public Type visitUnknownFieldExpr(UnknownFieldExpr node) {
        err.fieldNotFound(node, node.getFieldName());
        return getDefaultType();
    }

    @Override
    public Type visitErrorousExpr(ErrorousExpr node) {
        err.fail("not an expression",0, node);
        return null;
    }
    
    protected void validateAnnotation(AnnotationNode[] annotation){
        for(AnnotationNode an:annotation) validateAnnotation(an);
    }
    
    protected void validateAnnotation(AnnotationNode annotation){
        MethodNode[] mds = annotation.getAnnotationType().getDeclaredMethodNodes();
        Set<String> attrKeys = annotation.values.keySet();
        List<String> missingValues = new LinkedList<>();
        for(MethodNode m:mds){
            String name = m.name;
            if(!attrKeys.contains(name)){
                missingValues.add(name);
            }
        }
        if(missingValues.size()>0){
            err.fail("Missing attribute for annotation:" + missingValues.toString(), -1, clazz);
        }
    }

}