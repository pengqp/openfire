package com.plaso.xmpp.groupchat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author chenqing
 * @datetime 2013-6-2 下午11:25:07
 * @desc 加入群消息handler
 */
public class JoinGroupMsgHandler extends GroupBroadMsgHandler {
	/**
	 * 默认构造器
	 */
	public JoinGroupMsgHandler() {
		super(MsgHandlerFactory.GROUP_JOIN);
	}

	/*
	 * 构建邀请加入消息
	 */
	@Override
	public String buildMsg(JSONObject jsonObj) throws JSONException {
		StringBuilder sb = new StringBuilder("");
		JSONObject room = jsonObj.getJSONObject("roomInfo");
		JSONObject invitor = jsonObj.getJSONObject("invitor");
		JSONArray invitees = jsonObj.getJSONArray("invitee");
		sb.append("<invitebyJid>").append(invitor.getString("jid")).append(
				"</invitebyJid><invitebyName>").append(
				invitor.getString("name")).append("</invitebyName><groupname>")
				.append(room.getString("roomName")).append(
						"</groupname><timestamp>").append(
						jsonObj.getString("timestamp")).append(
						"</timestamp>");
		for (int i = 0; i < invitees.length(); i++) {
			JSONObject invitee = invitees.getJSONObject(i);
			sb.append("<invitee><invitedJid>").append(invitee.getString("jid"))
					.append("</invitedJid><invitedName>").append(
							invitee.getString("name")).append(
							"</invitedName></invitee>");
		}
		return sb.toString();
	}

}
