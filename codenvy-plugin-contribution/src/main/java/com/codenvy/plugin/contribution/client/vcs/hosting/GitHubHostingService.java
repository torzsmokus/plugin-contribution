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
package com.codenvy.plugin.contribution.client.vcs.hosting;

import com.codenvy.ide.dto.DtoFactory;
import com.codenvy.ide.ext.github.client.GitHubClientService;
import com.codenvy.ide.ext.github.shared.GitHubIssueComment;
import com.codenvy.ide.ext.github.shared.GitHubIssueCommentInput;
import com.codenvy.ide.ext.github.shared.GitHubPullRequest;
import com.codenvy.ide.ext.github.shared.GitHubPullRequestCreationInput;
import com.codenvy.ide.ext.github.shared.GitHubPullRequestList;
import com.codenvy.ide.ext.github.shared.GitHubRepository;
import com.codenvy.ide.ext.github.shared.GitHubRepositoryList;
import com.codenvy.ide.ext.github.shared.GitHubUser;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.codenvy.ide.rest.DtoUnmarshallerFactory;
import com.codenvy.ide.rest.Unmarshallable;
import com.codenvy.plugin.contribution.client.vcs.hosting.dto.HostUser;
import com.codenvy.plugin.contribution.client.vcs.hosting.dto.IssueComment;
import com.codenvy.plugin.contribution.client.vcs.hosting.dto.PullRequest;
import com.codenvy.plugin.contribution.client.vcs.hosting.dto.PullRequestHead;
import com.codenvy.plugin.contribution.client.vcs.hosting.dto.Repository;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.rpc.AsyncCallback;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link VcsHostingService} implementation for GitHub.
 */
public class GitHubHostingService implements VcsHostingService {
    private static final String SSH_URL_PREFIX                = "git@github.com:";
    private static final String HTTPS_URL_PREFIX              = "https://github.com/";
    private static final RegExp REPOSITORY_NAME_OWNER_PATTERN = RegExp.compile("([^/]+)/([^.]+)");

    private final DtoUnmarshallerFactory dtoUnmarshallerFactory;
    private final DtoFactory             dtoFactory;
    private final GitHubClientService    gitHubClientService;

    /**
     * The templates for repository URLs.
     */
    private final UrlTemplates urlTemplates;

    @Inject
    public GitHubHostingService(final DtoUnmarshallerFactory dtoUnmarshallerFactory,
                                final DtoFactory dtoFactory,
                                final GitHubClientService gitHubClientService,
                                final UrlTemplates urlTemplates) {
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.dtoFactory = dtoFactory;
        this.gitHubClientService = gitHubClientService;
        this.urlTemplates = urlTemplates;
    }

    @Override
    public void getUserInfo(@Nonnull final AsyncCallback<HostUser> callback) {
        gitHubClientService
                .getUserInfo(new AsyncRequestCallback<GitHubUser>(dtoUnmarshallerFactory.newUnmarshaller(GitHubUser.class)) {
                    @Override
                    protected void onSuccess(final GitHubUser result) {
                        if (result == null) {
                            callback.onFailure(new Exception("No user info"));
                        } else {
                            final HostUser user = dtoFactory.createDto(HostUser.class);
                            user.withId(result.getId()).withLogin(result.getLogin()).withName(result.getName()).withUrl(result.getUrl());
                            callback.onSuccess(user);
                        }
                    }

                    @Override
                    protected void onFailure(final Throwable exception) {
                        callback.onFailure(exception);
                    }
                });
    }

    @Override
    public void getRepositoriesList(@Nonnull final AsyncCallback<List<Repository>> callback) {
        gitHubClientService.getRepositoriesList(
                new AsyncRequestCallback<GitHubRepositoryList>(dtoUnmarshallerFactory.newUnmarshaller(GitHubRepositoryList.class)) {
                    @Override
                    protected void onSuccess(final GitHubRepositoryList result) {
                        final List<Repository> repositories = new ArrayList<>();
                        for (final GitHubRepository original : result.getRepositories()) {
                            final Repository repository = dtoFactory.createDto(Repository.class);
                            repository.withFork(original.isFork()).withName(original.getName())
                                      .withPrivateRepo(original.isPrivateRepo()).withUrl(original.getUrl());
                            repositories.add(repository);
                        }
                        callback.onSuccess(repositories);
                    }

                    @Override
                    protected void onFailure(final Throwable exception) {
                        callback.onFailure(exception);
                    }
                });
    }

    @Nonnull
    @Override
    public String getRepositoryNameFromUrl(@Nonnull final String url) {
        final String urlWithoutGitHubPrefix =
                url.substring(url.startsWith(SSH_URL_PREFIX) ? SSH_URL_PREFIX.length() : HTTPS_URL_PREFIX.length());
        final MatchResult result = REPOSITORY_NAME_OWNER_PATTERN.exec(urlWithoutGitHubPrefix);

        return result.getGroup(2);
    }

