package com.plaso.conf;

import java.util.List;

import org.freeswitch.esl.client.IEslEventListener;
import org.freeswitch.esl.client.inbound.Client;
import org.freeswitch.esl.client.inbound.InboundConnectionFailure;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.freeswitch.esl.client.transport.message.EslMessage;

public class Test {

	public static void main(String[] args) {

		Client c = new Client();
		try {
			c.connect("xmpp.plaso.cn", 8021, "ClueCon", 10);
			EslMessage el = c.sendSyncApiCommand("conference 3001-\\'sip.plaso.com\\' mute 274", "");
//			EslMessage el = c.sendSyncApiCommand("conference xml_list", "");
//			EslMessage el = c.sendSyncApiCommand("conference 3600-\\'sip.plaso.com\\' kick 11", "");
//			List res = el.getBodyLines();
			StringBuffer strBuf = new StringBuffer();
			c.addEventListener(new IEslEventListener(){

				@Override
				public void eventReceived(EslEvent event) {
					System.out.println("aa"+event);
					List<String> bodys = event.getEventBodyLines();
					for(int j=0;j<bodys.size();j++)
					{
						System.out.println(bodys.get(j));
					}
					
				}

				@Override
				public void backgroundJobResultReceived(EslEvent event) {
					System.out.println("bb"+event);
					// TODO Auto-generated method stubst
					
				}
				
			});
//			for(int i = 0; i < res.size(); i++)
//			{
//				System.out.println(res.get(i));
//			}
//			System.out.println(el.getBodyLines());
		} catch (InboundConnectionFailure e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
