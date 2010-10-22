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

package org.mifos.customers.business.service;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mifos.accounts.business.AccountActionDateEntity;
import org.mifos.accounts.business.AccountBO;
import org.mifos.accounts.business.AccountFeesActionDetailEntity;
import org.mifos.accounts.business.AccountStateMachines;
import org.mifos.accounts.loan.business.LoanBO;
import org.mifos.accounts.productdefinition.business.LoanOfferingBO;
import org.mifos.accounts.productdefinition.business.SavingsOfferingBO;
import org.mifos.accounts.savings.business.SavingsBO;
import org.mifos.accounts.savings.util.helpers.SavingsTestHelper;
import org.mifos.accounts.util.helpers.AccountState;
import org.mifos.accounts.util.helpers.AccountStateFlag;
import org.mifos.accounts.util.helpers.AccountStates;
import org.mifos.accounts.util.helpers.AccountTypes;
import org.mifos.accounts.util.helpers.WaiveEnum;
import org.mifos.application.meeting.business.MeetingBO;
import org.mifos.config.AccountingRulesConstants;
import org.mifos.config.ConfigurationManager;
import org.mifos.customers.business.CustomerAccountBOTestUtils;
import org.mifos.customers.business.CustomerActivityEntity;
import org.mifos.customers.business.CustomerBO;
import org.mifos.customers.business.CustomerFeeScheduleEntity;
import org.mifos.customers.business.CustomerNoteEntity;
import org.mifos.customers.business.CustomerScheduleEntity;
import org.mifos.customers.business.CustomerStatusEntity;
import org.mifos.customers.center.business.CenterBO;
import org.mifos.customers.checklist.business.CheckListBO;
import org.mifos.customers.checklist.business.CustomerCheckListBO;
import org.mifos.customers.checklist.util.helpers.CheckListConstants;
import org.mifos.customers.client.business.ClientBO;
import org.mifos.customers.group.business.GroupBO;
import org.mifos.customers.office.business.OfficeBO;
import org.mifos.customers.office.business.OfficecFixture;
import org.mifos.customers.persistence.CustomerPersistence;
import org.mifos.customers.personnel.business.PersonnelBO;
import org.mifos.customers.api.CustomerLevel;
import org.mifos.customers.util.helpers.CustomerRecentActivityDto;
import org.mifos.customers.util.helpers.CustomerStatus;
import org.mifos.customers.util.helpers.CustomerStatusFlag;
import org.mifos.framework.MifosIntegrationTestCase;
import org.mifos.framework.TestUtils;
import org.mifos.framework.exceptions.ApplicationException;
import org.mifos.framework.exceptions.ServiceException;
import org.mifos.framework.exceptions.SystemException;
import org.mifos.framework.hibernate.helper.QueryResult;
import org.mifos.framework.hibernate.helper.StaticHibernateUtil;
import org.mifos.framework.util.helpers.Money;
import org.mifos.framework.util.helpers.TestObjectFactory;
import org.mifos.security.util.UserContext;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;

public class CustomerBusinessServiceIntegrationTest extends MifosIntegrationTestCase {

    private static final Integer THREE = Integer.valueOf(3);
    private static final Integer ONE = Integer.valueOf(1);
    private static final OfficeBO OFFICE = OfficecFixture.createOffice(Short.valueOf("1"));

    private CustomerBO center;
    private GroupBO group;
    private CustomerBO client;
    private AccountBO account;
    private LoanBO groupAccount;
    private LoanBO clientAccount;
    private SavingsBO clientSavingsAccount;
    private MeetingBO meeting;
    private final SavingsTestHelper helper = new SavingsTestHelper();
    private SavingsOfferingBO savingsOffering;
    private SavingsBO savingsBO;
    private CustomerBusinessService service;
    private CustomerPersistence customerPersistenceMock;
    private CustomerBusinessService customerBusinessServiceWithMock;

