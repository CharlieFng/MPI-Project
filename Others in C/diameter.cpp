#include <cstdio>
#include <cstdlib>
#include <omp.h>
#include <mpi.h>
#include <math.h>


#define NOT_CONNECTED -1




int main(int argc, char *argv[]){

    double tStart = MPI_Wtime();
    MPI_Init(&argc, &argv);
    int size;
    int rank;
    MPI_Comm_size(MPI_COMM_WORLD, &size);
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    int master=0;
    int g_ncore = omp_get_num_procs();
    int nodesCount;
    int edgesCount;


    if(size>1){
        if(rank == master) {

            scanf("%d", &nodesCount);
            //edges count
            scanf("%d", &edgesCount);

            //initializeMatrix(nodesCount);
            int distance[nodesCount*nodesCount];
            int diameter = -1;

            for (int i = 1; i <= nodesCount; ++i) {
                for (int j = 1; j <= nodesCount; ++j) {
                    distance[(i-1)*nodesCount+(j-1)] = NOT_CONNECTED;
                }
                distance[(i-1)*nodesCount+(i-1)] = 0;
            }

            while(edgesCount--){
                //nodes - let the indexation begin from 1
                int a, b;
                //edge weight
                int c;
                scanf("%d-%d-%d", &a, &c, &b);
                distance[(a-1)*nodesCount+(b-1)]=c;
            }

            int mpn = 0;
            int npn = 0;
            int remain = 0;

            if (size > 1) {
                npn = nodesCount / size;
                remain = nodesCount % size;
            }

            mpn = npn + remain;
            

            int bufflen = npn*nodesCount;
            int updatelen = npn + nodesCount;

            int sendbuf[3];
            sendbuf[0] = nodesCount;
            sendbuf[1] = npn;
            sendbuf[2] = remain;

            for (int i = 1; i < size; i++) {
                MPI_Send(sendbuf, 3, MPI_INT, i, 0, MPI_COMM_WORLD);
            }

            for (int tag = 1; tag <= nodesCount; tag++) {

                int recvbuf[bufflen];
                int sendbuff[updatelen];


                if(tag == 1){

                    for (int i = 1; i < size; i++) {
                        MPI_Send(distance, nodesCount*nodesCount, MPI_INT, i, tag, MPI_COMM_WORLD);
                    }

                }else{

                    for (int i = 1; i < size; i++) {

                        int start = i * npn + remain + 1;
                        for(int i=0;i<npn;i++){
                            sendbuff[i] = distance[(start+i-1)*nodesCount+(tag-1)];
                        }

                        for(int j=1;j<=nodesCount;j++){
                            sendbuff[npn+j-1] = distance[(tag-1)*nodesCount+(j-1)];
                        }

                        MPI_Send(sendbuff, updatelen, MPI_INT, i, tag, MPI_COMM_WORLD);
                    }

                }
                
                for (int i = 1; i <= mpn; i++) {
                    if (distance[(i-1)*nodesCount+(tag-1)] != NOT_CONNECTED) {
                        #pragma omp parallel for num_threads(g_ncore) schedule(dynamic)
                        for (int j = 1; j <= nodesCount; ++j) {
                            if (distance[(tag-1)*nodesCount+(j-1)] != NOT_CONNECTED
                                    && (distance[(i-1)*nodesCount+(j-1)] == NOT_CONNECTED || distance[(i-1)*nodesCount+(tag-1)]
                                        + distance[(tag-1)*nodesCount+(j-1)] < distance[(i-1)*nodesCount+(j-1)])) {
                                distance[(i-1)*nodesCount+(j-1)] = distance[(i-1)*nodesCount+(tag-1)]
                                            + distance[(tag-1)*nodesCount+(j-1)];
                            }
                        }
                    }
                }
                

                for (int i = 1; i < size; i++) {

                    MPI_Recv(recvbuf,bufflen, MPI_INT, i, tag, MPI_COMM_WORLD,
                        MPI_STATUS_IGNORE);

                    int cc = (i * npn + remain ) * nodesCount;
                    for(int j=0; j<bufflen;j++){
                        distance[cc+j] = recvbuf[j];
                    }
                }

                //printf("master finish round %d\n", tag);

            }

            //diameter = getDiameter();
            for (int i = 1; i <= nodesCount; ++i) {
                for (int j = 1; j <= nodesCount; ++j) {
                    if (diameter < distance[(i-1)*nodesCount+(j-1)]) {
                        diameter = distance[(i-1)*nodesCount+(j-1)];
                   
                    }
                }
            }

            double tEnd = MPI_Wtime();
            printf("The diameter is %d\n", diameter);
            printf("Execution Time is %.3fs.\n", tEnd - tStart);

        }else if(rank > 0){ 


            int recvbuf[3];
            MPI_Recv(recvbuf,3, MPI_INT, master, 0, MPI_COMM_WORLD,
                        MPI_STATUS_IGNORE);

            nodesCount = recvbuf[0];
            int npn = recvbuf[1];
            int remain = recvbuf[2];

            int bufflen = npn*nodesCount;
            int updatelen = npn + nodesCount;

            int distance[nodesCount*nodesCount];
            

            for (int tag = 1; tag <= nodesCount; tag++) {

                //printf("rank %d, start round %d\n", rank, tag);

                int sendbuf[bufflen];
                int recvbuff[updatelen];

                int start = rank * npn + remain + 1;

                if(tag == 1){
                    MPI_Recv(distance,nodesCount*nodesCount, MPI_INT, master, tag, MPI_COMM_WORLD,
                        MPI_STATUS_IGNORE);
                }else{
                    MPI_Recv(recvbuff,updatelen, MPI_INT, master, tag, MPI_COMM_WORLD,
                        MPI_STATUS_IGNORE);
                
                    
                    for(int i=0;i<npn;i++){

                        distance[(start+i-1)*nodesCount+(tag-1)]= recvbuff[i];
                    }

                    for(int j=1;j<=nodesCount;j++){

                        distance[(tag-1)*nodesCount+(j-1)] = recvbuff[npn+j-1];
                    }
                }

                
                for (int i = start; i < start + npn; i++) {
                    if (distance[(i-1)*nodesCount+(tag-1)] != NOT_CONNECTED) {
                        #pragma omp parallel for num_threads(g_ncore) schedule(dynamic)
                        for (int j = 1; j <= nodesCount; ++j) {
                            if (distance[(tag-1)*nodesCount+(j-1)] != NOT_CONNECTED
                                    && (distance[(i-1)*nodesCount+(j-1)] == NOT_CONNECTED || distance[(i-1)*nodesCount+(tag-1)]
                                                + distance[(tag-1)*nodesCount+(j-1)] < distance[(i-1)*nodesCount+(j-1)])) {
                                distance[(i-1)*nodesCount+(j-1)] = distance[(i-1)*nodesCount+(tag-1)]
                                            + distance[(tag-1)*nodesCount+(j-1)];
                            }
                        }
                    }
                }

                int cc = (rank * npn + remain ) * nodesCount;
                for(int j=0; j<bufflen;j++){
                      sendbuf[j] = distance[cc+j];
                }

                MPI_Send(sendbuf, bufflen, MPI_INT, master, tag, MPI_COMM_WORLD);

                //printf("rank %d, finish round %d\n", rank, tag);
            }
        }
    }else{


        scanf("%d", &nodesCount);
            //edges count
        scanf("%d", &edgesCount);

           
        int distance[nodesCount*nodesCount];
        int diameter = -1;

        for (int i = 1; i <= nodesCount; ++i) {
            for (int j = 1; j <= nodesCount; ++j) {
                distance[(i-1)*nodesCount+(j-1)] = NOT_CONNECTED;
            }
            distance[(i-1)*nodesCount+(i-1)] = 0;
        }

        while(edgesCount--){
            //nodes - let the indexation begin from 1
            int a, b;
            //edge weight
            int c;
            scanf("%d-%d-%d", &a, &c, &b);
            distance[(a-1)*nodesCount+(b-1)]=c;
        }

        for(int k=1;k<=nodesCount;++k){
            for (int i=1;i<=nodesCount;i++) {
                if (distance[(i-1)*nodesCount+(k-1)] != NOT_CONNECTED) {
                    #pragma omp parallel for num_threads(g_ncore) schedule(dynamic)
                    for (int j=1;j<=nodesCount; ++j) {
                        if(distance[(k-1)*nodesCount+(j-1)] != NOT_CONNECTED
                                && (distance[(i-1)*nodesCount+(j-1)] == NOT_CONNECTED || distance[(i-1)*nodesCount+(k-1)]
                                        + distance[(k-1)*nodesCount+(j-1)] < distance[(i-1)*nodesCount+(j-1)])) {
                            distance[(i-1)*nodesCount+(j-1)] = distance[(i-1)*nodesCount+(k-1)]
                                        + distance[(k-1)*nodesCount+(j-1)];
                        }
                    }
                }
            }
        }
        //diameter = getDiameter();
        for (int i = 1; i <= nodesCount; ++i) {
            for (int j = 1; j <= nodesCount; ++j) {
                if (diameter < distance[(i-1)*nodesCount+(j-1)]) {
                        diameter = distance[(i-1)*nodesCount+(j-1)];
                   
                }
            }
        }
        double tEnd = MPI_Wtime();
        printf("%d\n", diameter);
        printf("Execution Time is %.3fs.\n", tEnd - tStart);
    }

    MPI_Finalize();
    return EXIT_SUCCESS;
}