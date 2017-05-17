package Parallel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import mpi.MPI;

public class DiameterMPI {

	static final int NOT_CONNECTED = -1;

	static int[][] distance = null;
	// number of nodes
	static int nodesCount;
	static int edgesCount;
	static int diameter = -1;

	public static void initializeGraph(String fileName) {
		File file = new File(fileName);
		BufferedReader reader = null;
		try {
			// System.out.println("read line one by one：");
			reader = new BufferedReader(new FileReader(file));

			nodesCount = Integer.parseInt(reader.readLine());
			edgesCount = Integer.parseInt(reader.readLine());

			distance = new int[nodesCount + 1][nodesCount + 1];
			initializeMatrix(nodesCount);

			String tempString = null;
			int a, b, c;
			while ((tempString = reader.readLine()) != null) {
				if (!tempString.startsWith("[ \t\n]")) {
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

	private static void initializeMatrix(int num) {
		for (int i = 0; i <= num; ++i) {
			for (int j = 0; j <= num; ++j) {
				distance[i][j] = NOT_CONNECTED;
			}
			distance[i][i] = 0;
		}
	}

	public static int getDiameter() {
		// look for the most distant pair
		for (int i = 1; i <= nodesCount; ++i) {
			for (int j = 1; j <= nodesCount; ++j) {
				if (diameter < distance[i][j]) {
					diameter = distance[i][j];
					// System.out.printf("%d-%d-%d\n", i, diameter, j);
				}
			}
		}

		return diameter;
	}

	public static void main(String[] args) {

		long startTime = System.currentTimeMillis();
		String[] arguments = MPI.Init(args);
		int rank = MPI.COMM_WORLD.Rank();
		int size = MPI.COMM_WORLD.Size();
		int master = 0;

		initializeGraph(arguments[0]);
		int npn = 0;
		int remainder = 0;
		if (size > 1) {
			npn = nodesCount / (size - 1);
			remainder = nodesCount % (size - 1);
		}

		if (size > 1) {
			if (rank == master) {

				Object[] sendbuf = new Object[1];
				Object[] recvbuf = new Object[npn * (size - 1)];

				for (int tag = 1; tag <= nodesCount; tag++) {

					sendbuf[0] = distance;

					for (int i = 1; i < size; i++) {
						MPI.COMM_WORLD.Send(sendbuf, 0, 1, MPI.OBJECT, i, tag);
						// MPI.COMM_WORLD.Bsend(sendbuf, 0, 1, MPI.OBJECT, i,tag);
					}

					if (remainder != 0) {
						int start = (size - 1) * npn + 1;
						for (int i = start; i <= nodesCount; i++) {
							if (distance[i][tag] != NOT_CONNECTED) {
								for (int j = 1; j <= nodesCount; ++j) {
									if (distance[tag][j] != NOT_CONNECTED
											&& (distance[i][j] == NOT_CONNECTED || distance[i][tag]
													+ distance[tag][j] < distance[i][j])) {
										distance[i][j] = distance[i][tag]
												+ distance[tag][j];
									}
								}
							}
						}
					}
					
					for (int i = 1; i < size; i++) {
						MPI.COMM_WORLD.Recv(recvbuf, (i - 1) * npn, npn,
								MPI.OBJECT, i, tag);
					}

					for (int i = 0; i<npn * (size - 1); i++) {
						distance[i + 1] = (int[]) recvbuf[i];
					}
				}

				diameter = getDiameter();
				long endTime = System.currentTimeMillis();

				System.out.printf("Given graph's diameter: %d\n", diameter);
				System.out.println("The program took " + (endTime - startTime)
						+ " ms to complete");
			} else {

				Object[] recvbuf = new Object[1];
				Object[] sendbuf = new Object[npn];
				for (int tag = 1; tag <= nodesCount; tag++) {
					MPI.COMM_WORLD.Recv(recvbuf, 0, 1, MPI.OBJECT, master, tag);
					distance = (int[][]) recvbuf[0];

					int start = (rank - 1) * npn + 1;
					for (int i = start; i < start + npn; i++) {
						if (distance[i][tag] != NOT_CONNECTED) {
							for (int j = 1; j <= nodesCount; ++j) {
								if (distance[tag][j] != NOT_CONNECTED
										&& (distance[i][j] == NOT_CONNECTED || distance[i][tag]
												+ distance[tag][j] < distance[i][j])) {
									distance[i][j] = distance[i][tag]
											+ distance[tag][j];
								}
							}
						}
					}

					for (int i = 0; i < npn; i++) {
						sendbuf[i] = distance[start + i];
					}

					MPI.COMM_WORLD.Send(sendbuf, 0, npn, MPI.OBJECT, master,
							tag);
				}

			}
		} else {

			for (int k = 1; k <= nodesCount; ++k) {
				for (int i = 1; i <= nodesCount; ++i) {
					if (distance[i][k] != NOT_CONNECTED) {
						for (int j = 1; j <= nodesCount; ++j) {
							if (distance[k][j] != NOT_CONNECTED
									&& (distance[i][j] == NOT_CONNECTED || distance[i][k]
											+ distance[k][j] < distance[i][j])) {
								distance[i][j] = distance[i][k]
										+ distance[k][j];
							}
						}
					}
				}
			}

			diameter = getDiameter();
			System.out.printf("%d\n", diameter);

			long endTime = System.currentTimeMillis();
			System.out.println("The program took " + (endTime - startTime)
					+ " ms to complete");
		}

		MPI.Finalize();
	}

}
