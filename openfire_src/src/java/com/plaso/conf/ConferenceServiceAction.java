package com.plaso.conf;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jivesoftware.openfire.XMPPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.Message;

import com.plaso.thrift.gen.TLiveClass;
import com.plaso.thrift.gen.TLiveClassService;
import com.plaso.thrift.gen.TPlasoException;
import com.plaso.xmpp.groupchat.GroupServiceAction;

/**
 * 会议控制管理类
 * @author pengqp
 *
 */
public class ConferenceServiceAction {

	private Logger log = LoggerFactory.getLogger(ConferenceServiceAction.class);
	
	/**
	 * fs连接对象
	 */
	private FreeSwitchConnector fsc = null;
	
	/**
	 * 会议监控定时器
	 */
	private Timer confMonitor = null;
	
	/**
	 * 会议对象存储容器
	 */
	private Hashtable<String,ConferenceBean> confList = null;
	
	/**
	 * 会议录音文件名存储容器
	 */
	private Hashtable<String,String> confRecordFile = null;
	
	/**
	 * 单实例对象
	 */
	private static ConferenceServiceAction ins_ = null;
	
	/**
	 * 获取会议信息命令
	 */
	private static String command_conf_list = "conference xml_list";
	
	/**
	 * 处理会控线程池
	 */
	private ThreadPoolExecutor threadPool;
	
	/**
	 * 实时课堂
	 */
	private static String liveREMOTE_API = "http://thrift.plaso.cn:8801/plaso/thrift/liveclass";
	
	/**
	 * 调用thrift接口令牌
	 */
	private static String MAGIC_TOKEN = "PLASO_MAGIC_TOKEN_MATMATMATMATHELLO";
	
	/**
	 * 录音路径
	 */
	private static String RecordPath = "/plaso/upload/conf_record/";
	
	/**
	 * 录音文件夹存储目录
	 */
	private static String RecordDir = "upload";
	
	/**
	 * lame压缩码率
	 */
	private static String Ratio = "16";
	
	/**
	 * 会议状态锁
	 */
	private static byte[] syncLock = new byte[0]; 
	
	private ConferenceServiceAction(){
		
	}
	
	public static ConferenceServiceAction getInstance(){
		if(ins_ == null){
			ins_ = new ConferenceServiceAction();
		}
		return ins_;
	}
	
	/**
	 * 初始化
	 */
	public void init(){
		fsc = new FreeSwitchConnector();
		fsc.init();
		confList = new Hashtable();
		confRecordFile = new Hashtable();
		confMonitor = new Timer();
		confMonitor.schedule(new confMonitorTask(), 1000, 5000);
		threadPool = new ThreadPoolExecutor(2, 10, 60,
				TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(10),
				new ThreadPoolExecutor.CallerRunsPolicy());
	}
	