    @Before
    public void setUp() throws Exception {
        service = new CustomerBusinessService();
        customerPersistenceMock = createMock(CustomerPersistence.class);
        customerBusinessServiceWithMock = new CustomerBusinessService(customerPersistenceMock);
    }

    @After
    public void tearDown() throws Exception {
        try {
            // if there is an additional currency code defined, then clear it
            ConfigurationManager.getInstance().clearProperty(AccountingRulesConstants.ADDITIONAL_CURRENCY_CODES);
            clientSavingsAccount = null;
            groupAccount = null;
            clientAccount = null;
            account = null;
            savingsBO = null;
            client = null;
            group = null;
            center = null;
            StaticHibernateUtil.flushSession();
        } catch (Exception e) {
            // throwing here tends to mask other failures
            e.printStackTrace();
        }
    }

    @Test
    public void testSearchGropAndClient() throws Exception {
        createInitialCustomers();
        QueryResult queryResult = new CustomerBusinessService().searchGroupClient("cl", Short.valueOf("1"));
        Assert.assertNotNull(queryResult);
        Assert.assertEquals(1, queryResult.getSize());
        Assert.assertEquals(1, queryResult.get(0, 10).size());

    }

    @Test
    public void testSearchCustForSavings() throws Exception {
        createInitialCustomers();
        QueryResult queryResult = new CustomerBusinessService().searchCustForSavings("c", Short.valueOf("1"));
        Assert.assertNotNull(queryResult);
        Assert.assertEquals(2, queryResult.getSize());
        Assert.assertEquals(2, queryResult.get(0, 10).size());

    }

    @Test
    public void testGetAllActivityView() throws Exception {
        MeetingBO meeting = TestObjectFactory.createMeeting(TestObjectFactory.getTypicalMeeting());
        center = TestObjectFactory.createWeeklyFeeCenter("Center", meeting);
        StaticHibernateUtil.flushSession();
        center = TestObjectFactory.getCenter(center.getCustomerId());
        List<CustomerRecentActivityDto> customerActivityViewList = service
                .getAllActivityView(center.getGlobalCustNum());
        Assert.assertEquals(0, customerActivityViewList.size());
        center.getCustomerAccount().setUserContext(TestUtils.makeUser());
        center.getCustomerAccount().waiveAmountDue(WaiveEnum.ALL);
        TestObjectFactory.flushandCloseSession();
        center = TestObjectFactory.getCenter(center.getCustomerId());
        List<CustomerActivityEntity> customerActivityDetails = center.getCustomerAccount().getCustomerActivitDetails();
        Assert.assertEquals(1, customerActivityDetails.size());
        for (CustomerActivityEntity customerActivityEntity : customerActivityDetails) {
            Assert.assertEquals(new Money(getCurrency(), "100"), customerActivityEntity.getAmount());
        }
        List<CustomerRecentActivityDto> customerActivityView = service.getAllActivityView(center.getGlobalCustNum());
        Assert.assertEquals(1, customerActivityView.size());
        for (CustomerRecentActivityDto view : customerActivityView) {
            Assert.assertEquals(new Money(getCurrency(), "100").toString(), view.getAmount());
            Assert.assertEquals("Amnt waived", view.getDescription());
            Assert.assertEquals(TestObjectFactory.getContext().getName(), view.getPostedBy());
        }
    }

    @Test
    @Ignore
    public void testFailureGetRecentActivityView() throws Exception {
        MeetingBO meeting = TestObjectFactory.createMeeting(TestObjectFactory.getTypicalMeeting());
        center = TestObjectFactory.createWeeklyFeeCenter("Center", meeting);
        StaticHibernateUtil.flushSession();
        center = TestObjectFactory.getCenter(center.getCustomerId());

        try {
            service.getAllActivityView(center.getGlobalCustNum());
            Assert.assertTrue(false);
        } catch (ServiceException e) {
            Assert.assertTrue(true);
        }
        StaticHibernateUtil.flushSession();
    }

