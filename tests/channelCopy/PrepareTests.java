package channelCopy;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Random;

class PrepareTests {
	public static final String sourcePath = "C:\\Users\\Developer\\Desktop\\source.pdf";
	public static final String destPath = "C:\\Users\\Developer\\Desktop\\dest.pdf";
	public static final int PORT = 1000;
	public static final long DATE = 102;
	
	private static boolean alreadyPreparedPrepare = false;

	private static ServerApplication serverApp;

	/**
	 * returns the client of a server which is constantly writing the content of a
	 * file into the TCP connection. The Client, however, would listen to TCP and
	 * write to a file
	 * 
	 * @return
	 * @throws IOException
	 */
	public static CopyTask getWritingServer() throws IOException {
		prapreAll();		

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					ChannelGetter file = new ChannelGetter(sourcePath);
					ChannelGetter tcp = new ChannelGetter(file, serverApp.getSocketChannel());
					CopyTask read = new CopyTask(file, tcp);

					read.start();
					tcp.getWritableByteChannel().close();
				} catch (Exception e) {
					e.printStackTrace();
					// evtl e nach JUnit weiterleiten
				}

			}
		}).start();
		ChannelGetter tcp = new ChannelGetter(SocketChannel.open(new InetSocketAddress(PORT)));
		ChannelGetter file = new ChannelGetter(tcp, destPath);
		
		return new CopyTask(tcp, file);
	}

	/**
	 * returns the client of a server that is listening to TCP and writing to file
	 * 
	 * @return
	 * @throws IOException
	 */
	public static CopyTask getReadingServer() throws IOException {
		prapreAll();

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					ChannelGetter tcp = new ChannelGetter(serverApp.getSocketChannel());
					ChannelGetter file = new ChannelGetter(tcp, destPath);
					CopyTask write = new CopyTask(tcp, file);
					System.out.println("1");
					write.start();
					write.getDestination().getWritableByteChannel().close();
				} catch (Exception e) {
					e.printStackTrace();
					// evtl e nach JUnit weiterleiten
				}

			}
		}).start();
		
		ChannelGetter file = new ChannelGetter(sourcePath);
		ChannelGetter tcp = new ChannelGetter(file, SocketChannel.open(new InetSocketAddress(PORT)));
System.out.println("2");
		return new CopyTask(file, tcp);
	}

	static class ServerApplication extends Thread {
		private ServerSocketChannel serverChannel;
		boolean working = true;
		SocketChannel c;

		public ServerApplication() throws IOException {
			serverChannel = ServerSocketChannel.open();
			serverChannel.bind(new InetSocketAddress(PORT));
		}

		@Override
		public void run() {
			try {
				while (working) {
					if (c == null) {
						c = serverChannel.accept();
					}
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public SocketChannel getSocketChannel() {
			while (this.c == null) {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			SocketChannel c = this.c;
			this.c = null;
			return c;
		}

		public void endWorking() {
			working = false;
			try {
				serverChannel.close();
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
	}

	/**
	 * Creates source File if not exists and created empty destination File
	 * 
	 * @throws IOException
	 */
	static void prapreAll() throws IOException {
		if (!alreadyPreparedPrepare) {
			System.out.println("Preparation create Source-File");
			if (!new File(sourcePath).exists()) {
				createFile(sourcePath);
			}

			System.out.println("Preperation create new Dest-File");
			File temp = new File(destPath);
			if (temp.exists() && !temp.delete()) {
				throw new IOException("File could not be deleted");
			}
			;
			temp.createNewFile();

			System.out.println("Preperation create ServerApp");
			if (serverApp == null) {
				serverApp = new ServerApplication();
				serverApp.start();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			alreadyPreparedPrepare = true;
		}
	}

	/**
	 * Generating 20MB File
	 * 
	 * @throws IOException
	 */
	public static void createFile(String path) throws IOException {
		Random r = new Random();
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(path)));
		for (int i = 0; i < 1024 * 1024 * 20; i++) {
			out.write(r.nextInt());
		}
		out.close();
		System.out.println("finished writing");
	}

	public static boolean nearlyEquals(long value1, long value2, byte percentile) {
		if (value1 * (100 + percentile) > value2 * 100 && value1 * (100 - percentile) < value2 * 100) {
			return true;
		} else {
			throw new IllegalArgumentException(
					"value: " + value1 + " (+-" + percentile + "%) [" + (value1 * (100 + percentile) / 100) + ", "
							+ (value1 * (100 - percentile) / 100) + "]" + ":" + (value2));
		}
	}
}
