package com.plaso.conf;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConferenceBean {

	/**
	 * 会议ID
	 */
	private String confId = null;
	
	/**
	 * 会议成员数
	 */
	private String memberCount = "";
	
	/**
	 * 会议进行时间
	 */
	private String runTime = "";
	
	/**
	 * 会议结束时间
	 */
	private Date finishTime = null;
	
	/**
	 * 会议主持人
	 */
	private String moderator = null;
	
	/**
	 * 会议成员列表
	 */
	private List<ConfMemberBean> memberList = null;

	public ConferenceBean(){
		memberList = new ArrayList<ConfMemberBean>();
	}
	
	public String getConfId() {
		return confId;
	}

	public void setConfId(String confId) {
		this.confId = confId;
	}

	public String getMemberCount() {
		return memberCount;
	}

	public String getRunTime() {
		return runTime;
	}

	public void setRunTime(String runTime) {
		this.runTime = runTime;
	}

	public void setMemberCount(String memberCount) {
		this.memberCount = memberCount;
	}
	public List<ConfMemberBean> getMemberList() {
		return memberList;
	}

	public void setMemberList(List<ConfMemberBean> memberList) {
		this.memberList = memberList;
	}
	
	public void addConferenceMem(ConfMemberBean memberBean){
		if(this.memberList != null){
			this.memberList.add(memberBean);
		}
	}

	public Date getFinishTime() {
		return finishTime;
	}

	public void setFinishTime(Date finishTime) {
		this.finishTime = finishTime;
	}

	public String getModerator() {
		return moderator;
	}

	public void setModerator(String moderator) {
		this.moderator = moderator;
	}
	
}
