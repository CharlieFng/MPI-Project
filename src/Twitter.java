import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import org.json.simple.*;
import org.json.simple.parser.*;

public class Twitter {
	
	private Map<String, Integer> userMap = new HashMap<String, Integer>();
	private Map<String, Integer> topicMap = new HashMap<String, Integer>();
	private Long wordCounts = (long) 0;
	private String wordToSearch;
	private JSONObject jsonObj;

	public Twitter(String word) {
		this.wordToSearch = word;
	}

	public void readFileByLines(String fileName) {
		File file = new File(fileName);
		BufferedReader reader = null;
		try {
			System.out.println("以行为单位读取文件内容，一次读一整行：");
			reader = new BufferedReader(new FileReader(file));
			String tempString = null;
			int line = 1;
			// 一次读入一行，直到读入null为文件结束
			while ((tempString = reader.readLine()) != null ) {
				// 显示行号
				if (line > 1) {
					System.out.println("line " + line + ": " + tempString);
					jsonObj = getJsonObj(tempString);
					//System.out.println("line" + line + ": "+ getJsonObj(tempString).toString());
					getField(jsonObj);
					countWords(jsonObj, wordToSearch);
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

	public JSONObject getJsonObj(String tempString) {
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

	public void getField(JSONObject jsonObj) {
		JSONObject obj = (JSONObject) jsonObj.get("entities");
		JSONArray userArray = (JSONArray) obj.get("user_mentions");
		JSONArray topicArray = (JSONArray) obj.get("hashtags");
		for (int i = 0; i < userArray.size(); i++) {
			JSONObject tempUser = (JSONObject) userArray.get(i);
			String screen_name = (String) tempUser.get("screen_name");
			if (!userMap.containsKey(screen_name)) {
				userMap.put(screen_name, 1);
			} else {
				int oldValue = userMap.get(screen_name);
				userMap.put(screen_name, oldValue + 1);
			}
		}

		for (int i = 0; i < topicArray.size(); i++) {
			JSONObject tempTopic = (JSONObject) topicArray.get(i);
			String topic_name = (String) tempTopic.get("text");
			if (!topicMap.containsKey(topic_name)) {
				topicMap.put(topic_name, 1);
			} else {
				int oldValue = topicMap.get(topic_name);
				topicMap.put(topic_name, oldValue + 1);
			}
		}
	}

	public void countWords(JSONObject jsonObj, String pattern) {
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

	public void showAll() {
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

	}

	public void showTop10() {
		ArrayList<Map.Entry<String, Integer>> userEntries = sortMap(userMap);
		ArrayList<Map.Entry<String, Integer>> topicEntries = sortMap(topicMap);
		for (int i = 0; i < 10; i++) {
			System.out.println("screen_name: " + userEntries.get(i).getKey()
					+ ", count:" + userEntries.get(i).getValue());
		}
		for (int i = 0; i < 10; i++) {
			System.out.println("topic_name: " + topicEntries.get(i).getKey()
					+ ", count:" + topicEntries.get(i).getValue());
		}

	}

	public ArrayList<Map.Entry<String, Integer>> sortMap(Map map) {
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

		Twitter twitter = new Twitter(args[0]);
		twitter.readFileByLines("/Users/fengsiyu/Documents/miniTwitter.csv");
		twitter.showTop10();
		System.out.println("The given word appears: " + twitter.wordCounts
				+ " times!");

	}
}
