/**
 * Siyu-Feng-745399
 */
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import mpi.MPI;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class TwitterMPIChange {

	private static Map<String, Integer> userMap = new HashMap<String, Integer>();
	private static Map<String, Integer> topicMap = new HashMap<String, Integer>();
	private static int wordCounts = 0;
	private static JSONObject jsonObj;

	public static void readFileOnSpecificLines(String fileName, String pattern,
			int startLine, int endLine, int rank) {
		File file = new File(fileName);
		BufferedReader reader = null;
		int line = 0;
		try {
			reader = new BufferedReader(new FileReader(file));
			String tempString = null;
			while ((tempString = reader.readLine()) != null) {
				if (line == endLine + 1)
					break;
				if (line > startLine - 1 && line < endLine + 1) {
					jsonObj = getJsonObj(tempString.toString());
					getField(jsonObj);
					countWordsOnWholeFile(jsonObj, pattern);
					// System.out.println("rank :" + rank + ", line :"+ line +
					// "-----" + tempString);
				}
				line++;
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

	public static void readFileAndCompute(String fileName, String pattern) {
		File file = new File(fileName);
		BufferedReader reader = null;
		String tempString = null;
		int line = 0;
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(file), "UTF-8"));
			while ((tempString = reader.readLine()) != null) {
				if (line > 0) {
					jsonObj = getJsonObj(tempString.toString());
					getField(jsonObj);
					countWordsOnWholeFile(jsonObj, pattern);
					// System.out.println("rank :" + rank + ", line :"+ line +
					// "-----" + tempString);
				}
				line++;
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

	public static int readFileByLines(String fileName) {
		File file = new File(fileName);
		BufferedReader reader = null;
		String tempString = null;
		int line = 0;
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(file), "UTF-8"));
			while ((tempString = reader.readLine()) != null) {
				// System.out.println("line :"+ line+ tempString);
				line++;
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
		return line;
	}

	public static void readFileAndSend(String fileName, String[] sendbuf,
			int size, int unitSize, int tag) {
		File file = new File(fileName);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String tempString = null;
			int line = 0, bufIndex = 0;
			// 一次读入一行，直到读入null为文件结束
			while ((tempString = reader.readLine()) != null) {
				// 显示行号
				if (line > 0) {
					sendbuf[bufIndex] = tempString;
					// System.out.println("line " + line + ": " + tempString);
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
					MPI.COMM_WORLD.Send(sendbuf, (i - 1) * unitSize, unitSize,
							MPI.OBJECT, i, tag);
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
		// System.out.println(jsonStr);
		String json = jsonStr.substring(1, jsonStr.length() - 1);
		// System.out.println(json);
		if (!json.endsWith("\"}")) {
			json += "\"}}}";
		}
		JSONParser parser = new JSONParser();
		JSONObject jsonObj = null;
		try {
			jsonObj = (JSONObject) parser.parse(json);
			// System.out.println(jsonObj.toString());
		} catch (ParseException e) {
			System.out.println(tempString);
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

	public static void countWordsOnWholeFile(JSONObject jsonObj, String pattern) {
		String wholeText = jsonObj.toString();
		String[] words = wholeText.replaceAll(
				"[;,\\(\\)\\{\\}!:\\?\\.\\*\\+\"\']", " ").split(" ");
		for (int i = 0; i < words.length; i++) {
			if (words[i].toLowerCase().equals(pattern.toLowerCase())) {
				wordCounts++;
			}
		}
	}

	public static void countWords(JSONObject jsonObj, String pattern) {
		String text = "";
		text = jsonObj.get("text").toString();
		// System.out.println(text);

		String[] words = text.replaceAll("[;,\\(\\)\\{\\}!\\?\\.\\*\\+]", " ")
				.split(" ");
		for (int i = 0; i < words.length; i++) {
			if (words[i].toLowerCase().equals(pattern.toLowerCase())) {
				wordCounts++;
			}
		}
	}

	public static Map<String, Integer> mergeResult(Map<String, Integer>... maps) {

		Map<String, Integer> result = new HashMap<String, Integer>();
		for (int i = 0; i < maps.length; i++) {
			Map temp = maps[i];
			Iterator iterator = temp.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, Integer> entry = (Entry<String, Integer>) iterator
						.next();
				if (result.containsKey(entry.getKey())) {
					result.put(entry.getKey(), result.get(entry.getKey())
							+ entry.getValue());
				} else {
					result.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return result;
	}

	public static void showAll(int rank) {
		System.out.println("From rank :" + rank);
		System.out
				.println("------------------------------------------------------------");
		Iterator userIt = userMap.entrySet().iterator();
		while (userIt.hasNext()) {
			Map.Entry<String, Integer> entry = (Entry<String, Integer>) userIt
					.next();
			System.out.println("screen_name: " + entry.getKey() + ", count:"
					+ entry.getValue());
		}

		Iterator topicIt = topicMap.entrySet().iterator();
		while (topicIt.hasNext()) {
			Map.Entry<String, Integer> entry = (Entry<String, Integer>) topicIt
					.next();
			System.out.println("topic_name: " + entry.getKey() + ", count:"
					+ entry.getValue());
		}
		System.out
				.println("------------------------------------------------------------");
	}

	public static void showTop10() {
		System.out
				.println("==============================================================");
		ArrayList<Map.Entry<String, Integer>> userEntries = sortMap(userMap);
		ArrayList<Map.Entry<String, Integer>> topicEntries = sortMap(topicMap);
		System.out.println("Top 10 users mentioned: ");
		for (int i = 0; i < 10; i++) {
			System.out.println("screen_name: " + userEntries.get(i).getKey()
					+ ", count:" + userEntries.get(i).getValue());
		}
		System.out.println();

		System.out.println("Top 10 topic mentioned: ");
		for (int i = 0; i < 10; i++) {
			System.out.println("topic_name: " + topicEntries.get(i).getKey()
					+ ", count:" + topicEntries.get(i).getValue());
		}
		System.out.println();

	}

	public static ArrayList<Map.Entry<String, Integer>> sortMap(Map map) {
		List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(
				map.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> obj1,
					Map.Entry<String, Integer> obj2) {
				return obj2.getValue() - obj1.getValue();
			}
		});
		return (ArrayList<Entry<String, Integer>>) entries;
	}

	public static void main(String[] args) {

		long startTime = System.currentTimeMillis();

		String[] arguments = MPI.Init(args);
		int rank = MPI.COMM_WORLD.Rank();
		int size = MPI.COMM_WORLD.Size();
		int unitSize = 10, tag = 200, master = 0;

		if (size > 1) {
			if (rank == master) {

				int[] sendbuf = new int[2];
				Object[] recvbuf = new Object[3 * (size - 1)];

				int totalLines = readFileByLines(arguments[1]);
				int unitLines = totalLines / (size - 1);
				sendbuf[0] = unitLines;
				sendbuf[1] = totalLines;
				for (int i = 1; i < size; i++) {
					MPI.COMM_WORLD.Send(sendbuf, 0, 2, MPI.INT, i, tag);
				}
				for (int i = 1; i < size; i++) {
					MPI.COMM_WORLD.Recv(recvbuf, (i - 1) * 3, 3, MPI.OBJECT, i,
							tag);
				}

				/*
				 * userMap = (Map) recvbuf[3]; topicMap = (Map) recvbuf[4];
				 * Map<String,Integer> wordWrapper = (Map)recvbuf[2];
				 * showAll(master);
				 */

				Map[] userMaps = new Map[size - 1];
				Map[] topicMaps = new Map[size - 1];
				Map[] wordWrappers = new Map[size - 1];

				for (int i = 0; i < size - 1; i++) {
					userMaps[i] = (Map) recvbuf[i * 3];
				}

				for (int j = 0; j < size - 1; j++) {
					topicMaps[j] = (Map) recvbuf[j * 3 + 1];
				}

				for (int i = 0; i < size - 1; i++) {
					wordWrappers[i] = (Map) recvbuf[i * 3 + 2];
				}

				userMap = mergeResult(userMaps);
				topicMap = mergeResult(topicMaps);

				for (int i = 0; i < size - 1; i++) {
					wordCounts += (Integer) wordWrappers[i].get(arguments[0]);
				}

				showTop10();

				System.out.println();
				System.out.println("Given word \"" + arguments[0]
						+ "\" appears: " + wordCounts + " times!");
				System.out.println();
				System.out.println("The program took "
						+ (System.currentTimeMillis() - startTime)
						+ " ms to complete");
			} else {

				Map<String, Integer> wordWrapper = new HashMap<String, Integer>();
				int[] recvbuf = new int[2];
				Object[] sendbuf = new Object[3];
				MPI.COMM_WORLD.Recv(recvbuf, 0, 2, MPI.INT, master, tag);
				int startLine, endLine;
				startLine = (rank - 1) * recvbuf[0] + 1;
				if (rank == size - 1) {
					endLine = recvbuf[1];
				} else {
					endLine = rank * recvbuf[0];
				}
				System.out.println("I'm Rank " + rank
						+ " start read from line " + startLine + " end in "
						+ endLine);
				readFileOnSpecificLines(arguments[1], arguments[0], startLine,
						endLine, rank);
				// showAll(rank);
				// System.out.println("In rank: " + rank +
				// ",given word appears: "
				// + wordCounts + " times!");

				sendbuf[0] = userMap;
				sendbuf[1] = topicMap;
				wordWrapper.put(arguments[0], wordCounts);
				sendbuf[2] = wordWrapper;
				MPI.COMM_WORLD.Send(sendbuf, 0, 3, MPI.OBJECT, master, tag);
			}
		}else{
			
			readFileAndCompute(arguments[1],arguments[0]);
			showTop10();

			System.out.println();
			System.out.println("Given word \"" + arguments[0]
					+ "\" appears: " + wordCounts + " times!");
			System.out.println();
			System.out.println("The program took "
					+ (System.currentTimeMillis() - startTime)
					+ " ms to complete");
		}

		MPI.Finalize();
		

	}

}
