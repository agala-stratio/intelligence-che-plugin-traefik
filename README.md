**STRATIO TRAEFIK PLUGIN FOR ECLIPSE CHE**

**1. Plugin pom.xml considerations**

```
We have to add in our pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
       <groupId>com.stratio.intelligence</groupId>
        <artifactId>che-plugin-parent</artifactId>
        <groupId>org.eclipse.che.plugin</groupId>
        <version>5.20.1</version> <!-- USE HERE THE VERSION YOU WANT TO EXTEND -->
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>che-plugin-traefik-stratio</artifactId>
    <packaging>jar</packaging>
    <name>Che Plugin :: Traefik :: Stratio</name>
    <dependencies>
        <dependency>
..........
```
Add the repossitories:
```
<repositories>
        <repository>
            <id>codenvy-public-repo</id>
            <name>codenvy public</name>
            <url>https://maven.codenvycorp.com/content/groups/public/</url>
        </repository>
        <repository>
            <id>codenvy-public-snapshots-repo</id>
            <name>codenvy public snapshots</name>
            <url>https://maven.codenvycorp.com/content/repositories/codenvy-public-snapshots/</url>
        </repository>
    </repositories>
    <pluginRepositories>
        <pluginRepository>
            <id>codenvy-public-repo</id>
            <name>codenvy public</name>
            <url>https://maven.codenvycorp.com/content/groups/public/</url>
        </pluginRepository>
        <pluginRepository>
            <id>codenvy-public-snapshots-repo</id>
            <name>codenvy public snapshots</name>
            <url>https://maven.codenvycorp.com/content/repositories/codenvy-public-snapshots/</url>
        </pluginRepository>
........
```

**2. jar packaging+install**
```
mvn sortpom:sort
mvn fmt:format test 
mvn clean install
```
That installs the jar into your .m2 local repo
That creates our che-plugin-traefik-stratio.jar. Now we have to assembly as a dependency (in our case wsmaster dependency, 3 kinds of dependecies: wsmaster, wsagent and IDE)

**3. assembly to che project**

We now download the che project:
```
git clone https://github.com/eclipse/che.git
git checkout tags/5.20.1 // USE HERE THE VERSION YOU WANT TO EXTEND
in the root pom.xml we add our module as a dependency:
 <dependency>
    <groupId>com.stratio.intelligence.che.plugin</groupId>
    <artifactId>che-plugin-traefik-stratio</artifactId>
    <version>${che.version}</version> 
 </dependency>
```
then 
```
mvn sortpom:sort
```

Since our plugin belongs to wsmaster, we have to re-assembly the master:
in assembly/assembly-wsmaster-war pom we add:
```
<dependency>
    <groupId>com.stratio.intelligence.che.plugin</groupId>
    <artifactId>che-plugin-traefik-stratio</artifactId>
</dependency>
```

then 
```
mvn sortpom:sort
mvn clean install
```
and finally in assembly/assembly-main
```
mvn clean install
```
We have created then a new assembly in target/ folder
assembly/assembly-main/target/eclipse-che-5.20.1.tar.gz
so we can deploy that artifact into our cluster node and untar it. (Is mandatory)

**4. Plugin testing**

We can test it on marathon by adding in the json:

ENV:
```
"CHE_PLUGIN_TRAEFIK_STRATIO_ENABLED":"true",
"CHE_DOCKER_SERVER_EVALUATION_STRATEGY_CUSTOM_TEMPLATE" : "exposed hostname or host:port/<serverName><machineName><workspaceId>"
```
ex:
predatio.traefik/<serverName><machineName><workspaceId>

VOLUME MOUNTING:
```   
{ "containerPath": "/assembly", "hostPath": "<local_path>/assembly/assembly-main/target/eclipse-che-5.20.1/eclipse-che-5.20.1", "mode": "RW" }
```
we can check that the plugin is running by checking in the container logs
docker logs <container_id che-server>
```
....
TRAEFIK STRATIO PLUGIN LOADED
....
```
