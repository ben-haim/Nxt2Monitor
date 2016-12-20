Nxt2Monitor
===========

Nxt2Monitor uses the NRS HTTP/HTTPS API to display the current node status.  The status includes the current block height, connected peers and a list of recent blocks.  Right-click on a table row to display a context menu.  Left-click on a column header to sort the table based on that column.

The NRS node must accept API connections.  This is done by specifying nxt.apiServerPort, nxt.apiServerHost and nxt.allowedBotHosts in nxt.properties.  A secret phrase is not required since NxtMonitor uses API requests that are not associated with a Nxt account.

The server administrator password is required for some of the API requests.  As a result, Nxt2Monitor will default to using SSL for the server connection.


Build
=====

I use the Netbeans IDE but any build environment with Maven and the Java compiler available should work.  The documentation is generated from the source code using javadoc.

Here are the steps for a manual build.  You will need to install Maven 3 and Java SE Development Kit 8 if you don't already have them.

  - Create the executable: mvn clean package    
  - [Optional] Create the documentation: mvn javadoc:javadoc    
  - [Optional] Copy target/Nxt2Monitor-v.r.m.jar and lib/* to wherever you want to store the executables.    
  - Create a shortcut to start Nxt2Monitor using java.exe for a command window or javaw.exe for GUI only.    


Runtime Options
===============

The following command-line options can be specified using -Dname=value

  - nxt.datadir=directory-path		
    Specifies the application data directory. Application data will be stored in a system-specific directory if this option is omitted:		
	    - Linux: user-home/.Nxt2Monitor	    
		- Mac: user-home/Library/Application Support/Nxt2Monitor	    
		- Windows: user-home\AppData\Roaming\Nxt2Monitor	    
	
  - java.util.logging.config.file=file-path		
    Specifies the logger configuration file. The logger properties will be read from 'logging.properties' in the application data directory. If this file is not found, the 'java.util.logging.config.file' system property will be used to locate the logger configuration file. If this property is not defined, the logger properties will be obtained from jre/lib/logging.properties.
	
    JDK FINE corresponds to the SLF4J DEBUG level	
	JDK INFO corresponds to the SLF4J INFO level	
	JDK WARNING corresponds to the SLF4J WARN level		
	JDK SEVERE corresponds to the SLF4J ERROR level		

The following configuration options can be specified in Nxt2Monitor.conf.  This file is optional and must be in the application directory in order to be used.	

  - connect=host:port;adminPW    
    Specifies an NRS server connection.  This parameter can be specified multiple times to connect to more than one NRS server.  The default administrator password will be used if 'adminPW' is omitted.  The default API port will be used if 'port' is omitted.  The local host will be used if no 'connect' parameters are specified.
	
  - apiPort=port		
	Specifies the default NRS API port.  Port 7876 will be used if this parameter is not specified.
    
  - adminPW=password        
    Specifies the default administrator password for the NRS server.
    
  - useSSL=boolean          
    Specify 'true' to use HTTPS or 'false' to use HTTP to connect to the NRS node.  The default is 'true'.
    
  - allowNameMismatch=boolean       
    Specify 'true' to allow an HTTPS connection or 'false' to reject an HTTPS connection if the host name does not match the SSL certificate name.  The default is 'false'.
    
  - acceptAnyCertificate=boolean       
    Specify 'true' to accept the server certificate without verifying the trust path or 'false' to verify the certificate trust path before accepting the connection.  The default is 'false'.

