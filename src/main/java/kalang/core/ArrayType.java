
package kalang.core;
import kalang.ast.ExprNode;
import kalang.ast.MethodNode;
import kalang.ast.VarObject;
import java.io.*;
import java.lang.reflect.Modifier;
import java.nio.*;
import java.net.*;
import java.util.*;
import kalang.ast.FieldNode;
import kalang.compiler.AstLoader;
/**
 *
 * @author Kason Yang <i@kasonyang.com>
 */
public class ArrayType extends ClassType{

    private Type componentType;

    public ArrayType(Type componentType) {
        //TODO should not hard code
        super(AstLoader.createArrayAst("java.lang.Object"));
        this.componentType = componentType;
    }
    
    @Override
    public String getName() {
        return componentType.getName() + "[]";
    }

    @Override
    public boolean isPrimitiveType() {
        return componentType.isPrimitiveType();
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public Type getComponentType() {
        return componentType;
    }

    @Override
    public FieldNode[] getFields() {
        return new FieldNode[0];
    }

    @Override
    public MethodNode[] getMethods() {
        return new MethodNode[0];
    }

//    @Override
//    public boolean isSubTypeOf(Type targetType) {
//        if(targetType instanceof ArrayType){
//            return componentType.isSubTypeOf(targetType);
//        }
//        return false;
//    }

    @Override
    public boolean isAssignedFrom(Type type) {
        if(super.isAssignedFrom(type)) return true;
        if(type instanceof ArrayType){
            return componentType.isAssignedFrom(((ArrayType)type).getComponentType());
        }
        return false;
    }

    @Override
    public boolean isSubTypeOf(Type targetType) {
        if(targetType.equals(superType)) return true;
        if(targetType instanceof ArrayType){
            return componentType.isSubTypeOf(((ArrayType)targetType).componentType);
        }
        return false;
    }


}
