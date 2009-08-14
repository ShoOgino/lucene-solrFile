package org.apache.lucene.analysis;

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

import org.apache.lucene.util.AttributeSource;

import java.io.Reader;
import java.io.IOException;

/** A Tokenizer is a TokenStream whose input is a Reader.
  <p>
  This is an abstract class.
  <p>
  NOTE: subclasses must override 
  {@link #incrementToken()} if the new TokenStream API is used
  and {@link #next(Token)} or {@link #next()} if the old
  TokenStream API is used.
  <p>
  NOTE: Subclasses overriding {@link #incrementToken()} must
  call {@link AttributeSource#clearAttributes()} before
  setting attributes.
  Subclasses overriding {@link #next(Token)} must call
  {@link Token#clear()} before setting Token attributes. 
 */

public abstract class Tokenizer extends TokenStream {
  /** The text source for this Tokenizer. */
  protected CharStream input;

  /** Construct a tokenizer with null input. */
  protected Tokenizer() {}
  
  /** Construct a token stream processing the given input. */
  protected Tokenizer(Reader input) {
    this.input = CharReader.get(input);
  }

  /** Construct a token stream processing the given input. */
  protected Tokenizer(CharStream input) {
    this.input = input;
  }
  
  /** Construct a tokenizer with null input using the given AttributeFactory. */
  protected Tokenizer(AttributeFactory factory) {
    super(factory);
  }

  /** Construct a token stream processing the given input using the given AttributeFactory. */
  protected Tokenizer(AttributeFactory factory, Reader input) {
    super(factory);
    this.input = CharReader.get(input);
  }
  
  /** Construct a token stream processing the given input using the given AttributeFactory. */
  protected Tokenizer(AttributeFactory factory, CharStream input) {
    super(factory);
    this.input = input;
  }

  /** Construct a token stream processing the given input using the given AttributeSource. */
  protected Tokenizer(AttributeSource source) {
    super(source);
  }

  /** By default, closes the input Reader. */
  public void close() throws IOException {
    input.close();
  }

  /** Expert: Reset the tokenizer to a new reader.  Typically, an
   *  analyzer (in its reusableTokenStream method) will use
   *  this to re-use a previously created tokenizer. */
  public void reset(Reader input) throws IOException {
    this.input = CharReader.get(input);
  }

  /** Expert: Reset the tokenizer to a new CharStream.  Typically, an
   *  analyzer (in its reusableTokenStream method) will use
   *  this to re-use a previously created tokenizer. */
  public void reset(CharStream input) throws IOException {
    this.input = input;
  }
}

