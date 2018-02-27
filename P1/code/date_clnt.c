/*
 * Please do not edit this file.
 * It was generated using rpcgen.
 */

#include "date.h"

/* Default timeout can be changed using clnt_control() */
static struct timeval TIMEOUT = { 25, 0 };

char **
date_1(argp, clnt)
	long *argp;
	CLIENT *clnt;
{
	static char *clnt_res;

	memset((char *)&clnt_res, 0, sizeof(clnt_res));
	if (clnt_call(clnt, DATE, xdr_long, argp, xdr_wrapstring, &clnt_res, TIMEOUT) != RPC_SUCCESS)
		return (NULL);
	return (&clnt_res);
}

double *
memory_1(argp, clnt)
	void *argp;
	CLIENT *clnt;
{
	static double clnt_res;

	memset((char *)&clnt_res, 0, sizeof(clnt_res));
	if (clnt_call(clnt, MEMORY, xdr_void, argp, xdr_double, &clnt_res, TIMEOUT) != RPC_SUCCESS)
		return (NULL);
	return (&clnt_res);
}

double *
process_1(argp, clnt)
	void *argp;
	CLIENT *clnt;
{
	static double clnt_res;

	memset((char *)&clnt_res, 0, sizeof(clnt_res));
	if (clnt_call(clnt, PROCESS, xdr_void, argp, xdr_double, &clnt_res, TIMEOUT) != RPC_SUCCESS)
		return (NULL);
	return (&clnt_res);
}

double *
cpu_1(argp, clnt)
	void *argp;
	CLIENT *clnt;
{
	static double clnt_res;

	memset((char *)&clnt_res, 0, sizeof(clnt_res));
	if (clnt_call(clnt, CPU, xdr_void, argp, xdr_double, &clnt_res, TIMEOUT) != RPC_SUCCESS)
		return (NULL);
	return (&clnt_res);
}
