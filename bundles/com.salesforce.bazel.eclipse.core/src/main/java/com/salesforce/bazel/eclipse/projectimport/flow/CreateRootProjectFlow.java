/**
 * Copyright (c) 2020, Salesforce.com, Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of Salesforce.com nor the names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.bazel.eclipse.projectimport.flow;

import java.io.File;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.SubMonitor;

import com.salesforce.bazel.eclipse.BazelPluginActivator;
import com.salesforce.bazel.eclipse.config.BazelProjectConstants;
import com.salesforce.bazel.eclipse.project.EclipseFileLinker;
import com.salesforce.bazel.eclipse.projectview.ProjectViewUtils;
import com.salesforce.bazel.eclipse.runtime.api.ResourceHelper;
import com.salesforce.bazel.sdk.model.BazelPackageLocation;
import com.salesforce.bazel.sdk.model.BazelWorkspace;
import com.salesforce.bazel.sdk.util.BazelDirectoryStructureUtil;

/**
 * Creates the root (WORKSPACE-level) project.
 */
public class CreateRootProjectFlow implements ImportFlow {

    @Override
    public String getProgressText() {
        return "Creating root project";
    }

    @Override
    public void assertContextState(ImportContext ctx) {
        Objects.requireNonNull(ctx.getBazelWorkspaceRootDirectory());
        Objects.requireNonNull(ctx.getSelectedBazelPackages());
        Objects.requireNonNull(ctx.getEclipseProjectCreator());
        Objects.requireNonNull(ctx.getEclipseFileLinker());
    }

    @Override
    public void run(ImportContext ctx, SubMonitor progressMonitor) {
        File bazelWorkspaceRootDirectory = ctx.getBazelWorkspaceRootDirectory();
        if (!BazelDirectoryStructureUtil.isWorkspaceRoot(bazelWorkspaceRootDirectory)) {
            throw new IllegalArgumentException();
        }

        ResourceHelper resourceHelper = BazelPluginActivator.getResourceHelper();
        BazelWorkspace bazelWorkspace = BazelPluginActivator.getBazelWorkspace();
        IProject rootProject = resourceHelper.getBazelWorkspaceProject(bazelWorkspace);
        if (rootProject == null) {
            String rootProjectName =
                    BazelProjectConstants.BAZELWORKSPACE_PROJECT_BASENAME + " (" + bazelWorkspace.getName() + ")";
            rootProject = ctx.getEclipseProjectCreator().createRootProject(rootProjectName);
        }

        // link all files in the workspace root into the Eclipse project
        EclipseFileLinker fileLinker = ctx.getEclipseFileLinker();
        ctx.getEclipseProjectCreator().linkFilesInPackageDirectory(fileLinker, rootProject, "",
            bazelWorkspaceRootDirectory, null);

        // write the project view file which lists the imported packages
        // TODO I think this is wrong, if someone imports multiple times into the same workspace, we want a list of all projects in workspace
        //   resourceHelper.getProjectsForBazelWorkspace(bazelWorkspace)
        List<BazelPackageLocation> selectedBazelPackages = ctx.getSelectedBazelPackages();
        ProjectViewUtils.writeProjectViewFile(bazelWorkspaceRootDirectory, rootProject, selectedBazelPackages);

        ctx.setRootProject(rootProject);
    }

}
