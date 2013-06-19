package com.github.neothemachine.gwt.junit;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.io.FileUtils;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.junit.JUnitShell;
import com.google.gwt.junit.RunStyle;

/**
 * Run style for starting a local PhantomJS process to run tests.
 * 
 * Note: phantomjs must be on your PATH!
 * 
 */
public class RunStylePhantomJS extends RunStyle {

	private Process process;

	/**
	 * Registered as a shutdown hook to make sure that the phantomjs process is killed
	 * after tests are run.
	 */
	private class ShutdownCb extends Thread {

		@Override
		public void run() {
			try {
				process.exitValue();
			} catch (IllegalThreadStateException e) {
				// phantomjs runs, as expected. Kill it.
				process.destroy();
			}
		}
	}

	@Override
	public String[] getInterruptedHosts() {
		// Make sure phantomjs is still running
		try {
			process.exitValue();

			// phantomjs exited, so return an arbitrary one-element list
			return new String[] { "phantomjs" };
		} catch (IllegalThreadStateException e) {
			// phantomjs is still active, keep looking.
		}
		return null;
	}

	public RunStylePhantomJS(final JUnitShell shell) {
		super(shell);
	}

	@Override
	public boolean setupMode(TreeLogger logger, boolean developmentMode) {
		return !developmentMode;
	}

	@Override
	public int initialize(String args) {
		Runtime.getRuntime().addShutdownHook(new ShutdownCb());
		return 1;
	}
	
	/**
	 * Work-around until GWT's JUnitShell handles IPv6 addresses correctly.
	 * @see https://groups.google.com/d/msg/google-web-toolkit/jLGhwUrKVRY/eQaDO6EUqdYJ
	 */
    public String getLocalHostName() {
        InetAddress a;
        try {
            a = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException("Unable to determine my ip address", e);
        }
        if (a instanceof Inet6Address) {
            return "[" + a.getHostAddress() + "]";
        } else {
            return a.getHostAddress();
        }
    }

	@Override
	public void launchModule(String moduleName)
			throws UnableToCompleteException {

		String url = shell.getModuleUrl(moduleName);

		shell.getTopLogger().log(TreeLogger.TRACE,
				"Letting PhantomJS fetch " + url);

		File temp;
		try {
			temp = File.createTempFile("phantomjs", null);
			temp.deleteOnExit();
			FileUtils.writeStringToFile(temp,
					"require('webpage').create().open('" + url + "',"
							+ " function () {});");
		} catch (IOException e) {
			shell.getTopLogger().log(TreeLogger.ERROR,
					"Couldn't create temporary PhantomJS script file", e);
			throw new UnableToCompleteException();
		}

		String[] args = { "phantomjs", temp.getAbsolutePath() };
		try {
			shell.getTopLogger().log(TreeLogger.TRACE,
					"About to start PhantomJS");
			this.process = Runtime.getRuntime().exec(args);
			shell.getTopLogger().log(TreeLogger.TRACE,
					"PhantomJS process started");
		} catch (IOException e) {
			shell.getTopLogger().log(TreeLogger.ERROR,
					"Error launching PhantomJS", e);
			throw new UnableToCompleteException();
		}

		StreamGobbler errorGobbler = new StreamGobbler(
				process.getErrorStream(), "ERROR");

		StreamGobbler outputGobbler = new StreamGobbler(
				process.getInputStream(), "OUTPUT");

		errorGobbler.start();
		outputGobbler.start();
	}

	private class StreamGobbler extends Thread {
		InputStream is;
		String type;

		StreamGobbler(InputStream is, String type) {
			this.is = is;
			this.type = type;
		}

		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				while ((line = br.readLine()) != null) {
					shell.getTopLogger().log(TreeLogger.TRACE,
							type + ">" + line);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

}
