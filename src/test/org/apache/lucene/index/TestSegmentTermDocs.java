package org.apache.lucene.index;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache Lucene" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Lucene", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

import junit.framework.TestCase;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import java.io.IOException;

public class TestSegmentTermDocs extends TestCase {
  private Document testDoc = new Document();
  private Directory dir = new RAMDirectory();

  public TestSegmentTermDocs(String s) {
    super(s);
  }

  protected void setUp() {
    DocHelper.setupDoc(testDoc);
    DocHelper.writeDoc(dir, testDoc);
  }


  protected void tearDown() {

  }

  public void test() {
    assertTrue(dir != null);
  }
  
  public void testTermDocs() {
    try {
      //After adding the document, we should be able to read it back in
      SegmentReader reader = new SegmentReader(new SegmentInfo("test", 1, dir));
      assertTrue(reader != null);
      SegmentTermDocs segTermDocs = new SegmentTermDocs(reader);
      assertTrue(segTermDocs != null);
      segTermDocs.seek(new Term(DocHelper.TEXT_FIELD_2_KEY, "field"));
      if (segTermDocs.next() == true)
      {
        int docId = segTermDocs.doc();
        assertTrue(docId == 0);
        int freq = segTermDocs.freq();
        assertTrue(freq == 3);  
      }
      reader.close();
    } catch (IOException e) {
      assertTrue(false);
    }
  }  
  
  public void testBadSeek() {
    try {
      //After adding the document, we should be able to read it back in
      SegmentReader reader = new SegmentReader(new SegmentInfo("test", 3, dir));
      assertTrue(reader != null);
      SegmentTermDocs segTermDocs = new SegmentTermDocs(reader);
      assertTrue(segTermDocs != null);
      segTermDocs.seek(new Term("textField2", "bad"));
      assertTrue(segTermDocs.next() == false);
      reader.close();
    } catch (IOException e) {
      assertTrue(false);
    }
    try {
      //After adding the document, we should be able to read it back in
      SegmentReader reader = new SegmentReader(new SegmentInfo("test", 3, dir));
      assertTrue(reader != null);
      SegmentTermDocs segTermDocs = new SegmentTermDocs(reader);
      assertTrue(segTermDocs != null);
      segTermDocs.seek(new Term("junk", "bad"));
      assertTrue(segTermDocs.next() == false);
      reader.close();
    } catch (IOException e) {
      assertTrue(false);
    }
  }    
}