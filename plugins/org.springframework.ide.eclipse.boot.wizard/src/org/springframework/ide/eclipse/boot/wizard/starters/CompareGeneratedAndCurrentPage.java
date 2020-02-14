/*******************************************************************************
 * Copyright (c) 2020 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.boot.wizard.starters;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.springframework.ide.eclipse.boot.wizard.InitializrFactoryModel;
import org.springframework.ide.eclipse.boot.wizard.starters.AddStartersCompareModel.AddStartersTrackerState;
import org.springframework.ide.eclipse.boot.wizard.starters.eclipse.ResourceCompareInput;
import org.springframework.ide.eclipse.maven.pom.PomPlugin;
import org.springsource.ide.eclipse.commons.livexp.core.LiveExpression;
import org.springsource.ide.eclipse.commons.livexp.core.ValueListener;
import org.springsource.ide.eclipse.commons.livexp.util.ExceptionUtil;
import org.springsource.ide.eclipse.commons.livexp.util.Log;

public class CompareGeneratedAndCurrentPage extends WizardPage {

	private final InitializrFactoryModel<AddStartersModel> factoryModel;
	private Composite contentsContainer;
	private Control compareViewer = null;

	public CompareGeneratedAndCurrentPage(InitializrFactoryModel<AddStartersModel> factoryModel) {
		super("Compare", "Compare local project with generated project from Spring Initializr", null);
		this.factoryModel = factoryModel;
	}

	@Override
	public void createControl(Composite parent) {
		contentsContainer = new Composite(parent, SWT.NONE);
		contentsContainer.setLayout(GridLayoutFactory.fillDefaults().create());
		setControl(contentsContainer);
	}

	private void connectModelToUi(AddStartersModel model) {
		AddStartersCompareModel compareModel = model.getCompareModel();

		// Add listener to be notified with compare model is populated
		compareModel.getCompareResult().addListener(new ValueListener<AddStartersCompareResult>() {
			@Override
			public void gotValue(LiveExpression<AddStartersCompareResult> exp, AddStartersCompareResult compareResult) {
				Display.getDefault().asyncExec(() -> {
					// Dispose the old viewer if it exists. This is the case of someone click back
					// button from this page, and
					// returning again to this page from the previous page
					if (compareViewer != null) {
						compareViewer.dispose();
					}
					if (compareResult != null) {
						setupCompareViewer(compareResult);
					}
				});
			}
		});
		compareModel.getDownloadTracker().addListener(new ValueListener<AddStartersTrackerState>() {
			@Override
			public void gotValue(LiveExpression<AddStartersTrackerState> exp, AddStartersTrackerState downloadState) {
				Display.getDefault().asyncExec(() -> {
					if (downloadState != null) {
						setMessage(downloadState.getMessage());
					} else {
						setMessage("");
					}
				});
			}
		});
	}

	private void setupCompareViewer(AddStartersCompareResult compareResult) {
		try {

			AddStartersModel model = factoryModel.getModel().getValue();

			// Transform the compare result from the model into a compare editor input
			final CompareEditorInput editorInput = createCompareEditorInput(compareResult);
			editorInput.getCompareConfiguration().setProperty(PomPlugin.POM_STRUCTURE_ADDITIONS_COMPARE_SETTING, true);

			// Save the editor on ok pressed
			model.onOkPressed(() -> {
				if (editorInput.isSaveNeeded()) {
					// This will save changes in the editor.
					editorInput.okPressed();
				}
			});

			compareViewer = editorInput.createContents(contentsContainer);
			compareViewer.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
			contentsContainer.layout();
		} catch (Exception e) {
			Log.log(e);
		}
	}

	/**
	 * Creates the Eclipse compare editor input from the compare model results.
	 *
	 * @param resultFromModel
	 * @return
	 * @throws Exception
	 */
	private CompareEditorInput createCompareEditorInput(AddStartersCompareResult resultFromModel) throws Exception {

		ResourceCompareInput compareEditorInput = new ResourceCompareInput(resultFromModel.getConfiguration());
		setResources(compareEditorInput, resultFromModel);

		compareEditorInput.setTitle(
				"Compare local project on the left with generated project from Spring Initializr on the right");

		new Job("Comparing local project with generated project from Spring Initializr.") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					compareEditorInput.run(monitor);
					return Status.OK_STATUS;
				} catch (InvocationTargetException | InterruptedException e) {
					return ExceptionUtil.coreException(e).getStatus();
				}
			}

		}.schedule();
		return compareEditorInput;
	}

	/**
	 * Sets the "left" and "right" resources to compare in the compare editor input
	 *
	 * @param input
	 * @param inputFromModel
	 * @throws Exception
	 */
	private void setResources(ResourceCompareInput input, AddStartersCompareResult inputFromModel) throws Exception {
		IProject leftProject = inputFromModel.getLocalResource().getProject();
		input.setSelection(leftProject, inputFromModel.getDownloadedProject());
	}

	@Override
	public boolean isPageComplete() {
		return getWizard().getContainer().getCurrentPage() == this;
	}

	@Override
	public void setVisible(boolean visible) {
		// Connect the model to the UI only when the page becomes visible.
		// If this connection is done before, either the UI controls may not yet be created
		// or the model may not yet be available.
		if (visible) {
			AddStartersModel model = factoryModel.getModel().getValue();
			connectModelToUi(model);
			model.populateComparison();
		}
		super.setVisible(visible);
	}

	@Override
	public void dispose() {
		super.dispose();
		AddStartersModel model = factoryModel.getModel().getValue();
		model.dispose();
	}
}