    @Test
    public void testGetRecentActivityView() throws Exception {
        MeetingBO meeting = TestObjectFactory.createMeeting(TestObjectFactory.getTypicalMeeting());
        center = TestObjectFactory.createWeeklyFeeCenter("Center", meeting);
        StaticHibernateUtil.flushSession();
        center = TestObjectFactory.getCenter(center.getCustomerId());
        List<CustomerRecentActivityDto> customerActivityViewList = service
                .getAllActivityView(center.getGlobalCustNum());
        Assert.assertEquals(0, customerActivityViewList.size());
        UserContext uc = TestUtils.makeUser();
        center.getCustomerAccount().setUserContext(uc);
        center.getCustomerAccount().waiveAmountDue(WaiveEnum.ALL);
        TestObjectFactory.flushandCloseSession();

        center = TestObjectFactory.getCenter(center.getCustomerId());
        for (AccountActionDateEntity accountAction : center.getCustomerAccount().getAccountActionDates()) {
            CustomerScheduleEntity accountActionDateEntity = (CustomerScheduleEntity) accountAction;
            if (accountActionDateEntity.getInstallmentId().equals(Short.valueOf("1"))) {
                Set<AccountFeesActionDetailEntity> accountFeesActionDetails = accountActionDateEntity
                        .getAccountFeesActionDetails();
                for (AccountFeesActionDetailEntity accountFeesActionDetailEntity : accountFeesActionDetails) {
                    CustomerAccountBOTestUtils.setFeeAmount((CustomerFeeScheduleEntity) accountFeesActionDetailEntity,
                            new Money(getCurrency(), "100"));
                }
            }
        }
        TestObjectFactory.updateObject(center);
        center.getCustomerAccount().setUserContext(uc);
        center.getCustomerAccount().waiveAmountDue(WaiveEnum.ALL);
        TestObjectFactory.flushandCloseSession();

        center = TestObjectFactory.getCenter(center.getCustomerId());
        for (AccountActionDateEntity accountAction : center.getCustomerAccount().getAccountActionDates()) {
            CustomerScheduleEntity accountActionDateEntity = (CustomerScheduleEntity) accountAction;
            if (accountActionDateEntity.getInstallmentId().equals(Short.valueOf("1"))) {
                Set<AccountFeesActionDetailEntity> accountFeesActionDetails = accountActionDateEntity
                        .getAccountFeesActionDetails();
                for (AccountFeesActionDetailEntity accountFeesActionDetailEntity : accountFeesActionDetails) {
                    CustomerAccountBOTestUtils.setFeeAmount((CustomerFeeScheduleEntity) accountFeesActionDetailEntity,
                            new Money(getCurrency(), "100"));
                }
            }
        }
        TestObjectFactory.updateObject(center);
        center.getCustomerAccount().setUserContext(uc);
        center.getCustomerAccount().waiveAmountDue(WaiveEnum.ALL);
        TestObjectFactory.flushandCloseSession();

        center = TestObjectFactory.getCenter(center.getCustomerId());
        for (AccountActionDateEntity accountAction : center.getCustomerAccount().getAccountActionDates()) {
            CustomerScheduleEntity accountActionDateEntity = (CustomerScheduleEntity) accountAction;
            if (accountActionDateEntity.getInstallmentId().equals(Short.valueOf("3"))) {
                Set<AccountFeesActionDetailEntity> accountFeesActionDetails = accountActionDateEntity
                        .getAccountFeesActionDetails();
                for (AccountFeesActionDetailEntity accountFeesActionDetailEntity : accountFeesActionDetails) {
                    CustomerAccountBOTestUtils.setFeeAmount((CustomerFeeScheduleEntity) accountFeesActionDetailEntity,
                            new Money(getCurrency(), "20"));
                }
            }
        }
        TestObjectFactory.updateObject(center);
        center.getCustomerAccount().setUserContext(uc);
        center.getCustomerAccount().waiveAmountDue(WaiveEnum.ALL);
        TestObjectFactory.flushandCloseSession();

        center = TestObjectFactory.getCenter(center.getCustomerId());
        List<CustomerActivityEntity> customerActivityDetails = center.getCustomerAccount().getCustomerActivitDetails();
        Assert.assertEquals(3, customerActivityDetails.size());
        for (CustomerActivityEntity customerActivityEntity : customerActivityDetails) {
            Assert.assertEquals(new Money(getCurrency(), "100"), customerActivityEntity.getAmount());
        }

        List<CustomerRecentActivityDto> customerActivityView = service.getRecentActivityView(center.getCustomerId());
        Assert.assertEquals(3, customerActivityView.size());
        for (CustomerRecentActivityDto view : customerActivityView) {
            Assert.assertEquals(new Money(getCurrency(), "100").toString(), view.getAmount());
            Assert.assertEquals(TestObjectFactory.getContext().getName(), view.getPostedBy());
        }
    }

