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

package org.mifos.customers.personnel.struts.action;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.joda.time.DateTime;
import org.mifos.application.admin.servicefacade.InvalidDateException;
import org.mifos.application.admin.servicefacade.PersonnelServiceFacade;
import org.mifos.application.master.business.CustomFieldDefinitionEntity;
import org.mifos.application.master.business.CustomFieldType;
import org.mifos.application.master.business.MasterDataEntity;
import org.mifos.application.master.business.SupportedLocalesEntity;
import org.mifos.application.master.persistence.MasterPersistence;
import org.mifos.application.master.util.helpers.MasterConstants;
import org.mifos.application.questionnaire.struts.DefaultQuestionnaireServiceFacadeLocator;
import org.mifos.application.questionnaire.struts.QuestionnaireFlowAdapter;
import org.mifos.application.servicefacade.DependencyInjectedServiceLocator;
import org.mifos.application.util.helpers.ActionForwards;
import org.mifos.application.util.helpers.EntityType;
import org.mifos.config.Localization;
import org.mifos.customers.office.business.OfficeBO;
import org.mifos.customers.office.business.service.OfficeBusinessService;
import org.mifos.customers.office.util.helpers.OfficeLevel;
import org.mifos.customers.personnel.business.PersonnelBO;
import org.mifos.customers.personnel.business.PersonnelCustomFieldEntity;
import org.mifos.customers.personnel.business.PersonnelDetailsEntity;
import org.mifos.customers.personnel.business.PersonnelLevelEntity;
import org.mifos.customers.personnel.business.PersonnelRoleEntity;
import org.mifos.customers.personnel.business.PersonnelStatusEntity;
import org.mifos.customers.personnel.business.service.PersonnelBusinessService;
import org.mifos.customers.personnel.exceptions.PersonnelException;
import org.mifos.customers.personnel.persistence.PersonnelPersistence;
import org.mifos.customers.personnel.struts.actionforms.PersonActionForm;
import org.mifos.customers.personnel.util.helpers.PersonnelConstants;
import org.mifos.customers.personnel.util.helpers.PersonnelLevel;
import org.mifos.customers.personnel.util.helpers.PersonnelStatus;
import org.mifos.customers.util.helpers.CustomerConstants;
import org.mifos.dto.domain.AddressDto;
import org.mifos.dto.domain.CreateOrUpdatePersonnelInformation;
import org.mifos.dto.domain.CustomFieldDto;
import org.mifos.dto.domain.UserDetailDto;
import org.mifos.dto.screen.DefinePersonnelDto;
import org.mifos.dto.screen.ListElement;
import org.mifos.dto.screen.PersonnelInformationDto;
import org.mifos.framework.business.service.BusinessService;
import org.mifos.framework.business.service.ServiceFactory;
import org.mifos.framework.business.util.Address;
import org.mifos.framework.business.util.Name;
import org.mifos.framework.components.tabletag.TableTagConstants;
import org.mifos.framework.exceptions.ApplicationException;
import org.mifos.framework.exceptions.PageExpiredException;
import org.mifos.framework.exceptions.ServiceException;
import org.mifos.framework.exceptions.SystemException;
import org.mifos.framework.struts.action.SearchAction;
import org.mifos.framework.util.DateTimeService;
import org.mifos.framework.util.helpers.BusinessServiceName;
import org.mifos.framework.util.helpers.CloseSession;
import org.mifos.framework.util.helpers.Constants;
import org.mifos.framework.util.helpers.DateUtils;
import org.mifos.framework.util.helpers.SessionUtils;
import org.mifos.framework.util.helpers.TransactionDemarcate;
import org.mifos.security.MifosUser;
import org.mifos.security.login.util.helpers.LoginConstants;
import org.mifos.security.rolesandpermission.business.RoleBO;
import org.mifos.security.util.ActionSecurity;
import org.mifos.security.util.SecurityConstants;
import org.mifos.security.util.UserContext;
import org.mifos.service.BusinessRuleException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetails;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.mifos.accounts.loan.util.helpers.LoanConstants.METHODCALLED;

public class PersonAction extends SearchAction {

    private final PersonnelServiceFacade personnelServiceFacade = DependencyInjectedServiceLocator.locatePersonnelServiceFacade();

