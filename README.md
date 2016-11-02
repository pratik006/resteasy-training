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
Step 2: Update the web.xml with security tags

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
Step 3: Add @RolesAllowed("user") annotation on all GET methods and @RolesAllowed("admin") for all PUT and 		DELETE methods
Step 4: Create a security-domain tag in Wildfly/standalone/standalone.xml file
		<security-domain name="training" cache-type="default">
            <authentication>
                <login-module code="org.jboss.security.auth.spi.DatabaseServerLoginModule" flag="required">
                    <module-option name="dsJndiName" value="java:jboss/datasources/ExampleDS"/>
                    <module-option name="principalsQuery" value="select password from Principals username where username=?"/>
                    <module-option name="rolesQuery" value="select Role, RoleGroup as Roles from UserRoles where username=?"/>
                    <module-option name="hashEncoding" value="base64"/>
                    <module-option name="hashUserPassword" value="false"/>
                </login-module>
            </authentication>
        </security-domain>
Step 5: Update the ExampleDS datasource's copnnection url with the one below
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
		
Step 7: 