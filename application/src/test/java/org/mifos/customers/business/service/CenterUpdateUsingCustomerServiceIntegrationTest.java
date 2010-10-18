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

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mifos.application.collectionsheet.persistence.CenterBuilder;
import org.mifos.application.collectionsheet.persistence.MeetingBuilder;
import org.mifos.application.master.business.MifosCurrency;
import org.mifos.application.master.business.SupportedLocalesEntity;
import org.mifos.application.meeting.business.MeetingBO;
import org.mifos.application.servicefacade.CenterUpdate;
import org.mifos.config.Localization;
import org.mifos.customers.business.CustomerNoteEntity;
import org.mifos.customers.business.CustomerPositionDto;
import org.mifos.customers.center.business.CenterBO;
import org.mifos.customers.office.business.OfficeBO;
import org.mifos.customers.persistence.CustomerDao;
import org.mifos.customers.personnel.business.PersonnelBO;
import org.mifos.customers.util.helpers.CustomerStatus;
import org.mifos.customers.util.helpers.CustomerStatusFlag;
import org.mifos.domain.builders.AddressBuilder;
import org.mifos.domain.builders.PersonnelBuilder;
import org.mifos.dto.domain.CustomFieldDto;
import org.mifos.framework.MifosIntegrationTestCase;
import org.mifos.framework.TestUtils;
import org.mifos.framework.business.util.Address;
import org.mifos.framework.components.audit.util.helpers.AuditConfigurtion;
import org.mifos.framework.hibernate.helper.StaticHibernateUtil;
import org.mifos.framework.util.StandardTestingService;
import org.mifos.framework.util.helpers.IntegrationTestObjectMother;
import org.mifos.framework.util.helpers.Money;
import org.mifos.security.util.UserContext;
import org.mifos.service.test.TestMode;
import org.mifos.test.framework.util.DatabaseCleaner;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mifos.framework.util.helpers.IntegrationTestObjectMother.sampleBranchOffice;
import static org.mifos.framework.util.helpers.IntegrationTestObjectMother.testUser;

/**
 * I test the update of {@link CenterBO}'s using the {@link CustomerService} implementation.
 *
 * This can move from being a 'integrated test' to a 'unit test' once usage of {@link StaticHibernateUtil} is removed from {@link CustomerService}.
 * Then there will only be the need for 'integrated test' at {@link CustomerDao} level.
 */
public class CenterUpdateUsingCustomerServiceIntegrationTest extends MifosIntegrationTestCase {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    // test data
    private CenterBO center;
    private PersonnelBO otherLoanOfficer;

    private static MifosCurrency oldDefaultCurrency;

    @BeforeClass
    public static void initialiseHibernateUtil() {
        Locale locale = Localization.getInstance().getMainLocale();
        AuditConfigurtion.init(locale);
        oldDefaultCurrency = Money.getDefaultCurrency();
        Money.setDefaultCurrency(TestUtils.RUPEE);
        new StandardTestingService().setTestMode(TestMode.INTEGRATION);
    }

    @AfterClass
    public static void resetCurrency() {
        Money.setDefaultCurrency(oldDefaultCurrency);
    }

    @After
    public void cleanDatabaseTablesAfterTest() {
        // NOTE: - only added to stop older integration tests failing due to brittleness
        databaseCleaner.clean();
    }

    @Before
    public void cleanDatabaseTables() {
        databaseCleaner.clean();

        // setup
        String centerName = "Center-IntegrationTest";
        OfficeBO existingBranch = sampleBranchOffice();
        PersonnelBO existingLoanOfficer = testUser();
        MeetingBO weeklyMeeting = new MeetingBuilder().customerMeeting().weekly().every(1).startingToday().build();
        DateTime today = new DateTime();
        Address noAddress = null;
        String noExternalId = null;

        center = new CenterBuilder().withName(centerName)
                                            .with(weeklyMeeting)
                                            .with(existingBranch)
                                            .withLoanOfficer(existingLoanOfficer)
                                            .withMfiJoiningDate(today)
                                            .with(noAddress)
                                            .withExternalId(noExternalId)
                                            .withUserContext()
                                            .build();
        IntegrationTestObjectMother.createCenter(center, center.getCustomerMeetingValue());

        otherLoanOfficer = new PersonnelBuilder().withName("otherLoanOfficer").with(existingBranch).build();
//        IntegrationTestObjectMother.createPersonnel(otherLoanOfficer);
    }

