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
/**
 * 
 */
package org.eclipse.equinox.internal.provisional.p2.ui.operations;

import org.eclipse.equinox.internal.provisional.p2.engine.Phase;
import org.eclipse.equinox.internal.provisional.p2.engine.PhaseSet;
import org.eclipse.equinox.internal.provisional.p2.engine.phases.*;

public class InstallAndConfigurePhaseSet extends PhaseSet {
	public InstallAndConfigurePhaseSet() {
		super(new Phase[] {new Unconfigure(10), new Uninstall(10), new Install(10), new Configure(10)});
	}
}