    @Nonnull
    @Override
    public String getRepositoryOwnerFromUrl(@Nonnull final String url) {
        final String urlWithoutGitHubPrefix =
                url.substring(url.startsWith(SSH_URL_PREFIX) ? SSH_URL_PREFIX.length() : HTTPS_URL_PREFIX.length());
        final MatchResult result = REPOSITORY_NAME_OWNER_PATTERN.exec(urlWithoutGitHubPrefix);

        return result.getGroup(1);
    }

    @Override
    public void getForks(@Nonnull final String owner, @Nonnull final String repository,
                         @Nonnull final AsyncCallback<List<Repository>> callback) {
        gitHubClientService.getForks(owner,
                                     repository,
                                     new AsyncRequestCallback<GitHubRepositoryList>(
                                             dtoUnmarshallerFactory.newUnmarshaller(GitHubRepositoryList.class)) {
                                         @Override
                                         protected void onSuccess(final GitHubRepositoryList result) {
                                             final List<Repository> repositories = new ArrayList<>();
                                             for (final GitHubRepository original : result.getRepositories()) {
                                                 final Repository repository = dtoFactory.createDto(Repository.class);
                                                 repository.withFork(original.isFork()).withName(original.getName())
                                                           .withPrivateRepo(original.isPrivateRepo()).withUrl(original.getUrl());
                                                 repositories.add(repository);
                                             }
                                             callback.onSuccess(repositories);
                                         }

                                         @Override
                                         protected void onFailure(final Throwable exception) {
                                             callback.onFailure(exception);
                                         }
                                     });
    }

    @Override
    public void fork(@Nonnull final String owner, @Nonnull final String repository, @Nonnull final AsyncCallback<Repository> callback) {
        gitHubClientService.fork(owner,
                                 repository,
                                 new AsyncRequestCallback<GitHubRepository>(
                                         dtoUnmarshallerFactory.newUnmarshaller(GitHubRepository.class)) {
                                     @Override
                                     protected void onSuccess(final GitHubRepository result) {
                                         if (result != null) {
                                             final Repository repository = dtoFactory.createDto(Repository.class);
                                             repository.withFork(result.isFork()).withName(result.getName())
                                                       .withPrivateRepo(result.isPrivateRepo()).withUrl(result.getUrl());
                                             callback.onSuccess(repository);
                                         } else {
                                             callback.onFailure(new Exception("No repository."));
                                         }
                                     }

                                     @Override
                                     protected void onFailure(final Throwable exception) {
                                         callback.onFailure(exception);
                                     }
                                 });
    }

    @Nonnull
    @Override
    public String makeSSHRemoteUrl(@Nonnull final String username, @Nonnull final String repository) {
        return urlTemplates.gitSSHRemoteTemplate(username, repository);
    }

    @Nonnull
    @Override
    public String makeHttpRemoteUrl(@Nonnull final String username, @Nonnull final String repository) {
        return urlTemplates.gitHttpRemoteTemplate(username, repository);
    }

    @Nonnull
    @Override
    public String makePullRequestUrl(@Nonnull String username, @Nonnull String repository, @Nonnull String pullRequestNumber) {
        return urlTemplates.gitPullRequestTemplate(username, repository, pullRequestNumber);
    }

    @Override
    public void getPullRequests(final String owner, final String repository, final AsyncCallback<List<PullRequest>> callback) {
        gitHubClientService.getPullRequests(owner,
                                            repository,
                                            new AsyncRequestCallback<GitHubPullRequestList>(dtoUnmarshallerFactory.newUnmarshaller(GitHubPullRequestList.class)) {
                                                @Override
                                                protected void onSuccess(final GitHubPullRequestList result) {
                                                    final List<PullRequest> pullRequests = new ArrayList<>();
                                                    for (final GitHubPullRequest original : result.getPullRequests()) {
                                                        final PullRequest pullRequest = dtoFactory.createDto(PullRequest.class);
                                                        PullRequestHead head = dtoFactory.createDto(PullRequestHead.class);
                                                        head.withLabel(original.getHead().getLabel()).withRef(original.getHead().getRef())
                                                            .withSha(original.getHead().getSha());
                                                        pullRequest.withId(original.getId()).withUrl(original.getUrl())
                                                                   .withHtmlUrl(original.getHtmlUrl()).withNumber(original.getNumber())
                                                                   .withState(original.getState()).withHead(head);
                                                        pullRequests.add(pullRequest);
                                                    }
                                                    callback.onSuccess(pullRequests);
                                                }

                                                @Override
                                                protected void onFailure(final Throwable exception) {
                                                    callback.onFailure(exception);
                                                }
                                            });
    }

