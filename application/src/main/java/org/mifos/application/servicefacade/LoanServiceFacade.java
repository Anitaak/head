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

package org.mifos.application.servicefacade;

import java.util.List;

import org.joda.time.DateTime;
import org.mifos.accounts.exceptions.AccountException;
import org.mifos.accounts.fund.business.FundBO;
import org.mifos.accounts.loan.struts.action.LoanCreationGlimDto;
import org.mifos.accounts.loan.struts.actionforms.LoanAccountActionForm;
import org.mifos.accounts.loan.util.helpers.LoanAccountDetailsDto;
import org.mifos.accounts.loan.util.helpers.LoanDisbursalDto;
import org.mifos.accounts.productdefinition.util.helpers.PrdOfferingDto;
import org.mifos.application.master.business.BusinessActivityEntity;
import org.mifos.customers.business.CustomerBO;
import org.mifos.framework.exceptions.ApplicationException;
import org.mifos.framework.exceptions.PersistenceException;
import org.mifos.framework.exceptions.ServiceException;
import org.mifos.security.util.UserContext;

public interface LoanServiceFacade {

    List<PrdOfferingDto> retrieveActiveLoanProductsApplicableForCustomer(CustomerBO customer);

    LoanCreationGlimDto retrieveGlimSpecificDataForGroup(CustomerBO customer);

    LoanCreationProductDetailsDto retrieveGetProductDetailsForLoanAccountCreation(Integer customerId) throws ApplicationException;

    LoanCreationLoanDetailsDto retrieveLoanDetailsForLoanAccountCreation(UserContext userContext, Integer customerId, Short productId) throws ApplicationException;

    LoanCreationLoanScheduleDetailsDto retrieveScheduleDetailsForLoanCreation(UserContext userContext, Integer customerId, DateTime disbursementDate, FundBO fund, LoanAccountActionForm loanActionForm) throws ApplicationException;

    LoanCreationLoanScheduleDetailsDto retrieveScheduleDetailsForRedoLoan(UserContext userContext, Integer customerId,
            DateTime disbursementDate, FundBO fund, LoanAccountActionForm loanActionForm) throws ApplicationException;

    LoanCreationPreviewDto previewLoanCreationDetails(Integer customerId, List<LoanAccountDetailsDto> accountDetails, List<String> selectedClientIds, List<BusinessActivityEntity> businessActEntity);

    LoanCreationResultDto createLoan(UserContext userContext, Integer customerId, DateTime disbursementDate, FundBO fund, LoanAccountActionForm loanActionForm) throws ApplicationException;

    void checkIfProductsOfferingCanCoexist(Integer loanAccountId) throws ServiceException, PersistenceException, AccountException;

    LoanDisbursalDto getLoanDisbursalDto(Integer loanAccountId) throws ServiceException;
}
