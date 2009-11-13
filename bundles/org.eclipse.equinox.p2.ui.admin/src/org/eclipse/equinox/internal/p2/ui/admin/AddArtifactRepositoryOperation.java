/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.admin;

import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.p2.operations.AddRepositoryJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;

/**
 * Operation which adds an artifact repository given its URI.
 * 
 * @since 3.4
 */
public class AddArtifactRepositoryOperation extends AddRepositoryJob {

	public AddArtifactRepositoryOperation(String label, ProvisioningSession session, URI location) {
		super(label, session, new URI[] {location});
	}

	protected IStatus doBatchedOperation(IProgressMonitor monitor) {
		SubMonitor mon = SubMonitor.convert(monitor, locations.length);
		for (int i = 0; i < locations.length; i++) {
			getSession().getArtifactRepositoryManager().addRepository(locations[i]);
			mon.worked(1);
		}
		return Status.OK_STATUS;
	}

	protected void setNickname(URI location, String nickname) {
		for (int i = 0; i < locations.length; i++) {
			getSession().getArtifactRepositoryManager().setRepositoryProperty(location, IRepository.PROP_NICKNAME, nickname);
		}
	}
}
