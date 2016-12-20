Steps to expose a method/class as a REST service

Step 1: create a class like:
	@ApplicationPath("/")
	public class ApplicationConfig extends Application {
		
	    public Set<Class<?>> getClasses() {
	        return new HashSet<Class<?>>(Arrays.asList(
	        		LibraryServiceImpl.class, 
	        		RestSecurityInterceptor.class, 
	        		ResponseHelperInterceptor.class, 
	        		ServiceExceptionMapper.class)
	        	);
	    }
	}
	
Step 2: Add '@Path' REST annotation on the interface (LibraryService) like 
			@Path("/library")
			public interface LibraryService {.....

Step 3:	Add the method level annotation like for retrieving methods
 
		@GET
		@Path("/books")
		@Produces({"application/json", "application/xml"})
		Collection<Book> getBooks();	
		
		
		
		
		
Securing REST end-points

Step 1: Add "<security-domain>training</security-domain>" tag in jboss-web.xml file
Step 2: Update the web.xml with security tags [web.xml way]

		<security-constraint>
			<web-resource-collection>
				<web-resource-name>LibraryService</web-resource-name>
				<description>application security constraints</description>
				<url-pattern>/library/*</url-pattern>
				<http-method>GET</http-method>
				<http-method>POST</http-method>
			</web-resource-collection>
			<auth-constraint>
				<role-name>admin</role-name>
				<role-name>user</role-name>
			</auth-constraint>
		</security-constraint>
		<login-config>
			<auth-method>BASIC</auth-method>
		</login-config>
		<security-role>
			<role-name>admin</role-name>
		</security-role>
		<security-role>
			<role-name>user</role-name>
		</security-role>
		
		OR
		
		[Annotation way]
		Add @RolesAllowed("user") annotation on all GET methods and 
		@RolesAllowed("admin") for all PUT and DELETE methods
		
Step 4: Create a security-domain tag in Wildfly/standalone/standalone.xml file
	<security-domain name="training" cache-type="default">
        <authentication>
            <login-module code="org.jboss.security.auth.spi.DatabaseServerLoginModule" flag="required">
                <module-option name="dsJndiName" value="java:jboss/datasources/ExampleDS"/>
                <module-option name="principalsQuery" value="select passwd from Users username where username=?"/>
                <module-option name="rolesQuery" value="select userRoles, RoleGroup from UserRoles where username=?"/>
                <module-option name="hashEncoding" value="base64"/>
                <module-option name="hashUserPassword" value="false"/>
            </login-module>
        </authentication>
    </security-domain>
    
Step 5: Update the ExampleDS datasource's copnnection url with the one below. Note the location of the 		craete.sql file.

		<connection-url>jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;;INIT=RUNSCRIPT FROM 'c:/users/prasengupta/create.sql'</connection-url>
		
Step 6: Create the SQL tables

		DROP TABLE IF EXISTS Principals;
		DROP TABLE IF EXISTS UserRoles;
		
		CREATE TABLE Principals (
		username VARCHAR, 
		password VARCHAR);
		
		CREATE TABLE UserRoles (
		username VARCHAR, 
		Role VARCHAR, 
		RoleGroup VARCHAR);
		
		
		INSERT INTO Principals(username, password) values ('testuser1', 'password1');
		INSERT INTO UserRoles(username, Role, RoleGroup) values ('testuser1', 'admin', 'admin');
		
		INSERT INTO Principals(username, password) values ('testuser2', 'password2');
		INSERT INTO UserRoles(username, Role, RoleGroup) values ('testuser2', 'user', 'user');



Keycloak steps:

	
Step 1: Start a Keycloak Server
		bin/standalone.sh -Djboss.socket.binding.port-offset=100

Step 2: Creating a Client in Keycloak
Open Keycloak admin console in browser and Sign in. Click on Clients in the menu on the left hand side. Once that's open click on Create on top of the table. On the next screen fill in the following values:

Client ID: service
Access Type: bearer-only
Then click on Save. In summary what you've just done is to configure a client with Keycloak. The client id is used for the client to identify itself to Keycloak. Setting the access type to bearer-only means that client will only verify bearer tokens and can't obtain the tokens itself. This means the service is not going to attempt to redirect users to a login page on Keycloak. This is perfect as we're deploying a service intended to be invoked by an application and not by users directly.
Now click on the Installation tab on the top of the form. Under format option select Keycloak JSON. Click on the Download tab. You should move the downloaded file (keycloak.json to src/main/webapp/WEB-INF. This provides the required configuration for the Keycloak client adapter.

There's also an alternative method when deploying to WildFly (or JBoss EAP) which is to specify the configuration found in keycloak.json inside standalone.xml instead. We're not going to cover this approach in this post, but it can be more convenient to use this option if you don't want to open up your WAR (or change the source like we did now).

Step 3: Now change the auth-method in web.xml file from 

		BASIC
		To:
		KEYCLOAK
	

Step 4: Keycloak Client Adapter configuration

Download the WildFly client adapter keycloak-wf9-adapter.tar.gz (or keycloak-wf9-adapter.zip). Extract the archive into the root of your WildFly installation and run:

bin/jboss-cli.sh -c ':shutdown(restart=true)'
Wait until WildFly has restarted then run:
bin/jboss-cli.sh -c --file=bin/adapter-install.cli
Finally run:
bin/jboss-cli.sh -c ':shutdown(restart=true)'
		
		

Step 7: KEYCLOAK token generation

Token generation for testuser1 which has admin privileges
RESULT=`curl --data "grant_type=password&client_id=curl&username=testuser1&password=password1" http://localhost:8180/auth/realms/master/protocol/openid-connect/token`
echo $RESULT
TOKEN=`echo $RESULT | sed 's/.*access_token":"//g' | sed 's/".*//g'`
echo $TOKEN
curl http://localhost:8080/rest/library/books/123abc -H "Authorization: bearer $TOKEN"

Token generation for testuser2 which has user privileges
RESULT=`curl --data "grant_type=password&client_id=curl&username=testuser2&password=password2" http://localhost:8180/auth/realms/master/protocol/openid-connect/token`
echo $RESULT
TOKEN=`echo $RESULT | sed 's/.*access_token":"//g' | sed 's/".*//g'`
echo $TOKEN

Step 8: Test via client (postman)

Ex: to test addBook generate the token for testuser1 and add the following in the Body
{
  "isbn": "123abc",
  "title": "test",
  "author": "asdasd",
  "publishedDate": null
}

set the HTTP method to PUT
add the Authorization header with the bearer token