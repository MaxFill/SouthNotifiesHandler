package ru.rt.fsom.conf;

import com.comptel.mds.sas5.taskengine.rmi_fifo.RMIFifoClient;
import com.comptel.mds.sas5.taskengine.rmi_fifo.exceptions.ClientAlreadyRegisteredException;
import com.comptel.mds.sas5.taskengine.rmi_fifo.exceptions.ClientNotAuthorisedException;
import com.comptel.mds.sas5.taskengine.rmi_fifo.exceptions.InvalidModeException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import ru.rt.fsom.dict.Params;
import ru.rt.fsom.utils.RMICallbackHandler;

@Singleton
@Startup
public class Conf {    
    private static final String DATA_SOURCE_NAME = "java:jboss/datasources/PostgresDS";
    private static final String DEFAULT_FIFO_USER = "hermes_tst";
    private static final String DEFAULT_FIFO_PASSWORD = "hermes_tst";            
    private static final String DEFAULT_WORKFLOW_HOSTNAME = "localhost";
    private static final String DEFAULT_WORKFLOW_PORT = "10000";
    private final static String UNREGISTR_EXCEPTION = "Client unregister exception";    
    private final static String RMIFIFO_ERR = "RMI_FIFO Error:";
    private final static String REMOTE_EXCEPTION = RMIFIFO_ERR + "RemoteException: there is a communication problem. Most probably something wrong with the physical connection OR TE has been shutdown";
    private final static String ILLEGAL_ARGUMENT_EXCEPTION = RMIFIFO_ERR + "IllegalArgumentException: the 'obj' response contains some invalid data...";
    private final static String CLIENT_ALREADY_REGISTERED_EXCEPTION = RMIFIFO_ERR + "ClientAlreadyRegisteredException: someone already uses this id... let's exit";
    private final static String INVALID_MODE_EXCEPTION = RMIFIFO_ERR + "InvalidModeException: TE is going down... do something with the response at";
    private final static String CLIENT_NOTAUTHORISED_EXCEPTION = RMIFIFO_ERR + "ClientNotAuthorisedException: can't happen... ignore"; 
    
    private RMIFifoClient client;    
    private ResourceBundle properties;
    private DataSource ds;        
    private String serviceGuid;
    private String versionInfo;
    
    @PostConstruct
    private void init() {	
	initConnectionPool();
	initConfFile();
	initRmiFifoClient();
	initServiceGuid();
	loadVersionInfo();
    }
    
    public Connection getJdbcConnection() throws SQLException{
	Connection connection = null;
	if (ds != null){ 
	    connection = ds.getConnection();
	} else {
	    throw new SQLException("DataSource is not initialized!");
	}
	return connection;
    }

    public String getPort() {
	return getStringPropertyValueByKey("port", DEFAULT_WORKFLOW_PORT);
    }
        
    public String getFIFOlogin(){
        return getStringPropertyValueByKey("hermes_fifo_login", DEFAULT_FIFO_USER);
    }
    
    public String getFIFOPassword(){
        return getStringPropertyValueByKey("hermes_fifo_pwl", DEFAULT_FIFO_PASSWORD);
    }         
    
    public String getHostName(){
        return getStringPropertyValueByKey("host", DEFAULT_WORKFLOW_HOSTNAME);
    }
    
    public RMIFifoClient getRMIFifoClient(){
	if (client == null){
	    initRmiFifoClient();
	}
	if (client != null && !client.isRegistered()){
	    rmiFifoClientRegistr();
	}
	return client;
    }
    
    public void resetRMIFifoClient(){
	rmiFifoClientUnregistr();
    }
    
    public void rmiFifoClientUnregistr(){
	//Params.LOGGER.log(Level.INFO, "start close RMI FIFO client...");
	if (client == null) return;
	try {
	    if (client.isRegistered()){
		client.unregister();
	    }
	    client = null;
	    //Params.LOGGER.log(Level.INFO, "RMI FIFO client unregistered successfuly!");
	} catch(RemoteException ex){
	    Params.LOGGER.log(Level.SEVERE, UNREGISTR_EXCEPTION, ex);
	}
    }    
    
    public String getServiceGuid(){
	return serviceGuid;
    }    
    
    public String getVersionInfo() {
	return versionInfo;
    }
    
