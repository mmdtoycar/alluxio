/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio.client.file.policy;

import alluxio.Constants;
import alluxio.client.block.BlockWorkerInfo;
import alluxio.wire.WorkerNetAddress;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests {@link DeterministicHashPolicy}.
 */
public final class DeterministicHashPolicyTest {
  private static final int PORT = 1;

  private List<BlockWorkerInfo> mWorkerInfos;

  @Before
  public void before() {
    mWorkerInfos = new ArrayList<>();
    mWorkerInfos.add(new BlockWorkerInfo(new WorkerNetAddress().setHost("worker1")
        .setRpcPort(PORT).setDataPort(PORT).setWebPort(PORT), Constants.GB, 0));
    mWorkerInfos.add(new BlockWorkerInfo(new WorkerNetAddress().setHost("worker2")
        .setRpcPort(PORT).setDataPort(PORT).setWebPort(PORT), 2 * (long) Constants.GB, 0));
    mWorkerInfos.add(new BlockWorkerInfo(new WorkerNetAddress().setHost("worker3")
        .setRpcPort(PORT).setDataPort(PORT).setWebPort(PORT), 3 * (long) Constants.GB, 0));
    mWorkerInfos.add(new BlockWorkerInfo(new WorkerNetAddress().setHost("worker4")
        .setRpcPort(PORT).setDataPort(PORT).setWebPort(PORT), 3 * (long) Constants.GB, 0));
  }

  @Test
  public void getWorkerDeterministically() {
    DeterministicHashPolicy policy = (DeterministicHashPolicy) BlockLocationPolicy.Factory
        .create("alluxio.client.file.policy.DeterministicHashPolicy");
    String host = policy.getWorkerForBlock(mWorkerInfos, 1, 2 * (long) Constants.GB).getHost();
    for (int i = 0; i < 10; i++) {
      DeterministicHashPolicy p = (DeterministicHashPolicy) BlockLocationPolicy.Factory
          .create("alluxio.client.file.policy.DeterministicHashPolicy");
      // For the same block, always return the same worker.
      Assert.assertEquals(host,
          p.getWorkerForBlock(mWorkerInfos, 1, 2 * (long) Constants.GB).getHost());
      Assert.assertEquals(host,
          p.getWorkerForBlock(mWorkerInfos, 1, 2 * (long) Constants.GB).getHost());
    }
  }

  @Test
  public void getWorkerEnoughCapacity() {
    DeterministicHashPolicy policy = (DeterministicHashPolicy) BlockLocationPolicy.Factory
        .create("alluxio.client.file.policy.DeterministicHashPolicy");
    for (long blockId = 0; blockId < 100; blockId++) {
      // worker1 does not have enough capacity. It should never be picked.
      Assert.assertNotEquals("worker1",
          policy.getWorkerForBlock(mWorkerInfos, blockId, 2 * (long) Constants.GB).getHost());
    }
  }

  @Test
  public void getWorkerMultipleShards() {
    DeterministicHashPolicy policy2 = (DeterministicHashPolicy) BlockLocationPolicy.Factory
        .create("alluxio.client.file.policy.DeterministicHashPolicy@2");
    Set<String> addresses1 = new HashSet<>();
    Set<String> addresses2 = new HashSet<>();
    for (int i = 0; i < 100; i++) {
      addresses1
          .add(policy2.getWorkerForBlock(mWorkerInfos, 1, 2 * (long) Constants.GB).getHost());
      addresses2
          .add(policy2.getWorkerForBlock(mWorkerInfos, 1, 2 * (long) Constants.GB).getHost());
    }
    // With sufficient traffic, 2 (= #shards) workers should be picked to serve the block.
    Assert.assertEquals(2, addresses1.size());
    Assert.assertEquals(2, addresses2.size());
    Assert.assertEquals(addresses1, addresses2);
  }
}
