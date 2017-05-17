import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import mpi.MPI;
import mpi.Status;

public class TwitterMPI {
	
	private static  Map<String, Integer> userMap = new HashMap<String, Integer>();
	private static Map<String, Integer> topicMap = new HashMap<String, Integer>();
	private static Long wordCounts = (long) 0;
	private String wordToSearch;
	private JSONObject jsonObj;

	
	public static void readFileAndSend(String fileName,String[] sendbuf, int size, int unitSize, int tag){
		File file = new File(fileName);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String tempString = null;
			int line = 0, bufIndex = 0;
			while ((tempString = reader.readLine()) != null) {
				if (line > 0) {
					sendbuf[bufIndex] =  tempString;
					//System.out.println("line " + line + ": " + tempString);
				}
				line++;
				bufIndex++;
				if (bufIndex == unitSize * (size - 1)) {
					for (int i = 1; i < size; i++) {
						MPI.COMM_WORLD.Send(sendbuf, (i - 1) * unitSize,
								unitSize, MPI.OBJECT, i, tag);
					}
					bufIndex = 0;
				}
			}
			if (bufIndex != 0) {
				for (int i = 1; i < size; i++) {
					MPI.COMM_WORLD.Send(sendbuf, (i - 1) * unitSize,
							unitSize, MPI.OBJECT, i, tag);
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
				}
			}
		}
	}
	
	public static JSONObject getJsonObj(String tempString) {
		String[] fields = tempString.split(",", 5);
		String jsonStr = fields[4].replaceAll("\"\"", "\"");
		String json = jsonStr.substring(1, jsonStr.length() - 1);
		JSONParser parser = new JSONParser();
		JSONObject jsonObj = null;
		try {
			jsonObj = (JSONObject) parser.parse(json);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return jsonObj;
	}
	

	public static void getField(JSONObject jsonObj) {
		JSONObject obj = (JSONObject) jsonObj.get("entities");
		JSONArray userArray = (JSONArray) obj.get("user_mentions");
		JSONArray topicArray = (JSONArray) obj.get("hashtags");
		for (int i = 0; i < userArray.size(); i++) {
			JSONObject tempUser = (JSONObject) userArray.get(i);
			String screen_name = (String) tempUser.get("screen_name");
			if (!userMap.containsKey(screen_name)) {
				userMap.put(screen_name, 1);
			} else {
				int oldValue = (Integer) userMap.get(screen_name);
				userMap.put(screen_name, oldValue + 1);
			}
		}

		for (int i = 0; i < topicArray.size(); i++) {
			JSONObject tempTopic = (JSONObject) topicArray.get(i);
			String topic_name = (String) tempTopic.get("text");
			if (!topicMap.containsKey(topic_name)) {
				topicMap.put(topic_name, 1);
			} else {
				int oldValue = (Integer) topicMap.get(topic_name);
				topicMap.put(topic_name, oldValue + 1);
			}
		}
	}

	public static void countWords(JSONObject jsonObj, String pattern){
		String text = "";
		text = jsonObj.get("text").toString();
		System.out.println(text);

		String[] words = text.replaceAll("[;,\\(\\)\\{\\}!\\?\\.\\*\\+]", " ").split(" ");
		for (int i = 0; i < words.length; i++) {
			if (words[i].toLowerCase().equals(pattern.toLowerCase())) {
				wordCounts++;
			}
		}
	}
	
	public static void main(String[] args) {
		

		//String[] temps = MPI.Init(args);
		String pattern = MPI.Init(args)[0];
		String fileUrl = MPI.Init(args)[1];
		int rank = MPI.COMM_WORLD.Rank();
		int size = MPI.COMM_WORLD.Size();
		int unitSize = 10, tag = 200, master = 0;

		if (rank == master) {
			
			
			String sendbuf[] = new String[unitSize * (size - 1)];
			/*
			File file = new File(fileUrl);
			BufferedReader reader = null;
			try {
				System.out.println("以行为单位读取文件内容，一次读一整行：");
				reader = new BufferedReader(new FileReader(file));
				String tempString = null;
				int line = 0, bufIndex = 0;
				// 一次读入一行，直到读入null为文件结束
				while ((tempString = reader.readLine()) != null) {
					// 显示行号
					if (line > 0) {
						sendbuf[bufIndex] =  tempString;
						//System.out.println("line " + line + ": " + tempString);
					}
					line++;
					bufIndex++;
					if (bufIndex == unitSize * (size - 1)) {
						for (int i = 1; i < size; i++) {
							MPI.COMM_WORLD.Send(sendbuf, (i - 1) * unitSize,
									unitSize, MPI.OBJECT, i, tag);
						}
						bufIndex = 0;
					}
				}
				if (bufIndex != 0) {
					for (int i = 1; i < size; i++) {
						MPI.COMM_WORLD.Send(sendbuf, (i - 1) * unitSize,
								unitSize, MPI.OBJECT, i, tag);
					}
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e1) {
					}
				}
			}
			*/
			
			readFileAndSend(fileUrl,sendbuf, size,unitSize,tag);
			
		} else {
			String[] recvbuf = new String[unitSize];
			while (MPI.COMM_WORLD.Probe(master, 200) != null) {
				MPI.COMM_WORLD.Recv(recvbuf, 0, unitSize, MPI.OBJECT, master,
						tag);
				for (int i = 0; i < recvbuf.length; i++) {
					JSONObject jsonObj = getJsonObj(recvbuf[i]);
					getField(jsonObj);
					countWords(jsonObj,pattern);
					System.out.println("I'm rank " + rank
							+ ", Recive string from master :" + recvbuf[i]);
				}
			}
		}

		MPI.Finalize();

	}

}
