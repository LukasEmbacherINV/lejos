package org.lejos.nxt.ldt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class for the plugin
 * 
 * @author Matthias Paul Scholz
 */
public class LeJOSPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String ID = "org.lejos.nxt.ldt";
	
	public static final String CONSOLE_CHARSET = "utf8";	

	// The shared instance
	private static LeJOSPlugin plugin;

	// the leJOS NXJ console
	private IOConsole console;
	private PrintWriter consoleWriter;
	private BufferedReader consoleReader;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static LeJOSPlugin getDefault() {
		return plugin;
	}
	
	public static Shell getShell() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null)
			return window.getShell();
			
		IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
		if (windows.length > 0)
			return windows[0].getShell();
		
		return null;
	}	


	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 * 
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(ID, path);
	}

	public IOConsole getConsole() {
		if (console == null) {
			// create console
			IOConsole newConsole = new IOConsole("leJOS NXJ", null, getImageDescriptor("icons/nxt.jpg"), LeJOSPlugin.CONSOLE_CHARSET, false);
			// add to console manager
			ConsolePlugin cplugin = ConsolePlugin.getDefault();
			IConsoleManager conMan = cplugin.getConsoleManager();
			
			try
			{
				consoleWriter = new PrintWriter(new OutputStreamWriter(newConsole.newOutputStream(), CONSOLE_CHARSET), true);
				consoleReader = new BufferedReader(new InputStreamReader(newConsole.getInputStream(), CONSOLE_CHARSET));
			}
			catch (UnsupportedEncodingException e)
			{
				throw new RuntimeException(e);
			}
			
			conMan.addConsoles(new IConsole[] { newConsole });
			this.console = newConsole;
		}

//		// make it visible
//		String id = IConsoleConstants.ID_CONSOLE_VIEW;
//		IWorkbench wb = PlatformUI.getWorkbench();
//		if (wb != null) {
//			IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
//			if (win != null) {
//				IWorkbenchPage page = win.getActivePage();
//				if (page != null) {
//					try {
//						IConsoleView view = (IConsoleView) page.showView(id);
//						view.display(_leJOSNXJConsole);
//					} catch (PartInitException p) {
//						log(p);
//					}
//				}
//			}
//		}
		return console;
	}
	
	public PrintWriter getConsoleWriter()
	{
		getConsole();
		return this.consoleWriter;
	}
	
	public BufferedReader getConsoleReader()
	{
		getConsole();
		return this.consoleReader;
	}
}
