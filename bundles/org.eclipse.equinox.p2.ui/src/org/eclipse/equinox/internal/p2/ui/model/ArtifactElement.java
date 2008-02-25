/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;

/**
 * Element wrapper class for an artifact key and its repository
 * 
 * @since 3.4
 */
public class ArtifactElement extends ProvElement {

	IArtifactKey key;
	IArtifactRepository repo;

	public ArtifactElement(IArtifactKey key, IArtifactRepository repo) {
		this.key = key;
		this.repo = repo;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.model.ProvElement#getImageID(java.lang.Object)
	 */
	protected String getImageId(Object obj) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
	 */
	public Object getParent(Object o) {
		return repo;
	}

	public String getLabel(Object o) {
		return key.getId() + " [" + key.getClassifier() + "]"; //$NON-NLS-1$//$NON-NLS-2$
	}

	public Object[] getChildren(Object o) {
		return repo.getArtifactDescriptors(key);
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IArtifactRepository.class)
			return getArtifactRepository();
		if (adapter == IArtifactKey.class)
			return getArtifactKey();
		return super.getAdapter(adapter);
	}

	public IArtifactKey getArtifactKey() {
		return key;
	}

	public IArtifactRepository getArtifactRepository() {
		return repo;
	}
}
