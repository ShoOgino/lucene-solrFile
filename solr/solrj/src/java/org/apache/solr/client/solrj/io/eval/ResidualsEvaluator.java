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
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.expr.Explanation;
import org.apache.solr.client.solrj.io.stream.expr.Explanation.ExpressionType;
import org.apache.solr.client.solrj.io.stream.expr.Expressible;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpression;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionParameter;
import org.apache.solr.client.solrj.io.stream.expr.StreamFactory;

public class ResidualsEvaluator extends ComplexEvaluator implements Expressible {

  private static final long serialVersionUID = 1;

  public ResidualsEvaluator(StreamExpression expression,
                            StreamFactory factory) throws IOException {
    super(expression, factory);

    if(3 != subEvaluators.size()){
      throw new IOException(String.format(Locale.ROOT,"Invalid expression %s - expecting three values (regression result and two numeric arrays) but found %d",expression,subEvaluators.size()));
    }
  }

  public List<Number> evaluate(Tuple tuple) throws IOException {

    StreamEvaluator r = subEvaluators.get(0);
    StreamEvaluator a = subEvaluators.get(1);
    StreamEvaluator b = subEvaluators.get(2);

    RegressionEvaluator.RegressionTuple rt= (RegressionEvaluator.RegressionTuple)r.evaluate(tuple);
    List<Number> listA = (List<Number>)a.evaluate(tuple);
    List<Number> listB = (List<Number>)b.evaluate(tuple);
    List<Number> residuals = new ArrayList();

    for(int i=0; i<listA.size(); i++) {
      double valueA = listA.get(i).doubleValue();
      double prediction = rt.predict(valueA);
      double valueB = listB.get(i).doubleValue();
      double residual = valueB - prediction;
      residuals.add(residual);
    }

    return residuals;
  }

  @Override
  public StreamExpressionParameter toExpression(StreamFactory factory) throws IOException {
    StreamExpression expression = new StreamExpression(factory.getFunctionName(getClass()));
    return expression;
  }

  @Override
  public Explanation toExplanation(StreamFactory factory) throws IOException {
    return new Explanation(nodeId.toString())
        .withExpressionType(ExpressionType.EVALUATOR)
        .withFunctionName(factory.getFunctionName(getClass()))
        .withImplementingClass(getClass().getName())
        .withExpression(toExpression(factory).toString());
  }
}