    @Override
    protected BusinessService getService() throws ServiceException {
        return ServiceFactory.getInstance().getBusinessService(BusinessServiceName.Personnel);
    }

    @Override
    protected boolean skipActionFormToBusinessObjectConversion(String method) {
        return true;
    }

    private PersonnelBusinessService getPersonnelBusinessService() throws ServiceException {
        return (PersonnelBusinessService) getService();
    }

    public static ActionSecurity getSecurity() {
        ActionSecurity security = new ActionSecurity("PersonAction");
        security.allow("get", SecurityConstants.VIEW);
        security.allow("loadSearch", SecurityConstants.VIEW);
        security.allow("search", SecurityConstants.VIEW);
        security.allow("chooseOffice", SecurityConstants.PERSONNEL_CREATE_PERSONNEL);
        security.allow("load", SecurityConstants.PERSONNEL_CREATE_PERSONNEL);
        security.allow("manage", SecurityConstants.PERSONNEL_EDIT_SELF_INFO);
        security.allow("previewManage", SecurityConstants.VIEW);

        security.allow("previousManage", SecurityConstants.PERSONNEL_EDIT_SELF_INFO);
        security.allow("update", SecurityConstants.PERSONNEL_EDIT_PERSONNEL);
        security.allow("/PersonnelAction-prevPersonalInfo", SecurityConstants.VIEW);

        security.allow("preview", SecurityConstants.PERSONNEL_CREATE_PERSONNEL);
        security.allow("previous", SecurityConstants.PERSONNEL_CREATE_PERSONNEL);
        security.allow("create", SecurityConstants.PERSONNEL_CREATE_PERSONNEL);
        security.allow("loadUnLockUser", SecurityConstants.PERSONNEL_UNLOCK_PERSONNEL);
        security.allow("unLockUserAccount", SecurityConstants.PERSONNEL_UNLOCK_PERSONNEL);
        security.allow("loadChangeLog", SecurityConstants.VIEW);
        security.allow("cancelChangeLog", SecurityConstants.VIEW);
        security.allow("captureQuestionResponses", SecurityConstants.VIEW);
        security.allow("editQuestionResponses", SecurityConstants.VIEW);
        return security;
    }

