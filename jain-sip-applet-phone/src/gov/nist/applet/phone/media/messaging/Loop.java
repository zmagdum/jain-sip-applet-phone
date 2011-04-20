/*
 * Loop.java
 * 
 * Created on Mar 16, 2004
 *
 */
package gov.nist.applet.phone.media.messaging;

import javax.media.protocol.PullBufferStream;

/**
 *  This is a processing loop to get data from a BufferDataSourceHandler.
 * 
 * @author Jean Deruelle <jean.deruelle@nist.gov>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class Loop extends Thread {

	RawDataSourceHandler handler;
	PullBufferStream stream;
	boolean paused = true;
	boolean killed = false;

	/**
	 * Constructs a processing loop to get data from a BufferDataSourceHandler.
	 * @param handler - the data source handler where to handle the data
	 * @param stream - the stream from where to get the data
	 */
	public Loop(RawDataSourceHandler handler, PullBufferStream stream) {
	    this.handler = handler;
	    this.stream = stream;
	    start();
	}

	/**
	 * Continue the process
	 */
	public synchronized void restart() {
	    paused = false;
	    notify();
	}

	/**
	 * This is the correct way to pause a thread; unlike suspend.
	 */
	public synchronized void pause() {
	    paused = true;
	}

	/**
	 * This is the correct way to kill a thread; unlike stop.
	 */
	public synchronized void kill() {
	    killed = true;
	    notify();
	}

	/**
	 * This is the processing loop to pull data from a 
	 * BufferDataSourceHandler.
	 */
	public void run() {
	    while (!killed) {
		try {
		    while (paused && !killed) {
			wait();
		    }
		} catch (InterruptedException e) {}

		if (!killed) {
		    boolean done = handler.readPullData(stream);
		    if (done)
			pause();
		}
	    }
	}

}
