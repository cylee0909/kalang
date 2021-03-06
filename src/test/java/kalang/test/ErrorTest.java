package kalang.test;

import static org.junit.Assert.*;

import org.junit.Test;
import kalang.compiler.*;
import kalang.compiler.KalangCompiler;
//import kalang.MainCompiler as KC
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import kalang.compiler.SemanticError;// as E;
//import kalang.tool.MainCompiler as TC;
public class ErrorTest {
	
    String outDir = "TestScript/generatedCode";
    String srcDir = "antlr/TestScript";
    String errSrcDir = "TestScript/error_src";
	
    int eCode;
	
    String errMsg;
	
    KalangCompiler kc;

    CompileErrorHandler allowErrorHandler = (e) -> {
        if(e instanceof SemanticError){
            SemanticError error = (SemanticError) e;
            System.out.println("on error:" + error.getDescription());
            eCode = error.getErrorCode();
            errMsg =error.getDescription();
            kc.setCompileTargetPhase(CompilePhase.PHASE_SEMANTIC);
        }else if(e instanceof SyntaxError){
            SyntaxError error = (SyntaxError) e;
            String errorToken =  error.getStart().getText();
            System.err.println("syntax error on token:" + errorToken);
        }else{
            System.err.println(e.getDescription());
        }
    };
    
    CompileErrorHandler strictErrorHandler = new CompileErrorHandler() {

        @Override
        public void handleCompileError(CompileError error) {
            fail(error.getDescription());
        }
    };
	
    private void compile(CompileErrorHandler compileErrorHandler,String dir,String...name) throws IOException{
        eCode = -1;
        kc = new KalangCompiler();
        if(compileErrorHandler!=null){
            kc.setCompileErrorHandler(compileErrorHandler);
        }
        for(String n : name){
            File src = new File(dir,n+".kl");//.readLines().join("\r\n");
            String source = FileUtils.readFileToString(src);
            kc.addSource(n,source,n+".kl");
        }
        kc.compile();
        //
    }
	
    private void cp(String... name) throws IOException{
        this.errMsg = null;
        compile(strictErrorHandler,srcDir,name);
    }
	
    private void ecp(String... name) throws IOException{
        compile(allowErrorHandler,errSrcDir,name);
    }
	
    @Test
    public void errorTest() throws IOException {
        //throw new RuntimeException("tt")
        //cp "NotImplemented"
        ecp("SyntaxError");
		
        ecp("ErrorAssign");
        assertEquals(eCode , SemanticError.UNABLE_TO_CAST);
        //TODO catch sematic error
        //ecp("NotImplemented");
        //assertEquals( eCode , SemanticError.CLASS_NOT_FOUND);
        ecp("NotImplemented","MyFace");
        assertEquals(eCode , SemanticError.METHOD_NOT_IMPLEMENTED);
    }
	
    @Test
    public void test() throws IOException{
        //cp("TestInput")
        cp("Base");
        //cp("HelloWorld","MyInterface")
        //cp "kava"
    }
	
    @Test
    public void toolTest() throws IOException{
        //kalang.tool.MainCompiler.main(new String[]{this.errSrcDir,outDir});
    }
	

}
