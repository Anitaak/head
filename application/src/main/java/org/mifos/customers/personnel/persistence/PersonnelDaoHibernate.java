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

package org.mifos.customers.personnel.persistence;

import org.mifos.application.master.business.SupportedLocalesEntity;
import org.mifos.config.persistence.ApplicationConfigurationPersistence;
import org.mifos.framework.exceptions.PersistenceException;
import org.mifos.framework.hibernate.helper.QueryResult;
import org.mifos.security.MifosUser;
import org.mifos.security.rolesandpermission.business.RoleBO;
import org.mifos.security.rolesandpermission.persistence.RolesPermissionsPersistence;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.mifos.accounts.savings.persistence.GenericDao;
import org.mifos.application.NamedQueryConstants;
import org.mifos.application.servicefacade.CenterCreation;
import org.mifos.customers.personnel.business.PersonnelBO;
import org.mifos.customers.personnel.business.PersonnelDto;
import org.mifos.customers.personnel.util.helpers.PersonnelLevel;
import org.mifos.customers.personnel.util.helpers.PersonnelStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;

public class PersonnelDaoHibernate implements PersonnelDao {

    private final GenericDao genericDao;
    private static RolesPermissionsPersistence rolesPermissionsPersistence;

    public PersonnelDaoHibernate(GenericDao genericDao) {
        this.genericDao = genericDao;
    }

    @Override
    public void save(PersonnelBO user) {
        this.genericDao.createOrUpdate(user);
    }

    @Override
    public PersonnelBO findPersonnelById(Short id) {
        if (id == null) {
            return null;
        }

        HashMap<String, Object> queryParameters = new HashMap<String, Object>();
        queryParameters.put("PERSONNEL_ID", id);

        return (PersonnelBO) this.genericDao.executeUniqueResultNamedQuery("findPersonnelById", queryParameters);
    }

    @Override
    public PersonnelBO findPersonnelByUsername(final String username) {
        HashMap<String, Object> queryParameters = new HashMap<String, Object>();
        queryParameters.put("USER_NAME", username);

        return (PersonnelBO) this.genericDao.executeUniqueResultNamedQuery(NamedQueryConstants.GETPERSONNELBYNAME, queryParameters);
    }

    @Override
    public MifosUser findAuthenticatedUserByUsername(String username) {
        HashMap<String, Object> queryParameters = new HashMap<String, Object>();
        queryParameters.put("USER_NAME", username);

        PersonnelBO user = (PersonnelBO) this.genericDao.executeUniqueResultNamedQuery(NamedQueryConstants.GETPERSONNELBYNAME, queryParameters);

        byte[] password = user.getEncryptedPassword();
        boolean enabled = user.isActive();
        boolean accountNonExpired = !user.isLocked();
        boolean credentialsNonExpired = true;
        boolean accountNonLocked = !user.isLocked();

        GrantedAuthority mifosUser = new GrantedAuthorityImpl(MifosUser.FULLY_AUTHENTICATED_USER);
        List<GrantedAuthority> authorities = Arrays.asList(mifosUser);

        return new MifosUser(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<PersonnelDto> findActiveLoanOfficersForOffice(CenterCreation centerCreationDto) {

        HashMap<String, Object> queryParameters = new HashMap<String, Object>();
        queryParameters.put("levelId", PersonnelLevel.LOAN_OFFICER.getValue());
        queryParameters.put("userId", centerCreationDto.getLoggedInUserId());
        queryParameters.put("userLevelId", centerCreationDto.getLoggedInUserLevelId());
        queryParameters.put("officeId", centerCreationDto.getOfficeId());
        queryParameters.put("statusId", PersonnelStatus.ACTIVE.getValue());

        List<PersonnelDto> queryResult = (List<PersonnelDto>) genericDao.executeNamedQuery(
                NamedQueryConstants.MASTERDATA_ACTIVE_LOANOFFICERS_INBRANCH, queryParameters);

        return queryResult;
    }

    @Override
    public List<PersonnelBO> getActiveBranchManagersUnderOffice(Short officeId) throws org.mifos.framework.exceptions.ServiceException {
        try {
            return new PersonnelPersistence().getActiveLoanOfficersUnderOffice(officeId);
        } catch (PersistenceException e) {
            throw new org.mifos.framework.exceptions.ServiceException(e);
        }
    }

    @Override
    public List<PersonnelBO> getActiveLoanOfficersUnderOffice(Short officeId) throws org.mifos.framework.exceptions.ServiceException {
        try {
            return new PersonnelPersistence().getActiveLoanOfficersUnderOffice(officeId);
        } catch (PersistenceException e) {
            throw new org.mifos.framework.exceptions.ServiceException(e);
        }
    }

    @Override
    public List<SupportedLocalesEntity> getAllLocales() {

        return new ApplicationConfigurationPersistence().getSupportedLocale();
    }

    @Override
    public List<PersonnelBO> getAllPersonnel() throws org.mifos.framework.exceptions.ServiceException {

        try {
            return new PersonnelPersistence().getAllPersonnel();
        } catch (PersistenceException pe) {
            throw new org.mifos.framework.exceptions.ServiceException(pe);
        }
    }

    @Override
    public QueryResult getAllPersonnelNotes(Short personnelId) throws org.mifos.framework.exceptions.ServiceException {
        try {
            return new PersonnelPersistence().getAllPersonnelNotes(personnelId);
        } catch (PersistenceException e) {
            throw new org.mifos.framework.exceptions.ServiceException(e);
        }
    }

    @Override
    public PersonnelBO getPersonnelByGlobalPersonnelNum(String globalPersonnelNum) throws org.mifos.framework.exceptions.ServiceException {
        try {
            return new PersonnelPersistence().getPersonnelByGlobalPersonnelNum(globalPersonnelNum);
        } catch (PersistenceException e) {

            throw new org.mifos.framework.exceptions.ServiceException(e);
        }
    }

    @Override
    public List<RoleBO> getRoles() throws org.mifos.framework.exceptions.ServiceException {
        try {
            return rolesPermissionsPersistence.getRoles();
        } catch (PersistenceException e) {
            throw new org.mifos.framework.exceptions.ServiceException(e);
        }
    }

    @Override
    public List<SupportedLocalesEntity> getSupportedLocales() throws org.mifos.framework.exceptions.ServiceException {
        try {
            List<SupportedLocalesEntity> locales = new PersonnelPersistence().getSupportedLocales();
            return locales;
        } catch (PersistenceException e) {
            throw new org.mifos.framework.exceptions.ServiceException(e);
        }
    }

    @Override
    public QueryResult search(String searchString, Short officeId, Short userId)  throws org.mifos.framework.exceptions.ServiceException {
        try {
            return new PersonnelPersistence().search(searchString, userId);
        } catch (PersistenceException e) {
            throw new org.mifos.framework.exceptions.ServiceException(e);
        }
    }
}