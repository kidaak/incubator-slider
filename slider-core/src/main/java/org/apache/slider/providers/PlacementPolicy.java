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

package org.apache.slider.providers;

/**
 * Placement values.
 * This is nominally a bitmask, though not all values make sense
 */
public class PlacementPolicy {

  /**
<<<<<<< HEAD
<<<<<<< HEAD
   * Default values
   */
  public static final int DEFAULT = 0;
=======
=======
>>>>>>> refs/remotes/apache/develop
   * Default value: history used, anti-affinity hinted at on rebuild/flex up
   */
  public static final int NONE = 0;

  /**
   * Default value: history used, anti-affinity hinted at on rebuild/flex up
   */
  public static final int DEFAULT = NONE;
<<<<<<< HEAD
>>>>>>> refs/remotes/apache/develop
=======
>>>>>>> refs/remotes/apache/develop

  /**
   * Strict placement: when asking for an instance for which there is
   * history, mandate that it is strict
   */
  public static final int STRICT = 1;

  /**
<<<<<<< HEAD
<<<<<<< HEAD
   * No data locality; do not bother trying to ask for any location
   */
  public static final int NO_DATA_LOCALITY = 2;
  /**
   * Anti-affinity is mandatory. 
=======
   * No data locality; do not use placement history
   */
  public static final int NO_DATA_LOCALITY = 2;

  /**
   * Anti-affinity is mandatory.
>>>>>>> refs/remotes/apache/develop
=======
   * No data locality; do not use placement history
   */
  public static final int NO_DATA_LOCALITY = 2;

  /**
   * Anti-affinity is mandatory.
>>>>>>> refs/remotes/apache/develop
   */
  public static final int ANTI_AFFINITY_REQUIRED = 4;
  
  /**
   * Exclude from flexing; used internally to mark AMs.
   */
  public static final int EXCLUDE_FROM_FLEXING = 16;

}