    private void loadVersionInfo() {	
	Enumeration resEnum;
	try {
	    resEnum = Thread.currentThread().getContextClassLoader().getResources(JarFile.MANIFEST_NAME);
	    while (resEnum.hasMoreElements()) {
		try {
		    URL url = (URL)resEnum.nextElement();
		    InputStream is = url.openStream();
		    if (is != null) {
			Manifest manifest = new Manifest(is);
			Attributes attrs = manifest.getMainAttributes();
			if (attrs.getValue("build_name") != null){
			    StringBuilder sb = new StringBuilder();
			    sb.
				append("{").
				append("name:'").append(attrs.getValue("build_name")).append("', ").
				append("version:'").append(attrs.getValue("build_version")).append("', ").
				append("date:'").append(attrs.getValue("build_date")).append("', ").
				append("specification:'").append(attrs.getValue("build_specif")).append("'").
				append("}");
			    Params.LOGGER.log(Level.INFO, "Version info loaded: {0}", sb.toString());
			    versionInfo = sb.toString();
			}
		    }		    
		}
		catch (IOException ex) {
		    Params.LOGGER.log(Level.SEVERE, null, ex);
		}
	    }
	} catch (IOException ex){
	    Params.LOGGER.log(Level.SEVERE, null, ex);
	}
    }
  
    /* *** *** */
    
    private void initServiceGuid(){
	try {
	    InetAddress inetAddress = InetAddress.getLocalHost();	    
	    String ipv4 = inetAddress.getHostAddress();
	    Params.LOGGER.log(Level.INFO, "IP Address: {0}", ipv4);
	    serviceGuid = UUID.nameUUIDFromBytes(ipv4.getBytes()).toString();
	    Params.LOGGER.log(Level.INFO, "Service GUID={0}", serviceGuid);
	} catch (UnknownHostException ex) {
	    Params.LOGGER.log(Level.SEVERE, null, ex);
	}
    }
	
    private void initRmiFifoClient() {
	String hostname = getHostName();
	int port = Integer.valueOf(getPort());
	String login = getFIFOlogin();
	String pwl = getFIFOPassword();
	Params.LOGGER.log(Level.INFO, "------------------------------------------------");
	Params.LOGGER.log(Level.INFO, "");
	Params.LOGGER.log(Level.INFO, "");
	Params.LOGGER.log(Level.INFO, "Сreate RMI FIFO client for: {0} {1} host: {2}:{3}", new Object[]{login, pwl, hostname, port});
	client = new RMIFifoClient(login, login, pwl, hostname, port, new RMICallbackHandler());
	rmiFifoClientRegistr();
    }

    private void rmiFifoClientRegistr(){
	try {
	    Params.LOGGER.log(Level.INFO, "RMI FIFO client start registration...");
	    client.register();
	    Params.LOGGER.log(Level.INFO, "RMI FIFO client registered successfully!");
        } catch(ClientAlreadyRegisteredException care){
	    Params.LOGGER.log(Level.SEVERE, CLIENT_ALREADY_REGISTERED_EXCEPTION, care);	    
        } catch(InvalidModeException ime){	    
	    Params.LOGGER.log(Level.SEVERE, INVALID_MODE_EXCEPTION, ime);	    
        } catch(IllegalArgumentException iae){
	    Params.LOGGER.log(Level.SEVERE, ILLEGAL_ARGUMENT_EXCEPTION, iae);	    
        } catch(RemoteException re){
	    Params.LOGGER.log(Level.SEVERE, REMOTE_EXCEPTION, re);	    
        } catch(ClientNotAuthorisedException cnae){	    
	    Params.LOGGER.log(Level.SEVERE, CLIENT_NOTAUTHORISED_EXCEPTION, cnae);	    
	}
    }    
    
    private void initConnectionPool(){
	try {
	    InitialContext initContext = new InitialContext();
	    ds = (DataSource) initContext.lookup(DATA_SOURCE_NAME);
	} catch (NamingException ex) {
	    Params.LOGGER.log(Level.SEVERE, "error init connection pool! ", ex);
	}
    }
    
    private void initConfFile(){
        try {
            File props_path = new File(System.getProperty("jboss.server.config.dir"));
            URL[] urls = {props_path.toURI().toURL()};
            ClassLoader loader = new URLClassLoader(urls);
            properties = ResourceBundle.getBundle("FSOM", Locale.ROOT, loader);
        } catch (MalformedURLException ex) {
            Params.LOGGER.log(Level.SEVERE, "error loading config file!", ex);
        }
    }
    
    private String getStringPropertyValueByKey(String key, String defaultValue){       
        String result = defaultValue;
        if (null != properties){
            try { 
               result = properties.getString(key);
            }
            catch (MissingResourceException | ClassCastException ignoreIt){}//ignore this and return default value
        }
        return result;
    }  
}
