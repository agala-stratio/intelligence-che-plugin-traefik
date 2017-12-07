/*
 * Copyright (c) 2017. Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.intelligence.che.plugin.traefik;

import static java.lang.String.format;

import com.google.common.collect.ImmutableSet;
import com.google.inject.name.Named;
import java.util.*;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang.ArrayUtils;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.plugin.docker.client.DockerConnector;
import org.eclipse.che.plugin.docker.client.json.ContainerConfig;
import org.eclipse.che.plugin.docker.client.json.ImageInfo;
import org.eclipse.che.plugin.docker.client.params.CreateContainerParams;
import org.eclipse.che.plugin.docker.client.params.InspectImageParams;
import org.eclipse.che.plugin.docker.machine.CustomServerEvaluationStrategy;
import org.eclipse.che.plugin.docker.machine.ServerEvaluationStrategy;
import org.eclipse.che.plugin.docker.machine.ServerEvaluationStrategyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Traefik has a listener on Docker containers. Each time a container is added or removed, it checks
 * if the container has specific Traefik Labels and then create routes based upon these labels. The
 * job of this interceptor is to add Traefik labels prior the start of the container (it will be
 * done when we create the container) by adding Traefik labels. The routes are built using the
 * custom strategy template.
 */
public class TraefikCreateContainerInterceptor implements MethodInterceptor {

  private static final Logger LOG =
      LoggerFactory.getLogger(TraefikCreateContainerInterceptor.class);

  /** Inject the server evaluation strategy provider. */
  private ServerEvaluationStrategyProvider serverEvaluationStrategyProvider;

  /** Template. */
  private String template;

  /**
   * Grab labels of the config and from image to get all exposed ports and the labels defined if any
   *
   * @param methodInvocation intercepting data of createContainer method on {@link DockerConnector}
   * @return the result of the intercepted method
   * @throws Throwable if there is an exception
   */
  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {

    ServerEvaluationStrategy serverEvaluationStrategy = serverEvaluationStrategyProvider.get();
    // Abort if custom server evaluation strategy is not enabled.
    if (!(CustomServerEvaluationStrategy.class.isInstance(serverEvaluationStrategy))) {
      return methodInvocation.proceed();
    }
    final CustomServerEvaluationStrategy customServerEvaluationStrategy =
        (CustomServerEvaluationStrategy) serverEvaluationStrategy;

    // Get the connector
    DockerConnector dockerConnector = (DockerConnector) methodInvocation.getThis();

    // only one parameter which is CreateContainerParams
    CreateContainerParams createContainerParams =
        (CreateContainerParams) methodInvocation.getArguments()[0];

    // Grab container configuration
    ContainerConfig containerConfig = createContainerParams.getContainerConfig();

    /** test */
    // Env vars
    LOG.info("Container Initial Env: \n{}", containerConfig.getEnv());

    // TODO get from DOCKER ENV_VARIABLE
    String[] newEnv = {"STRATIO_VAULT_PATH=/path/to/vault"};
    containerConfig.setEnv((String[]) ArrayUtils.addAll(containerConfig.getEnv(), newEnv));

    String[] cmd = {
      "/bin/sh", "-c", "sleep 20; echo $STRATIO_VAULT_PATH > /tmp/stratio.file; tail -f /dev/null"
    };

    LOG.info("Container Initial Cmd: \n{}", containerConfig.getCmd());

    containerConfig.setCmd(cmd);

    /** end-test */
    containerConfig.setUser("root");

    /**
     * TODO change withVolumeDriver("nfs") as a DOCKER ENV_VARIABLE NFSHostConfig nfsHostConfig =
     * new NFSHostConfig(containerConfig.getHostConfig()); nfsHostConfig.setVolumeDriver("");
     * containerConfig.setHostConfig(nfsHostConfig);
     */
    LOG.info("Container Config: \n{}", containerConfig.toString());

    String image = containerConfig.getImage();

    // first, get labels defined in the container configuration
    Map<String, String> containerLabels = containerConfig.getLabels();

    // Also, get labels from the image itself
    final ImageInfo imageInfo = dockerConnector.inspectImage(InspectImageParams.create(image));
    Map<String, String> imageLabels = imageInfo.getConfig().getLabels();

    // Now merge all labels
    final Map<String, String> allLabels = new HashMap<>(containerLabels);
    if (imageLabels != null) {
      // If image has some labels, merge them
      allLabels.putAll(imageLabels);
    }

    // Get all ports exposed by the container and by the image
    // it is under the form "22/tcp"
    final Set<String> allExposedPorts =
        ImmutableSet.<String>builder()
            .addAll(containerConfig.getExposedPorts().keySet())
            .addAll(imageInfo.getConfig().getExposedPorts().keySet())
            .build();
    final String[] allEnv =
        Stream.concat(
                Arrays.stream(containerConfig.getEnv()),
                Arrays.stream(imageInfo.getConfig().getEnv()))
            .toArray(String[]::new);

    CustomServerEvaluationStrategy.RenderingEvaluation renderingEvaluation =
        customServerEvaluationStrategy.getOfflineRenderingEvaluation(
            allLabels, allExposedPorts, allEnv);

    // portValue is under format <port-number>/<tcp>
    allExposedPorts.forEach(
        (portValue) -> {
          final String serviceName = renderingEvaluation.render("service-<serverName>", portValue);
          final String port = portValue.split("/")[0];

          String hostnameAndPort = renderingEvaluation.render(this.template, portValue);

          /**
           * CHE_DOCKER_SERVER__EVALUATION__STRATEGY_CUSTOM_TEMPLATE hostname/<final_endpoint_id> or
           * host:port/<final_endpoint_id>
           */
          String[] elements = hostnameAndPort.split("/");
          String hostName = elements[0];
          final String serviceId = (elements.length > 1) ? elements[1] : "";
          final String path = format("PathPrefixStrip:/%s", serviceId);
          containerLabels.put(format("traefik.%s.port", serviceName), port);
          containerLabels.put(format("traefik.%s.frontend.entryPoints", serviceName), "http");
          containerLabels.put(format("traefik.%s.frontend.rule", serviceName), path);
          containerLabels.put("traefik.frontend.rule", createContainerParams.getContainerName());
        });

    return methodInvocation.proceed();
  }

  /**
   * Sets the server evaluation provider
   *
   * @param serverEvaluationStrategyProvider
   */
  @Inject
  protected void setServerEvaluationStrategyProvider(
      ServerEvaluationStrategyProvider serverEvaluationStrategyProvider) {
    this.serverEvaluationStrategyProvider = serverEvaluationStrategyProvider;
  }

  /**
   * Sets the template of server evaluation strategy
   *
   * @param cheDockerCustomExternalTemplate
   */
  @Inject
  protected void setTemplate(
      // TODO: change to CHE_DOCKER_SERVER__EVALUATION__STRATEGY_STRATIO_TEMPLATE ( stratio env
      // variable )
      @Nullable @Named("che.docker.server_evaluation_strategy.custom.template")
          String cheDockerCustomExternalTemplate) {
    this.template = cheDockerCustomExternalTemplate;
  }
}
