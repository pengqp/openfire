package com.plaso.xmpp.groupchat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author chenqing
 * @date 2013-7-19 下午01:57:45
 * @desc 群信息类
 */
public class GroupInfo {
	/**
	 * 日志实例
	 */
	private static Logger log = LoggerFactory.getLogger(GroupInfo.class);
	
	private String groupId;
	private String groupName;
	private String groupOwner;

	public String getGroupId() {
		return groupId;
	}
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	public String getGroupOwner() {
		return groupOwner;
	}
	public void setGroupOwner(String groupOwner) {
		this.groupOwner = groupOwner;
	}
	
	public GroupInfo(){}
	
	public GroupInfo(String groupId){
		this.groupId = groupId;
	}
	
	/**
	 * 转换JSONObject为自身对象
	 * @param jo JSONObject
	 * @return 自身实例
	 */
	public static GroupInfo toGroupInfo(JSONObject jo){
		GroupInfo group = new GroupInfo();
		try {
			group.setGroupId(jo.getString("Id"));
			group.setGroupName(jo.getString("name"));
			group.setGroupOwner(jo.getString("owner"));
		} catch (JSONException e) {
			log.error("json转换失败:" + jo.toString());
			e.printStackTrace();
			group = null;
		}
		return group;
	}
}
