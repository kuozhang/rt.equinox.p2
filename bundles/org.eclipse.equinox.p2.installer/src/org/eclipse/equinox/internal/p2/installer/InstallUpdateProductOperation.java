/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.installer;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.director.IDirector;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.installer.IInstallOperation;
import org.eclipse.equinox.p2.installer.InstallDescription;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;

/**
 * This operation performs installation or update of an Eclipse-based product.
 */
public class InstallUpdateProductOperation implements IInstallOperation {

	/**
	 * This constant comes from value of FrameworkAdmin.SERVICE_PROP_KEY_LAUNCHER_NAME.
	 * This profile property is being used as a short term solution for branding of the launcher.
	 */
	private static final String PROP_LAUNCHER_NAME = "org.eclipse.equinox.frameworkhandler.launcher.name";

	private IArtifactRepositoryManager artifactRepoMan;
	private BundleContext bundleContext;
	private IDirector director;
	private final InstallDescription installDescription;
	private boolean isInstall = true;
	private IMetadataRepositoryManager metadataRepoMan;
	private IProfileRegistry profileRegistry;
	private IStatus result;

	private ArrayList serviceReferences = new ArrayList();

	public InstallUpdateProductOperation(BundleContext context, InstallDescription description) {
		this.bundleContext = context;
		this.installDescription = description;
	}

	/**
	 * Determine what top level installable units should be installed by the director
	 */
	private IInstallableUnit[] computeUnitsToInstall() throws CoreException {
		IInstallableUnit root = installDescription.getRootInstallableUnit();
		//The install description just contains a prototype of the root IU. We need
		//to find the real IU in an available metadata repository
		return new IInstallableUnit[] {findUnit(root.getId(), root.getVersion())};
	}

	/**
	 * Create and return the profile into which units will be installed.
	 */
	private Profile createProfile() {
		Profile profile = getProfile();
		if (profile == null) {
			profile = new Profile(installDescription.getProductName());
			profile.setValue(Profile.PROP_INSTALL_FOLDER, installDescription.getInstallLocation().toString());
			profile.setValue(Profile.PROP_FLAVOR, installDescription.getFlavor());
			profile.setValue(PROP_LAUNCHER_NAME, installDescription.getLauncherName());
			EnvironmentInfo info = (EnvironmentInfo) ServiceHelper.getService(InstallerActivator.getDefault().getContext(), EnvironmentInfo.class.getName());
			String env = "osgi.os=" + info.getOS() + ",osgi.ws=" + info.getWS() + ",osgi.arch=" + info.getOSArch();
			profile.setValue(Profile.PROP_ENVIRONMENTS, env);
			profileRegistry.addProfile(profile);
		}
		return profile;
	}

	/**
	 * Throws an exception of severity error with the given error message.
	 */
	private CoreException fail(String message) {
		return fail(message, null);
	}

	/**
	 * Throws an exception of severity error with the given error message.
	 */
	private CoreException fail(String message, Throwable throwable) {
		return new CoreException(new Status(IStatus.ERROR, InstallerActivator.PI_INSTALLER, message, throwable));
	}

	/**
	 * Finds and returns the installable unit with the given id, and optionally the
	 * given version.
	 */
	private IInstallableUnit findUnit(String id, Version version) throws CoreException {
		if (id == null)
			throw fail("Installable unit id not specified");
		VersionRange range = VersionRange.emptyRange;
		if (version != null && !version.equals(Version.emptyVersion))
			range = new VersionRange(version, true, version, true);
		IMetadataRepository[] repos = metadataRepoMan.getKnownRepositories();
		for (int i = 0; i < repos.length; i++) {
			IInstallableUnit[] found = repos[i].query(id, range, null, true, null);
			if (found.length > 0)
				return found[0];
		}
		throw fail("Installable unit not found: " + id);
	}

	/**
	 * Returns the profile being installed into.
	 */
	private Profile getProfile() {
		return profileRegistry.getProfile(installDescription.getProductName());
	}

	/**
	 * Returns the result of the install operation, or <code>null</code> if
	 * no install operation has been run.
	 */
	public IStatus getResult() {
		return result;
	}

