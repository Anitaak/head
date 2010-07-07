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
import org.mifos.framework.exceptions.ServiceException;
import org.mifos.security.MifosUser;
import org.mifos.security.rolesandpermission.business.RoleBO;
import java.util.List;

import org.mifos.application.servicefacade.CenterCreation;
import org.mifos.customers.personnel.business.PersonnelBO;
import org.mifos.customers.personnel.business.PersonnelDto;

public interface PersonnelDao {

    void save(PersonnelBO user);

    PersonnelBO findPersonnelById(Short id);

    PersonnelBO findPersonnelByUsername(String personnelName);

    MifosUser findAuthenticatedUserByUsername(String username);

    PersonnelBO getPersonnelByGlobalPersonnelNum(String globalPersonnelNum) throws ServiceException;

    List<PersonnelDto> findActiveLoanOfficersForOffice(CenterCreation centerCreation);

    List<RoleBO> getRoles() throws ServiceException;

    List<SupportedLocalesEntity> getAllLocales();

    org.mifos.framework.hibernate.helper.QueryResult getAllPersonnelNotes(Short personnelId) throws ServiceException;

    org.mifos.framework.hibernate.helper.QueryResult search(String searchString, Short officeId, Short userId) throws ServiceException;

    List<PersonnelBO> getAllPersonnel() throws ServiceException;

    List<PersonnelBO> getActiveLoanOfficersUnderOffice(Short officeId) throws ServiceException;

    List<SupportedLocalesEntity> getSupportedLocales() throws ServiceException;

    List<PersonnelBO> getActiveBranchManagersUnderOffice(Short officeId) throws ServiceException;
}
