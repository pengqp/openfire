package com.plaso.xmpp.groupchat;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author chenqing
 * @date 2013-7-19 上午08:40:43
 * @desc 群服务工具类
 */
public class GroupUtils {
	/**
	 * 日志实例
	 */
	private static Logger log = LoggerFactory.getLogger(GroupUtils.class);

	/**
	 * get方式读取httpURL资源内容
	 * 
	 * @param httpUrl
	 *            httpUrl
	 * @param params
	 *            参数map
	 * @return httpURL资源内容,json格式的：{"ResponseCode":"200","ResponseMessage":
	 *         "成功","content":"..."}
	 * @throws IOException
	 * @desc 方法内部对参数采用UTF-8编码
	 */
	public static String readGetContent(String httpUrl,
			HashMap<String, String> params) {
		// 解析完整资源URL
		String resourceUrl = (null != params && !params.isEmpty()) ? httpUrl
				+ "?" + parseQueryStr(params) : httpUrl;
		String code, message;
		boolean bSucessful = false;
		StringBuilder content = new StringBuilder();
		BufferedReader reader = null;

		try {
			URL resUrl = new URL(resourceUrl);
			HttpURLConnection connection = (HttpURLConnection) resUrl
					.openConnection();
			connection.connect();
			// 取得输入流，并使用Reader读取
			reader = new BufferedReader(new InputStreamReader(connection
					.getInputStream()));
			content = new StringBuilder();
			String lines = "";
			while ((lines = reader.readLine()) != null) {
				content.append(lines);
			}
			// 断开连接
			connection.disconnect();
			bSucessful = true;
		} catch (MalformedURLException e) {
			log.error("URL(" + resourceUrl + ")格式错误: " + e);
			content.append(e.getMessage());
		} catch (IOException e) {
			log.error("IO异常: " + e);
			content.append(e.getMessage());
		} finally {
			if (null != reader)
				try {
					reader.close();
				} catch (IOException e) {
				}
		}

		code = bSucessful ? "200" : "201";
		message = bSucessful ? "GET读取成功" : "读取失败";

		return new StringBuilder("\"ResponseCode\":").append(code).append(
				",\"ResponseMessage\": ").append(message).append(
				",\"content\":").append(content).toString();
	}

