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
package org.mifos.accounts.savings.persistence;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.joda.time.LocalDate;
import org.mifos.accounts.savings.business.SavingsBO;
import org.mifos.accounts.savings.interest.EndOfDayDetail;
import org.mifos.application.master.business.MifosCurrency;
import org.mifos.application.servicefacade.CollectionSheetCustomerSavingDto;
import org.mifos.application.servicefacade.CollectionSheetCustomerSavingsAccountDto;
import org.mifos.application.servicefacade.CustomerHierarchyParams;
import org.mifos.framework.util.helpers.Money;
import org.mifos.accounts.business.AccountCustomFieldEntity;
import org.mifos.application.NamedQueryConstants;
import org.mifos.application.master.business.CustomFieldDefinitionEntity;
import org.mifos.application.master.util.helpers.MasterConstants;
import org.mifos.application.servicefacade.CollectionSheetCustomerSavingDto;
import org.mifos.application.servicefacade.CollectionSheetCustomerSavingsAccountDto;
import org.mifos.application.servicefacade.CustomerHierarchyParams;
import org.mifos.application.util.helpers.EntityType;

/**
 *
 */
public class SavingsDaoHibernate implements SavingsDao {

    private final GenericDao baseDao;

    public SavingsDaoHibernate(final GenericDao baseDao) {
        this.baseDao = baseDao;
    }

    @SuppressWarnings("unchecked")
    public List<CollectionSheetCustomerSavingDto> findAllMandatorySavingAccountsForClientsOrGroupsWithCompleteGroupStatusForCustomerHierarchy(
            final CustomerHierarchyParams customerHierarchyParams) {

        final Map<String, Object> topOfHierarchyParameters = new HashMap<String, Object>();
        topOfHierarchyParameters.put("CUSTOMER_ID", customerHierarchyParams.getCustomerAtTopOfHierarchyId());
        topOfHierarchyParameters.put("TRANSACTION_DATE", customerHierarchyParams.getTransactionDate().toString());

        final List<CollectionSheetCustomerSavingDto> mandatorySavingsOnRootCustomer = (List<CollectionSheetCustomerSavingDto>) baseDao
                .executeNamedQueryWithResultTransformer(
                        "findAllMandatorySavingAccountsForClientsOrGroupsWithCompleteGroupStatusForTopOfCustomerHierarchy",
                        topOfHierarchyParameters, CollectionSheetCustomerSavingDto.class);

        final Map<String, Object> restOfHierarchyParameters = new HashMap<String, Object>();
        restOfHierarchyParameters.put("BRANCH_ID", customerHierarchyParams.getBranchId());
        restOfHierarchyParameters.put("SEARCH_ID", customerHierarchyParams.getSearchId());
        restOfHierarchyParameters.put("TRANSACTION_DATE", customerHierarchyParams.getTransactionDate().toString());

        final List<CollectionSheetCustomerSavingDto> mandatorySavingsOnRestOfHierarchy = (List<CollectionSheetCustomerSavingDto>) baseDao
                .executeNamedQueryWithResultTransformer(
                        "findAllMandatorySavingAccountsForClientsOrGroupsWithCompleteGroupStatusForRestOfCustomerHierarchy",
                        restOfHierarchyParameters, CollectionSheetCustomerSavingDto.class);

        return nullSafeSavingsHierarchy(mandatorySavingsOnRootCustomer, mandatorySavingsOnRestOfHierarchy);
    }

    @SuppressWarnings("unchecked")
    public List<CollectionSheetCustomerSavingDto> findAllVoluntarySavingAccountsForClientsAndGroupsWithCompleteGroupStatusForCustomerHierarchy(
            final CustomerHierarchyParams customerHierarchyParams) {

        final Map<String, Object> topOfHierarchyParameters = new HashMap<String, Object>();
        topOfHierarchyParameters.put("CUSTOMER_ID", customerHierarchyParams.getCustomerAtTopOfHierarchyId());
        topOfHierarchyParameters.put("TRANSACTION_DATE", customerHierarchyParams.getTransactionDate().toString());

        final List<CollectionSheetCustomerSavingDto> voluntarySavingsOnRootCustomer = (List<CollectionSheetCustomerSavingDto>) baseDao
                .executeNamedQueryWithResultTransformer(
                        "findAllVoluntarySavingsAccountsForClientsAndGroupsWithCompleteGroupStatusForTopOfCustomerHierarchy",
                        topOfHierarchyParameters, CollectionSheetCustomerSavingDto.class);

        final Map<String, Object> restOfHierarchyParameters = new HashMap<String, Object>();
        restOfHierarchyParameters.put("BRANCH_ID", customerHierarchyParams.getBranchId());
        restOfHierarchyParameters.put("SEARCH_ID", customerHierarchyParams.getSearchId());
        restOfHierarchyParameters.put("TRANSACTION_DATE", customerHierarchyParams.getTransactionDate().toString());

        final List<CollectionSheetCustomerSavingDto> voluntarySavingsOnRestOfHierarchy = (List<CollectionSheetCustomerSavingDto>) baseDao
                .executeNamedQueryWithResultTransformer(
                        "findAllVoluntarySavingsAccountsForClientsAndGroupsWithCompleteGroupStatusForRestOfCustomerHierarchy",
                        restOfHierarchyParameters, CollectionSheetCustomerSavingDto.class);

        return nullSafeSavingsHierarchy(voluntarySavingsOnRootCustomer, voluntarySavingsOnRestOfHierarchy);
    }

