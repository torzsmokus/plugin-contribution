/*******************************************************************************
 * Copyright (c) 2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.plugin.contribution.client.steps;

import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.ide.api.app.AppContext;
import com.codenvy.ide.api.app.CurrentProject;
import com.codenvy.plugin.contribution.client.ContributeMessages;
import com.codenvy.plugin.contribution.client.NotificationHelper;
import com.codenvy.plugin.contribution.client.value.Context;
import com.codenvy.plugin.contribution.client.vcs.Remote;
import com.codenvy.plugin.contribution.client.vcs.VcsService;
import com.codenvy.plugin.contribution.client.vcs.VcsServiceProvider;
import com.codenvy.plugin.contribution.client.vcs.hosting.VcsHostingService;
import com.google.gwt.user.client.rpc.AsyncCallback;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static com.codenvy.plugin.contribution.client.ContributeConstants.ATTRIBUTE_CONTRIBUTE_BRANCH;

/**
 * This step initialize the contribution workflow context.
 *
 * @author Kevin Pollet
 */
public class InitializeWorkflowContextStep implements Step {
    private static final String ORIGIN_REMOTE_NAME = "origin";

    private final VcsServiceProvider vcsServiceProvider;
    private final VcsHostingService  vcsHostingService;
    private final AppContext         appContext;
    private final NotificationHelper notificationHelper;
    private final ContributeMessages messages;
    private final Step               createWorkBranchStep;

    @Inject
    public InitializeWorkflowContextStep(@Nonnull final VcsServiceProvider vcsServiceProvider,
                                         @Nonnull final VcsHostingService vcsHostingService,
                                         @Nonnull final AppContext appContext,
                                         @Nonnull final NotificationHelper notificationHelper,
                                         @Nonnull final ContributeMessages messages,
                                         @Nonnull final DefineWorkBranchStep defineWorkBranchStep) {
        this.vcsServiceProvider = vcsServiceProvider;
        this.vcsHostingService = vcsHostingService;
        this.appContext = appContext;
        this.notificationHelper = notificationHelper;
        this.messages = messages;
        this.createWorkBranchStep = defineWorkBranchStep;
    }

    @Override
    public void execute(@Nonnull final ContributorWorkflow workflow) {
        final Context context = workflow.getContext();
        final CurrentProject currentProject = appContext.getCurrentProject();
        final VcsService vcsService = vcsServiceProvider.getVcsService();

        if (currentProject != null && vcsService != null) {
            final ProjectDescriptor project = currentProject.getRootProject();
            final Map<String, List<String>> attributes = project.getAttributes();

            context.setProject(project);

            // get origin repository's URL from default remote
            vcsService.listRemotes(project, new AsyncCallback<List<Remote>>() {
                @Override
                public void onSuccess(final List<Remote> result) {
                    for (final Remote remote : result) {

                        // save origin repository name & owner in context
                        if (ORIGIN_REMOTE_NAME.equalsIgnoreCase(remote.getName())) {
                            final String resultRemoteUrl = remote.getUrl();
                            final String repositoryName = vcsHostingService.getRepositoryNameFromUrl(resultRemoteUrl);
                            final String repositoryOwner = vcsHostingService.getRepositoryOwnerFromUrl(resultRemoteUrl);

                            context.setOriginRepositoryOwner(repositoryOwner);
                            context.setOriginRepositoryName(repositoryName);

                            // set project information
                            if (attributes.containsKey(ATTRIBUTE_CONTRIBUTE_BRANCH) &&
                                !attributes.get(ATTRIBUTE_CONTRIBUTE_BRANCH).isEmpty()) {

                                final String clonedBranch = attributes.get(ATTRIBUTE_CONTRIBUTE_BRANCH).get(0);
                                context.setClonedBranchName(clonedBranch);
                            }

                            workflow.setStep(createWorkBranchStep);
                            workflow.executeStep();
                            break;
                        }
                    }
                }

                @Override
                public void onFailure(final Throwable exception) {
                    notificationHelper.showError(InitializeWorkflowContextStep.class,
                                                 messages.contributorExtensionErrorSetupOriginRepository(exception.getMessage()),
                                                 exception);
                }
            });
        }
    }
}
