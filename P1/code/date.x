program DATE_PROG {
	version DATE_VERS{
		string DATE(long) = 1;
		double MEMORY(void) = 2;
		double PROCESS(void) = 3;
		double CPU(void) = 4;	
	} = 1;
} = 0x31234567;
