/*
 * Copyright 2002-2004 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package org.springframework.ide.eclipse.beans.ui.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.ui.views.properties.IPropertySource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.ChildBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.ide.eclipse.beans.ui.model.properties.ChildBeanProperties;
import org.springframework.ide.eclipse.beans.ui.model.properties.RootBeanProperties;

/**
 * Representation of a Spring bean.
 */
public class BeanNode extends AbstractNode {

	private ConfigNode config;
	private BeanDefinition beanDefinition;
	private boolean isOverride;
	private List constructorArguments;
	private List properties;
	private Map propertiesMap;	// lazily initialized in getProperty()

	/**
	 * Creates a new bean node with the given name within the specified config.
	 * 
	 * @param config the config this bean belongs to
	 * @param name the new bean's name
	 */
	public BeanNode(ConfigNode config, String name) {
		super(config, name);
		this.config = config; 
		this.beanDefinition = null;
		this.constructorArguments = new ArrayList();
		this.properties = new ArrayList();
		this.isOverride = false;

		// copy external flag from config node
		if (config != null &&
							(config.getFlags() & INode.FLAG_IS_EXTERNAL) != 0) {
			setFlags(INode.FLAG_IS_EXTERNAL);
		}
	}

	/**
	 * Creates a new bean node which is a clone from given bean node within the
	 * given config set.
	 *
	 * @param configSet the config this bean belongs to 
	 * @param bean the bean which is to clone
	 */
	public BeanNode(ConfigSetNode configSet, BeanNode bean) {
		super(configSet, bean.getName());
		setStartLine(bean.getStartLine()); 
		this.config = bean.getConfigNode(); 
		this.beanDefinition = bean.getBeanDefinition();

		// clone contructor arguments
		this.constructorArguments = new ArrayList();
		ConstructorArgumentNode[] cargs = bean.getConstructorArguments();
		for (int i = 0; i < cargs.length; i++) {
			this.constructorArguments.add(new ConstructorArgumentNode(this,
																   cargs[i]));
		}

		// clone properties
		this.properties = new ArrayList();
		PropertyNode[] props = bean.getProperties();
		for (int i = 0; i < props.length; i++) {
			this.properties.add(new PropertyNode(this, props[i]));
		}
		this.isOverride = false;

		// copy external flag from config set node
		if (this.config != null &&
					   (this.config.getFlags() & INode.FLAG_IS_EXTERNAL) != 0) {
			setFlags(INode.FLAG_IS_EXTERNAL);
		}
	}

	public void setBeanDefinition(BeanDefinition beanDefinition) {
		this.beanDefinition = beanDefinition;
		if (!isSingleton()) {
			setFlags(INode.FLAG_IS_PROTOTYPE);
		}
	}

	public BeanDefinition getBeanDefinition() {
		return beanDefinition;
	}

	public void setIsOverride(boolean isOverridden) {
		this.isOverride = isOverridden;
		setFlags(INode.FLAG_HAS_WARNINGS);
	}

	public boolean isOverride() {
		return isOverride;
	}

	public void addConstructorArgument(ConstructorArgumentNode carg) {
		constructorArguments.add(carg);
	}

	public ConstructorArgumentNode[] getConstructorArguments() {
		return (ConstructorArgumentNode[]) constructorArguments.toArray(
					  new ConstructorArgumentNode[constructorArguments.size()]);
	}

	public boolean hasConstructorArguments() {
		return !constructorArguments.isEmpty();
	}

	/**
	 * Adds the given property to this bean.
	 * 
	 * @param property the property to add
	 */
	public void addProperty(PropertyNode property) {
		properties.add(property);
	}

	public PropertyNode getProperty(String name) {
		if (propertiesMap == null) {

			// Lazily initialize the property map
			propertiesMap = new HashMap();
			Iterator iter = properties.iterator();
			while (iter.hasNext()) {
				PropertyNode property = (PropertyNode) iter.next();
				propertiesMap.put(property.getName(), property);
			}
		}
		return (PropertyNode) propertiesMap.get(name);
	}

	public boolean hasProperties() {
		return !properties.isEmpty();
	}

	/**
	 * Returns the property nodes of this bean.
	 * 
	 * @return property nodes of this bean
	 */
	public PropertyNode[] getProperties() {
		return (PropertyNode[]) properties.toArray(
										   new PropertyNode[properties.size()]);
	}

	public String getClassName() {
		if (beanDefinition instanceof RootBeanDefinition) {
			return ((RootBeanDefinition) beanDefinition).getBeanClassName();
		}
		return null;
	}

	public String getParentName() {
		if (beanDefinition instanceof ChildBeanDefinition) {
			return ((ChildBeanDefinition) beanDefinition).getParentName();
		}
		return null;
	}

	public boolean isRootBean() {
		return (beanDefinition instanceof RootBeanDefinition);
	}

	public boolean isSingleton() {
		if (beanDefinition != null) {
			if (beanDefinition instanceof RootBeanDefinition) {
				return ((RootBeanDefinition) beanDefinition).isSingleton();
			} else {
				return ((ChildBeanDefinition) beanDefinition).isSingleton();
			}
		}
		return true;
	}
	
	/**
	 * Returns the <code>ConfigNode</code> containing this bean.
	 * 
	 * @return ConfigNode the project containing this bean
	 */
	public ConfigNode getConfigNode() {
		return config;
	}

	/**
	 * Returns list of beans which are referenced from within this bean's
	 * constructor arguments or properties.
	 */
	public List getReferencedBeans() {
		List beans = new ArrayList();

		// Add referenced beans from constructor arguments
		Iterator iter = constructorArguments.iterator();
		while (iter.hasNext()) {
			ConstructorArgumentNode carg = (ConstructorArgumentNode) 
																	iter.next();
			beans.addAll(carg.getReferencedBeans());
		}

		// Add referenced beans from properties
		iter = properties.iterator();
		while (iter.hasNext()) {
			PropertyNode property = (PropertyNode) iter.next();
			beans.addAll(property.getReferencedBeans());
		}
		return beans;
	}

	public void remove(INode node) {
		properties.remove(node);
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IPropertySource.class) {
			if (isRootBean()) {
				return new RootBeanProperties(this);
			} else {
				return new ChildBeanProperties(this);
			}
		}
		return null;
	}

	public String toString() {
		StringBuffer text = new StringBuffer();
		text.append(getName());
		if (getClassName() != null) {
			text.append(" [");
			text.append(getClassName());
			text.append(']');
		} else if (getParentName() != null) {
			text.append(" <");
			text.append(getParentName());
			text.append('>');
		}
		return text.toString();
	}
}
