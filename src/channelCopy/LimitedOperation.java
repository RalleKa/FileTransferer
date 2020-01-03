package channelCopy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

final class LimitedOperation implements AbstractCopyOperation{
	public static final int DEFAULT_BUFFER_SIZE = 1024*1024;
	public static final int MAX_BUFFER_SIZE = 1024*1024*1024;
	
	private ReadableByteChannel sourceChannel;
	private WritableByteChannel destinationChannel;
	
	private LimitedTransferable sourceLimit;
	private LimitedTransferable destinationLimit;
	
	private int bufferSize;
	private ByteBuffer buffer;
	private boolean bufferEmpty = true;
	
	LimitedOperation(ChannelGetter source, ChannelGetter destination) throws IOException{
		bufferSize = DEFAULT_BUFFER_SIZE;
		buffer = ByteBuffer.allocate(bufferSize);
		
		sourceLimit = source.getLimitedTransferable();
		destinationLimit = destination.getLimitedTransferable();
		
		sourceChannel = source.getReadableByteChannel();
		destinationChannel = destination.getWritableByteChannel();
	}
	
	/**
	 * @return the size of the buffer used in @copyStep
	 */
	public int getBufferSize() {
		return bufferSize;
	}
	
	/**
	 * sets the size of the buffer used in @copyStep
	 * @param bufferSize between 1B and 1GB
	 */
	public void setBufferSize(int bufferSize) {
		if (bufferSize > 0 && bufferSize <= MAX_BUFFER_SIZE) {
			this.bufferSize = bufferSize;
			synchronized (buffer) {
				buffer = ByteBuffer.allocate(bufferSize);
			}
		}
	}
	
	/**
	 * read and write, may be optimized through multithreading (read and write at the same time)
	 * @return total written/read bytes
	 */
	@Override
	public synchronized long copyStep() {
		if (bufferEmpty) {
			if (sourceLimit.getMaxOperationSpeed() != LimitedTransferable.NO_COPY_SPEED_LIMIT
					&& buffer.capacity() > sourceLimit.getMaxOperationSpeed()) {
				buffer = ByteBuffer.allocate(sourceLimit.getMaxOperationSpeed());
			}
			if (destinationLimit.getMaxOperationSpeed() != LimitedTransferable.NO_COPY_SPEED_LIMIT
					&& buffer.capacity() > destinationLimit.getMaxOperationSpeed()) {
				buffer = ByteBuffer.allocate(destinationLimit.getMaxOperationSpeed());
			}
		}
		
		long amount = 0;		
		
		synchronized (buffer) {
			try {
				if (bufferEmpty) {
					amount = sourceChannel.read(buffer);
					sourceLimit.waitForOperation((int) amount);
					buffer.flip();
				}
				
				try {
					destinationLimit.waitForOperation(buffer.capacity());
					destinationChannel.write(buffer);
					buffer.clear();
					bufferEmpty = true;
				}catch (IOException e) {
					bufferEmpty = false;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return amount;
	}
}
