package com.plaso.conf;

import java.util.List;

public class ConfManagerImpl implements IConfManager {

	@Override
	public boolean muteSomeBody(String confId, String mid) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean unMuteSomeBody(String confId, String mid) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean muteConference(String confId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean unMuteConference(String confId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deafSomeBody(String mid) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean unDeafSomeBodyDeaf(String mid) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deafConference(String confId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean unDeafConference(String confId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<ConfMemberBean> getMemState(String confId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean closeConf(String confId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ConfMemberBean getConfMemberBean(String callNum) {
		// TODO Auto-generated method stub
		return null;
	}
	

}
