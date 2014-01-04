package com.plaso.xmpp.groupchat;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.util.JiveGlobals;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author chenqing
 * @date 2013-7-25 下午07:14:15
 * @desc 存放消息的队列
 */
public class MessageQueue {
	/**
	 * 日志实例
	 */
	private static Logger log = LoggerFactory
			.getLogger(GroupServiceManager.class);

	/**
	 * 单例
	 */
	private static MessageQueue msgQueue = new MessageQueue();

	private ThreadPoolExecutor threadPool;

	private BlockingQueue<String> queue = new LinkedBlockingQueue<String>(10000);

	/**
	 * 默认构造函数,启动消息助理线程
	 */
	private MessageQueue() {

		int coreThreads = GroupChatProperties.getInstance().getIntProperty(
				"groupchat.server.processing.core.threads", 2);
		int maxThreads = GroupChatProperties.getInstance().getIntProperty(
				"groupchat.server.processing.max.threads", 10);
		int queueSize = GroupChatProperties.getInstance().getIntProperty(
				"groupchat.server.processing.queue", 10);
		threadPool = new ThreadPoolExecutor(coreThreads, maxThreads, 60,
				TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(queueSize),
				new ThreadPoolExecutor.CallerRunsPolicy());

		XmppMsgAssist assist = new XmppMsgAssist();
		new Thread(assist).start();
	}

	/**
	 * 获取MessageQueue配置实例，单例模式
	 */
	public static MessageQueue getInstance() {
		return msgQueue;
	}

	public void add(String str) {
		queue.add(str);
	}

	/**
	 * @author chenqing
	 * @datetime 2013-6-3 上午12:37:56
	 * @desc 群聊消息助理，处理消息队列中的消息
	 */
	private class XmppMsgAssist implements Runnable {
		private XmppMsgAssist() {
		}

		private void processMessage() throws InterruptedException {

			try {
				final JSONObject jo = new JSONObject(queue.take());

				threadPool.execute(new Runnable() {
					public void run() {
						XmppMsgHandler handler = MsgHandlerFactory
								.getInstance().getMsgHandler(jo);
						if (null != handler)
							try {
								handler.processMsg(jo);

							} catch (JSONException e) {
								log.error("处理消息时异常：", e);
							}
					}
				});

			} catch (JSONException e1) {
				log.error(e1.toString());
				e1.printStackTrace();
				return;
			}

		}

		public void run() {
			log.info("thread(" + Thread.currentThread().getName()
					+ ") begin run");
			while (true) {
				try {
					processMessage();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}
}
