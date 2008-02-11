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
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * PropertyPage that shows an IU's properties
 * 
 * @since 3.4
 */
public abstract class IUPropertyPage extends PropertyPage {

	protected Control createContents(Composite parent) {
		noDefaultAndApplyButton();
		IInstallableUnit iu = (IInstallableUnit) ProvUI.getAdapter(getElement(), IInstallableUnit.class);
		Control control;
		if (iu == null) {
			Label label = new Label(parent, SWT.DEFAULT);
			label.setText(ProvUIMessages.IUPropertyPage_NoIUSelected);
			control = label;
		}
		control = createIUPage(parent, iu);
		Dialog.applyDialogFont(parent);
		return control;
	}

	protected int computeWidthLimit(Control control, int nchars) {
		GC gc = new GC(control);
		gc.setFont(control.getFont());
		FontMetrics fontMetrics = gc.getFontMetrics();
		gc.dispose();
		return Dialog.convertWidthInCharsToPixels(fontMetrics, nchars);
	}

	protected abstract Control createIUPage(Composite parent, IInstallableUnit iu);
}
