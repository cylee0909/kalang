package kalang.test;

import kalang.antlr.KalangLexer;
import kalang.antlr.KalangParser;
import kalang.util.TokenStreamFactory;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.runtime.misc.Pair;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Kason Yang <i@kasonyang.com>
 */
public class ErrorRecoverTest {
    
    public ErrorRecoverTest() {
    }
    
    @Test
    public void test(){
        testCode("class{ void test(){  String s;s;  }}");
        testCode("class{ void test(){  String s;s  }}");
        testCode("class{ void test(){  String s;s.  }}");
        testCode("class{ void test(){  String s;test().  }}");
    }
    public void testCode(String code){
        KalangParser parser = new KalangParser(TokenStreamFactory.createTokenStream(code));
        parser.setErrorHandler(new DefaultErrorStrategy(){
            @Override
            public void recover(Parser recognizer, RecognitionException e) {
                Token ot = e.getOffendingToken();
                System.out.println("offending token:" + ot.getText());
                IntervalSet exceptedTokens = e.getExpectedTokens();
                RuleContext ctx = e.getCtx();
                if(ctx!=null){
                    System.out.println("context:" + ctx.getClass().getName());
                }
                System.out.println("offending state:" + e.getOffendingState());
                String excTks = exceptedTokens.toString(KalangLexer.VOCABULARY);
                System.out.println("excepted:" + excTks);
                super.recover(recognizer, e);
            }
            
            
            
            @Override
            public Token recoverInline(Parser recognizer) throws RecognitionException {
                System.out.println("calling recover inline");
//                IntervalSet exceptedTokens = recognizer.getExpectedTokens();
//                if(exceptedTokens.contains(KalangLexer.SEMI)){
//                    Token curToken = recognizer.getCurrentToken();
//                    recognizer.getTokenFactory().create(
//                            new Pair(
//                                    curToken.getTokenSource()
//                                    ,
//                            ), lastErrorIndex, text, lastErrorIndex, lastErrorIndex, lastErrorIndex, lastErrorIndex, lastErrorIndex);
//                }
                Token v = super.recoverInline(recognizer);
                if(v!=null)
                    System.out.println("inserted token:" + v.getText());
                return v;
            }
        });
        parser.compilantUnit();
    }
    
}
