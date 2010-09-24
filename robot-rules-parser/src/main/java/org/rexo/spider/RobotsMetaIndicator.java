/* Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Extracted from RobotsMetaProcessor.java from Nutch
 * author: mikem
 * */

/* Copyright (c) 2003 The Nutch Organization.  All rights reserved.   */
/* Use subject to the conditions in http://www.nutch.org/LICENSE.txt. */

package org.rexo.spider;

import java.net.URL;

/**
 * Utility class with indicators for the robots directives "noindex"
 * and "nofollow", and HTTP-EQUIV/no-cache.
 * */
public class RobotsMetaIndicator
{
	private boolean noIndex = false;
	private boolean noFollow = false;
	private boolean noCache = false;
	private URL baseHref = null;

	/** 
	 * Sets <code>noIndex</code>, <code>noFollow</code> and
	 * <code>noCache</code> to <code>false</code>.
	 */
	public void reset()
	{
		noIndex = false;
		noFollow = false;
		noCache = false;
		baseHref = null;
	}

	/** 
	 * Sets <code>noFollow</code> to <code>true</code>.
	 */
	public void setNoFollow()
	{
		noFollow = true;
	}

	/** 
	 * Sets <code>noIndex</code> to <code>true</code>.
	 */
	public void setNoIndex()
	{
		noIndex = true;
	}

	/** 
	 * Sets <code>noCache</code> to <code>true</code>.
	 */
	public void setNoCache()
	{
		noCache = true;
	}

	/**
	 * Sets the <code>baseHref</code>.
	 */
	public void setBaseHref(URL baseHref)
	{
		this.baseHref = baseHref;
	}

	/** 
	 * Returns the current value of <code>noIndex</code>.
	 */
	public boolean getNoIndex()
	{
		return noIndex;
	}

	/** 
	 * Returns the current value of <code>noFollow</code>.
	 */
	public boolean getNoFollow()
	{
		return noFollow;
	}

	/** 
	 * Returns the current value of <code>noCache</code>.
	 */
	public boolean getNoCache()
	{
		return noCache;
	}

	/**
	 * Returns the <code>baseHref</code>, if set, or <code>null</code>
	 * otherwise.
	 */
	public URL getBaseHref()
	{
		return baseHref;
	}
}