    @SuppressWarnings("unchecked")
    public List<CollectionSheetCustomerSavingDto> findAllMandatorySavingAccountsForIndividualChildrenOfCentersOrGroupsWithPerIndividualStatusForCustomerHierarchy(
            final CustomerHierarchyParams customerHierarchyParams) {

        final Map<String, Object> topOfHierarchyParameters = new HashMap<String, Object>();
        topOfHierarchyParameters.put("CUSTOMER_ID", customerHierarchyParams.getCustomerAtTopOfHierarchyId());
        topOfHierarchyParameters.put("TRANSACTION_DATE", customerHierarchyParams.getTransactionDate().toString());

        final List<CollectionSheetCustomerSavingDto> centerOrPerIndividualGroupSavingsOnRootCustomer = (List<CollectionSheetCustomerSavingDto>) baseDao
                .executeNamedQueryWithResultTransformer(
                        "findAllMandatorySavingsAccountsForCentersAndGroupsWithPerIndividualStatusForTopOfCustomerHierarchy",
                        topOfHierarchyParameters, CollectionSheetCustomerSavingDto.class);

        final Map<String, Object> restOfHierarchyParameters = new HashMap<String, Object>();
        restOfHierarchyParameters.put("BRANCH_ID", customerHierarchyParams.getBranchId());
        restOfHierarchyParameters.put("SEARCH_ID", customerHierarchyParams.getSearchId());
        restOfHierarchyParameters.put("TRANSACTION_DATE", customerHierarchyParams.getTransactionDate().toString());

        final List<CollectionSheetCustomerSavingDto> perIndividualGroupSavingsOnRestOfHierarchy = (List<CollectionSheetCustomerSavingDto>) baseDao
                .executeNamedQueryWithResultTransformer(
                        "findAllMandatorySavingsAccountsForCentersAndGroupsWithPerIndividualStatusForRestOfCustomerHierarchy",
                        restOfHierarchyParameters, CollectionSheetCustomerSavingDto.class);

        return nullSafeSavingsHierarchy(centerOrPerIndividualGroupSavingsOnRootCustomer,
                perIndividualGroupSavingsOnRestOfHierarchy);
    }

    @SuppressWarnings("unchecked")
    public List<CollectionSheetCustomerSavingDto> findAllVoluntarySavingAccountsForIndividualChildrenOfCentersOrGroupsWithPerIndividualStatusForCustomerHierarchy(
            final CustomerHierarchyParams customerHierarchyParams) {
        final Map<String, Object> topOfHierarchyParameters = new HashMap<String, Object>();
        topOfHierarchyParameters.put("CUSTOMER_ID", customerHierarchyParams.getCustomerAtTopOfHierarchyId());
        topOfHierarchyParameters.put("TRANSACTION_DATE", customerHierarchyParams.getTransactionDate().toString());

        final List<CollectionSheetCustomerSavingDto> centerOrPerIndividualGroupSavingsOnRootCustomer = (List<CollectionSheetCustomerSavingDto>) baseDao
                .executeNamedQueryWithResultTransformer(
                        "findAllVoluntarySavingsAccountsForCentersAndGroupsWithPerIndividualStatusForTopOfCustomerHierarchy",
                        topOfHierarchyParameters, CollectionSheetCustomerSavingDto.class);

        final Map<String, Object> restOfHierarchyParameters = new HashMap<String, Object>();
        restOfHierarchyParameters.put("BRANCH_ID", customerHierarchyParams.getBranchId());
        restOfHierarchyParameters.put("SEARCH_ID", customerHierarchyParams.getSearchId());
        restOfHierarchyParameters.put("TRANSACTION_DATE", customerHierarchyParams.getTransactionDate().toString());

        final List<CollectionSheetCustomerSavingDto> perIndividualGroupSavingsOnRestOfHierarchy = (List<CollectionSheetCustomerSavingDto>) baseDao
                .executeNamedQueryWithResultTransformer(
                        "findAllVoluntarySavingsAccountsForCentersAndGroupsWithPerIndividualStatusForRestOfCustomerHierarchy",
                        restOfHierarchyParameters, CollectionSheetCustomerSavingDto.class);

        return nullSafeSavingsHierarchy(centerOrPerIndividualGroupSavingsOnRootCustomer,
                perIndividualGroupSavingsOnRestOfHierarchy);
    }

