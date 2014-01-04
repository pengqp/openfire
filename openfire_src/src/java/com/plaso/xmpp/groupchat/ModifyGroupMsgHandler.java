package com.plaso.xmpp.groupchat;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author chenqing
 * @datetime 2013-6-2 下午11:27:43
 * @desc 修改群消息handler
 */
public class ModifyGroupMsgHandler extends GroupBroadMsgHandler {
	/**
	 * 默认构造器
	 */
	public ModifyGroupMsgHandler() {
		super(MsgHandlerFactory.GROUP_MODIFY);
	}

	/*
	 * 构建改名消息
	 */
	@Override
	public String buildMsg(JSONObject jsonObj) throws JSONException {
		StringBuilder sb = new StringBuilder("");
		JSONObject modifier = jsonObj.getJSONObject("modifier");
		sb.append("<updatebyJid>").append(modifier.getString("jid")).append(
				"</updatebyJid>").append("<groupname>").append(
				jsonObj.getString("newName")).append("</groupname>")
				.append("<timestamp>").append(
						jsonObj.getString("timestamp")).append(
						"</timestamp>").append("<updatebyName>").append(
						modifier.getString("name")).append("</updatebyName>");
		return sb.toString();
	}

}
