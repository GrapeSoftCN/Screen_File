package model;

import JGrapeSystem.jGrapeFW_Message;
import apps.appsProxy;
import authority.plvDef.UserMode;
import database.DBHelper;
import database.db;
import interfaceModel.GrapeDBSpecField;
import interfaceModel.GrapeTreeDBModel;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import json.JSONHelper;
import nlogger.nlogger;
import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import rpc.execRequest;
import session.session;
import string.StringHelper;

public class FileModel {
	private GrapeTreeDBModel file;
    private GrapeDBSpecField gDbSpecField;
	
	private JSONObject _obj = new JSONObject();
	private String sid = null;
	private String currentWeb = null;
	private String userId = null;
	private JSONObject UserInfo = null;
	private session se;

	public FileModel() {
		file = new GrapeTreeDBModel();
        gDbSpecField = new GrapeDBSpecField();
        gDbSpecField.importDescription(appsProxy.tableConfig("Files"));
        file.descriptionModel(gDbSpecField);
        file.bindApp();
		
		se = new session();
		sid = session.getSID();
		if (this.sid != null) {
			this.UserInfo = se.getDatas();
		}
		if (UserInfo != null && UserInfo.size() > 0) {
			currentWeb = UserInfo.getString("currentWeb");
			userId = UserInfo.getString("_id");
		}
	}

	private db bind() {
		return file.bind(String.valueOf(appsProxy.appid()));
	}

	public db getDB() {
		return bind();
	}

	public String Show() {
		JSONArray array = null;
		db db = bind();
		if (currentWeb != null && !currentWeb.equals("null") && !currentWeb.equals("null")) {
			if (userId != null && !userId.equals("null") && !userId.equals("null")) {
				db.eq("wbid", currentWeb).eq("userid", userId);
				array = db.select();
				bind().clear();
			}

		}
		return resultMessage((array != null && array.size() > 0) ? array : new JSONArray());
	}

	public JSONObject add(JSONObject files) {
		String info = bind().data(files).insertOnce().toString();
		return find(info);
	}

	public String update(String fid, JSONObject FileInfo) {
		int code = bind().eq("_id", new ObjectId(fid)).data(FileInfo).update() != null ? 0 : 99;
		if (code != 0) {
			return resultmsg(code, "操作失败");
		}
		return find(fid).toString();
	}

	public int updates(String fids, JSONObject FileInfo) {
		bind().or();
		String[] value = fids.split(",");
		int i = 0;
		for (int len = value.length; i < len; i++) {
			bind().eq("_id", new ObjectId(value[i]));
		}
		return bind().data(FileInfo).updateAll() != 0L ? 0 : 99;
	}

	public JSONObject find(String fid) {
		return bind().eq("_id", new ObjectId(fid)).find();
	}

	public JSONObject find(String fileoldname, String url) {
		return bind().eq("fileoldname", fileoldname).eq("url", url).eq("isdelete", Integer.valueOf(0)).eq("userid", userId).find();
	}

	public JSONObject GetFile(String fid) {
		String[] value = fid.split(",");
		db db = bind().or();
		for (String tempid : value) {
			if (!tempid.equals("")) {
				db.eq("_id", new ObjectId(tempid));
			}
		}
		JSONArray array = db.select();

		JSONObject rObject = new JSONObject();
		if ((array != null) && (array.size() != 0)) {
			for (Iterator localIterator = array.iterator(); localIterator.hasNext();) {
				Object object2 = localIterator.next();
				JSONObject object = (JSONObject) object2;
				rObject.put(object.getString("_id"), object);
			}
		}
		return rObject;
	}

	public int getSize(JSONArray array) {
		int size = 0;
		int i = 0;
		for (int len = array.size(); i < len; i++) {
			JSONObject object = (JSONObject) array.get(i);
			size += Integer.parseInt(object.get("size").toString());
		}
		return size;
	}

