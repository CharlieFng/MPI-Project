#include <cstdio>
#include <cstdlib>
#include <omp.h>
#include <mpi.h>
#include <math.h>


int inset(double real, double img, int maxiter){
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

// count the number of points in the set, within the region
int mandelbrotSetCount(double real_lower, double real_upper, double img_lower, double img_upper, int num, int maxiter){

	int count=0;
	double real_step = (real_upper-real_lower)/num;
	double img_step = (img_upper-img_lower)/num;

		#pragma omp parallel for reduction (+:count), schedule(dynamic), collapse(2)
		for(int real=0; real<=num; real++){
			for(int img=0; img<=num; img++){
				count+=inset(real_lower+real*real_step,img_lower+img*img_step,maxiter);
			}
		}
	return count;
}

// main
int main(int argc, char *argv[]){

	MPI_Init(&argc, &argv);
	int world_size;
	MPI_Comm_size(MPI_COMM_WORLD, &world_size);
	int world_rank;
	MPI_Comm_rank(MPI_COMM_WORLD,&world_rank);

	double tStart = MPI_Wtime();

	double real_lower;
	double real_upper;
	double img_lower;
	double img_upper;
	int num;
	int maxiter;
	int num_regions = (argc-1)/6;
	int self_num_regions = ceil(double(num_regions)/double(world_size));
	int mandel_result[self_num_regions];

	const int MASTER = world_size - 1;

	for(int i=0;i<self_num_regions;i++){
		mandel_result[i] = -1;
	}

	int self_start_pos = world_rank  * self_num_regions;
	int self_end_pos = (world_rank + 1) * self_num_regions;

	if(self_start_pos<num_regions){
		if(self_end_pos>=num_regions){
			self_end_pos = num_regions;
		}
		for(int region = self_start_pos;region<self_end_pos;region++){
			// scan the arguments
			sscanf(argv[region*6+1],"%lf",&real_lower);
			sscanf(argv[region*6+2],"%lf",&real_upper);
			sscanf(argv[region*6+3],"%lf",&img_lower);
			sscanf(argv[region*6+4],"%lf",&img_upper);
			sscanf(argv[region*6+5],"%i",&num);
			sscanf(argv[region*6+6],"%i",&maxiter);
			mandel_result[region-self_start_pos] = mandelbrotSetCount(real_lower,real_upper,img_lower,img_upper,num,maxiter);
		}
	}

	if (world_rank != MASTER) {
		 MPI_Send(&mandel_result, self_num_regions, MPI_INT, MASTER, 0, MPI_COMM_WORLD);

	} else{
		int result[self_num_regions];

		for(int i = 0; i < MASTER; i++){
			MPI_Recv(&result, self_num_regions, MPI_INT, i, 0, MPI_COMM_WORLD,
	             MPI_STATUS_IGNORE);
			for(int i = 0; i < self_num_regions; i++){
				if(result[i] == -1){
					break;
				}
				printf("%d\n", result[i]);
			}
		}
		for(int j = 0; j < self_num_regions; j++){
			if(mandel_result[j] == -1){
				break;
			}
			printf("%d\n", mandel_result[j]);
		}
		
		double tEnd = MPI_Wtime();
		printf("Execution Time is %.3fs.\n", tEnd - tStart);
	}

	MPI_Finalize();
	return EXIT_SUCCESS;
}
