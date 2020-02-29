package fin;

import fin.callback.*;
import fin.write.FileWriteContent;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Fin {

	private static Fin instance;

	private Fin() { }

	public static Fin getInstance() {
		if (instance == null) {
			synchronized (Fin.class) {
				if (instance == null) instance = new Fin();
			}
		}
		return instance;
	}

	private String projectPath = null;

	public void readFile(String filename, IOStringCallback callback) {
		final String[] str = {null};
		final FileAttrCode[] fileAttrCode = new FileAttrCode[1];
		readFile(filename, (IOBytesCallback) (content, code) -> {
			fileAttrCode[0] = code;
			if (code == FileAttrCode.readSuccess) {
				str[0] = new String(content);
			}
		});

		callback.read(str[0], fileAttrCode[0]);
	}

	public void readFile(String filename, IOBytesCallback callback) {
		File file = new File(filename);

		if (!file.exists()) {
			callback.read(null, FileAttrCode.notExist);
			return ;
		}

		try {
			InputStream stream = new FileInputStream(file);
			int avail = stream.available();
			byte[] bytes = new byte[avail];
			stream.read(bytes);
			callback.read(bytes, FileAttrCode.readSuccess);
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void readFile(String filename, IOJsonCallback callback) {
		final JSONObject[] obj = new JSONObject[1];
		final FileAttrCode[] fileAttrCode = new FileAttrCode[1];
		readFile(filename, (IOStringCallback) (content, code) -> {
			fileAttrCode[0] = code;
			if (code == FileAttrCode.readSuccess) {
				try {
					obj[0] = JSONObject.parseObject(content);
				} catch (JSONException e) {
					fileAttrCode[0] = FileAttrCode.cantConvertJson;
				}
			}
		});

		callback.read(obj[0], fileAttrCode[0]);
	}

	public void readFile(String filename, IOBufferCallback callback) {
		File file = new File(filename);

		if (!file.exists()) {
			callback.read(null, FileAttrCode.notExist);
			return ;
		}

		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			callback.read(reader, FileAttrCode.readSuccess);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void readFile(String filename, IOArrayCallback callback) {
		List<String> result = new ArrayList<>();
		final FileAttrCode[] fileCode = new FileAttrCode[1];

		readFile(filename, (IOBufferCallback) (reader, code) -> {
			if (code == FileAttrCode.readSuccess) {

				try {
					String line;
					while ((line = reader.readLine()) != null) {
						result.add(line + '\n');
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
			fileCode[0] = code;
		});

		callback.read(result, fileCode[0]);
	}

	public String getProjectPath() {
		if (projectPath != null) return projectPath;

		File file = new File("");

		try {
			projectPath = file.getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return projectPath;
	}

	public FileAttrCode rewrite(String filename, String str) {
		if (str == null) return FileAttrCode.writeContentIsNull;

		FileAttrCode attrCode = FileAttrCode.writeSuccess;
		File file = createFile(filename);

		try {
			FileWriter writer = new FileWriter(file);
			writer.write(str);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return attrCode;
	}

	public FileAttrCode rewrite(String filename, byte[] bytes) {
		return rewrite(filename, new String(bytes));
	}

	public FileAttrCode rewrite(String filename, FileWriteContent content) {
		return rewrite(filename, content.constructStr);
	}



	public FileAttrCode appendWrite(String filename, String str) {
		if (str == null) return FileAttrCode.writeContentIsNull;

		FileAttrCode attrCode = FileAttrCode.writeSuccess;
		File file = createFile(filename);

		try {
			FileWriter writer = new FileWriter(file, true);
			writer.write(str);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return attrCode;
	}

	public FileAttrCode appendWrite(String filename, byte[] bytes) {
		return appendWrite(filename, new String(bytes));
	}

	public FileAttrCode appendWrite(String filename, FileWriteContent content) {
		return appendWrite(filename, content.constructStr);
	}

	private File createFile(String filename) {
		File file = new File(filename);

		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				String dir = filename.substring(0, filename.lastIndexOf('/'));
				file = new File(dir);
				file.mkdirs();
				file = new File(filename);
				try {
					file.createNewFile();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}

		return file;
	}
}