	public JSONArray find(JSONObject fileInfo) {
		db db = bind();

		if (!fileInfo.containsKey("isdelete")) {
			db.eq("isdelete", Integer.valueOf(0));
		}
		for (Iterator localIterator = fileInfo.keySet().iterator(); localIterator.hasNext();) {
			Object object2 = localIterator.next();
			String key = object2.toString();
			String value = fileInfo.getString(key);
			try {
				if (!key.equals("fatherid")) {
					long values = Long.parseLong(value);
					db.eq(key, Long.valueOf(values));
				} else {
					db.eq(key, value);
				}
			} catch (Exception e) {
				nlogger.logout(e);
			}
		}
		return db.limit(20).select();
	}

	@SuppressWarnings("unchecked")
	public JSONObject page(int ids, int pageSize, JSONObject fileInfo) {
		String key;
		JSONArray array = null;
		JSONObject object = new JSONObject();
		long count = 0, totalSize = 0;
		db db = bind();
		if (fileInfo != null && fileInfo.size() != 0) {
			if (!fileInfo.containsKey("isdelete")) {
				db.eq("isdelete", 0);
			}
			for (Object object2 : fileInfo.keySet()) {
				key = object2.toString();
				if (key.equals("isdelete") || key.equals("filetype")) {
					db.eq(key, fileInfo.get(key));
				} else {
					db.eq(key, fileInfo.getString(key));
				}
			}
			if (currentWeb != null && !currentWeb.equals("null") && !currentWeb.equals("null")) {
				if (userId != null && !userId.equals("null") && !userId.equals("null")) {
					db.eq("wbid", currentWeb).eq("userid", userId);
					array = db.dirty().desc("time").page(ids, pageSize);
					count = db.dirty().count();
					totalSize = db.pageMax(pageSize);
					bind().clear();
				}

			}
		}
		object.put("totalSize", totalSize);
		object.put("currentPage", ids);
		object.put("pageSize", pageSize);
		object.put("total", count);
		object.put("data", (array != null && array.size() > 0) ? array : new JSONArray());
		return object;
	}

	/**
	 * 判断当前用户是否为管理员
	 * 
	 * @return
	 */
	public boolean isAdmin() {
		String temp = "";
		int userType = 0;
		session se = new session();
		JSONObject userInfo = se.getDatas();
		if (userInfo != null && userInfo.size() != 0) {
			temp = userInfo.getString("userType");
		}
		userType = !temp.equals("") ? Integer.parseInt(temp) : 0;
		return UserMode.admin == userType;
	}

	public int RecyBatch(String fid, JSONObject FileInfo) {
		if (!fid.contains(",")) {
			if (isfile(fid) == 0) {
				fid = getfid(fid);
			}
		} else
			fid = Batch(fid.split(","));

		return updates(fid, FileInfo);
	}

	private String Batch(String[] fids) {
		ArrayList list = new ArrayList();
		int i = 0;
		for (int len = fids.length; i < len; i++) {
			if (isfile(fids[i]) == 0) {
				list.add(getfid(fids[i]));
			} else
				list.add(fids[i]);
		}

		return StringHelper.join(list);
	}

	private int isfile(String fid) {
		int ckcode = 0;
		String type = find(fid).get("filetype").toString();
		if ("0".equals(type))
			ckcode = 0;
		else {
			ckcode = 1;
		}
		return ckcode;
	}

	private String getfid(String fid) {
		ArrayList list = new ArrayList();
		String cond = "{\"fatherid\":\"" + fid + "\"" + "}";
		JSONArray array = find(JSONHelper.string2json(cond));
		if (array.size() != 0) {
			int i = 0;
			for (int lens = array.size(); i < lens; i++) {
				JSONObject object = (JSONObject) array.get(i);
				list.add(object.getString("_id"));
			}
		}
		list.add(fid);
		return StringHelper.join(list);
	}

	private int delete(String fid) {
		if (fid.contains(",")) {
			bind().or();
			String[] value = fid.split(",");
			int i = 0;
			for (int len = value.length; i < len; i++)
				bind().eq("_id", new ObjectId(value[i]));
		} else {
			bind().eq("_id", new ObjectId(fid));
		}
		return bind().deleteAll() != 0L ? 0 : 99;
	}

