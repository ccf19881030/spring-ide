/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.boot.launch.test;

import static org.springframework.ide.eclipse.boot.launch.BootLaunchConfigurationDelegate.DEFAULT_ENABLE_DEBUG_OUTPUT;
import static org.springframework.ide.eclipse.boot.launch.BootLaunchConfigurationDelegate.DEFAULT_ENABLE_LIVE_BEAN_SUPPORT;
import static org.springframework.ide.eclipse.boot.launch.BootLaunchConfigurationDelegate.getEnableDebugOutput;
import static org.springframework.ide.eclipse.boot.launch.BootLaunchConfigurationDelegate.getEnableLiveBeanSupport;
import static org.springframework.ide.eclipse.boot.launch.BootLaunchConfigurationDelegate.getJMXPort;
import static org.springframework.ide.eclipse.boot.launch.BootLaunchConfigurationDelegate.getProfile;
import static org.springframework.ide.eclipse.boot.launch.BootLaunchConfigurationDelegate.getProperties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.ui.PlatformUI;
import org.springframework.ide.eclipse.boot.launch.BootLaunchConfigurationDelegate;
import org.springframework.ide.eclipse.boot.launch.BootLaunchShortcut;
import org.springframework.ide.eclipse.boot.test.util.LaunchResult;
import org.springframework.ide.eclipse.boot.test.util.LaunchUtil;

/**
 * @author Kris De Volder
 */
public class BootLaunchShortcutTest extends BootLaunchTestCase {

	private static final String ALT_MAIN_TYPE = "demo.AlternateMain";
	private static final String PROJECT = "empty-boot-project";
	private static final String MAIN_TYPE = "demo.EmptyBootProjectApplication";

	protected BootLaunchShortcut shortcut = new BootLaunchShortcut();
	private IRunnableContext testContext = PlatformUI.getWorkbench().getProgressService();

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		ILaunch[] launches = lm.getLaunches();
		if (launches!=null) {
			lm.removeLaunches(lm.getLaunches());
		}
	}

	public void testProjectFindTypes() throws Exception {
		IProject project = createLaunchReadyProject(PROJECT);
		assertElements(findTypes(project), //something
				MAIN_TYPE
		);
	}

	public void testJavaProjectFindTypes() throws Exception {
		IJavaProject project = JavaCore.create(createLaunchReadyProject(PROJECT));
		assertElements(findTypes(project), //something
				MAIN_TYPE
		);
	}

	public void testMainClassFindTypes() throws Exception {
		IJavaProject project = JavaCore.create(createLaunchReadyProject(PROJECT));
		IType target = project.findType(ALT_MAIN_TYPE);
		assertElements(findTypes(target),
				ALT_MAIN_TYPE
		);
	}

	public void testMainMethodFindTypes() throws Exception {
		IJavaProject project = JavaCore.create(createLaunchReadyProject(PROJECT));
		IMethod target = getMainMethod(project.findType(ALT_MAIN_TYPE));
		assertElements(findTypes(target),
				ALT_MAIN_TYPE
		);
	}

	public void testMainClassSourceFileFindTypes() throws Exception {
		IJavaProject project = JavaCore.create(createLaunchReadyProject(PROJECT));
		IResource target = project.findType(ALT_MAIN_TYPE).getUnderlyingResource();
		assertElements(findTypes(target),
				ALT_MAIN_TYPE
		);
	}

	public void testMainClassCompilationUnitFindTypes() throws Exception {
		IJavaProject project = JavaCore.create(createLaunchReadyProject(PROJECT));
		ICompilationUnit target = JavaCore.createCompilationUnitFrom(
				(IFile) project.findType(ALT_MAIN_TYPE).getUnderlyingResource());
		assertElements(findTypes(target),
				ALT_MAIN_TYPE
		);
	}

	public void testCreateConfiguration() throws Exception {
		IJavaProject project = JavaCore.create(createLaunchReadyProject(PROJECT));
		IType mainType = project.findType(MAIN_TYPE);
		ILaunchConfiguration conf = shortcut.createConfiguration(mainType);

		assertEquals(getProject(PROJECT), BootLaunchConfigurationDelegate.getProject(conf));
		assertEquals(MAIN_TYPE,
				conf.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, ""));
		assertEquals("", getProfile(conf));
		assertEquals(DEFAULT_ENABLE_DEBUG_OUTPUT, getEnableDebugOutput(conf));
		assertEquals(DEFAULT_ENABLE_LIVE_BEAN_SUPPORT, getEnableLiveBeanSupport(conf));
		int port = Integer.parseInt(getJMXPort(conf));
		assertTrue(1000 < port && port < 65536);
		assertElements(getProperties(conf)
				/*empty*/
		);
	}

	public void testLaunch() throws Exception {
		IJavaProject project = JavaCore.create(createLaunchReadyProject(PROJECT));
		IType mainType = project.findType(MAIN_TYPE);
		ILaunchConfiguration conf = shortcut.createConfiguration(mainType);
		LaunchResult r = LaunchUtil.synchLaunch(conf);
		assertContains(":: Spring Boot ::", r.out);
		assertOk(r);
	}

	////////////////////////////////////////////////////////////////


	private IMethod getMainMethod(IType type) throws Exception {
		for (IMethod m : type.getMethods()) {
			if ("main".equals(m.getElementName())) {
				return m;
			}
		}
		throw new Error("No main method in "+type);
	}

	public String[] findTypes(Object selection) throws InterruptedException, CoreException {
		IType[] types = shortcut.findTypes(new Object[] {selection}, testContext);
		if (types!=null && types.length>0) {
			String[] names = new String[types.length];
			for (int i = 0; i < names.length; i++) {
				names[i] = types[i].getFullyQualifiedName();
			}
			return names;
		}
		return new String[0];
	}

}
