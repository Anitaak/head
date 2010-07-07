/*
 * Copyright (c) 2005-2010 Grameen Foundation USA
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * See also http://www.apache.org/licenses/LICENSE-2.0.html for an
 * explanation of the license and how it is applied.
 */

package org.mifos.security.login.struts.action;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.mifos.application.admin.system.ShutdownManager;
import org.mifos.application.servicefacade.DependencyInjectedServiceLocator;
import org.mifos.application.servicefacade.LoginActivityDto;
import org.mifos.application.servicefacade.LegacyLoginServiceFacade;
import org.mifos.application.util.helpers.ActionForwards;
import org.mifos.application.util.helpers.Methods;
import org.mifos.customers.personnel.business.PersonnelBO;
import org.mifos.customers.personnel.business.service.PersonnelBusinessService;
import org.mifos.framework.business.service.BusinessService;
import org.mifos.framework.components.logger.LoggerConstants;
import org.mifos.framework.components.logger.MifosLogManager;
import org.mifos.framework.components.logger.MifosLogger;
import org.mifos.framework.exceptions.ApplicationException;
import org.mifos.framework.exceptions.ServiceException;
import org.mifos.framework.hibernate.helper.StaticHibernateUtil;
import org.mifos.framework.struts.action.BaseAction;
import org.mifos.framework.util.helpers.Constants;
import org.mifos.framework.util.helpers.FilePaths;
import org.mifos.framework.util.helpers.FlowManager;
import org.mifos.framework.util.helpers.ServletUtils;
import org.mifos.framework.util.helpers.SessionUtils;
import org.mifos.framework.util.helpers.TransactionDemarcate;
import org.mifos.security.login.struts.actionforms.LoginActionForm;
import org.mifos.security.login.util.helpers.LoginConstants;
import org.mifos.security.util.ActionSecurity;
import org.mifos.security.util.SecurityConstants;
import org.mifos.security.util.UserContext;

public class LoginAction extends BaseAction {

    private static final MifosLogger loginLogger = MifosLogManager.getLogger(LoggerConstants.LOGINLOGGER);
    private final LegacyLoginServiceFacade loginServiceFacade = DependencyInjectedServiceLocator.locationLoginServiceFacade();

    @Override
    protected boolean skipActionFormToBusinessObjectConversion(@SuppressWarnings("unused") String method) {
        return true;
    }

    @Override
    protected BusinessService getService() throws ServiceException {
        return getPersonnelBizService();
    }

    public static ActionSecurity getSecurity() {
        ActionSecurity security = new ActionSecurity("loginAction");
        security.allow("login", SecurityConstants.VIEW);
        security.allow("logout", SecurityConstants.VIEW);
        security.allow("updatePassword", SecurityConstants.VIEW);
        return security;
    }

    public ActionForward load(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
        loginLogger.debug("Inside load of LoginAction");
        SessionUtils.setAttribute(LoginConstants.LOGINACTIONFORM, null, request.getSession());
        request.getSession(false).setAttribute(Constants.FLOWMANAGER, new FlowManager());
        return mapping.findForward(ActionForwards.load_success.toString());
    }

    @TransactionDemarcate(saveToken = true)
    public ActionForward login(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
        loginLogger.debug("Inside login of LoginAction");
        loginLogger.debug("Using Thread: " + Thread.currentThread().getName());
        loginLogger.debug("Using hibernate session: " + StaticHibernateUtil.getSessionTL().hashCode());

        ShutdownManager shutdownManager = (ShutdownManager) ServletUtils.getGlobal(request, ShutdownManager.class
                .getName());
        if (shutdownManager.isInShutdownCountdownNotificationThreshold()) {
            request.getSession(false).invalidate();
            ActionErrors error = new ActionErrors();
            error.add(LoginConstants.SHUTDOWN, new ActionMessage(LoginConstants.SHUTDOWN));
            request.setAttribute(Globals.ERROR_KEY, error);
            return mapping.findForward(ActionForwards.load_main_page.toString());
        }

        LoginActionForm loginActionForm = (LoginActionForm) form;
        String userName = loginActionForm.getUserName();
        String password = loginActionForm.getPassword();

        LoginActivityDto loginActivity = loginServiceFacade.login(userName, password);

        request.getSession(false).setAttribute(Constants.ACTIVITYCONTEXT, loginActivity.getActivityContext());

        UserContext userContext = loginActivity.getUserContext();
        if (loginActivity.isPasswordChanged()) {
            setUserContextInSession(userContext, request);
        } else {
            SessionUtils.setAttribute(Constants.TEMPUSERCONTEXT, userContext, request);
        }

        //  set flow
        Short passwordChanged = loginActivity.getPasswordChangedFlag();
        if (null != passwordChanged && LoginConstants.PASSWORDCHANGEDFLAG.equals(passwordChanged)) {
            FlowManager flowManager = (FlowManager) request.getSession().getAttribute(Constants.FLOWMANAGER);
            flowManager.removeFlow((String) request.getAttribute(Constants.CURRENTFLOWKEY));
            request.setAttribute(Constants.CURRENTFLOWKEY, null);
        }

        final String loginForward = getLoginForward(loginActivity.getPasswordChangedFlag());

        return mapping.findForward(loginForward);
    }