    @SuppressWarnings("unchecked")
    private List<CollectionSheetCustomerSavingDto> nullSafeSavingsHierarchy(
            final List<CollectionSheetCustomerSavingDto> mandatorySavingsOnRootCustomer,
            final List<CollectionSheetCustomerSavingDto> mandatorySavingsOnRestOfHierarchy) {

        List<CollectionSheetCustomerSavingDto> nullSafeSavings = (List<CollectionSheetCustomerSavingDto>) ObjectUtils
                .defaultIfNull(mandatorySavingsOnRootCustomer, new ArrayList<CollectionSheetCustomerSavingDto>());

        List<CollectionSheetCustomerSavingDto> nullSafeRest = (List<CollectionSheetCustomerSavingDto>) ObjectUtils
                .defaultIfNull(mandatorySavingsOnRestOfHierarchy, new ArrayList<CollectionSheetCustomerSavingDto>());

        nullSafeSavings.addAll(nullSafeRest);
        return nullSafeSavings;
    }

    @SuppressWarnings("unchecked")
    public List<CollectionSheetCustomerSavingsAccountDto> findAllSavingAccountsForCustomerHierarchy(
            CustomerHierarchyParams customerHierarchyParams) {
        final Map<String, Object> topOfHierarchyParameters = new HashMap<String, Object>();
        topOfHierarchyParameters.put("BRANCH_ID", customerHierarchyParams.getBranchId());
        topOfHierarchyParameters.put("TRANSACTION_DATE", customerHierarchyParams.getTransactionDate().toString());
        topOfHierarchyParameters.put("SEARCH_ID", customerHierarchyParams.getSearchId());
        //snip the '.%' from SEARCH_ID
        topOfHierarchyParameters.put("SEARCH_ID_NO_PERCENTAGE", customerHierarchyParams.getSearchId().substring(0, customerHierarchyParams.getSearchId().length() - 2));

        return (List<CollectionSheetCustomerSavingsAccountDto>) baseDao.executeNamedQueryWithResultTransformer(
                "findAllSavingAccountsForCustomerHierarchy", topOfHierarchyParameters,
                CollectionSheetCustomerSavingsAccountDto.class);
    }

    @Override
    public final Iterator<CustomFieldDefinitionEntity> retrieveCustomFieldEntitiesForSavings() {
        Map<String, Object> queryParameters = new HashMap<String, Object>();
        queryParameters.put(MasterConstants.ENTITY_TYPE, EntityType.SAVINGS.getValue());
        return (Iterator<CustomFieldDefinitionEntity>) baseDao.executeNamedQueryIterator(NamedQueryConstants.RETRIEVE_CUSTOM_FIELDS, queryParameters);
    }

    @Override
    public Iterator<AccountCustomFieldEntity> getCustomFieldResponses(Short customFieldId) {
        Map<String, Object> queryParameters = new HashMap<String, Object>();
        queryParameters.put("CUSTOM_FIELD_ID", customFieldId);
        return (Iterator<AccountCustomFieldEntity>) baseDao.executeNamedQueryIterator("AccountCustomFieldEntity.getResponses", queryParameters);
    }

    @Override
    public SavingsBO findById(Long savingsId) {

        final Map<String, Object> queryParameters = new HashMap<String, Object>();
        queryParameters.put("ACCOUNT_ID", savingsId.intValue());

        return (SavingsBO) this.baseDao.executeUniqueResultNamedQuery("savings.findById", queryParameters);
    }

    @Override
    public void save(SavingsBO savingsAccount) {
        this.baseDao.createOrUpdate(savingsAccount);
    }

    @Override
    public List<EndOfDayDetail> retrieveAllEndOfDayDetailsFor(MifosCurrency currency, Long savingsId) {

        List<EndOfDayDetail> allEndOfDayDetailsForAccount = new ArrayList<EndOfDayDetail>();

        Map<String, Object> queryParameters = new HashMap<String, Object>();
        queryParameters.put("ACCOUNT_ID", savingsId.intValue());
        List<Object[]> queryResult = (List<Object[]>) this.baseDao.executeNamedQuery("savings.retrieveAllEndOfDayTransactionDetails", queryParameters);

        if (queryResult != null) {
            for (Object[] dailyRecord : queryResult) {
                Date dayOfYear = (Date)dailyRecord[0];
                BigDecimal totalDeposits = (BigDecimal)dailyRecord[1];
                BigDecimal totalWithdrawals = (BigDecimal)dailyRecord[2];
                BigDecimal totalInterest = (BigDecimal)dailyRecord[3];

                EndOfDayDetail endOfDayDetail = new EndOfDayDetail(new LocalDate(dayOfYear), new Money(currency, totalDeposits), new Money(currency, totalWithdrawals), new Money(currency, totalInterest));
                allEndOfDayDetailsForAccount.add(endOfDayDetail);
            }
        }

        return allEndOfDayDetailsForAccount;
    }
}
