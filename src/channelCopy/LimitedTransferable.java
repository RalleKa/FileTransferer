package channelCopy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public final class LimitedTransferable {
	public static final int MILLIS_FOR_OPERATION_TO_BE_CONSIDERED_STOPPED = 5;
	public static final int NO_COPY_SPEED_LIMIT = -1;

	private static ArrayList<LimitedTransferable> instances = new ArrayList<>();
	private String identString;

	private int maxOperationSpeed;

	private long totalRead;
	private long totalTime;
	private long since;

	private LimitedTransferable(String identString) {
		this.identString = identString;
		maxOperationSpeed = NO_COPY_SPEED_LIMIT;
	}

	/**
	 * must be greater than 1kb/s
	 * 
	 * @param speed
	 */
	public void setMaxOperationSpeed(int speed) {
		if (speed > 1024 || speed == NO_COPY_SPEED_LIMIT) {
			maxOperationSpeed = speed;
		}
	}

	public int getMaxOperationSpeed() {
		return maxOperationSpeed;
	}

	public long getAverageOperationSpeed() {
		return getTotalBytesRead() * 1000 / (getTotalUpTime() + 5);
	}

	public long getTotalUpTime() {
		return totalTime;
	}

	public long getTotalBytesRead() {
		return totalRead;
	}

	/**
	 * for LimitedOperations to call. The Tread will block until the Transferable is
	 * allowed to read/write size Bytes.
	 * 
	 * @param size the size the Operation wants to read/write
	 */
	protected synchronized void waitForOperation(int size) {
		if (since == 0) {
			since = System.currentTimeMillis();
		}

		totalRead += size;

		long sleepTime = size * 1000 / maxOperationSpeed;

		try {
			if (sleepTime > 0) {
				totalTime += sleepTime;
				since += sleepTime;

				Thread.sleep(sleepTime);
			}
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		long passed = System.currentTimeMillis() - since;
		if (passed < MILLIS_FOR_OPERATION_TO_BE_CONSIDERED_STOPPED) {
			since = 0;
		}
//		
//		
//		
//		
//		// setting new since if required
//		if (since == 0) {
//			since = System.currentTimeMillis();
//		}
//
//		// fast forward if there is no copy-speed-limit
//		if (NO_COPY_SPEED_LIMIT == maxOperationSpeed) {
//			long now = System.currentTimeMillis();
//			int passed = (int) (now - since);
//			int reduce = (int) (passed * bRead / 1000);
//			averageSpeed = (totalRead + reduce)
//					/ Math.max(1, (passed + (averageSpeed != 0 ? totalRead / averageSpeed : 0)));
//			return;
//		}
//
//		// alternative calculation if there is a speed-limit
//		bRead += size;
//
//		// updating some vars
//		long now = System.currentTimeMillis();
//		int passed = (int) (now - since);
//		long reduce = (int) passed * maxOperationSpeed / 1000;
//		since = now;
//
//		// calculation new bRead from the time passed
//		bRead = Math.max(0, bRead - reduce);
//
//		// calculation new average speed (because vars where updated, it must be here)
//		System.out.println(averageSpeed);
//		averageSpeed = (totalRead + reduce)
//				/ Math.max(1, (passed + (averageSpeed != 0 ? totalRead / averageSpeed : 0)));
//
//		try {
//			long sleep = bRead * 1000 / maxOperationSpeed;
//			if (sleep > 0) {
//				Thread.sleep(Math.min(sleep, 1000));
//			}
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
	}

	public static LimitedTransferable getInstance(ChannelGetter path) {
		String identString = getIdentString(path);

		synchronized (instances) {
			for (LimitedTransferable t : instances) {
				if (t.identString.equals(identString)) {
					return t;
				}
			}

			LimitedTransferable t = new LimitedTransferable(identString);
			instances.add(t);
			
			return t;
		}		
	}

	private static String getIdentString(ChannelGetter path) {
		if (!path.isFile()) {
			try {
				return path.getIP();
			} catch (Exception e) {
				return path.getPath();
			}
		} else {
			try {
				File f = new File(path.getPath());
				if (!f.exists()) {
					f.createNewFile();
				} else {
					f = null;
				}
				String s = Files.getFileStore(Paths.get(path.getPath())).toString();
				if (f != null) {
					f.delete();
				}

				return s;
			} catch (IOException e) {
				e.printStackTrace();
				return path.getPath();
			}
		}
	}

	public String getIdentString() {
		return identString;
	}
}
