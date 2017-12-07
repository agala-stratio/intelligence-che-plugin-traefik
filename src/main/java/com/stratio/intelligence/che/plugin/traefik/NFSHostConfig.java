/*
 * Copyright (c) 2017. Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */

package com.stratio.intelligence.che.plugin.traefik;

import org.eclipse.che.plugin.docker.client.json.HostConfig;

/** Proxy pattern for HostConfig */
public class NFSHostConfig extends HostConfig {
  private String volumeDriver;

  public NFSHostConfig(HostConfig hostConfig) {

    // TODO change by BeanUtils / Java reflections to call dynamically every set method with its
    // get/is
    // (maybe this way better because is needed to avoid usage try catch blocks with for GSON.toJson
    // )

    this.setBinds(hostConfig.getBinds());

    this.setLinks(hostConfig.getLinks());
    this.setLxcConf(hostConfig.getLxcConf());
    this.setPublishAllPorts(hostConfig.isPublishAllPorts());
    this.setPrivileged(hostConfig.isPrivileged());
    // FIXME securityOpt ??
    this.setDns(hostConfig.getDns());
    this.setDnsSearch(hostConfig.getDnsSearch());
    this.setExtraHosts(hostConfig.getExtraHosts());
    this.setVolumesFrom(hostConfig.getVolumesFrom());
    this.setCapAdd(hostConfig.getCapAdd());
    this.setCapDrop(hostConfig.getCapDrop());
    this.setRestartPolicy(hostConfig.getRestartPolicy());
    this.setNetworkMode(hostConfig.getNetworkMode());
    this.setDevices(hostConfig.getDevices());
    this.setContainerIDFile(hostConfig.getContainerIDFile());
    this.setMemory(hostConfig.getMemory());
    this.setMemorySwap(hostConfig.getMemorySwap());
    this.setLogConfig(hostConfig.getLogConfig());
    this.setIpcMode(hostConfig.getIpcMode());
    this.setCgroupParent(hostConfig.getCgroupParent());
    this.setCpuShares(hostConfig.getCpuShares());
    this.setCpusetCpus(hostConfig.getCpusetCpus());
    this.setPidMode(hostConfig.getPidMode());
    this.setReadonlyRootfs(hostConfig.isReadonlyRootfs());
    this.setUlimits(hostConfig.getUlimits());
    this.setCpuQuota(hostConfig.getCpuQuota());
    this.setCpuPeriod(hostConfig.getCpuPeriod());
    this.setPortBindings(hostConfig.getPortBindings());
    this.setMemorySwappiness(hostConfig.getMemorySwappiness());
    this.setPidsLimit((int) hostConfig.getPidsLimit());
  }

  @Override
  public String toString() {
    return removeLastChar(super.toString()) + ", volumeDriver='" + this.volumeDriver + '\'' + '}';
  }

  private static String removeLastChar(String str) {
    return str.substring(0, str.length() - 1);
  }

  public void setVolumeDriver(String volumeDriver) {
    this.volumeDriver = volumeDriver;
  }

  public String getVolumeDriver() {
    return this.volumeDriver;
  }
}
