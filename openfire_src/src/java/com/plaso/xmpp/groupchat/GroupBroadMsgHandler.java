package com.plaso.xmpp.groupchat;

import java.util.List;

import org.dom4j.CDATA;
import org.jivesoftware.openfire.XMPPServer;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmpp.packet.Message;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;

import com.plaso.thrift.gen.TGroupService;
import com.plaso.thrift.gen.TLiveClassService;
import com.plaso.thrift.gen.TStudent;
import com.plaso.thrift.gen.TTeacher;

/**
 * @author chenqing
 * @datetime 2013-6-2 下午11:19:24
 * @desc 群广播消息基类
 */
public abstract class GroupBroadMsgHandler extends XmppMsgHandler {
	public static String MAGIC_TOKEN = "PLASO_MAGIC_TOKEN_MATMATMATMATHELLO";
	private static String liveREMOTE_API = "http://thrift.plaso.cn:8801/plaso/thrift/liveclass";
	static String groupREMOTE_API = "http://thrift.plaso.cn:8801/plaso/thrift/group";
	/**
	 * 单聊服务器域名
	 */
	public static String chatXmppDomain = "xmpp.plaso.cn";
	//protected String groupId;
	/**
	 * 继承基类构造器
	 */
	protected GroupBroadMsgHandler(String handlerName) {
		super(handlerName);
	}

	/* 
	 * 群广播消息处理
	 */
	@Override
	public void processMsg(JSONObject jsonObj) throws JSONException {
		
		sendXmppMsg(getHandlerName(), jsonObj);
	}

	/**
	 * 构建消息，子类实现
	 */
	public abstract String buildMsg(JSONObject jsonObj) throws JSONException;

	/**
	 * 获取群房间
	 * 
	 * @param groupId
	 *            群ID
	 * @return 群房间，包括群成员
	 */
	private RoomInfo getRoom(String groupId) {
		GroupMembersResponse members = GroupServiceAction.getInstance()
				.getGroupMembersById(groupId);
		if (null == members || !members.getResponseCode().equals("200")) {
			log.error("获取群(" + groupId + ")的成员失败");
			return null;
		}
		return members.getGroup();
	}

	TLiveClassService.Client getLiveClient(String servletUrl) throws TTransportException {
		THttpClient thc = new THttpClient(servletUrl);
		TProtocol loPFactory = new TBinaryProtocol(thc);
		TLiveClassService.Client client = new TLiveClassService.Client(loPFactory);
		return client;
	}

	TLiveClassService.Client getLiveClient() throws TTransportException {
		return getLiveClient(liveREMOTE_API);
	}
	
	

	TGroupService.Client getGroupClient(String servletUrl)
			throws TTransportException {
		THttpClient thc = new THttpClient(servletUrl);
		TProtocol loPFactory = new TBinaryProtocol(thc);
		TGroupService.Client client = new TGroupService.Client(loPFactory);
		return client;
	}

	TGroupService.Client getGroupClient() throws TTransportException {
		return getGroupClient(groupREMOTE_API);
	}

	
	/**
	 * 发送XMPP消息，子类可扩展此消息实现
	 * 
	 * @param extName
	 *            扩展消息名
	 * @param extValue
	 *            扩展消息体
	 * @param jo
	 *            json对象
	 * @throws JSONException
	 */
	protected void sendXmppMsg(String handlename, JSONObject jo)
			throws JSONException {
		// 获取群和成员，不存在则退出; 邀请人的群ID与其他不一样，需要在处理下
		String groupId = getHandlerName().equals(MsgHandlerFactory.GROUP_JOIN) ? jo
				.getJSONObject("roomInfo").getString(ROOM_ID)
				: jo.getString(ROOM_ID);
		try{
			List<TTeacher> v1 = null;
			List<TStudent> v2 = null;
			if(groupId.startsWith("live")){
				TLiveClassService.Client c = getLiveClient();
				v1 = c.getTeacherByLiveClass(Integer.parseInt(groupId.split("_")[1]), MAGIC_TOKEN);
				v2 = c.getStudentByLiveClass(Integer.parseInt(groupId.split("_")[1]), MAGIC_TOKEN);
//				RoomInfo group = getRoom(groupId);
			}else {
				TGroupService.Client c = getGroupClient();
				v1 = c.getTeacherByGroup(Integer.parseInt(groupId.split("_")[1]), MAGIC_TOKEN);
				v2 = c.getStudentByGroup(Integer.parseInt(groupId.split("_")[1]), MAGIC_TOKEN);
			}
			String groupJid = groupId + "@" + XMPPServer.getInstance().getServerInfo().getXMPPDomain();
			
			if (null == v1 && null == v2)
				return;
			
			Message msg = new Message();
			
			msg.setFrom(groupJid);
			msg.setType(Message.Type.chat);	
	        msg.addChildElement("ext","jabber:client").addElement(handlename).addCDATA(buildMsg(jo));

	        //发送消息给teacher
	        for(TTeacher teacher : v1)
	        {
	        	Message beanmsg = msg.createCopy();				
				beanmsg.setTo(teacher.getLoginName() + "@" + chatXmppDomain);
				XMPPServer.getInstance().getMessageRouter().route(beanmsg);				
				log.info("发送消息成功" + beanmsg.toXML());
	        }
	        
	        //发送消息给teacher
	        for(TStudent student : v2)
	        {
	        	Message beanmsg = msg.createCopy();				
				beanmsg.setTo(student.getLoginName() + "@" + chatXmppDomain);
				XMPPServer.getInstance().getMessageRouter().route(beanmsg);				
				log.info("发送消息成功" + beanmsg.toXML());
	        }
			// 发送XMPP消息给群成员
//			for (MemberBean bean : group.getMbList()) {
//				
//					Message beanmsg = msg.createCopy();				
//					beanmsg.setTo(bean.getJid());
//					XMPPServer.getInstance().getMessageRouter().route(beanmsg);				
//					log.info("发送消息成功" + beanmsg.toXML());
//				
//				}
		
			
			// 如果是踢出消息，则给kickee发消息，通知其被踢出了（因为成员信息里已经没有此人了）
			boolean bKicked = getHandlerName().equals(MsgHandlerFactory.GROUP_KICK);
			if (bKicked){
					String jid = jo.getJSONObject("kickee").getString("jid");
	                Message kickmsg =msg.createCopy();				
	                kickmsg.setTo(jid);
					XMPPServer.getInstance().getMessageRouter().route(kickmsg);
					log.info("发送消息成功" + msg.toXML());
				}
		}catch(Exception ex){
			log.info("处理异常");
		}
		
		}
	}

