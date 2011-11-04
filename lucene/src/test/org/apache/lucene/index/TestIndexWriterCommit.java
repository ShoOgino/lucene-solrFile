package org.apache.lucene.index;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.analysis.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MockDirectoryWrapper;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util._TestUtil;

public class TestIndexWriterCommit extends LuceneTestCase {
  /*
   * Simple test for "commit on close": open writer then
   * add a bunch of docs, making sure reader does not see
   * these docs until writer is closed.
   */
  public void testCommitOnClose() throws IOException {
      Directory dir = newDirectory();
      IndexWriter writer = new IndexWriter(dir, newIndexWriterConfig( TEST_VERSION_CURRENT, new MockAnalyzer(random)));
      for (int i = 0; i < 14; i++) {
        TestIndexWriter.addDoc(writer);
      }
      writer.close();

      Term searchTerm = new Term("content", "aaa");
      IndexSearcher searcher = new IndexSearcher(dir, false);
      ScoreDoc[] hits = searcher.search(new TermQuery(searchTerm), null, 1000).scoreDocs;
      assertEquals("first number of hits", 14, hits.length);
      searcher.close();

      IndexReader reader = IndexReader.open(dir, true);

      writer = new IndexWriter(dir, newIndexWriterConfig( TEST_VERSION_CURRENT, new MockAnalyzer(random)));
      for(int i=0;i<3;i++) {
        for(int j=0;j<11;j++) {
          TestIndexWriter.addDoc(writer);
        }
        searcher = new IndexSearcher(dir, false);
        hits = searcher.search(new TermQuery(searchTerm), null, 1000).scoreDocs;
        assertEquals("reader incorrectly sees changes from writer", 14, hits.length);
        searcher.close();
        assertTrue("reader should have still been current", reader.isCurrent());
      }

      // Now, close the writer:
      writer.close();
      assertFalse("reader should not be current now", reader.isCurrent());

      searcher = new IndexSearcher(dir, false);
      hits = searcher.search(new TermQuery(searchTerm), null, 1000).scoreDocs;
      assertEquals("reader did not see changes after writer was closed", 47, hits.length);
      searcher.close();
      reader.close();
      dir.close();
  }

  /*
   * Simple test for "commit on close": open writer, then
   * add a bunch of docs, making sure reader does not see
   * them until writer has closed.  Then instead of
   * closing the writer, call abort and verify reader sees
   * nothing was added.  Then verify we can open the index
   * and add docs to it.
   */
  public void testCommitOnCloseAbort() throws IOException {
    MockDirectoryWrapper dir = newDirectory();
    IndexWriter writer = new IndexWriter(dir, newIndexWriterConfig( TEST_VERSION_CURRENT, new MockAnalyzer(random)).setMaxBufferedDocs(10));
    for (int i = 0; i < 14; i++) {
      TestIndexWriter.addDoc(writer);
    }
    writer.close();

    Term searchTerm = new Term("content", "aaa");
    IndexSearcher searcher = new IndexSearcher(dir, false);
    ScoreDoc[] hits = searcher.search(new TermQuery(searchTerm), null, 1000).scoreDocs;
    assertEquals("first number of hits", 14, hits.length);
    searcher.close();

    writer = new IndexWriter(dir, newIndexWriterConfig( TEST_VERSION_CURRENT, new MockAnalyzer(random))
      .setOpenMode(OpenMode.APPEND).setMaxBufferedDocs(10));
    for(int j=0;j<17;j++) {
      TestIndexWriter.addDoc(writer);
    }
    // Delete all docs:
    writer.deleteDocuments(searchTerm);

    searcher = new IndexSearcher(dir, false);
    hits = searcher.search(new TermQuery(searchTerm), null, 1000).scoreDocs;
    assertEquals("reader incorrectly sees changes from writer", 14, hits.length);
    searcher.close();

    // Now, close the writer:
    writer.rollback();

    TestIndexWriter.assertNoUnreferencedFiles(dir, "unreferenced files remain after rollback()");

    searcher = new IndexSearcher(dir, false);
    hits = searcher.search(new TermQuery(searchTerm), null, 1000).scoreDocs;
    assertEquals("saw changes after writer.abort", 14, hits.length);
    searcher.close();

    // Now make sure we can re-open the index, add docs,
    // and all is good:
    writer = new IndexWriter(dir, newIndexWriterConfig( TEST_VERSION_CURRENT, new MockAnalyzer(random))
      .setOpenMode(OpenMode.APPEND).setMaxBufferedDocs(10));

    // On abort, writer in fact may write to the same
    // segments_N file:
    dir.setPreventDoubleWrite(false);

    for(int i=0;i<12;i++) {
      for(int j=0;j<17;j++) {
        TestIndexWriter.addDoc(writer);
      }
      searcher = new IndexSearcher(dir, false);
      hits = searcher.search(new TermQuery(searchTerm), null, 1000).scoreDocs;
      assertEquals("reader incorrectly sees changes from writer", 14, hits.length);
      searcher.close();
    }

    writer.close();
    searcher = new IndexSearcher(dir, false);
    hits = searcher.search(new TermQuery(searchTerm), null, 1000).scoreDocs;
    assertEquals("didn't see changes after close", 218, hits.length);
    searcher.close();

    dir.close();
  }