    public ActionForward logout(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {

        loginLogger.debug("Inside logout of LoginAction");

        ResourceBundle resources;
        UserContext userContext = getUserContext(request);
        if (null == userContext) {
            // user might have just been given an empty session, so we
            // can't assume that their session has a preferred locale
            resources = ResourceBundle.getBundle(FilePaths.LOGIN_UI_PROPERTY_FILE);
        } else {
            // get locale first
            Locale locale = userContext.getPreferredLocale();
            resources = ResourceBundle.getBundle(FilePaths.LOGIN_UI_PROPERTY_FILE, locale);
        }

        request.getSession(false).invalidate();
        ActionErrors error = new ActionErrors();

        String errorMessage = resources.getString(LoginConstants.LOGOUTOUT);

        // ActionMessage: take errorMessage as literal
        error.add(LoginConstants.LOGOUTOUT, new ActionMessage(errorMessage, false));

        request.setAttribute(Globals.ERROR_KEY, error);
        return mapping.findForward(ActionForwards.logout_success.toString());
    }

    @TransactionDemarcate(validateAndResetToken = true)
    public ActionForward updatePassword(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
        loginLogger.debug("Inside updatePassword of LoginAction");
        LoginActionForm loginActionForm = (LoginActionForm) form;
        UserContext userContext = null;
        String userName = loginActionForm.getUserName();
        if (null == userName || "".equals(userName)) {
            throw new ApplicationException(LoginConstants.SESSIONTIMEOUT);
        }
        String oldPassword = loginActionForm.getOldPassword();
        String newpassword = loginActionForm.getNewPassword();
        PersonnelBO personnelBO = getPersonnelBizService().getPersonnel(userName);
        if (personnelBO.isPasswordChanged()) {
            userContext = (UserContext) SessionUtils.getAttribute(Constants.USERCONTEXT, request.getSession());
        } else {
            userContext = (UserContext) SessionUtils.getAttribute(Constants.TEMPUSERCONTEXT, request);
        }
        PersonnelBO personnelInit = ((PersonnelBusinessService) getService()).getPersonnel(Short.valueOf(personnelBO
                .getPersonnelId()));
        checkVersionMismatch(personnelBO.getVersionNo(), personnelInit.getVersionNo());
        personnelInit.setVersionNo(personnelBO.getVersionNo());
        personnelInit.setUserContext(userContext);
        setInitialObjectForAuditLogging(personnelInit);
        personnelInit.updatePassword(oldPassword, newpassword, userContext.getId());
        setUserContextInSession(userContext, request);
        personnelBO = null;
        personnelInit = null;
        return mapping.findForward(ActionForwards.updatePassword_success.toString());
    }

    @TransactionDemarcate(validateAndResetToken = true)
    public ActionForward cancel(ActionMapping mapping, ActionForm form, @SuppressWarnings("unused") HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
        PersonnelBO personnelBO = getPersonnelBizService().getPersonnel(((LoginActionForm) form).getUserName());
        return mapping.findForward(getCancelForward(personnelBO.getPasswordChanged()));
    }

    public ActionForward validate(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse httpservletresponse) throws Exception {
        String method = (String) request.getAttribute("methodCalled");
        if (method.equalsIgnoreCase(Methods.login.toString())) {
            return mapping.findForward(ActionForwards.login_failure.toString());
        }
        if (method.equalsIgnoreCase(Methods.updatePassword.toString())) {
            return mapping.findForward(ActionForwards.updatePassword_failure.toString());
        }
        return null;
    }

    private PersonnelBusinessService getPersonnelBizService() {
        return new PersonnelBusinessService();
    }

    private void setUserContextInSession(UserContext userContext, HttpServletRequest request) {
        HttpSession hs = request.getSession(false);
        hs.setAttribute(Constants.USERCONTEXT, userContext);
        hs.setAttribute("org.apache.struts.action.LOCALE", userContext.getCurrentLocale());
    }

    private String getLoginForward(Short passwordChanged) {
        return (null == passwordChanged || LoginConstants.FIRSTTIMEUSER.equals(passwordChanged)) ? ActionForwards.loadChangePassword_success
                .toString()
                : ActionForwards.login_success.toString();
    }

    private String getCancelForward(Short passwordChanged) {
        return (null == passwordChanged || LoginConstants.FIRSTTIMEUSER.equals(passwordChanged)) ? ActionForwards.cancel_success
                .toString()
                : ActionForwards.updateSettings_success.toString();
    }
}