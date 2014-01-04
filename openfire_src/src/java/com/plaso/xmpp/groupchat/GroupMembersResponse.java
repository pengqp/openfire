package com.plaso.xmpp.groupchat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author chenqing
 * @date 2013-7-19 下午12:35:11
 * @desc 群成员响应类
 */
public class GroupMembersResponse {
	/**
	 * 日志实例
	 */
	private static Logger log = LoggerFactory.getLogger(GroupMembersResponse.class);

	private String ResponseCode;

	private String ResponseMessage;

	private RoomInfo group;

	public String getResponseCode() {
		return ResponseCode;
	}

	public void setResponseCode(String responseCode) {
		this.ResponseCode = responseCode;
	}

	public String getResponseMessage() {
		return ResponseMessage;
	}

	public void setResponseMessage(String responseMessage) {
		this.ResponseMessage = responseMessage;
	}

	public RoomInfo getGroup() {
		return group;
	}

	public void setGroup(RoomInfo group) {
		this.group = group;
	}

	public String toString() {
		return new StringBuilder("ResponseCode=").append(ResponseCode).append(
				", ResponseMessage=").append(ResponseMessage).append(
				", RoomInfo=").append(group).toString();
	}

	/**
	 * 转换JSONObject为自身对象
	 * @param jo JSONObject
	 * @return 自身实例
	 */
	public static GroupMembersResponse toGroupMembers(JSONObject jo) {
		GroupMembersResponse res = new GroupMembersResponse();
		try {
			res.setResponseCode(jo.getString("ResponseCode"));
			res.setResponseMessage(jo.getString("ResponseMessage"));
			jo = jo.getJSONObject("RoomInfo");
			res.setGroup(RoomInfo.toRoomInfo(jo));
		} catch (JSONException e) {
			log.error("json转换失败:" + jo.toString());
			res = null;
		}
		return res;
	}
}
