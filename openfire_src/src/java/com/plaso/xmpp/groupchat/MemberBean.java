package com.plaso.xmpp.groupchat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author chenqing
 * @date 2013-7-19 下午12:38:51
 * @desc 群房间类
 */
public class MemberBean {
	/**
	 * 日志实例
	 */
	private static Logger log = LoggerFactory.getLogger(MemberBean.class);
	
	private String name;
	private String id;
	private String jid;
	private String role;

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getJid() {
		return jid;
	}
	public void setJid(String jid) {
		this.jid = jid;
	}
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}

	public String toString(){
		return new StringBuilder("{name=").append(name)
			.append(", id=").append(id)
			.append(", jid=").append(jid)
			.append(", role=").append(role).append("}").toString();
	}
	
	/**
	 * 转换JSONObject为自身对象
	 * @param jo JSONObject
	 * @return 自身实例
	 */
	public static MemberBean toMemberBean(JSONObject jo){
		MemberBean member = new MemberBean();
		try {
			member.setId(jo.getString("id"));
			member.setName(jo.getString("name"));
			member.setJid(jo.getString("jid"));
			member.setRole(jo.getString("role"));
		} catch (JSONException e) {
			log.error("json转换失败:" + jo.toString());
			member = null;
		}
		return member;
	}
}
