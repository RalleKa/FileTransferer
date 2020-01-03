package channelCopy;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LimitedTransferableTest {
	static String sourcePath;
	static String destPath;

	static CopyTask listeningClient;

	@BeforeAll
	static void prepare() throws IOException {
		System.out.println("Transferable prepare test");
		sourcePath = PrepareTests.sourcePath;
		destPath = PrepareTests.destPath;

		listeningClient = PrepareTests.getWritingServer();
	}

	@Test
	public void test() throws IOException {
		int maxOperationSpeed = 1024 * 1024;

		// set + get
		System.out.println("Transferable test get+set maxOperationSpeed");
		listeningClient.getSource().getLimitedTransferable().setMaxOperationSpeed(maxOperationSpeed);
		assertEquals(maxOperationSpeed, listeningClient.getSource().getLimitedTransferable().getMaxOperationSpeed());

		// actual max Operation speed + averageTransferableSpeed
		System.out.println("Transferable test actual operationSpeed");
		long start = System.currentTimeMillis();
		listeningClient.start();
		long end = System.currentTimeMillis();
		assertTrue(PrepareTests.nearlyEquals(20000, end - start, (byte) 5));
		assertTrue(PrepareTests.nearlyEquals(1024 * 1024,
				listeningClient.getSource().getLimitedTransferable().getAverageOperationSpeed(), (byte) 5));

		//assert File-length
		System.out.println("Transferable test File length");
		assertEquals(new File(sourcePath).length(), new File(destPath).length());
		
		// assert Total bytes read
		System.out.println("Transferable test bytes written");
		assertEquals(new File(sourcePath).length()-1, listeningClient.getSource().getLimitedTransferable().getTotalBytesRead());

		// identification
		System.out.println("Transferable test identification");
		assertEquals(LimitedTransferable.getInstance(new ChannelGetter(sourcePath)),
				LimitedTransferable.getInstance(new ChannelGetter(destPath)));
	}

}
