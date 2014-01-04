package com.plaso.conf;

import java.util.List;

public interface IConfManager {

	/**
	 * 使某人禁言
	 * @param confId
	 * @param mid
	 * @return
	 */
	public boolean muteSomeBody(String confId, String mid);
	
	/**
	 * 使某人可以发言
	 * @param confId
	 * @param mid
	 * @return
	 */
	public boolean unMuteSomeBody(String confId, String mid);
	
	/**
	 * 会议室禁言
	 * @param confId
	 * @return
	 */
	public boolean muteConference(String confId);
	
	/**
	 * 会议室可以发言
	 * @param confId
	 * @return
	 */
	public boolean unMuteConference(String confId);
	
	/**
	 * 禁听
	 * @param mid
	 * @return
	 */
	public boolean deafSomeBody(String mid);
	
	/**
	 * 某人不禁听
	 * @param mid
	 * @return
	 */
	public boolean unDeafSomeBodyDeaf(String mid);
	
	/**
	 * 会议室禁听
	 * @param confId
	 * @return
	 */
	public boolean deafConference(String confId);
	
	/**
	 * 会议室不禁听
	 * @param confId
	 * @return
	 */
	public boolean unDeafConference(String confId);
	
	/**
	 * 获取成员状态
	 * @param confId
	 * @return
	 */
	public List<ConfMemberBean> getMemState(String confId);
	
	/**
	 * 结束会议
	 * @param confId
	 * @return
	 */
	public boolean closeConf(String confId);
	
	/**
	 * 根据呼叫号码获取成员对象
	 * @param mid
	 * @return
	 */
	public ConfMemberBean getConfMemberBean(String callNum);
}