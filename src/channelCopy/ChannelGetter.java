package channelCopy;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class ChannelGetter {
	public static final int PATH_BYTE_LENGTH = 1024*1024;

	private String path;
	private SocketChannel socketChannel;
	private FileChannel fileChannel;
	private long date;
	private boolean sender;
	private boolean receiver;

	/**
	 * FileChannel Receiver-Channel (reads the File)
	 * @param path
	 * @throws IOException
	 */
	public ChannelGetter(String path) throws IOException {
		this.path = path;
		fileChannel = FileChannel.open(Path.of(path), StandardOpenOption.WRITE, StandardOpenOption.READ);
		date = -1;
		sender = true;
		receiver = true;
	}
	
	public ChannelGetter(ChannelGetter source, String newPath) throws IOException{
		this(source);
		path = newPath;
	}

	/**
	 * Server or Client SocketChannel (Receiver-Channel). May read. Requests the path and the Date of the connected Socket
	 * @param channel
	 * @throws IOException
	 */
	public ChannelGetter(SocketChannel channel) throws IOException {
		this(channel, requestDate(channel), requestPath(channel), false);
	}

	/**
	 * Saves the params to the Objects-values. Will be used by Sender-Channel and Receiver-Channel
	 * @param channel
	 * @param date
	 * @param path
	 * @param sender sender or receiver?
	 * @throws IOException
	 */
	private ChannelGetter(SocketChannel channel, long date, String path, boolean sender) throws IOException {
		socketChannel = channel;
		this.date = date;
		this.path = path;
		if (sender) {
			this.sender = true;
		}else {
			this.receiver = true;
		}
	}

	/**
	 * Server or Client SocketChannel (Sender-Channel). May write. Sends the path and the Date to the connected Socket
	 * @param channel
	 * @param date
	 * @param path
	 * @throws IOException
	 */
	public ChannelGetter(ChannelGetter source, SocketChannel channel) throws IOException {
		this(channel, source.getDate(), source.getPath(), true);
		sendDate(socketChannel, date);
		sendPath(socketChannel, path);
	}
	
	/**
	 * FileChannel Sender-Channel (writes into a File)
	 */
	public ChannelGetter(ChannelGetter source) throws IOException{
		this.path = source.getPath();
		fileChannel = FileChannel.open(Path.of(path), StandardOpenOption.WRITE, StandardOpenOption.READ);
		date = source.getDate();
		sender = true;
	}
	

	public String getPath() {
		return path;
	}

	public long getDate() {
		return date == -1 ? new File(path).lastModified() : date;
	}

	private static void sendPath(SocketChannel socketChannel, String path) throws IOException {
		byte[] bytePath = path.getBytes();
		ByteBuffer bBuffer = ByteBuffer.wrap(bytePath);
		socketChannel.write(bBuffer);
	}

	private static String requestPath(SocketChannel socketChannel) throws IOException {
		ByteBuffer bBuffer = ByteBuffer.allocate(PATH_BYTE_LENGTH);
		socketChannel.read(bBuffer);		
		byte[] bytePath = bBuffer.array();
		return new String(bytePath).trim();
	}

	private static void sendDate(SocketChannel socketChannel, long date) throws IOException {
		ByteBuffer bBuffer = ByteBuffer.allocate(8);
		LongBuffer lBuffer = bBuffer.asLongBuffer();
		lBuffer.put(0, date);
		socketChannel.write(bBuffer);
	}

	private static long requestDate(SocketChannel socketChannel) throws IOException {
		ByteBuffer bBuffer = ByteBuffer.allocate(8);
		
		socketChannel.read(bBuffer);
		bBuffer.flip();
		LongBuffer lBuffer = bBuffer.asLongBuffer();
		
		return lBuffer.get(0);
	}

	public LimitedTransferable getLimitedTransferable() {
		return LimitedTransferable.getInstance(this);
	}
	
	/**
	 * 
	 * @return a Readable-Byte-Channel or null, if the ChannelGetter was declared as Sender-Channel
	 */
	public ReadableByteChannel getReadableByteChannel() {
		return socketChannel == null ? fileChannel : (sender ? null : socketChannel);
	}

	/**
	 * 
	 * @return a Writable-Byte-Channel or null, if the ChannelGetter was declared as a Receiver-Channel
	 */
	public WritableByteChannel getWritableByteChannel() {
		return socketChannel == null ? fileChannel : (sender ? socketChannel : null);
	}
	
	public boolean isSenderChannel() {
		return sender;
	}
	
	public boolean isReceiverChannel() {
		return receiver;
	}

	public boolean isIP() {
		return socketChannel != null;
	}

	public boolean isFile() {
		return fileChannel != null;
	}
	
	public String getIP() throws IOException {
		return socketChannel != null ? socketChannel.getRemoteAddress().toString() : null;
	}
}
