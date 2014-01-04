package com.plaso.xmpp.groupchat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocketClient {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) throws UnknownHostException, IOException {
		// TODO Auto-generated method stub
		// 获取服务器地址
        String server = "127.0.0.1";
        String json = "{\"ResponseCode\": \"200\",\"ResponseMessage\": \"成功\",\"RoomInfo\": {" + 
        		"\"RoomId\": \"20130613173448006080810005\",\"Member\": [{\"name\": \"苏林\",\"id\": \"6261000100\",\"jid\": " +
				"\"6261000100@10.67.12.6\",\"role\": \"0\"},{\"name\": \"陈文虎\",\"id\": \"6261000011\",\"jid\": " +
				"\"6261000011@10.67.12.6\",\"role\": \"1\"},{\"name\": \"蒋永乐\",\"id\": \"6261000666\",\"jid\": " +
				"\"6261000666@10.67.12.6\",\"role\": \"1\"}]}}";
        // 获取需要发送的信息
        byte[] data = json.getBytes("utf-8");
        // 如果有三个从参数那么就获取发送信息的端口号，默认端口号为8099
        int servPort = 5001;
        // 根据服务器地址和端口号实例化一个Socket实例
        Socket socket = new Socket(server, servPort);
        System.out.println("Connected to server...sending echo string");
        // 返回此套接字的输入流，即从服务器接受的数据对象
        //InputStream in = socket.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(
				new BufferedInputStream(socket.getInputStream())));
        // 返回此套接字的输出流，即向服务器发送的数据对象
        OutputStream out = socket.getOutputStream();
        // 向服务器发送从控制台接收的数据
        out.write(data);
        out.flush();
        // 接收数据的计数器，将写入数据的初始偏移量
       /* int totalBytesRcvd = 0;
        // 初始化接收数据的总字节数
        int bytesRcvd;*/
       /* while (totalBytesRcvd < data.length) {
            // 服务器关闭连接，则返回 -1,read方法返回接收数据的总字节数
            if ((bytesRcvd = in.read(data, totalBytesRcvd, data.length
                    - totalBytesRcvd)) == -1)
                throw new SocketException("与服务器的连接已关闭");
            totalBytesRcvd += bytesRcvd;
        }*/

		String str = br.readLine();
		//while ((str = br.readLine()) != null)
			System.out.println(str);
        // 打印服务器发送来的数据
        //System.out.println("Received:" + new String(data,"utf-8"));
        // 关闭连接
        socket.close();
	}

}
