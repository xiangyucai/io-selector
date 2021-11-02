package test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class SelectorMultiplexingSingleThread {

	private ServerSocketChannel server;
	private Selector selector;

	private void initServer() {
		try {
			server = ServerSocketChannel.open();
			server.configureBlocking(false);
			server.bind(new InetSocketAddress(9090));
			selector = Selector.open();

			server.register(selector, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void start() {
		System.out.println("server starting...");
		initServer();
		System.out.println("server started.");
		while (true) {
			try {
				while (selector.select() > 0) {
					Set<SelectionKey> selectedKeys = selector.selectedKeys();
					System.out.println(selectedKeys);
					Iterator<SelectionKey> it = selectedKeys.iterator();
					while (it.hasNext()) {
						SelectionKey key = it.next();
						it.remove();
						if (key.isAcceptable()) {
							handleAccept(key);
						} else if (key.isReadable()) {
							handleRead(key);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void handleAccept(SelectionKey key) throws IOException {
		System.out.println("handleAccept");
		ServerSocketChannel channel = (ServerSocketChannel) key.channel();
		SocketChannel accept = channel.accept();
		accept.configureBlocking(false);

		ByteBuffer buffer = ByteBuffer.allocate(8192);
		accept.register(selector, SelectionKey.OP_READ, buffer);
		System.out.println("handleAccept - new client: " + accept.getRemoteAddress());
	}

	private void handleRead(SelectionKey key) {
		System.out.println("handleRead");
		SocketChannel channel = (SocketChannel) key.channel();
		ByteBuffer buffer = (ByteBuffer) key.attachment();
		buffer.clear();
		try {
			while (true) {
				int read = channel.read(buffer);
				if (read > 0) {
					buffer.flip();
					while (buffer.hasRemaining()) {
						channel.write(buffer);
					}
					buffer.clear();
				} else if (read == 0) {
					System.out.println("handleRead - read nothing");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					break;
				} else {
					channel.close();
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (channel != null) {
				try {
					channel.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
		System.out.println("handleRead - end");
	}

	public static void main(String[] args) {
		SelectorMultiplexingSingleThread s = new SelectorMultiplexingSingleThread();
		s.start();

	}

}
