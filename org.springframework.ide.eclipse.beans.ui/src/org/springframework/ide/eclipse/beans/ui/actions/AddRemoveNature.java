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

package org.springframework.ide.eclipse.beans.ui.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.springframework.ide.eclipse.beans.core.BeansCoreUtils;
import org.springframework.ide.eclipse.beans.core.internal.project.BeansProjectNature;
import org.springframework.ide.eclipse.beans.ui.BeansUILabelDecorator;


public class AddRemoveNature implements IObjectActionDelegate {
    private IWorkbenchPart targetPart;
	private List selected = new ArrayList();

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
    }

	public void selectionChanged(IAction action, ISelection selection) {
		selected.clear();
		if (selection instanceof IStructuredSelection) {
			boolean enabled = true;
			Iterator iter = ((IStructuredSelection)selection).iterator();
			while (iter.hasNext()) {
				Object obj = iter.next();
				if (obj instanceof IJavaProject) {
					obj = ((IJavaProject) obj).getProject();
				}
				if (obj instanceof IProject) {
					IProject project = (IProject) obj;
					if (!project.isOpen()) {
						enabled = false;
						break;
					} else {
						selected.add(project);
					}
				} else {
					enabled = false;
					break;
				}
			}
			action.setEnabled(enabled);
		}
	}

	public void run(IAction action) {
		Iterator iter = selected.iterator();
		while (iter.hasNext()) {
			IProject project = (IProject) iter.next();
			if (BeansCoreUtils.isBeansProject(project)) {
				BeansCoreUtils.removeProjectNature(project,
												  BeansProjectNature.NATURE_ID);
			} else {
				BeansCoreUtils.addProjectNature(project,
												BeansProjectNature.NATURE_ID);
			}

			// Refresh label decoration for Spring project and config files
			BeansUILabelDecorator.update();
		}
	}
}
