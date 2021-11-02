package test.multithreads;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class SelectorThread extends Thread {

	private SelectorMultiplexingGroup group;
	Selector selector;
	BlockingQueue<Channel> q;

	public SelectorThread(SelectorMultiplexingGroup group) throws IOException {
		if (group == null)
			throw new IllegalArgumentException("The group can not be set null");
		this.group = group;
		this.q = new LinkedBlockingDeque<>(100);
		this.selector = Selector.open();
	}

	@Override
	public void run() {
		while (true) {
			try {
				System.out.println(Thread.currentThread().getName() + " select");
				int num = selector.select();
				System.out.println(Thread.currentThread().getName() + " select " + num);
				while (num > 0) {
					Set<SelectionKey> selectedKeys = selector.selectedKeys();
					Iterator<SelectionKey> it = selectedKeys.iterator();
					while (it.hasNext()) {
						SelectionKey key = it.next();
						it.remove();
						if (key.isAcceptable()) {
							acceptHandler(key);
						} else if (key.isReadable()) {
							readHandler(key);
						} else if (key.isWritable()) {
							// TODO
							System.out.println("write handler");
						}
					}
				}
				System.out.println(Thread.currentThread().getName() + " queue: " + q);
				while (!q.isEmpty()) {
					Channel c = q.poll(1, TimeUnit.SECONDS);
					if (c instanceof ServerSocketChannel) {
						ServerSocketChannel server = (ServerSocketChannel) c;
						server.register(this.selector, SelectionKey.OP_ACCEPT);
					} else {
						SocketChannel client = (SocketChannel) c;
						client.register(this.selector, SelectionKey.OP_READ);
					}
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void acceptHandler(SelectionKey key) throws ClosedChannelException {
		ByteBuffer buffer = ByteBuffer.allocate(4096);
		key.attach(buffer);
		// 此处不能直接调用register，因为thread的selector还在阻塞状态，需要先wakeup
		// 但是先wakeup，再调register的话，可能会因为线程不同步导致那边的thread的selector又变成阻塞状态后，
		// register才被调用，最终结果还是阻塞
		SelectorThread thread = group.nextWorkerSelectorThread();
		try {
			thread.q.put(key.channel());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		thread.selector.wakeup();
	}

	private void readHandler(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		ByteBuffer buffer = (ByteBuffer) key.attachment();
		buffer.clear();
		try {
			while (true) {
				int n = channel.read(buffer);
				if (n > 0) {
					buffer.flip();
					while (buffer.hasRemaining()) {
						channel.write(buffer);
					}
					buffer.clear();
				} else if (n == 0) {
					break;
				} else {
					System.out.println("client had closed");
					channel.close();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			if (channel != null) {
				try {
					channel.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			throw e;
		}
	}

}
