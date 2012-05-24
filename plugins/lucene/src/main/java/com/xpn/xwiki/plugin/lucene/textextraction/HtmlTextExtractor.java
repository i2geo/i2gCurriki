package com.xpn.xwiki.plugin.lucene.textextraction;


import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.io.InputStream;
import java.io.StringReader;
import java.net.ContentHandler;

import org.cyberneko.html.parsers.SAXParser;

import org.xml.sax.SAXException;
import org.xml.sax.DocumentHandler;
import org.xml.sax.HandlerBase;
import org.xml.sax.helpers.DefaultHandler;
import org.cyberneko.html.HTMLScanner;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.impl.xs.opti.DefaultXMLDocumentHandler;

/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Extracts all text between tags in an XML document. Only works (of course) for well formed XML
 * documents.
 */
public class HtmlTextExtractor implements MimetypeTextExtractor
{
    public String getText(byte[] data) throws Exception
    {
        return getText(new ByteArrayInputStream(data));
    }

    public String getText(InputStream in) throws Exception {
        SAXParser saxParser = new SAXParser();
        Handler h = new Handler();
        saxParser.setDocumentHandler(h);
        XMLInputSource source = new XMLInputSource(null,null,null);
        source.setEncoding("utf-8");
        source.setByteStream(in);
        saxParser.parse(source);
        return h.getText();
    }


    protected static class Handler extends HandlerBase
    {
        StringBuffer text = new StringBuffer();

        public void characters(char ch[], int start, int length) throws SAXException
        {
            text.append(ch, start, length);
        }

        public String getText()
        {
            return text.toString();
        }
    }
}
