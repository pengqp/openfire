package com.plaso.xmpp.groupchat;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author chenqing
 * @datetime 2013-6-2 下午11:17:34
 * @desc 群下线handler
 */
public class GroupOfflineHandler extends XmppMsgHandler {
	/**
	 * 默认构造器
	 */
	public GroupOfflineHandler(){
		super(MsgHandlerFactory.GROUP_OFFLINE);
	}

	@Override
	public void processMsg(JSONObject jsonObj) throws JSONException {
	}

}
