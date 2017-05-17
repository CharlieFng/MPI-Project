package Parallel;

public class Mandelbrot {

	
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
		
		double real_lower;
		double real_upper;
		double img_lower;
		double img_upper;
		int num;
		int maxiter;
		int num_regions = args.length/6;
		for(int region=0;region<num_regions;region++){
			// scan the arguments
			real_lower = Double.parseDouble(args[region*6]);
			real_upper = Double.parseDouble(args[region*6+1]);
			img_lower =  Double.parseDouble(args[region*6+2]);
			img_upper =  Double.parseDouble(args[region*6+3]);
			num = Integer.parseInt((args[region*6+4]));
			maxiter = Integer.parseInt((args[region*6+5]));
	
			System.out.println("region "+ region + ": " +mandelbrotSetCount(real_lower,real_upper,img_lower,img_upper,num,maxiter));
			
		}
		
		long endTime = System.currentTimeMillis();
		
		System.out.println("The program took "
				+ (endTime - startTime)
				+ " ms to complete");

	}



}
