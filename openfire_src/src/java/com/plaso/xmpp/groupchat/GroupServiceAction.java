package com.plaso.xmpp.groupchat;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jivesoftware.openfire.XMPPServer;
import org.xmpp.packet.Message;
import org.json.JSONObject;
import org.json.JSONException;

import com.plaso.thrift.gen.TGroupService;
import com.plaso.thrift.gen.TLiveClass;
import com.plaso.thrift.gen.TLiveClassService;
import com.plaso.thrift.gen.TPlasoException;
import com.plaso.thrift.gen.TStudent;
import com.plaso.thrift.gen.TTeacher;

/**
 * @author pengqp
 * @date 2013-7-19 上午10:13:43
 * @desc 群服务类，负责接口间的交互
 */
public class GroupServiceAction {
	/**
	 * 日志实例
	 */
	private static Logger log = LoggerFactory.getLogger(GroupServiceAction.class);
	
	private static GroupServiceAction gsAction = new GroupServiceAction();
	
	public static String MAGIC_TOKEN = "PLASO_MAGIC_TOKEN_MATMATMATMATHELLO";
	
	/**
	 * 实时课堂
	 */
	private static String liveREMOTE_API = "http://thrift.plaso.cn:8801/plaso/thrift/liveclass";
	
	/**
	 * 获取班级链接
	 */
	private static String groupREMOTE_API = "http://thrift.plaso.cn:8801/plaso/thrift/group";
	
	/**
	 * 单聊服务器域名
	 */
	public static String chatXmppDomain = "xmpp.plaso.cn";
	
	public static String teacherF = "tea";
	
	public static String studentF = "stu";

	
	private GroupServiceAction(){
	
	}
	
	/** 
	 * 获取MessageQueue配置实例，单例模式
	 */
	public static GroupServiceAction getInstance(){
		return gsAction;
	}
	
