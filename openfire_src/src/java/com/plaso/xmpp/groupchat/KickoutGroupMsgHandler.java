package com.plaso.xmpp.groupchat;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author chenqing
 * @datetime 2013-6-2 下午11:26:51
 * @desc 踢出群消息handler
 */
public class KickoutGroupMsgHandler extends GroupBroadMsgHandler {
	/**
	 * 默认构造器
	 */
	public KickoutGroupMsgHandler(){
		super(MsgHandlerFactory.GROUP_KICK);
	}
	/* 
	 * 构建踢人消息
	 */
	@Override
	public String buildMsg(JSONObject jsonObj) throws JSONException {
		StringBuilder sb = new StringBuilder("");
		JSONObject kickor = jsonObj.getJSONObject("kickor");
		JSONObject kickee = jsonObj.getJSONObject("kickee");
		sb.append("<kickedJid>").append(kickee.getString("jid")).append(
				"</kickedJid><kickedbyJid>").append(
						kickor.getString("jid")).append(
				"</kickedbyJid><timestamp>").append(jsonObj.getString("timestamp"))
				.append("</timestamp><kickedName>").append(
						kickee.getString("name")).append(
						"</kickedName><kickedbyName>").append(
								kickor.getString("name")).append(
						"</kickedbyName>");
		return sb.toString();
	}

}
