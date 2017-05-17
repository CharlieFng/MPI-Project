import mpi.MPI;


public class ToyExample {

	public static void main(String[] args) throws Exception {
		
		MPI.Init(args);
		int rank = MPI.COMM_WORLD.Rank();
		int size = MPI.COMM_WORLD.Size();
		int unitSize=4, tag=100, master=0;
		
		if(rank == master) {
			String sendbuf[] = new String[unitSize*(size-1)];
			
			for(int i=0;i<unitSize*(size-1);i++){
				sendbuf[i] = "TaoShuang";
			}
			
			for(int i=1;i<size;i++){
				MPI.COMM_WORLD.Send(sendbuf,(i-1)*unitSize,unitSize,MPI.OBJECT,i,tag);
			}
			
			for(int i=1;i<size;i++){
				MPI.COMM_WORLD.Recv(sendbuf, (i-1)*unitSize, unitSize, MPI.OBJECT, i, tag);
			}
			
			for(int i=0;i<unitSize*(size-1);i++){
				System.out.print(sendbuf[i]+ " ");
				
			}
			
		}else{
			
			String recvbuf[] = new String[unitSize];
			
			MPI.COMM_WORLD.Recv(recvbuf, 0, unitSize, MPI.OBJECT, master,tag);
			
			for(int i=0;i<recvbuf.length;i++){
				System.out.println("Recive string from master :" + recvbuf[i]);
			}
			
			for(int i=0;i<unitSize;i++){
				recvbuf[i] = recvbuf[i] + "from " + rank;
			}
			
			
			
			MPI.COMM_WORLD.Send(recvbuf, 0, unitSize, MPI.OBJECT, master, tag);
		}
		
		MPI.Finalize();
		
	}

}
