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
 * @date 2013-7-19 下午01:56:39
 * @desc 所有群响应类
 */
public class AllGroupsResponse {
	/**
	 * 日志实例
	 */
	private static Logger log = LoggerFactory.getLogger(AllGroupsResponse.class);
	
	private String ResponseCode;
	private String ResponseMessage;
	private List<GroupInfo> giList = new ArrayList<GroupInfo>();

	public String getResponseCode() {
		return ResponseCode;
	}
	public void setResponseCode(String responseCode) {
		ResponseCode = responseCode;
	}
	public String getResponseMessage() {
		return ResponseMessage;
	}
	public void setResponseMessage(String responseMessage) {
		ResponseMessage = responseMessage;
	}
	public List<GroupInfo> getGiList() {
		return giList;
	}
	public void setGiList(List<GroupInfo> giList) {
		this.giList = giList;
	}
	
	/**
	 * 转换JSONObject为自身对象
	 * @param jo JSONObject
	 * @return 自身实例
	 */
	public static AllGroupsResponse toAllGroupsResponse(JSONObject jo){
		AllGroupsResponse res = new AllGroupsResponse();
		try {
			res.setResponseCode(jo.getString("ResponseCode"));
			res.setResponseMessage(jo.getString("ResponseMessage"));
			JSONArray groups = jo.getJSONArray("groups");
			for (int i = 0; i < groups.length(); i++){
				res.getGiList().add(
						GroupInfo.toGroupInfo(groups.getJSONObject(i)));
			}
		} catch (JSONException e) {
			log.error("json转换失败:" + jo.toString());
			e.printStackTrace();
			res = null;
		}
		return res;
	}
}
