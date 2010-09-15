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

package org.mifos.accounts.savings.business.service;

import java.util.List;

import org.apache.log4j.Logger;
import org.mifos.accounts.savings.business.SavingsBO;
import org.mifos.accounts.savings.persistence.SavingsPersistence;
import org.mifos.accounts.savings.util.helpers.SavingsConstants;
import org.mifos.application.master.business.CustomFieldDefinitionEntity;
import org.mifos.customers.business.CustomerLevelEntity;
import org.mifos.customers.office.business.OfficeBO;
import org.mifos.dto.domain.PrdOfferingDto;
import org.mifos.framework.business.AbstractBusinessObject;
import org.mifos.framework.business.service.BusinessService;
import org.mifos.framework.exceptions.PersistenceException;
import org.mifos.framework.exceptions.ServiceException;
import org.mifos.security.util.UserContext;

public class SavingsBusinessService implements BusinessService {
    private SavingsPersistence savingsPersistence = new SavingsPersistence();

    private static final Logger logger = Logger.getLogger(SavingsBusinessService.class);

    @Override
    public AbstractBusinessObject getBusinessObject(@SuppressWarnings("unused")UserContext userContext) {
        return null;
    }

    public List<PrdOfferingDto> getSavingProducts(OfficeBO branch, CustomerLevelEntity customerLevel, short accountType)
            throws ServiceException {
        logger.debug("In SavingsBusinessService::getSavingProducts()");
        try {
            return savingsPersistence.getSavingsProducts(branch, customerLevel, accountType);
        } catch (PersistenceException e) {
            throw new ServiceException(e);
        }
    }

    public List<CustomFieldDefinitionEntity> retrieveCustomFieldsDefinition() throws ServiceException {
        logger.debug("In SavingsBusinessService::retrieveCustomFieldsDefinition()");
        try {
            List<CustomFieldDefinitionEntity> customFields = savingsPersistence
                    .retrieveCustomFieldsDefinition(SavingsConstants.SAVINGS_CUSTOM_FIELD_ENTITY_TYPE);
            initialize(customFields);
            return customFields;
        } catch (PersistenceException e) {
            throw new ServiceException(e);
        }
    }

    public SavingsBO findById(Integer accountId) throws ServiceException {
        logger.debug("In SavingsBusinessService::findById(), accountId: " + accountId);
        try {
            return savingsPersistence.findById(accountId);
        } catch (PersistenceException e) {
            throw new ServiceException(e);
        }
    }

    public SavingsBO findBySystemId(String globalAccountNumber) throws ServiceException {
        logger.debug("In SavingsBusinessService::findBySystemId(), globalAccountNumber: " + globalAccountNumber);
        try {
            return savingsPersistence.findBySystemId(globalAccountNumber);
        } catch (PersistenceException e) {
            throw new ServiceException(e);
        }
    }

    public List<SavingsBO> getAllClosedAccounts(Integer customerId) throws ServiceException {
        try {
            return savingsPersistence.getAllClosedAccount(customerId);
        } catch (PersistenceException e) {
            throw new ServiceException(e);
        }
    }

    public List<SavingsBO> getAllSavingsAccount() throws ServiceException {
        try {
            return savingsPersistence.getAllSavingsAccount();
        } catch (PersistenceException pe) {
            throw new ServiceException(pe);
        }
    }

    public void initialize(Object object) {
        savingsPersistence.initialize(object);
    }
}
