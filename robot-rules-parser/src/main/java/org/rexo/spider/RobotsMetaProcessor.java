/* Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Based on RobotsMetaProcessor.java from Nutch
 * author: mikem
 * */

/* Copyright (c) 2003 The Nutch Organization.  All rights reserved.   */
/* Use subject to the conditions in http://www.nutch.org/LICENSE.txt. */

package org.rexo.spider;

import java.net.URL;

import org.w3c.dom.*;
import org.w3c.dom.html.*;

/**
 * Class for parsing META Directives from DOM trees.  This class
 * currently handles Robots META directives (all, none, nofollow,
 * noindex), finding BASE HREF tags, and HTTP-EQUIV no-cache
 * instructions.
 */
public class RobotsMetaProcessor
{
	/**
	 * Sets the indicators in <code>robotsMeta</code> to appropriate
	 * values, based on any META tags found under the given
	 * <code>node</code>.
	 */
	public static final void getRobotsMetaDirectives(RobotsMetaIndicator robotsMeta, Node node, URL currURL)
	{
		robotsMeta.reset();
		getRobotsMetaDirectivesHelper(robotsMeta, node, currURL);
	}

	private static final void getRobotsMetaDirectivesHelper(RobotsMetaIndicator robotsMeta, Node node, URL currURL)
	{
		if(node.getNodeType() == Node.ELEMENT_NODE)
		{
			if("BODY".equals(node.getNodeName()))
			{
				// META tags should not be under body
				return;
			}
			
			if("META".equals(node.getNodeName()))
			{
				NamedNodeMap attrs = node.getAttributes();
				Node nameNode = attrs.getNamedItem("name");
				
				if(nameNode != null)
				{
					if("robots".equalsIgnoreCase(nameNode.getNodeValue()))
					{
						Node contentNode = attrs.getNamedItem("content");
						
						if(contentNode != null)
						{
							String directives = contentNode.getNodeValue().toLowerCase();
							int index = directives.indexOf("none");
							
							if(index >= 0)
							{
								robotsMeta.setNoIndex();
								robotsMeta.setNoFollow();
							}
							
							index = directives.indexOf("all");
							if(index >= 0)
							{
								// do nothing...
							}
							
							index = directives.indexOf("noindex");
							if(index >= 0)
								robotsMeta.setNoIndex();
							
							index = directives.indexOf("nofollow");
							if(index >= 0)
								robotsMeta.setNoFollow();
						}
					}
				}
				
				Node HTTPEquivNode = attrs.getNamedItem("http-equiv");
				
				if(HTTPEquivNode != null && "Pragma".equalsIgnoreCase(HTTPEquivNode.getNodeValue()))
				{
					Node contentNode = attrs.getNamedItem("content");
					
					if(contentNode != null)
					{
						String content = contentNode.getNodeValue().toLowerCase();
						int index = content.indexOf("no-cache");
						if(index >= 0) 
							robotsMeta.setNoCache();
					}
				}
			}
			else if("BASE".equalsIgnoreCase(node.getNodeName()))
			{
				NamedNodeMap attrs = node.getAttributes();
				Node hrefNode = attrs.getNamedItem("href");
				
				if(hrefNode != null)
				{
					String urlString = hrefNode.getNodeValue();
					URL url = null;
					
					try {
						if(currURL == null)
							url = new URL(urlString);
						else 
							url = new URL(currURL, urlString);
					}
					catch(Exception e)
					{
					}

					if(url != null) 
						robotsMeta.setBaseHref(url);
				}
			}
		}
		
		NodeList children = node.getChildNodes();
		if(children != null)
		{
			int len = children.getLength();
			for(int i = 0; i < len; i++)
				getRobotsMetaDirectivesHelper(robotsMeta, children.item(i), currURL);
		}
	}
}
