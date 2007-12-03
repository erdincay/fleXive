/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
 *  license from the author are found in LICENSE.txt distributed with
 *  these libraries.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  For further information about UCS - unique computing solutions gmbh,
 *  please see the company website: http://www.ucs.at
 *
 *  For further information about [fleXive](R), please see the
 *  project website: http://www.flexive.org
 *
 *
 *  This copyright notice MUST APPEAR in all copies of the file!
 ***************************************************************/
package com.flexive.tests.embedded.jsf.util;

import com.flexive.faces.beans.MessageBean;
import com.sun.facelets.compiler.SAXCompiler;
import com.sun.faces.application.ApplicationAssociate;
import com.sun.faces.el.ELContextImpl;
import com.sun.faces.el.ImplicitObjectELResolver;

import javax.el.ELContext;
import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseStream;
import javax.faces.context.ResponseWriter;
import javax.faces.render.RenderKit;
import java.lang.reflect.Field;
import java.util.Iterator;


/**
 * Mock faces context - supports only some methods used in the backing beans. 
 */
public class MockFacesContext extends FacesContext {
	private ExternalContext externalContext;
	private Application application;
    private ELContext elContext;

    /**
	 * Set the current instance in the faces threadlocal.
	 * 
	 * @param ctx	the mock context
	 */
	public static void setMockContext(MockFacesContext ctx) {
		setCurrentInstance(ctx);
	}
	
	/**
	 * Default constructor
	 */
	@SuppressWarnings("unchecked")
	public MockFacesContext() {
		this.externalContext = new MockExternalContext();
    }
	
	@Override
	public void addMessage(String clientId, FacesMessage message) {
		System.out.println("Add message for " + clientId + ": " + message.getSummary() + " / " + message.getDetail());
	}

    @Override
	public Application getApplication() {
        if (application == null) {
            ApplicationFactory appFactory = (ApplicationFactory) FactoryFinder.getFactory(FactoryFinder.APPLICATION_FACTORY);
            this.application = appFactory.getApplication();
            // FIXME: this is a really ugly hack for JSF-RI 1.2 - maybe we should use JBoss seam's mock classes?
            try {
                final Field associateField = this.application.getClass().getDeclaredField("associate");
                associateField.setAccessible(true);
                ((ApplicationAssociate) associateField.get(this.application)).setExpressionFactory(new SAXCompiler().createExpressionFactory());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }
        return application;
	}

    @Override
    public ELContext getELContext() {
        if (elContext == null) {
            this.elContext = new ELContextImpl(new ImplicitObjectELResolver());
            this.elContext.putContext(FacesContext.class, this);
            // store required managed beans in el context
            this.elContext.getVariableMapper().setVariable("fxMessageBean",
                    getApplication().getExpressionFactory().createValueExpression(
                            new MessageBean(), MessageBean.class
                    ));
        }
        return elContext;
    }

    @Override
	public Iterator getClientIdsWithMessages() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public ExternalContext getExternalContext() {
		return externalContext;
	}

	@Override
	public Severity getMaximumSeverity() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public Iterator getMessages() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public Iterator getMessages(String clientId) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public RenderKit getRenderKit() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public boolean getRenderResponse() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public boolean getResponseComplete() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public ResponseStream getResponseStream() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public ResponseWriter getResponseWriter() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public UIViewRoot getViewRoot() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void release() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void renderResponse() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void responseComplete() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void setResponseStream(ResponseStream responseStream) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void setResponseWriter(ResponseWriter responseWriter) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void setViewRoot(UIViewRoot root) {
		throw new RuntimeException("not implemented");
	}
	
}