	public int ckDelete(String fid) {
		if (!fid.contains(",")) {
			if (isfile(fid) == 0)
				deleteall(fid);
		} else {
			String[] value = fid.split(",");
			ArrayList list = new ArrayList();
			int i = 0;
			for (int len = value.length; i < len; i++) {
				if (isfile(value[i]) != 0)
					continue;
				list.add(value[i]);
			}

			if (list.size() != 0) {
				deleteall(StringHelper.join(list));
			}
		}
		return delete(fid);
	}

	public int delete(JSONObject object) {
		int code = 0;
		long size = ((Long) object.get("size")).longValue();
		if (object.containsKey("isdelete")) {
			code = ckDelete(object.get("_id").toString());
		}
		if (size > 0L) {
			code = ckDelete(object.get("_id").toString());
		} else {
			String infos = "{\"isdelete\":1}";
			code = RecyBatch(object.get("_id").toString(), JSONHelper.string2json(infos));
		}
		return code;
	}

	public int batch(JSONArray array) {
		int code = 0;

		boolean flag = false;
		List list = new ArrayList();
		List lists = new ArrayList();
		long FIXSIZE = new Long(4294967296L).longValue();
		int i = 0;
		for (int len = array.size(); i < len; i++) {
			JSONObject object = (JSONObject) array.get(i);
			if (object.containsKey("isdelete")) {
				flag = true;
				list.add(object.get("_id").toString());
			} else {
				Object temp = object.get("size");
				if (temp == null) {
					lists.add(object.get("_id").toString());
				} else if (((Long) temp).longValue() > FIXSIZE) {
					flag = true;
					list.add(object.get("_id").toString());
				} else {
					lists.add(object.get("_id").toString());
				}
			}
		}

		if (flag) {
			code = ckDelete(StringHelper.join(list));
		}
		if (lists.size() != 0) {
			String infos = "{\"isdelete\":1}";
			code = RecyBatch(StringHelper.join(lists), JSONHelper.string2json(infos));
		}
		return code;
	}

	private void deleteall(String fid) {
		if (fid.contains(",")) {
			bind().or();
			String[] value = fid.split(",");
			int i = 0;
			for (int len = value.length; i < len; i++)
				bind().eq("fatherid", value[i]);
		} else {
			bind().eq("fatherid", fid);
		}
		bind().deleteAll();
	}

	public String getPath(String key) {
		String value = "";
		try {
			value = getAppIp(key);
		} catch (Exception e) {
			value = "";
		}
		return value;
	}

	public String getAppIp(String key) {
		String value = "";
		try {
			Properties pro = new Properties();
			pro.load(new FileInputStream("URLConfig.properties"));
			value = pro.getProperty(key);
		} catch (Exception e) {
			value = "";
		}
		return value;
	}

	public String resultMessage(int num) {
		return resultmsg(num, "");
	}

	public String resultMessage(JSONObject object) {
		this._obj.put("records", object);
		return resultmsg(0, this._obj.toString());
	}

	public String resultMessage(JSONArray array) {
		this._obj.put("records", array);
		return resultmsg(0, this._obj.toString());
	}

	public String resultmsg(int num, String mString) {
		String msg = "";
		switch (num) {
		case 0:
			msg = mString;
			break;
		case 1:
			msg = "必填项为空";
			break;
		case 2:
			msg = "没有创建数据权限，请联系管理员进行权限调整";
			break;
		case 3:
			msg = "没有修改数据权限，请联系管理员进行权限调整";
			break;
		case 4:
			msg = "没有删除数据权限，请联系管理员进行权限调整";
			break;
		case 5:
			msg = "添加失败";
			break;
		case 6:
			msg = "该文件已存在";
			break;
		default:
			msg = "其他异常";
		}

		return jGrapeFW_Message.netMSG(num, msg);
	}
}