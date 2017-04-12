/**
 *
 */
package aless.jenkinsci.plugins.brutal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.RunList;

/**
 * Builder that discards old build histories according to more detail configurations
 * than the core function. This enables discarding builds by build status or keeping
 * older builds for every N builds / N days or discarding buildswhich has too small
 * or too big logfile size.
 *
 * @author aless
 */
public class BrutalBuildCleaner extends Recorder {

	/**
	 * If not -1, only this number of build logs are kept.
	 */
	private final int numToKeep;

	@DataBoundConstructor
	public BrutalBuildCleaner(String numToKeep) {

		this.numToKeep = parse(numToKeep);
	}

	private static int parse(String p) {
		if (p == null)
			return -1;
		try {
			return Integer.parseInt(p);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static String intToString(int i) {
		if (i == -1) {
			return ""; //$NON-NLS-1$
		} else {
			return Integer.toString(i);
		}
	}

	static class ExtendRunList extends RunList<Run<?, ?>> {
		private ArrayList<Run<?, ?>> newList;

		ExtendRunList() {
			newList = new ArrayList<Run<?, ?>>();
		}

		ArrayList<Run<?, ?>> getNewList() {
			return newList;
		}

		@Override
		public boolean add(Run<?, ?> run) {
			newList.add(run);
			return true;
		}
	}

	private void deleteOldBuildsByNum(AbstractBuild<?, ?> build, BuildListener listener, int numToKeep) {
		int currentBuildNumber = build.getNumber();
		try {
			while (build != null) {
				if (build.getNumber() <= currentBuildNumber - numToKeep) {
					listener.getLogger().println("Brutally deleting build #" + build.getNumber() + "...");
					build.delete();
				}
				build = build.getPreviousBuild();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
		listener.getLogger().println("Brutal Build Cleaner..."); //$NON-NLS-1$

		// priority influence discard results, TODO: dynamic adjust priority on UI
		deleteOldBuildsByNum(build, listener, numToKeep);

		return true;
	}

	public String getNumToKeep() {
		return intToString(numToKeep);
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		@SuppressWarnings("rawtypes")
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
			return "Brutal Build Cleaner";
		}
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	// for test
	protected Calendar getCurrentCalendar() {
		return Calendar.getInstance();
	}
}
