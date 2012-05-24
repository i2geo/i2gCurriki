package org.curriki.xwiki.plugin.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.CharTokenizer;

import java.io.Reader;
import java.io.IOException;
import java.io.StringReader;

/** An analyzer that splits along a whitespace or a comma.
 */
class IdentifierListAnalyzer extends Analyzer {

    public IdentifierListAnalyzer() {
    }


    @Override
    public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
        TokenStream tok = (TokenStream) getPreviousTokenStream();
        if(tok==null) {
            tok = tokenStream(fieldName,reader);
            setPreviousTokenStream(tok);
        }
        return tok;
    }

    public TokenStream tokenStream(String fieldName, Reader reader) {
        return new CharTokenizer(reader) {
                protected boolean isTokenChar(char c) {
                    return !( Character.isWhitespace(c) || c == ',');
                }
            };
    }
}
