/*******************************************************************************
 * Copyright (c) 2015 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.boot.properties.editor.reconciling;

import static org.springframework.ide.eclipse.boot.properties.editor.StsConfigMetadataRepositoryJsonLoader.ADDITIONAL_SPRING_CONFIGURATION_METADATA_JSON;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.springframework.ide.eclipse.boot.core.BootActivator;
import org.springframework.ide.eclipse.boot.properties.editor.quickfix.MetaDataManipulator;
import org.springframework.ide.eclipse.boot.util.JavaProjectUtil;
import org.springframework.ide.eclipse.editor.support.util.UserInteractions;
import org.springsource.ide.eclipse.commons.frameworks.core.ExceptionUtil;

@SuppressWarnings("restriction")
public class CreateAdditionalMetadataQuickfix implements ICompletionProposal {

	private static final IPath METADATA_PATH = new Path(ADDITIONAL_SPRING_CONFIGURATION_METADATA_JSON);

	private IJavaProject project;
	private String missingPropertyKey;
	private UserInteractions ui;

	public CreateAdditionalMetadataQuickfix(IJavaProject project, String missingProperty, UserInteractions ui) {
		Assert.isNotNull(ui);
		this.project = project;
		this.missingPropertyKey = missingProperty;
		this.ui = ui;
	}

	@Override
	public void apply(IDocument document) {
		try {
			IFile file = getMetadataFile(project);
			if (file!=null) {
				addDefaultMetadataTo(file);
			}
		} catch (Exception e) {
			BootActivator.log(e);
		}
	}

	private IFile getMetadataFile(IJavaProject project) {
		try {
			boolean includeDerived = false;
			IContainer[] srcRoots = JavaProjectUtil.getSourceFolders(project, includeDerived);
			IFile file = findExistingMetadataFile(srcRoots);
			if (file==null) {
				IContainer srcRoot = chooseSourceFolder(srcRoots);
				if (srcRoot!=null) {
					file = createMetadataFile(srcRoot);
				}
			}
			return file;
		} catch (CoreException e) {
			BootActivator.log(e);
		}
		return null;
	}

	private IFile createMetadataFile(IContainer srcRoot) throws CoreException {
		IFile toCreate = srcRoot.getFile(METADATA_PATH);
		createFolder(toCreate.getParent());
		toCreate.create(new ByteArrayInputStream("".getBytes()), true, new NullProgressMonitor());
		return toCreate;
	}

	private void createFolder(IContainer container) throws CoreException {
		if (!container.exists()) {
			if (container instanceof IFolder) {
				IFolder folder = (IFolder) container;
				folder.create(true, true, new NullProgressMonitor());
			} else {
				throw ExceptionUtil.coreException("Can't create folder '"+container+"': not a IFolder instance");
			}
		}
	}

	private IContainer chooseSourceFolder(IContainer[] srcRoots) {
		if (srcRoots.length==1) {
			return srcRoots[0];
		} else if (srcRoots.length>1) {
			return ui.chooseOneSourceFolder("Create Metadata File?", "No existing metadata file found. Choose a source folder to create one.",
					srcRoots,
					getPreferredSourceFolder(srcRoots)
			);
		}
		return null;
	}

	private IContainer getPreferredSourceFolder(IContainer[] srcRoots) {
		for (IContainer f : srcRoots) {
			if ("resources".equals(f.getFullPath().lastSegment())) {
				return f;
			}
		}
		return null;
	}

	private void addDefaultMetadataTo(IFile file) throws Exception {
		MetaDataManipulator metadata = new MetaDataManipulator(file);
		if (!metadata.isReliable()) {
			ui.error("Failed to add metadata!",
					"'"+file.getFullPath()+"' does not appear to contain valid JSON!\n");
		} else {
			metadata.addDefaultInfo(missingPropertyKey);
			metadata.save();
		}
	}

	private IFile findExistingMetadataFile(IContainer[] srcRoots) {
		for (IContainer srcFolder : srcRoots) {
			IFile file = srcFolder.getFile(METADATA_PATH);
			if (file.exists()) {
				return file;
			}
		}
		return null;
	}

	@Override
	public Point getSelection(IDocument document) {
		return null;
	}

	@Override
	public String getAdditionalProposalInfo() {
		return "Add property '"
				+ missingPropertyKey + "' to the 'additional-spring-configuration-metadata.json' file"
				+ " in project '"+getProjectName()+"'";
	}

	private String getProjectName() {
		return project.getProject().getName();
	}

	@Override
	public String getDisplayString() {
		return "Create metadata for '"+missingPropertyKey+"'.";
	}

	@Override
	public Image getImage() {
		return JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD);
	}

	@Override
	public IContextInformation getContextInformation() {
		return null;
	}

}
