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

package org.apache.slider.server.appmaster.model.mock

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId
import org.apache.hadoop.yarn.api.records.ApplicationId
import org.apache.hadoop.yarn.api.records.Container
import org.apache.hadoop.yarn.api.records.ContainerId
import org.apache.hadoop.yarn.client.api.AMRMClient
import org.apache.slider.server.appmaster.operations.AbstractRMOperation
import org.apache.slider.server.appmaster.operations.CancelSingleRequest
import org.apache.slider.server.appmaster.operations.ContainerReleaseOperation
import org.apache.slider.server.appmaster.operations.ContainerRequestOperation
import org.junit.Assert

/**
 * This is an evolving engine to mock YARN operations
 */
@CompileStatic
@Slf4j
class MockYarnEngine {

  MockYarnCluster cluster;
  Allocator allocator;
  List<ContainerRequestOperation> pending = [];

  ApplicationId appId = new MockApplicationId(
      id: 0,
      clusterTimestamp: 0,
      )

  ApplicationAttemptId attemptId = new MockApplicationAttemptId(
      applicationId: appId,
      attemptId: 1,
      )

  @Override
  String toString() {
    return "MockYarnEngine $cluster + pending=${pending.size()}" 
  }

  int containerCount() {
    return cluster.containersInUse();
  }

  MockYarnEngine(int clusterSize, int containersPerNode) {
    cluster = new MockYarnCluster(clusterSize, containersPerNode)
    allocator = new Allocator(cluster)
  }

  /**
   * Allocate a container from a request. The containerID will be
   * unique, nodeId and other fields chosen internally with
   * no such guarantees; resource and priority copied over
   * @param request request
   * @return container
   */
  Container allocateContainer(AMRMClient.ContainerRequest request) {
    MockContainer allocated = allocator.allocate(request)
    if (allocated != null) {
      MockContainerId id = allocated.id as MockContainerId
      id.applicationAttemptId = attemptId;
    }
    return allocated
  }

  MockYarnCluster.MockYarnClusterContainer releaseContainer(ContainerId containerId) {
    return cluster.release(containerId)
  }

  /**
   * Process a list of operations -release containers to be released,
   * allocate those for which there is space (but don't rescan the list after
   * the scan)
   * @param ops
   * @return
   */
  List<Container> execute(List<AbstractRMOperation> ops) {
    return execute(ops, [])
  }

  /**
   * Process a list of operations -release containers to be released,
   * allocate those for which there is space (but don't rescan the list after
   * the scan). Unsatisifed entries are appended to the "pending" list
   * @param ops operations
   * @return the list of all satisfied operations
   */
  List<Container> execute(List<AbstractRMOperation> ops,
                               List<ContainerId> released) {
    validateRequests(ops)
    List<Container> allocation = [];
    ops.each { AbstractRMOperation op ->
      if (op instanceof ContainerReleaseOperation) {
        ContainerReleaseOperation cro = (ContainerReleaseOperation) op
        ContainerId cid = cro.containerId
        assert releaseContainer(cid);
        released.add(cid)
      } else if (op instanceof CancelSingleRequest) {
        // no-op
      } else if (op instanceof ContainerRequestOperation) {
        ContainerRequestOperation req = (ContainerRequestOperation) op
        Container container = allocateContainer(req.request)
        log.info("allocated container $container for $req")
        if (container != null) {
          log.info("allocated container $container for $req")
          allocation.add(container)
        } else {
          log.debug("Unsatisfied allocation $req")
          pending.add(req)
        }
      } else {
        log.warn("Unsupported operation $op")
      }
    }
    return allocation
  }

  /**
   * Try and mimic some of the logic of <code>AMRMClientImpl.checkLocalityRelaxationConflict</code>
   * @param ops operations list
   */
  void validateRequests(List<AbstractRMOperation> ops) {
    // run through the requests and verify that they are all consistent.
    List<ContainerRequestOperation> outstandingRequests = []
    for (AbstractRMOperation operation : ops) {
      if (operation instanceof ContainerRequestOperation) {
        ContainerRequestOperation containerRequest = (ContainerRequestOperation) operation
        def amRequest = containerRequest.request
        def priority = amRequest.priority
        def relax = amRequest.relaxLocality

        outstandingRequests.each { req ->

          if (req.priority == priority && req.relaxLocality != relax) {
            // mismatch in values
            Assert.fail("operation $operation has incompatible request priority" +
                        " from outsanding request")
          }
          outstandingRequests << containerRequest

        }

      }
    }
  }

  /**
   * Get the list of node reports. These are not cloned; updates will persist in the nodemap
   * @return current node report list
   */
  List<MockNodeReport> getNodeReports() {
    cluster.nodeReports
  }
}