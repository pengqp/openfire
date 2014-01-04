package com.plaso.xmpp.groupchat;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Administrator
 * @datetime 2013-6-2 下午10:56:49
 * @desc 消息处理工厂类
 */
public class MsgHandlerFactory {
	/**
	 * 日志实例
	 */
	private static Logger log = LoggerFactory.getLogger(MsgHandlerFactory.class);
	
	/**
	 * 代价最小的锁对象，比new Object()要经济高效
	 */
	private static byte[] syncLock = new byte[0]; 
	
	/**
	 * 单例
	 */
	private static MsgHandlerFactory factory = null;
	
	/**
	 * 常量
	 */
	public static final String GROUP_ONLINE = "online";
	public static final String GROUP_OFFLINE = "offline";
	public static final String GROUP_MODIFY = "newtitle";
	public static final String GROUP_JOIN = "invite";
	public static final String GROUP_QUIT = "quitroom";
	public static final String GROUP_KICK = "kick";
	
	/**
	 * 集合对象，建立类处理器集合
	 */
	private Map<Integer,String> nameMap = new HashMap<Integer,String>();
	private Map<Integer,Class<?>> classMap = new HashMap<Integer,Class<?>>();
	private Map<String,XmppMsgHandler> handlerMap = 
		new HashMap<String,XmppMsgHandler>();
	
	/**
	 * 私有构造器，初始化集合元素
	 */
	private MsgHandlerFactory(){
		nameMap.put(1, GROUP_ONLINE);
		nameMap.put(2, GROUP_OFFLINE);
		nameMap.put(3, GROUP_MODIFY);
		nameMap.put(4, GROUP_JOIN);
		nameMap.put(5, GROUP_QUIT);
		nameMap.put(6, GROUP_KICK);
		
		classMap.put(1, GroupOnlineHandler.class);
		classMap.put(2, GroupOfflineHandler.class);
		classMap.put(3, ModifyGroupMsgHandler.class);
		classMap.put(4, JoinGroupMsgHandler.class);
		classMap.put(5, QuitGroupMsgHandler.class);
		classMap.put(6, KickoutGroupMsgHandler.class);
	}

	/** 
	 * 获取GroupServiceManager配置实例，单例模式
	 */
	public static MsgHandlerFactory getInstance(){
		synchronized(syncLock){
			if (factory == null)
				factory = new MsgHandlerFactory();
		}
		return factory;
	}
	
	/**
	 * 获取消息处理类
	 * @param json json串
	 * @return 消息处理类
	 */
	public XmppMsgHandler getMsgHandler(JSONObject jo){
		log.info("getMsgHandler:" + jo.toString());
		XmppMsgHandler handler = null;
		try {
			int operType = jo.getInt("operType");
			handler = handlerMap.get(nameMap.get(operType));
			if (null == handler)
				handler = instantiateHandler(classMap.get(operType));
		} catch (JSONException e) {
			log.error("获取消息处理器时异常：",e);
			handler = null;
		}
		return handler;
	}
	
	/**
	 * 实例化消息处理类，并加入集合中
	 * @param handlerClass
	 * @return 消息处理类
	 */
	private XmppMsgHandler instantiateHandler(Class<?> handlerClass) {
		log.info("begin instantiate Class: " + handlerClass.getName());
		XmppMsgHandler handler = null;
		if (handlerClass != null) {
			try {
				handler = (XmppMsgHandler)handlerClass.newInstance();
				handlerMap.put(handler.getHandlerName(), handler);
			} catch (Exception e) {
				log.error("实例化handler类时异常",e);
				handler = null;
			} 
		}

		return handler;
	}
	
	
	
	
	
}
