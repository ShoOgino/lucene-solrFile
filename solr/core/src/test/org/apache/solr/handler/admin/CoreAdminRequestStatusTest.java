package org.apache.solr.handler.admin;

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

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.TestUtil;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.BeforeClass;
import org.junit.Test;


public class CoreAdminRequestStatusTest extends SolrTestCaseJ4{
  @BeforeClass
  public static void beforeClass() throws Exception {
    initCore("solrconfig.xml", "schema.xml");
  }

  @Test
  public void testCoreAdminRequestStatus() throws Exception {
    final File workDir = TestUtil.createTempDir(LuceneTestCase.getTestClass().getSimpleName());
    final CoreContainer cores = h.getCoreContainer();

    final CoreAdminHandler admin = new CoreAdminHandler(cores);

    String instDir;
    try (SolrCore template = cores.getCore("collection1")) {
      assertNotNull(template);
      instDir = template.getCoreDescriptor().getInstanceDir();
    }

    final File instDirFile = new File(instDir);
    assertTrue("instDir doesn't exist: " + instDir, instDirFile.exists());
    final File instPropFile = new File(workDir, "instProp");
    FileUtils.copyDirectory(instDirFile, instPropFile);

    // create a new core (using CoreAdminHandler) w/ properties

    SolrQueryResponse resp = new SolrQueryResponse();
    admin.handleRequestBody
        (req(CoreAdminParams.ACTION,
            CoreAdminParams.CoreAdminAction.CREATE.toString(),
            CoreAdminParams.INSTANCE_DIR, instPropFile.getAbsolutePath(),
            CoreAdminParams.NAME, "dummycore",
            "async", "42"),
            resp);
    assertNull("Exception on create", resp.getException());

    int maxRetries = 10;

    while(maxRetries-- > 0) {
    resp = new SolrQueryResponse();
    admin.handleRequestBody
        (req(CoreAdminParams.ACTION,
            CoreAdminParams.CoreAdminAction.REQUESTSTATUS.toString(),
            CoreAdminParams.REQUESTID, "42"),
            resp
        );
      if(resp.getValues().get("STATUS") != null && resp.getValues().get("STATUS").equals("completed"))
        break;
      Thread.sleep(1000);
    }

    assertEquals("The status of request was expected to be completed",
                 "completed", resp.getValues().get("STATUS"));

    resp = new SolrQueryResponse();
    admin.handleRequestBody
        (req(CoreAdminParams.ACTION,
            CoreAdminParams.CoreAdminAction.REQUESTSTATUS.toString(),
            CoreAdminParams.REQUESTID, "9999999"),
            resp
        );

    assertEquals("Was expecting it to be invalid but found a task with the id.",
                 "notfound", resp.getValues().get("STATUS"));

    admin.shutdown();
  }

}
