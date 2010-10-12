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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mifos.accounts.business.AccountActionDateEntity;
import org.mifos.accounts.business.AccountPaymentEntity;
import org.mifos.accounts.business.AccountStateEntity;
import org.mifos.accounts.business.AccountTestUtils;
import org.mifos.accounts.persistence.AccountPersistence;
import org.mifos.accounts.productdefinition.business.SavingsOfferingBO;
import org.mifos.accounts.productdefinition.util.helpers.RecommendedAmountUnit;
import org.mifos.accounts.savings.business.SavingBOTestUtils;
import org.mifos.accounts.savings.business.SavingsBO;
import org.mifos.accounts.savings.business.SavingsTrxnDetailEntity;
import org.mifos.accounts.savings.util.helpers.SavingsConstants;
import org.mifos.accounts.savings.util.helpers.SavingsTestHelper;
import org.mifos.accounts.util.helpers.AccountActionTypes;
import org.mifos.accounts.util.helpers.AccountState;
import org.mifos.accounts.util.helpers.AccountStates;
import org.mifos.accounts.util.helpers.AccountTypes;
import org.mifos.accounts.util.helpers.PaymentStatus;
import org.mifos.application.master.business.CustomFieldDefinitionEntity;
import org.mifos.application.meeting.business.MeetingBO;
import org.mifos.config.business.Configuration;
import org.mifos.customers.business.CustomerBO;
import org.mifos.customers.checklist.business.AccountCheckListBO;
import org.mifos.customers.personnel.business.PersonnelBO;
import org.mifos.customers.personnel.persistence.PersonnelPersistence;
import org.mifos.customers.personnel.util.helpers.PersonnelConstants;
import org.mifos.customers.util.helpers.CustomerStatus;
import org.mifos.dto.domain.PrdOfferingDto;
import org.mifos.framework.MifosIntegrationTestCase;
import org.mifos.framework.TestUtils;
import org.mifos.framework.exceptions.PersistenceException;
import org.mifos.framework.hibernate.helper.StaticHibernateUtil;
import org.mifos.framework.util.helpers.Money;
import org.mifos.framework.util.helpers.TestObjectFactory;
import org.mifos.security.util.UserContext;

public class SavingsPersistenceIntegrationTest extends MifosIntegrationTestCase {

    private final int SAVINGS_CUSTOMFIELDS_NUMBER = 1;

    private UserContext userContext;

    private SavingsPersistence savingsPersistence;

    private AccountPersistence accountPersistence;

    private CustomerBO group;

    private CustomerBO center;

    private SavingsBO savings;

    private SavingsBO savings1;

    private SavingsBO savings2;

    private SavingsOfferingBO savingsOffering;

    private SavingsOfferingBO savingsOffering1;

    private SavingsOfferingBO savingsOffering2;

    private AccountCheckListBO accountCheckList;

    @Before
    public void setUp() throws Exception {
        savingsPersistence = new SavingsPersistence();
        accountPersistence = new AccountPersistence();
        userContext = TestUtils.makeUser();

    }

    @After
    public void tearDown() throws Exception {
        TestObjectFactory.cleanUp(savings);
        if (savings1 != null) {
            TestObjectFactory.cleanUp(savings1);
            savingsOffering1 = null;
        }
        if (savings2 != null) {
            TestObjectFactory.cleanUp(savings2);
            savingsOffering2 = null;
        }

        TestObjectFactory.cleanUp(group);
        TestObjectFactory.cleanUp(center);
        TestObjectFactory.cleanUp(accountCheckList);
        TestObjectFactory.removeObject(savingsOffering1);
        TestObjectFactory.removeObject(savingsOffering2);
        StaticHibernateUtil.closeSession();
    }

