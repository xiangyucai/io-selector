package test.multithreads;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

public class SelectorMultiplexingGroup {

	private SelectorMultiplexingGroup master;
	private SelectorMultiplexingGroup worker;

	private SelectorThread[] threads;
	private static AtomicInteger xid = new AtomicInteger();

	public SelectorMultiplexingGroup(int threadNumber, boolean isMaster) throws IOException {
		if (threadNumber <= 0)
			throw new IllegalArgumentException("Illegal thread number: " + threadNumber);
		if (isMaster) {
			master = this;
		} else {
			worker = this;
		}
		threads = new SelectorThread[threadNumber];
		for (int i = 0; i < threadNumber; i++) {
			threads[i] = new SelectorThread(this);
			threads[i].start();
		}
	}

	public void setMasterGroup(SelectorMultiplexingGroup master) {
		this.master = master;
	}

	public void setWorkerGroup(SelectorMultiplexingGroup worker) {
		this.worker = worker;
	}

	public SelectorThread nextMasterSelectorThread() {
		return next(master.threads);
	}

	public SelectorThread nextWorkerSelectorThread() {
		return next(worker.threads);
	}

	private SelectorThread next(SelectorThread[] threads) {
		return threads[xid.getAndIncrement() % threads.length];
	}

	public void bind(int port) throws IOException {
		System.out.println(Thread.currentThread().getName() + ": bind " + port);
		ServerSocketChannel server = ServerSocketChannel.open();
		server.configureBlocking(false);
		server.bind(new InetSocketAddress(port));
		SelectorThread thread = nextMasterSelectorThread();
		try {
			thread.q.put(server);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		thread.selector.wakeup();
	}

}
