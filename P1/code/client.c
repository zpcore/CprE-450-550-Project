#include <stdio.h>
#include <string.h>
#include <time.h>
#include <sys/types.h>
#include <rpc/rpc.h>
#include "date.h"

#define MAX_LEN 100

CLIENT *rpc_setup(char *host);
long get_response(void);
void date(CLIENT *clnt, long *option);
void memory(CLIENT *clnt);
void process(CLIENT *clnt);
void cpu(CLIENT *clnt);

main(int argc, char **argv)
{
  CLIENT *clnt;  /* client handle to server */
  char *server;  /* server */
  long response;

  if(argc != 2)
  {
    fprintf(stderr, "usage:%s hostname\n", argv[0]);
    exit(1);
  }

  server = argv[1];

  if((clnt = rpc_setup(server)) == 0)
    exit(1);	/* cannot connect */ 

  response = get_response();
  while(response!=7)
  {
    switch(response)
    {
      case 1: case 2: case 3:
        date(clnt, &response);
      break;
      case 4:
        memory(clnt);
      break;
      case 5:
        process(clnt);
      break;
      case 6:
        cpu(clnt);
      break;
    }
    response = get_response();
  }

  clnt_destroy(clnt);
  exit(0);
}

CLIENT *rpc_setup(char *server)
{
  CLIENT *clnt = clnt_create(server,DATE_PROG,DATE_VERS,"udp");
  if(clnt == NULL)
  {
    clnt_pcreateerror(server);
    return(0);
  }
  return(clnt);
}

long get_response()
{
  long choice;
  printf("Menu: \n");
  printf("1. Date\n");
  printf("2. Time\n");
  printf("3. Both\n");
  printf("4. Memory\n");
  printf("5. Process\n");
  printf("6. CPU\n");
  printf("7. Exit\n");
  printf("Make a choice (1-7):");
  scanf("%ld", &choice);
  return(choice);
}

void date(CLIENT *clnt, long *option)
{
  char** sresult;
  if ((sresult = date_1(option, clnt)) == NULL) 
  { 
    return;
  }
  printf("%s\n\n",*sresult);
}


void memory(CLIENT *clnt)
{
  double* sresult;
  if ((sresult = memory_1(NULL,clnt)) == NULL) 
  { 
    return;
  }
  printf("RAM usage precentage: %lf %% \n\n",*sresult);
}

void process(CLIENT *clnt)
{
  double* sresult;
  if ((sresult = process_1(NULL,clnt)) == NULL) 
  { 
    return;
  }
  int res = (int)*sresult;
  printf("1 min load average: %d\n\n",res);
}

void cpu(CLIENT *clnt)
{
  double* sresult;
  if ((sresult = cpu_1(NULL,clnt)) == NULL) 
  { 
    return;
  }
  printf("CPU usage percentage: %lf %% \n\n",*sresult);
}