	private Object getService(String name) throws CoreException {
		ServiceReference ref = bundleContext.getServiceReference(name);
		if (ref == null)
			throw fail("Install requires a service that is not available: " + name);
		Object service = bundleContext.getService(ref);
		if (service == null)
			throw fail("Install requires a service implementation that is not available: " + name);
		serviceReferences.add(ref);
		return service;
	}

	/**
	 * Performs the actual product install or update.
	 */
	private void doInstall(SubMonitor monitor) throws CoreException {
		prepareMetadataRepository();
		prepareArtifactRepository();
		Profile p = createProfile();
		IInstallableUnit[] toInstall = computeUnitsToInstall();
		monitor.worked(5);

		IStatus s;
		if (isInstall) {
			monitor.subTask("Installing...");
			s = director.install(toInstall, p, null, monitor.newChild(90));
		} else {
			monitor.subTask("Updating...");
			IInstallableUnit[] toUninstall = computeUnitsToUninstall(p);
			s = director.replace(toUninstall, toInstall, p, monitor.newChild(90));
		}
		if (!s.isOK())
			throw new CoreException(s);
	}

	/**
	 * This profile is being updated; return the units to uninstall from the profile.
	 */
	private IInstallableUnit[] computeUnitsToUninstall(Profile profile) {
		ArrayList units = new ArrayList();
		for (Iterator it = profile.getInstallableUnits(); it.hasNext();)
			units.add(it.next());
		return (IInstallableUnit[]) units.toArray(new IInstallableUnit[units.size()]);
	}

	/**
	 * Returns whether this operation represents the product being installed
	 * for the first time, in a new profile.
	 */
	public boolean isFirstInstall() {
		return isInstall;
	}

	private void postInstall() {
		for (Iterator it = serviceReferences.iterator(); it.hasNext();) {
			ServiceReference sr = (ServiceReference) it.next();
			bundleContext.ungetService(sr);
		}
		serviceReferences.clear();
	}

	private void preInstall() throws CoreException {
		//setup system properties
		if (System.getProperty("eclipse.p2.data.area") == null) //$NON-NLS-1$
			System.setProperty("eclipse.p2.data.area", installDescription.getInstallLocation().append("installer").toString()); //$NON-NLS-1$ //$NON-NLS-2$
		if (System.getProperty("eclipse.p2.cache") == null) //$NON-NLS-1$
			System.setProperty("eclipse.p2.cache", installDescription.getInstallLocation().toString()); //$NON-NLS-1$
		//obtain required services
		serviceReferences.clear();
		director = (IDirector) getService(IDirector.class.getName());
		metadataRepoMan = (IMetadataRepositoryManager) getService(IMetadataRepositoryManager.class.getName());
		artifactRepoMan = (IArtifactRepositoryManager) getService(IArtifactRepositoryManager.class.getName());
		profileRegistry = (IProfileRegistry) getService(IProfileRegistry.class.getName());
	}

	private void prepareArtifactRepository() {
		URL artifactRepo = installDescription.getArtifactRepository();
		if (artifactRepo != null)
			artifactRepoMan.loadRepository(artifactRepo, null);
	}

	private void prepareMetadataRepository() {
		URL metadataRepo = installDescription.getMetadataRepository();
		if (metadataRepo != null)
			metadataRepoMan.loadRepository(metadataRepo, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.installer.IInstallOperation#install(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus install(IProgressMonitor pm) {
		SubMonitor monitor = SubMonitor.convert(pm, "Preparing to install", 100);
		try {
			try {
				preInstall();
				isInstall = getProfile() == null;
				String taskName = isInstall ? "Installing {0}" : "Updating {0}";
				monitor.setTaskName(NLS.bind(taskName, installDescription.getProductName()));
				doInstall(monitor);
				result = new Status(IStatus.OK, InstallerActivator.PI_INSTALLER, isInstall ? "Install complete" : "Update complete", null);
				monitor.setTaskName("Some final housekeeping");
			} finally {
				postInstall();
			}
		} catch (CoreException e) {
			result = e.getStatus();
		} finally {
			monitor.done();
		}
		return result;
	}
}
