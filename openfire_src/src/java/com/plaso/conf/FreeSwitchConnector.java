package com.plaso.conf;

import java.util.List;

import org.freeswitch.esl.client.inbound.Client;
import org.freeswitch.esl.client.inbound.InboundConnectionFailure;
import org.freeswitch.esl.client.transport.message.EslMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FreeSwitchConnector {

	private Client c = new Client();
	private final Logger log = LoggerFactory.getLogger( this.getClass() );
	
	public FreeSwitchConnector(){
		
	}
	
	public void init(){
		try {
			c.connect("xmpp.plaso.cn", 8021, "ClueCon", 10);
			log.info("freeSwitch连接成功");
		} catch (InboundConnectionFailure e) {
			log.error("freeSwitch连接失败",e);
			e.printStackTrace();
		}
	}
	
	/**
	 * 获取会议室
	 * @return
	 */
	public String getConferences(){
		EslMessage el = c.sendSyncApiCommand("conference xml_list", "");
		List<String> res = el.getBodyLines();
		StringBuffer xmlStrBuf = new StringBuffer();
		for(String line : res)
		{
			xmlStrBuf.append(line);
		}
		return xmlStrBuf.toString();
	}
	
	/**
	 * 发送消息
	 * @param command
	 * @return
	 */
	public String sendMessage(String command){
		EslMessage el = c.sendSyncApiCommand(command, "");
		List<String> res = el.getBodyLines();
		StringBuffer xmlStrBuf = new StringBuffer();
		for(String line : res)
		{
			xmlStrBuf.append(line);
		}
		return xmlStrBuf.toString();
	}
}
