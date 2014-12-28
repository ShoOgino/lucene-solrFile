package org.apache.lucene.codecs.compressing;

/*
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
import java.util.Arrays;

import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.StoredFieldsReader;
import org.apache.lucene.codecs.StoredFieldsWriter;
import org.apache.lucene.codecs.compressing.CompressingStoredFieldsReader.SerializedDocument;
import org.apache.lucene.document.DocumentStoredFieldVisitor;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.MergeState;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.StorableField;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BitUtil;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.GrowableByteArrayDataOutput;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.packed.PackedInts;

/**
 * {@link StoredFieldsWriter} impl for {@link CompressingStoredFieldsFormat}.
 * @lucene.experimental
 */
public final class CompressingStoredFieldsWriter extends StoredFieldsWriter {

  /** Extension of stored fields file */
  public static final String FIELDS_EXTENSION = "fdt";
  
  /** Extension of stored fields index file */
  public static final String FIELDS_INDEX_EXTENSION = "fdx";

  static final int         STRING = 0x00;
  static final int       BYTE_ARR = 0x01;
  static final int    NUMERIC_INT = 0x02;
  static final int  NUMERIC_FLOAT = 0x03;
  static final int   NUMERIC_LONG = 0x04;
  static final int NUMERIC_DOUBLE = 0x05;

  static final int TYPE_BITS = PackedInts.bitsRequired(NUMERIC_DOUBLE);
  static final int TYPE_MASK = (int) PackedInts.maxValue(TYPE_BITS);

  static final String CODEC_SFX_IDX = "Index";
  static final String CODEC_SFX_DAT = "Data";
  static final int VERSION_START = 0;
  static final int VERSION_CURRENT = VERSION_START;

  private final String segment;
  private CompressingStoredFieldsIndexWriter indexWriter;
  private IndexOutput fieldsStream;

  private final CompressionMode compressionMode;
  private final Compressor compressor;
  private final int chunkSize;
  private final int maxDocsPerChunk;

  private final GrowableByteArrayDataOutput bufferedDocs;
  private int[] numStoredFields; // number of stored fields
  private int[] endOffsets; // end offsets in bufferedDocs
  private int docBase; // doc ID at the beginning of the chunk
  private int numBufferedDocs; // docBase + numBufferedDocs == current doc ID

  /** Sole constructor. */
  public CompressingStoredFieldsWriter(Directory directory, SegmentInfo si, String segmentSuffix, IOContext context,
      String formatName, CompressionMode compressionMode, int chunkSize, int maxDocsPerChunk) throws IOException {
    assert directory != null;
    this.segment = si.name;
    this.compressionMode = compressionMode;
    this.compressor = compressionMode.newCompressor();
    this.chunkSize = chunkSize;
    this.maxDocsPerChunk = maxDocsPerChunk;
    this.docBase = 0;
    this.bufferedDocs = new GrowableByteArrayDataOutput(chunkSize);
    this.numStoredFields = new int[16];
    this.endOffsets = new int[16];
    this.numBufferedDocs = 0;

    boolean success = false;
    IndexOutput indexStream = directory.createOutput(IndexFileNames.segmentFileName(segment, segmentSuffix, FIELDS_INDEX_EXTENSION), 
                                                                     context);
    try {
      fieldsStream = directory.createOutput(IndexFileNames.segmentFileName(segment, segmentSuffix, FIELDS_EXTENSION),
                                                    context);

      final String codecNameIdx = formatName + CODEC_SFX_IDX;
      final String codecNameDat = formatName + CODEC_SFX_DAT;
      CodecUtil.writeIndexHeader(indexStream, codecNameIdx, VERSION_CURRENT, si.getId(), segmentSuffix);
      CodecUtil.writeIndexHeader(fieldsStream, codecNameDat, VERSION_CURRENT, si.getId(), segmentSuffix);
      assert CodecUtil.indexHeaderLength(codecNameDat, segmentSuffix) == fieldsStream.getFilePointer();
      assert CodecUtil.indexHeaderLength(codecNameIdx, segmentSuffix) == indexStream.getFilePointer();

      indexWriter = new CompressingStoredFieldsIndexWriter(indexStream);
      indexStream = null;

      fieldsStream.writeVInt(chunkSize);
      fieldsStream.writeVInt(PackedInts.VERSION_CURRENT);

      success = true;
    } finally {
      if (!success) {
        IOUtils.closeWhileHandlingException(fieldsStream, indexStream, indexWriter);
      }
    }
  }

  @Override
  public void close() throws IOException {
    try {
      IOUtils.close(fieldsStream, indexWriter);
    } finally {
      fieldsStream = null;
      indexWriter = null;
    }
  }

  private int numStoredFieldsInDoc;

  @Override
  public void startDocument() throws IOException {
  }

