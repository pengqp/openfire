package com.plaso.conf;

public class ConfMemberBean {

	/**
	 * 成员ID
	 */
	private String mid;
	
	/**
	 * 成员呼叫号
	 */
	private String memberNum;
	
	/**
	 * 成员名称
	 */
	private String memberName;
	
	/**
	 * 是否禁听
	 */
	private boolean isDeaf;
	
	/**
	 * 是否禁言
	 */
	private boolean isMute;
	
	/**
	 * 是否发言
	 */
	private boolean talking;
	
	/**
	 * 是否主持人
	 */
	private boolean isModerator;
	
	/**
	 * 是否可以手写，默认禁止手写
	 */
	private boolean isDraw = false;

	public boolean isDeaf() {
		return isDeaf;
	}

	public void setDeaf(boolean isDeaf) {
		this.isDeaf = isDeaf;
	}

	public boolean isMute() {
		return isMute;
	}

	public void setMute(boolean isMute) {
		this.isMute = isMute;
	}

	public String getMid() {
		return mid;
	}

	public void setMid(String mid) {
		this.mid = mid;
	}

	public String getMemberNum() {
		return memberNum;
	}

	public void setMemberNum(String memberNum) {
		this.memberNum = memberNum;
	}

	public String getMemberName() {
		return memberName;
	}

	public void setMemberName(String memberName) {
		this.memberName = memberName;
	}

	public boolean isTalking() {
		return talking;
	}

	public void setTalking(boolean talking) {
		this.talking = talking;
	}

	public boolean isModerator() {
		return isModerator;
	}

	public void setModerator(boolean isModerator) {
		this.isModerator = isModerator;
	}

	public boolean isDraw() {
		return isDraw;
	}

	public void setDraw(boolean isDraw) {
		this.isDraw = isDraw;
	}
	
}
