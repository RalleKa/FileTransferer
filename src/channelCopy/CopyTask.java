package channelCopy;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

public final class CopyTask {
	public static final int DEFAULT_POLLING_RESUME_REQUEST_RATE = 1000;

	private int pollingResumeRequestRate = DEFAULT_POLLING_RESUME_REQUEST_RATE;

	private long totalBytesWritten = 0;
	private long startLastOperation;
	private long durationBeforeLastOperation;

	private ChannelGetter source;
	private ChannelGetter destination;
	private AbstractCopyOperation operation;

	private ReentrantLock valueLock;
	private ReentrantLock taskLock;

	private boolean finished = false;
	private boolean existing = true;
	private boolean working = true;

	/**
	 * copys the content of the source to the destination
	 * @param source
	 * @param destination
	 * @throws IOException
	 */
	public CopyTask(ChannelGetter source, ChannelGetter destination) throws IOException {
		if (!(source.isReceiverChannel() && destination.isSenderChannel())) {
			System.out.println(source.getIP() + "\t" + source.isSenderChannel());
			System.out.println(destination.getIP() + "\t" + destination.isReceiverChannel());
			
			
			
			throw new IOException("Source or Destination was not declered correctly");
		}
		this.source = source;
		this.destination = destination;

		operation = new LimitedOperation(source, destination);

		valueLock = new ReentrantLock();
		taskLock = new ReentrantLock();
	}

	public ChannelGetter getSource() {
		return source;
	}

	public ChannelGetter getDestination() {
		return destination;
	}

	/**
	 * starts the copy-task and blocks until the process is finished or the task is stopped
	 */
	public void start() {
		valueLock.lock();
		existing = true;
		working = true;
		valueLock.unlock();

		taskLock.lock();
		try {
			run();
		} catch (Exception e) {
			taskLock.unlock();
			throw e;
		}
		taskLock.unlock();
	}

	/**
	 * stops the task. start() Thread will be released soon.
	 */
	public void stop() {
		valueLock.lock();
		existing = false;
		working = false;
		valueLock.unlock();
	}

	/**
	 * pauses the task. start()-Thread stays blocked. The Task may be resume()d.
	 */
	public void pause() {
		valueLock.lock();
		working = false;
		valueLock.unlock();
	}

	/**
	 * resumes the task. Works only if start()-Thread is still bound.
	 */
	public void resume() {
		if (existing) {
			valueLock.lock();
			working = true;
			valueLock.unlock();
		}
	}

	/**
	 * if the task is set to working, existing and not finished, the start()-Thread should be bound
	 * @return
	 */
	public boolean isActive() {
		return working && existing && !finished;
	}

	private void finish() {
		finished = true;
		existing = false;
		working = false;
	}

	/**
	 * is the task already finished
	 * @return
	 */
	public boolean isFinished() {
		return finished;
	}

	/**
	 * copies the source step by step to the destination
	 */
	private void run() {
		while (existing && !finished) {
			if (working) {
				startLastOperation = System.currentTimeMillis();

				while (working && !finished) {
					long written;
					if ((written = operation.copyStep()) == -1) {
						finish();
						return;
					}
					totalBytesWritten += written;
				}

				durationBeforeLastOperation += (startLastOperation == -1 ? 0
						: System.currentTimeMillis() - startLastOperation);
				startLastOperation = -1;
			}

			try {
				Thread.sleep(pollingResumeRequestRate);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public long getAverageSpeed() {
		return totalBytesWritten / totalUpTime();
	}

	public long totalWrittenBytes() {
		return totalBytesWritten;
	}
	
	/**
	 * @return The time elapsed since start() or resume() was triggered the last time
	 */
	public long elapsedSinceLastStart() {
		return System.currentTimeMillis() - startLastOperation;
	}
	
	/**
	 * @return The total time some Thread was working on the Task. After calling stop() or pause() the time will be paused as well
	 */
	public long totalUpTime() {
		return durationBeforeLastOperation + elapsedSinceLastStart();
	}

	/**
	 * @return The time in milliseconds at which a pause()d Thread will test whether it was resume()d
	 */
	public int getPollingResumeRequestRate() {
		return pollingResumeRequestRate;
	}

	/**
	 * @param rate The time in milliseconds at which a pause()d Thread will test whether it was resume()d
	 */
	public void setPollingResumeRequestRate(int rate) {
		if (rate < 0) {
			throw new IllegalArgumentException("The polling rate for a resume request must be greater than one");
		}
		if (rate > 3600000) {
			throw new IllegalArgumentException("The polling rate for a resume request must be less then 1 hour");
		}
		valueLock.lock();
		pollingResumeRequestRate = rate;
		valueLock.unlock();
	}
}
