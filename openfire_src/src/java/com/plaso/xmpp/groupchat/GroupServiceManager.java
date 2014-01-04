package com.plaso.xmpp.groupchat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jivesoftware.openfire.XMPPServer;
//import org.xmpp.packet.Message;

/**
 * @author Administrator
 * @datetime 2013-7-19 上午06:38:54
 * @desc 群服务管理类，负责群初始化，群接口提供和调用，服务线程调配等
 */
public class GroupServiceManager {
	/**
	 * 日志实例
	 */
	private static Logger log = LoggerFactory.getLogger(GroupServiceManager.class);
	
	/**
	 * 代价最小的锁对象，比new Object()要经济高效
	 */
	private static boolean keep_run = false;
	
	private static Thread groupchat_thread = null;
	
	/**
	 * 单例
	 */
	private static GroupServiceManager gsManager = new GroupServiceManager();
	
	/**
	 * 私有构造函数
	 */
	private GroupServiceManager(){
	
	}
	
	/** 
	 * 获取GroupServiceManager配置实例，单例模式
	 */
	public static GroupServiceManager getInstance(){
		return gsManager;
	}
	
	/**
	 * 处理群消息
	 * @remark 启动Socket来接收客户端消息
	 */
	public void start() {
		keep_run = true;
		if (groupchat_thread == null) {
			groupchat_thread = new Thread(new Runnable() {
				public void run() {
					try {
						// 实例化一个ServerSocket对象实例
						ServerSocket servSocket = new ServerSocket(GroupChatProperties.getInstance().getSocketPort());
						log.info("Group Chat Socket is ready on port!" + GroupChatProperties.getInstance().getSocketPort());

						while(XMPPServer.getInstance().getMessageRouter()==null)
						{
							Thread.sleep(5000);
						}
						// 循环迭代，监听端口号,处理新的连接请求
						while (keep_run) {
//							 阻塞等待，每接收到一个请求就创建一个新的连接实例
							Socket socket = servSocket.accept();
							readSocket(socket);
						}
					} catch (NumberFormatException e) {
						log.error("端口号错误："+ e.getMessage());
					} catch (IOException e) {
						log.error("Socket异常：" + e.getMessage());
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
		}

		groupchat_thread.start();
	}


//	private void test() {
//		Message msg = new Message();		
//		msg.setFrom("group_ict_001@zte-20120713wxh");
//		msg.setTo("test1@zteict-svn");
//		msg.setType(Message.Type.chat);
//		msg.setBody("Hello Moto");
//		XMPPServer.getInstance().getMessageRouter().route(msg);	
//	}
	
	public void stop(){	
		keep_run = false;
		groupchat_thread = null;
	}
	/**
	 * 读取socket内容，并加入消息队列待处理
	 * @param socket
	 */
	private static void readSocket(final Socket socket) {
		new Thread(new Runnable() {
			public void run() {
				StringBuilder msgBuilder = new StringBuilder();
				try {
					// 读取客户端消息
					BufferedReader br = new BufferedReader(
							new InputStreamReader(new BufferedInputStream(
									socket.getInputStream())));
					String message = "";
					while ((message = br.readLine()) != null) {
						msgBuilder.append(message);
					}
					br.close();
					log.info("读取socket内容： " + msgBuilder.toString());
					MessageQueue.getInstance().add(msgBuilder.toString());
				} catch (IOException e) {
					log.error("读取socket内容时异常： " + e.getMessage());
				} finally {
					try {
						socket.close();
					} catch (IOException e) {
					}
				}
			}
		}).start();
	}

	
}