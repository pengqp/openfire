package com.plaso.xmpp.groupchat;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author chenqing
 * @datetime 2013-6-2 下午11:25:47
 * @desc 退出群消息handler
 */
public class QuitGroupMsgHandler extends GroupBroadMsgHandler {
	/**
	 * 默认构造器
	 */
	public QuitGroupMsgHandler() {
		super(MsgHandlerFactory.GROUP_QUIT);
	}

	/*
	 * 构建退出消息
	 */
	@Override
	public String buildMsg(JSONObject jsonObj) throws JSONException {
		StringBuilder sb = new StringBuilder("");
		JSONObject quitor = jsonObj.getJSONObject("quitor");
		sb.append("<quitJid>").append(quitor.getString("jid")).append(
				"</quitJid>").append("<timestamp>").append(
				jsonObj.getString("timestamp")).append("</timestamp>")
				.append("<quitName>").append(quitor.getString("name")).append(
						"</quitName>");
		return sb.append("").toString();
	}

}
