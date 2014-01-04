package com.plaso.xmpp.groupchat;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * @author chenqing
 * @datetime 2013-6-2 下午11:16:25
 * @desc 群上线 handler
 */
public class GroupOnlineHandler extends XmppMsgHandler {	
	/**
	 * 默认构造器
	 */
	protected GroupOnlineHandler() {
		super(MsgHandlerFactory.GROUP_ONLINE);
	}



	@Override
	public void processMsg(JSONObject jsonObj) throws JSONException {
		
	}
}
