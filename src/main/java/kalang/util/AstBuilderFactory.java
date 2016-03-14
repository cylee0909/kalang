
package kalang.util;
import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;
import kalang.antlr.KalangLexer;
import kalang.antlr.KalangParser;
import kalang.compiler.AstBuilder;
import kalang.compiler.KalangSource;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
/**
 *
 * @author Kason Yang <i@kasonyang.com>
 */
public class AstBuilderFactory {
    
    public static AstBuilder createAstBuilder(KalangSource source,KalangLexer lexer){
        return AstBuilderFactory.createAstBuilder(
                source
                ,TokenStreamFactory.createTokenStream(lexer)
        );
    }
    
    public static AstBuilder createAstBuilder(KalangSource source){
        return AstBuilderFactory.createAstBuilder(
                source
                ,TokenStreamFactory.createTokenStream(source.getText())
        );
    }
        
    public static AstBuilder createAstBuilder(KalangSource source,TokenStream tokens){
        KalangParser p = new KalangParser(tokens);
        AstBuilder sp = new AstBuilder(source, p);
        p.setErrorHandler(new DefaultErrorStrategy() {

            @Override
            public void reportError(Parser recognizer, RecognitionException e) {
                String msg = AntlrErrorString.exceptionString(recognizer, e);
                RuleContext ctx = e.getCtx();
                if(ctx instanceof ParserRuleContext){
                    sp.reportSyntaxError(msg, (ParserRuleContext) e.getCtx(), e.getOffendingToken());
                }else{
                    sp.reportSyntaxError(msg, RuleContext.EMPTY, e.getOffendingToken());
                }
            }
        });
        return sp;
    }

}