    @Test
    public void testGetSavingsProducts() throws Exception {
        createInitialObjects();
        Date currentDate = new Date(System.currentTimeMillis());
        savingsOffering1 = TestObjectFactory.createSavingsProduct("SavingPrd1", "sdcf", currentDate,
                RecommendedAmountUnit.COMPLETE_GROUP);
        savingsOffering2 = TestObjectFactory.createSavingsProduct("SavingPrd2", "1asq", currentDate,
                RecommendedAmountUnit.COMPLETE_GROUP);
        List<PrdOfferingDto> products = savingsPersistence.getSavingsProducts(null, group.getCustomerLevel(),
                new Short("2"));
        Assert.assertEquals(2, products.size());
        Assert.assertEquals("Offerng name for the first product do not match.", products.get(0).getPrdOfferingName(),
                "SavingPrd1");
        Assert.assertEquals("Offerng name for the second product do not match.", products.get(1).getPrdOfferingName(),
                "SavingPrd2");

    }

    @Test
    public void testRetrieveCustomFieldsDefinition() throws Exception {
        List<CustomFieldDefinitionEntity> customFields = savingsPersistence
                .retrieveCustomFieldsDefinition(SavingsConstants.SAVINGS_CUSTOM_FIELD_ENTITY_TYPE);
        Assert.assertNotNull(customFields);
        Assert.assertEquals(SAVINGS_CUSTOMFIELDS_NUMBER, customFields.size());
    }

    @Test
    public void testFindById() throws Exception {
        createInitialObjects();
        Date currentDate = new Date(System.currentTimeMillis());
        savingsOffering = TestObjectFactory.createSavingsProduct("SavingPrd1", "xdsa", currentDate,
                RecommendedAmountUnit.COMPLETE_GROUP);
        savings = createSavingsAccount("FFFF", savingsOffering);
        SavingsBO savings1 = savingsPersistence.findById(savings.getAccountId());
        Assert.assertEquals(savingsOffering.getRecommendedAmount(), savings1.getRecommendedAmount());
    }

    @Test
    public void testGetAccountStatus() throws Exception {
        AccountStateEntity accountState = savingsPersistence.getAccountStatusObject(AccountStates.SAVINGS_ACC_CLOSED);
        Assert.assertNotNull(accountState);
        Assert.assertEquals(accountState.getId().shortValue(), AccountStates.SAVINGS_ACC_CLOSED);
    }

    @Test
    public void testRetrieveAllAccountStateList() throws NumberFormatException, PersistenceException {
        List<AccountStateEntity> accountStateEntityList = accountPersistence.retrieveAllAccountStateList(Short
                .valueOf("2"));
        Assert.assertNotNull(accountStateEntityList);
        Assert.assertEquals(6, accountStateEntityList.size());
    }

    @Test
    public void testRetrieveAllActiveAccountStateList() throws NumberFormatException, PersistenceException {
        List<AccountStateEntity> accountStateEntityList = accountPersistence.retrieveAllActiveAccountStateList(Short
                .valueOf("2"));
        Assert.assertNotNull(accountStateEntityList);
        Assert.assertEquals(6, accountStateEntityList.size());
    }

    @Test
    public void testGetStatusChecklist() throws Exception {
        accountCheckList = TestObjectFactory.createAccountChecklist(AccountTypes.SAVINGS_ACCOUNT.getValue(),
                AccountState.SAVINGS_PARTIAL_APPLICATION, Short.valueOf("1"));
        List statusCheckList = accountPersistence.getStatusChecklist(Short.valueOf("13"), AccountTypes.SAVINGS_ACCOUNT
                .getValue());
        Assert.assertNotNull(statusCheckList);

        Assert.assertEquals(1, statusCheckList.size());
    }

    @Test
    public void testFindBySystemId() throws Exception {
        createInitialObjects();
        Date currentDate = new Date(System.currentTimeMillis());
        savingsOffering = TestObjectFactory.createSavingsProduct("SavingPrd1", "v1ws", currentDate,
                RecommendedAmountUnit.COMPLETE_GROUP);
        savings = createSavingsAccount("kkk", savingsOffering);
        SavingsBO savings1 = savingsPersistence.findBySystemId(savings.getGlobalAccountNum());
        Assert.assertEquals(savings.getAccountId(), savings1.getAccountId());
        Assert.assertEquals(savingsOffering.getRecommendedAmount(), savings1.getRecommendedAmount());
    }

