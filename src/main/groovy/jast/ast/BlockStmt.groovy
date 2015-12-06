/*
Don't modify!This file is generated automately.
*/
package jast.ast;
import java.util.*;
public class BlockStmt extends Statement{
    
    public List<Statement> statements;
    
    
    
    public BlockStmt(List<Statement> statements=null){
        
            if(statements == null) statements = new LinkedList();
        
        
            this.statements = statements;
        
    }
    
    
    public static BlockStmt create(){
        BlockStmt node = new BlockStmt();
        
        node.statements = new LinkedList();
        
        return node;
    }
    
    private void addChild(List<AstNode> list,List nodes){
        if(nodes!=null) list.addAll(nodes);
    }
    
    private void addChild(List<AstNode> list,AstNode node){
        if(node!=null) list.add(node);
    }
    
    public List<AstNode> getChildren(){
        List<AstNode> ls = new LinkedList();
        
        addChild(ls,statements);
        
        return ls;
    }
    
    public String toString(){
        String str = "BlockStmt{\r\n";
        
        str += "  statements:" + statements.toString()+"\r\n";
        
        return str+"}";
    }
}