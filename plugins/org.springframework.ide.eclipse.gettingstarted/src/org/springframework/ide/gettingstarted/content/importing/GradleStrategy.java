/*******************************************************************************
 *  Copyright (c) 2013 VMware, Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.gettingstarted.content.importing;

import static org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression.constant;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.springframework.ide.gettingstarted.content.CodeSet;
import org.springsource.ide.eclipse.gradle.core.samples.SampleProject;
import org.springsource.ide.eclipse.gradle.core.util.ErrorHandler;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;
import org.springsource.ide.eclipse.gradle.core.wizards.GradleImportOperation;
import org.springsource.ide.eclipse.gradle.core.wizards.NewGradleProjectOperation;

/**
 * Importer strategy implementation for importing CodeSets into the workspace and set them
 * up to use Gradle Tooling.
 * 
 * TODO: Gradle support should be in a separate 'plugin' that contributes the import strategy
 * as an extension point. So that we can make Gradle support optional.
 * 
 * @author Kris De Volder
 */
public class GradleStrategy extends ImportStrategy {
	
	private static SampleProject asSample(final String projectName, final CodeSet codeset) {
		return new SampleProject() {
			@Override
			public String getName() {
				//Probably nobody cares about the name but anyway...
				return projectName;
			}

			@Override
			public void createAt(final File location) throws CoreException {
				if (location.exists()) {
					//Delete anything that is in the way
					FileUtils.deleteQuietly(location);
				}
				try {
					codeset.createAt(location);
				} catch (Throwable e) {
					throw ExceptionUtil.coreException(e);
				}
			}
		};
	}
	

	/**
	 * Implements the import by means of 'NewGradleProjectOperation'
	 */
	private static class GradleCodeSetImport implements IRunnableWithProgress {

		private NewGradleProjectOperation op;

		public GradleCodeSetImport(ImportConfiguration conf) {
			this.op = new NewGradleProjectOperation();
			
			//Get the present values from the ImportConfiguration.
			String location = conf.getLocationField().getValue();
			final String projectName = conf.getProjectNameField().getValue();
			final CodeSet codeset = conf.getCodeSetField().getValue();
			
			//Use these values to populate the
			op.setProjectNameField(constant(projectName));
			op.setLocationField(constant(location));
			op.setSampleProjectField(constant(asSample(projectName, codeset)));
		}


		@Override
		public void run(IProgressMonitor mon) throws InvocationTargetException, InterruptedException {
			try {
				op.perform(mon);
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			}
		}

	}
	
	/**
	 * Alternate implementation that uses GradleImportOperation. This is able customise a
	 * bit more how the guides are getting imported (i.e. disable DSLD and disable
	 * dependency management. This gives behavior that is more similar to using commandline
	 * tools to generate metadata. So the behavior is more likely to match what guides
	 * authors actually do.
	 */
	private static class GradleCodeSetImport2 implements IRunnableWithProgress {

		private String location;
		private String projectName;
		private CodeSet codeset;

		public GradleCodeSetImport2(ImportConfiguration conf) {
			this.location = conf.getLocationField().getValue();
			this.projectName = conf.getProjectNameField().getValue();
			this.codeset = conf.getCodeSetField().getValue();
		}

		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			monitor.beginTask("Import "+projectName, 2);
			try {
				//1:
				SampleProject sample = asSample(projectName, codeset);
				File rootFolder = new File(location);
				sample.createAt(rootFolder);
				monitor.worked(1);
				
				//2:
				GradleImportOperation importOp = GradleImportOperation.importAll(rootFolder);
				importOp.setEnableDependencyManagement(false);
				importOp.setEnableDSLD(false);
				
				ErrorHandler eh = ErrorHandler.forImportWizard();
				importOp.perform(eh, new SubProgressMonitor(monitor, 1));
				eh.rethrowAsCore();
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			} finally {
				monitor.done();
			}
		}
	}

	@Override
	public IRunnableWithProgress createOperation(ImportConfiguration conf) {
		return new GradleCodeSetImport2(conf);
	}

	
	
}
