package channelCopy;

import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ChannelGetterTest {
	static String sourcePath;
	static String destPath;
	static long date;
	
	static CopyTask readingServer;
	static CopyTask writingServer;

	@BeforeAll
	static void prepare() throws IOException{
		sourcePath = PrepareTests.sourcePath;
		destPath = PrepareTests.destPath;
		
		readingServer = PrepareTests.getReadingServer();
		writingServer = PrepareTests.getWritingServer();
		
		date = PrepareTests.DATE;
	}

	@Test
	void getPath() {
		System.out.println("ChannelGetter test Path");
		assertEquals(sourcePath, readingServer.getSource().getPath());
		assertEquals(destPath, readingServer.getDestination().getPath());
		assertEquals(sourcePath, writingServer.getSource().getPath());
		assertEquals(destPath, writingServer.getDestination().getPath());
	}

	@Test
	void getByteChannel() {
		System.out.println("ChannelGetter test getChannel");
		assertNotEquals(null, readingServer.getSource().getReadableByteChannel());
		assertNotEquals(null, readingServer.getDestination().getWritableByteChannel());
		assertNotEquals(null, readingServer.getSource().getWritableByteChannel());
		assertEquals(null, readingServer.getDestination().getReadableByteChannel());
		
		assertNotEquals(null, writingServer.getSource().getReadableByteChannel());
		assertNotEquals(null, writingServer.getDestination().getWritableByteChannel());
		assertEquals(null, writingServer.getSource().getWritableByteChannel());
		assertNotEquals(null, writingServer.getDestination().getReadableByteChannel());
	}


	@Test
	void getLimitedTransferable() {
		System.out.println("ChannelGetter test getTransferable");
		fail("Not yet implemented");
	}

	@Test
	void isIP() {
		System.out.println("ChannelGetter test isIP");
		assertEquals(false, readingServer.getSource().isIP());
		assertEquals(true, readingServer.getDestination().isIP());

		assertEquals(true, writingServer.getSource().isIP());
		assertEquals(false, writingServer.getDestination().isIP());
	}

	@Test
	void isFile() {
		System.out.println("ChannelGetter test isFile");
		assertEquals(true, readingServer.getSource().isFile());
		assertEquals(false, readingServer.getDestination().isFile());

		assertEquals(false, writingServer.getSource().isFile());
		assertEquals(true, writingServer.getDestination().isFile());
	}

	@Test
	void getDate() {
		System.out.println("ChannelGetter test isDate");
		assertEquals(date, readingServer.getDestination().getDate());
		assertEquals(new File(sourcePath).lastModified(), readingServer.getSource().getDate());
		
		assertEquals(new File(sourcePath).lastModified(), writingServer.getDestination().getDate());
		assertEquals(date, writingServer.getSource().getDate());
	}
	
	@Test
	void isSomeChannel() {
		System.out.println("ChannelGetter test isSomeChannel");
		assertEquals(true, readingServer.getSource().isSenderChannel());
		assertEquals(true, readingServer.getSource().isReceiverChannel());
		
		assertEquals(true, readingServer.getDestination().isSenderChannel());
		assertEquals(false, readingServer.getDestination().isReceiverChannel());
		
		assertEquals(false, writingServer.getSource().isSenderChannel());
		assertEquals(true, writingServer.getSource().isReceiverChannel());
		
		assertEquals(true, writingServer.getSource().isSenderChannel());
		assertEquals(true, writingServer.getSource().isReceiverChannel());
	}
	
	@Test
	void getIP() throws IOException {
		System.out.println("ChannelGetter test getIP");
		assertEquals(null, readingServer.getSource().getIP());
		assertEquals("192.168.182.128", readingServer.getDestination().getIP());
		
		assertEquals("192.168.182.128", writingServer.getSource().getIP());
		assertEquals(null, writingServer.getDestination().getIP());
	}

	
	@AfterAll
	static void deconstruct() throws IOException{
		System.out.println("ChannelGetter deconstruct this");
		readingServer.getSource().getReadableByteChannel().close();
		writingServer.getSource().getReadableByteChannel().close();
	}
}
