/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.ISurrogateProfileHandler;
import org.eclipse.equinox.p2.engine.query.IUProfilePropertyQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.osgi.util.NLS;

public class Profile implements IProfile {
	private final IProvisioningAgent agent;
	//Internal id of the profile
	private final String profileId;

	private Profile parentProfile;

	/**
	 * 	A collection of child profiles.
	 */
	private List<String> subProfileIds; // child profile ids

	/**
	 * This storage is to be used by the touchpoints to store data.
	 */
	private OrderedProperties storage = new OrderedProperties();

	private Set<IInstallableUnit> ius = new HashSet<IInstallableUnit>();
	private Map<IInstallableUnit, OrderedProperties> iuProperties = new HashMap<IInstallableUnit, OrderedProperties>();
	private boolean changed = false;

	private long timestamp;
	private ISurrogateProfileHandler surrogateProfileHandler;

	public Profile(IProvisioningAgent agent, String profileId, Profile parent, Map<String, String> properties) {
		this.agent = agent;
		if (profileId == null || profileId.length() == 0) {
			throw new IllegalArgumentException(NLS.bind(Messages.Profile_Null_Profile_Id, null));
		}
		this.profileId = profileId;
		setParent(parent);
		if (properties != null)
			storage.putAll(properties);

	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.IProfile#getProfileId()
	 */
	public String getProfileId() {
		return profileId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.IProfile#getParentProfile()
	 */
	public IProfile getParentProfile() {
		return parentProfile;
	}

	public void setParent(Profile profile) {
		if (profile == parentProfile)
			return;

		if (parentProfile != null)
			parentProfile.removeSubProfile(profileId);

		parentProfile = profile;
		if (parentProfile != null)
			parentProfile.addSubProfile(profileId);
	}

	/*
	 * 	A profile is a root profile if it is not a sub-profile
	 * 	of another profile.
	 */
	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.IProfile#isRootProfile()
	 */
	public boolean isRootProfile() {
		return parentProfile == null;
	}

	public void addSubProfile(String subProfileId) throws IllegalArgumentException {
		if (subProfileIds == null)
			subProfileIds = new ArrayList<String>();

		if (!subProfileIds.contains(subProfileId))
			subProfileIds.add(subProfileId);

		//		if (!subProfileIds.add(subProfileId))
		//			throw new IllegalArgumentException(NLS.bind(Messages.Profile_Duplicate_Child_Profile_Id, new String[] {subProfileId, this.getProfileId()}));
	}

	public void removeSubProfile(String subProfileId) throws IllegalArgumentException {
		if (subProfileIds != null)
			subProfileIds.remove(subProfileId);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.IProfile#hasSubProfiles()
	 */
	public boolean hasSubProfiles() {
		return subProfileIds != null && !subProfileIds.isEmpty();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.IProfile#getSubProfileIds()
	 */
	public List<String> getSubProfileIds() {
		if (subProfileIds == null)
			return CollectionUtils.emptyList();
		return Collections.unmodifiableList(subProfileIds);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.IProfile#getProperty(java.lang.String)
	 */
	public String getProperty(String key) {
		String value = getLocalProperty(key);
		if (value == null && parentProfile != null) {
			value = parentProfile.getProperty(key);
		}
		return value;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.IProfile#getLocalProperty(java.lang.String)
	 */
	public String getLocalProperty(String key) {
		return storage.getProperty(key);
	}

	/**
	 * 	Associate the given value with the given key
	 * 	in the local storage of this profile.
	 */
	public void setProperty(String key, String value) {
		storage.setProperty(key, value);
		changed = true;
	}

	public void removeProperty(String key) {
		storage.remove(key);
		changed = true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.IProfile#query(org.eclipse.equinox.internal.provisional.p2.query.Query, org.eclipse.equinox.internal.provisional.p2.query.Collector, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		propagateProfileContext(query);
		if (query instanceof IUProfilePropertyQuery) {
			return query.perform(iuProperties.keySet().iterator());
		}
		return query.perform(ius.iterator());
	}

	private <T> void propagateProfileContext(IQuery<T> query) {
		// FIXME
		if (query instanceof IUProfilePropertyQuery) {
			((IUProfilePropertyQuery) query).setProfile(this);
			return;
		}
		if (query instanceof ICompositeQuery<?>) {
			List<IQuery<T>> queries = ((ICompositeQuery<T>) query).getQueries();
			for (int i = 0; i < queries.size(); i++) {
				propagateProfileContext(queries.get(i));
			}
		}
	}

	public IQueryResult<IInstallableUnit> available(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		if (surrogateProfileHandler != null)
			return surrogateProfileHandler.queryProfile(this, query, monitor);
		return query(query, new NullProgressMonitor());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.IProfile#getInstallableUnitProperty(org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit, java.lang.String)
	 */
	public String getInstallableUnitProperty(IInstallableUnit iu, String key) {
		OrderedProperties properties = iuProperties.get(iu);
		if (properties == null)
			return null;

		return properties.getProperty(key);
	}

	public String setInstallableUnitProperty(IInstallableUnit iu, String key, String value) {
		//		String iuKey = createIUKey(iu);
		OrderedProperties properties = iuProperties.get(iu);
		if (properties == null) {
			properties = new OrderedProperties();
			iuProperties.put(iu, properties);
		}

		changed = true;
		return (String) properties.setProperty(key, value);
	}

	public String removeInstallableUnitProperty(IInstallableUnit iu, String key) {
		//		String iuKey = createIUKey(iu);
		OrderedProperties properties = iuProperties.get(iu);
		if (properties == null)
			return null;

		String oldValue = properties.remove(key);
		if (properties.isEmpty())
			iuProperties.remove(iu);

		changed = true;
		return oldValue;
	}

	//	private static String createIUKey(IInstallableUnit iu) {
	//		return iu.getId() + "_" + iu.getVersion().toString(); //$NON-NLS-1$
	//	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.IProfile#getLocalProperties()
	 */
	public Map<String, String> getLocalProperties() {
		return OrderedProperties.unmodifiableProperties(storage);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.IProfile#getProperties()
	 */
	public Map<String, String> getProperties() {
		if (parentProfile == null)
			return getLocalProperties();

		Map<String, String> properties = new HashMap<String, String>(parentProfile.getProperties());
		properties.putAll(storage);
		return OrderedProperties.unmodifiableProperties(properties);
	}

	public IProvisioningAgent getProvisioningAgent() {
		return agent;
	}

	/**
	 * 	Add all the properties in the map to the local properties
	 * 	of the profile.
	 */
	public void addProperties(Map<String, String> properties) {
		storage.putAll(properties);
		changed = true;
	}

	public void addInstallableUnit(IInstallableUnit iu) {
		iu = iu.unresolved();
		if (ius.contains(iu))
			return;

		ius.add(iu);
		changed = true;
	}

	public void removeInstallableUnit(IInstallableUnit iu) {
		iu = iu.unresolved();
		ius.remove(iu);
		changed = true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.IProfile#getInstallableUnitProperties(org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit)
	 */
	public Map<String, String> getInstallableUnitProperties(IInstallableUnit iu) {
		OrderedProperties properties = iuProperties.get(iu);
		if (properties == null)
			properties = new OrderedProperties();

		return OrderedProperties.unmodifiableProperties(properties);
	}

	public void clearLocalProperties() {
		storage.clear();
		changed = true;
	}

	public boolean isChanged() {
		return changed;
	}

	public void setChanged(boolean isChanged) {
		changed = isChanged;
	}

	public void clearInstallableUnits() {
		ius.clear();
		iuProperties.clear();
		changed = true;
	}

	public Profile snapshot() {
		Profile parentSnapshot = null;
		if (parentProfile != null)
			parentSnapshot = parentProfile.snapshot();

		Profile snapshot = new Profile(agent, profileId, parentSnapshot, storage);
		if (surrogateProfileHandler != null)
			snapshot.setSurrogateProfileHandler(surrogateProfileHandler);
		snapshot.setTimestamp(timestamp);

		if (subProfileIds != null) {
			for (String subProfileId : subProfileIds) {
				snapshot.addSubProfile(subProfileId);
			}
		}

		for (IInstallableUnit iu : ius) {
			snapshot.addInstallableUnit(iu);
			Map<String, String> properties = getInstallableUnitProperties(iu);
			if (properties != null)
				snapshot.addInstallableUnitProperties(iu, properties);
		}
		snapshot.setChanged(false);
		return snapshot;
	}

	public void addInstallableUnitProperties(IInstallableUnit iu, Map<String, String> properties) {
		for (Entry<String, String> entry : properties.entrySet()) {
			setInstallableUnitProperty(iu, entry.getKey(), entry.getValue());
		}
	}

	public void clearInstallableUnitProperties(IInstallableUnit iu) {
		iuProperties.remove(iu);
		changed = true;
	}

	public void clearOrphanedInstallableUnitProperties() {
		Set<IInstallableUnit> keys = iuProperties.keySet();
		//		Set orphans = new HashSet();
		Collection<IInstallableUnit> toRemove = new ArrayList<IInstallableUnit>();
		for (IInstallableUnit iu : keys) {
			if (!ius.contains(iu))
				toRemove.add(iu);
		}

		for (IInstallableUnit iu : toRemove) {
			iuProperties.remove(iu);
		}
		//		List iuKeys = new ArrayList();
		//		for (Iterator it = ius.iterator(); it.hasNext();)
		//			iuKeys.add((IInstallableUnit) it.next());
		//
		//		iuProperties.keySet().retainAll(iuKeys);
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long millis) {
		timestamp = millis;
	}

	public void setSurrogateProfileHandler(ISurrogateProfileHandler surrogateProfileHandler) {
		this.surrogateProfileHandler = surrogateProfileHandler;
	}

	/**
	 * Prints a string representation for debugging purposes only.
	 */
	public String toString() {
		return "Profile(" + getProfileId() + ')'; //$NON-NLS-1$
	}
}
