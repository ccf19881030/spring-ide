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

package org.springframework.ide.eclipse.beans.ui.graph.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.springframework.ide.eclipse.beans.ui.BeansUIPlugin;
import org.springframework.ide.eclipse.beans.ui.BeansUIUtils;
import org.springframework.ide.eclipse.beans.ui.graph.editor.GraphEditor;
import org.springframework.ide.eclipse.beans.ui.graph.editor.GraphEditorInput;
import org.springframework.ide.eclipse.beans.ui.model.BeanNode;
import org.springframework.ide.eclipse.beans.ui.model.ConfigNode;
import org.springframework.ide.eclipse.beans.ui.model.ConfigSetNode;
import org.springframework.ide.eclipse.beans.ui.model.INode;

public class ShowGraphAction extends Action implements IViewActionDelegate {

    private IViewPart view;
	private INode node;

	public void init(IViewPart view) {
		this.view = view;
    }

	public void selectionChanged(IAction action, ISelection selection) {
		node = (INode) ((IStructuredSelection) selection).getFirstElement();
	}

	public void run(IAction action) {
		if (node != null && (node instanceof ConfigNode ||
			 	   node instanceof ConfigSetNode || node instanceof BeanNode)) {
			GraphEditorInput input = new GraphEditorInput(node);
			BeansUIUtils.openInEditor(input, GraphEditor.EDITOR_ID);
		}
		BeansUIPlugin.beep();
	}
}
