<a href="https://www.jahia.com/">
    <img src="https://www.jahia.com/modules/jahiacom-templates/images/jahia-3x.png" alt="Jahia logo" title="Jahia" align="right" height="60" />
</a>

Jahia OAuth
================

This module exposes an API which allows you to manage open authentication on your site.

### MINIMAL REQUIREMENTS
* Jahia 8.0.0.0
* Jahia Authentication 1.0.0

### INSTALLATION
Download the jar and deploy it on your instance, then activate the module on the site you wish to use.  
You will also need to download connectors and actions (mappers or providers) to use this module.

### HOW TO USE IT?
* Go to your `site > site settings > Jahia Authentication`
* In the panel you will see the list of OAuth connectors that are available for your site and if you open the card you will see the parameters to fill in order to activate and use it
* You will need to go to the open authentication website of your connector to setup an app and get the parameters
* Once this is done a new button will appear `Actions` and if you click on it you will access to the action modules part
* On this part you can activate as many action modules type mapper as you which but you can only activate one provider
* Create a mapping for the provider
* Then in edit mode add the connection button of your connector to a page
* Publish your site
* Your users can now connect using open authentication

### AVAILABLE CONNECTORS
* Google open authentication - It allows you to display a button `Sign in with Google` on your site

### WHAT IS A CONNECTOR?
A connector is a module that will allow your user to connect using the open authentication API like Facebook or LinkedIn.  
You can find connectors made by Jahia:
* [Facebook OAuth Connector](https://github.com/Jahia/facebook-oauth-connector)
* [LinkedIn OAuth Connector](https://github.com/Jahia/linkedin-oauth-connector)

### HOW TO MAKE YOUR OWN CONNECTOR?
Create a connector is really easy however there is a few requirements:
* You should make sure that the open authentication API you want to add is handled by Jahia OAuth, you can find a complete list in the spring file [here](https://github.com/Jahia/jahia-oauth/blob/master/src/main/resources/META-INF/spring/jahia-oauth.xml)
* The open authentication protocol must be 2.x
* You must have an understanding of DX modules and AngularJS

Once those points are OK you can start and follow the steps:
* Create a module using DX studio
* Add the dependency to Jahia OAuth
* In the `pom.xml` add the following:
```xml
<dependencies>
    <dependency>
        <groupId>org.jahia.modules</groupId>
        <artifactId>jahia-oauth</artifactId>
        <version>3.0.0</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>org.jahia.modules</groupId>
        <artifactId>jahia-authentication</artifactId>
        <version>1.0.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <artifactId>jahia-maven-plugin</artifactId>
            <groupId>org.jahia.server</groupId>
            <executions>
                <execution>
                    <id>i18n2js</id>
                    <goals>
                        <goal>javascript-dictionary</goal>
                    </goals>
                    <configuration>
                        <dictionaryName>myConnectoroai18n</dictionaryName>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```
* Update the `<dictionaryName>myConnectoroai18n</dictionaryName>` by your own name
* In the `definitions.cnd` add the 2 following nodes types
```cnd
<jnt = 'http://www.jahia.org/jahia/nt/1.0'>
<jmix = 'http://www.jahia.org/jahia/mix/1.0'>
<joant = 'http://www.jahia.org/jahia/joa/nt/1.0'>
<joamix = 'http://www.jahia.org/jahia/joa/mix/1.0'>

[joant:myConnectorOAuthView] > jnt:content, joamix:oauthConnectorView

[joant:myConnectorButton] > jnt:content, joamix:oauthButtonConnector
```
* Then create a content template using the studio or directly in your repository.xml
```xml
<myConnector-oauth-view j:defaultTemplate="false"
                                 j:hiddenTemplate="true"
                                 j:invertCondition="false"
                                 j:requireLoggedUser="false"
                                 j:requirePrivilegedUser="false"
                                 jcr:primaryType="jnt:contentTemplate">
    <pagecontent jcr:primaryType="jnt:contentList">
        <myconnectoroauthview jcr:primaryType="joant:myConnectorOAuthView"/>
    </pagecontent>
</myConnector-oauth-view>
```
* Create a view for the node type `joant:myConnectorOAuthView` that will be displayed in your content template
* Create a view for the node type `joant:myConnectorButton` that will be used to display the connection button
* Create a folder `javascript` with a sub-folder `myconnector-oauth-connector` and create a js file `myconnector-controller.js` to use in the view of your component `joant:myConnectorOAuthView`
* Extends existing implementation if needed for the 2 actions and ConnectorService
* To fill those files please use the existing connectors as example  

**Note:**  
In your blueprint file there is a few details that you must follow:
* The structure of the available properties
* The connectorServiceName must be the same name as referenced in the spring file of Jahia OAuth module and it must be the same across your module (JS and Java)
* You will need to declare your osgi service and reference the one from Jahia OAuth module
Example with Facebook
```xml
<osgi:reference id="jahiaOAuthService" interface="org.jahia.modules.jahiaoauth.service.JahiaOAuthService" availability="mandatory"/>

<osgi:service ref="FacebookConnectorImpl" interface="org.jahia.modules.jahiaoauth.service.ConnectorService">
    <osgi:service-properties>
        <entry key="connectorServiceName" value="FacebookApi"/>
    </osgi:service-properties>
</osgi:service>
```

### WHAT IS AN ACTION MODULE?
An action module is a module that will allow you to execute some action after a user tried to register using a connector.  
There are two kinds of action modules, provider or data mapper.  
Provider will most likely perform the connection once a user tries to login using a connector. A data mapper will register the user data where you need it.
You can find action modules made by Jahia:
* [JCR OAuth provider](https://github.com/Jahia/jcr-oauth-provider)
* [Marketing Factory OAuth data mapper](https://github.com/Jahia/Marketing-Factory-OAuth-data-mapper)

### HOW TO MAKE YOUR OWN ACTION MODULE?
Create an action module is a bit more complex than the connector but mainly because it will be very dependant of what you want to do.  
Here again there is a few requirements:
* You must have an understanding of DX modules and AngularJS

Once those points are OK you can start and follow the steps:
* Create a module using DX studio
* Add the dependency to Jahia OAuth
* In the `pom.xml` add the following:
```xml
<dependencies>
    <dependency>
        <groupId>org.jahia.modules</groupId>
        <artifactId>jahia-authentication</artifactId>
        <version>1.0.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.felix</groupId>
            <artifactId>maven-bundle-plugin</artifactId>
            <extensions>true</extensions>
            <configuration>
                <instructions />
            </configuration>
        </plugin>
        <plugin>
            <artifactId>jahia-maven-plugin</artifactId>
            <groupId>org.jahia.server</groupId>
            <executions>
                <execution>
                    <id>i18n2js</id>
                    <goals>
                        <goal>javascript-dictionary</goal>
                    </goals>
                    <configuration>
                        <dictionaryName>myActionoai18n</dictionaryName>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```
* Update the `<dictionaryName>myActionoai18n</dictionaryName>` by your own name
* In the `definitions.cnd` add the following node type
```cnd
[joant:actionOAuthView] > jnt:content, joamix:oauthMapperView
```
* Then create a content template using the studio or directly in your repository.xml
```xml
<action-oauth-view j:applyOn=""
                              j:defaultTemplate="false"
                              j:hiddenTemplate="true"
                              jcr:primaryType="jnt:contentTemplate">
    <pagecontent jcr:primaryType="jnt:contentList">
        <actionoauthview jcr:primaryType="joant:actionOAuthView"/>
    </pagecontent>
</action-oauth-view>
```
* Create a view for the node type `joant:actionOAuthView` that will be displayed in your content template
* Create a folder `javascript` with a sub-folder `action-oauth-connector` and create a js file `action-controller.js` to use in the view of your component `joant:actionOAuthView`
* On the java part you can do pretty much what you want there is an interface that can be implemented `org.jahia.modules.jahiaauth.service.Mapper`
* To fill those files please use the existing action module as example

**Note:**  
In your blueprint file there is a few details that you must follow:
* The structure of the properties
* The mapperServiceName must be the same across your module (JS and Java)

## Open-Source

This is an Open-Source module, you can find more details about Open-Source @ Jahia [in this repository](https://github.com/Jahia/open-source).

