package com.plaso.xmpp.groupchat;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.manybrain.persistent.MemCacheClient;
import com.plaso.thrift.gen.TStudent;
import com.plaso.thrift.gen.TTeacher;

public class MemCacheService {
	
	/**
	 * 日志实例
	 */
	private static Logger log = LoggerFactory.getLogger(GroupServiceAction.class);
	
	private static MemCacheService ins_ = null;
	
	/**
	 * 缓存客户端
	 */
	private static MemCacheClient mcc = null;
	
	/**
	 * 获取缓存对象
	 * @return
	 */
	public static MemCacheService getInstance(){
		if(ins_== null){
			ins_ = new MemCacheService();
		}
		return ins_;
	}
	
	/**
	 * 构造函数
	 */
	private MemCacheService(){
	}
	
	public void start(){
		String[] servers = new String[] { "xmpp.plaso.cn:11211" };
		int[] weights = new int[] { 3 };
		try {
			mcc = new MemCacheClient(servers, weights);
			log.info("memcache 初始化完成");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void saveObj(String key, Object o) throws IOException
	{
		if(mcc != null){
			mcc.set(key, o);
		}
	}
	
	public void saveTTeacherList(String key, List<TTeacher> o) throws IOException
	{
		if(mcc != null){
			mcc.set(key, o);
		}
	}
	
	public void saveTStudentList(String key, List<TStudent> o) throws IOException
	{
		if(mcc != null){
			mcc.set(key, o);
		}
	}
	
	public Object getObj(String key){
		if(mcc != null){
			return mcc.get(key);
		}else{
			return null;
		}
		
	}
	
	public List<TTeacher> getTeacherListByGroupClassId(String classId){
		Object o = this.getObj(classId);
		if(o instanceof List<?>){
			return (List<TTeacher>) o;
		}
		return null;
	}
	
	public List<TStudent> getStudentListByGroupClassId(String classId){
		Object o = this.getObj(classId);
		if(o instanceof List<?>){
			return (List<TStudent>) o;
		}
		return null;
	}
}