	/**
	 * POST方式读取httpURL资源内容
	 * 
	 * @param httpUrl
	 *            httpUrl
	 * @param params
	 *            参数map
	 * @return httpURL资源内容,json格式的：{"ResponseCode":"200","ResponseMessage":
	 *         "成功","content":"..."}
	 * @throws IOException
	 * @desc 方法内部对参数采用UTF-8编码
	 */
	public static String readPostContent(String httpUrl,
			HashMap<String, String> params) {
		StringBuilder sb = new StringBuilder();
		String code, message;
		boolean bSucessful = false;
		BufferedReader reader = null;
		try {
			URL resourceUrl = new URL(httpUrl);
			HttpURLConnection connection = (HttpURLConnection) resourceUrl
					.openConnection();

			// 设置是否向connection输出，因为这个是post请求，参数要放在http正文内，因此需要设为true
			connection.setDoOutput(true);
			// Read from the connection. Default is true.
			connection.setDoInput(true);
			// Set the post method. Default is GET
			connection.setRequestMethod("POST");
			// Post 请求不能使用缓存
			connection.setUseCaches(false);
			// URLConnection.setInstanceFollowRedirects是成员函数，仅作用于当前函数
			connection.setInstanceFollowRedirects(true);
			// Content-type: application/x-www-form-urlencoded
			// 表明urlencoded编码过的form参数
			connection.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			// 开始连接（连接前配置必须要完成，否则无效）
			connection.connect();
			DataOutputStream out = new DataOutputStream(connection
					.getOutputStream());
			String content = parseQueryStr(params);
			// DataOutputStream.writeBytes将字符串中的16位的unicode字符以8位的字符形式写道流里面
			out.writeBytes(content);
			out.flush();
			out.close(); // flush and close
			reader = new BufferedReader(new InputStreamReader(connection
					.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
			connection.disconnect();
			bSucessful = true;
		} catch (MalformedURLException e) {
			log.error("URL(" + httpUrl + ")格式错误: " + e);
			sb.append(e.getMessage());
		} catch (ProtocolException e) {
			log.error("协议异常: " + e);
			sb.append(e.getMessage());
		} catch (IOException e) {
			log.error("IO异常: " + e);
			sb.append(e.getMessage());
		} finally {
			if (null != reader)
				try {
					reader.close();
				} catch (IOException e) {
				}
		}

		code = bSucessful ? "200" : "201";
		message = bSucessful ? "POST读取成功" : "读取失败";

		return new StringBuilder("\"ResponseCode\":").append(code).append(
				",\"ResponseMessage\": ").append(message).append(
				",\"content\":").append(sb).toString();
	}

	/**
	 * 拼接参数字符串，参数值采用UTF-8编码，如果不支持，采用系统默认编码
	 * 
	 * @param params
	 *            参数集合
	 * @return 解析后的参数字符串，格式key1=value1&key2=value2...
	 */
	private static String parseQueryStr(HashMap<String, String> params) {
		if (null == params || params.isEmpty())
			return "";
		StringBuilder sb = new StringBuilder();
		for (String key : params.keySet())
			try {
				sb.append(key).append("=").append(
						URLEncoder.encode(params.get(key), "utf-8"))
						.append("&");
			} catch (UnsupportedEncodingException e) {
				log
						.error("system does not support UTF-8 encoder, will use default charset("
								+ Charset.defaultCharset().name() + ").");
				try {
					sb.append(key).append("=").append(
							URLEncoder.encode(params.get(key), Charset
									.defaultCharset().name())).append("&");
				} catch (UnsupportedEncodingException e1) {
				}
			}
		return sb.subSequence(0, sb.length() - 1).toString();
	}

	/**
	 * 获取http资源
	 * @param httpUrl httpUrl
	 * @param data 参数数据
	 * @return httpURL资源内容,json格式的：{"ResponseCode":"200","ResponseMessage":
	 *         "成功","content":"..."} 
	 * @throws Exception
	 * @desc 方法内部对参数采用UTF-8编码
	 */
	public static String sendHttpData(String httpUrl, String data) {
		HttpClient client = new HttpClient();

		PostMethod httpPost = new PostMethod(httpUrl);
		InputStream is = null;
		try {
			is = new java.io.ByteArrayInputStream(data.getBytes("utf-8"));
		} catch (UnsupportedEncodingException e1) {
			try {
				is = new java.io.ByteArrayInputStream(data.getBytes(Charset.defaultCharset().name()));
			} catch (UnsupportedEncodingException e) {}
		}

		httpPost.setRequestHeader("Content-type",
				"application/x-www-form-urlencoded;");
		httpPost.setRequestHeader("Connection", "close");
		httpPost.setRequestBody(is);

		/*String code, message;
		boolean bSucessful = false;*/

		StringBuffer sb = new StringBuffer();
		int resStatusCode = 0;
		try {
			client.executeMethod(httpPost);
			resStatusCode = httpPost.getStatusCode();
			if (resStatusCode == HttpStatus.SC_OK) {
				BufferedReader br = new BufferedReader(new InputStreamReader(
						httpPost.getResponseBodyAsStream(), "utf-8"));
				String res = null;
				while ((res = br.readLine()) != null) {
					sb.append(res);
				}
				//bSucessful = true;
			}
		} catch (Exception e) {
			log.error("远程请求http(" + httpUrl + ")异常： " + e);
			sb.append("远程请求http(" + httpUrl + ")异常： " + e.getMessage());
		} finally {
			httpPost.releaseConnection();
		}

		return sb.toString();
	}
}
