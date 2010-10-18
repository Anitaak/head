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

package org.mifos.framework.util.helpers;

import static org.mifos.application.meeting.util.helpers.MeetingType.CUSTOMER_MEETING;
import static org.mifos.application.meeting.util.helpers.RecurrenceType.WEEKLY;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.mifos.accounts.business.AccountActionDateEntity;
import org.mifos.accounts.business.AccountBO;
import org.mifos.accounts.business.AccountFeesActionDetailEntity;
import org.mifos.accounts.business.AccountFeesEntity;
import org.mifos.accounts.business.AccountPaymentEntity;
import org.mifos.accounts.business.AccountStateEntity;
import org.mifos.accounts.business.AccountTestUtils;
import org.mifos.accounts.business.AccountTrxnEntity;
import org.mifos.accounts.business.FeesTrxnDetailEntity;
import org.mifos.accounts.exceptions.AccountException;
import org.mifos.accounts.fees.business.AmountFeeBO;
import org.mifos.accounts.fees.business.CategoryTypeEntity;
import org.mifos.accounts.fees.business.FeeBO;
import org.mifos.accounts.fees.business.FeeDto;
import org.mifos.accounts.fees.business.FeeFormulaEntity;
import org.mifos.accounts.fees.business.FeeFrequencyTypeEntity;
import org.mifos.accounts.fees.business.FeePaymentEntity;
import org.mifos.accounts.fees.business.RateFeeBO;
import org.mifos.accounts.fees.servicefacade.FeeFormulaDto;
import org.mifos.accounts.fees.servicefacade.FeeStatusDto;
import org.mifos.accounts.fees.util.helpers.FeeCategory;
import org.mifos.accounts.fees.util.helpers.FeeFormula;
import org.mifos.accounts.fees.util.helpers.FeeFrequencyType;
import org.mifos.accounts.fees.util.helpers.FeePayment;
import org.mifos.accounts.financial.business.FinancialTransactionBO;
import org.mifos.accounts.financial.business.GLCodeEntity;
import org.mifos.accounts.financial.util.helpers.ChartOfAccountsCache;
import org.mifos.accounts.fund.business.FundBO;
import org.mifos.accounts.loan.business.LoanBO;
import org.mifos.accounts.loan.business.LoanBOIntegrationTest;
import org.mifos.accounts.loan.business.LoanBOTestUtils;
import org.mifos.accounts.loan.business.LoanScheduleEntity;
import org.mifos.accounts.loan.business.LoanTrxnDetailEntity;
import org.mifos.accounts.loan.util.helpers.LoanAccountDto;
import org.mifos.accounts.productdefinition.business.GracePeriodTypeEntity;
import org.mifos.accounts.productdefinition.business.InterestCalcTypeEntity;
import org.mifos.accounts.productdefinition.business.LoanOfferingBO;
import org.mifos.accounts.productdefinition.business.LoanOfferingTestUtils;
import org.mifos.accounts.productdefinition.business.PrdApplicableMasterEntity;
import org.mifos.accounts.productdefinition.business.PrdOfferingBO;
import org.mifos.accounts.productdefinition.business.PrdOfferingMeetingEntity;
import org.mifos.accounts.productdefinition.business.PrdStatusEntity;
import org.mifos.accounts.productdefinition.business.ProductCategoryBO;
import org.mifos.accounts.productdefinition.business.ProductTypeEntity;
import org.mifos.accounts.productdefinition.business.RecommendedAmntUnitEntity;
import org.mifos.accounts.productdefinition.business.SavingsOfferingBO;
import org.mifos.accounts.productdefinition.business.SavingsTypeEntity;
import org.mifos.accounts.productdefinition.exceptions.ProductDefinitionException;
import org.mifos.accounts.productdefinition.struts.actionforms.LoanPrdActionForm;
import org.mifos.accounts.productdefinition.util.helpers.ApplicableTo;
import org.mifos.accounts.productdefinition.util.helpers.GraceType;
import org.mifos.accounts.productdefinition.util.helpers.InterestCalcType;
import org.mifos.accounts.productdefinition.util.helpers.InterestType;
import org.mifos.accounts.productdefinition.util.helpers.PrdStatus;
import org.mifos.accounts.productdefinition.util.helpers.RecommendedAmountUnit;
import org.mifos.accounts.productdefinition.util.helpers.SavingsType;
import org.mifos.accounts.productsmix.business.ProductMixBO;
import org.mifos.accounts.savings.SavingBOTestUtils;
import org.mifos.accounts.savings.business.SavingsBO;
import org.mifos.accounts.savings.business.SavingsScheduleEntity;
import org.mifos.accounts.savings.util.helpers.SavingsTestHelper;
import org.mifos.accounts.util.helpers.AccountState;
import org.mifos.accounts.util.helpers.AccountTypes;
import org.mifos.accounts.util.helpers.CustomerAccountPaymentData;
import org.mifos.accounts.util.helpers.PaymentData;
import org.mifos.accounts.util.helpers.PaymentStatus;
import org.mifos.application.collectionsheet.business.CollectionSheetEntryAccountFeeActionDto;
import org.mifos.application.collectionsheet.business.CollectionSheetEntryCustomerAccountInstallmentDto;
import org.mifos.application.collectionsheet.business.CollectionSheetEntryInstallmentDto;
import org.mifos.application.collectionsheet.business.CollectionSheetEntryLoanInstallmentDto;
import org.mifos.application.collectionsheet.business.CollectionSheetEntrySavingsInstallmentDto;
import org.mifos.application.holiday.business.Holiday;
import org.mifos.application.master.business.CustomFieldType;
import org.mifos.application.master.business.FundCodeEntity;
import org.mifos.application.master.business.InterestTypesEntity;
import org.mifos.application.master.business.LookUpValueEntity;
import org.mifos.application.master.business.MifosCurrency;
import org.mifos.application.meeting.business.MeetingBO;
import org.mifos.application.meeting.exceptions.MeetingException;
import org.mifos.application.meeting.util.helpers.MeetingType;
import org.mifos.application.meeting.util.helpers.RecurrenceType;
import org.mifos.application.meeting.util.helpers.WeekDay;
import org.mifos.application.servicefacade.DependencyInjectedServiceLocator;
import org.mifos.application.util.helpers.EntityType;
import org.mifos.application.util.helpers.YesNoFlag;
import org.mifos.config.FiscalCalendarRules;
import org.mifos.core.MifosRuntimeException;
import org.mifos.customers.business.CustomerBO;
import org.mifos.customers.business.CustomerCustomFieldEntity;
import org.mifos.customers.business.CustomerLevelEntity;
import org.mifos.customers.business.CustomerNoteEntity;
import org.mifos.customers.business.CustomerPositionEntity;
import org.mifos.customers.business.CustomerScheduleEntity;
import org.mifos.customers.business.CustomerStatusEntity;
import org.mifos.customers.center.business.CenterBO;
import org.mifos.customers.center.persistence.CenterPersistence;
import org.mifos.customers.checklist.business.AccountCheckListBO;
import org.mifos.customers.checklist.business.CheckListBO;
import org.mifos.customers.checklist.business.CheckListDetailEntity;
import org.mifos.customers.checklist.business.CustomerCheckListBO;
import org.mifos.customers.client.business.ClientAttendanceBO;
import org.mifos.customers.client.business.ClientBO;
import org.mifos.customers.client.business.ClientInitialSavingsOfferingEntity;
import org.mifos.customers.client.business.ClientNameDetailDto;
import org.mifos.customers.client.business.ClientPersonalDetailDto;
import org.mifos.customers.client.business.NameType;
import org.mifos.customers.client.persistence.ClientPersistence;
import org.mifos.customers.exceptions.CustomerException;
import org.mifos.customers.group.GroupTemplate;
import org.mifos.customers.group.business.GroupBO;
import org.mifos.customers.group.persistence.GroupPersistence;
import org.mifos.customers.office.business.OfficeBO;
import org.mifos.customers.office.persistence.OfficePersistence;
import org.mifos.customers.office.util.helpers.OfficeLevel;
import org.mifos.customers.office.util.helpers.OperationMode;
import org.mifos.customers.persistence.CustomerPersistence;
import org.mifos.customers.personnel.business.PersonnelBO;
import org.mifos.customers.personnel.persistence.PersonnelPersistence;
import org.mifos.customers.personnel.util.helpers.PersonnelConstants;
import org.mifos.customers.personnel.util.helpers.PersonnelLevel;
import org.mifos.customers.util.helpers.CustomerAccountDto;
import org.mifos.customers.util.helpers.CustomerLevel;
import org.mifos.customers.util.helpers.CustomerStatus;
import org.mifos.dto.domain.CustomFieldDto;
import org.mifos.framework.TestUtils;
import org.mifos.framework.business.AbstractEntity;
import org.mifos.framework.business.util.Address;
import org.mifos.framework.business.util.Name;
import org.mifos.framework.components.audit.business.AuditLog;
import org.mifos.framework.exceptions.ApplicationException;
import org.mifos.framework.exceptions.PersistenceException;
import org.mifos.framework.exceptions.SystemException;
import org.mifos.framework.hibernate.helper.StaticHibernateUtil;
import org.mifos.framework.persistence.TestObjectPersistence;
import org.mifos.framework.util.DateTimeService;
import org.mifos.reports.business.ReportsBO;
import org.mifos.reports.business.ReportsCategoryBO;
import org.mifos.schedule.ScheduledDateGeneration;
import org.mifos.schedule.ScheduledEvent;
import org.mifos.schedule.ScheduledEventFactory;
import org.mifos.schedule.internal.HolidayAndWorkingDaysAndMoratoriaScheduledDateGeneration;
import org.mifos.security.authentication.EncryptionService;
import org.mifos.security.rolesandpermission.business.ActivityEntity;
import org.mifos.security.rolesandpermission.business.RoleBO;
import org.mifos.security.util.ActivityContext;
import org.mifos.security.util.UserContext;

/**
 * This class assumes that you are connected to the model database, which has master data in it and also you have some
 * default objects in it, for this you can run the master data script and then the test scripts, this script has
 * statements for creating some default objects.
 * <p/>
 * The convention followed here is that any method that starts with "get" is returning an object already existing in the
 * database, this object is not meant to be modified and the method that starts with "create" creates a new object
 * inserts it into the database and returns that hence these objects are meant to be cleaned up by the user.
 */
public class TestObjectFactory {

    private static TestObjectPersistence testObjectPersistence = new TestObjectPersistence();

    /**
     * Constants to make calls to {@link #getNewMeeting(RecurrenceType, short, MeetingType, WeekDay)} and
     * {@link #getNewMeetingForToday(RecurrenceType, short, MeetingType)} more readable.
     */
    public static final short EVERY_WEEK = 1;
    public static final short EVERY_MONTH = 1;
    public static final short EVERY_DAY = 1;
    public static final short EVERY_SECOND_WEEK = 2;
    public static final short EVERY_SECOND_MONTH = 2;

    /**
     * We supply this for the salutation in a lot of test data, but I'm not sure a value of 1 really has a well-defined
     * meaning given our master data.
     */
    public static final int SAMPLE_SALUTATION = 1;

    private static final short SAMPLE_CATEGORY = 2;

    /**
     * Corresponds to a locale we set up in latest-data.
     */
    public static final Short TEST_LOCALE = 1;

    /**
     * Set up in latest-data.
     */
    public static final Short HEAD_OFFICE = 1;

    /**
     * Set up in testdbinsertionscript.
     */
    public static final Short SAMPLE_AREA_OFFICE = 2;

    /**
     * Set up in testdbinsertionscript.
     */
    public static final Short SAMPLE_BRANCH_OFFICE = 3;

    /**
     * @return - Returns the office created by test data scripts. If the row does not already exist in the database it
     *         returns null. defaults created are 1- Head Office , 2 - Area Office , 3 - BranchOffice.
     */
    public static OfficeBO getOffice(final Short officeId) {
        return (OfficeBO) addObject(testObjectPersistence.getOffice(officeId));
    }

    public static void removeObject(AbstractEntity obj) {
        if (obj != null) {
            testObjectPersistence.removeObject(obj);
            obj = null;
        }
    }

    /**
     * @return - Returns the personnel created by master data scripts. This record does not have any custom fields or
     *         roles associated with it. If the row does not already exist in the database it returns null.
     */

    public static PersonnelBO getPersonnel(final Session session, final Short personnelId) {
        return (PersonnelBO) addObject(testObjectPersistence.getPersonnel(session, personnelId));
    }

    public static PersonnelBO getPersonnel(final Short personnelId) {
        return getPersonnel(StaticHibernateUtil.getSessionTL(), personnelId);
    }

    /*
     * Create a center which includes a weekly maintenance fee of 100
     */

    public static CenterBO createWeeklyFeeCenter(final String customerName, final MeetingBO meeting) {
        return createCenter(customerName, meeting, getFees());
    }

    public static CenterBO createWeeklyFeeCenterForTestGetLoanAccounts(final String customerName,
                                                                       final MeetingBO meeting) {
        return createCenterForTestGetLoanAccounts(customerName, meeting, getFees());
    }

    public static CenterBO createWeeklyFeeCenter(final String customerName, final MeetingBO meeting,
                                                 final Short officeId, final Short personnelId) {
        return createCenter(customerName, meeting, officeId, personnelId, getFees());
    }

