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

package org.apache.slider.providers.agent

import org.apache.slider.common.SliderExitCodes
import org.apache.slider.core.main.ServiceLaunchException
import org.apache.slider.core.exceptions.BadClusterStateException

import java.io.IOException;

import static org.apache.slider.common.params.Arguments.*
import static org.apache.slider.providers.agent.AgentKeys.*
import groovy.util.logging.Slf4j

import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.RawLocalFileSystem
import org.apache.hadoop.yarn.conf.YarnConfiguration
import org.apache.slider.client.SliderClient
import org.apache.slider.common.params.Arguments
import org.apache.slider.common.params.ClientArgs
import org.apache.slider.common.tools.SliderFileSystem
import org.apache.slider.common.tools.SliderUtils
import org.apache.slider.core.exceptions.SliderException
import org.apache.slider.core.main.ServiceLauncher
import org.apache.slider.providers.agent.AgentKeys
import org.apache.slider.api.ResourceKeys
import org.apache.slider.common.SliderXmlConfKeys
import org.apache.slider.test.SliderTestUtils
import org.junit.Before
import org.junit.Test

/**
 * Tests an echo command
 */
@Slf4j
class TestAddonPackage extends AgentTestBase {

  File slider_core
  String echo_py
  File echo_py_path
  File app_def_path
  String agt_ver
  File agt_ver_path
  String agt_conf
  File agt_conf_path
  
  static String APPCONFIG_PATH = "src/test/app_packages/test_addon_pkg/appConfig.json"
  static String RESOURCE_PATH = "src/test/app_packages/test_addon_pkg/resources.json"
  static String MASTER_PKG_PATH = "src/test/app_packages/test_addon_pkg/master-app-pkg.zip"
  static String ADD_ON_PACKAGE_NAME1 = "LISTEN1";
  static String ADD_ON_PACKAGE_ECHO_FOLDER = "src/test/app_packages/test_add_on_package/add-on-package-echo"
  static String ADD_ON_PACKAGE_ECHO_NAME = "add-on-package-echo"
  static String ADD_ON_PACKAGE_ECHO_FILE = "target/add-on-package-echo.zip";
  static String APP_NAME = "FAKE"
  
  @Before
  public void setupArtifacts() {
    slider_core = new File(new File(".").absoluteFile, "src/test/python");
    echo_py = "echo.py"
    echo_py_path = new File(slider_core, echo_py)
    app_def_path = new File(app_def_pkg_path)
    agt_ver = "version"
    agt_ver_path = new File(slider_core, agt_ver)
    agt_conf = "agent.ini"
    agt_conf_path = new File(slider_core, agt_conf)

  }
  
  @Override
  void checkTestAssumptions(YarnConfiguration conf) {

  }

  @Test
  public void testEchoApplicationAddPackage() throws Throwable {
    assumeValidServerEnv()
    SliderUtils.zipFolder(new File(ADD_ON_PACKAGE_ECHO_FOLDER), new File(ADD_ON_PACKAGE_ECHO_FILE))
    
    String clustername = createMiniCluster("",
        configuration,
        1,
        1,
        1,
        true,
        false)

    assert echo_py_path.exists()
    assert app_def_path.exists()
    assert agt_ver_path.exists()
    assert agt_conf_path.exists()
    
    def role = "echo"
    Map<String, Integer> roles = [
        (role): 2,
    ];
    ServiceLauncher<SliderClient> launcher = buildAgentCluster(clustername,
        roles,
        [
            ARG_OPTION, PACKAGE_PATH, slider_core.absolutePath,
            ARG_OPTION, APP_DEF, toURIArg(app_def_path),
            ARG_OPTION, AGENT_CONF, toURIArg(agt_conf_path),
            ARG_OPTION, AGENT_VERSION, toURIArg(agt_ver_path),
            ARG_RES_COMP_OPT, role, ResourceKeys.COMPONENT_PRIORITY, "1",
            ARG_COMP_OPT, role, SCRIPT_PATH, echo_py,
            ARG_COMP_OPT, role, SERVICE_NAME, "Agent",
            ARG_DEFINE,
            SliderXmlConfKeys.KEY_SLIDER_AM_DEPENDENCY_CHECKS_DISABLED + "=false",
            ARG_COMP_OPT, role, TEST_RELAX_VERIFICATION, "true",
            ARG_ADDON, ADD_ON_PACKAGE_ECHO_NAME, ADD_ON_PACKAGE_ECHO_FILE
        ],
        true, true,
        true)
        
    SliderClient sliderClient = launcher.service

    waitForRoleCount(sliderClient, roles, 15000)
    //sleep a bit
    sleep(5000)
    //expect the role count to be the same
    waitForRoleCount(sliderClient, roles, 1000)

    // flex size
    // while running, flex it with no changes
    sliderClient.flex(clustername, [(role): 2]);
    sleep(5000)
    waitForRoleCount(sliderClient, roles, 1000)

    // flex to an illegal value
    try {
      sliderClient.flex(clustername, [(role): -1]);
      fail("expected an exception")
    } catch (BadClusterStateException e) {
      assertExceptionDetails(e, SliderExitCodes.EXIT_BAD_STATE, "negative")
    }
  }
}