    @Test
    public void testFindBySystemId() throws Exception {
        MeetingBO meeting = TestObjectFactory.createMeeting(TestObjectFactory.getTypicalMeeting());
        center = TestObjectFactory.createWeeklyFeeCenter("Center_Active_test", meeting);
        group = TestObjectFactory.createWeeklyFeeGroupUnderCenter("Group_Active_test", CustomerStatus.GROUP_ACTIVE,
                center);
        savingsBO = getSavingsAccount(group, "fsaf5", "ads5");
        StaticHibernateUtil.flushAndClearSession();
        group = (GroupBO) service.findBySystemId(group.getGlobalCustNum());
        Assert.assertEquals("Group_Active_test", group.getDisplayName());
        Assert.assertEquals(2, group.getAccounts().size());
        Assert.assertEquals(0, group.getOpenLoanAccounts().size());
        Assert.assertEquals(1, group.getOpenSavingAccounts().size());
        Assert.assertEquals(CustomerStatus.GROUP_ACTIVE, group.getStatus());
        StaticHibernateUtil.flushSession();
        savingsBO = TestObjectFactory.getObject(SavingsBO.class, savingsBO.getAccountId());
        center = TestObjectFactory.getCenter(center.getCustomerId());
        group = TestObjectFactory.getGroup(group.getCustomerId());
    }

    @Test
    public void testSuccessfulGet() throws Exception {
        center = createCenter("MyCenter");
        savingsBO = getSavingsAccount(center, "fsaf5", "ads5");
        StaticHibernateUtil.flushAndClearSession();
        center = service.getCustomer(center.getCustomerId());
        Assert.assertNotNull(center);
        Assert.assertEquals("MyCenter", center.getDisplayName());
        Assert.assertEquals(2, center.getAccounts().size());
        Assert.assertEquals(0, center.getOpenLoanAccounts().size());
        Assert.assertEquals(1, center.getOpenSavingAccounts().size());
        Assert.assertEquals(CustomerStatus.CENTER_ACTIVE.getValue(), center.getCustomerStatus().getId());
        StaticHibernateUtil.flushSession();
        savingsBO = TestObjectFactory.getObject(SavingsBO.class, savingsBO.getAccountId());
        center = TestObjectFactory.getCenter(center.getCustomerId());
    }

