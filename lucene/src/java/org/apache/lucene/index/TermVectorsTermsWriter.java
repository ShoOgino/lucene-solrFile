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
import java.util.Map;

import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.RamUsageEstimator;

final class TermVectorsTermsWriter extends TermsHashConsumer {

  final DocumentsWriterPerThread docWriter;
  TermVectorsWriter termVectorsWriter;
  int freeCount;
  IndexOutput tvx;
  IndexOutput tvd;
  IndexOutput tvf;
  int lastDocID;
  
  final DocumentsWriterPerThread.DocState docState;
  final BytesRef flushTerm = new BytesRef();
  
  // Used by perField when serializing the term vectors
  final ByteSliceReader vectorSliceReader = new ByteSliceReader();

  public TermVectorsTermsWriter(DocumentsWriterPerThread docWriter) {
    this.docWriter = docWriter;
    docState = docWriter.docState;
  }

  @Override
  void flush(Map<FieldInfo, TermsHashConsumerPerField> fieldsToFlush, final SegmentWriteState state) throws IOException {

    if (tvx != null) {

      if (state.numDocs > 0) {
        // In case there are some final documents that we
        // didn't see (because they hit a non-aborting exception):
        fill(state.numDocs);
      }

      tvx.flush();
      tvd.flush();
      tvf.flush();
      
      tvx.close();
      tvf.close();
      tvd.close();
      tvx = null;
      String idxName = IndexFileNames.segmentFileName(state.segmentName, "", IndexFileNames.VECTORS_INDEX_EXTENSION);
      if (4+((long) state.numDocs)*16 != state.directory.fileLength(idxName))
        throw new RuntimeException("after flush: tvx size mismatch: " + state.numDocs + " docs vs " + state.directory.fileLength(idxName) + " length in bytes of " + idxName + " file exists?=" + state.directory.fileExists(idxName));

      String fldName = IndexFileNames.segmentFileName(state.segmentName, "", IndexFileNames.VECTORS_FIELDS_EXTENSION);
      String docName = IndexFileNames.segmentFileName(state.segmentName, "", IndexFileNames.VECTORS_DOCUMENTS_EXTENSION);
      state.flushedFiles.add(idxName);
      state.flushedFiles.add(fldName);
      state.flushedFiles.add(docName);

      docWriter.removeOpenFile(idxName);
      docWriter.removeOpenFile(fldName);
      docWriter.removeOpenFile(docName);

      lastDocID = 0;

    }

    for (final TermsHashConsumerPerField field : fieldsToFlush.values() ) {
      TermVectorsTermsWriterPerField perField = (TermVectorsTermsWriterPerField) field;
      perField.termsHashPerField.reset();
      perField.shrinkHash();
    }
  }

  /** Fills in no-term-vectors for all docs we haven't seen
   *  since the last doc that had term vectors. */
  void fill(int docID) throws IOException {
    final int end = docID;
    if (lastDocID < end) {
      final long tvfPosition = tvf.getFilePointer();
      while(lastDocID < end) {
        tvx.writeLong(tvd.getFilePointer());
        tvd.writeVInt(0);
        tvx.writeLong(tvfPosition);
        lastDocID++;
      }
    }
  }

  void initTermVectorsWriter() throws IOException {        
    if (tvx == null) {
      
      final String segment = docWriter.getSegment();

      if (segment == null)
        return;

      // If we hit an exception while init'ing the term
      // vector output files, we must abort this segment
      // because those files will be in an unknown
      // state:
      String idxName = IndexFileNames.segmentFileName(segment, "", IndexFileNames.VECTORS_INDEX_EXTENSION);
      String docName = IndexFileNames.segmentFileName(segment, "", IndexFileNames.VECTORS_DOCUMENTS_EXTENSION);
      String fldName = IndexFileNames.segmentFileName(segment, "", IndexFileNames.VECTORS_FIELDS_EXTENSION);
      tvx = docWriter.directory.createOutput(idxName);
      tvd = docWriter.directory.createOutput(docName);
      tvf = docWriter.directory.createOutput(fldName);
      
      tvx.writeInt(TermVectorsReader.FORMAT_CURRENT);
      tvd.writeInt(TermVectorsReader.FORMAT_CURRENT);
      tvf.writeInt(TermVectorsReader.FORMAT_CURRENT);

      docWriter.addOpenFile(idxName);
      docWriter.addOpenFile(fldName);
      docWriter.addOpenFile(docName);

      lastDocID = 0;
    }
  }

  @Override
  void finishDocument(TermsHash termsHash) throws IOException {

    assert docWriter.writer.testPoint("TermVectorsTermsWriter.finishDocument start");

    initTermVectorsWriter();

    fill(docState.docID);
    
    // Append term vectors to the real outputs:
    tvx.writeLong(tvd.getFilePointer());
    tvx.writeLong(tvf.getFilePointer());
    tvd.writeVInt(numVectorFields);
    if (numVectorFields > 0) {
      for(int i=0;i<numVectorFields;i++) {
        tvd.writeVInt(perFields[i].fieldInfo.number);
      }
      long lastPos = tvf.getFilePointer();
      perFields[0].finishDocument();
      for(int i=1;i<numVectorFields;i++) {
        long pos = tvf.getFilePointer();
        tvd.writeVLong(pos-lastPos);
        lastPos = pos;
        perFields[i].finishDocument();
      }
    }

    assert lastDocID == docState.docID;

    lastDocID++;

    termsHash.reset();
    reset();
    assert docWriter.writer.testPoint("TermVectorsTermsWriter.finishDocument end");
  }

  @Override
  public void abort() {

    if (tvx != null) {
      try {
        tvx.close();
      } catch (Throwable t) {
      }
      tvx = null;
    }
    if (tvd != null) {
      try {
        tvd.close();
      } catch (Throwable t) {
      }
      tvd = null;
    }
    if (tvf != null) {
      try {
        tvf.close();
      } catch (Throwable t) {
      }
      tvf = null;
    }
    lastDocID = 0;
    

  }

  int numVectorFields;

  TermVectorsTermsWriterPerField[] perFields;

  void reset() {
    numVectorFields = 0;
    perFields = new TermVectorsTermsWriterPerField[1];
  }

  @Override
  public TermsHashConsumerPerField addField(TermsHashPerField termsHashPerField, FieldInfo fieldInfo) {
    return new TermVectorsTermsWriterPerField(termsHashPerField, this, fieldInfo);
  }

  void addFieldToFlush(TermVectorsTermsWriterPerField fieldToFlush) {
    if (numVectorFields == perFields.length) {
      int newSize = ArrayUtil.oversize(numVectorFields + 1, RamUsageEstimator.NUM_BYTES_OBJ_REF);
      TermVectorsTermsWriterPerField[] newArray = new TermVectorsTermsWriterPerField[newSize];
      System.arraycopy(perFields, 0, newArray, 0, numVectorFields);
      perFields = newArray;
    }

    perFields[numVectorFields++] = fieldToFlush;
  }
  
  @Override
  void startDocument() throws IOException {
    assert clearLastVectorFieldName();
    perFields = new TermVectorsTermsWriterPerField[1];
    reset();
  }
  
  // Called only by assert
  final boolean clearLastVectorFieldName() {
    lastVectorFieldName = null;
    return true;
  }

  // Called only by assert
  String lastVectorFieldName;
  final boolean vectorFieldsInOrder(FieldInfo fi) {
    try {
      if (lastVectorFieldName != null)
        return lastVectorFieldName.compareTo(fi.name) < 0;
      else
        return true;
    } finally {
      lastVectorFieldName = fi.name;
    }
  }

}
