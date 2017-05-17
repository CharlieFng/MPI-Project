package Parallel;

public class Test {


	public static void main(String[] args) {
		int[][] distance = new int[2][2];
		distance[0][0] = 1;
		distance[0][1] = 2;
		distance[1][0] = 3;
		distance[1][1] = 4;
		
		for(int i : distance[0]){
			System.out.println(i);
		}
		
	}

}
