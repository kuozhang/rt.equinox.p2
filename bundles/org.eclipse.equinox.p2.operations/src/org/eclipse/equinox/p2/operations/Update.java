/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.p2.operations;

import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;

/**
 * A simple data structure describing a possible update.
 * 
 * @since 2.0
 *
 */
public class Update {

	public IInstallableUnit toUpdate;
	public IInstallableUnit replacement;

	public Update(IInstallableUnit toUpdate, IInstallableUnit replacement) {
		this.toUpdate = toUpdate;
		this.replacement = replacement;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Update))
			return false;
		if (toUpdate == null)
			return false;
		if (replacement == null)
			return false;
		Update other = (Update) obj;
		return toUpdate.equals(other.toUpdate) && replacement.equals(other.replacement);
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((toUpdate == null) ? 0 : toUpdate.hashCode());
		result = prime * result + ((replacement == null) ? 0 : replacement.hashCode());
		return result;
	}

}
