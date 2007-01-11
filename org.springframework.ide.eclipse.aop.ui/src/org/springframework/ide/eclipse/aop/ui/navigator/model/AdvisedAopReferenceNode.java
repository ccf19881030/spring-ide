/*
 * Copyright 2002-2006 the original author or authors.
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
package org.springframework.ide.eclipse.aop.ui.navigator.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.springframework.ide.eclipse.aop.core.model.IAopReference;
import org.springframework.ide.eclipse.beans.ui.BeansUIImages;

public class AdvisedAopReferenceNode implements IReferenceNode {

    private List<IAopReference> references;

    public AdvisedAopReferenceNode(List<IAopReference> reference) {
        this.references = reference;
    }

    public IReferenceNode[] getChildren() {
        List<IReferenceNode> nodes = new ArrayList<IReferenceNode>();
        for (IAopReference r : references) {
            nodes.add(new AdvisedAopSourceNode(r));
        }
        return nodes.toArray(new IReferenceNode[nodes.size()]);
    }

    public Image getImage() {
        return BeansUIImages.getImage(BeansUIImages.IMG_OBJS_REFERENCE);
    }

    public String getText() {
        return "advised by";
    }

    public boolean hasChildren() {
        return true;
    }

}
