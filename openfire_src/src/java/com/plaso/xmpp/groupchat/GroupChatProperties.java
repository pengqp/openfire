package com.plaso.xmpp.groupchat;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class GroupChatProperties {
	private static final String GROUP_WEB_URL = "group_web_url";
	private static final String METHOD_GROUP_MEMBERS = "method_group_members";
	private static final String SERVER_SOCKET_PORT = "socket_port";
	private static final String REMOTE_XMPP_PORT = "remote_xmpp_port";
	
	private static Logger log = LoggerFactory.getLogger(GroupChatProperties.class);
	
    private static GroupChatProperties gcp = new GroupChatProperties();
	
	private Properties props;
	
	private GroupChatProperties()  {
		props = new Properties();
		
		File file = new File(JiveGlobals.getHomeDirectory() + File.separator + "conf" + File.separator + "groupchat.properties");
		if (!file.exists()) {
			log.error("The file groupchat.properties is missing from Openfire conf folder");
		} else {
			try {
				props.load(new FileInputStream(file));
			} catch (IOException ioe) {
				log.error("Unable to load groupchat.properties file");
			}
		}
		
		// checking for required info in file
		if (StringUtils.isBlank(props.getProperty(GROUP_WEB_URL))
				|| StringUtils.isBlank(props.getProperty(METHOD_GROUP_MEMBERS))
				|| StringUtils.isBlank(props.getProperty(SERVER_SOCKET_PORT))) {
			
			log.error("groupchat.properties is missing required information (group web url, method for group members, server socket port)");
		}
	}
	
	public static GroupChatProperties getInstance(){
		return gcp;
	}
	
	public String getGroupWebUrl() {
		return noNull(props.getProperty(GROUP_WEB_URL));
	}
	
	public String getMethodGroupMembers() {
		return noNull(props.getProperty(METHOD_GROUP_MEMBERS));
	}
	
	
	public int getSocketPort() {
		return getIntegerValue(SERVER_SOCKET_PORT, 12321);
	}
	
	public int getRemoteXmppPort() {
		return getIntegerValue(REMOTE_XMPP_PORT, 5269);
	}
	
	public int getIntProperty(String propKey, int defaultValue) {
		return getIntegerValue(propKey, defaultValue);
	}
	
	private int getIntegerValue(String propKey, int defaultValue) {
		int i = 0;
		try {
			i = Integer.parseInt(props.getProperty(propKey));
		} catch (NumberFormatException nfe) {
			i = defaultValue;
		}
		return i;
	}
	
	private String noNull(String str) {
		return (str != null) ? str : "";
	}
}