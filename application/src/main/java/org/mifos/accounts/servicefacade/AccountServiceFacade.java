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

package org.mifos.accounts.servicefacade;

import java.util.List;

import org.mifos.accounts.util.helpers.ApplicableCharge;
import org.mifos.dto.domain.AccountReferenceDto;
import org.mifos.dto.domain.UserReferenceDto;
import org.mifos.framework.exceptions.ApplicationException;
import org.mifos.framework.exceptions.ServiceException;
import org.mifos.security.util.UserContext;

/**
 * Interface for presentation layer to access accounts
 *
 */
public interface AccountServiceFacade {

    AccountPaymentDto getAccountPaymentInformation(
            Integer accountId, String paymentType, Short localeId, UserReferenceDto userReferenceDto) throws Exception;

    boolean isPaymentPermitted(final UserContext userContext, Integer accountId) throws ServiceException;

    List<ApplicableCharge> getApplicableFees(Integer accountId, UserContext userContext) throws ServiceException;

    void applyCharge(Integer accountId, UserContext userContext, Short feeId, Double chargeAmount) throws ServiceException, ApplicationException;

    AccountTypeCustomerLevelDto getAccountTypeCustomerLevelDto(Integer accountId) throws ServiceException;


}