    @Test
    public void testGetCustomerChecklist() throws Exception {

        MeetingBO meeting = TestObjectFactory.createMeeting(TestObjectFactory.getTypicalMeeting());
        center = TestObjectFactory.createWeeklyFeeCenter("Center_Active_test", meeting);
        group = TestObjectFactory.createWeeklyFeeGroupUnderCenter("Group", CustomerStatus.GROUP_ACTIVE, center);
        client = TestObjectFactory.createClient("client1", CustomerStatus.CLIENT_ACTIVE, group);
        CustomerCheckListBO checklistCenter = TestObjectFactory.createCustomerChecklist(center.getCustomerLevel()
                .getId(), center.getCustomerStatus().getId(), CheckListConstants.STATUS_ACTIVE);
        CustomerCheckListBO checklistClient = TestObjectFactory.createCustomerChecklist(client.getCustomerLevel()
                .getId(), client.getCustomerStatus().getId(), CheckListConstants.STATUS_INACTIVE);
        CustomerCheckListBO checklistGroup = TestObjectFactory.createCustomerChecklist(
                group.getCustomerLevel().getId(), group.getCustomerStatus().getId(), CheckListConstants.STATUS_ACTIVE);
        StaticHibernateUtil.flushSession();
        Assert.assertEquals(1,
                service.getStatusChecklist(center.getCustomerStatus().getId(), center.getCustomerLevel().getId())
                        .size());
        client = (ClientBO) (StaticHibernateUtil.getSessionTL().get(ClientBO.class,
                Integer.valueOf(client.getCustomerId())));
        group = (GroupBO) (StaticHibernateUtil.getSessionTL()
                .get(GroupBO.class, Integer.valueOf(group.getCustomerId())));
        center = (CenterBO) (StaticHibernateUtil.getSessionTL().get(CenterBO.class,
                Integer.valueOf(center.getCustomerId())));
        checklistCenter = (CustomerCheckListBO) (StaticHibernateUtil.getSessionTL().get(CheckListBO.class, new Short(
                checklistCenter.getChecklistId())));
        checklistClient = (CustomerCheckListBO) (StaticHibernateUtil.getSessionTL().get(CheckListBO.class, new Short(
                checklistClient.getChecklistId())));
        checklistGroup = (CustomerCheckListBO) (StaticHibernateUtil.getSessionTL().get(CheckListBO.class, new Short(
                checklistGroup.getChecklistId())));

    }

    @Test
    public void testRetrieveAllCustomerStatusList() throws NumberFormatException, SystemException, ApplicationException {
        MeetingBO meeting = TestObjectFactory.createMeeting(TestObjectFactory.getTypicalMeeting());
        center = TestObjectFactory.createWeeklyFeeCenter("Center_Active_test", meeting);
        Assert.assertEquals(2, service.retrieveAllCustomerStatusList(center.getCustomerLevel().getId()).size());
    }

    @Test
    public void testGetAllCustomerNotes() throws Exception {
        MeetingBO meeting = TestObjectFactory.createMeeting(TestObjectFactory.getTypicalMeeting());
        center = TestObjectFactory.createWeeklyFeeCenter("Center_Active_test", meeting);
        center.addCustomerNotes(TestObjectFactory.getCustomerNote("Test Note", center));
        TestObjectFactory.updateObject(center);
        Assert.assertEquals(1, service.getAllCustomerNotes(center.getCustomerId()).getSize());
        for (CustomerNoteEntity note : center.getCustomerNotes()) {
            Assert.assertEquals("Test Note", note.getComment());
            Assert.assertEquals(center.getPersonnel().getPersonnelId(), note.getPersonnel().getPersonnelId());
        }
        center = (CenterBO) (StaticHibernateUtil.getSessionTL().get(CenterBO.class,
                Integer.valueOf(center.getCustomerId())));
    }

    @Test
    public void testGetAllCustomerNotesWithZeroNotes() throws Exception {
        MeetingBO meeting = TestObjectFactory.createMeeting(TestObjectFactory.getTypicalMeeting());
        center = TestObjectFactory.createWeeklyFeeCenter("Center_Active_test", meeting);
        Assert.assertEquals(0, service.getAllCustomerNotes(center.getCustomerId()).getSize());
        Assert.assertEquals(0, center.getCustomerNotes().size());
    }