  /*
   * Verify that a writer with "commit on close" indeed
   * cleans up the temp segments created after opening
   * that are not referenced by the starting segments
   * file.  We check this by using MockDirectoryWrapper to
   * measure max temp disk space used.
   */
  public void testCommitOnCloseDiskUsage() throws IOException {
    // MemoryCodec, since it uses FST, is not necessarily
    // "additive", ie if you add up N small FSTs, then merge
    // them, the merged result can easily be larger than the
    // sum because the merged FST may use array encoding for
    // some arcs (which uses more space):

    final String idFormat = _TestUtil.getPostingsFormat("id");
    final String contentFormat = _TestUtil.getPostingsFormat("content");
    assumeFalse("This test cannot run with Memory codec", idFormat.equals("Memory") || contentFormat.equals("Memory"));
    MockDirectoryWrapper dir = newDirectory();
    Analyzer analyzer;
    if (random.nextBoolean()) {
      // no payloads
     analyzer = new Analyzer() {
        @Override
        public TokenStreamComponents createComponents(String fieldName, Reader reader) {
          return new TokenStreamComponents(new MockTokenizer(reader, MockTokenizer.WHITESPACE, true));
        }
      };
    } else {
      // fixed length payloads
      final int length = random.nextInt(200);
      analyzer = new Analyzer() {
        @Override
        public TokenStreamComponents createComponents(String fieldName, Reader reader) {
          Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
          return new TokenStreamComponents(tokenizer, new MockFixedLengthPayloadFilter(random, tokenizer, length));
        }
      };
    }
    
    IndexWriter writer  = new IndexWriter(
        dir,
        newIndexWriterConfig( TEST_VERSION_CURRENT, analyzer).
            setMaxBufferedDocs(10).
            setReaderPooling(false).
            setMergePolicy(newLogMergePolicy(10))
    );
    for(int j=0;j<30;j++) {
      TestIndexWriter.addDocWithIndex(writer, j);
    }
    writer.close();
    dir.resetMaxUsedSizeInBytes();

    dir.setTrackDiskUsage(true);
    long startDiskUsage = dir.getMaxUsedSizeInBytes();
    writer = new IndexWriter(
        dir,
        newIndexWriterConfig( TEST_VERSION_CURRENT, analyzer)
            .setOpenMode(OpenMode.APPEND).
            setMaxBufferedDocs(10).
            setMergeScheduler(new SerialMergeScheduler()).
            setReaderPooling(false).
            setMergePolicy(newLogMergePolicy(10))

    );
    for(int j=0;j<1470;j++) {
      TestIndexWriter.addDocWithIndex(writer, j);
    }
    long midDiskUsage = dir.getMaxUsedSizeInBytes();
    dir.resetMaxUsedSizeInBytes();
    writer.optimize();
    writer.close();

    IndexReader.open(dir, true).close();

    long endDiskUsage = dir.getMaxUsedSizeInBytes();

    // Ending index is 50X as large as starting index; due
    // to 3X disk usage normally we allow 150X max
    // transient usage.  If something is wrong w/ deleter
    // and it doesn't delete intermediate segments then it
    // will exceed this 150X:
    // System.out.println("start " + startDiskUsage + "; mid " + midDiskUsage + ";end " + endDiskUsage);
    assertTrue("writer used too much space while adding documents: mid=" + midDiskUsage + " start=" + startDiskUsage + " end=" + endDiskUsage + " max=" + (startDiskUsage*150),
               midDiskUsage < 150*startDiskUsage);
    assertTrue("writer used too much space after close: endDiskUsage=" + endDiskUsage + " startDiskUsage=" + startDiskUsage + " max=" + (startDiskUsage*150),
               endDiskUsage < 150*startDiskUsage);
    dir.close();
  }