	/**
	 * 接收客户端发过来的会控命令
	 * @param chatMsg
	 */
	public void receiveConfCommand(Message chatMsg){
		log.info("会控服务接收到消息:" + chatMsg.toXML());
		String xmlBody = chatMsg.toXML();
		try {
			log.info("消息体报文："+xmlBody);
			Document doc = DocumentHelper.parseText(xmlBody);
			Element elt = doc.getRootElement();
			Element eltBody = elt.element("body");
			Element operationE = eltBody.element("operationList");
			String confId = operationE.attributeValue("confId");
			Iterator iterAction = operationE.elementIterator("action");
			while(iterAction.hasNext())
			{
				Element actionE = (Element) iterAction.next();
				String mid = actionE.attributeValue("mid");
				String operation = actionE.attributeValue("operation");
				handleConfOperaion(confId,mid,operation);
			}
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 会控处理
	 * @param confId
	 * @param mid
	 * @param operation
	 */
	private void handleConfOperaion(String confId, String mid, String operation){
		if("mute".equals(operation)){
			String command = "conference "+confId+"-\\'sip.plaso.com\\'"+" mute "+mid;
			String res = fsc.sendMessage(command);
			log.info("静音处理结果："+res);
		}else if("unmute".equals(operation)){
			String command = "conference "+confId+"-\\'sip.plaso.com\\'"+" unmute "+mid;
			String res = fsc.sendMessage(command);
			log.info("允许发言处理结果："+res);
		}else if("draw".equals(operation)){
			setDrawState(mid,confId,true);
			sendConfState(confId);//手写状态变化直接通知客户端
		}else if("undraw".equals(operation)){
			setDrawState(mid,confId,false);
			sendConfState(confId);//手写状态变化直接通知客户端
		}else if("startRecord".equals(operation)){
			startRecord(confId, mid);
		}else if("endRecord".equals(operation)){
			endRecord(confId);
		}
	}
	
	/**
	 * 获取会议室
	 */
	private void getConferences(){
		String xmlStr = fsc.sendMessage(command_conf_list);
		Document doc = null;
		try {//解释xml暂时用dom方式实现，量小的时候没问题，后期量大了可以采用STAX方式解释进行优化，速度比较快。
			doc = DocumentHelper.parseText(xmlStr);
			Element rootElt = doc.getRootElement();
			log.info("会议室信息"+doc.asXML());
			Iterator iterConf = rootElt.elementIterator("conference");
			ConfMemberBean membean = null;
			while (iterConf.hasNext())
			{
				try{
					Element confE = (Element) iterConf.next();
					final ConferenceBean conference = new ConferenceBean();
					String confId = confE.attributeValue("name");
					confId = confId.split("-")[0];
					String memCount = confE.attributeValue("member-count");
					String runTime = confE.attributeValue("run_time");
					conference.setConfId(confId);
					conference.setMemberCount(memCount);
					conference.setRunTime(runTime);
					
					Element memsE = confE.element("members");
					if(memsE != null){
						Iterator itermems = memsE.elementIterator("member");
						while(itermems.hasNext())
						{
							try{
								Element memE = (Element)itermems.next();
								String type = memE.attributeValue("type");
								if(type.equals("caller")){
									membean = new ConfMemberBean();
									String mid = memE.elementTextTrim("id");
									String callNum = memE.elementTextTrim("caller_id_number");
									String callName = memE.elementTextTrim("caller_id_name");
									membean.setMemberNum(callNum);
									membean.setMid(mid);
									membean.setMemberName(callName);
									Element memsFlagE = memE.element("flags");
									if(memsFlagE != null){
										String canSpeak = memsFlagE.elementTextTrim("can_speak");
										if("true".equals(canSpeak)){
											membean.setMute(false);
										}else{
											membean.setMute(true);
										}
									}else{
										membean.setMute(true);
									}
									conference.addConferenceMem(membean);
								}
							}catch(Exception ex){
								ex.printStackTrace();
							}
						}
						threadPool.execute(new Runnable(){
							@Override
							public void run() {
								String confId = conference.getConfId();
								//会议到时挂断
								if(isConfTimeOut(confId)){
									log.info("会议:"+confId+"到时，挂断。。。。。");
									endRecord(confId);//结束录音
									hangupConf(confId);
									sendConfEndState(confId);//发送结束会议消息
									return;
								}
								if(checkConferenceState(conference)){//检查会议状态是否发生变化
									setConferenceInfo(conference);//发生变化之后，将fs的最新状态设置到内存
									sendConfState(confId);//发生变化之后，将最新信息推送消息到客户端
								}
							}
						});
					}
				}catch(Exception e){
					e.printStackTrace();
				}
				
			}
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 检查会议状态是否发生变化
	 * @param confBean
	 */
	private boolean checkConferenceState(ConferenceBean confBean){
		String confId = confBean.getConfId();
		ConferenceBean tmpBean = this.confList.get(confId);
		if(tmpBean == null){
			return true;
		} else {
			List<ConfMemberBean> newMemberList = confBean.getMemberList();
			List<ConfMemberBean> oldMemberList = tmpBean.getMemberList();
			if(newMemberList.size() != oldMemberList.size()){
				return true;
			}else{
				for(int i = 0; i < newMemberList.size(); i++)
				{
					boolean flag = false;
					ConfMemberBean newItem = newMemberList.get(i);
					String newMid = newItem.getMid();
					boolean newisMute = newItem.isMute();
					for(int j = 0; j< oldMemberList.size(); j++)
					{
						ConfMemberBean oldItem = oldMemberList.get(j);
						String oldMid = oldItem.getMid();
						boolean oldisMute = oldItem.isMute();
						if (newMid.equals(oldMid)) {
							if(newisMute == oldisMute){
								break;
							}else{
								flag = true;
								break;
							}
						}else{
							if(j == (oldMemberList.size() -1)){
								flag = true;
							}else{
								continue;
							}
						}
					}
					if(flag){
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * 设置会议室基本信息
	 * @param confBean
	 */
	public void setConferenceInfo(ConferenceBean confBean){
		
			String confId = confBean.getConfId();
			ConferenceBean memoryConfInfo = this.confList.get(confId);
			
			if(memoryConfInfo == null || memoryConfInfo.getFinishTime() == null){
				TLiveClassService.Client lc = null;
				try {
					lc = getLiveClient();
					TLiveClass classT = lc.getByMettingNumber(confBean.getConfId(), MAGIC_TOKEN);
					String teaLoginName = classT.getTeacherObj().getLoginName();
					confBean.setModerator(teaLoginName);//设置主持人
					//会议结束时间没有设置，则设置结束时间
					String endDate = classT.getEndtime();
					SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
					Date confEndDate = null;
					try {
						confEndDate = df.parse(endDate);
					} catch (ParseException e) {
						e.printStackTrace();
					}
					confBean.setFinishTime(confEndDate);//设置结束时间
				} catch (TTransportException e) {
					log.error("调用thrift接口出错",e);
					e.printStackTrace();
				} catch (TPlasoException e) {
					log.error("调用thrift接口出错",e);
					e.printStackTrace();
				} catch (TException e) {
					log.error("调用thrift接口出错",e);
					e.printStackTrace();
				}
			}else{
				confBean.setFinishTime(memoryConfInfo.getFinishTime());
				confBean.setModerator(memoryConfInfo.getModerator());
			}
			synchronized(syncLock){
			List<ConfMemberBean> memberList = confBean.getMemberList();
			if(memoryConfInfo != null){
				List<ConfMemberBean> memoryMemberList = memoryConfInfo.getMemberList();
				if(memoryMemberList != null && memoryMemberList.size() > 0){//如果会议已经存在，设置会议室成员原来的状态
					int len = memberList.size();
					int lenMemory = memoryMemberList.size();
					ConfMemberBean memItem = null;
					ConfMemberBean memoryMemItem = null;
					for(int i = 0; i < len; i++){
						boolean isExist = false;
						memItem = memberList.get(i);
						for(int j = 0; j < lenMemory; j++)
						{
							memoryMemItem = memoryMemberList.get(j);
							if(memItem.getMid().equals(memoryMemItem.getMid())){
								memItem.setDraw(memoryMemItem.isDraw());
								isExist = true;
								break;
							}
						}
						if(!isExist){
							//设置主持人为可以手写状态
							if(memItem.getMemberNum().equals(confBean.getModerator())){
								
								memItem.setDraw(true);
							}
						}
					}
				}
			}else{//会议不存在，则设置主持人手写状态
					int len = memberList.size();
					ConfMemberBean memItem = null;
					for(int i = 0; i < len; i++){
						memItem = memberList.get(i);
						if(memItem.getMemberNum().equals(confBean.getModerator())){
							memItem.setDraw(true);
						}
					}
			}
			this.confList.put(confId, confBean);
		}
	}
	
	/**
	 * 获取thrift客户端
	 * @return
	 * @throws TTransportException
	 */
	private TLiveClassService.Client getLiveClient() throws TTransportException {
		return getLiveClient(liveREMOTE_API);
	}
	
	private TLiveClassService.Client getLiveClient(String servletUrl) throws TTransportException {
		THttpClient thc = new THttpClient(servletUrl);
		TProtocol loPFactory = new TBinaryProtocol(thc);
		TLiveClassService.Client client = new TLiveClassService.Client(loPFactory);
		return client;
	}
	
	/**
	 * 设置会议手写状态
	 * @param mid 成员ID
	 * @param confId  会议ID
	 * @param drawState 手写状态
	 */
	private void setDrawState(String mid, String confId, boolean drawState){
		synchronized(syncLock){
			ConferenceBean tmpBean = this.confList.get(confId);
			List<ConfMemberBean> memberList = tmpBean.getMemberList();
			int len = memberList.size();
			ConfMemberBean memItem = null;
			for(int i= 0; i <len; i++ )
			{
				memItem = memberList.get(i);
				if(mid.equals(memItem.getMid())){
					memItem.setDraw(drawState);
					break;
				}
			}
		}

	}
	
	/**
	 * 开始录音
	 * @param confId
	 */
	protected void startRecord(String confId, String fileName){
		log.info("会议："+confId+"开始录音。。。");
		String recordFile = this.confRecordFile.get(confId);
		if(recordFile != null){//如果上一次还没结束，则将上次录音结束
			log.info("上次录音还没结束，结束上次录音。。。");
			String command = "conference "+confId+"-\\'sip.plaso.com\\' recording stop "+recordFile;
			log.info("发送结束录音命令："+command);
			String rtn = fsc.sendMessage(command);
			log.info(rtn);
			
			log.info("开始新的录音。。。");
			fileName = fileName.split("\\.")[0];
			recordFile = RecordPath+fileName+".wav";
			command = "conference "+confId+"-\\'sip.plaso.com\\' record "+recordFile;
			log.info("发送开始录音命令："+command);
			rtn = fsc.sendMessage(command);
			log.info(rtn);
			this.confRecordFile.put(confId, recordFile);
		}else{
			fileName = fileName.split("\\.")[0];
			recordFile = RecordPath+fileName+".wav";
			String command = "conference "+confId+"-\\'sip.plaso.com\\' record "+recordFile;
			log.info("发送开始录音命令："+command);
			String rtn = fsc.sendMessage(command);
			log.info(rtn);
			this.confRecordFile.put(confId, recordFile);
		}
	}
	
	/**
	 * 结束录音
	 * @param confId
	 */
	protected void endRecord(String confId){
		log.info("会议"+confId+"结束录音");
		String recordFile = this.confRecordFile.get(confId);
		if(recordFile != null){
			String command = "conference "+confId+"-\\'sip.plaso.com\\' recording stop "+recordFile;
			log.info("发送结束录音命令："+command);
			String rtn = fsc.sendMessage(command);
			log.info(rtn);
			wav2iLBC(recordFile);//wav转ilbc
			this.confRecordFile.remove(confId);
		}
	}
	
	/**
	 * wav 转 ilbc
	 * @param fileName
	 */
	protected void wav2iLBC(String fileName){
		log.info("wav转ilbc 开始。。。。");
		log.info("原文件："+fileName);
		String tarFileName = fileName.replaceAll("wav", "ilbc");
		log.info("目的文件："+tarFileName);
		Runtime run=null;
	 	 try{
				run=Runtime.getRuntime();
				long start=System.currentTimeMillis();
				String command = "lame -b 16 "+fileName+" "+tarFileName;
				log.info("转换命令"+command);
				Process p=run.exec(command); //16为码率，可自行修改
//				int exitVal = p.exitValue();
//
//				log.info("转换结束标志"+exitVal);
				//important
//				p.getOutputStream().flush();
//				p.getOutputStream().close();
//				p.getInputStream().close();
//				p.getErrorStream().close();
//				
//				p.waitFor();
				long end=System.currentTimeMillis();
				log.info("convert costs:"+(end-start));
			}
			catch(Exception err){
				err.printStackTrace();
			}
			finally{
				if(run!=null){
					run.freeMemory();
				}
			}
	}
	
	/**
	 * 推送录音文件名
	 * @param confId
	 * @param fileName
	 */
	private void sendRecordFile(String confId){
		ConferenceBean confBean = this.confList.get(confId);
		String fileName = this.confRecordFile.get(confId);
		if(confBean != null){
			Message beanmsg = null;
			List<ConfMemberBean> memberList = confBean.getMemberList();
			ConfMemberBean memItem = null;
			if(memberList != null){
				int len = memberList.size();
				for(int i = 0; i < len; i++)
				{
					memItem = memberList.get(i);
					if(memItem.getMemberNum().equals(confBean.getModerator())){
						fileName = fileName.substring(fileName.indexOf(RecordDir)+(RecordDir.length()+1));
						fileName = fileName.replaceAll("wav", "ilbc");
						StringBuffer bodyMsgBuf = new StringBuffer();
						bodyMsgBuf.append("<confRecord confId=\"");
						bodyMsgBuf.append(confId);
						bodyMsgBuf.append("\" ");
						bodyMsgBuf.append("recordFile=\"");
						bodyMsgBuf.append(fileName);
						bodyMsgBuf.append("\" />");
						log.info("推送录音文件名："+bodyMsgBuf.toString());

						beanmsg = new Message();
						beanmsg.setFrom("conf@"+ XMPPServer.getInstance().getServerInfo().getXMPPDomain());
						beanmsg.setTo(memberList.get(i).getMemberNum() + "@" + GroupServiceAction.chatXmppDomain);
						beanmsg.setBody(bodyMsgBuf.toString());
						beanmsg.addChildElement("PlasoType", "").addText("PlasoConfRecord");
						XMPPServer.getInstance().getMessageRouter().route(beanmsg);
					}
					log.info("推送录音文件名发送成功");
				}
			}
		}
		
	}
	
	/**
	 * 判断会议是否到时
	 * @param confBean
	 * @return
	 */
	private boolean isConfTimeOut(String confId){
		ConferenceBean confBean = this.confList.get(confId);
		if(confBean != null && confBean.getFinishTime() != null){
			Date endDate = confBean.getFinishTime();
			long endTime = endDate.getTime();
			long nowTime = System.currentTimeMillis();
			if(nowTime > endTime){//当发现会议过期，再去调用接口，取最新结束时间判断一下。
				TLiveClassService.Client lc = null;
				try {
					lc = getLiveClient();
					TLiveClass classT = lc.getByMettingNumber(confId, MAGIC_TOKEN);
					String refreshEndDate = classT.getEndtime();
					log.info(confId+"即将挂断前判断：原结束时间为："+endDate.toGMTString());
					log.info(confId+"即将挂断前判断：新结束时间为："+refreshEndDate);
					SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
					Date confEndDate = null;
					try {
						confEndDate = df.parse(refreshEndDate);
					} catch (ParseException e) {
						e.printStackTrace();
					}
					long newEndTime = confEndDate.getTime();
					if(nowTime > newEndTime){
						log.info("newEndTime="+newEndTime);
						log.info("nowTime="+nowTime);
						log.info("会议挂断前判断：该会议Id对应的最新结束时间已到，将执行挂断操作");
						return true;
					}else{//有新的结束时间，说明此对象为上次未清除对象，则从容器中删除
						log.info("会议挂断前判断：该会议为新开会议，将不执行挂断操作");
						this.confList.remove(confId);
					}
				} catch (TTransportException e) {
					log.error("调用thrift接口出错",e);
					e.printStackTrace();
				} catch (TPlasoException e) {
					log.error("调用thrift接口出错",e);
					e.printStackTrace();
				} catch (TException e) {
					log.error("调用thrift接口出错",e);
					e.printStackTrace();
				}
				return false;
			}
		}
		
		return false;
	}
	
	/**
	 * 挂断会议
	 * @param confId 会议ID
	 */
	public void hangupConf(String confId){
		log.info("挂断会议："+confId);
		String command = "conference "+confId+"-\\'sip.plaso.com\\'"+" hup all";
		fsc.sendMessage(command);
	}
	
	/**
	 * 会议结束，发送通知消息
	 * @param confId
	 */
	private void sendConfEndState(String confId){
		ConferenceBean confBean = this.confList.get(confId);
		if(confBean != null){
			Message beanmsg = null;
			List<ConfMemberBean> memberList = confBean.getMemberList();
			if(memberList != null){
				String bodyMsg = "<memList confId = \""+confId+"\"/>";
				log.info("结束会议发送消息："+bodyMsg);
				int len = memberList.size();
				for(int i = 0; i < len; i++)
				{
					beanmsg = new Message();
					beanmsg.setFrom("conf@"+ XMPPServer.getInstance().getServerInfo().getXMPPDomain());
					beanmsg.setTo(memberList.get(i).getMemberNum() + "@" + GroupServiceAction.chatXmppDomain);
					beanmsg.setBody(bodyMsg);
					beanmsg.addChildElement("PlasoType", "").addText("PlasoConfStatus");
					XMPPServer.getInstance().getMessageRouter().route(beanmsg);
					log.info("结束会议消息发送成功");
				}
			}
		}
		//清除会议
		this.confList.remove(confId);
		
		log.info("挂断会议："+confId+"成功，当前剩余会议场数："+this.confList.size());
	}
	
	/**
	 * 推送会议状态
	 * @param confBean
	 */
	private void sendConfState(String confId){
		ConferenceBean confBean = this.confList.get(confId);
		Message beanmsg = null;
		List<ConfMemberBean> memberList = confBean.getMemberList();
		String bodyMsg = parseSendXml(confBean);
		log.info("发送消息："+bodyMsg);
		int len = memberList.size();
		for(int i = 0; i < len; i++)
		{
			beanmsg = new Message();
			beanmsg.setFrom("conf@"+ XMPPServer.getInstance().getServerInfo().getXMPPDomain());
			beanmsg.setTo(memberList.get(i).getMemberNum() + "@" + GroupServiceAction.chatXmppDomain);
			beanmsg.setBody(bodyMsg);
			beanmsg.addChildElement("PlasoType", "").addText("PlasoConfStatus");
			XMPPServer.getInstance().getMessageRouter().route(beanmsg);
			log.info("发送消息成功");
		}
	}
	
	/**
	 * 构造会议状态xml报文
	 * @param confBean
	 * @return
	 */
	private String parseSendXml(ConferenceBean confBean){
		List<ConfMemberBean> memberList = confBean.getMemberList();
		StringBuffer strBuf = new StringBuffer();
		strBuf.append("<memList confId=\""+confBean.getConfId()+"\">");
		int len = memberList.size();
		for(int i = 0; i < len; i++)
		{
			ConfMemberBean tmpBean = memberList.get(i);
			
			strBuf.append("<member id=\""+tmpBean.getMemberNum()+"\" mid=\""+tmpBean.getMid()+"\" status=\"");
			
			if(tmpBean.isMute()){
				strBuf.append("mute");
			}else{
				strBuf.append("unmute");
			}
			if(tmpBean.isDraw()){
				strBuf.append(",draw\"/>");
			}else{
				strBuf.append(",undraw\"/>");
			}
		}
		strBuf.append("</memList>");
		return strBuf.toString();
	}
	
	/**
	 * 会议室轮训任务
	 * @author pengqp
	 *
	 */
	private class confMonitorTask extends TimerTask{

		@Override
		public void run() {
			getConferences();
		}
	}
	
	/**
	 * 根据会议ID返回会议对象
	 * @param confId
	 * @return
	 */
	public ConferenceBean getConfById(String confId){
		return this.confList.get(confId);
	}

	public static void main(String[] args) throws DocumentException {
//		String xml = "<operationList confId = \"3000\"><action mid=\"100\" operation=\"mute\"/><action mid=\"101\" operation=\"unmute\"/></operationList>";
//		String xml = "<operationList confId=\"3001\"><action mid=\"274\" operation=\"mute\"/></operationList>";
		String xml = "<message type=\"chat\" to=\"conf@xmppgroup.plaso.cn\" from=\"pt@xmpp.plaso.cn/7370614d\"><body><operationList confId=\"3001\"><action mid=\"282\" operation=\"mute\"/></operationList></body><PlasoType>PlasoConfControl</PlasoType></message>";
		try {
//			log.info("消息体报文："+xml);
			Document doc = DocumentHelper.parseText(xml);
			Element elt = doc.getRootElement();
			Element eltBody = elt.element("body");
			Element operationE = eltBody.element("operationList");
			String confId = operationE.attributeValue("confId");
			Iterator iterAction = operationE.elementIterator("action");
			while(iterAction.hasNext())
			{
				Element actionE = (Element) iterAction.next();
				String mid = actionE.attributeValue("mid");
				String operation = actionE.attributeValue("operation");
//				handleConfOperaion(confId,mid,operation);
				System.out.println(mid);
				System.out.println(operation);
			}
		} catch (DocumentException e) {
			e.printStackTrace();
		}

//		System.out.print(elt.asXML());
	}
}