    @Test
    public void testGetStatusName() throws Exception {
        createInitialCustomers();
        AccountStateMachines.getInstance().initialize(TestObjectFactory.TEST_LOCALE, center.getOffice().getOfficeId(),
                AccountTypes.CUSTOMER_ACCOUNT, CustomerLevel.CENTER);
        String statusNameForCenter = service.getStatusName(TestObjectFactory.TEST_LOCALE, center.getStatus(),
                CustomerLevel.CENTER);
        Assert.assertEquals("Active", statusNameForCenter);

        AccountStateMachines.getInstance().initialize(TestObjectFactory.TEST_LOCALE, group.getOffice().getOfficeId(),
                AccountTypes.CUSTOMER_ACCOUNT, CustomerLevel.GROUP);
        String statusNameForGroup = service.getStatusName(TestObjectFactory.TEST_LOCALE, group.getStatus(),
                CustomerLevel.GROUP);
        Assert.assertEquals("Active", statusNameForGroup);

        AccountStateMachines.getInstance().initialize(TestObjectFactory.TEST_LOCALE, client.getOffice().getOfficeId(),
                AccountTypes.CUSTOMER_ACCOUNT, CustomerLevel.CLIENT);
        String statusNameForClient = service.getStatusName(TestObjectFactory.TEST_LOCALE, client.getStatus(),
                CustomerLevel.CLIENT);
        Assert.assertNotNull("Active", statusNameForClient);
    }

    @Test
    public void testGetFlagName() throws Exception {
        MeetingBO meeting = TestObjectFactory.createMeeting(TestObjectFactory.getTypicalMeeting());
        center = TestObjectFactory.createWeeklyFeeCenter("Center_Active_test", meeting);
        group = TestObjectFactory.createWeeklyFeeGroupUnderCenter("Group", CustomerStatus.GROUP_ACTIVE, center);
        client = TestObjectFactory.createClient("client", CustomerStatus.CLIENT_CLOSED, group);

        AccountStateMachines.getInstance().initialize(TestObjectFactory.TEST_LOCALE, client.getOffice().getOfficeId(),
                AccountTypes.CUSTOMER_ACCOUNT, CustomerLevel.CLIENT);
        String flagNameForClient = service.getFlagName(TestObjectFactory.TEST_LOCALE,
                CustomerStatusFlag.CLIENT_CLOSED_DUPLICATE, CustomerLevel.CLIENT);
        Assert.assertNotNull("Duplicate", flagNameForClient);

        AccountStateMachines.getInstance().initialize(TestObjectFactory.TEST_LOCALE, group.getOffice().getOfficeId(),
                AccountTypes.CUSTOMER_ACCOUNT, CustomerLevel.GROUP);
        String flagNameForGroup = service.getFlagName(TestObjectFactory.TEST_LOCALE,
                CustomerStatusFlag.GROUP_CLOSED_DUPLICATE, CustomerLevel.GROUP);
        Assert.assertNotNull("Duplicate", flagNameForGroup);
    }

    @Test
    public void testGetStatusList() throws Exception {
        createInitialCustomers();
        AccountStateMachines.getInstance().initialize(TestObjectFactory.TEST_LOCALE, center.getOffice().getOfficeId(),
                AccountTypes.CUSTOMER_ACCOUNT, CustomerLevel.CENTER);
        List<CustomerStatusEntity> statusListForCenter = service.getStatusList(center.getCustomerStatus(),
                CustomerLevel.CENTER, TestObjectFactory.TEST_LOCALE);
        Assert.assertEquals(1, statusListForCenter.size());

        AccountStateMachines.getInstance().initialize(TestObjectFactory.TEST_LOCALE, group.getOffice().getOfficeId(),
                AccountTypes.CUSTOMER_ACCOUNT, CustomerLevel.GROUP);
        List<CustomerStatusEntity> statusListForGroup = service.getStatusList(group.getCustomerStatus(),
                CustomerLevel.GROUP, TestObjectFactory.TEST_LOCALE);
        Assert.assertEquals(2, statusListForGroup.size());

        AccountStateMachines.getInstance().initialize(TestObjectFactory.TEST_LOCALE, client.getOffice().getOfficeId(),
                AccountTypes.CUSTOMER_ACCOUNT, CustomerLevel.CLIENT);
        List<CustomerStatusEntity> statusListForClient = service.getStatusList(client.getCustomerStatus(),
                CustomerLevel.CLIENT, TestObjectFactory.TEST_LOCALE);
        Assert.assertEquals(2, statusListForClient.size());
    }

