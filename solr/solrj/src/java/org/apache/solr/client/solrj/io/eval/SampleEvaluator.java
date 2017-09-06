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

package org.apache.solr.client.solrj.io.eval;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpression;
import org.apache.solr.client.solrj.io.stream.expr.StreamFactory;

public class SampleEvaluator extends RecursiveObjectEvaluator implements TwoValueWorker {

  private static final long serialVersionUID = 1;

  public SampleEvaluator(StreamExpression expression, StreamFactory factory) throws IOException {
    super(expression, factory);
  }
  
  @Override
  public Object doWork(Object first, Object second) throws IOException{
    if(null == first){
      throw new IOException(String.format(Locale.ROOT,"Invalid expression %s - null found for the first value",toExpression(constructingFactory)));
    }
    if(null == second){
      throw new IOException(String.format(Locale.ROOT,"Invalid expression %s - null found for the second value",toExpression(constructingFactory)));
    }
    if(!(first instanceof RealDistribution) && !(first instanceof IntegerDistribution)){
      throw new IOException(String.format(Locale.ROOT,"Invalid expression %s - found type %s for the first value, expecting a Real or Integer Distribution",toExpression(constructingFactory), first.getClass().getSimpleName()));
    }
    if(!(second instanceof Number)){
      throw new IOException(String.format(Locale.ROOT,"Invalid expression %s - found type %s for the second value, expecting a Number",toExpression(constructingFactory), first.getClass().getSimpleName()));
    }

    if(first instanceof RealDistribution) {
      RealDistribution realDistribution = (RealDistribution) first;
      return Arrays.stream(realDistribution.sample(((Number) second).intValue())).mapToObj(item -> item).collect(Collectors.toList());
    } else {
      IntegerDistribution integerDistribution = (IntegerDistribution) first;
      return Arrays.stream(integerDistribution.sample(((Number) second).intValue())).mapToObj(item -> item).collect(Collectors.toList());
    }
  }
}