    @SuppressWarnings("unused")
    @TransactionDemarcate(saveToken = true)
    public ActionForward chooseOffice(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        return mapping.findForward(ActionForwards.chooseOffice_success.toString());
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward load(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
        PersonActionForm personActionForm = (PersonActionForm) form;
        OfficeBO office = ((PersonnelBusinessService) getService()).getOffice(getShortValue(personActionForm.getOfficeId()));
        SessionUtils.setAttribute(PersonnelConstants.OFFICE, office, request);
        personActionForm.clear();
        //Shahid - keeping the previous session objects for the sake of existing tests, once fully converted to spring
        //then we can get rid of the session objects made redundant by the dto
        DefinePersonnelDto definePersonnelDto = this.personnelServiceFacade.retrieveInfoForNewUserDefinition(
                                                    getShortValue(personActionForm.getOfficeId()), getUserContext(request).getPreferredLocale());
        SessionUtils.setAttribute("definePersonnelDto", definePersonnelDto, request);
        loadCreateMasterData(request, personActionForm);
        if (office.getOfficeLevel() != OfficeLevel.BRANCHOFFICE) {
            updatePersonnelLevelList(request);
        }
        personActionForm.setCustomFields(definePersonnelDto.getCustomFields());
        personActionForm.setDateOfJoiningMFI(DateUtils.makeDateAsSentFromBrowser());
        return mapping.findForward(ActionForwards.load_success.toString());
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward preview(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {

        PersonActionForm personActionForm = (PersonActionForm) form;
        UserContext userContext = (UserContext) SessionUtils.getAttribute(Constants.USER_CONTEXT_KEY, request
                .getSession());
        personActionForm.setDateOfJoiningBranch(DateUtils.getCurrentDate(userContext.getPreferredLocale()));
        updateRoleLists(request, (PersonActionForm) form);

        return createGroupQuestionnaire.fetchAppliedQuestions(mapping, personActionForm, request, ActionForwards.preview_success);
    }

    @SuppressWarnings("unused")
    @TransactionDemarcate(joinToken = true)
    public ActionForward previous(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        PersonActionForm personActionForm = (PersonActionForm) form;
        personActionForm.setPersonnelRoles(null);
        return mapping.findForward(ActionForwards.previous_success.toString());
    }

    @TransactionDemarcate(validateAndResetToken = true)
    public ActionForward create(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {

        PersonActionForm personActionForm = (PersonActionForm) form;
        CreateOrUpdatePersonnelInformation perosonnelInfo = translateFormToCreatePersonnelInformationDto(request, personActionForm);

        try {
            UserDetailDto userDetails = this.personnelServiceFacade.createPersonnelInformation(perosonnelInfo);

            String globalPersonnelNum = userDetails.getSystemId();
            Name name = new Name(personActionForm.getFirstName(), personActionForm.getMiddleName(), personActionForm.getSecondLastName(), personActionForm.getLastName());

            request.setAttribute("displayName", name.getDisplayName());
            request.setAttribute("globalPersonnelNum", globalPersonnelNum);
            createGroupQuestionnaire.saveResponses(request, personActionForm, userDetails.getId());
            return mapping.findForward(ActionForwards.create_success.toString());
        } catch (BusinessRuleException e) {
            throw new PersonnelException(e.getMessageKey(), e);
        }
    }

    private CreateOrUpdatePersonnelInformation translateFormToCreatePersonnelInformationDto(HttpServletRequest request,
            PersonActionForm personActionForm) throws PageExpiredException, ServiceException, InvalidDateException {
        UserContext userContext = getUserContext(request);
        PersonnelLevel level = PersonnelLevel.fromInt(getShortValue(personActionForm.getLevel()));

        PersonnelStatus personnelStatus = PersonnelStatus.ACTIVE;
        if (StringUtils.isNotBlank(personActionForm.getStatus())) {
            personnelStatus = PersonnelStatus.getPersonnelStatus(getShortValue(personActionForm.getStatus()));
        }

        OfficeBO office = (OfficeBO) SessionUtils.getAttribute(PersonnelConstants.OFFICE, request);
        if (office == null) {
            OfficeBusinessService officeService = (OfficeBusinessService) ServiceFactory.getInstance().getBusinessService(
                    BusinessServiceName.Office);
            office = officeService.getOffice(getShortValue(personActionForm.getOfficeId()));
        }

        Integer title = getIntegerValue(personActionForm.getTitle());
        Short preferredLocale = getLocaleId(getPerefferedLocale(personActionForm, userContext));
        Date dob = null;
        if (personActionForm.getDob() != null && !personActionForm.getDob().equals("")) {
            dob = DateUtils.getDate(personActionForm.getDob());
        }
        Date dateOfJoiningMFI = null;

        if (personActionForm.getDateOfJoiningMFI() != null && !personActionForm.getDateOfJoiningMFI().equals("")) {
            dateOfJoiningMFI = DateUtils.getDateAsSentFromBrowser(personActionForm.getDateOfJoiningMFI());
        }

        List<RoleBO> roles = getRoles(request, personActionForm);
        List<ListElement> roleList = new ArrayList<ListElement>();
        if (roles != null) {
            for (RoleBO element : roles) {
                ListElement listElement = new ListElement(new Integer(element.getId()), element.getName());
                roleList.add(listElement);
            }
        }
        Address address = personActionForm.getAddress();
        AddressDto addressDto = new AddressDto(address.getLine1(), address.getLine2(), address.getLine3(), address.getCity(), address.getState(),
                address.getCountry(), address.getZip(), address.getPhoneNumber());

        Long id = null;
        if (StringUtils.isNotBlank(personActionForm.getPersonnelId())) {
            id = Long.valueOf(personActionForm.getPersonnelId());
        }

        CreateOrUpdatePersonnelInformation perosonnelInfo = new CreateOrUpdatePersonnelInformation(id, level.getValue(), office.getOfficeId(), title,
                preferredLocale, personActionForm.getUserPassword(), personActionForm.getLoginName(), personActionForm.getEmailId(), roleList,
                personActionForm.getCustomFields(), personActionForm.getFirstName(), personActionForm.getMiddleName(), personActionForm.getLastName(),
                personActionForm.getSecondLastName(), personActionForm.getGovernmentIdNumber(), new DateTime(dob),
                getIntegerValue(personActionForm.getMaritalStatus()), getIntegerValue(personActionForm.getGender()), new DateTime(dateOfJoiningMFI),
                new DateTimeService().getCurrentDateTime(), addressDto, personnelStatus.getValue());
        return perosonnelInfo;
    }

    private Short getPerefferedLocale(PersonActionForm personActionForm, UserContext userContext) {
        if (StringUtils.isNotBlank(personActionForm.getPreferredLocale())) {
            return getShortValue(personActionForm.getPreferredLocale());
        }

        return userContext.getLocaleId();
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward manage(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
        PersonActionForm actionform = (PersonActionForm) form;
        actionform.clear();
        PersonnelBO personnel = (PersonnelBO) SessionUtils.getAttribute(Constants.BUSINESS_KEY, request);
        PersonnelBO personnelBO = ((PersonnelBusinessService) getService()).getPersonnel(personnel.getPersonnelId());
        personnel = null;
        SessionUtils.setAttribute(Constants.BUSINESS_KEY, personnelBO, request);
        loadUpdateMasterData(request);
        setValuesInActionForm(actionform, request);
        return mapping.findForward(ActionForwards.manage_success.toString());
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward previewManage(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
        PersonActionForm actionForm = (PersonActionForm) form;
        updateRoleLists(request, actionForm);
        return mapping.findForward(ActionForwards.previewManage_success.toString());
    }

    @SuppressWarnings("unused")
    @TransactionDemarcate(joinToken = true)
    public ActionForward previousManage(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        PersonActionForm personActionForm = (PersonActionForm) form;
        personActionForm.setPersonnelRoles(null);
        return mapping.findForward(ActionForwards.previousManage_success.toString());
    }

    @CloseSession
    @TransactionDemarcate(validateAndResetToken = true)
    public ActionForward update(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
        PersonActionForm actionForm = (PersonActionForm) form;

        try {
            CreateOrUpdatePersonnelInformation perosonnelInfo = translateFormToCreatePersonnelInformationDto(request, actionForm);
            UserDetailDto userDetails = this.personnelServiceFacade.updatePersonnel(perosonnelInfo);

            String globalPersonnelNum = userDetails.getSystemId();
            Name name = new Name(actionForm.getFirstName(), actionForm.getMiddleName(), actionForm.getSecondLastName(),
                    actionForm.getLastName());
            request.setAttribute("displayName", name.getDisplayName());
            request.setAttribute("globalPersonnelNum", globalPersonnelNum);

            reloadUserDetailsForSecurityContext(perosonnelInfo.getUserName());

            return mapping.findForward(ActionForwards.update_success.toString());
        } catch (BusinessRuleException e) {
            throw new PersonnelException(e.getMessageKey(), e);
        }
    }

    private void reloadUserDetailsForSecurityContext(String username) {
        UserDetails userSecurityDetails = this.authenticationAuthorizationServiceFacade.loadUserByUsername(username);
        MifosUser reloadedUserDetails = (MifosUser) userSecurityDetails;

        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext == null) {
            securityContext = new SecurityContextImpl();
            SecurityContextHolder.setContext(securityContext);
        }
        Authentication authentication = new UsernamePasswordAuthenticationToken(reloadedUserDetails, reloadedUserDetails, reloadedUserDetails.getAuthorities());
        securityContext.setAuthentication(authentication);
    }

    @SuppressWarnings("unused")
    @TransactionDemarcate(joinToken = true)
    public ActionForward validate(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String method = (String) request.getAttribute("methodCalled");
        return mapping.findForward(method + "_failure");
    }

    @SuppressWarnings("unused")
    @TransactionDemarcate(validateAndResetToken = true)
    public ActionForward cancel(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String forward = null;
        PersonActionForm actionForm = (PersonActionForm) form;
        String fromPage = actionForm.getInput();
        if (fromPage.equals(PersonnelConstants.MANAGE_USER) || fromPage.equals(PersonnelConstants.UNLOCK_USER)) {
            forward = ActionForwards.cancelEdit_success.toString();
        } else {
            forward = ActionForwards.cancel_success.toString();
        }
        return mapping.findForward(forward.toString());
    }

    @TransactionDemarcate(saveToken = true)
    public ActionForward get(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {

        PersonActionForm personActionForm = (PersonActionForm) form;

        String personnelId = ((PersonActionForm) form).getPersonnelId();
        Long userId = null;
        if (personnelId != null) {
            userId = Long.valueOf(personnelId);
        }
        PersonnelInformationDto personnelInformationDto = this.personnelServiceFacade.getPersonnelInformationDto(userId, ((PersonActionForm) form).getGlobalPersonnelNum());

        SessionUtils.removeThenSetAttribute("personnelInformationDto", personnelInformationDto, request);

        // John W - for other actions downstream (like edit) business_key set (until all actions refactored)
        PersonnelBO personnelBO = ((PersonnelBusinessService) getService()).getPersonnel(personnelInformationDto.getPersonnelId());
        SessionUtils.removeThenSetAttribute(Constants.BUSINESS_KEY, personnelBO, request);
        setCurrentPageUrl(request, personnelBO);
        loadCreateMasterData(request, personActionForm);
        return mapping.findForward(ActionForwards.get_success.toString());
    }

    private void setCurrentPageUrl(HttpServletRequest request, PersonnelBO personnelBO) throws PageExpiredException, UnsupportedEncodingException {
        SessionUtils.removeThenSetAttribute("currentPageUrl", constructCurrentPageUrl(personnelBO), request);
    }

    private String constructCurrentPageUrl(PersonnelBO personnelBO) throws UnsupportedEncodingException {
        String url = String.format("PersonAction.do?globalPersonnelNum=%s",
                personnelBO.getGlobalPersonnelNum());
        return URLEncoder.encode(url, "UTF-8");
    }

    @SuppressWarnings("unused")
    @TransactionDemarcate(joinToken = true)
    public ActionForward loadUnLockUser(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        SessionUtils.setAttribute(PersonnelConstants.LOGIN_ATTEMPTS_COUNT, LoginConstants.MAXTRIES, request);
        return mapping.findForward(ActionForwards.loadUnLockUser_success.toString());
    }

    @SuppressWarnings("unused")
    @TransactionDemarcate(validateAndResetToken = true)
    public ActionForward unLockUserAccount(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        PersonnelBO personnel = (PersonnelBO) SessionUtils.getAttribute(Constants.BUSINESS_KEY, request);
        UserContext userContext = (UserContext) SessionUtils.getAttribute(Constants.USER_CONTEXT_KEY, request
                .getSession());
        personnel.unlockPersonnel(userContext.getId());
        return mapping.findForward(ActionForwards.unLockUserAccount_success.toString());
    }

    @Override
    @TransactionDemarcate(saveToken = true)
    public ActionForward search(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        ActionForward actionForward = null;
        UserContext userContext = getUserContext(request);
        PersonnelBusinessService personnelBusinessService = getPersonnelBusinessService();
        PersonnelBO personnel = personnelBusinessService.getPersonnel(userContext.getId());
        String searchString = ((PersonActionForm) form).getSearchString();
        addSeachValues(searchString, personnel.getOffice().getOfficeId().toString(), personnel.getOffice()
                .getOfficeName(), request);
        searchString = org.mifos.framework.util.helpers.SearchUtils.normalizeSearchString(searchString);
        actionForward = super.search(mapping, form, request, response);
        SessionUtils.setQueryResultAttribute(Constants.SEARCH_RESULTS, new PersonnelPersistence().search(searchString,
                userContext.getId()), request);
        return actionForward;
    }

    @SuppressWarnings("unused")
    @TransactionDemarcate(saveToken = true)
    public ActionForward loadSearch(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        cleanUpOnLoadSearch(request);
        return mapping.findForward(ActionForwards.search_success.toString());
    }

    private void cleanUpOnLoadSearch(HttpServletRequest request) throws PageExpiredException {
        SessionUtils.setRemovableAttribute("TableCache", null, TableTagConstants.PATH, request.getSession());
        SessionUtils.removeAttribute(Constants.SEARCH_RESULTS, request);
    }

    private void loadMasterData(HttpServletRequest request) throws Exception {
        UserContext userContext = getUserContext(request);
        MasterPersistence masterPersistence = new MasterPersistence();

        SessionUtils.setCollectionAttribute(PersonnelConstants.TITLE_LIST, masterPersistence.retrieveMasterEntities(
                MasterConstants.PERSONNEL_TITLE, userContext.getLocaleId()), request);

        SessionUtils.setCollectionAttribute(PersonnelConstants.PERSONNEL_LEVEL_LIST, masterPersistence
                .retrieveMasterEntities(PersonnelLevelEntity.class, userContext.getLocaleId()), request);

        SessionUtils.setCollectionAttribute(PersonnelConstants.GENDER_LIST, masterPersistence.retrieveMasterEntities(
                MasterConstants.GENDER, userContext.getLocaleId()), request);

        SessionUtils.setCollectionAttribute(PersonnelConstants.MARITAL_STATUS_LIST, masterPersistence
                .retrieveMasterEntities(MasterConstants.MARITAL_STATUS, userContext.getLocaleId()), request);

        SessionUtils.setCollectionAttribute(PersonnelConstants.LANGUAGE_LIST, masterPersistence.retrieveMasterEntities(
                MasterConstants.LANGUAGE, userContext.getLocaleId()), request);

        SessionUtils.setCollectionAttribute(PersonnelConstants.ROLES_LIST, ((PersonnelBusinessService) getService())
                .getRoles(), request);

        SessionUtils.setCollectionAttribute(PersonnelConstants.ROLEMASTERLIST,
                ((PersonnelBusinessService) getService()).getRoles(), request);

        List<CustomFieldDefinitionEntity> customFieldDefs = masterPersistence
                .retrieveCustomFieldsDefinition(EntityType.PERSONNEL);
        SessionUtils.setCollectionAttribute(CustomerConstants.CUSTOM_FIELDS_LIST, customFieldDefs, request);
    }

    @SuppressWarnings("unchecked")
    private void updatePersonnelLevelList(HttpServletRequest request) throws PageExpiredException {
        List<MasterDataEntity> levelList = (List<MasterDataEntity>) SessionUtils.getAttribute(PersonnelConstants.PERSONNEL_LEVEL_LIST, request);
        for (MasterDataEntity level : levelList) {
            if (level.getId().equals(PersonnelLevel.LOAN_OFFICER.getValue())) {
                levelList.remove(level);
                break;
            }
        }

    }

    @SuppressWarnings("unchecked")
    private void loadCreateMasterData(HttpServletRequest request, PersonActionForm personActionForm) throws Exception {
        loadMasterData(request);
        List<CustomFieldDefinitionEntity> customFieldDefs = (List<CustomFieldDefinitionEntity>) SessionUtils.getAttribute(CustomerConstants.CUSTOM_FIELDS_LIST, request);
        loadCreateCustomFields(personActionForm, customFieldDefs, getUserContext(request));
    }


    private void loadUpdateMasterData(HttpServletRequest request) throws Exception {
        loadMasterData(request);
        UserContext userContext = (UserContext) SessionUtils.getAttribute(Constants.USER_CONTEXT_KEY, request
                .getSession());
        SessionUtils.setCollectionAttribute(PersonnelConstants.STATUS_LIST, getMasterEntities(
                PersonnelStatusEntity.class, getUserContext(request).getLocaleId()), request);
        OfficeBusinessService officeService = (OfficeBusinessService) ServiceFactory.getInstance().getBusinessService(BusinessServiceName.Office);
        OfficeBO loggedInOffice = officeService.getOffice(userContext.getBranchId());
        SessionUtils.setCollectionAttribute(PersonnelConstants.OFFICE_LIST, officeService.getChildOffices(loggedInOffice.getSearchId()), request);
    }

    private void loadCreateCustomFields(PersonActionForm actionForm, List<CustomFieldDefinitionEntity> customFieldDefs,
            UserContext userContext) throws SystemException {
        List<CustomFieldDto> customFields = new ArrayList<CustomFieldDto>();

        for (CustomFieldDefinitionEntity fieldDef : customFieldDefs) {
            if (StringUtils.isNotBlank(fieldDef.getDefaultValue())
                    && fieldDef.getFieldType().equals(CustomFieldType.DATE.getValue())) {
                customFields.add(new CustomFieldDto(fieldDef.getFieldId(), DateUtils.getUserLocaleDate(userContext
                        .getPreferredLocale(), fieldDef.getDefaultValue()), fieldDef.getFieldType()));
            } else {
                customFields.add(new CustomFieldDto(fieldDef.getFieldId(), fieldDef.getDefaultValue(), fieldDef
                        .getFieldType()));
            }
        }
        actionForm.setCustomFields(customFields);
    }

    private void setValuesInActionForm(PersonActionForm actionForm, HttpServletRequest request) throws Exception {
        PersonnelBO personnel = (PersonnelBO) SessionUtils.getAttribute(Constants.BUSINESS_KEY, request);
        actionForm.setPersonnelId(personnel.getPersonnelId().toString());
        if (personnel.getOffice() != null) {
            actionForm.setOfficeId(personnel.getOffice().getOfficeId().toString());
        }
        if (personnel.getTitle() != null) {
            actionForm.setTitle(personnel.getTitle().toString());
        }
        if (personnel.getLevel() != null) {
            actionForm.setLevel(personnel.getLevelEnum().getValue().toString());
        }
        if (personnel.getStatus() != null) {
            actionForm.setStatus(personnel.getStatus().getId().toString());
        }
        actionForm.setLoginName(personnel.getUserName());
        actionForm.setGlobalPersonnelNum(personnel.getGlobalPersonnelNum());
        actionForm.setCustomFields(createCustomFieldViews(personnel.getCustomFields(), request));

        if (personnel.getPersonnelDetails() != null) {
            PersonnelDetailsEntity personnelDetails = personnel.getPersonnelDetails();
            actionForm.setFirstName(personnelDetails.getName().getFirstName());
            actionForm.setMiddleName(personnelDetails.getName().getMiddleName());
            actionForm.setSecondLastName(personnelDetails.getName().getSecondLastName());
            actionForm.setLastName(personnelDetails.getName().getLastName());
            if (personnelDetails.getGender() != null) {
                actionForm.setGender(personnelDetails.getGender().toString());
            }
            if (personnelDetails.getMaritalStatus() != null) {
                actionForm.setMaritalStatus(personnelDetails.getMaritalStatus().toString());
            }
            actionForm.setAddress(personnelDetails.getAddress());
            if (personnelDetails.getDateOfJoiningMFI() != null) {
                actionForm.setDateOfJoiningMFI(DateUtils.makeDateAsSentFromBrowser(personnelDetails
                        .getDateOfJoiningMFI()));
            }
            if (personnelDetails.getDob() != null) {
                actionForm.setDob(DateUtils.makeDateAsSentFromBrowser(personnelDetails.getDob()));
            }

        }
        actionForm.setEmailId(personnel.getEmailId());
        if (personnel.getPreferredLocale() != null) {
            actionForm.setPreferredLocale(getStringValue(personnel.getPreferredLocale().getLanguage().getLookUpValue().getLookUpId()));
        }
        List<RoleBO> selectList = new ArrayList<RoleBO>();
        for (PersonnelRoleEntity personnelRole : personnel.getPersonnelRoles()) {
            selectList.add(personnelRole.getRole());
        }
        SessionUtils.setCollectionAttribute(PersonnelConstants.PERSONNEL_ROLES_LIST, selectList, request);
    }

    @SuppressWarnings("unchecked")
    private List<CustomFieldDto> createCustomFieldViews(Set<PersonnelCustomFieldEntity> customFieldEntities,
            HttpServletRequest request) throws ApplicationException {
        List<CustomFieldDto> customFields = new ArrayList<CustomFieldDto>();

        List<CustomFieldDefinitionEntity> customFieldDefs = (List<CustomFieldDefinitionEntity>) SessionUtils.getAttribute(CustomerConstants.CUSTOM_FIELDS_LIST, request);
        Locale locale = getUserContext(request).getPreferredLocale();
        for (CustomFieldDefinitionEntity customFieldDef : customFieldDefs) {
            for (PersonnelCustomFieldEntity customFieldEntity : customFieldEntities) {
                if (customFieldDef.getFieldId().equals(customFieldEntity.getFieldId())) {
                    if (customFieldDef.getFieldType().equals(CustomFieldType.DATE.getValue())) {
                        customFields.add(new CustomFieldDto(customFieldEntity.getFieldId(), DateUtils
                                .getUserLocaleDate(locale, customFieldEntity.getFieldValue()), customFieldDef
                                .getFieldType()));
                    } else {
                        customFields.add(new CustomFieldDto(customFieldEntity.getFieldId(), customFieldEntity
                                .getFieldValue(), customFieldDef.getFieldType()));
                    }
                }
            }
        }
        return customFields;
    }

    @SuppressWarnings("unchecked")
    private List<RoleBO> getRoles(HttpServletRequest request, PersonActionForm personActionForm)
            throws PageExpiredException {
        boolean addFlag = false;
        List<RoleBO> selectList = new ArrayList<RoleBO>();
        List<RoleBO> masterList = (List<RoleBO>) SessionUtils.getAttribute(PersonnelConstants.ROLEMASTERLIST, request);
        if (personActionForm.getPersonnelRoles() != null) {
            for (RoleBO role : masterList) {
                for (String roleId : personActionForm.getPersonnelRoles()) {
                    if (roleId != null && role.getId().intValue() == Integer.valueOf(roleId).intValue()) {
                        selectList.add(role);
                        addFlag = true;
                    }
                }
            }
        }
        if (addFlag) {
            return selectList;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private void updateRoleLists(HttpServletRequest request, PersonActionForm personActionForm)
            throws PageExpiredException {

        boolean addFlag = false;
        List<RoleBO> selectList = new ArrayList<RoleBO>();
        if (personActionForm.getPersonnelRoles() != null) {
            List<RoleBO> masterList = (List<RoleBO>) SessionUtils.getAttribute(PersonnelConstants.ROLEMASTERLIST, request);
            for (RoleBO role : masterList) {
                for (String roleId : personActionForm.getPersonnelRoles()) {
                    if (roleId != null && role.getId().intValue() == Integer.valueOf(roleId).intValue()) {
                        selectList.add(role);
                        addFlag = true;
                    }
                }
            }
        }
        if (addFlag) {
            SessionUtils.setCollectionAttribute(PersonnelConstants.PERSONNEL_ROLES_LIST, selectList, request);
        } else {
            SessionUtils.setAttribute(PersonnelConstants.PERSONNEL_ROLES_LIST, null, request);
        }
    }

    protected Locale getUserLocale(HttpServletRequest request) {
        Locale locale = null;
        HttpSession session = request.getSession();
        if (session != null) {
            UserContext userContext = (UserContext) session.getAttribute(LoginConstants.USERCONTEXT);
            if (null != userContext) {
                locale = userContext.getCurrentLocale();

            }
        }
        return locale;
    }

    private Short getLocaleId(Short lookUpId) throws ServiceException {
        if (lookUpId != null) {
            for (SupportedLocalesEntity locale : ((PersonnelBusinessService) getService()).getAllLocales()) {
                if (locale.getLanguage().getLookUpValue().getLookUpId() == lookUpId.intValue()) {
                    return locale.getLocaleId();
                }
            }
        }

        return Localization.getInstance().getLocaleId();
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward captureQuestionResponses(
            final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
            @SuppressWarnings("unused") final HttpServletResponse response) throws Exception {
        request.setAttribute(METHODCALLED, "captureQuestionResponses");
        ActionErrors errors = createGroupQuestionnaire.validateResponses(request, (PersonActionForm) form);
        if (errors != null && !errors.isEmpty()) {
            addErrors(request, errors);
            return mapping.findForward(ActionForwards.captureQuestionResponses.toString());
        }
        return createGroupQuestionnaire.rejoinFlow(mapping);
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward editQuestionResponses(
            final ActionMapping mapping, final ActionForm form,
            final HttpServletRequest request, @SuppressWarnings("unused") final HttpServletResponse response) throws Exception {
        request.setAttribute(METHODCALLED, "editQuestionResponses");
        return createGroupQuestionnaire.editResponses(mapping, request, (PersonActionForm) form);
    }

    private QuestionnaireFlowAdapter createGroupQuestionnaire = new QuestionnaireFlowAdapter("Create", "Personnel",
            ActionForwards.preview_success, "custSearchAction.do?method=loadMainSearch", new DefaultQuestionnaireServiceFacadeLocator());

}