    @Test
    public void canUpdateCenterWithDifferentLoanOfficer() throws Exception {

        // setup
        String externalId = center.getExternalId();
        String mfiJoiningDate = new SimpleDateFormat("dd/MM/yyyy").format(center.getMfiJoiningDate());
        Address address = center.getAddress();
        List<CustomFieldDto> customFields = new ArrayList<CustomFieldDto>();
        List<CustomerPositionDto> customerPositions = new ArrayList<CustomerPositionDto>();
        UserContext userContext = TestUtils.makeUser();
        otherLoanOfficer.setPreferredLocale(new SupportedLocalesEntity(userContext.getLocaleId()));
        IntegrationTestObjectMother.createPersonnel(otherLoanOfficer);
        CenterUpdate centerUpdate = new CenterUpdate(center.getCustomerId(), center.getVersionNo(), otherLoanOfficer.getPersonnelId(), externalId, mfiJoiningDate, address, customFields, customerPositions);


        // exercise test
        customerService.updateCenter(userContext, centerUpdate);

        // verification
        center = customerDao.findCenterBySystemId(center.getGlobalCustNum());
        assertThat(center.getPersonnel().getDisplayName(), is(otherLoanOfficer.getDisplayName()));
    }

    @Test
    public void canUpdateCenterWithNoLoanOfficerWhenCenterIsInactive() throws Exception {

        // setup
        CustomerStatusFlag centerStatusFlag = null;
        CustomerNoteEntity customerNote = null;
        customerService.updateCenterStatus(center, CustomerStatus.CENTER_INACTIVE, centerStatusFlag, customerNote);
        StaticHibernateUtil.flushAndClearSession();
        Short loanOfficerId = null;
        String externalId = center.getExternalId();
        String mfiJoiningDate = new SimpleDateFormat("dd/MM/yyyy").format(center.getMfiJoiningDate());
        Address address = center.getAddress();
        List<CustomFieldDto> customFields = new ArrayList<CustomFieldDto>();
        List<CustomerPositionDto> customerPositions = new ArrayList<CustomerPositionDto>();

        CenterUpdate centerUpdate = new CenterUpdate(center.getCustomerId(), center.getVersionNo(), loanOfficerId, externalId, mfiJoiningDate, address, customFields, customerPositions);

        UserContext userContext = TestUtils.makeUser();

        // exercise test
        customerService.updateCenter(userContext, centerUpdate);

        // verification
        center = customerDao.findCenterBySystemId(center.getGlobalCustNum());
        assertThat(center.getPersonnel(), is(nullValue()));
    }

    @Test
    public void canUpdateCenterWithDifferentMfiJoiningDateInPastOrFuture() throws Exception {

        // setup
        Short loanOfficerId = center.getPersonnel().getPersonnelId();
        String externalId = center.getExternalId();

        LocalDate dateInPast = new LocalDate(center.getMfiJoiningDate()).minusWeeks(4);
        String mfiJoiningDate = new SimpleDateFormat("dd/MM/yyyy").format(dateInPast.toDateMidnight().toDate());

        Address address = center.getAddress();
        List<CustomFieldDto> customFields = new ArrayList<CustomFieldDto>();
        List<CustomerPositionDto> customerPositions = new ArrayList<CustomerPositionDto>();

        CenterUpdate centerUpdate = new CenterUpdate(center.getCustomerId(), center.getVersionNo(), loanOfficerId, externalId, mfiJoiningDate, address, customFields, customerPositions);

        UserContext userContext = TestUtils.makeUser();

        // exercise test
        customerService.updateCenter(userContext, centerUpdate);

        // verification
        center = customerDao.findCenterBySystemId(center.getGlobalCustNum());
        assertThat(center.getMfiJoiningDate(), is(dateInPast.toDateMidnight().toDate()));
    }

    @Test
    public void canUpdateCenterWithExternalIdAndAddress() throws Exception {

        // setup
        Short loanOfficerId = center.getPersonnel().getPersonnelId();
        String newExternalId = "ext123";

        LocalDate dateInPast = new LocalDate(center.getMfiJoiningDate()).minusWeeks(4);
        String mfiJoiningDate = new SimpleDateFormat("dd/MM/yyyy").format(dateInPast.toDateMidnight().toDate());

        Address newAddress = new AddressBuilder().anAddress();
        List<CustomFieldDto> customFields = new ArrayList<CustomFieldDto>();
        List<CustomerPositionDto> customerPositions = new ArrayList<CustomerPositionDto>();

        CenterUpdate centerUpdate = new CenterUpdate(center.getCustomerId(), center.getVersionNo(), loanOfficerId, newExternalId, mfiJoiningDate, newAddress, customFields, customerPositions);

        UserContext userContext = TestUtils.makeUser();

        // exercise test
        customerService.updateCenter(userContext, centerUpdate);

        // verification
        center = customerDao.findCenterBySystemId(center.getGlobalCustNum());
        assertThat(center.getExternalId(), is(newExternalId));
        assertThat(center.getAddress().getDisplayAddress(), is("line1, line2, line3"));
    }

    /**
     * FIXME - #00001 - keithw - add custom fields to center being saved.
     */
    @Test
    public void canUpdateCenterWithMandatoryAdditionalFields() throws Exception {

    }

    /**
     * FIXME - #00001 - keithw - add clients beneath center-group hierarchy to validate this..
     */
    @Test
    public void canUpdateCenterWithCustomerPositionsAssignedToCenterClients() throws Exception {
    }
}