  /*
   * Verify that calling optimize when writer is open for
   * "commit on close" works correctly both for rollback()
   * and close().
   */
  public void testCommitOnCloseOptimize() throws IOException {
    MockDirectoryWrapper dir = newDirectory();
    // Must disable throwing exc on double-write: this
    // test uses IW.rollback which easily results in
    // writing to same file more than once
    dir.setPreventDoubleWrite(false);
    IndexWriter writer = new IndexWriter(
        dir,
        newIndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer(random)).
            setMaxBufferedDocs(10).
            setMergePolicy(newLogMergePolicy(10))
    );
    for(int j=0;j<17;j++) {
      TestIndexWriter.addDocWithIndex(writer, j);
    }
    writer.close();

    writer  = new IndexWriter(dir, newIndexWriterConfig( TEST_VERSION_CURRENT, new MockAnalyzer(random)).setOpenMode(OpenMode.APPEND));
    writer.optimize();

    if (VERBOSE) {
      writer.setInfoStream(System.out);
    }

    // Open a reader before closing (commiting) the writer:
    IndexReader reader = IndexReader.open(dir, true);

    // Reader should see index as unoptimized at this
    // point:
    assertFalse("Reader incorrectly sees that the index is optimized", reader.isOptimized());
    reader.close();

    // Abort the writer:
    writer.rollback();
    TestIndexWriter.assertNoUnreferencedFiles(dir, "aborted writer after optimize");

    // Open a reader after aborting writer:
    reader = IndexReader.open(dir, true);

    // Reader should still see index as unoptimized:
    assertFalse("Reader incorrectly sees that the index is optimized", reader.isOptimized());
    reader.close();

    if (VERBOSE) {
      System.out.println("TEST: do real optimize");
    }
    writer = new IndexWriter(dir, newIndexWriterConfig( TEST_VERSION_CURRENT, new MockAnalyzer(random)).setOpenMode(OpenMode.APPEND));
    if (VERBOSE) {
      writer.setInfoStream(System.out);
    }
    writer.optimize();
    writer.close();

    if (VERBOSE) {
      System.out.println("TEST: writer closed");
    }
    TestIndexWriter.assertNoUnreferencedFiles(dir, "aborted writer after optimize");

    // Open a reader after aborting writer:
    reader = IndexReader.open(dir, true);