  @Override
  public void finishDocument() throws IOException {
    if (numBufferedDocs == this.numStoredFields.length) {
      final int newLength = ArrayUtil.oversize(numBufferedDocs + 1, 4);
      this.numStoredFields = Arrays.copyOf(this.numStoredFields, newLength);
      endOffsets = Arrays.copyOf(endOffsets, newLength);
    }
    this.numStoredFields[numBufferedDocs] = numStoredFieldsInDoc;
    numStoredFieldsInDoc = 0;
    endOffsets[numBufferedDocs] = bufferedDocs.length;
    ++numBufferedDocs;
    if (triggerFlush()) {
      flush();
    }
  }

  private static void saveInts(int[] values, int length, DataOutput out) throws IOException {
    assert length > 0;
    if (length == 1) {
      out.writeVInt(values[0]);
    } else {
      boolean allEqual = true;
      for (int i = 1; i < length; ++i) {
        if (values[i] != values[0]) {
          allEqual = false;
          break;
        }
      }
      if (allEqual) {
        out.writeVInt(0);
        out.writeVInt(values[0]);
      } else {
        long max = 0;
        for (int i = 0; i < length; ++i) {
          max |= values[i];
        }
        final int bitsRequired = PackedInts.bitsRequired(max);
        out.writeVInt(bitsRequired);
        final PackedInts.Writer w = PackedInts.getWriterNoHeader(out, PackedInts.Format.PACKED, length, bitsRequired, 1);
        for (int i = 0; i < length; ++i) {
          w.add(values[i]);
        }
        w.finish();
      }
    }
  }

  private void writeHeader(int docBase, int numBufferedDocs, int[] numStoredFields, int[] lengths, boolean sliced) throws IOException {
    final int slicedBit = sliced ? 1 : 0;
    
    // save docBase and numBufferedDocs
    fieldsStream.writeVInt(docBase);
    fieldsStream.writeVInt((numBufferedDocs) << 1 | slicedBit);

    // save numStoredFields
    saveInts(numStoredFields, numBufferedDocs, fieldsStream);

    // save lengths
    saveInts(lengths, numBufferedDocs, fieldsStream);
  }

  private boolean triggerFlush() {
    return bufferedDocs.length >= chunkSize || // chunks of at least chunkSize bytes
        numBufferedDocs >= maxDocsPerChunk;
  }

  private void flush() throws IOException {
    indexWriter.writeIndex(numBufferedDocs, fieldsStream.getFilePointer());

    // transform end offsets into lengths
    final int[] lengths = endOffsets;
    for (int i = numBufferedDocs - 1; i > 0; --i) {
      lengths[i] = endOffsets[i] - endOffsets[i - 1];
      assert lengths[i] >= 0;
    }
    final boolean sliced = bufferedDocs.length >= 2 * chunkSize;
    writeHeader(docBase, numBufferedDocs, numStoredFields, lengths, sliced);

    // compress stored fields to fieldsStream
    if (sliced) {
      // big chunk, slice it
      for (int compressed = 0; compressed < bufferedDocs.length; compressed += chunkSize) {
        compressor.compress(bufferedDocs.bytes, compressed, Math.min(chunkSize, bufferedDocs.length - compressed), fieldsStream);
      }
    } else {
      compressor.compress(bufferedDocs.bytes, 0, bufferedDocs.length, fieldsStream);
    }

    // reset
    docBase += numBufferedDocs;
    numBufferedDocs = 0;
    bufferedDocs.length = 0;
  }

  @Override
  public void writeField(FieldInfo info, StorableField field)
      throws IOException {

    ++numStoredFieldsInDoc;

    int bits = 0;
    final BytesRef bytes;
    final String string;

    Number number = field.numericValue();
    if (number != null) {
      if (number instanceof Byte || number instanceof Short || number instanceof Integer) {
        bits = NUMERIC_INT;
      } else if (number instanceof Long) {
        bits = NUMERIC_LONG;
      } else if (number instanceof Float) {
        bits = NUMERIC_FLOAT;
      } else if (number instanceof Double) {
        bits = NUMERIC_DOUBLE;
      } else {
        throw new IllegalArgumentException("cannot store numeric type " + number.getClass());
      }
      string = null;
      bytes = null;
    } else {
      bytes = field.binaryValue();
      if (bytes != null) {
        bits = BYTE_ARR;
        string = null;
      } else {
        bits = STRING;
        string = field.stringValue();
        if (string == null) {
          throw new IllegalArgumentException("field " + field.name() + " is stored but does not have binaryValue, stringValue nor numericValue");
        }
      }
    }

    final long infoAndBits = (((long) info.number) << TYPE_BITS) | bits;
    bufferedDocs.writeVLong(infoAndBits);

    if (bytes != null) {
      bufferedDocs.writeVInt(bytes.length);
      bufferedDocs.writeBytes(bytes.bytes, bytes.offset, bytes.length);
    } else if (string != null) {
      bufferedDocs.writeString(field.stringValue());
    } else {
      if (number instanceof Byte || number instanceof Short || number instanceof Integer) {
        bufferedDocs.writeZInt(number.intValue());
      } else if (number instanceof Long) {
        writeTLong(bufferedDocs, number.longValue());
      } else if (number instanceof Float) {
        writeZFloat(bufferedDocs, number.floatValue());
      } else if (number instanceof Double) {
        writeZDouble(bufferedDocs, number.doubleValue());
      } else {
        throw new AssertionError("Cannot get here");
      }
    }
  }

