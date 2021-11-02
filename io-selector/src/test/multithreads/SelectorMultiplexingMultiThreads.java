package test.multithreads;

import java.io.IOException;

public class SelectorMultiplexingMultiThreads {


	public static void main(String[] args) throws IOException {
		SelectorMultiplexingGroup master = new SelectorMultiplexingGroup(1, true);
		SelectorMultiplexingGroup worker = new SelectorMultiplexingGroup(3, false);
		master.setWorkerGroup(worker);
		worker.setMasterGroup(master);

		master.bind(9999);
	}

}