    @Override
    public void getPullRequest(final String owner, final String repository, final String headBranch, final AsyncCallback<PullRequest> callback) {
        getPullRequests(owner, repository, new AsyncCallback<List<PullRequest>>() {

            @Override
            public void onSuccess(List<PullRequest> result) {
                PullRequest pr = getPullRequestByBranch(headBranch, result);
                if (pr != null) {
                    callback.onSuccess(pr);
                } else {
                    callback.onFailure(new NoPullRequestException(headBranch));
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }
        });
    }

    protected PullRequest getPullRequestByBranch(final String headBranch, final List<PullRequest> pullRequests) {
        PullRequest foundPr = null;
        for (PullRequest pr : pullRequests) {
            if (headBranch.equals(pr.getHead().getLabel())) {
                foundPr = pr;
                break;
            }
        }
        return foundPr;
    }

    @Override
    public void createPullRequest(@Nonnull final String owner,
                                  @Nonnull final String repository,
                                  @Nonnull final String title,
                                  @Nonnull final String headBranch,
                                  @Nonnull final String baseBranch,
                                  @Nonnull final String body,
                                  @Nonnull final AsyncCallback<PullRequest> callback) {

        final GitHubPullRequestCreationInput input = GitHubHostingService.this.dtoFactory.createDto(GitHubPullRequestCreationInput.class);
        input.withTitle(title).withHead(headBranch).withBase(baseBranch).withBody(body);
        final Unmarshallable<GitHubPullRequest> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(GitHubPullRequest.class);
        gitHubClientService.createPullRequest(owner, repository, input, new AsyncRequestCallback<GitHubPullRequest>(unmarshaller) {

            @Override
            protected void onSuccess(final GitHubPullRequest result) {
                if (result != null) {
                    final PullRequest pr = GitHubHostingService.this.dtoFactory.createDto(PullRequest.class);
                    PullRequestHead head = dtoFactory.createDto(PullRequestHead.class);
                    head.withLabel(result.getHead().getLabel()).withRef(result.getHead().getRef())
                        .withSha(result.getHead().getSha());
                    pr.withId(result.getId()).withUrl(result.getUrl())
                               .withHtmlUrl(result.getHtmlUrl()).withNumber(result.getNumber())
                               .withState(result.getState()).withHead(head);
                    callback.onSuccess(pr);
                } else {
                    callback.onFailure(new Exception("No pull request."));
                }
            }

            @Override
            protected void onFailure(final Throwable exception) {
                callback.onFailure(exception);
            }
        });
    }

    @Override
    public void commentPullRequest(@Nonnull final String username, @Nonnull final String repository,
                                   @Nonnull final String pullRequestId, @Nonnull final String commentText,
                                   @Nonnull final AsyncCallback<IssueComment> callback) {
        final GitHubIssueCommentInput input = GitHubHostingService.this.dtoFactory.createDto(GitHubIssueCommentInput.class);
        input.withBody(commentText);
        final Unmarshallable<GitHubIssueComment> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(GitHubIssueComment.class);
        gitHubClientService.commentIssue(username, repository, pullRequestId, input, new AsyncRequestCallback<GitHubIssueComment>(unmarshaller) {

            @Override
            protected void onSuccess(GitHubIssueComment result) {
                if (result != null) {
                    final IssueComment comment = GitHubHostingService.this.dtoFactory.createDto(IssueComment.class);
                    comment.withId(result.getId()).withUrl(result.getUrl()).withBody(result.getBody());
                } else {
                    callback.onFailure(new Exception("No pull request comment."));
                }
            }

            @Override
            protected void onFailure(Throwable exception) {
                callback.onFailure(exception);
            }
        });
    }

    @Override
    public void getUserFork(@Nonnull final String user, @Nonnull String owner, @Nonnull final String repository,
                            @Nonnull final AsyncCallback<Repository> callback) {
        getForks(owner, repository, new AsyncCallback<List<Repository>>() {

            @Override
            public void onSuccess(final List<Repository> result) {
                // find out if current user has a fork
                Repository fork = getUserFork(user, result);
                if (fork != null) {
                    callback.onSuccess(fork);
                } else {
                    callback.onFailure(new NoUserForkException(user));
                }
            }

            @Override
            public void onFailure(final Throwable caught) {
                callback.onFailure(caught);
            }
        });
    }

    protected Repository getUserFork(final String login, final List<Repository> forks) {
        Repository userFork = null;
        for (final Repository repository : forks) {
            String forkURL = repository.getUrl();
            if (forkURL.toLowerCase().contains("/repos/" + login + "/")) {
                userFork = repository;
            }
        }
        return userFork;
    }
}
