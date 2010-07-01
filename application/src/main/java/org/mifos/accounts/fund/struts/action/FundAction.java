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

package org.mifos.accounts.fund.struts.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.mifos.accounts.fund.struts.actionforms.FundActionForm;
import org.mifos.accounts.fund.util.helpers.FundConstants;
import org.mifos.accounts.fund.servicefacade.FundDto;
import org.mifos.accounts.fund.servicefacade.FundCodeDto;
import org.mifos.accounts.productdefinition.util.helpers.ProductDefinitionConstants;
import org.mifos.application.util.helpers.ActionForwards;
import org.mifos.framework.business.service.BusinessService;
import org.mifos.framework.components.logger.LoggerConstants;
import org.mifos.framework.components.logger.MifosLogManager;
import org.mifos.framework.components.logger.MifosLogger;
import org.mifos.framework.exceptions.ServiceException;
import org.mifos.framework.struts.action.BaseAction;
import org.mifos.framework.util.helpers.CloseSession;
import org.mifos.framework.util.helpers.SessionUtils;
import org.mifos.framework.util.helpers.TransactionDemarcate;
import org.mifos.security.util.ActionSecurity;
import org.mifos.security.util.SecurityConstants;

public class FundAction extends BaseAction {

    private MifosLogger logger = MifosLogManager.getLogger(LoggerConstants.FUNDLOGGER);

    @Override
    protected BusinessService getService() throws ServiceException {
        return null;
    }

    @Override
    protected boolean skipActionFormToBusinessObjectConversion(@SuppressWarnings("unused") String method) {
        return true;
    }

    public static ActionSecurity getSecurity() {
        ActionSecurity security = new ActionSecurity("fundAction");
        security.allow("load", SecurityConstants.FUNDS_CREATE_FUNDS);
        security.allow("create", SecurityConstants.FUNDS_CREATE_FUNDS);
        security.allow("preview", SecurityConstants.VIEW);
        security.allow("previous", SecurityConstants.FUNDS_CREATE_FUNDS);
        security.allow("cancelCreate", SecurityConstants.VIEW);
        security.allow("cancelManage", SecurityConstants.VIEW);
        security.allow("manage", SecurityConstants.FUNDS_EDIT_FUNDS);
        security.allow("previewManage", SecurityConstants.FUNDS_EDIT_FUNDS);
        security.allow("previousManage", SecurityConstants.FUNDS_EDIT_FUNDS);
        security.allow("update", SecurityConstants.FUNDS_EDIT_FUNDS);
        security.allow("viewAllFunds", SecurityConstants.VIEW);
        return security;
    }

    @TransactionDemarcate(saveToken = true)
    public ActionForward load(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        logger.debug("start Load method of Fund Action");
        doCleanUp(request);
        SessionUtils.setCollectionAttribute(FundConstants.ALL_FUNDLIST, this.fundServiceFacade.getFundCodes(), request);
        logger.debug("Load method of Fund Action called");
        return mapping.findForward(ActionForwards.load_success.toString());
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward preview(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        logger.debug("start preview method of loan Product Action");
        return mapping.findForward(ActionForwards.preview_success.toString());
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward previous(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        logger.debug("start previous method of Fund Action");
        return mapping.findForward(ActionForwards.previous_success.toString());
    }

    @TransactionDemarcate(validateAndResetToken = true)
    public ActionForward cancelCreate(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        logger.debug("start cancelCreate method of Fund Action");
        return mapping.findForward(ActionForwards.cancelCreate_success.toString());
    }

    @TransactionDemarcate(validateAndResetToken = true)
    public ActionForward create(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
        logger.debug("start create method of Fund Action");
        FundActionForm fundActionForm = (FundActionForm) form;
        FundDto fundDto = new FundDto();
        fundDto.setCode(getFundCode(fundActionForm.getFundCode(), request));
        fundDto.setName(fundActionForm.getFundName());

        this.fundServiceFacade.createFund(fundDto);

        return mapping.findForward(ActionForwards.create_success.toString());
    }

    @TransactionDemarcate(saveToken = true)
    public ActionForward viewAllFunds(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        SessionUtils.setCollectionAttribute(FundConstants.FUNDLIST, this.fundServiceFacade.getFunds(), request);
        return mapping.findForward(ActionForwards.viewAllFunds_success.toString());
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward manage(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        logger.debug("start manage method of Fund Action");
        FundActionForm fundActionForm = (FundActionForm) form;
        Short fundId = getShortValue(fundActionForm.getFundCodeId());
        FundDto fundDto = this.fundServiceFacade.getFund(fundId);
        SessionUtils.setAttribute(FundConstants.OLDFUNDNAME, fundDto.getName(), request);
        setFormAttributes(fundActionForm, fundDto.getName(), fundDto.getCode().getValue());
        return mapping.findForward(ActionForwards.manage_success.toString());
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward previewManage(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        logger.debug("start editPreview of Fund Action ");
        return mapping.findForward(ActionForwards.previewManage_success.toString());
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward previousManage(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        logger.debug("start editPrevious of Fund Action ");
        return mapping.findForward(ActionForwards.previousManage_success.toString());
    }

    @TransactionDemarcate(validateAndResetToken = true)
    public ActionForward cancelManage(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        logger.debug("start cancelCreate method of Fund Action");
        return mapping.findForward(ActionForwards.cancelManage_success.toString());
    }

    @CloseSession
    @TransactionDemarcate(validateAndResetToken = true)
    public ActionForward update(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        logger.debug("start update method of Fund Action");
        FundActionForm fundActionForm = (FundActionForm) form;
        FundDto fundDto = new FundDto();
        fundDto.setId(fundActionForm.getFundCodeId());
        fundDto.setName(fundActionForm.getFundName());

        this.fundServiceFacade.updateFund(fundDto);

        return mapping.findForward(ActionForwards.update_success.toString());
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward validate(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String method = (String) request.getAttribute(ProductDefinitionConstants.METHODCALLED);
        logger.debug("start validate method of Fund Action" + method);
        if (method != null) {
            return mapping.findForward(method + "_failure");
        }
        return mapping.findForward(ActionForwards.preview_failure.toString());
    }

    private void doCleanUp(HttpServletRequest request) {
        SessionUtils.setAttribute(FundConstants.FUND_ACTIONFORM, null, request.getSession());
    }

    private void setFormAttributes(FundActionForm actionForm, String fundName, String fundCodeValue) {
        actionForm.setFundName(fundName);
        actionForm.setFundCode(fundCodeValue);
    }

    private FundCodeDto getFundCode(String fundCode, HttpServletRequest request) throws Exception {
        FundCodeDto fundCodeDto = new FundCodeDto();
        fundCodeDto.setId(fundCode);
        return fundCodeDto;
    }
}
