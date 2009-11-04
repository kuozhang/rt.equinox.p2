/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.operations;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.operations.Activator;
import org.eclipse.equinox.internal.p2.operations.Messages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.osgi.util.NLS;

/**
 * Abstract class representing provisioning jobs.  Provisioning jobs
 * can be run in the background by scheduling them, or they can
 * be run by a client in a modal context.  An additional progress monitor
 * can be set into the job for progress reporting.
 * 
 * @since 2.0
 */

public abstract class ProvisioningJob extends Job {

	private class DoubleProgressMonitor extends ProgressMonitorWrapper {

		IProgressMonitor additionalMonitor;

		protected DoubleProgressMonitor(IProgressMonitor monitor1, IProgressMonitor monitor2) {
			super(monitor1);
			additionalMonitor = monitor2;
		}

		public void beginTask(String name, int totalWork) {
			super.beginTask(name, totalWork);
			additionalMonitor.beginTask(name, totalWork);
		}

		public void clearBlocked() {
			super.clearBlocked();
			if (additionalMonitor instanceof IProgressMonitorWithBlocking)
				((IProgressMonitorWithBlocking) additionalMonitor).clearBlocked();
		}

		public void done() {
			super.done();
			additionalMonitor.done();
		}

		public void internalWorked(double work) {
			super.internalWorked(work);
			additionalMonitor.internalWorked(work);
		}

		public boolean isCanceled() {
			if (super.isCanceled())
				return true;
			return additionalMonitor.isCanceled();
		}

		public void setBlocked(IStatus reason) {
			super.setBlocked(reason);
			if (additionalMonitor instanceof IProgressMonitorWithBlocking)
				((IProgressMonitorWithBlocking) additionalMonitor).setBlocked(reason);
		}

		public void setCanceled(boolean b) {
			super.setCanceled(b);
			additionalMonitor.setCanceled(b);
		}

		public void setTaskName(String name) {
			super.setTaskName(name);
			additionalMonitor.setTaskName(name);
		}

		public void subTask(String name) {
			super.subTask(name);
			additionalMonitor.subTask(name);
		}

		public void worked(int work) {
			super.worked(work);
			additionalMonitor.worked(work);
		}
	}

	/**
	 * Constant which indicates that the operation or job being managed does
	 * not require a restart upon completion.  This constant is typically used
	 * for operations that do not modify the running profile.
	 * 
	 * @since 3.6
	 */
	public static final int RESTART_NONE = 1;

	/**
	 * Constant which indicates that the operation or job being managed requires
	 * the user to either restart or apply the configuration changes in order to 
	 * pick up the provisioning changes.  This constant is typically used for
	 * operations that modify the running profile.
	 * 
	 * @since 3.6
	 */
	public static final int RESTART_OR_APPLY = 2;
	/**
	 * Constant which indicates that the operation or job being managed requires
	 * the user to restart in order to pick up the provisioning changes.  This constant
	 * is typically used for operations that modify the running profile but don't 
	 * handle dynamic changes without restarting the workbench.
	 * 
	 * @since 3.6
	 */
	public static final int RESTART_ONLY = 3;

	private ProvisioningSession session;
	private IProgressMonitor additionalMonitor;

	public ProvisioningJob(String name, ProvisioningSession session) {
		super(name);
		this.session = session;
	}

	protected ProvisioningSession getSession() {
		return session;
	}

	protected IProgressMonitor getCombinedProgressMonitor(IProgressMonitor mon1, IProgressMonitor mon2) {
		if (mon1 == null)
			return mon2;
		if (mon2 == null)
			return mon1;
		return new DoubleProgressMonitor(mon1, mon2);
	}

	public void setAdditionalProgressMonitor(IProgressMonitor monitor) {
		additionalMonitor = monitor;
	}

	/**
	 * Executes this job.  Returns the result of the execution.
	 * This method is overridden to look for a wrapped progress monitor for
	 * reporting progress.
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 * 
	 */
	public final IStatus run(IProgressMonitor monitor) {
		IProgressMonitor wrappedMonitor = getCombinedProgressMonitor(monitor, additionalMonitor);
		try {
			runModal(wrappedMonitor);
		} catch (OperationCanceledException e) {
			return Status.CANCEL_STATUS;
		} catch (ProvisionException e) {
			return getErrorStatus(null, e);
		}
		return Status.OK_STATUS;
	}

	/**
	 * Perform the specific work involved in running this job.
	 * 
	 * @param monitor
	 *            the progress monitor to use for the operation
	 * 
	 */
	public abstract void runModal(IProgressMonitor monitor) throws ProvisionException;

	public int getRestartPolicy() {
		return RESTART_NONE;
	}

	protected IStatus getErrorStatus(String message, ProvisionException e) {
		if (message == null)
			if (e == null)
				message = NLS.bind(Messages.ProvisioningJob_GenericErrorStatusMessage, getName());
			else
				message = e.getLocalizedMessage();
		return new Status(IStatus.ERROR, Activator.ID, message, e);
	}

}