  // -0 isn't compressed.
  static final int NEGATIVE_ZERO_FLOAT = Float.floatToIntBits(-0f);
  static final long NEGATIVE_ZERO_DOUBLE = Double.doubleToLongBits(-0d);

  // for compression of timestamps
  static final long SECOND = 1000L;
  static final long HOUR = 60 * 60 * SECOND;
  static final long DAY = 24 * HOUR;
  static final int SECOND_ENCODING = 0x40;
  static final int HOUR_ENCODING = 0x80;
  static final int DAY_ENCODING = 0xC0;

  /** 
   * Writes a float in a variable-length format.  Writes between one and 
   * five bytes. Small integral values typically take fewer bytes.
   * <p>
   * ZFloat --&gt; Header, Bytes*?
   * <ul>
   *    <li>Header --&gt; {@link DataOutput#writeByte Uint8}. When it is
   *       equal to 0xFF then the value is negative and stored in the next
   *       4 bytes. Otherwise if the first bit is set then the other bits
   *       in the header encode the value plus one and no other
   *       bytes are read. Otherwise, the value is a positive float value
   *       whose first byte is the header, and 3 bytes need to be read to
   *       complete it.
   *    <li>Bytes --&gt; Potential additional bytes to read depending on the
   *       header.
   * </ul>
   * <p>
   */
  static void writeZFloat(DataOutput out, float f) throws IOException {
    int intVal = (int) f;
    final int floatBits = Float.floatToIntBits(f);

    if (f == intVal
        && intVal >= -1
        && intVal <= 0x7D
        && floatBits != NEGATIVE_ZERO_FLOAT) {
      // small integer value [-1..125]: single byte
      out.writeByte((byte) (0x80 | (1 + intVal)));
    } else if ((floatBits >>> 31) == 0) {
      // other positive floats: 4 bytes
      out.writeInt(floatBits);
    } else {
      // other negative float: 5 bytes
      out.writeByte((byte) 0xFF);
      out.writeInt(floatBits);
    }
  }
  
  /** 
   * Writes a float in a variable-length format.  Writes between one and 
   * five bytes. Small integral values typically take fewer bytes.
   * <p>
   * ZFloat --&gt; Header, Bytes*?
   * <ul>
   *    <li>Header --&gt; {@link DataOutput#writeByte Uint8}. When it is
   *       equal to 0xFF then the value is negative and stored in the next
   *       8 bytes. When it is equal to 0xFE then the value is stored as a
   *       float in the next 4 bytes. Otherwise if the first bit is set
   *       then the other bits in the header encode the value plus one and
   *       no other bytes are read. Otherwise, the value is a positive float
   *       value whose first byte is the header, and 7 bytes need to be read
   *       to complete it.
   *    <li>Bytes --&gt; Potential additional bytes to read depending on the
   *       header.
   * </ul>
   * <p>
   */
  static void writeZDouble(DataOutput out, double d) throws IOException {
    int intVal = (int) d;
    final long doubleBits = Double.doubleToLongBits(d);
    
    if (d == intVal &&
        intVal >= -1 && 
        intVal <= 0x7C &&
        doubleBits != NEGATIVE_ZERO_DOUBLE) {
      // small integer value [-1..124]: single byte
      out.writeByte((byte) (0x80 | (intVal + 1)));
      return;
    } else if (d == (float) d) {
      // d has an accurate float representation: 5 bytes
      out.writeByte((byte) 0xFE);
      out.writeInt(Float.floatToIntBits((float) d));
    } else if ((doubleBits >>> 63) == 0) {
      // other positive doubles: 8 bytes
      out.writeLong(doubleBits);
    } else {
      // other negative doubles: 9 bytes
      out.writeByte((byte) 0xFF);
      out.writeLong(doubleBits);
    }
  }

