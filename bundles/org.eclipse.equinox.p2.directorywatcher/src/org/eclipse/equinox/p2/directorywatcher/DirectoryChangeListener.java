/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM Corporation - initial implementation and ideas 
 ******************************************************************************/
package org.eclipse.equinox.p2.directorywatcher;

import java.io.File;

/*
 * Abstract class which contains stub methods. Sub-classes to over-ride
 * methods which they are interested in.
 */
public abstract class DirectoryChangeListener {

	public void startPoll() {
		// do nothing
	}

	public void stopPoll() {
		// do nothing
	}

	public boolean isInterested(File file) {
		return false;
	}

	public boolean added(File file) {
		return false;
	}

	public boolean removed(File file) {
		return false;
	}

	public boolean changed(File file) {
		return false;
	}

	public Long getSeenFile(File file) {
		return null;
	}
}
