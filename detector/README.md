# Jena - detector module
This module can detect enterprise anti-patterns in project of your choosing. Beware that Jena can detect anti-pattern only
in source code you suply to it and that it will function badly if the analyzed projects uses tools such as Lombok.  
Before running the detector run the [docker compose file](compose.yml).
It creates a database so Jena has where to store anti-pattern and metadata.
After running the database you can run Jena using the main method.
### Project preparation
Before using detectIssues command on project make use it has its dependencies exposed in target/dependency.
For maven project use this plugin (replace `current-jead-version` by the actual used versio of Jead):

```xml

<plugin>
    <groupId>cz.muni.fi.jead</groupId>
    <artifactId>jead-maven-plugin</artifactId>
    <version>current-jead-version</version>
    <executions>
        <execution>
            <id>copy-dependencies</id>
            <phase>package</phase>
            <goals>
                <goal>copy-dependencies</goal>
            </goals>
        </execution>
        <execution>
            <id>delombok</id>
            <phase>package</phase>
            <goals>
                <goal>delombok</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
You might need to add also plugin repository definition:
```xml
<pluginRepositories>
    <pluginRepository>
      <releases>
        <enabled>true</enabled>
      </releases>
      <snapshots>
        <enabled>true</enabled>
      </snapshots>
      <id>jead-github</id>
      <url>https://x-access-token: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@maven.pkg.github.com/Ricaps/Jead</url>
    </pluginRepository>
  </pluginRepositories>
```

For Gradle use these tasks (replace `<current-jead-version>` with actual Jead version used):
```groovy
buildscript {
    repositories {
        mavenLocal()
        maven {
            name = "jead-github"
            url = uri('https://x-access-token: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@maven.pkg.github.com/Ricaps/Jead')

        }
    }

    dependencies {
        classpath("cz.muni.fi.jead:jead-gradle-plugin:<current-jead-version>") {
            changing = true
        }
    }
    configurations.classpath {
        resolutionStrategy.cacheChangingModulesFor 0, 'seconds'
    }
}

apply plugin: 'jead-gradle-plugin'

