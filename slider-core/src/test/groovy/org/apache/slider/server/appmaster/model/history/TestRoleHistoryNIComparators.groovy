/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.slider.server.appmaster.model.history

import groovy.transform.CompileStatic
import org.apache.slider.server.appmaster.model.mock.BaseMockAppStateTest
import org.apache.slider.server.appmaster.model.mock.MockFactory
import org.apache.slider.server.appmaster.state.NodeInstance
import org.junit.Before
import org.junit.Test

/**
 * Unit test to verify the comparators sort as expected
 */
@CompileStatic
class TestRoleHistoryNIComparators extends BaseMockAppStateTest  {

  NodeInstance age1Active4 = nodeInstance(1001, 4, 0, 0)
  NodeInstance age2Active2 = nodeInstance(1002, 2, 0, 0)
  NodeInstance age3Active0 = nodeInstance(1003, 0, 0, 0)
  NodeInstance age4Active1 = nodeInstance(1004, 1, 0, 0)
  NodeInstance empty = new NodeInstance("empty", MockFactory.ROLE_COUNT)
  NodeInstance age6failing = nodeInstance(1006, 0, 0, 0)
  NodeInstance age1failing = nodeInstance(1001, 0, 0, 0)

  List<NodeInstance> nodes = [age2Active2, age4Active1, age1Active4, age3Active0]
  List<NodeInstance> nodesPlusEmpty = [age2Active2, age4Active1, age1Active4, age3Active0, empty]
  List<NodeInstance> allnodes = [age6failing, age2Active2, age4Active1, age1Active4, age3Active0, age1failing]

  @Before
  public void setup() {
    age6failing.get(0).failedRecently = 2;
    age1failing.get(0).failedRecently = 1;
  }
  
  @Override
  String getTestName() {
    return "TestNIComparators"
  }

  @Test
  public void testPreferred() throws Throwable {
    Collections.sort(nodes, new NodeInstance.Preferred(0))
    assertListEquals(nodes, [age4Active1, age3Active0, age2Active2, age1Active4])
  }

  /**
   * The preferred sort still includes failures; up to next phase in process
   * to handle that
   * @throws Throwable
   */
  @Test
  public void testPreferredWithFailures() throws Throwable {
    Collections.sort(allnodes, new NodeInstance.Preferred(0))
    assert allnodes[0] == age6failing
    assert allnodes[1] == age4Active1
  }

  @Test
  public void testPreferredComparatorDowngradesFailures() throws Throwable {
    def preferred = new NodeInstance.Preferred(0)
    assert preferred.compare(age6failing, age1failing) == -1
    assert preferred.compare(age1failing, age6failing) == 1
  }

  @Test
  public void testNewerThanNoRole() throws Throwable {
    Collections.sort(nodesPlusEmpty, new NodeInstance.Preferred(0))
    assertListEquals(nodesPlusEmpty, [age4Active1, age3Active0, age2Active2, age1Active4, empty])
  }

  @Test
  public void testMoreActiveThan() throws Throwable {

    Collections.sort(nodes, new NodeInstance.MoreActiveThan(0))
    assertListEquals(nodes, [age1Active4, age2Active2, age4Active1, age3Active0])
  }

  @Test
  public void testMoreActiveThanEmpty() throws Throwable {

    Collections.sort(nodesPlusEmpty, new NodeInstance.MoreActiveThan(0))
    assertListEquals(nodesPlusEmpty, [age1Active4, age2Active2, age4Active1, age3Active0, empty])
  }

}