    // Reader should still see index as unoptimized:
    assertTrue("Reader incorrectly sees that the index is unoptimized", reader.isOptimized());
    reader.close();
    dir.close();
  }
  
  // LUCENE-2095: make sure with multiple threads commit
  // doesn't return until all changes are in fact in the
  // index
  public void testCommitThreadSafety() throws Throwable {
    final int NUM_THREADS = 5;
    final double RUN_SEC = 0.5;
    final Directory dir = newDirectory();
    final RandomIndexWriter w = new RandomIndexWriter(random, dir, newIndexWriterConfig(
                                                                                        TEST_VERSION_CURRENT, new MockAnalyzer(random)).setMergePolicy(newLogMergePolicy()));
    _TestUtil.reduceOpenFiles(w.w);
    w.commit();
    final AtomicBoolean failed = new AtomicBoolean();
    Thread[] threads = new Thread[NUM_THREADS];
    final long endTime = System.currentTimeMillis()+((long) (RUN_SEC*1000));
    for(int i=0;i<NUM_THREADS;i++) {
      final int finalI = i;
      threads[i] = new Thread() {
          @Override
          public void run() {
            try {
              final Document doc = new Document();
              IndexReader r = IndexReader.open(dir);
              Field f = newField("f", "", StringField.TYPE_UNSTORED);
              doc.add(f);
              int count = 0;
              do {
                if (failed.get()) break;
                for(int j=0;j<10;j++) {
                  final String s = finalI + "_" + String.valueOf(count++);
                  f.setValue(s);
                  w.addDocument(doc);
                  w.commit();
                  IndexReader r2 = IndexReader.openIfChanged(r);
                  assertNotNull(r2);
                  assertTrue(r2 != r);
                  r.close();
                  r = r2;
                  assertEquals("term=f:" + s + "; r=" + r, 1, r.docFreq(new Term("f", s)));
                }
              } while(System.currentTimeMillis() < endTime);
              r.close();
            } catch (Throwable t) {
              failed.set(true);
              throw new RuntimeException(t);
            }
          }
        };
      threads[i].start();
    }
    for(int i=0;i<NUM_THREADS;i++) {
      threads[i].join();
    }
    assertFalse(failed.get());
    w.close();
    dir.close();
  }

  // LUCENE-1044: test writer.commit() when ac=false
  public void testForceCommit() throws IOException {
    Directory dir = newDirectory();

    IndexWriter writer = new IndexWriter(
        dir,
        newIndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer(random)).
            setMaxBufferedDocs(2).
            setMergePolicy(newLogMergePolicy(5))
    );
    writer.commit();

    for (int i = 0; i < 23; i++)
      TestIndexWriter.addDoc(writer);

    IndexReader reader = IndexReader.open(dir, true);
    assertEquals(0, reader.numDocs());
    writer.commit();
    IndexReader reader2 = IndexReader.openIfChanged(reader);
    assertNotNull(reader2);
    assertEquals(0, reader.numDocs());
    assertEquals(23, reader2.numDocs());
    reader.close();

    for (int i = 0; i < 17; i++)
      TestIndexWriter.addDoc(writer);
    assertEquals(23, reader2.numDocs());
    reader2.close();
    reader = IndexReader.open(dir, true);
    assertEquals(23, reader.numDocs());
    reader.close();
    writer.commit();

    reader = IndexReader.open(dir, true);
    assertEquals(40, reader.numDocs());
    reader.close();
    writer.close();
    dir.close();
  }
  
  public void testFutureCommit() throws Exception {
    Directory dir = newDirectory();

    IndexWriter w = new IndexWriter(dir, newIndexWriterConfig( TEST_VERSION_CURRENT, new MockAnalyzer(random)).setIndexDeletionPolicy(NoDeletionPolicy.INSTANCE));
    Document doc = new Document();
    w.addDocument(doc);

    // commit to "first"
    Map<String,String> commitData = new HashMap<String,String>();
    commitData.put("tag", "first");
    w.commit(commitData);

    // commit to "second"
    w.addDocument(doc);
    commitData.put("tag", "second");
    w.commit(commitData);
    w.close();

    // open "first" with IndexWriter
    IndexCommit commit = null;
    for(IndexCommit c : IndexReader.listCommits(dir)) {
      if (c.getUserData().get("tag").equals("first")) {
        commit = c;
        break;
      }
    }

    assertNotNull(commit);

    w = new IndexWriter(dir, newIndexWriterConfig( TEST_VERSION_CURRENT, new MockAnalyzer(random)).setIndexDeletionPolicy(NoDeletionPolicy.INSTANCE).setIndexCommit(commit));

    assertEquals(1, w.numDocs());

    // commit IndexWriter to "third"
    w.addDocument(doc);
    commitData.put("tag", "third");
    w.commit(commitData);
    w.close();

    // make sure "second" commit is still there
    commit = null;
    for(IndexCommit c : IndexReader.listCommits(dir)) {
      if (c.getUserData().get("tag").equals("second")) {
        commit = c;
        break;
      }
    }

    assertNotNull(commit);

    IndexReader r = IndexReader.open(commit, true);
    assertEquals(2, r.numDocs());
    r.close();

    // open "second", w/ writeable IndexReader & commit
    r = IndexReader.open(commit, NoDeletionPolicy.INSTANCE, false);
    assertEquals(2, r.numDocs());
    r.deleteDocument(0);
    r.deleteDocument(1);
    commitData.put("tag", "fourth");
    r.commit(commitData);
    r.close();

    // make sure "third" commit is still there
    commit = null;
    for(IndexCommit c : IndexReader.listCommits(dir)) {
      if (c.getUserData().get("tag").equals("third")) {
        commit = c;
        break;
      }
    }
    assertNotNull(commit);

    dir.close();
  }
  
  public void testNoCommits() throws Exception {
    // Tests that if we don't call commit(), the directory has 0 commits. This has
    // changed since LUCENE-2386, where before IW would always commit on a fresh
    // new index.
    Directory dir = newDirectory();
    IndexWriter writer = new IndexWriter(dir, newIndexWriterConfig( TEST_VERSION_CURRENT, new MockAnalyzer(random)));
    try {
      IndexReader.listCommits(dir);
      fail("listCommits should have thrown an exception over empty index");
    } catch (IndexNotFoundException e) {
      // that's expected !
    }
    // No changes still should generate a commit, because it's a new index.
    writer.close();
    assertEquals("expected 1 commits!", 1, IndexReader.listCommits(dir).size());
    dir.close();
  }
  
  // LUCENE-1274: test writer.prepareCommit()
  public void testPrepareCommit() throws IOException {
    Directory dir = newDirectory();

    IndexWriter writer = new IndexWriter(
        dir,
        newIndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer(random)).
            setMaxBufferedDocs(2).
            setMergePolicy(newLogMergePolicy(5))
    );
    writer.commit();

    for (int i = 0; i < 23; i++)
      TestIndexWriter.addDoc(writer);

    IndexReader reader = IndexReader.open(dir, true);
    assertEquals(0, reader.numDocs());

    writer.prepareCommit();

    IndexReader reader2 = IndexReader.open(dir, true);
    assertEquals(0, reader2.numDocs());

    writer.commit();

    IndexReader reader3 = IndexReader.openIfChanged(reader);
    assertNotNull(reader3);
    assertEquals(0, reader.numDocs());
    assertEquals(0, reader2.numDocs());
    assertEquals(23, reader3.numDocs());
    reader.close();
    reader2.close();

    for (int i = 0; i < 17; i++)
      TestIndexWriter.addDoc(writer);

    assertEquals(23, reader3.numDocs());
    reader3.close();
    reader = IndexReader.open(dir, true);
    assertEquals(23, reader.numDocs());
    reader.close();

    writer.prepareCommit();

    reader = IndexReader.open(dir, true);
    assertEquals(23, reader.numDocs());
    reader.close();

    writer.commit();
    reader = IndexReader.open(dir, true);
    assertEquals(40, reader.numDocs());
    reader.close();
    writer.close();
    dir.close();
  }

  // LUCENE-1274: test writer.prepareCommit()
  public void testPrepareCommitRollback() throws IOException {
    MockDirectoryWrapper dir = newDirectory();
    dir.setPreventDoubleWrite(false);

    IndexWriter writer = new IndexWriter(
        dir,
        newIndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer(random)).
            setMaxBufferedDocs(2).
            setMergePolicy(newLogMergePolicy(5))
    );
    writer.commit();

    for (int i = 0; i < 23; i++)
      TestIndexWriter.addDoc(writer);

    IndexReader reader = IndexReader.open(dir, true);
    assertEquals(0, reader.numDocs());

    writer.prepareCommit();

    IndexReader reader2 = IndexReader.open(dir, true);
    assertEquals(0, reader2.numDocs());

    writer.rollback();

    IndexReader reader3 = IndexReader.openIfChanged(reader);
    assertNull(reader3);
    assertEquals(0, reader.numDocs());
    assertEquals(0, reader2.numDocs());
    reader.close();
    reader2.close();

    writer = new IndexWriter(dir, newIndexWriterConfig( TEST_VERSION_CURRENT, new MockAnalyzer(random)));
    for (int i = 0; i < 17; i++)
      TestIndexWriter.addDoc(writer);

    reader = IndexReader.open(dir, true);
    assertEquals(0, reader.numDocs());
    reader.close();

    writer.prepareCommit();

    reader = IndexReader.open(dir, true);
    assertEquals(0, reader.numDocs());
    reader.close();

    writer.commit();
    reader = IndexReader.open(dir, true);
    assertEquals(17, reader.numDocs());
    reader.close();
    writer.close();
    dir.close();
  }

  // LUCENE-1274
  public void testPrepareCommitNoChanges() throws IOException {
    Directory dir = newDirectory();

    IndexWriter writer = new IndexWriter(dir, newIndexWriterConfig( TEST_VERSION_CURRENT, new MockAnalyzer(random)));
    writer.prepareCommit();
    writer.commit();
    writer.close();

    IndexReader reader = IndexReader.open(dir, true);
    assertEquals(0, reader.numDocs());
    reader.close();
    dir.close();
  }
  
  // LUCENE-1382
  public void testCommitUserData() throws IOException {
    Directory dir = newDirectory();
    IndexWriter w = new IndexWriter(dir, newIndexWriterConfig( TEST_VERSION_CURRENT, new MockAnalyzer(random)).setMaxBufferedDocs(2));
    for(int j=0;j<17;j++)
      TestIndexWriter.addDoc(w);
    w.close();

    assertEquals(0, IndexReader.getCommitUserData(dir).size());

    IndexReader r = IndexReader.open(dir, true);
    // commit(Map) never called for this index
    assertEquals(0, r.getCommitUserData().size());
    r.close();

    w = new IndexWriter(dir, newIndexWriterConfig( TEST_VERSION_CURRENT, new MockAnalyzer(random)).setMaxBufferedDocs(2));
    for(int j=0;j<17;j++)
      TestIndexWriter.addDoc(w);
    Map<String,String> data = new HashMap<String,String>();
    data.put("label", "test1");
    w.commit(data);
    w.close();

    assertEquals("test1", IndexReader.getCommitUserData(dir).get("label"));

    r = IndexReader.open(dir, true);
    assertEquals("test1", r.getCommitUserData().get("label"));
    r.close();

    w = new IndexWriter(dir, newIndexWriterConfig( TEST_VERSION_CURRENT, new MockAnalyzer(random)));
    w.optimize();
    w.close();

    assertEquals("test1", IndexReader.getCommitUserData(dir).get("label"));

    dir.close();
  }
}
