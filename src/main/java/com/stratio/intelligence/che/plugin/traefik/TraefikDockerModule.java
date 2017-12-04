/*
 * Copyright (c) 2017. Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.intelligence.che.plugin.traefik;

import static com.google.inject.matcher.Matchers.subclassesOf;
import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getenv;
import static org.eclipse.che.inject.Matchers.names;

import com.google.inject.AbstractModule;
import org.eclipse.che.plugin.docker.client.DockerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Module for Traefik components. */
public class TraefikDockerModule extends AbstractModule {

  private static final Logger LOG = LoggerFactory.getLogger(TraefikDockerModule.class);

  /** Configure the traefik components */
  @Override
  protected void configure() {

    // add logic only if plug-in is enabled.
    if (parseBoolean(getenv("CHE_PLUGIN_TRAEFIK_STRATIO_ENABLED"))) {
      // add an interceptor to intercept createContainer calls and then get the final labels
      final TraefikCreateContainerInterceptor traefikCreateContainerInterceptor =
          new TraefikCreateContainerInterceptor();
      requestInjection(traefikCreateContainerInterceptor);
      bindInterceptor(
          subclassesOf(DockerConnector.class),
          names("createContainer"),
          traefikCreateContainerInterceptor);

      System.out.println("************\n\n\nTRAEFIK STRATIO PLUGIN LOADED\n\n\n************");
    }
  }
}