    @Test
    public void testRetrieveLastTransaction() throws Exception {
        try {
            SavingsTestHelper helper = new SavingsTestHelper();
            createInitialObjects();
            PersonnelBO createdBy = new PersonnelPersistence().getPersonnel(userContext.getId());
            savingsOffering = helper.createSavingsOffering("effwe", "231");
            savings = new SavingsBO(userContext, savingsOffering, group, AccountState.SAVINGS_ACTIVE, savingsOffering
                    .getRecommendedAmount(), null);

            AccountPaymentEntity payment = helper.createAccountPaymentToPersist(savings, new Money(Configuration
                    .getInstance().getSystemConfig().getCurrency(), "700.0"), new Money(Configuration.getInstance()
                    .getSystemConfig().getCurrency(), "1700.0"), helper.getDate("15/01/2006"),
                    AccountActionTypes.SAVINGS_DEPOSIT.getValue(), savings, createdBy, group);
            AccountTestUtils.addAccountPayment(payment, savings);
            savings.save();
            StaticHibernateUtil.commitTransaction();
            StaticHibernateUtil.closeSession();

            payment = helper.createAccountPaymentToPersist(savings, new Money(Configuration.getInstance()
                    .getSystemConfig().getCurrency(), "1000.0"), new Money(Configuration.getInstance()
                    .getSystemConfig().getCurrency(), "2700.0"), helper.getDate("20/02/2006"),
                    AccountActionTypes.SAVINGS_DEPOSIT.getValue(), savings, createdBy, group);
            AccountTestUtils.addAccountPayment(payment, savings);
            savings.update();
            StaticHibernateUtil.commitTransaction();
            StaticHibernateUtil.closeSession();

            savings = savingsPersistence.findById(savings.getAccountId());
            savings.setUserContext(userContext);
            payment = helper.createAccountPaymentToPersist(savings, new Money(Configuration.getInstance()
                    .getSystemConfig().getCurrency(), "500.0"), new Money(Configuration.getInstance().getSystemConfig()
                    .getCurrency(), "2200.0"), helper.getDate("10/03/2006"), AccountActionTypes.SAVINGS_WITHDRAWAL
                    .getValue(), savings, createdBy, group);
            AccountTestUtils.addAccountPayment(payment, savings);
            savings.update();
            StaticHibernateUtil.commitTransaction();
            StaticHibernateUtil.closeSession();

            savings = savingsPersistence.findById(savings.getAccountId());
            savings.setUserContext(userContext);
            payment = helper.createAccountPaymentToPersist(savings, new Money(Configuration.getInstance()
                    .getSystemConfig().getCurrency(), "1200.0"), new Money(Configuration.getInstance()
                    .getSystemConfig().getCurrency(), "3400.0"), helper.getDate("15/03/2006"),
                    AccountActionTypes.SAVINGS_DEPOSIT.getValue(), savings, createdBy, group);
            AccountTestUtils.addAccountPayment(payment, savings);
            savings.update();
            StaticHibernateUtil.commitTransaction();
            StaticHibernateUtil.closeSession();

            savings = savingsPersistence.findById(savings.getAccountId());
            savings.setUserContext(userContext);
            payment = helper.createAccountPaymentToPersist(savings, new Money(Configuration.getInstance()
                    .getSystemConfig().getCurrency(), "2500.0"), new Money(Configuration.getInstance()
                    .getSystemConfig().getCurrency(), "900.0"), helper.getDate("25/03/2006"),
                    AccountActionTypes.SAVINGS_WITHDRAWAL.getValue(), savings, createdBy, group);
            AccountTestUtils.addAccountPayment(payment, savings);
            savings.update();
            StaticHibernateUtil.commitTransaction();
            StaticHibernateUtil.closeSession();

            savings = savingsPersistence.findById(savings.getAccountId());
            savings.setUserContext(userContext);
            SavingsTrxnDetailEntity trxn = savingsPersistence.retrieveLastTransaction(savings.getAccountId(), helper
                    .getDate("12/03/2006"));
            Assert.assertEquals(TestUtils.createMoney("500"), trxn.getAmount());
            group = savings.getCustomer();
            center = group.getParentCustomer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetAccountsPendingForIntCalc() throws Exception {
        SavingsTestHelper helper = new SavingsTestHelper();
        createInitialObjects();
        Date currentDate = new Date(System.currentTimeMillis());

        savingsOffering = TestObjectFactory.createSavingsProduct("prd1", "sagf", currentDate,
                RecommendedAmountUnit.COMPLETE_GROUP);
        savingsOffering1 = TestObjectFactory.createSavingsProduct("prd2", "q14f", currentDate,
                RecommendedAmountUnit.COMPLETE_GROUP);
        savingsOffering2 = TestObjectFactory.createSavingsProduct("prd3", "z1as", currentDate,
                RecommendedAmountUnit.COMPLETE_GROUP);
        savings = helper.createSavingsAccount("000100000000021", savingsOffering, group,
                AccountStates.SAVINGS_ACC_PARTIALAPPLICATION, userContext);
        savings.setUserContext(TestObjectFactory.getContext());
        savings.changeStatus(AccountState.SAVINGS_INACTIVE.getValue(), null, "");

        savings1 = helper.createSavingsAccount("000100000000022", savingsOffering1, group,
                AccountStates.SAVINGS_ACC_PARTIALAPPLICATION, userContext);
        savings2 = helper.createSavingsAccount("000100000000023", savingsOffering2, group,
                AccountStates.SAVINGS_ACC_APPROVED, userContext);
        SavingBOTestUtils.setNextIntCalcDate(savings, helper.getDate("30/06/2006"));
        SavingBOTestUtils.setNextIntCalcDate(savings1, helper.getDate("30/06/2006"));
        SavingBOTestUtils.setNextIntCalcDate(savings2, helper.getDate("31/07/2006"));

        savings.update();
        savings1.update();
        savings2.update();
        StaticHibernateUtil.commitTransaction();
        StaticHibernateUtil.closeSession();

        List<Integer> savingsList = savingsPersistence.retreiveAccountsPendingForIntCalc(helper.getDate("01/07/2006"));
        Assert.assertEquals(Integer.valueOf("1").intValue(), savingsList.size());
        Assert.assertEquals(savings.getAccountId(), savingsList.get(0));

        // retrieve objects to remove
        savings = savingsPersistence.findById(savings.getAccountId());
        savings1 = savingsPersistence.findById(savings1.getAccountId());
        savings2 = savingsPersistence.findById(savings2.getAccountId());
        group = savings.getCustomer();
        center = group.getParentCustomer();
    }

    @Test
    public void testGetMissedDeposits() throws Exception {
        SavingsTestHelper helper = new SavingsTestHelper();
        MeetingBO meeting = TestObjectFactory.createMeeting(TestObjectFactory.getTypicalMeeting());
        center = TestObjectFactory.createWeeklyFeeCenter("Center", meeting);
        group = TestObjectFactory.createWeeklyFeeGroupUnderCenter("Group", CustomerStatus.GROUP_ACTIVE, center);
        savingsOffering = helper.createSavingsOffering("SavingPrd1", "wsed", Short.valueOf("1"), Short.valueOf("1"));
        ;
        savings = TestObjectFactory.createSavingsAccount("43245434", group, Short.valueOf("16"), new Date(System
                .currentTimeMillis()), savingsOffering);

        AccountActionDateEntity accountActionDateEntity = savings.getAccountActionDate((short) 1);
        SavingBOTestUtils.setActionDate(accountActionDateEntity, offSetCurrentDate(7));

        savings.update();
        StaticHibernateUtil.commitTransaction();
        StaticHibernateUtil.closeSession();

        savings = savingsPersistence.findById(savings.getAccountId());
        savings.setUserContext(userContext);
        StaticHibernateUtil.commitTransaction();
        Calendar currentDateCalendar = new GregorianCalendar();
        java.sql.Date currentDate = new java.sql.Date(currentDateCalendar.getTimeInMillis());

        Assert.assertEquals(savingsPersistence.getMissedDeposits(savings.getAccountId(), currentDate), 1);
    }

    @Test
    public void testGetMissedDepositsPaidAfterDueDate() throws Exception {
        SavingsTestHelper helper = new SavingsTestHelper();
        MeetingBO meeting = TestObjectFactory.createMeeting(TestObjectFactory.getTypicalMeeting());
        center = TestObjectFactory.createWeeklyFeeCenter("Center", meeting);
        group = TestObjectFactory.createWeeklyFeeGroupUnderCenter("Group", CustomerStatus.GROUP_ACTIVE, center);
        savingsOffering = helper.createSavingsOffering("SavingPrd1", "cvfg", Short.valueOf("1"), Short.valueOf("1"));
        ;
        savings = TestObjectFactory.createSavingsAccount("43245434", group, Short.valueOf("16"), new Date(System
                .currentTimeMillis()), savingsOffering);

        AccountActionDateEntity accountActionDateEntity = savings.getAccountActionDate((short) 1);
        SavingBOTestUtils.setActionDate(accountActionDateEntity, offSetCurrentDate(7));
        accountActionDateEntity.setPaymentStatus(PaymentStatus.PAID);
        Calendar currentDateCalendar = new GregorianCalendar();
        java.sql.Date currentDate = new java.sql.Date(currentDateCalendar.getTimeInMillis());

        SavingBOTestUtils.setPaymentDate(accountActionDateEntity, currentDate);
        savings.update();
        StaticHibernateUtil.commitTransaction();
        StaticHibernateUtil.closeSession();
        savings = savingsPersistence.findById(savings.getAccountId());
        savings.setUserContext(userContext);
        StaticHibernateUtil.commitTransaction();
        Assert.assertEquals(savingsPersistence.getMissedDepositsPaidAfterDueDate(savings.getAccountId()), 1);
    }

    @Test
    public void testGetAllSavingsAccount() throws Exception {
        createInitialObjects();
        Date currentDate = new Date(System.currentTimeMillis());
        savingsOffering = TestObjectFactory.createSavingsProduct("SavingPrd1", "v1ws", currentDate,
                RecommendedAmountUnit.COMPLETE_GROUP);
        savings = createSavingsAccount("kkk", savingsOffering);

        List<SavingsBO> savingsAccounts = savingsPersistence.getAllSavingsAccount();
        Assert.assertNotNull(savingsAccounts);
        Assert.assertEquals(1, savingsAccounts.size());

    }

    private void createInitialObjects() {
        MeetingBO meeting = TestObjectFactory.createMeeting(TestObjectFactory.getTypicalMeeting());
        center = TestObjectFactory.createWeeklyFeeCenter("Center_Active_test", meeting);
        group = TestObjectFactory.createWeeklyFeeGroupUnderCenter("Group_Active_test", CustomerStatus.GROUP_ACTIVE, center);

    }

    private SavingsBO createSavingsAccount(String globalAccountNum, SavingsOfferingBO savingsOffering)
            throws NumberFormatException, Exception {
        UserContext userContext = new UserContext();
        userContext.setId(PersonnelConstants.SYSTEM_USER);
        userContext.setBranchGlobalNum("1001");
        return TestObjectFactory.createSavingsAccount(globalAccountNum, group, AccountState.SAVINGS_PENDING_APPROVAL,
                new Date(), savingsOffering, userContext);
    }

    private java.sql.Date offSetCurrentDate(int noOfDays) {
        Calendar currentDateCalendar = new GregorianCalendar();
        int year = currentDateCalendar.get(Calendar.YEAR);
        int month = currentDateCalendar.get(Calendar.MONTH);
        int day = currentDateCalendar.get(Calendar.DAY_OF_MONTH);
        currentDateCalendar = new GregorianCalendar(year, month, day - noOfDays);
        return new java.sql.Date(currentDateCalendar.getTimeInMillis());
    }
}
