/*******************************************************************************
 * Copyright (c) 2014-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.plugin.review.client.inject;

import com.codenvy.ide.api.extension.ExtensionGinModule;
import com.codenvy.plugin.review.client.reviewpart.ReviewPartPresenter;
import com.codenvy.plugin.review.client.reviewpart.ReviewPartView;
import com.codenvy.plugin.review.client.reviewpart.ReviewPartViewImpl;
import com.google.gwt.inject.client.AbstractGinModule;

/**
 * Gin module definition for the review extension.
 */
@ExtensionGinModule
public class ReviewGinModule extends AbstractGinModule {

    @Override
    protected void configure() {
        // bind the part view
        bind(ReviewPartView.class).to(ReviewPartViewImpl.class);
        bind(ReviewPartPresenter.class);
    }
}
