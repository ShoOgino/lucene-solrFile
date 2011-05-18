package org.apache.lucene.search.grouping;

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

import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.util.BytesRef;

/** Represents one group in the results.
 * 
 * @lucene.experimental */
public class GroupDocs {
  /** The groupField value for all docs in this group; this
   *  may be null if hits did not have the groupField. */
  public final BytesRef groupValue;

  /** Max score in this group */
  public final float maxScore;

  /** Hits; this may be {@link
   * org.apache.lucene.search.FieldDoc} instances if the
   * withinGroupSort sorted by fields. */
  public final ScoreDoc[] scoreDocs;

  /** Total hits within this group */
  public final int totalHits;

  /** Matches the groupSort passed to {@link
   *  FirstPassGroupingCollector}. */
  public final Comparable[] groupSortValues;

  public GroupDocs(float maxScore,
                   int totalHits,
                   ScoreDoc[] scoreDocs,
                   BytesRef groupValue,
                   Comparable[] groupSortValues) {
    this.maxScore = maxScore;
    this.totalHits = totalHits;
    this.scoreDocs = scoreDocs;
    this.groupValue = groupValue;
    this.groupSortValues = groupSortValues;
  }
}
