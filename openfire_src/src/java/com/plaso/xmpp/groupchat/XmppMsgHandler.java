package com.plaso.xmpp.groupchat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author chenqing
 * @date 2013-7-25 下午07:40:04
 * @desc XMPP消息处理基类
 */
public abstract class XmppMsgHandler {
	public static final String ROOM_ID = "RoomId";
	
	/**
	 * 日志实例
	 */
	protected Logger log = LoggerFactory.getLogger(XmppMsgHandler.class);
	
	/**
	 * handler名称
	 */
	protected String handlerName;

	public String getHandlerName() {
		return handlerName;
	}

	public void setHandlerName(String handlerName) {
		this.handlerName = handlerName;
	}

	protected XmppMsgHandler(String handlerName){
		this.handlerName = handlerName;
	}
	
	public String getGroupId(JSONObject jsonObj) throws JSONException{
		return jsonObj.getString(ROOM_ID);
	}

	/**
	 * 消息处理，子类要实现此方法
	 */
	public abstract void processMsg(JSONObject jsonObj) throws JSONException; 
}