  /** 
   * Writes a long in a variable-length format.  Writes between one and 
   * ten bytes. Small values or values representing timestamps with day,
   * hour or second precision typically require fewer bytes.
   * <p>
   * ZLong --&gt; Header, Bytes*?
   * <ul>
   *    <li>Header --&gt; The first two bits indicate the compression scheme:
   *       <ul>
   *          <li>00 - uncompressed
   *          <li>01 - multiple of 1000 (second)
   *          <li>10 - multiple of 3600000 (hour)
   *          <li>11 - multiple of 86400000 (day)
   *       </ul>
   *       Then the next bit is a continuation bit, indicating whether more
   *       bytes need to be read, and the last 5 bits are the lower bits of
   *       the encoded value. In order to reconstruct the value, you need to
   *       combine the 5 lower bits of the header with a vLong in the next
   *       bytes (if the continuation bit is set to 1). Then
   *       {@link BitUtil#zigZagDecode(int) zigzag-decode} it and finally
   *       multiply by the multiple corresponding to the compression scheme.
   *    <li>Bytes --&gt; Potential additional bytes to read depending on the
   *       header.
   * </ul>
   * <p>
   */
  // T for "timestamp"
  static void writeTLong(DataOutput out, long l) throws IOException {
    int header; 
    if (l % SECOND != 0) {
      header = 0;
    } else if (l % DAY == 0) {
      // timestamp with day precision
      header = DAY_ENCODING;
      l /= DAY;
    } else if (l % HOUR == 0) {
      // timestamp with hour precision, or day precision with a timezone
      header = HOUR_ENCODING;
      l /= HOUR;
    } else {
      // timestamp with second precision
      header = SECOND_ENCODING;
      l /= SECOND;
    }

    final long zigZagL = BitUtil.zigZagEncode(l);
    header |= (zigZagL & 0x1F); // last 5 bits
    final long upperBits = zigZagL >>> 5;
    if (upperBits != 0) {
      header |= 0x20;
    }
    out.writeByte((byte) header);
    if (upperBits != 0) {
      out.writeVLong(upperBits);
    }
  }

  @Override
  public void finish(FieldInfos fis, int numDocs) throws IOException {
    if (numBufferedDocs > 0) {
      flush();
    } else {
      assert bufferedDocs.length == 0;
    }
    if (docBase != numDocs) {
      throw new RuntimeException("Wrote " + docBase + " docs, finish called with numDocs=" + numDocs);
    }
    indexWriter.finish(numDocs, fieldsStream.getFilePointer());
    CodecUtil.writeFooter(fieldsStream);
    assert bufferedDocs.length == 0;
  }

  @Override
  public int merge(MergeState mergeState) throws IOException {
    int docCount = 0;
    int numReaders = mergeState.maxDocs.length;
    
    MatchingReaders matching = new MatchingReaders(mergeState);
    
    for (int readerIndex=0;readerIndex<numReaders;readerIndex++) {
      CompressingStoredFieldsReader matchingFieldsReader = null;
      if (matching.matchingReaders[readerIndex]) {
        final StoredFieldsReader fieldsReader = mergeState.storedFieldsReaders[readerIndex];
        // we can only bulk-copy if the matching reader is also a CompressingStoredFieldsReader
        if (fieldsReader != null && fieldsReader instanceof CompressingStoredFieldsReader) {
          matchingFieldsReader = (CompressingStoredFieldsReader) fieldsReader;
        }
      }

      final int maxDoc = mergeState.maxDocs[readerIndex];
      final Bits liveDocs = mergeState.liveDocs[readerIndex];

      // if its some other format, or an older version of this format:
      if (matchingFieldsReader == null || matchingFieldsReader.getVersion() != VERSION_CURRENT) {
        // naive merge...
        StoredFieldsReader storedFieldsReader = mergeState.storedFieldsReaders[readerIndex];
        if (storedFieldsReader != null) {
          storedFieldsReader.checkIntegrity();
        }
        for (int docID = 0; docID < maxDoc; docID++) {
          if (liveDocs != null && liveDocs.get(docID) == false) {
            continue;
          }
          DocumentStoredFieldVisitor visitor = new DocumentStoredFieldVisitor();
          storedFieldsReader.visitDocument(docID, visitor);
          addDocument(visitor.getDocument(), mergeState.mergeFieldInfos);
          ++docCount;
          mergeState.checkAbort.work(300);
        }
      } else {
        // optimized merge, we copy serialized (but decompressed) bytes directly
        // even on simple docs (1 stored field), it seems to help by about 20%
        matchingFieldsReader.checkIntegrity();
        for (int docID = 0; docID < maxDoc; docID++) {
          if (liveDocs != null && liveDocs.get(docID) == false) {
            continue;
          }
          SerializedDocument doc = matchingFieldsReader.document(docID);
          startDocument();
          bufferedDocs.copyBytes(doc.in, doc.length);
          numStoredFieldsInDoc = doc.numStoredFields;
          finishDocument();
          ++docCount;
          mergeState.checkAbort.work(300);
        }
      }
    }
    finish(mergeState.mergeFieldInfos, docCount);
    return docCount;
  }
}