    @Test
    public void testSearch() throws Exception {

        center = createCenter("MyCenter");
        QueryResult queryResult = service
                .search("MyCenter", Short.valueOf("3"), Short.valueOf("1"), Short.valueOf("1"));
        Assert.assertNotNull(queryResult);
        Assert.assertEquals(1, queryResult.getSize());
    }

    @Test
    public void testGetAllClosedAccounts() throws Exception {
        getCustomer();
        groupAccount.changeStatus(AccountState.LOAN_CANCELLED.getValue(), AccountStateFlag.LOAN_WITHDRAW.getValue(),
                "WITHDRAW LOAN ACCOUNT");
        clientAccount.changeStatus(AccountState.LOAN_CLOSED_WRITTEN_OFF.getValue(), null, "WITHDRAW LOAN ACCOUNT");
        clientSavingsAccount.changeStatus(AccountState.SAVINGS_CANCELLED.getValue(),
                AccountStateFlag.SAVINGS_REJECTED.getValue(), "WITHDRAW LOAN ACCOUNT");
        TestObjectFactory.updateObject(groupAccount);
        TestObjectFactory.updateObject(clientAccount);
        TestObjectFactory.updateObject(clientSavingsAccount);
        Assert.assertEquals(1, service
                .getAllClosedAccount(client.getCustomerId(), AccountTypes.LOAN_ACCOUNT.getValue()).size());
        Assert.assertEquals(1, service.getAllClosedAccount(group.getCustomerId(), AccountTypes.LOAN_ACCOUNT.getValue())
                .size());
        Assert.assertEquals(1,
                service.getAllClosedAccount(client.getCustomerId(), AccountTypes.SAVINGS_ACCOUNT.getValue()).size());
    }

    @Test
    public void testGetAllClosedAccountsWhenNoAccountsClosed() throws Exception {
        getCustomer();
        Assert.assertEquals(0, service
                .getAllClosedAccount(client.getCustomerId(), AccountTypes.LOAN_ACCOUNT.getValue()).size());
        Assert.assertEquals(0, service.getAllClosedAccount(group.getCustomerId(), AccountTypes.LOAN_ACCOUNT.getValue())
                .size());
        Assert.assertEquals(0,
                service.getAllClosedAccount(client.getCustomerId(), AccountTypes.SAVINGS_ACCOUNT.getValue()).size());
    }

    @Test
    public void testGetActiveCentersUnderUser() throws Exception {
        MeetingBO meeting = TestObjectFactory.createMeeting(TestObjectFactory.getTypicalMeeting());
        center = TestObjectFactory.createWeeklyFeeCenter("center", meeting, Short.valueOf("1"), Short.valueOf("1"));
        PersonnelBO personnel = TestObjectFactory.getPersonnel(Short.valueOf("1"));
        List<CustomerBO> customers = service.getActiveCentersUnderUser(personnel);
        Assert.assertNotNull(customers);
        Assert.assertEquals(1, customers.size());
    }

    @Test
    public void testgetGroupsUnderUser() throws Exception {
        MeetingBO meeting = TestObjectFactory.createMeeting(TestObjectFactory.getTypicalMeeting());
        center = TestObjectFactory.createWeeklyFeeCenter("center", meeting, Short.valueOf("1"), Short.valueOf("1"));
        group = TestObjectFactory.createWeeklyFeeGroupUnderCenter("Group", CustomerStatus.GROUP_ACTIVE, center);
        PersonnelBO personnel = TestObjectFactory.getPersonnel(Short.valueOf("1"));
        List<CustomerBO> customers = service.getGroupsUnderUser(personnel);
        Assert.assertNotNull(customers);
        Assert.assertEquals(1, customers.size());
    }

