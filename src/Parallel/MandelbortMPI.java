package Parallel;

import java.util.HashMap;
import java.util.Map;

import mpi.MPI;

public class MandelbortMPI {

	
	private static int mandelbrotSetCount(double real_lower,
			double real_upper, double img_lower, double img_upper, int num,
			int maxiter) {
		int count=0;
		double real_step = (real_upper-real_lower)/num;
		double img_step = (img_upper-img_lower)/num;
		
		for(int real=0; real<=num; real++){
			for(int img=0; img<=num; img++){
				count+=inset(real_lower+real*real_step,img_lower+img*img_step,maxiter);
			}
		}
		return count;
	}
	
	private static int inset(double real, double img, int maxiter) {
		double z_real = real;
		double z_img = img;
		for(int iters = 0; iters < maxiter; iters++){
			double z2_real = z_real*z_real-z_img*z_img;
			double z2_img = 2.0*z_real*z_img;
			z_real = z2_real + real;
			z_img = z2_img + img;
			if(z_real*z_real + z_img*z_img > 4.0) return 0;
		}
		return 1;
	}
	
	public static void main(String[] args) {
		
		long startTime = System.currentTimeMillis();

		String[] arguments = MPI.Init(args);
		int rank = MPI.COMM_WORLD.Rank();
		int size = MPI.COMM_WORLD.Size();
		int tag = 200, master = 0;
		
		double real_lower;
		double real_upper;
		double img_lower;
		double img_upper;
		int num;
		int maxiter;
		
		int num_regions = arguments.length/6;
		
		int mrgs = 0;
		
		int rgs = num_regions/size;
		int remainder = num_regions%size;
		
		if(rgs<1){
			System.out.println("The number of regions must be greater than the number of nodes");
			System.exit(-1);
		}else{
			if (remainder == 0) {
				mrgs = num_regions / size;
			} else {
				mrgs = num_regions / size + remainder;
			}
		}
		
		if (size > 1) {
			// master calculate more regions if remainder not equal 0, other nodes process the 
			//same number of regions
			if (rank == master) {
				
				int[] recvbuf = new int[num_regions];

				for(int i=0;i<mrgs;i++){
					// scan the arguments
					int regionNo = rgs*rank+i;
					real_lower = Double.parseDouble(arguments[regionNo*6]);
					real_upper = Double.parseDouble(arguments[regionNo*6+1]);
					img_lower =  Double.parseDouble(arguments[regionNo*6+2]);
					img_upper =  Double.parseDouble(arguments[regionNo*6+3]);
					num = Integer.parseInt((arguments[regionNo*6+4]));
					maxiter = Integer.parseInt((arguments[regionNo*6+5]));
			
					recvbuf[regionNo] = mandelbrotSetCount(real_lower,real_upper,img_lower,img_upper,num,maxiter);
					
				}
				
				for (int i = 1; i < size; i++) {
					MPI.COMM_WORLD.Recv(recvbuf, i* rgs+remainder, rgs, MPI.INT, i,
							tag);
				}
				
				for(int i=0; i<num_regions; i++){
					System.out.println("regionNo "+ i + ": " +recvbuf[i]);
				}
				
				long endTime = System.currentTimeMillis();
				
				System.out.println("The program took "
						+ (endTime - startTime) + " ms to complete");
			} else {
				int[] sendbuf = new int[rgs];
				
				for(int i=0;i<rgs;i++){
					// scan the arguments
					int regionNo = rgs*rank+i+remainder;
					real_lower = Double.parseDouble(arguments[regionNo*6]);
					real_upper = Double.parseDouble(arguments[regionNo*6+1]);
					img_lower =  Double.parseDouble(arguments[regionNo*6+2]);
					img_upper =  Double.parseDouble(arguments[regionNo*6+3]);
					num = Integer.parseInt((arguments[regionNo*6+4]));
					maxiter = Integer.parseInt((arguments[regionNo*6+5]));
			
					sendbuf[i] = mandelbrotSetCount(real_lower,real_upper,img_lower,img_upper,num,maxiter);
					
				}

				MPI.COMM_WORLD.Send(sendbuf, 0, rgs, MPI.INT, master, tag);
			}
		}else{
			
			for(int regionNo=0;regionNo<num_regions;regionNo++){
				real_lower = Double.parseDouble(arguments[regionNo*6]);
				real_upper = Double.parseDouble(arguments[regionNo*6+1]);
				img_lower =  Double.parseDouble(arguments[regionNo*6+2]);
				img_upper =  Double.parseDouble(arguments[regionNo*6+3]);
				num = Integer.parseInt((arguments[regionNo*6+4]));
				maxiter = Integer.parseInt((arguments[regionNo*6+5]));
		
				System.out.println("regionNo "+ regionNo + ": " +mandelbrotSetCount(real_lower,real_upper,img_lower,img_upper,num,maxiter));
				
			}
			
			long endTime = System.currentTimeMillis();
			
			System.out.println("The program took "
					+ (endTime - startTime)
					+ " ms to complete");
			
		}

		MPI.Finalize();

	}

}
