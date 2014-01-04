package com.plaso.xmpp.groupchat;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author chenqing
 * @date 2013-7-19 下午12:36:18
 * @desc 群房间类,包括群成员
 */
public class RoomInfo {
	/**
	 * 日志实例
	 */
	private static Logger log = LoggerFactory.getLogger(RoomInfo.class);
	
	private String RoomId;
	private String RoomName;
	private List<MemberBean> mbList = new ArrayList<MemberBean>();
	
	public List<MemberBean> getMbList() {
		return mbList;
	}
	public void setMbList(List<MemberBean> mbList) {
		this.mbList = mbList;
	}
	public String getRoomId() {
		return RoomId;
	}
	public void setRoomId(String roomId) {
		RoomId = roomId;
	}
	public String getRoomName() {
		return RoomName;
	}
	public void setRoomName(String roomName) {
		RoomName = roomName;
	}
	
	private String toMembers(){
		StringBuilder sb = new StringBuilder("[");
		for (MemberBean bean : mbList){
			sb.append(bean);
		}
		return sb.append("]").toString();
	}
	
	public String toString(){
		return new StringBuilder("RoomId=").append(RoomId)
			.append(", RoomName=").append(RoomName)
			.append(", MemberBean[]=").append(toMembers()).toString();
	}

	/**
	 * 转换JSONObject为自身对象
	 * @param jo JSONObject
	 * @return 自身实例
	 */
	public static RoomInfo toRoomInfo(JSONObject jo){
		RoomInfo room = new RoomInfo();
		try {
			room.setRoomId(jo.getString("RoomId"));
			JSONArray members = jo.getJSONArray("Member");
			for (int i = 0; i < members.length(); i++){
				room.getMbList().add(
						MemberBean.toMemberBean(members.getJSONObject(i)));
			}
		} catch (JSONException e) {
			log.error("json转换失败:" + jo.toString());
			room = null;
		}
		return room;
	}
}
