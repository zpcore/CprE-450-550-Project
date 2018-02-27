#include <rpc/rpc.h>
#include <time.h>
#include <sys/types.h>
#include <linux/kernel.h>
#include <sys/sysinfo.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include "date.h"

#define MAX_LEN 100

char **date_1(long *option)
{
  struct tm *timeptr;
  time_t clock;
  static char *ptr;
  static char err[] = "Invalid Response \0";
  static char s[MAX_LEN];

  clock = time(0);
  timeptr = localtime(&clock);
  switch(*option)
  {
  case 1:strftime(s,MAX_LEN,"%A, %B %d, %Y",timeptr);
    ptr=s;
    break;

  case 2:strftime(s,MAX_LEN,"%T",timeptr);
    ptr=s;
    break;
  
  case 3:strftime(s,MAX_LEN,"%A, %B %d, %Y - %T",timeptr);
    ptr=s;
    break;

  default: ptr=err;
    break;
  }

  return(&ptr);
}

double *memory_1(void)
{
  static double percent;
  struct sysinfo memInfo;
  sysinfo(&memInfo);
  long long totalPhysMem = memInfo.totalram;
  long long physMemUsed = memInfo.totalram - memInfo.freeram;
  percent = (double)physMemUsed/(double)totalPhysMem*100;
  //percent = totalPhysMem;
  return &percent;

}

double *process_1(void)
{
  struct sysinfo memInfo;
  sysinfo(&memInfo);
  static double result;
  unsigned long  loadProcessPermin = memInfo.loads[0];
  result = (double)loadProcessPermin;
  return &result;
}

double *cpu_1(void)
{
  unsigned long long lastTotalUser, lastTotalUserLow, lastTotalSys, lastTotalIdle;
  FILE* file = fopen("/proc/stat", "r");
  fscanf(file, "cpu %llu %llu %llu %llu", &lastTotalUser, &lastTotalUserLow,
      &lastTotalSys, &lastTotalIdle);
  fclose(file);

  sleep(2);
  static double percent;
  unsigned long long totalUser, totalUserLow, totalSys, totalIdle, total;

  file = fopen("/proc/stat", "r");
  fscanf(file, "cpu %llu %llu %llu %llu", &totalUser, &totalUserLow,
      &totalSys, &totalIdle);
  fclose(file);

  if (totalUser < lastTotalUser || totalUserLow < lastTotalUserLow ||
      totalSys < lastTotalSys || totalIdle < lastTotalIdle){
      //Overflow detection. Just skip this value.
      percent = -1.0;
  }
  else{
      total = (totalUser - lastTotalUser) + (totalUserLow - lastTotalUserLow) +
          (totalSys - lastTotalSys);
      percent = total;
      total += (totalIdle - lastTotalIdle);
      percent /= total;
      percent *= 100;
  }

  return &percent;
}
