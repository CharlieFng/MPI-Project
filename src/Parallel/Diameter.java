package Parallel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Diameter {

	static final int MAX = 10000;
	static final int NOT_CONNECTED = -1;

	static int[][] distance = new int[MAX][MAX];
	// number of nodes
	static int nodesCount;
	static int edgesCount;
	static int diameter = -1;
	
	public static void initializeGraph(String fileName) {
		File file = new File(fileName);
		BufferedReader reader = null;
		try {
			System.out.println("read line one by one：");
			reader = new BufferedReader(new FileReader(file));
		
			
			nodesCount = Integer.parseInt(reader.readLine());
			edgesCount = Integer.parseInt(reader.readLine());
			String tempString = null;
			int a, b, c;
			while ((tempString = reader.readLine()) != null ) {
				if(!tempString.startsWith("[ \t\n]")){
					String[] strArray = tempString.trim().split("-");
					a = Integer.parseInt(strArray[0]);
					c = Integer.parseInt(strArray[1]);
					b = Integer.parseInt(strArray[2]);
					distance[a][b] = c;
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
	
	public static void main(String[] args) {

		long startTime = System.currentTimeMillis();
		for (int i = 0; i < MAX; ++i) {
			for (int j = 0; j < MAX; ++j) {
				distance[i][j] = NOT_CONNECTED;
			}
			distance[i][i] = 0;
		}

		initializeGraph(args[0]);

		// Floyd-Warshall
		for (int k = 1; k <= nodesCount; ++k) {
			for (int i = 1; i <= nodesCount; ++i) {
				if (distance[i][k] != NOT_CONNECTED) {
					for (int j = 1; j <= nodesCount; ++j) {
						if (distance[k][j] != NOT_CONNECTED
								&& (distance[i][j] == NOT_CONNECTED || distance[i][k]
										+ distance[k][j] < distance[i][j])) {
							distance[i][j] = distance[i][k] + distance[k][j];
						}
					}
				}
			}
		}

		// look for the most distant pair
		for (int i = 1; i <= nodesCount; ++i) {
			for (int j = 1; j <= nodesCount; ++j) {
				if (diameter < distance[i][j]) {
					diameter = distance[i][j];
					//System.out.printf("%d-%d-%d\n", i, diameter, j);
				}
			}
		}

		long endTime = System.currentTimeMillis();
		System.out.printf("%d\n", diameter);
		System.out.println("The program took " + (endTime - startTime)
				+ " ms to complete");

	}

}
