package org.apache.lucene.codecs.lucene40;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.codecs.LiveDocsFormat;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.MutableBits;

public class Lucene40LiveDocsFormat extends LiveDocsFormat {

  /** Extension of deletes */
  static final String DELETES_EXTENSION = "del";
  
  @Override
  public MutableBits newLiveDocs(int size) throws IOException {
    BitVector bitVector = new BitVector(size);
    bitVector.invertAll();
    return bitVector;
  }

  @Override
  public Bits readLiveDocs(Directory dir, SegmentInfo info, IOContext context) throws IOException {
    String filename = IndexFileNames.fileNameFromGeneration(info.name, DELETES_EXTENSION, info.getDelGen());
    return new BitVector(dir, filename, context);
  }

  @Override
  public void writeLiveDocs(MutableBits bits, Directory dir, SegmentInfo info, IOContext context) throws IOException {
    // nocommit: this api is ugly...
    String filename = IndexFileNames.fileNameFromGeneration(info.name, DELETES_EXTENSION, info.getDelGen());
    
    // nocommit: test if we really need this
    boolean success = false;
    try {
      ((BitVector)bits).write(dir, filename, context);
      success = true;
    } finally {
      if (!success) {
        IOUtils.deleteFilesIgnoringExceptions(dir, filename);
      }
    }
  }

  @Override
  public void separateFiles(Directory dir, SegmentInfo info, Set<String> files) throws IOException {
    if (info.hasDeletions()) {
      files.add(IndexFileNames.fileNameFromGeneration(info.name, DELETES_EXTENSION, info.getDelGen()));
    }
  }
}