    @Test
    public void testGetCustomersByLevelId() throws Exception {
        createInitialCustomers();

        List<CustomerBO> client = service.getCustomersByLevelId(Short.parseShort("1"));
        Assert.assertNotNull(client);
        Assert.assertEquals(1, client.size());

        List<CustomerBO> group = service.getCustomersByLevelId(Short.parseShort("2"));
        Assert.assertNotNull(group);
        Assert.assertEquals(1, group.size());

        List<CustomerBO> center = service.getCustomersByLevelId(Short.parseShort("3"));
        Assert.assertNotNull(center);
        Assert.assertEquals(1, client.size());

    }

    private void createInitialCustomers() throws Exception {
        center = createCenter("Center_Active_test");
        group = TestObjectFactory.createWeeklyFeeGroupUnderCenter("Group", CustomerStatus.GROUP_ACTIVE, center);
        client = TestObjectFactory.createClient("client", CustomerStatus.CLIENT_ACTIVE, group);
    }

    private CenterBO createCenter(String name) throws Exception {
        meeting = TestObjectFactory.createMeeting(TestObjectFactory.getTypicalMeeting());
        return TestObjectFactory.createWeeklyFeeCenter(name, meeting);
    }

    private SavingsBO getSavingsAccount(CustomerBO customerBO, String offeringName, String shortName) throws Exception {
        savingsOffering = helper.createSavingsOffering(offeringName, shortName);
        return TestObjectFactory.createSavingsAccount("000100000000017", customerBO,
                AccountStates.SAVINGS_ACC_APPROVED, new Date(System.currentTimeMillis()), savingsOffering);
    }

    private void getCustomer() throws Exception {
        Date startDate = new Date(System.currentTimeMillis());

        createInitialCustomers();
        LoanOfferingBO loanOffering1 = TestObjectFactory.createLoanOffering("Loanwer", "43fs", startDate, meeting);
        LoanOfferingBO loanOffering2 = TestObjectFactory.createLoanOffering("Loancd123", "vfr", startDate, meeting);
        groupAccount = TestObjectFactory.createLoanAccount("42423142341", group,
                AccountState.LOAN_ACTIVE_IN_GOOD_STANDING, startDate, loanOffering1);
        clientAccount = TestObjectFactory.createLoanAccount("3243", client, AccountState.LOAN_ACTIVE_IN_GOOD_STANDING,
                startDate, loanOffering2);
        clientSavingsAccount = getSavingsAccount(client, "SavingPrd11", "abc2");
    }

    @Test
    public void testDropOutRate() throws Exception {
        expect(customerPersistenceMock.getDropOutClientsCountForOffice(OFFICE)).andReturn(ONE);
        expect(customerPersistenceMock.getActiveOrHoldClientCountForOffice(OFFICE)).andReturn(THREE);
        replay(customerPersistenceMock);
        BigDecimal dropOutRate = customerBusinessServiceWithMock.getClientDropOutRateForOffice(OFFICE);
        verify(customerPersistenceMock);
        Assert.assertEquals(25d, dropOutRate.doubleValue(), 0.001);
    }

    @Test
    public void testVeryPoorClientDropoutRate() throws Exception {
        expect(customerPersistenceMock.getVeryPoorDropOutClientsCountForOffice(OFFICE)).andReturn(ONE);
        expect(customerPersistenceMock.getVeryPoorActiveOrHoldClientCountForOffice(OFFICE)).andReturn(THREE);
        replay(customerPersistenceMock);
        BigDecimal veryPoorClientDropoutRateForOffice = customerBusinessServiceWithMock
                .getVeryPoorClientDropoutRateForOffice(OFFICE);
        Assert.assertEquals(25d, veryPoorClientDropoutRateForOffice.doubleValue(), 0.001);
        verify(customerPersistenceMock);
    }
}