    public static CenterBO createCenter(final String customerName, final MeetingBO meeting, final List<FeeDto> fees) {
        return createCenter(customerName, meeting, SAMPLE_BRANCH_OFFICE, PersonnelConstants.SYSTEM_USER, fees);
    }

    public static CenterBO createCenterForTestGetLoanAccounts(final String customerName, final MeetingBO meeting,
                                                              final List<FeeDto> fees) {
        return createCenter(customerName, meeting, SAMPLE_BRANCH_OFFICE, PersonnelConstants.TEST_USER, fees);
    }

    public static CenterBO createCenter(final String customerName, final MeetingBO meeting, final Short officeId,
                                        final Short personnelId, final List<FeeDto> fees) {
        CenterBO center;
        try {
            center = new CenterBO(TestUtils.makeUserWithLocales(), customerName, null, null, fees, null, null,
                    new OfficePersistence().getOffice(officeId), meeting, new PersonnelPersistence()
                            .getPersonnel(personnelId), new CustomerPersistence());
            new CenterPersistence().saveCenter(center);
            StaticHibernateUtil.commitTransaction();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        addObject(center);
        return center;
    }

    public static ProductMixBO createNotAllowedProductForAProductOffering(final PrdOfferingBO prdOffering,
                                                                          final PrdOfferingBO prdOfferingNotAllowedId) {
        ProductMixBO prdmix;
        try {
            prdmix = new ProductMixBO(prdOffering, prdOfferingNotAllowedId);
            prdmix.save();
            StaticHibernateUtil.commitTransaction();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        addObject(prdmix);
        return prdmix;
    }

    public static List<FeeDto> getFees() {
        List<FeeDto> fees = new ArrayList<FeeDto>();
        AmountFeeBO maintenanceFee = (AmountFeeBO) createPeriodicAmountFee("Maintenance Fee", FeeCategory.ALLCUSTOMERS,
                "100", RecurrenceType.WEEKLY, Short.valueOf("1"));
        FeeDto fee = new FeeDto(getContext(), maintenanceFee);
        fees.add(fee);
        return fees;
    }

    public static List<FeeDto> getFeesWithMakeUser() {
        List<FeeDto> fees = new ArrayList<FeeDto>();
        AmountFeeBO maintenanceFee = (AmountFeeBO) createPeriodicAmountFeeWithMakeUser("Maintenance Fee",
                FeeCategory.ALLCUSTOMERS, "100", RecurrenceType.WEEKLY, Short.valueOf("1"));
        FeeDto fee = new FeeDto(getContext(), maintenanceFee);
        fees.add(fee);
        return fees;
    }

    public static List<CustomFieldDto> getCustomFields() {
        List<CustomFieldDto> customFields = new ArrayList<CustomFieldDto>();
        CustomFieldDto fee = new CustomFieldDto(Short.valueOf("4"), "Custom", CustomFieldType.NUMERIC.getValue());
        customFields.add(fee);
        return customFields;
    }

    /**
     * This is just a helper method which returns a address object , this is just a helper it does not persist any data.
     */
    public static Address getAddressHelper() {
        Address address = new Address();
        address.setLine1("line1");
        address.setCity("city");
        address.setCountry("country");
        return address;
    }

    public static GroupBO createWeeklyFeeGroupUnderCenter(final String customerName,
                                                          final CustomerStatus customerStatus, final CustomerBO parentCustomer) {
        Short formedBy = PersonnelConstants.SYSTEM_USER;
        return createGroupUnderCenter(customerName, customerStatus, null, false, null, null, getCustomFields(),
                getFees(), formedBy, parentCustomer);
    }

    public static GroupBO createNoFeeGroupUnderCenter(final String customerName, final CustomerStatus customerStatus,
                                                      final CustomerBO parentCustomer) {
        Short formedBy = PersonnelConstants.SYSTEM_USER;
        return createGroupUnderCenter(customerName, customerStatus, null, false, null, null, getCustomFields(), null,
                formedBy, parentCustomer);
    }

    public static GroupBO createWeeklyFeeGroupUnderCenterForTestGetLoanAccountsInActiveBadStanding(
            final String customerName, final CustomerStatus customerStatus, final CustomerBO parentCustomer) {
        Short formedBy = PersonnelConstants.TEST_USER;
        return createGroupUnderCenter(customerName, customerStatus, null, false, null, null, getCustomFields(),
                getFees(), formedBy, parentCustomer);
    }

    public static GroupBO createGroupUnderCenter(final String customerName, final CustomerStatus customerStatus,
                                                 final String externalId, final boolean trained, final Date trainedDate, final Address address,
                                                 final List<CustomFieldDto> customFields, final List<FeeDto> fees, final Short formedById,
                                                 final CustomerBO parentCustomer) {
        GroupBO group;
        try {
            group = new GroupBO(TestUtils.makeUserWithLocales(), customerName, customerStatus, externalId, trained,
                    trainedDate, address, customFields, fees, new PersonnelPersistence().getPersonnel(formedById),
                    parentCustomer);
            new GroupPersistence().saveGroup(group);
            StaticHibernateUtil.commitTransaction();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        addObject(group);
        return group;
    }

    public static GroupBO createGroupUnderBranch(final String customerName, final CustomerStatus customerStatus,
                                                 final Short officeId, final MeetingBO meeting, final Short loanOfficerId) {
        Short formedBy = PersonnelConstants.SYSTEM_USER;
        return createGroupUnderBranch(customerName, customerStatus, null, false, null, null, null, getFees(), formedBy,
                officeId, meeting, loanOfficerId);
    }

    public static GroupBO createGroupUnderBranchWithMakeUser(final String customerName,
                                                             final CustomerStatus customerStatus, final Short officeId, final MeetingBO meeting,
                                                             final Short loanOfficerId) {
        Short formedBy = PersonnelConstants.SYSTEM_USER;
        return createGroupUnderBranch(customerName, customerStatus, null, false, null, null, null,
                getFeesWithMakeUser(), formedBy, officeId, meeting, loanOfficerId);
    }

    public static GroupBO createGroupUnderBranch(final String customerName, final CustomerStatus customerStatus,
                                                 final String externalId, final boolean trained, final Date trainedDate, final Address address,
                                                 final List<CustomFieldDto> customFields, final List<FeeDto> fees, final Short formedById,
                                                 final Short officeId, final MeetingBO meeting, final Short loanOfficerId) {
        GroupBO group;
        PersonnelBO loanOfficer = null;
        try {
            if (loanOfficerId != null) {
                loanOfficer = new PersonnelPersistence().getPersonnel(loanOfficerId);
            }
            group = new GroupBO(TestUtils.makeUserWithLocales(), customerName, customerStatus, externalId, trained,
                    trainedDate, address, customFields, fees, new PersonnelPersistence().getPersonnel(formedById),
                    new OfficePersistence().getOffice(officeId), meeting, loanOfficer);
            new GroupPersistence().saveGroup(group);
            StaticHibernateUtil.commitTransaction();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        addObject(group);
        return group;
    }

    public static ClientBO createClient(final String customerName, final CustomerStatus status,
                                        final CustomerBO parentCustomer) {
        return createClient(customerName, status, parentCustomer, getFees(), (String) null, new Date(1222333444000L));
    }

    public static ClientBO createClient(final String customerName, final CustomerStatus status,
                                        final CustomerBO parentCustomer, final List<FeeDto> fees, final String governmentId, final Date dateOfBirth) {

        ClientPersonalDetailDto clientPersonalDetailDto = new ClientPersonalDetailDto(1, 1, 1, 1, 1, 1, Short
                .valueOf("1"), Short.valueOf("1"), Short.valueOf("41"));
        ClientNameDetailDto clientNameDetailDto = clientNameView(NameType.CLIENT, customerName);
        ClientNameDetailDto spouseNameDetailView = clientNameView(NameType.SPOUSE, customerName);

        ClientBO client;
        try {
            client = new ClientBO(TestUtils.makeUserWithLocales(), customerName, status, null, null, null, null, fees,
                    null, new PersonnelPersistence().getPersonnel(PersonnelConstants.SYSTEM_USER),
                    new OfficePersistence().getOffice(SAMPLE_BRANCH_OFFICE), parentCustomer, dateOfBirth, governmentId,
                    null, null, YesNoFlag.YES.getValue(), clientNameDetailDto, spouseNameDetailView,
                    clientPersonalDetailDto, null);
            new ClientPersistence().saveClient(client);
        } catch (CustomerException e) {
            throw new RuntimeException(e);
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        }
        StaticHibernateUtil.commitTransaction();
        addObject(client);
        return client;
    }

    public static ClientNameDetailDto clientNameView(final NameType nameType, final String customerName) {
        return new ClientNameDetailDto(nameType, SAMPLE_SALUTATION, customerName, "middle", customerName, "secondLast");
    }

    public static ClientBO createClient(final String customerName, final MeetingBO meeting, final CustomerStatus status) {
        ClientBO client;

        try {
            PersonnelBO systemUser = new PersonnelPersistence().getPersonnel(PersonnelConstants.SYSTEM_USER);
            ClientNameDetailDto clientNameDetailDto = new ClientNameDetailDto(NameType.CLIENT, SAMPLE_SALUTATION,
                    customerName, "middle", customerName, "secondLast");
            ClientNameDetailDto spouseNameDetailView = new ClientNameDetailDto(NameType.SPOUSE, SAMPLE_SALUTATION,
                    customerName, "middle", customerName, "secondLast");
            ClientPersonalDetailDto clientPersonalDetailDto = new ClientPersonalDetailDto(1, 1, 1, 1, 1, 1, Short
                    .valueOf("1"), Short.valueOf("1"), Short.valueOf("41"));
            client = new ClientBO(TestUtils.makeUserWithLocales(), clientNameDetailDto.getDisplayName(), status, null,
                    null, null, null, getFees(), null, systemUser, new OfficePersistence()
                            .getOffice(SAMPLE_BRANCH_OFFICE), meeting, systemUser, new DateTimeService()
                            .getCurrentJavaDateTime(), null, null, null, YesNoFlag.NO.getValue(), clientNameDetailDto,
                    spouseNameDetailView, clientPersonalDetailDto, null);
            new ClientPersistence().saveClient(client);
            StaticHibernateUtil.commitTransaction();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        addObject(client);
        return client;
    }

    public static ClientBO createClient(final String customerName, final CustomerStatus status,
                                        final CustomerBO parentCustomer, final Date startDate) {
        ClientBO client;
        Short personnel = PersonnelConstants.SYSTEM_USER;
        try {
            ClientNameDetailDto clientNameDetailDto = new ClientNameDetailDto(NameType.MAYBE_CLIENT, SAMPLE_SALUTATION,
                    customerName, "", customerName, "");
            ClientNameDetailDto spouseNameDetailView = new ClientNameDetailDto(NameType.SPOUSE, SAMPLE_SALUTATION,
                    customerName, "middle", customerName, "secondLast");
            ClientPersonalDetailDto clientPersonalDetailDto = new ClientPersonalDetailDto(1, 1, 1, 1, 1, 1, Short
                    .valueOf("1"), Short.valueOf("1"), Short.valueOf("41"));

            // Add a way to create parentless clients; like clients outside
            // groups
            if (null == parentCustomer) {
                client = new ClientBO(TestUtils.makeUserWithLocales(), // UserContext
                        clientNameDetailDto.getDisplayName(), // String
                        // displayName
                        status, // CustomerStatus
                        null, // String externalId
                        null, // Date mfiJoiningDate
                        null, // Address
                        null, // List<CustomFieldDto> customFields
                        getFees(), // List<FeeDto> fees
                        null, // List<SavingsOfferingBO> offeringsSelected
                        new PersonnelPersistence().getPersonnel(personnel), // Short
                        // formedById
                        new OfficePersistence().getOffice(SAMPLE_BRANCH_OFFICE), // Short
                        // officeId
                        null, // MeetingBO
                        null, // Short loanOfficerId
                        null, // Date dateOfBirth
                        "", // String governmentId
                        null, // Short trained
                        null, // Date trainedDate
                        YesNoFlag.NO.getValue(), // Short groupFlag
                        clientNameDetailDto, // ClientNameDetailDto
                        spouseNameDetailView, // ClientNameDetailDto
                        clientPersonalDetailDto, // ClientPersonalDetailDto
                        null); // InputStream picture
            } else {
                client = new ClientBO(TestUtils.makeUserWithLocales(), clientNameDetailDto.getDisplayName(), status,
                        null, null, null, null, getFees(), null, new PersonnelPersistence().getPersonnel(personnel),
                        parentCustomer.getOffice(), parentCustomer, null, null, null, null, YesNoFlag.YES.getValue(),
                        clientNameDetailDto, spouseNameDetailView, clientPersonalDetailDto, null);
            }

            new ClientPersistence().saveClient(client);
            StaticHibernateUtil.commitTransaction();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        addObject(client);
        return client;
    }

    public static LoanOfferingBO createLoanOffering(final String name, final ApplicableTo applicableTo,
                                                    final Date startDate, final PrdStatus offeringStatus, final Double defLnAmnt, final Double defIntRate,
                                                    final int defInstallments, final InterestType interestType, final boolean intDedAtDisb,
                                                    final boolean princDueLastInst, final MeetingBO meeting) {
        return createLoanOffering(name, name.substring(0, 1), applicableTo, startDate, offeringStatus, defLnAmnt,
                defIntRate, defInstallments, interestType, intDedAtDisb, princDueLastInst, meeting);
    }

    public static LoanOfferingBO createLoanOffering(final String name, final ApplicableTo applicableTo,
                                                    final Date startDate, final PrdStatus offeringStatus, final Double defLnAmnt, final Double defIntRate,
                                                    final int defInstallments, final InterestType interestType, final MeetingBO meeting) {
        return createLoanOffering(name, name.substring(0, 1), applicableTo, startDate, offeringStatus, defLnAmnt,
                defIntRate, defInstallments, interestType, false, false, meeting);
    }

    public static LoanOfferingBO createLoanOffering(final String name, final String shortName,
                                                    final ApplicableTo applicableTo, final Date startDate, final PrdStatus offeringStatus,
                                                    final Double defLnAmnt, final Double defIntRate, final int defInstallments,
                                                    final InterestType interestType, final boolean intDedAtDisb, final boolean princDueLastInst,
                                                    final MeetingBO meeting) {
        return createLoanOffering(name, shortName, applicableTo, startDate, offeringStatus, defLnAmnt, defIntRate,
                (short) defInstallments, interestType, intDedAtDisb, princDueLastInst, meeting,
                GraceType.GRACEONALLREPAYMENTS, (short) 8, "1", "1");
    }

    public static LoanOfferingBO createLoanOffering(final String name, final String shortName,
                                                    final ApplicableTo applicableTo, final Date startDate, final PrdStatus offeringStatus,
                                                    final Double defLnAmnt, final Double defIntRate, final int defInstallments,
                                                    final InterestType interestType, final MeetingBO meeting) {
        return createLoanOffering(name, shortName, applicableTo, startDate, offeringStatus, defLnAmnt, defIntRate,
                (short) defInstallments, interestType, false, false, meeting, GraceType.GRACEONALLREPAYMENTS,
                (short) 1, "1", "1");
    }

    public static LoanOfferingBO createLoanOffering(final String name, final String shortName,
                                                    final ApplicableTo applicableTo, final Date startDate, final PrdStatus offeringStatus,
                                                    final Double defLnAmnt, final Double defIntRate, final int defInstallments,
                                                    final InterestType interestType, final MeetingBO meeting, final String loanAmtCalcType,
                                                    final String calcInstallmentType, final MifosCurrency currency) {
        return createLoanOffering(name, shortName, applicableTo, startDate, offeringStatus, defLnAmnt, defIntRate,
                (short) defInstallments, interestType, false, false, meeting, GraceType.GRACEONALLREPAYMENTS,
                (short) 8, loanAmtCalcType, calcInstallmentType, currency);
    }

    public static LoanOfferingBO createLoanOffering(final Date currentTime, final MeetingBO meeting) {
        return TestObjectFactory.createLoanOffering("Loan", "L", currentTime, meeting);
    }

    public static LoanOfferingBO createLoanOffering(final String name, final String shortName, final Date currentTime,
                                                    final MeetingBO meeting) {
        return TestObjectFactory.createLoanOffering(name, shortName, ApplicableTo.GROUPS, currentTime,
                PrdStatus.LOAN_ACTIVE, 300.0, 1.2, 3, InterestType.FLAT, meeting, "1", "1", TestUtils.RUPEE);
    }

    public static LoanOfferingBO createLoanOffering(final String name, final String shortName, final Date currentTime,
                                                    final MeetingBO meeting, final MifosCurrency currency) {
        return TestObjectFactory.createLoanOffering(name, shortName, ApplicableTo.GROUPS, currentTime,
                PrdStatus.LOAN_ACTIVE, 300.0, 1.2, 3, InterestType.FLAT, meeting, "1", "1", currency);
    }

    /**
     * @param defLnAmnt       - Loan Amount same would be set as min and max amounts
     * @param defIntRate      - Interest Rate same would be set as min and max amounts
     * @param defInstallments Number of installments set as min and max amounts
     */
    public static LoanOfferingBO createLoanOffering(final String name, final String shortName,
                                                    final ApplicableTo applicableTo, final Date startDate, final PrdStatus offeringStatus,
                                                    final Double defLnAmnt, final Double defIntRate, final Short defInstallments,
                                                    final InterestType interestType, final boolean interestDeductedAtDisbursement,
                                                    final boolean principalDueInLastInstallment, final MeetingBO meeting, final GraceType graceType) {
        PrdApplicableMasterEntity prdApplicableMaster = new PrdApplicableMasterEntity(applicableTo);
        ProductCategoryBO productCategory = TestObjectFactory.getLoanPrdCategory();
        GracePeriodTypeEntity gracePeriodType = new GracePeriodTypeEntity(graceType);
        InterestTypesEntity interestTypes = new InterestTypesEntity(interestType);
        GLCodeEntity glCodePrincipal = (GLCodeEntity) StaticHibernateUtil.getSessionTL().get(GLCodeEntity.class,
                Short.valueOf("11"));

        GLCodeEntity glCodeInterest = (GLCodeEntity) StaticHibernateUtil.getSessionTL().get(GLCodeEntity.class,
                Short.valueOf("21"));
        LoanOfferingBO loanOffering;
        short gracePeriodDuration = (short) 0;

            boolean loanCounter = true;
            boolean waiverInterest = true;
        try {
            loanOffering = new LoanOfferingBO(getContext(), name, shortName, productCategory, prdApplicableMaster,
                    startDate, null, null, gracePeriodType, gracePeriodDuration, interestTypes, TestUtils
                            .createMoney(defLnAmnt), TestUtils.createMoney(defLnAmnt),
                    TestUtils.createMoney(defLnAmnt), defIntRate, defIntRate, defIntRate, defInstallments,
                    defInstallments, defInstallments, loanCounter, interestDeductedAtDisbursement,
                    principalDueInLastInstallment, new ArrayList<FundBO>(), new ArrayList<FeeBO>(), meeting,
                    glCodePrincipal, glCodeInterest, waiverInterest);
        } catch (ProductDefinitionException e) {
            throw new RuntimeException(e);
        }

        PrdStatusEntity prdStatus = testObjectPersistence.retrievePrdStatus(offeringStatus);
        LoanOfferingTestUtils.setStatus(loanOffering, prdStatus);
        LoanOfferingTestUtils.setGracePeriodType(loanOffering, gracePeriodType, gracePeriodDuration);
        return (LoanOfferingBO) addObject(testObjectPersistence.persist(loanOffering));
    }

    public static LoanOfferingBO createLoanOffering(final String name, final String shortName,
                                                    final ApplicableTo applicableTo, final Date startDate, final PrdStatus offeringStatus,
                                                    final Double defLnAmnt, final Double defIntRate, final Short defInstallments,
                                                    final InterestType interestType, final boolean interestDeductedAtDisbursement,
                                                    final boolean principalDueInLastInstallment, final MeetingBO meeting, final GraceType graceType,
                                                    final short gracePeriodDuration, final String loanAmountCalcType, final String noOfInstallCalcType) {
        return createLoanOffering(name, shortName, applicableTo, startDate, offeringStatus, defLnAmnt, defIntRate,
                defInstallments, interestType, interestDeductedAtDisbursement, principalDueInLastInstallment, meeting,
                graceType, gracePeriodDuration, loanAmountCalcType, noOfInstallCalcType, TestUtils.RUPEE);
    }

    public static LoanOfferingBO createLoanOffering(final String name, final String shortName,
                                                    final ApplicableTo applicableTo, final Date startDate, final PrdStatus offeringStatus,
                                                    final Double defLnAmnt, final Double defIntRate, final Short defInstallments,
                                                    final InterestType interestType, final boolean interestDeductedAtDisbursement,
                                                    final boolean principalDueInLastInstallment, final MeetingBO meeting, final GraceType graceType,
                                                    final short gracePeriodDuration, final String loanAmountCalcType, final String noOfInstallCalcType,
                                                    final MifosCurrency currency) {
        PrdApplicableMasterEntity prdApplicableMaster = new PrdApplicableMasterEntity(applicableTo);
        ProductCategoryBO productCategory = TestObjectFactory.getLoanPrdCategory();
        GracePeriodTypeEntity gracePeriodType = new GracePeriodTypeEntity(graceType);
        InterestTypesEntity interestTypes = new InterestTypesEntity(interestType);
        GLCodeEntity glCodePrincipal = (GLCodeEntity) StaticHibernateUtil.getSessionTL().get(GLCodeEntity.class,
                TestGeneralLedgerCode.LOANS_TO_CLIENTS);

        GLCodeEntity glCodeInterest = (GLCodeEntity) StaticHibernateUtil.getSessionTL().get(GLCodeEntity.class,
                TestGeneralLedgerCode.INTEREST_ON_LOANS);
        LoanOfferingBO loanOffering;

        boolean loanCounter = true;
        boolean waiverInterest = true;
        try {
            loanOffering = new LoanOfferingBO(getContext(), name, shortName, productCategory, prdApplicableMaster,
                    startDate, null, null, gracePeriodType, gracePeriodDuration, interestTypes, new Money(currency, defLnAmnt.toString()),
                    new Money(currency, defLnAmnt.toString()), new Money(currency, defLnAmnt.toString()),
                    defIntRate, defIntRate, defIntRate, defInstallments,
                    defInstallments, defInstallments, loanCounter, interestDeductedAtDisbursement,
                    principalDueInLastInstallment, new ArrayList<FundBO>(), new ArrayList<FeeBO>(), meeting,
                    glCodePrincipal, glCodeInterest, loanAmountCalcType, noOfInstallCalcType, waiverInterest);
        } catch (ProductDefinitionException e) {
            throw new RuntimeException(e);
        }

        loanOffering.setCurrency(currency);
        PrdStatusEntity prdStatus = testObjectPersistence.retrievePrdStatus(offeringStatus);
        LoanOfferingTestUtils.setStatus(loanOffering, prdStatus);
        LoanOfferingTestUtils.setGracePeriodType(loanOffering, gracePeriodType, gracePeriodDuration);
        return (LoanOfferingBO) addObject(testObjectPersistence.persist(loanOffering));
    }

    public static LoanOfferingBO createLoanOfferingFromLastLoan(final String name, final String shortName,
                                                                final ApplicableTo applicableTo, final Date startDate, final PrdStatus offeringStatus,
                                                                final Double defIntRate, final InterestType interestType, final boolean interestDeductedAtDisbursement,
                                                                final boolean principalDueInLastInstallment, final MeetingBO meeting, final GraceType graceType,
                                                                final LoanPrdActionForm loanPrdActionForm) {
        PrdApplicableMasterEntity prdApplicableMaster = new PrdApplicableMasterEntity(applicableTo);
        ProductCategoryBO productCategory = TestObjectFactory.getLoanPrdCategory();
        GracePeriodTypeEntity gracePeriodType = new GracePeriodTypeEntity(graceType);
        InterestTypesEntity interestTypes = new InterestTypesEntity(interestType);
        GLCodeEntity glCodePrincipal = (GLCodeEntity) StaticHibernateUtil.getSessionTL().get(GLCodeEntity.class,
                Short.valueOf("11"));

        GLCodeEntity glCodeInterest = (GLCodeEntity) StaticHibernateUtil.getSessionTL().get(GLCodeEntity.class,
                Short.valueOf("21"));
        short gracePeriodDuration = (short) 2;
        LoanOfferingBO loanOffering;

            boolean loanCounter = true;
            boolean waiverInterest = true;
        try {
            loanOffering = new LoanOfferingBO(getContext(), name, shortName, productCategory, prdApplicableMaster,
                    startDate, null, null, gracePeriodType, gracePeriodDuration, interestTypes, defIntRate, defIntRate,
                    defIntRate, loanCounter, interestDeductedAtDisbursement, principalDueInLastInstallment,
                    new ArrayList<FundBO>(), new ArrayList<FeeBO>(), meeting, glCodePrincipal, glCodeInterest,
                    loanPrdActionForm, waiverInterest);
        } catch (ProductDefinitionException e) {
            throw new RuntimeException(e);
        }

        PrdStatusEntity prdStatus = testObjectPersistence.retrievePrdStatus(offeringStatus);
        LoanOfferingTestUtils.setStatus(loanOffering, prdStatus);
        LoanOfferingTestUtils.setGracePeriodType(loanOffering, gracePeriodType, gracePeriodDuration);
        return (LoanOfferingBO) addObject(testObjectPersistence.persist(loanOffering));
    }

    public static LoanOfferingBO createCompleteLoanOfferingObject() throws Exception {
        PrdApplicableMasterEntity prdApplicableMaster = new PrdApplicableMasterEntity(ApplicableTo.GROUPS);
        MeetingBO frequency = TestObjectFactory.createMeeting(getNewMeetingForToday(WEEKLY, EVERY_WEEK,
                CUSTOMER_MEETING));
        GLCodeEntity principalglCodeEntity = (GLCodeEntity) StaticHibernateUtil.getSessionTL().get(GLCodeEntity.class,
                TestGeneralLedgerCode.BANK_ACCOUNT_ONE);
        GLCodeEntity intglCodeEntity = (GLCodeEntity) StaticHibernateUtil.getSessionTL().get(GLCodeEntity.class,
                TestGeneralLedgerCode.BANK_ACCOUNT_ONE);
        ProductCategoryBO productCategory = getLoanPrdCategory();
        InterestTypesEntity interestTypes = new InterestTypesEntity(InterestType.FLAT);
        GracePeriodTypeEntity gracePeriodType = new GracePeriodTypeEntity(GraceType.GRACEONALLREPAYMENTS);
        List<FeeBO> fees = new ArrayList<FeeBO>();
        List<FundBO> funds = new ArrayList<FundBO>();
        FundBO fundBO = (FundBO) StaticHibernateUtil.getSessionTL().get(FundBO.class, Short.valueOf("2"));
        funds.add(fundBO);
        LoanOfferingBO loanOfferingBO = new LoanOfferingBO(getContext(), "Loan Offering", "LOAP", productCategory,
                prdApplicableMaster, DateUtils.getCurrentDateWithoutTimeStamp(), null, null, gracePeriodType,
                (short) 2, interestTypes, TestUtils.createMoney("1000"), TestUtils.createMoney("3000"), TestUtils
                        .createMoney("2000.0"), 12.0, 2.0, 3.0, (short) 20, (short) 11, (short) 17, false, false,
                false, funds, fees, frequency, principalglCodeEntity, intglCodeEntity, true);
        loanOfferingBO.save();
        return loanOfferingBO;
    }

    public static ProductCategoryBO getLoanPrdCategory() {
        return (ProductCategoryBO) addObject(testObjectPersistence.getLoanPrdCategory());
    }

    public static LoanBO createLoanAccount(final String globalNum, final CustomerBO customer, final AccountState state,
                                           final Date startDate, final LoanOfferingBO offering) {
        LoanBO loan = LoanBOTestUtils.createLoanAccount(globalNum, customer, state, startDate, offering);
        try {
            loan.save();
        } catch (AccountException e) {
            throw new RuntimeException(e);
        }
        StaticHibernateUtil.commitTransaction();
        return (LoanBO) addObject(getObject(LoanBO.class, loan.getAccountId()));
    }

    public static LoanBO createBasicLoanAccount(final CustomerBO customer, final AccountState state,
                                                final Date startDate, final LoanOfferingBO offering) {
        LoanBO loan = LoanBOTestUtils.createBasicLoanAccount(customer, state, startDate, offering);
        try {
            loan.save();
        } catch (AccountException e) {
            throw new RuntimeException(e);
        }
        StaticHibernateUtil.commitTransaction();
        return (LoanBO) addObject(getObject(LoanBO.class, loan.getAccountId()));
    }

    public static LoanBO createIndividualLoanAccount(final String globalNum, final CustomerBO customer,
                                                     final AccountState state, final Date startDate, final LoanOfferingBO offering) {
        LoanBO loan = LoanBOTestUtils.createIndividualLoanAccount(globalNum, customer, state, startDate, offering);
        try {
            loan.save();
        } catch (AccountException e) {
            throw new RuntimeException(e);
        }
        StaticHibernateUtil.commitTransaction();
        return (LoanBO) addObject(getObject(LoanBO.class, loan.getAccountId()));
    }

    public static SavingsOfferingBO createSavingsProduct(final String name, final ApplicableTo applicableTo,
                                                         final Date startDate, final PrdStatus status, final Double recommendedAmount,
                                                         final RecommendedAmountUnit unit, final Double intRate, final Double maxAmtWithdrawl,
                                                         final Double minAmtForInt, final SavingsType savingsType, final InterestCalcType interestCalculation,
                                                         final MeetingBO intCalcMeeting, final MeetingBO intPostMeeting) {
        return createSavingsProduct(name, name.substring(0, 1), applicableTo, startDate, status, recommendedAmount,
                unit, intRate, maxAmtWithdrawl, minAmtForInt, savingsType, interestCalculation, intCalcMeeting,
                intPostMeeting);
    }

    public static SavingsOfferingBO createSavingsProduct(final String name, final String shortName,
                                                         final ApplicableTo applicableTo, final Date startDate, final PrdStatus status,
                                                         final Double recommendedAmount, final RecommendedAmountUnit unit, final Double intRate,
                                                         final Double maxAmtWithdrawl, final Double minAmtForInt, final SavingsType savingsType,
                                                         final InterestCalcType interestCalculation, final MeetingBO intCalcMeeting, final MeetingBO intPostMeeting) {
        return createSavingsProduct(name, shortName, applicableTo, startDate, status, recommendedAmount, unit, intRate,
                maxAmtWithdrawl, minAmtForInt, savingsType, interestCalculation, intCalcMeeting, intPostMeeting,
                TestGeneralLedgerCode.BANK_ACCOUNT_ONE, TestGeneralLedgerCode.BANK_ACCOUNT_ONE);
    }

    public static SavingsOfferingBO createSavingsProduct(final String name, final String shortName,
                                                         final ApplicableTo applicableTo, final Date startDate, final PrdStatus status,
                                                         final Double recommendedAmount, final RecommendedAmountUnit recommendedAmountUnit, final Double intRate,
                                                         final Double maxAmtWithdrawl, final Double minAmtForInt, final SavingsType savingsType,
                                                         final InterestCalcType interestCalculationType, final MeetingBO intCalcMeeting,
                                                         final MeetingBO intPostMeeting, final Short depGLCode, final Short withGLCode) {
        GLCodeEntity depglCodeEntity = (GLCodeEntity) StaticHibernateUtil.getSessionTL().get(GLCodeEntity.class,
                depGLCode);
        GLCodeEntity intglCodeEntity = (GLCodeEntity) StaticHibernateUtil.getSessionTL().get(GLCodeEntity.class,
                withGLCode);
        ProductCategoryBO productCategory = (ProductCategoryBO) TestObjectFactory.getObject(ProductCategoryBO.class,
                SAMPLE_CATEGORY);

        PrdApplicableMasterEntity prdApplicableMaster = new PrdApplicableMasterEntity(applicableTo);
        SavingsTypeEntity savingsTypeEntity = new SavingsTypeEntity(savingsType);
        InterestCalcTypeEntity intCalType = new InterestCalcTypeEntity(interestCalculationType);
        RecommendedAmntUnitEntity amountUnit = new RecommendedAmntUnitEntity(recommendedAmountUnit);
        SavingsOfferingBO product;
        try {
            product = new SavingsOfferingBO(TestUtils.makeUserWithLocales(), name, shortName, productCategory,
                    prdApplicableMaster, startDate, null, null, amountUnit, savingsTypeEntity, intCalType,
                    intCalcMeeting, intPostMeeting, TestUtils.createMoney(recommendedAmount), TestUtils
                            .createMoney(maxAmtWithdrawl), TestUtils.createMoney(minAmtForInt), intRate,
                    depglCodeEntity, intglCodeEntity);
        } catch (ProductDefinitionException e) {
            throw new RuntimeException(e);
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }

        PrdStatusEntity prdStatus = testObjectPersistence.retrievePrdStatus(status);
        product.setPrdStatus(prdStatus);
        return (SavingsOfferingBO) addObject(testObjectPersistence.persist(product));
    }

    public static SavingsOfferingBO createSavingsProduct(final String offeringName, final String shortName,
                                                         final SavingsType savingsType, final ApplicableTo applicableTo, final Date currentDate) {
        MeetingBO meetingIntCalc = TestObjectFactory.createMeeting(TestObjectFactory.getTypicalMeeting());
        MeetingBO meetingIntPost = TestObjectFactory.createMeeting(TestObjectFactory.getTypicalMeeting());
        return createSavingsProduct(offeringName, shortName, applicableTo, currentDate, PrdStatus.SAVINGS_ACTIVE,
                300.0, RecommendedAmountUnit.PER_INDIVIDUAL, 24.0, 200.0, 200.0, savingsType,
                InterestCalcType.MINIMUM_BALANCE, meetingIntCalc, meetingIntPost);
    }

    public static SavingsBO createSavingsAccount(final String globalNum, final CustomerBO customer,
                                                 final Short accountStateId, final Date startDate, final SavingsOfferingBO savingsOffering) throws Exception {
        UserContext userContext = TestUtils.makeUserWithLocales();
        MifosCurrency currency = testObjectPersistence.getCurrency();
        MeetingBO meeting = createLoanMeeting(customer.getCustomerMeeting().getMeeting());
        SavingsBO savings = new SavingsBO(userContext, savingsOffering, customer,
                AccountState.SAVINGS_PARTIAL_APPLICATION, new Money(currency, "300.0"), null);
        savings.save();
        savings.setUserContext(TestObjectFactory.getContext());
        savings.changeStatus(accountStateId, null, "");
        SavingBOTestUtils.setActivationDate(savings, new Date(System.currentTimeMillis()));
        List<Date> meetingDates = getMeetingDates(customer.getOfficeId(), meeting, 3);
        short installment = 0;
        for (Date date : meetingDates) {
            SavingsScheduleEntity actionDate = new SavingsScheduleEntity(savings, customer, ++installment,
                    new java.sql.Date(date.getTime()), PaymentStatus.UNPAID, new Money(currency, "200.0"));
            AccountTestUtils.addAccountActionDate(actionDate, savings);
        }
        StaticHibernateUtil.commitTransaction();
        return (SavingsBO) addObject(getObject(SavingsBO.class, savings.getAccountId()));
    }

    private static List<CustomFieldDto> getCustomFieldView() {
        List<CustomFieldDto> customFields = new ArrayList<CustomFieldDto>();
        customFields.add(new CustomFieldDto(new Short("8"), "custom field value", null));
        return customFields;
    }

    /**
     * Also see {@link SavingsTestHelper#createSavingsAccount(SavingsOfferingBO, CustomerBO, AccountState, UserContext)}
     * which is less elaborate.
     */
    public static SavingsBO createSavingsAccount(final String globalNum, final CustomerBO customer,
                                                 final AccountState state, final Date startDate, final SavingsOfferingBO savingsOffering,
                                                 UserContext userContext) throws Exception {
        userContext = TestUtils.makeUserWithLocales();
        SavingsBO savings = new SavingsBO(userContext, savingsOffering, customer, state, savingsOffering
                .getRecommendedAmount(), getCustomFieldView());
        savings.save();
        SavingBOTestUtils.setActivationDate(savings, new DateTimeService().getCurrentJavaDateTime());
        StaticHibernateUtil.commitTransaction();
        return (SavingsBO) addObject(getObject(SavingsBO.class, savings.getAccountId()));
    }

    public static MeetingBO createLoanMeeting(final MeetingBO customerMeeting) {
        MeetingBO meetingToReturn = null;
        try {
            RecurrenceType recurrenceType = RecurrenceType.fromInt(customerMeeting.getMeetingDetails()
                    .getRecurrenceType().getRecurrenceId());
            MeetingType meetingType = MeetingType.fromInt(customerMeeting.getMeetingType().getMeetingTypeId());
            Short recurAfter = customerMeeting.getMeetingDetails().getRecurAfter();

            if (recurrenceType.equals(RecurrenceType.MONTHLY)) {
                if (customerMeeting.isMonthlyOnDate()) {
                    meetingToReturn = new MeetingBO(customerMeeting.getMeetingDetails().getMeetingRecurrence()
                            .getDayNumber(), recurAfter, customerMeeting.getMeetingStartDate(), meetingType,
                            "meetingPlace");
                } else {
                    meetingToReturn = new MeetingBO(customerMeeting.getMeetingDetails().getWeekDay(), customerMeeting
                            .getMeetingDetails().getWeekRank(), recurAfter, customerMeeting.getMeetingStartDate(),
                            meetingType, "meetingPlace");
                }
            } else if (recurrenceType.equals(RecurrenceType.WEEKLY)) {
                meetingToReturn = new MeetingBO(WeekDay.getWeekDay(customerMeeting.getMeetingDetails()
                        .getMeetingRecurrence().getWeekDayValue().getValue()), recurAfter, customerMeeting
                        .getMeetingStartDate(), meetingType, "meetingPlace");
            } else {
                meetingToReturn = new MeetingBO(recurrenceType, recurAfter, customerMeeting.getMeetingStartDate(),
                        meetingType);
            }

            meetingToReturn.setMeetingPlace(customerMeeting.getMeetingPlace());
        } catch (MeetingException e) {
            throw new RuntimeException(e);
        }
        return meetingToReturn;
    }

    public static List<Date> getMeetingDates(short officeId, final MeetingBO meeting, final int occurrences) {
        List<Days> workingDays = new FiscalCalendarRules().getWorkingDaysAsJodaTimeDays();
        List<Holiday> upcomingHolidays = DependencyInjectedServiceLocator.locateHolidayDao()
                .findAllHolidaysThisYearAndNext(officeId);

        ScheduledEvent scheduledEvent = ScheduledEventFactory.createScheduledEventFrom(meeting);
        DateTime meetingStartDate = new DateTime(meeting.getMeetingStartDate());

        ScheduledDateGeneration dateGeneration = new HolidayAndWorkingDaysAndMoratoriaScheduledDateGeneration(
                workingDays, upcomingHolidays);
        List<DateTime> allDates = dateGeneration.generateScheduledDates(occurrences, meetingStartDate, scheduledEvent);

        List<Date> dates = new ArrayList<Date>();
        for (DateTime dateTime : allDates) {
            dates.add(dateTime.toDate());
        }

        return dates;
    }

    public static List<Date> getMeetingDatesThroughTo(short officeId, final MeetingBO meeting, Date endDate) {

        List<Days> workingDays = new FiscalCalendarRules().getWorkingDaysAsJodaTimeDays();
        List<Holiday> upcomingHolidays = DependencyInjectedServiceLocator.locateHolidayDao()
                .findAllHolidaysThisYearAndNext(officeId);

        ScheduledEvent scheduledEvent = ScheduledEventFactory.createScheduledEventFrom(meeting);
        DateTime meetingStartDate = new DateTime(meeting.getMeetingStartDate());
        DateTime endDateTime = new DateTime(endDate);

        ScheduledDateGeneration dateGeneration = new HolidayAndWorkingDaysAndMoratoriaScheduledDateGeneration(
                workingDays, upcomingHolidays);
        List<DateTime> allDates = dateGeneration.generateScheduledDatesThrough(meetingStartDate, endDateTime,
                scheduledEvent);

        List<Date> dates = new ArrayList<Date>();
        for (DateTime dateTime : allDates) {
            dates.add(dateTime.toDate());
        }

        return dates;
    }

    /**
     * createPeriodicAmountFee.
     * <p/>
     * Changing TestObjectFactory#getUserContext() to {@link TestUtils#makeUserWithLocales()} caused a failure in
     * CustomerAccountBOIntegrationTest#testApplyPeriodicFee (and about 163 other tests).
     */
    public static FeeBO createPeriodicAmountFee(final String feeName, final FeeCategory feeCategory,
                                                final String feeAmnt, final RecurrenceType meetingFrequency, final Short recurAfter) {
        return createPeriodicAmountFee(feeName, feeCategory, feeAmnt, meetingFrequency, recurAfter, "Test Category");
    }

    public static FeeBO createPeriodicAmountFee(final String feeName, final FeeCategory feeCategory,
                                                final String feeAmnt, final RecurrenceType meetingFrequency, final Short recurAfter, String categoryTypeName) {
        try {
            FeeBO fee = createPeriodicAmountFee(feeName, feeCategory, feeAmnt, meetingFrequency, recurAfter,
                    TestObjectFactory.getUserContext(), categoryTypeName);
            return fee;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static FeeBO createPeriodicAmountFeeWithMakeUser(final String feeName, final FeeCategory feeCategory,
                                                            final String feeAmnt, final RecurrenceType meetingFrequency, final Short recurAfter) {
        FeeBO fee;
        try {
            fee = createPeriodicAmountFee(feeName, feeCategory, feeAmnt, meetingFrequency, recurAfter, TestUtils
                    .makeUserWithLocales());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return fee;
    }

    public static FeeBO createPeriodicAmountFee(final String feeName, final FeeCategory feeCategory,
                                                final String feeAmnt, final RecurrenceType meetingFrequency, final Short recurAfter,
                                                final UserContext userContext) {
        return createPeriodicAmountFee(feeName, feeCategory, feeAmnt, meetingFrequency, recurAfter, userContext,
                "Test Category");
    }

    private static FeeBO createPeriodicAmountFee(final String feeName, final FeeCategory feeCategory,
                                                 final String feeAmnt, final RecurrenceType meetingFrequency, final Short recurAfter,
                                                 final UserContext userContext, String categoryLookupValue) {
        try {
            GLCodeEntity glCode = ChartOfAccountsCache.get("31301").getAssociatedGlcode();
            MeetingBO meeting = new MeetingBO(meetingFrequency, recurAfter, new DateTimeService()
                    .getCurrentJavaDateTime(), MeetingType.PERIODIC_FEE);
            LookUpValueEntity lookUpValue = new LookUpValueEntity();
            lookUpValue.setLookUpName(categoryLookupValue);
            CategoryTypeEntity categoryType = new CategoryTypeEntity(feeCategory);
            categoryType.setLookUpValue(lookUpValue);
            FeeBO fee = new AmountFeeBO(userContext, feeName, categoryType, new FeeFrequencyTypeEntity(
                    FeeFrequencyType.PERIODIC), glCode, TestUtils.createMoney(feeAmnt), false, meeting);
            return (FeeBO) addObject(testObjectPersistence.createFee(fee));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * *************************
     * Begin creating a periodic rate fee
     * **************************
     */
    public static RateFeeBO createPeriodicRateFee(final String feeName, final FeeCategory feeCategory,
                                                  final Double rate, final FeeFormula feeFormula, final RecurrenceType meetingFrequency,
                                                  final Short recurAfter) {

        try {
            MeetingBO meeting = new MeetingBO(meetingFrequency, recurAfter, new DateTimeService()
                    .getCurrentJavaDateTime(), MeetingType.PERIODIC_FEE);
            return createPeriodicRateFee(feeName, feeCategory, rate, feeFormula, meetingFrequency, recurAfter,
                    TestUtils.makeUserWithLocales(), meeting);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static RateFeeBO createPeriodicRateFee(final String feeName, final FeeCategory feeCategory,
                                                  final Double rate, final FeeFormula feeFormula, final RecurrenceType meetingFrequency,
                                                  final Short recurAfter, final UserContext userContext, final MeetingBO meeting) {

        try {
            MeetingBO newMeeting = new MeetingBO(meetingFrequency, recurAfter, new DateTimeService()
                    .getCurrentJavaDateTime(), MeetingType.PERIODIC_FEE);
            // GLCodeEntity glCode =
            // ChartOfAccountsCache.get("31301").getAssociatedGlcode();
            GLCodeEntity glCode = new GLCodeEntity((short) 1, "31301");
            RateFeeBO fee = new RateFeeBO(userContext, feeName, new CategoryTypeEntity(feeCategory),
                    new FeeFrequencyTypeEntity(FeeFrequencyType.PERIODIC), glCode, rate, new FeeFormulaEntity(
                            feeFormula), false, newMeeting);
            // [keith] I have no idea why the fee must save itself. Otherwise
            // mySQL errors crop up
            // when you try to attach the fee to a loan.
            fee.save();
            // addObject(fee);
            return fee;
            // return (RateFeeBO) addObject(testObjectPersistence.persist(fee));
            // return (RateFeeBO)
            // addObject(testObjectPersistence.createFee(fee));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /****************************
     * End creating a periodic rate fee
     ****************************/

    /**
     * createOneTimeAmountFee.
     * <p/>
     * Changing TestObjectFactory#getUserContext() to {@link TestUtils#makeUserWithLocales()} caused a failure in
     * CustomerAccountBOIntegrationTest#testApplyUpfrontFee (and other tests).
     */
    public static FeeBO createOneTimeAmountFee(final String feeName, final FeeCategory feeCategory,
                                               final String feeAmnt, final FeePayment feePayment) {
        FeeBO fee;
        try {
            fee = createOneTimeAmountFee(feeName, feeCategory, feeAmnt, feePayment, getUserContext(), "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return fee;
    }

    public static FeeBO createOneTimeAmountFee(final String feeName, final FeeCategory feeCategory,
                                               final String feeAmnt, final FeePayment feePayment, final UserContext userContext, String categoryTypeName) {
        GLCodeEntity glCode = (GLCodeEntity) StaticHibernateUtil.getSessionTL().get(GLCodeEntity.class,
                TestGeneralLedgerCode.FEES);
        try {
            CategoryTypeEntity categoryType = new CategoryTypeEntity(feeCategory);
            LookUpValueEntity lookUpValue = new LookUpValueEntity();
            lookUpValue.setLookUpName(categoryTypeName);
            categoryType.setLookUpValue(lookUpValue);
            FeeBO fee = new AmountFeeBO(userContext, feeName, categoryType, new FeeFrequencyTypeEntity(
                    FeeFrequencyType.ONETIME), glCode, TestUtils.createMoney(feeAmnt), false, new FeePaymentEntity(
                    feePayment));
            return (FeeBO) addObject(testObjectPersistence.createFee(fee));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * createOneTimeRateFee.
     * <p/>
     * Changing TestObjectFactory#getUserContext() to {@link TestUtils#makeUserWithLocales()} caused a failure in
     * {@link LoanBOIntegrationTest#testApplyUpfrontFee} (and other tests).
     *
     * @param categoryTypeName TODO
     */
    public static FeeBO createOneTimeRateFee(final String feeName, final FeeCategory feeCategory, final Double rate,
                                             final FeeFormula feeFormula, final FeePayment feePayment, String categoryTypeName) {
        GLCodeEntity glCode = (GLCodeEntity) StaticHibernateUtil.getSessionTL().get(GLCodeEntity.class,
                TestGeneralLedgerCode.FEES);
        FeeBO fee;
        try {
            LookUpValueEntity lookUpValue = new LookUpValueEntity();
            lookUpValue.setLookUpName(categoryTypeName);
            CategoryTypeEntity categoryType = new CategoryTypeEntity(feeCategory);
            categoryType.setLookUpValue(lookUpValue);
            fee = new RateFeeBO(getUserContext(), feeName, categoryType, new FeeFrequencyTypeEntity(
                    FeeFrequencyType.ONETIME), glCode, rate, new FeeFormulaEntity(feeFormula), false,
                    new FeePaymentEntity(feePayment));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return (FeeBO) addObject(testObjectPersistence.createFee(fee));
    }

    /**
     * Return a new meeting object with a meeting occurring on the day of the week that the test is run. Creating a new
     * method to fix issues with meeting creation without breaking existing tests that may depend on it.
     *
     * @param recurAfter 1 means every day/week/month, 2 means every second day/week/month...
     */
    public static MeetingBO getNewWeeklyMeeting(final short recurAfter) {
        Calendar calendar = new GregorianCalendar();
        MeetingBO meeting;
        try {
            meeting = new MeetingBO(WeekDay.getWeekDay((short) calendar.get(Calendar.DAY_OF_WEEK)), recurAfter,
                    new DateTimeService().getCurrentJavaDateTime(), CUSTOMER_MEETING, "meetingPlace");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return meeting;
    }

    /**
     * Return a new meeting object.
     *
     * @param frequency   DAILY, WEEKLY, MONTHLY
     * @param recurAfter  1 means every day/week/month, 2 means every second day/week/month...
     * @param meetingType most commonly a CUSTOMER_MEETING
     * @param weekday     MONDAY, TUESDAY...
     */
    public static MeetingBO getNewMeeting(final RecurrenceType frequency, final short recurAfter,
                                          final MeetingType meetingType, final WeekDay weekday) {
        MeetingBO meeting;
        try {
            meeting = new MeetingBO(frequency, recurAfter, new DateTimeService().getCurrentJavaDateTime(), meetingType);
            meeting.setMeetingPlace("Loan Meeting Place");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        meeting.getMeetingDetails().getMeetingRecurrence().setWeekDay(weekday);
        return meeting;
    }

    /**
     * Return a new meeting object which occurs on today's day of the week.
     * <p/>
     * Not recommended: New tests should call {@link #getNewMeeting(RecurrenceType, short, MeetingType, WeekDay)}
     * instead to avoid bugs where the test will pass on one day but not another.
     *
     * @param frequency   DAILY, WEEKLY, MONTHLY
     * @param recurAfter  1 means every day/week/month, 2 means every second day/week/month...
     * @param meetingType most commonly a CUSTOMER_MEETING
     */
    public static MeetingBO getNewMeetingForToday(final RecurrenceType frequency, final short recurAfter,
                                                  final MeetingType meetingType) {
        LocalDate today = new LocalDate();
        return getNewMeeting(frequency, recurAfter, meetingType, WeekDay.getJodaWeekDay(today.getDayOfWeek()));
    }

    /**
     * Return a new meeting object that represents a weekly customer meeting occurring every Monday. This is the most
     * commonly used meeting type in the unit tests.
     */
    public static MeetingBO getTypicalMeeting() {
        return getNewMeeting(RecurrenceType.WEEKLY, EVERY_WEEK, MeetingType.CUSTOMER_MEETING, WeekDay.MONDAY);
    }

    /**
     * Persist a meeting object.
     */
    public static MeetingBO createMeeting(final MeetingBO meeting) {
        return (MeetingBO) addObject(testObjectPersistence.persist(meeting));
    }

    public static void cleanUp(Holiday holiday) {
        if (null != holiday) {
            deleteHoliday(holiday);
        }
    }

    public static void cleanUp(CustomerBO customer) {
        if (null != customer) {
            deleteCustomer(customer);
            customer = null;
        }
    }

    public static void cleanUp(List<CustomerBO> customerList) {
        if (null != customerList) {
            deleteCustomers(customerList);
            customerList = null;
        }
    }

    public static void cleanUp(FeeBO fee) {
        if (null != fee) {
            deleteFee(fee);
            fee = null;
        }
    }

    public static void cleanUp(AccountBO account) {
        if (null != account) {
            deleteAccount(account, null);
            account = null;
        }
    }

    public static void cleanUpAccount(final Integer accountId) {
        if (null != accountId) {
            Session session = StaticHibernateUtil.getSessionTL();
            Transaction transaction = session.beginTransaction();
            AccountBO account = (AccountBO) session.get(AccountBO.class, accountId);
            deleteAccount(account, session);
            transaction.commit();
            session.close();
        }
    }

    private static void deleteFee(final FeeBO fee) {
        Session session = StaticHibernateUtil.getSessionTL();
        Transaction transaction = StaticHibernateUtil.startTransaction();
        if (fee.isPeriodic()) {
            session.delete(fee.getFeeFrequency().getFeeMeetingFrequency());
        }
        session.delete(fee);
        transaction.commit();
    }

    private static void deleteFees(final List<FeeBO> feeList) {
        Session session = StaticHibernateUtil.getSessionTL();
        for (FeeBO fee : feeList) {
            if (fee.isPeriodic()) {
                session.delete(fee.getFeeFrequency().getFeeMeetingFrequency());
            }
            session.delete(fee);
        }
    }

    private static void deleteAccountPayments(final AccountBO account) {
        Session session = StaticHibernateUtil.getSessionTL();
        for (AccountPaymentEntity accountPayment : account.getAccountPayments()) {
            if (null != accountPayment) {
                deleteAccountPayment(accountPayment, session);
            }
        }
    }

    private static void deleteAccountActionDates(final AccountBO account, final Session session) {
        AccountTypes accountType = account.getType();
        for (AccountActionDateEntity actionDates : account.getAccountActionDates()) {
            // TODO: this will never be true. Do we want to fix it or nuke it?
            if (accountType.getValue().equals(org.mifos.accounts.util.helpers.AccountTypes.LOAN_ACCOUNT)) {
                LoanScheduleEntity loanScheduleEntity = (LoanScheduleEntity) actionDates;
                for (AccountFeesActionDetailEntity actionFees : loanScheduleEntity.getAccountFeesActionDetails()) {
                    session.delete(actionFees);
                }
            }
            // TODO: this will never be true. Do we want to fix it or nuke it?
            if (accountType.getValue().equals(org.mifos.accounts.util.helpers.AccountTypes.CUSTOMER_ACCOUNT)) {
                CustomerScheduleEntity customerScheduleEntity = (CustomerScheduleEntity) actionDates;
                for (AccountFeesActionDetailEntity actionFees : customerScheduleEntity.getAccountFeesActionDetails()) {
                    session.delete(actionFees);
                }
            }
            session.delete(actionDates);
        }
    }

    private static void deleteAccountFees(final AccountBO account) {
        Session session = StaticHibernateUtil.getSessionTL();
        for (AccountFeesEntity accountFees : account.getAccountFees()) {
            session.delete(accountFees);
        }
    }

    private static void deleteSpecificAccount(final AccountBO account, final Session session) {
        if (account instanceof LoanBO) {

            LoanBO loan = (LoanBO) account;
            if (null != loan.getLoanSummary()) {
                session.delete(loan.getLoanSummary());
            }
            session.delete(account);
            loan.getLoanOffering().getLoanOfferingMeeting().setMeeting(null);
            session.delete(loan.getLoanOffering().getLoanOfferingMeeting());
            session.delete(loan.getLoanOffering());

        }
        if (account instanceof SavingsBO) {

            SavingsBO savings = (SavingsBO) account;
            session.delete(account);
            session.delete(savings.getTimePerForInstcalc());
                PrdOfferingMeetingEntity prdOfferingMeeting1 = savings.getSavingsOffering().getTimePerForInstcalc();
                prdOfferingMeeting1.setMeeting(null);
                session.delete(prdOfferingMeeting1);
                PrdOfferingMeetingEntity prdOfferingMeeting2 = savings.getSavingsOffering().getFreqOfPostIntcalc();
                prdOfferingMeeting2.setMeeting(null);
                session.delete(prdOfferingMeeting2);
            session.delete(savings.getSavingsOffering());
        } else {
            session.delete(account);
        }
    }

    private static void deleteAccountWithoutFee(final AccountBO account) {
        Session session = StaticHibernateUtil.getSessionTL();

        deleteAccountPayments(account);
        deleteAccountActionDates(account, session);
        deleteAccountFees(account);
        deleteSpecificAccount(account, session);
    }

    private static void deleteAccount(final AccountBO account, Session session) {
        boolean newSession = false;
        Transaction transaction = null;
        if (null == session) {
            session = StaticHibernateUtil.getSessionTL();
            transaction = StaticHibernateUtil.startTransaction();
            newSession = true;
        }

        List<FeeBO> feeList = new ArrayList<FeeBO>();
        for (AccountFeesEntity accountFees : account.getAccountFees()) {
            if (!feeList.contains(accountFees.getFees())) {
                feeList.add(accountFees.getFees());
            }
        }

        deleteAccountPayments(account);
        deleteAccountActionDates(account, session);
        deleteAccountFees(account);
        deleteSpecificAccount(account, session);
        deleteFees(feeList);

        if (newSession) {
            transaction.commit();
        }
    }

    private static void deleteAccountPayment(final AccountPaymentEntity accountPayment, final Session session) {
        Set<AccountTrxnEntity> loanTrxns = accountPayment.getAccountTrxns();
        for (AccountTrxnEntity accountTrxn : loanTrxns) {
            if (accountTrxn instanceof LoanTrxnDetailEntity) {
                LoanTrxnDetailEntity loanTrxn = (LoanTrxnDetailEntity) accountTrxn;
                for (FeesTrxnDetailEntity feesTrxn : loanTrxn.getFeesTrxnDetails()) {
                    session.delete(feesTrxn);
                }
                for (FinancialTransactionBO financialTrxn : loanTrxn.getFinancialTransactions()) {
                    session.delete(financialTrxn);
                }

                session.delete(loanTrxn);

            }
        }

        session.delete(accountPayment);
    }

    private static void deleteCustomers(final List<CustomerBO> customerList) {
        List<FeeBO> feeList = new ArrayList<FeeBO>();
        for (CustomerBO customer : customerList) {
            Session session = StaticHibernateUtil.getSessionTL();
            session.lock(customer, LockMode.UPGRADE);
            for (AccountBO account : customer.getAccounts()) {
                for (AccountFeesEntity accountFees : account.getAccountFees()) {
                    if (!feeList.contains(accountFees.getFees())) {
                        feeList.add(accountFees.getFees());
                    }
                }
            }
            Transaction transaction = StaticHibernateUtil.startTransaction();
            deleteCustomerWithoutFee(customer);
            transaction.commit();
        }
        Transaction transaction = StaticHibernateUtil.startTransaction();
        deleteFees(feeList);
        transaction.commit();
    }

    private static void deleteCustomerWithoutFee(final CustomerBO customer) {
        Session session = StaticHibernateUtil.getSessionTL();
        deleteCenterMeeting(customer);
        deleteClientAttendence(customer);
        for (AccountBO account : customer.getAccounts()) {
            if (null != account) {
                deleteAccountWithoutFee(account);
            }
        }
        session.delete(customer);
    }

    private static void deleteCustomer(CustomerBO customer) {
        Session session = StaticHibernateUtil.getSessionTL();
        StaticHibernateUtil.startTransaction();
        customer = (CustomerBO) session.load(CustomerBO.class, customer.getCustomerId());
        session.lock(customer, LockMode.NONE);
        deleteCenterMeeting(customer);
        deleteClientAttendence(customer);
        deleteCustomerNotes(customer);
        deleteClientOfferings(customer);

        List<FeeBO> feeList = new ArrayList<FeeBO>();
        for (AccountBO account : customer.getAccounts()) {
            if (null != account) {
                for (AccountFeesEntity accountFee : account.getAccountFees()) {
                    if (!feeList.contains(accountFee.getFees())) {
                        feeList.add(accountFee.getFees());
                    }
                }
                deleteAccountWithoutFee(account);
            }
        }
        session.delete(customer);
        deleteFees(feeList);
        StaticHibernateUtil.commitTransaction();
    }

    private static void deleteClientOfferings(final CustomerBO customer) {
        Session session = StaticHibernateUtil.getSessionTL();
        if (customer instanceof ClientBO) {
            Set<ClientInitialSavingsOfferingEntity> clientOfferings = ((ClientBO) customer)
                    .getOfferingsAssociatedInCreate();
            if (clientOfferings != null) {
                for (ClientInitialSavingsOfferingEntity clientOffering : clientOfferings) {
                    session.delete(clientOffering);
                }
            }
        }
    }

    private static void deleteCustomerNotes(final CustomerBO customer) {
        Session session = StaticHibernateUtil.getSessionTL();
        Set<CustomerNoteEntity> customerNotes = customer.getCustomerNotes();
        if (customerNotes != null) {
            for (CustomerNoteEntity customerNote : customerNotes) {
                session.delete(customerNote);
            }
        }

    }

    private static void deleteHoliday(final Holiday holiday) {
        Session session = StaticHibernateUtil.getSessionTL();
        session.delete(holiday);

    }

    private static void deleteCenterMeeting(final CustomerBO customer) {
        Session session = StaticHibernateUtil.getSessionTL();
        if (customer instanceof CenterBO) {
            // session.delete(customer.getCustomerMeeting());
        }
    }

    public static void deleteClientAttendence(final CustomerBO customer) {
        Session session = StaticHibernateUtil.getSessionTL();
        if (customer instanceof ClientBO) {
            Set<ClientAttendanceBO> attendance = ((ClientBO) customer).getClientAttendances();
            if (attendance != null && attendance.size() > 0) {
                for (ClientAttendanceBO custAttendance : attendance) {
                    // custAttendance.setCustomer(null);
                    session.delete(custAttendance);
                }
            }
        }
    }

    public static void updateObject(final AbstractEntity obj) {
        testObjectPersistence.update(obj);
    }

    private static UserContext userContext;

    public static UserContext getContext() {
        try {
            if (userContext == null) {
                userContext = getUserContext();
            }
            return userContext;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ActivityContext activityContext;

    public static ActivityContext getActivityContext() {
        if (activityContext == null) {
            UserContext uc = getContext();
            activityContext = new ActivityContext((short) 0, uc.getBranchId().shortValue(), uc.getId().shortValue());
        }
        return activityContext;
    }

    // FIXME: why is this here?
    private static final ThreadLocal<TestObjectsHolder> threadLocal = new ThreadLocal<TestObjectsHolder>();

    public static Object addObject(final Object obj) {
        TestObjectsHolder holder = threadLocal.get();
        if (holder == null) {
            holder = new TestObjectsHolder();
            threadLocal.set(holder);
        }
        holder.addObject(obj);

        return obj;
    }

    public static void cleanUpTestObjects() {
        TestObjectsHolder holder = threadLocal.get();
        if (holder != null) {
            holder.removeObjects();
        }

        holder = null;
        threadLocal.set(null);

    }

    /**
     * Also see {@link TestUtils#makeUser()} which should be faster (this method involves several database accesses).
     */
    private static UserContext getUserContext() throws SystemException, ApplicationException {
        byte[] password = EncryptionService.getInstance().createEncryptedPassword("mifos");
        PersonnelBO personnel = getPersonnel(PersonnelConstants.SYSTEM_USER);
        personnel.setEncryptedPassword(password);
        updateObject(personnel);
        return personnel.login("mifos");
    }

    public static void flushandCloseSession() {
        testObjectPersistence.flushandCloseSession();
    }

    public static CustomerBO getCustomer(final Integer customerId) {
        return testObjectPersistence.getCustomer(customerId);
    }

    public static GroupBO getGroup(final Integer groupId) {
        return testObjectPersistence.getGroup(groupId);
    }

    public static ClientBO getClient(final Integer clientId) {
        return testObjectPersistence.getClient(clientId);
    }

    public static CenterBO getCenter(final Integer centerId) {
        return testObjectPersistence.getCenter(centerId);
    }

    public static <T> T getObject(final Class<T> clazz, final Integer pk) {
        T object = testObjectPersistence.getObject(clazz, pk);
        addObject(object);
        return object;
    }

    public static Object getObject(final Class clazz, final Short pk) {
        return addObject(testObjectPersistence.getObject(clazz, pk));
    }

    public static CustomerCheckListBO createCustomerChecklist(final Short customerLevel, final Short customerStatus,
                                                              final Short checklistStatus) throws Exception {
        List<String> details = new ArrayList<String>();
        details.add("item1");
        CustomerLevelEntity customerLevelEntity = new CustomerLevelEntity(CustomerLevel.getLevel(customerLevel));
        CustomerStatusEntity customerStatusEntity = new CustomerStatusEntity(CustomerStatus.fromInt(customerStatus));
        CustomerCheckListBO customerChecklist = new CustomerCheckListBO(customerLevelEntity, customerStatusEntity,
                "productchecklist", checklistStatus, details, TEST_LOCALE, PersonnelConstants.SYSTEM_USER);
        customerChecklist.save();
        StaticHibernateUtil.commitTransaction();
        return customerChecklist;
    }

    public static AccountCheckListBO createAccountChecklist(final Short prdTypeId, final AccountState accountState,
                                                            final Short checklistStatus) throws Exception {
        List<String> details = new ArrayList<String>();
        details.add("item1");
        ProductTypeEntity productTypeEntity = (ProductTypeEntity) StaticHibernateUtil.getSessionTL().get(
                ProductTypeEntity.class, prdTypeId);
        AccountStateEntity accountStateEntity = new AccountStateEntity(accountState);
        AccountCheckListBO accountChecklist = new AccountCheckListBO(productTypeEntity, accountStateEntity,
                "productchecklist", checklistStatus, details, TEST_LOCALE, PersonnelConstants.SYSTEM_USER);
        accountChecklist.save();
        StaticHibernateUtil.commitTransaction();
        return accountChecklist;
    }

    public static void cleanUp(CheckListBO checkListBO) {
        if (null != checkListBO) {
            deleteChecklist(checkListBO);
            checkListBO = null;
        }
    }

    public static void deleteChecklist(final CheckListBO checkListBO) {
        Session session = StaticHibernateUtil.getSessionTL();
        Transaction transaction = StaticHibernateUtil.startTransaction();

        if (checkListBO.getChecklistDetails() != null) {
            for (CheckListDetailEntity checklistDetail : checkListBO.getChecklistDetails()) {
                if (null != checklistDetail) {
                    session.delete(checklistDetail);
                }
            }
        }
        session.delete(checkListBO);
        transaction.commit();

    }

    public static void cleanUp(ReportsCategoryBO reportsCategoryBO) {
        if (null != reportsCategoryBO) {
            deleteReportCategory(reportsCategoryBO);
            reportsCategoryBO = null;
        }
    }

    public static void deleteReportCategory(final ReportsCategoryBO reportsCategoryBO) {

        Session session = StaticHibernateUtil.getSessionTL();
        Transaction transaction = StaticHibernateUtil.startTransaction();
        session.delete(reportsCategoryBO);
        transaction.commit();
    }

    public static void cleanUp(ReportsBO reportsBO) {
        if (null != reportsBO) {
            deleteReportCategory(reportsBO);
            reportsBO = null;
        }
    }

    public static void deleteReportCategory(final ReportsBO reportsBO) {

        Session session = StaticHibernateUtil.getSessionTL();
        Transaction transaction = StaticHibernateUtil.startTransaction();
        session.delete(reportsBO);
        transaction.commit();
    }

    public static LoanBO createLoanAccountWithDisbursement(final String globalNum, final CustomerBO customer,
                                                           final AccountState state, final Date startDate, final LoanOfferingBO loanOfering, final int disbursalType) {

        final LoanBO loan = LoanBOTestUtils.createLoanAccountWithDisbursement(customer, state, startDate, loanOfering,
                disbursalType, Short.valueOf("6"));

        try {
            loan.save();
        } catch (AccountException e) {
            throw new RuntimeException(e);
        }

        StaticHibernateUtil.commitTransaction();
        return (LoanBO) addObject(getObject(LoanBO.class, loan.getAccountId()));
    }

    private static void deleteAccountWithoutDeletetingProduct(final AccountBO account, Session session) {
        boolean newSession = false;

        Transaction transaction = null;
        if (null == session) {
            session = StaticHibernateUtil.getSessionTL();
            transaction = StaticHibernateUtil.startTransaction();
            newSession = true;
        }
        for (AccountPaymentEntity accountPayment : account.getAccountPayments()) {
            if (null != accountPayment) {
                deleteAccountPayment(accountPayment, session);
            }
        }
        for (AccountActionDateEntity actionDates : account.getAccountActionDates()) {
            if (account.getAccountType().getAccountTypeId().equals(
                    org.mifos.accounts.util.helpers.AccountTypes.LOAN_ACCOUNT)) {
                LoanScheduleEntity loanScheduleEntity = (LoanScheduleEntity) actionDates;
                for (AccountFeesActionDetailEntity actionFees : loanScheduleEntity.getAccountFeesActionDetails()) {
                    session.delete(actionFees);
                }
            }
            if (account.getAccountType().getAccountTypeId().equals(
                    org.mifos.accounts.util.helpers.AccountTypes.CUSTOMER_ACCOUNT)) {
                CustomerScheduleEntity customerScheduleEntity = (CustomerScheduleEntity) actionDates;
                for (AccountFeesActionDetailEntity actionFees : customerScheduleEntity.getAccountFeesActionDetails()) {
                    session.delete(actionFees);
                }
            }
            session.delete(actionDates);
        }

        List<FeeBO> feeList = new ArrayList<FeeBO>();
        for (AccountFeesEntity accountFees : account.getAccountFees()) {
            if (!feeList.contains(accountFees.getFees())) {
                feeList.add(accountFees.getFees());
            }
        }

        for (AccountFeesEntity accountFees : account.getAccountFees()) {
            session.delete(accountFees);
        }

        deleteFees(feeList);
        if (account instanceof LoanBO) {

            LoanBO loan = (LoanBO) account;
            if (null != loan.getLoanSummary()) {
                session.delete(loan.getLoanSummary());
            }
            session.delete(account);

        }
        session.delete(account);

        if (newSession) {
            transaction.commit();
        }
    }

    public static void cleanUpWithoutDeletingProduct(AccountBO account) {
        if (null != account) {
            deleteAccountWithoutDeletetingProduct(account, null);
            account = null;
        }
    }

    public static PaymentData getCustomerAccountPaymentDataView(final List<AccountActionDateEntity> accountActions,
                                                                final Money totalAmount, final CustomerBO customer, final PersonnelBO personnel, final String receiptNum,
                                                                final Short paymentId, final Date receiptDate, final Date transactionDate) {
        PaymentData paymentData = PaymentData.createPaymentData(totalAmount, personnel, paymentId, transactionDate);
        paymentData.setCustomer(customer);
        paymentData.setReceiptDate(receiptDate);
        paymentData.setReceiptNum(receiptNum);
        for (AccountActionDateEntity actionDate : accountActions) {
            CustomerAccountPaymentData customerAccountPaymentData = new CustomerAccountPaymentData(actionDate);
            paymentData.addAccountPaymentData(customerAccountPaymentData);
        }

        return paymentData;
    }

    public static CustomerAccountDto getCustomerAccountView(final CustomerBO customer) throws Exception {
        CustomerAccountDto customerAccountDto = new CustomerAccountDto(customer.getCustomerAccount().getAccountId(),
                TestUtils.RUPEE);
        List<AccountActionDateEntity> accountAction = getDueActionDatesForAccount(customer.getCustomerAccount()
                .getAccountId(), new java.sql.Date(System.currentTimeMillis()));
        customerAccountDto.setAccountActionDates(getBulkEntryAccountActionViews(accountAction));
        return customerAccountDto;
    }

    public static List<AccountActionDateEntity> getDueActionDatesForAccount(final Integer accountId,
                                                                            final java.sql.Date transactionDate) throws Exception {
        List<AccountActionDateEntity> dueActionDates = new CustomerPersistence().retrieveCustomerAccountActionDetails(
                accountId, transactionDate);
        for (AccountActionDateEntity accountActionDate : dueActionDates) {
            Hibernate.initialize(accountActionDate);

            if (accountActionDate instanceof LoanScheduleEntity) {
                LoanScheduleEntity loanScheduleEntity = (LoanScheduleEntity) accountActionDate;
                for (AccountFeesActionDetailEntity accountFeesActionDetail : loanScheduleEntity
                        .getAccountFeesActionDetails()) {
                    Hibernate.initialize(accountFeesActionDetail);
                }
            }
            if (accountActionDate instanceof CustomerScheduleEntity) {
                CustomerScheduleEntity customerScheduleEntity = (CustomerScheduleEntity) accountActionDate;
                for (AccountFeesActionDetailEntity accountFeesActionDetail : customerScheduleEntity
                        .getAccountFeesActionDetails()) {
                    Hibernate.initialize(accountFeesActionDetail);
                }
            }
        }
        StaticHibernateUtil.closeSession();
        return dueActionDates;
    }

    public static PaymentData getLoanAccountPaymentData(final List<AccountActionDateEntity> accountActions,
                                                        final Money totalAmount, final CustomerBO customer, final PersonnelBO personnel, final String receiptNum,
                                                        final Short paymentId, final Date receiptDate, final Date transactionDate) {
        PaymentData paymentData = PaymentData.createPaymentData(totalAmount, personnel, paymentId, transactionDate);
        paymentData.setCustomer(customer);
        paymentData.setReceiptDate(receiptDate);
        paymentData.setReceiptNum(receiptNum);
        return paymentData;
    }

    public static LoanAccountDto getLoanAccountView(final LoanBO loan) {
        final Integer customerId = null;
        return new LoanAccountDto(loan.getAccountId(), customerId, loan.getLoanOffering().getPrdOfferingName(), loan
                .getLoanOffering().getPrdOfferingId(), loan.getState().getValue(), loan.getIntrestAtDisbursement(),
                loan.getLoanBalance());
    }

    public static CollectionSheetEntryInstallmentDto getBulkEntryAccountActionView(
            final AccountActionDateEntity accountActionDateEntity) {
        CollectionSheetEntryInstallmentDto bulkEntryAccountActionView = null;
        if (accountActionDateEntity instanceof LoanScheduleEntity) {
            LoanScheduleEntity actionDate = (LoanScheduleEntity) accountActionDateEntity;
            CollectionSheetEntryLoanInstallmentDto installmentView = new CollectionSheetEntryLoanInstallmentDto(
                    actionDate.getAccount().getAccountId(), actionDate.getCustomer().getCustomerId(), actionDate
                            .getInstallmentId(), actionDate.getActionDateId(), actionDate.getActionDate(), actionDate
                            .getPrincipal(), actionDate.getPrincipalPaid(), actionDate.getInterest(), actionDate
                            .getInterestPaid(), actionDate.getMiscFee(), actionDate.getMiscFeePaid(), actionDate
                            .getPenalty(), actionDate.getPenaltyPaid(), actionDate.getMiscPenalty(), actionDate
                            .getMiscPenaltyPaid(), TestUtils.RUPEE);
            installmentView
                    .setCollectionSheetEntryAccountFeeActions(getBulkEntryAccountFeeActionViews(accountActionDateEntity));
            bulkEntryAccountActionView = installmentView;
        } else if (accountActionDateEntity instanceof SavingsScheduleEntity) {
            SavingsScheduleEntity actionDate = (SavingsScheduleEntity) accountActionDateEntity;
            CollectionSheetEntrySavingsInstallmentDto installmentView = new CollectionSheetEntrySavingsInstallmentDto(
                    actionDate.getAccount().getAccountId(), actionDate.getCustomer().getCustomerId(), actionDate
                            .getInstallmentId(), actionDate.getActionDateId(), actionDate.getActionDate(), actionDate
                            .getDeposit(), actionDate.getDepositPaid());
            bulkEntryAccountActionView = installmentView;

        } else if (accountActionDateEntity instanceof CustomerScheduleEntity) {
            CustomerScheduleEntity actionDate = (CustomerScheduleEntity) accountActionDateEntity;
            CollectionSheetEntryCustomerAccountInstallmentDto installmentView = new CollectionSheetEntryCustomerAccountInstallmentDto(
                    actionDate.getAccount().getAccountId(), actionDate.getCustomer().getCustomerId(), actionDate
                            .getInstallmentId(), actionDate.getActionDateId(), actionDate.getActionDate(), actionDate
                            .getMiscFee(), actionDate.getMiscFeePaid(), actionDate.getMiscPenalty(), actionDate
                            .getMiscPenaltyPaid(), TestUtils.RUPEE);
            installmentView
                    .setCollectionSheetEntryAccountFeeActions(getBulkEntryAccountFeeActionViews(accountActionDateEntity));
            bulkEntryAccountActionView = installmentView;
        }
        return bulkEntryAccountActionView;
    }

    public static CollectionSheetEntryAccountFeeActionDto getBulkEntryAccountFeeActionView(
            final AccountFeesActionDetailEntity feeAction) {
        return new CollectionSheetEntryAccountFeeActionDto(feeAction.getAccountActionDate().getActionDateId(),
                feeAction.getFee().getFeeId(), feeAction.getFeeAmount(), feeAction.getFeeAmountPaid());
    }

    public static List<CollectionSheetEntryAccountFeeActionDto> getBulkEntryAccountFeeActionViews(
            final AccountActionDateEntity accountActionDateEntity) {
        List<CollectionSheetEntryAccountFeeActionDto> bulkEntryFeeViews = new ArrayList<CollectionSheetEntryAccountFeeActionDto>();
        Set<AccountFeesActionDetailEntity> feeActions = null;
        if (accountActionDateEntity instanceof LoanScheduleEntity) {
            feeActions = ((LoanScheduleEntity) accountActionDateEntity).getAccountFeesActionDetails();
        } else if (accountActionDateEntity instanceof CustomerScheduleEntity) {
            feeActions = ((CustomerScheduleEntity) accountActionDateEntity).getAccountFeesActionDetails();
        }
        if (feeActions != null && feeActions.size() > 0) {
            for (AccountFeesActionDetailEntity accountFeesActionDetail : feeActions) {
                bulkEntryFeeViews.add(getBulkEntryAccountFeeActionView(accountFeesActionDetail));
            }
        }
        return bulkEntryFeeViews;

    }

    public static List<CollectionSheetEntryInstallmentDto> getBulkEntryAccountActionViews(
            final List<AccountActionDateEntity> actionDates) {
        List<CollectionSheetEntryInstallmentDto> bulkEntryActionViews = new ArrayList<CollectionSheetEntryInstallmentDto>();
        if (actionDates != null && actionDates.size() > 0) {
            for (AccountActionDateEntity actionDate : actionDates) {
                bulkEntryActionViews.add(getBulkEntryAccountActionView(actionDate));
            }
        }
        return bulkEntryActionViews;

    }

    public static CustomerNoteEntity getCustomerNote(final String comment, final CustomerBO customer) {
        java.sql.Date commentDate = new java.sql.Date(System.currentTimeMillis());
        CustomerNoteEntity notes = new CustomerNoteEntity(comment, commentDate, customer.getPersonnel(), customer);
        return (CustomerNoteEntity) addObject(notes);
    }

    public static OfficeBO createOffice(final OfficeLevel level, final OfficeBO parentOffice, final String officeName,
                                        final String shortName) throws Exception {
        OfficeBO officeBO = new OfficeBO(TestUtils.makeUserWithLocales(), level, parentOffice, null, officeName,
                shortName, null, OperationMode.REMOTE_SERVER);
        officeBO.save();
        StaticHibernateUtil.commitTransaction();
        return (OfficeBO) addObject(officeBO);
    }

    public static void cleanUp(final OfficeBO office) {
        if (office != null) {
            Session session = StaticHibernateUtil.getSessionTL();
            Transaction transaction = StaticHibernateUtil.startTransaction();
            session.lock(office, LockMode.NONE);
            session.delete(office);
            transaction.commit();
        }
    }

    public static void removeCustomerFromPosition(final CustomerBO customer) throws CustomerException {
        if (customer != null) {
            for (CustomerPositionEntity customerPositionEntity : customer.getCustomerPositions()) {
                customerPositionEntity.setCustomer(null);
            }
            customer.update();
            StaticHibernateUtil.commitTransaction();
        }
    }

    public static void cleanUp(final PersonnelBO personnel) {
        if (personnel != null) {
            Session session = StaticHibernateUtil.getSessionTL();
            Transaction transaction = StaticHibernateUtil.startTransaction();
            session.lock(personnel, LockMode.NONE);
            session.delete(personnel);
            transaction.commit();
        }
    }

    public static PersonnelBO createPersonnel(final PersonnelLevel level, final OfficeBO office, final Integer title,
                                              final Short preferredLocale, final String password, final String userName, final String emailId,
                                              final List<RoleBO> personnelRoles, final List<CustomFieldDto> customFields, final Name name,
                                              final String governmentIdNumber, final Date dob, final Integer maritalStatus, final Integer gender,
                                              final Date dateOfJoiningMFI, final Date dateOfJoiningBranch, final Address address) throws Exception {
        PersonnelBO personnelBO = new PersonnelBO(level, office, title, preferredLocale, password, userName, emailId,
                personnelRoles, customFields, name, governmentIdNumber, dob, maritalStatus, gender, dateOfJoiningMFI,
                dateOfJoiningBranch, address, Short.valueOf("1"));

        personnelBO.save();
        StaticHibernateUtil.commitTransaction();
        return personnelBO;
    }

    public static void simulateInvalidConnection() {
        StaticHibernateUtil.getSessionTL().close();
    }

    public static void cleanUp(final RoleBO roleBO) {
        if (roleBO != null) {
            Session session = StaticHibernateUtil.getSessionTL();
            Transaction transaction = StaticHibernateUtil.startTransaction();
            session.lock(roleBO, LockMode.NONE);
            session.delete(roleBO);
            transaction.commit();
        }
    }

    public static RoleBO createRole(final UserContext context, final String roleName,
                                    final List<ActivityEntity> activities) throws Exception {
        RoleBO roleBO = new RoleBO(context, roleName, activities);
        roleBO.save();
        StaticHibernateUtil.commitTransaction();
        return roleBO;
    }

    public static void cleanUp(final FundBO fundBO) {
        if (fundBO != null) {
            Session session = StaticHibernateUtil.getSessionTL();
            Transaction transaction = StaticHibernateUtil.startTransaction();
            session.lock(fundBO, LockMode.NONE);
            session.delete(fundBO);
            transaction.commit();
        }
    }

    public static FundBO createFund(final FundCodeEntity fundCode, final String fundName) throws Exception {
        FundBO fundBO = new FundBO(fundCode, fundName);

        try {
            StaticHibernateUtil.startTransaction();
            DependencyInjectedServiceLocator.locateFundDao().save(fundBO);
            StaticHibernateUtil.commitTransaction();
            return fundBO;
        } catch (Exception e) {
            StaticHibernateUtil.rollbackTransaction();
            throw new MifosRuntimeException(e.getMessage(), e);
        } finally {
            StaticHibernateUtil.closeSession();
        }
    }

    public static GroupBO createGroupUnderBranch(final String customerName, final CustomerStatus customerStatus,
                                                 final Short officeId, final MeetingBO meeting, final Short loanOfficerId,
                                                 final List<CustomFieldDto> customFields) {
        Short formedBy = new Short("1");
        return createGroupUnderBranch(customerName, customerStatus, null, false, null, null, customFields, getFees(),
                formedBy, officeId, meeting, loanOfficerId);
    }

    public static void cleanUpChangeLog() {
        Session session = StaticHibernateUtil.getSessionTL();
        StaticHibernateUtil.startTransaction();
        List<AuditLog> auditLogList = session
                .createQuery("from org.mifos.framework.components.audit.business.AuditLog").list();
        for (AuditLog auditLog : auditLogList) {
            session.delete(auditLog);
        }
        StaticHibernateUtil.commitTransaction();
    }

    public static List<AuditLog> getChangeLog(final EntityType type, final Integer entityId) {
        return StaticHibernateUtil.getSessionTL().createQuery(
                "from org.mifos.framework.components.audit.business.AuditLog al " + "where al.entityType="
                        + type.getValue() + " and al.entityId=" + entityId).list();
    }

    public static void cleanUp(final AuditLog auditLog) {
        Session session = StaticHibernateUtil.getSessionTL();
        Transaction transaction = StaticHibernateUtil.startTransaction();
        session.lock(auditLog, LockMode.NONE);
        session.delete(auditLog);
        transaction.commit();
    }

    public static Calendar getCalendar(final Date date) {
        Calendar dateCalendar = new GregorianCalendar();
        dateCalendar.setTimeInMillis(date.getTime());
        int year = dateCalendar.get(Calendar.YEAR);
        int month = dateCalendar.get(Calendar.MONTH);
        int day = dateCalendar.get(Calendar.DAY_OF_MONTH);
        dateCalendar = new GregorianCalendar(year, month, day);
        return dateCalendar;
    }

    /**
     * Return the int corresponding to the day of the week of the date parameter. Returns Calendar.SUNDAY (1),
     * Calendar.MONDAY (2), etc.
     */
    public static int getDayForDate(final Date date) {
        Calendar dateConversionCalendar = new GregorianCalendar();
        dateConversionCalendar.setTime(date);
        return dateConversionCalendar.get(Calendar.DAY_OF_WEEK);
    }

    /**
     * This method is not recommended because it calls
     * {@link #getNewMeetingForToday(RecurrenceType, short, MeetingType)}.
     */
    public static SavingsOfferingBO createSavingsProduct(final String offeringName, final String shortName,
                                                         final Short depGLCode, final Short intGLCode, final RecommendedAmountUnit recommendedAmountUnit) {
        MeetingBO meetingIntCalc = createMeeting(getNewMeetingForToday(WEEKLY, EVERY_WEEK, CUSTOMER_MEETING));
        MeetingBO meetingIntPost = createMeeting(getNewMeetingForToday(WEEKLY, EVERY_WEEK, CUSTOMER_MEETING));
        return createSavingsProduct(offeringName, shortName, ApplicableTo.GROUPS, new Date(System.currentTimeMillis()),
                PrdStatus.SAVINGS_ACTIVE, 300.0, recommendedAmountUnit, 24.0, 200.0, 200.0, SavingsType.VOLUNTARY,
                InterestCalcType.MINIMUM_BALANCE, meetingIntCalc, meetingIntPost, depGLCode, intGLCode);
    }

    /**
     * This method is not recommended because it indirectly calls
     * {@link #getNewMeetingForToday(RecurrenceType, short, MeetingType)}.
     */
    public static SavingsOfferingBO createSavingsProduct(final String offeringName, final String shortName,
                                                         final RecommendedAmountUnit recommendedAmountUnit) {
        return createSavingsProduct(offeringName, shortName, TestGeneralLedgerCode.ASSETS,
                TestGeneralLedgerCode.CASH_AND_BANK_BALANCES, recommendedAmountUnit);
    }

    public static SavingsOfferingBO createSavingsProduct(final String productName, final String shortName,
                                                         final Date currentDate, final RecommendedAmountUnit recommendedAmountUnit) {
        MeetingBO meetingIntCalc = createMeeting(getTypicalMeeting());
        MeetingBO meetingIntPost = createMeeting(getTypicalMeeting());
        return createSavingsProduct(productName, shortName, currentDate, recommendedAmountUnit, meetingIntCalc,
                meetingIntPost);
    }

    public static SavingsOfferingBO createSavingsProduct(final String productName, final String shortName,
                                                         final Date currentDate, final RecommendedAmountUnit recommendedAmountUnit, final MeetingBO meetingIntCalc,
                                                         final MeetingBO meetingIntPost) {
        return createSavingsProduct(productName, shortName, ApplicableTo.GROUPS, currentDate, PrdStatus.SAVINGS_ACTIVE,
                300.0, recommendedAmountUnit, 1.2, 200.0, 200.0, SavingsType.VOLUNTARY,
                InterestCalcType.MINIMUM_BALANCE, meetingIntCalc, meetingIntPost);
    }

    public static void cleanUp(SavingsOfferingBO savingPrdBO) {
        if (null != savingPrdBO) {
            deleteSavingProduct(savingPrdBO);
            savingPrdBO = null;
        }
    }

    public static void cleanUp(MeetingBO meeting) {
        if (null != meeting) {
            deleteMeeting(meeting);
            meeting = null;
        }
    }

    public static void cleanUp(ProductMixBO prdmix) {
        if (null != prdmix) {
            deleteProductMix(prdmix);
            prdmix = null;
        }
    }

    private static void deleteProductMix(final ProductMixBO prdmix) {
        Session session = StaticHibernateUtil.getSessionTL();
        session.lock(prdmix, LockMode.UPGRADE);
        Transaction transaction = StaticHibernateUtil.startTransaction();
        session.delete(prdmix);
        transaction.commit();
    }

    private static void deleteSavingProduct(final SavingsOfferingBO savingPrdBO) {
        Session session = StaticHibernateUtil.getSessionTL();
        session.lock(savingPrdBO, LockMode.UPGRADE);
        Transaction transaction = StaticHibernateUtil.startTransaction();
        session.delete(savingPrdBO);
        transaction.commit();
    }

    private static void deleteMeeting(MeetingBO meeting) {
        Session session = StaticHibernateUtil.getSessionTL();
        Transaction transaction = StaticHibernateUtil.startTransaction();
        meeting = (MeetingBO) session.load(MeetingBO.class, meeting.getMeetingId());
        session.delete(meeting);
        transaction.commit();
    }

    public static ProductMixBO createAllowedProductsMix(final SavingsOfferingBO saving1, final SavingsOfferingBO saving2) {
        ProductMixBO prdmix;
        try {
            prdmix = new ProductMixBO(saving1, saving2);
            addObject(testObjectPersistence.persist(prdmix));
            StaticHibernateUtil.commitTransaction();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return prdmix;
    }

    public static final int SAMPLE_BUSINESS_ACTIVITY_2 = 2;

    public static GroupBO createInstanceForTest(final UserContext userContext, final GroupTemplate template,
                                                final CenterBO center, final Date customerActivationDate) {

        List<CustomFieldDto> customFields = new ArrayList<CustomFieldDto>();
        if (template.getCustomFieldViews() != null) {
            customFields = template.getCustomFieldViews();
        }

        List<CustomerCustomFieldEntity> customerCustomFields = CustomerCustomFieldEntity.fromDto(customFields, null);

        PersonnelBO formedBy = null;
        GroupBO group = GroupBO.createGroupWithCenterAsParent(userContext, template.getDisplayName(), formedBy, center,
                customerCustomFields, template.getAddress(), template.getExternalId(), template.isTrained(),
                new DateTime(template.getTrainedDate()), template.getCustomerStatus());

        // GroupBO group = new GroupBO(userContext, template.getDisplayName(), template.getCustomerStatus(), template
        // .getExternalId(), template.isTrained(), template.getTrainedDate(), template.getAddress(), template
        // .getCustomFieldViews(), template.getFees(), new PersonnelPersistence().getPersonnel(template
        // .getLoanOfficerId()), center);
        group.setCustomerActivationDate(customerActivationDate);
        return group;
    }

    /*
     * Gets the test data office with office_id == 3
     */

    public static OfficeBO getBranchOffice() {
        try {
            return new OfficePersistence().getOffice(TestObjectFactory.SAMPLE_BRANCH_OFFICE);
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Gets the test data user personnel_id == 1
     */

    public static PersonnelBO getSystemUser() {
        try {
            return new PersonnelPersistence().getPersonnel(PersonnelConstants.SYSTEM_USER);
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Gets the test data user personnel_id == 3
     */

    public static PersonnelBO getTestUser() {
        try {
            return new PersonnelPersistence().getPersonnel(PersonnelConstants.TEST_USER);
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        }
    }

    public static org.mifos.accounts.fees.servicefacade.FeeDto getAmountBasedFee(String feeId, String statusId,
                                                                                 String amount) {
        org.mifos.accounts.fees.servicefacade.FeeDto fee = new org.mifos.accounts.fees.servicefacade.FeeDto();
        FeeStatusDto feeStatus = new FeeStatusDto();
        feeStatus.setId(statusId);
        fee.setFeeStatus(feeStatus);
        fee.setRateBasedFee(false);
        fee.setAmount(TestUtils.createMoney(amount));
        fee.setId(feeId);
        return fee;
    }

    public static org.mifos.accounts.fees.servicefacade.FeeDto getRateBasedFee(String feeId, String statusId,
                                                                               double rate, String formulaId) {
        org.mifos.accounts.fees.servicefacade.FeeDto fee = new org.mifos.accounts.fees.servicefacade.FeeDto();
        FeeStatusDto feeStatus = new FeeStatusDto();
        feeStatus.setId(statusId);
        fee.setFeeStatus(feeStatus);
        fee.setRateBasedFee(true);
        fee.setRate(rate);
        FeeFormulaDto feeFormula = new FeeFormulaDto();
        feeFormula.setId(Short.valueOf(formulaId));
        fee.setFeeFormula(feeFormula);
        fee.setId(feeId);
        return fee;
    }

}