	/**
	 * 转发聊天消息到群成员
	 * 
	 * @param chatMsg
	 *            聊天消息
	 */
	public  void broadChatMsg(Message chatMsg) {
		log.info("New GroupChat message from memember!");
		log.info("chatMsg:" + chatMsg.toXML());
		String groupJid = chatMsg.getTo().toBareJID();
		log.info("groupJid="+groupJid);
		String groupId = groupJid.split("@")[0];
		
		//班级或实事课堂ID
		String realGroupId = groupId.split("_")[1];
		log.info("realGroupId="+realGroupId);
		//老师list
		List<TTeacher> v1 = null;
		//学生list
		List<TStudent> v2 = null;
		if(groupId.startsWith("live")){
			log.info("进入live处理。。。");
			TLiveClassService.Client lc = null;
			try {
				v1 = MemCacheService.getInstance().getTeacherListByGroupClassId(realGroupId+teacherF);
				log.info("开始从内存数据库查询老师数据");
				if(v1 == null || v1.size() == 0){
					log.info("从内存数据库没有查到老师数据，将调接口获取");
					lc = getLiveClient();
					if(lc != null){
//						TLiveClass classT = lc.getByMettingNumber(meetingNumber, access_token);
//						classT.getTeacherObj().getLoginName();
						v1 = lc.getTeacherByLiveClass(Integer.parseInt(realGroupId), MAGIC_TOKEN);
						if(v1 != null && v1.size() > 0){
							try {
								MemCacheService.getInstance().saveTTeacherList(realGroupId+teacherF, v1);
								log.info("调接口获取到老师数据，保存数据到内存数据库");
							} catch (IOException e) {
								log.error("读写内存数据库IO异常",e);
								e.printStackTrace();
							}
						}
					}else{
						log.error("连接thrift失败");
					}
				}
				v2 = MemCacheService.getInstance().getStudentListByGroupClassId(realGroupId+studentF);
				log.info("开始从内存数据库查询学生数据");
				if(v2 == null || v2.size() == 0){
					log.info("从内存数据库没有查到学生数据，将调接口获取");
					if(lc == null){
						lc = getLiveClient();
					}
					if(lc != null){
						v2 = lc.getStudentByLiveClass(
								Integer.parseInt(realGroupId), MAGIC_TOKEN);
						if(v2 != null && v2.size() > 0){
							try {
								MemCacheService.getInstance().saveTStudentList(
										realGroupId + studentF, v2);
								log.info("调接口获取到学生数据，保存数据到内存数据库");
							} catch (IOException e) {
								log.error("读写内存数据库IO异常",e);
								e.printStackTrace();
							}
						}
					}else{
						log.error("连接thrift失败");
					}
				}
			} catch (TTransportException e) {
				log.info("TTransportException", e);
				e.printStackTrace();
			} catch (NumberFormatException e) {
				log.info("组ID非法", e);
				e.printStackTrace();
			} catch (TException e) {
				log.info("其他错误", e);
				e.printStackTrace();
			}
			
		}else if(groupId.startsWith("class")) {
			log.info("进入class 处理。。。。");
			TGroupService.Client gc = null;
			try {
				gc = getGroupClient();
				if(gc != null){
					log.info("获取班级数据开始");
					v1 = MemCacheService.getInstance().getTeacherListByGroupClassId(realGroupId+teacherF);
					log.info("开始从内存数据库查询老师数据");
					if(v1 == null){
						log.info("从内存数据库没有查到老师数据，将调接口获取");
						gc = getGroupClient();
						if(gc != null){
							v1 = gc.getTeacherByGroup(Integer.parseInt(realGroupId), MAGIC_TOKEN);
							if(v1 != null){
								try {
									MemCacheService.getInstance().saveTTeacherList(realGroupId+teacherF, v1);
									log.info("调接口获取到老师数据，保存数据到内存数据库");
								} catch (IOException e) {
									log.error("读写内存数据库IO异常",e);
									e.printStackTrace();
								}
							}
						}else{
							log.error("连接thrift失败");
						}
					}
					v2 = MemCacheService.getInstance().getStudentListByGroupClassId(realGroupId+studentF);
					log.info("开始从内存数据库查询学生数据");
					if(v2 == null){
						log.info("从内存数据库没有查到学生数据，将调接口获取");
						if(gc == null){
							gc = getGroupClient();
						}
						if(gc != null){
							v2 = gc.getStudentByGroup(Integer.parseInt(realGroupId), MAGIC_TOKEN);
							if(v2 != null){
								try {
									MemCacheService.getInstance().saveTStudentList(
											realGroupId + studentF, v2);
									log.info("调接口获取到学生数据，保存数据到内存数据库");
								} catch (IOException e) {
									log.error("读写内存数据库IO异常",e);
									e.printStackTrace();
								}
							}
						}else{
							log.error("连接thrift失败");
						}
					}
				}
				
			} catch (TTransportException e) {
				log.info("TTransportException", e.getMessage());
				e.printStackTrace();
			} catch (NumberFormatException e) {
				log.info("组ID非法", e.getMessage());
				e.printStackTrace();
			} catch (TPlasoException e) {
				log.info("TPlasoException", e.getMessage());
				e.printStackTrace();
			} catch (TException e) {
				log.info("其他错误", e.getMessage());
				e.printStackTrace();
			}
			
		}
		
		if (null == v1 && null == v2){
			log.info("获取到群组数据为空，返回");
			return;
		}
			
		
		String fromGroupJid = groupId + "@" + XMPPServer.getInstance().getServerInfo().getXMPPDomain();
		log.info("fromgroupJID="+fromGroupJid);
		Message msg = chatMsg.createCopy();
		msg.setFrom(fromGroupJid);
		msg.setType(Message.Type.chat);
		msg.addChildElement("ext",
				"jabber:client").addElement("from").addText(chatMsg.getFrom().toFullJID());
		log.info("老师数据长度："+v1.size());
		//发送消息给teacher
        for(TTeacher teacher : v1)
        {
        	if (chatMsg.getFrom().toBareJID().startsWith(teacher.getLoginName())) // 会话发送排除自身
				continue;
        	Message beanmsg = msg.createCopy();
			beanmsg.setTo(teacher.getLoginName() + "@" + chatXmppDomain);
			XMPPServer.getInstance().getMessageRouter().route(beanmsg);				
			log.info("发送消息成功" + beanmsg.toXML());
        }
        log.info("学生数据长度："+v2.size());
        //发送消息给teacher
        for(TStudent student : v2)
        {
        	if (chatMsg.getFrom().toBareJID().startsWith(student.getLoginName())) // 会话发送排除自身
				continue;
        	Message beanmsg = msg.createCopy();				
			beanmsg.setTo(student.getLoginName() + "@" + chatXmppDomain);
			XMPPServer.getInstance().getMessageRouter().route(beanmsg);				
			log.info("发送消息成功" + beanmsg.toXML());
        }
		
		
		// 发送XMPP消息给群成员
//		for (MemberBean bean : group.getMbList()) {
//			if (bean.getJid().equals(chatMsg.getFrom().toBareJID())) // 会话发送排除自身
//				continue;
//			
//			Message beanmsg = msg.createCopy();
//			beanmsg.setTo(bean.getJid());
//			XMPPServer.getInstance().getMessageRouter().route(beanmsg);
//			log.info("发送消息成功" + beanmsg.toXML());
//		}
	
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
	 * 获取群房间
	 * 
	 * @param groupId
	 *            群ID
	 * @return 群房间，包括群成员
	 */
	private  RoomInfo getRoom(String groupId) {
		GroupMembersResponse members = getGroupMembersById(groupId);
		if (null == members || !members.getResponseCode().equals("200")) {
			log.error("获取群(" + groupId + ")的成员失败");
			return null;
		}
		return members.getGroup();
	}

	/**
	 * 获取指定群成员
	 * 
	 * @param groupId
	 *            群ID
	 * @return 群成员对象
	 */
	public  GroupMembersResponse getGroupMembersById(String groupId) {
		String url = GroupChatProperties.getInstance().getGroupWebUrl()+
				     GroupChatProperties.getInstance().getMethodGroupMembers();
		StringBuilder data = new StringBuilder("{\"RoomId\": \"").append(
				groupId).append("\",\"SystemId\": \"-1\",\"Token\": \"\"}");
		/*
		 * System.out.println("url=" + url); System.out.println("data=" + data);
		 */
		String str = GroupUtils.sendHttpData(url, data.toString());
		if (StringUtils.isBlank(str)) {
			GroupMembersResponse res = new GroupMembersResponse();
			res.setResponseCode("202");
			res.setResponseMessage("获取到的群成员为空");
			return res;
		}
		log.info("获取到的群(" + groupId + ")成员：" + str);
		try {
			GroupMembersResponse res = GroupMembersResponse
					.toGroupMembers(new JSONObject(str));
			return res;
		} catch (JSONException e) {
			log.error("群成员json转换失败:" + str);
			GroupMembersResponse res = new GroupMembersResponse();
			res.setResponseCode("202");
			res.setResponseMessage("json转换失败:" + str);
			return res;
		}
	}

}
