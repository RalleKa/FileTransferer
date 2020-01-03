package channelCopy;

interface AbstractCopyOperation {
	/**
	 * read and write
	 * @return total written/read bytes
	 */
	long copyStep();
}
