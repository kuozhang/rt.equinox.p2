/*******************************************************************************
* Copyright (c) 2007, 2009 compeople AG and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* 	compeople AG (Stefan Liebig) - initial API and implementation
*******************************************************************************/
package org.eclipse.equinox.p2.repository.artifact;

/**
 * Describes a processing step. Processing steps are pieces of code that participate
 * in the the transfer of an artifact between artifact repositories. A step may alter
 * the shape of the artifact from its storage format in the repository (such as performing
 * compression), or it may perform additional checks on the transferred bytes such as 
 * checksums or signature verification.
 * 
 * @see IArtifactDescriptor#getProcessingSteps()
 * @since 2.0
 */
public class ProcessingStepDescriptor {

	private final String processorId; //the operation to be applied (e.g: unpack, md5, signature verification, etc.)
	private final String data; //data requested for the processing (eg. expected checksum)
	private final boolean required; //whether the step is optional or not

	/**
	 * Create a processing step description.
	 * 
	 * @param processorId
	 * @param data
	 * @param required
	 */
	public ProcessingStepDescriptor(String processorId, String data, boolean required) {
		super();
		this.processorId = processorId;
		this.data = data;
		this.required = required;
	}

	/**
	 * Returns the fully qualified id of the processing step extension.
	 * 
	 * @return The fully qualified processing step extension id
	 */
	public String getProcessorId() {
		return processorId;
	}

	public String getData() {
		return data;
	}

	/**
	 * Returns whether the successful execution of this processing step is
	 * required for the transfer to be successful. If the processing step extension
	 * is not installed, or fails to execute, then the artifact transfer will fail if the
	 * step is required. Failure of optional steps will result in warnings but not prevent
	 * the transfer from succeeding.
	 * 
	 * @return <code>true</code> if the transfer will fail if this step does not succeed,
	 * and <code>false</code> otherwise
	 */
	public boolean isRequired() {
		return required;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		result = prime * result + ((processorId == null) ? 0 : processorId.hashCode());
		result = prime * result + (required ? 1231 : 1237);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ProcessingStepDescriptor))
			return false;
		final ProcessingStepDescriptor other = (ProcessingStepDescriptor) obj;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		if (processorId == null) {
			if (other.processorId != null)
				return false;
		} else if (!processorId.equals(other.processorId))
			return false;
		if (required != other.required)
			return false;
		return true;
	}

	/**
	 * Returns a string representation of this descriptor for debugging purposes only.
	 */
	public String toString() {
		return "Processor: " + processorId + (required ? "(req)" : "(notReq)") + " ,data: " + data; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
	}
}