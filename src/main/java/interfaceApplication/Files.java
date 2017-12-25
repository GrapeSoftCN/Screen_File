package interfaceApplication;

import JGrapeSystem.rMsg;
import apps.appsProxy;
import file.fileHelper;
import httpClient.request;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Properties;
import json.JSONHelper;
import model.FileModel;
import nlogger.nlogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import security.codec;
import session.session;
import sun.misc.BASE64Decoder;
import time.TimeHelper;

public class Files {
	private String sid = null;
	private String currentWeb = null;
	private String userId = null;
	private JSONObject UserInfo = null;
	private session se;
	private FileModel fileModel = new FileModel();

	private String thumailPath = "\\File\\upload\\icon\\folder.ico";

	public Files() {
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
	private String getFlePath(String key) {
		String value = "0";
		JSONObject object = JSONObject.toJSON(appsProxy.configValue().getString("other"));
		if ((object != null) && (object.size() > 0) && (object.containsKey(key))) {
			value = object.getString(key);
		}

		return value;
	}

	@SuppressWarnings("unchecked")
	public String AddNetFile(String fileInfo) {
		String thumbnail = "";
		String result = this.fileModel.resultMessage(5);
		fileInfo = codec.DecodeHtmlTag(fileInfo);
		fileInfo = codec.decodebase64(fileInfo);
		JSONObject object = JSONObject.toJSON(fileInfo);
		if ((object != null) && (object.size() > 0)) {
			if (isExsits(object)) {
				return this.fileModel.resultMessage(6);
			}

			int width = Integer.parseInt(getFlePath("width"));
			int height = Integer.parseInt(getFlePath("height"));
			String url = encodeURL(object);
			if ((url != null) && (!url.equals(""))) {
//				String urls = host + "/" + appid + "/GrapeCutImage/ImageOpeation/getNetImage/" + url + "/int:" + width
//						+ "/int:" + height;
//				System.out.println(urls);
//				thumbnail = request.Get(urls);
				thumbnail = (String)appsProxy.proxyCall("/GrapeCutImage/ImageOpeation/getNetImage/" + url + "/int:" + width+"/int:" + height);
			}
			thumbnail = CreateImage(thumbnail);
			if ((thumbnail == null) || (thumbnail.equals("")) || (thumbnail.equals("null"))) {
				return rMsg.netMSG(7, "生成网页缩略图失败");
			}
			object.put("ThumbnailImage", thumbnail);
			object.put("isdelete", Integer.valueOf(0));
			object.put("filetype", object.get("type"));
			object.put("fatherid", "0");
			object.put("wbid", currentWeb);
			object.put("userid", userId);
			object.remove("type");
			System.out.println(object.toJSONString());
			result = this.fileModel.resultMessage(this.fileModel.add(object));
		}
		return result;
	}

	private String CreateImage(String thumbnail) {
		String path = "";
		String ext = "jpg";
		thumbnail = codec.DecodeHtmlTag(thumbnail);
		if (thumbnail != null) {
			if (thumbnail.contains("data:image/")) {
				ext = thumbnail.substring(thumbnail.indexOf("data:image/") + 11, thumbnail.indexOf(";base64,"));
				thumbnail = thumbnail.substring(thumbnail.lastIndexOf(",") + 1);
			}
			BASE64Decoder decoder = new BASE64Decoder();
			try {
				path = getFlePath("filepath");
				String Date = TimeHelper.stampToDate(TimeHelper.nowMillis()).split(" ")[0];
				path = path + "\\" + Date + "\\" + TimeHelper.nowMillis() + "." + ext;
				byte[] bytes = decoder.decodeBuffer(thumbnail);
				if (fileHelper.createFile(path)) {
					OutputStream out = new FileOutputStream(path);
					out.write(bytes);
					out.flush();
					out.close();
				}
			} catch (Exception e) {
				nlogger.logout(e);
				path = "";
			}
		}
		return getImgUrl(path);
	}

	private String getImgUrl(String imageURL) {
		int i = 0;
		if (imageURL.contains("File//upload")) {
			i = imageURL.toLowerCase().indexOf("file//upload");
			imageURL = "\\" + imageURL.substring(i);
		}
		if (imageURL.contains("File\\upload")) {
			i = imageURL.toLowerCase().indexOf("file\\upload");
			imageURL = "\\" + imageURL.substring(i);
		}
		if (imageURL.contains("File/upload")) {
			i = imageURL.toLowerCase().indexOf("file/upload");
			imageURL = "\\" + imageURL.substring(i);
		}
		return imageURL;
	}

	private String encodeURL(JSONObject object) {
		String url = "";
		if ((object != null) && (object.size() > 0) && (object.containsKey("url"))) {
			url = object.getString("url");
		}

		if (!url.equals("")) {
			url = codec.encodebase64(url);
			url = url.replaceAll("/", "@t").replaceAll("&", "@q").replaceAll("\\+", "@w").replaceAll("=", "@m");
			url = url.replaceAll("\\r\\n", "");
		}
		return url;
	}

	public boolean isExsits(JSONObject object) {
		String fileoldname = "";
		String url = "";
		if ((object != null) && (object.size() > 0)) {
			if (object.containsKey("fileoldname")) {
				fileoldname = object.getString("fileoldname");
			}
			if (object.containsKey("url")) {
				url = object.getString("url");
			}
		}
		JSONObject obj = this.fileModel.find(fileoldname, url);
		return (obj != null) && (obj.size() > 0);
	}

	public String AddFolder(String fileInfo) {
		JSONObject object = JSONHelper.string2json(fileInfo);
		object.put("filetype", Integer.valueOf(0));
		object.put("isdelete", Integer.valueOf(0));
		object.put("ThumbnailImage", this.thumailPath);
		object.put("wbid", currentWeb);
		object.put("userid", userId);
		if (!object.containsKey("fatherid"))
			object.put("fatherid", "0");
		else {
			object.put("fatherid", object.getString("fatherid"));
		}
		return this.fileModel.resultMessage(this.fileModel.add(object));
	}

	public String FileUpdate(String fid, String fileInfo) {
		return this.fileModel
				.resultMessage(JSONHelper.string2json(this.fileModel.update(fid, JSONHelper.string2json(fileInfo))));
	}

	public String RecyCle(String fid) {
		String fileInfo = "{\"isdelete\":1}";
		return this.fileModel.resultmsg(this.fileModel.RecyBatch(fid, JSONHelper.string2json(fileInfo)), "存入回收站成功");
	}

	public String Restore(String fid) {
		String fileInfo = "{\"isdelete\":0}";
		return this.fileModel.resultmsg(this.fileModel.RecyBatch(fid, JSONHelper.string2json(fileInfo)), "从回收站还原文件成功");
	}

	public String FileUpdateBatch(String fids, String folderid) {
		String FileInfo = "{\"fatherid\":\"" + folderid + "\"" + "}";
		return this.fileModel.resultmsg(this.fileModel.updates(fids, JSONHelper.string2json(FileInfo)), "文件移动到文件夹成功");
	}

	public String PageBy(int idx, int pageSize, String fileInfo) {
		return this.fileModel.resultMessage(this.fileModel.page(idx, pageSize, JSONHelper.string2json(fileInfo)));
	}

	public String FindFile(String fileInfo) {
		return this.fileModel.resultMessage(this.fileModel.find(JSONHelper.string2json(fileInfo)));
	}

	public String Delete(String FileInfo) {
		JSONObject object = JSONHelper.string2json(FileInfo);
		return this.fileModel.resultmsg(this.fileModel.delete(object), "操作成功");
	}

	public String BatchDelete(String FileInfo) {
		JSONArray array = JSONHelper.string2array(FileInfo);
		return this.fileModel.resultmsg(this.fileModel.batch(array), "操作成功");
	}

	public String getWord(String fid) {
		JSONObject object = this.fileModel.find(fid);
		String message = "";
		if (object != null) {
			try {
				String hoString = "http://" + getFileIp("file", 0);
				String filepath = object.get("filepath").toString();
				filepath = filepath.replace("\\", "@t");
				message = request.Get(hoString + "/File/FileConvert?sourceFile=" + filepath + "&type=2");
				message = message.replace("gb2312", "utf-8");
			} catch (Exception e) {
				e.printStackTrace();
				message = "";
			}
		}
		return message;
	}

	public String getFile(String fid) {
		JSONObject object = this.fileModel.find(fid);
		return this.fileModel.resultmsg(0, object != null ? object.toString() : "");
	}

	public String getFiles(String fid) {
		JSONObject object = this.fileModel.GetFile(fid);
		return object.toString();
	}

	public String geturl(String fid) {
		String url = "";
		JSONObject object = this.fileModel.find(fid);
		if (object == null) {
			return "";
		}
		if (object.containsKey("filepath")) {
			url = object.get("filepath").toString();
			url = getAppIp("file").split("/")[0] + url;
		}
		return url;
	}

	public String ShowFile() {
		return this.fileModel.Show();
	}

	private String getAppIp(String key) {
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

	private String getFileIp(String key, int sign) {
		String value = "";
		try {
			if ((sign == 0) || (sign == 1))
				value = getAppIp(key).split("/")[sign];
		} catch (Exception e) {
			value = "";
		}
		return value;
	}
}