package physicalLayer;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.locks.ReentrantLock;
import channelCopy.ChannelGetter;
import channelCopy.CopyTask;

public class TaskManager {
	public static final String TEMP_ENDING = ".temp";
	
	private CopyTask task;
	private ReentrantLock lock;

	public TaskManager() {
		lock = new ReentrantLock();
	}

	public CopyTask getTask() {
		return task;
	}

	public void copy(ChannelGetter source, ChannelGetter destination) throws IOException {
		lock.lock();
		File destinationFile = null;
		File destinationTempFile = null;
		ChannelGetter destinationTemp = null;
		try {
			if (destination.isFile()) {
				destinationFile = new File(destination.getPath()); 
				
				//tests weather destination exists
				if (destinationFile.exists()) {
					throw new FileAlreadyExistsException(destination.getPath());
				}

				//creates new Temp file
				destinationTempFile = new File(destination + TEMP_ENDING);
				if (!destinationTempFile.exists() && !destinationTempFile.createNewFile()) {
					throw new IOException("File could not be created");
				}
				
				//creates destinationTemp
				destinationTemp = new ChannelGetter(destination.getPath()+TEMP_ENDING);
			}
			
			//Writes File to Temp position or the socket
			task = new CopyTask(source, destinationTemp == null ? destination : destinationTemp);
			task.start();

			if (destinationTempFile != null && destinationTempFile.exists()) {
				//moves the Temp file to the required position or deletes it on error
				if (task.isFinished()) {
					Files.move(Paths.get(destinationTempFile.getPath()), Paths.get(destinationFile.getPath()));
				} else {
					destinationTempFile.delete();
				}
			}
			
			//sets Date
			new File(destinationTempFile.getPath()).setLastModified(source.getDate());

			task = null;
			lock.unlock();
		} catch (Exception e) {
			task = null;
			lock.unlock();
			//deletes tempFile on error
			if (destinationTempFile != null && destinationTempFile.exists()) {
				if (!destinationTempFile.delete());
			}
			throw e;
		}
	}

	// TODO get all ever used Laufwerke/Netzwerkverbindungen

	public void move(ChannelGetter source, ChannelGetter destination) throws IOException {
		if (!source.isFile() || !destination.isFile()) {
			throw new IllegalArgumentException("move is only allowed for Files");
		}
		
		if (new File(destination.getPath()).exists()) {
			throw new FileAlreadyExistsException(destination.getPath());
		}
		
		if (source.getLimitedTransferable() == destination.getLimitedTransferable()) {
			Files.move(Paths.get(source.getPath()), Paths.get(destination.getPath()));
		} else {
			copy(source, destination);
			new File(source.getPath()).delete();
		}
	}

	public boolean seemsSame(File toCompare) {
		return false;
	}

	public boolean isSame() {
		return false;
	}
}