compileJava.finalizedBy delombok
compileJava.finalizedBy copyDependencies
```
Jena's test require the dependencies of [antipatterns](../antipatterns), [AuthorizationServer](../AuthorizationServer),
and [PowerMockUsage](../PowerMockUsage) to be exposed. You can ensure that happens by running Maven package on those modules.

### Example usage
Let's say there is a project in E:/WorkSpace/Sample01 and you would like to analyze it. First you would need to add a plugin
or Gradle tasks to it. Jena can add this plugin/task to it for you using following command:
```
prepareProjects -d E:/WorkSpace/Sample01
```
Or you could use following command if you have multiple project you would like to prepare in this directory:
```
prepareProjects -d E:/WorkSpace
```
Then in E:/WorkSpace/Sample01 run Maven package if it is Maven project or run the Gradle tasks and verify that there are
jars in E:/WorkSpace/Sample01/target/dependency. Then in Jena you can use following command to find out what issue this project has:
```
detectIssues -p E:/WorkSpace/Sample01
```
If there are a lot of issues you can use following command in Jena to view summary of the issue in the project:
```
aggregateIssues
```
You can use help command to find out all commands of jena and
use help + name of command to find out description of all attributes of 
command. For example following command tells you about what does 
detectIssues do:
```
help detectIssues
```
### Issue types
Jena can detect following issues:
- DI1 Unused Injection - Injected field is unused.
- DI2 Direct Container Call - Direct call of DI container for example call of
org.springframework.beans.factory.BeanFactory.getBean.
- DI3 Concrete Class Injection - Concrete class is injected instead of class hidden behind interface or abstract class.
- DI4 Open Window Injection - The injected dependency is passed to parameters of methods or constructors of different classes or returned
by method for example by getter.
- DI5 Framework Coupling - The use of framework specific annotations such as
org.springframework.beans.factory.annotation.Autowired instead of javax.inject.Inject
- DI6 Multiple Forms Of Injection - The dependency is injected by two or more ways.
- DI7 Open Door Injection - Injected dependency is open to modification usually using setter.
- DI8 God Di Class - Class that has too many dependencies injected into it.
- DI9 Multiple Assigned Injection - Same as in DI4 Open Window Injection or injected dependency is assigned to multiple fields.
- DI10 Long Producer Method - Producer method should be responsible only for providing the dependency. If it is too
long it might suggest it handles much more.
- MOC1 Final Method Call With Exception - Final method throwing exception is called. It's mocking might be problematic.
- MOC2 Constructor Call With Exception - Constructor throwing exception is called. It's mocking might be problematic.
- MOC3 Static Method Call With Exception - Static method throwing exception is called. It's mocking might be problematic.
- MOC4 Inappropriate Method Mocking - Static or final method are mocked.
- PER1 N Plus1 Query Problem - N + 1 queries are used to load data instead of loading them in single query.
- SEC1 Storing Secrets In Insecure Places - There are secrets stored in application.yml or in application.properties.
- SEC2 Disabling Csrf Protection - Protection against CSRF attacks is disabled.
- SEC3 Lifelong Access Tokens - The lifetime of access tokens is too long.
- SEC4 Insecure Default Configuration - Insecure default configuration was chosen. For example
  org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder.BCryptPasswordEncoder's default constructor defaults the
strength to 10 but 14 is considered secure.
- SEC5 Signing Jwt With Fixed Secret - JWT token is signed with predefined fixed key instead of being signed by one of
randomly generated keys the application manages.
- SEC6 Insecure Communication - For example using http instead of https
- SER1 Tiny Service - Too small service
- SER2 Multi Service - Too big service

### Configuration
While Jena's default configuration is very good most of the time there might situations where we would recommend you to change it.
For example if your application uses some new dependency injection framework you might want check if Jena is looking for the
correct annotations or you might want to add more encryption algorithms other than org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder.BCryptPasswordEncoder
to Jena's configuration so it can check if they don't have set too low of a strength. 
Jena's configuration is in a form of JSON file. You can export the default configuration using following command:
```
copyConfig -p absolutePath
```
Then modify the file and use its absolute path as a option to detectIssues command. Following is a description of all
attributes in the configuration file:
- diConfiguration.injectionAnnotations - Array of fully qualified names of injection annotations. Jena uses this array in
detection of all dependency injection anti-patterns.
- diConfiguration.maxNumberOfInjections - Jena uses this number as a threshold for the number of injected dependencies
class is allowed to have. If it has more injected dependencies then there is a DI8 God Di Class anti-pattern.
- diConfiguration.maxProducerMethodComplexity - Jena uses this number as a threshold for the cyclomatic complexity of
producer methods. If a producer method has greater cyclomatic complexity then it is marked as DI10 Long Producer Method
anti-pattern.
- diConfiguration.producerAnnotations - Array of fully qualified names of producer annotation. Jena uses this to identify
producer methods when identifying DI10 Long Producer Method anti-patterns.
- diConfiguration.directContainerCallMethods - Array of fully qualified names of DI container methods. All calls of these
methods will be marked as DI2 Direct Container Call anti-patterns.
- mockingConfiguration.exceptions - Array of fully qualified names of Exceptions. Jena uses these exception while detecting
mocking anti-patterns. For more details see descriptions of MOC1, MOC2 and MOC3 anti-patterns. This array are the exceptions
reference in these anti-patterns.
- mockingConfiguration.mockingMethods - Array of fully qualified names of methods for mocking. It also contains some more details
about these methods. Jena uses them when identifying MOC4 anti-patterns. 
- securityConfiguration.sensitiveInformationRegex - Regex Jena uses to identify sensitive information in configuration files
while searching for SEC1 anti-patterns.
- securityConfiguration.configurationFileRegex - Regex Jena uses to identify configuration files by name while serching for SEC1
anti-patterns.
- securityConfiguration.tokenLifetimeSettings - Maximum duration of tokens lifetime Jena uses to identify SEC3 anti-patterns.
- securityConfiguration.encryptionAlgorithms - Array of encryption algorithms Jena searches for when identifying SEC4 anti-patterns.
- securityConfiguration.jwtSigningMethods - Array of JWT signing methods Jena uses when identifying SEC5 anti-patterns.
- securityConfiguration.unsecureCommunicationRegexes - Regexes Jena uses to identify insecure communication. Each string
matching any of these regexes will be marked as SEC6 anti-pattern.
- serviceLayerConfiguration.minServiceMethods - Jena uses this number as threshold while identifying SER1 anti-patterns.
- serviceLayerConfiguration.maxServiceMethods - Jena uses this number as threshold while identifying SER2 anti-patterns.
- serviceLayerConfiguration.serviceAnnotations - Array of fully qualified names of service annotation Jena uses to
search for service when identifying SER1 and SER2 anti-patterns.
- persistenceConfiguration.nPlusOneQueryRegex - Regex Jena uses while identifying potential candidates for N+1 query
anti-patterns.
- persistenceConfiguration.queryMethods - Array of fully qualified names of query method. Jena uses this array while searching
for PER1 anti-patterns.
