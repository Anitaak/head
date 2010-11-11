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


import org.mifos.accounts.fees.business.service.FeeService;
import org.mifos.accounts.fees.business.service.FeeServiceImpl;
import org.mifos.accounts.fees.persistence.FeeDao;
import org.mifos.accounts.fees.persistence.FeeDaoHibernate;
import org.mifos.accounts.fees.servicefacade.FeeServiceFacade;
import org.mifos.accounts.fees.servicefacade.WebTierFeeServiceFacade;
import org.mifos.accounts.financial.business.service.GeneralLedgerDao;
import org.mifos.accounts.financial.business.service.GeneralLedgerDaoHibernate;
import org.mifos.accounts.fund.persistence.FundDao;
import org.mifos.accounts.fund.persistence.FundDaoHibernate;
import org.mifos.accounts.fund.servicefacade.FundServiceFacade;
import org.mifos.accounts.fund.servicefacade.WebTierFundServiceFacade;
import org.mifos.accounts.loan.business.ScheduleCalculatorAdaptor;
import org.mifos.accounts.loan.business.service.validators.InstallmentFormatValidator;
import org.mifos.accounts.loan.business.service.validators.InstallmentFormatValidatorImpl;
import org.mifos.accounts.loan.business.service.validators.InstallmentRulesValidator;
import org.mifos.accounts.loan.business.service.validators.InstallmentRulesValidatorImpl;
import org.mifos.accounts.loan.business.service.validators.InstallmentsValidator;
import org.mifos.accounts.loan.business.service.validators.InstallmentsValidatorImpl;
import org.mifos.accounts.loan.business.service.validators.ListOfInstallmentsValidator;
import org.mifos.accounts.loan.business.service.validators.ListOfInstallmentsValidatorImpl;
import org.mifos.accounts.loan.persistance.ClientAttendanceDao;
import org.mifos.accounts.loan.persistance.LoanDao;
import org.mifos.accounts.loan.persistance.LoanDaoHibernate;
import org.mifos.accounts.loan.persistance.LoanPersistence;
import org.mifos.accounts.loan.persistance.StandardClientAttendanceDao;
import org.mifos.accounts.persistence.AccountPersistence;
import org.mifos.accounts.productdefinition.persistence.LoanProductDao;
import org.mifos.accounts.productdefinition.persistence.LoanProductDaoHibernate;
import org.mifos.accounts.productdefinition.persistence.SavingsProductDao;
import org.mifos.accounts.productdefinition.persistence.SavingsProductDaoHibernate;
import org.mifos.accounts.savings.persistence.GenericDao;
import org.mifos.accounts.savings.persistence.GenericDaoHibernate;
import org.mifos.accounts.savings.persistence.SavingsDao;
import org.mifos.accounts.savings.persistence.SavingsDaoHibernate;
import org.mifos.accounts.savings.persistence.SavingsPersistence;
import org.mifos.application.admin.servicefacade.CheckListServiceFacade;
import org.mifos.application.admin.servicefacade.HolidayServiceFacade;
import org.mifos.application.admin.servicefacade.OfficeServiceFacade;
import org.mifos.application.admin.servicefacade.PersonnelServiceFacade;
import org.mifos.application.admin.servicefacade.RolesPermissionServiceFacade;
import org.mifos.application.collectionsheet.persistence.CollectionSheetDao;
import org.mifos.application.collectionsheet.persistence.CollectionSheetDaoHibernate;
import org.mifos.application.holiday.business.service.HolidayService;
import org.mifos.application.holiday.business.service.HolidayServiceImpl;
import org.mifos.application.holiday.persistence.HolidayDao;
import org.mifos.application.holiday.persistence.HolidayDaoHibernate;
import org.mifos.application.holiday.persistence.HolidayServiceFacadeWebTier;
import org.mifos.application.master.persistence.MasterPersistence;
import org.mifos.customers.business.service.CustomerService;
import org.mifos.customers.business.service.CustomerServiceImpl;
import org.mifos.customers.center.business.service.CenterDetailsServiceFacade;
import org.mifos.customers.center.business.service.WebTierCenterDetailsServiceFacade;
import org.mifos.customers.client.business.service.ClientDetailsServiceFacade;
import org.mifos.customers.client.business.service.WebTierClientDetailsServiceFacade;
import org.mifos.customers.group.business.service.GroupDetailsServiceFacade;
import org.mifos.customers.group.business.service.WebTierGroupDetailsServiceFacade;
import org.mifos.customers.office.business.service.LegacyOfficeServiceFacade;
import org.mifos.customers.office.business.service.OfficeServiceFacadeWebTier;
import org.mifos.customers.office.persistence.OfficeDao;
import org.mifos.customers.office.persistence.OfficeDaoHibernate;
import org.mifos.customers.office.persistence.OfficePersistence;
import org.mifos.customers.persistence.CustomerDao;
import org.mifos.customers.persistence.CustomerDaoHibernate;
import org.mifos.customers.persistence.CustomerPersistence;
import org.mifos.customers.personnel.business.service.PersonnelService;
import org.mifos.customers.personnel.business.service.PersonnelServiceImpl;
import org.mifos.customers.personnel.persistence.PersonnelDao;
import org.mifos.customers.personnel.persistence.PersonnelDaoHibernate;
import org.mifos.customers.personnel.persistence.PersonnelPersistence;
import org.mifos.framework.hibernate.helper.HibernateTransactionHelper;
import org.mifos.framework.hibernate.helper.HibernateTransactionHelperForStaticHibernateUtil;

/**
 * I contain static factory methods for locating/creating application services.
 *
 * NOTE: Use of DI frameworks method would make this redundant. e.g. spring/juice
 */
public class DependencyInjectedServiceLocator {

    // service facade
    private static CollectionSheetServiceFacade collectionSheetServiceFacade;
    private static LoanServiceFacade loanServiceFacade;
    private static SavingsServiceFacade savingsServiceFacade;
    private static CustomerServiceFacade customerServiceFacade;
    private static CenterDetailsServiceFacade centerDetailsServiceFacade;
    private static GroupDetailsServiceFacade groupDetailsServiceFacade;
    private static ClientDetailsServiceFacade clientDetailsServiceFacade;
    private static LegacyLoginServiceFacade loginServiceFacade;
    private static MeetingServiceFacade meetingServiceFacade;

    private static HolidayServiceFacade holidayServiceFacade;
    private static LegacyOfficeServiceFacade legacyOfficeServiceFacade;
    private static OfficeServiceFacade officeServiceFacade;
    private static FeeServiceFacade feeServiceFacade;
    private static FundServiceFacade fundServiceFacade;
    private static PersonnelServiceFacade personnelServiceFacade;
    private static RolesPermissionServiceFacade rolesPermissionServiceFacade;
    private static CheckListServiceFacade checkListServiceFacade;

    // services
    private static CollectionSheetService collectionSheetService;
    private static CustomerService customerService;
    private static HolidayService holidayService;
    private static FeeService feeService;
    private static PersonnelService personnelService;

    // DAOs
    private static OfficePersistence officePersistence = new OfficePersistence();
    private static MasterPersistence masterPersistence = new MasterPersistence();
    private static PersonnelPersistence personnelPersistence = new PersonnelPersistence();
    private static CustomerPersistence customerPersistence = new CustomerPersistence();
    private static SavingsPersistence savingsPersistence = new SavingsPersistence();
    private static LoanPersistence loanPersistence = new LoanPersistence();
    private static AccountPersistence accountPersistence = new AccountPersistence();
    private static ClientAttendanceDao clientAttendanceDao = new StandardClientAttendanceDao(masterPersistence);

    private static GenericDao genericDao = new GenericDaoHibernate();
    private static FundDao fundDao = new FundDaoHibernate(genericDao);
    private static LoanDao loanDao = new LoanDaoHibernate(genericDao);
    private static FeeDao feeDao = new FeeDaoHibernate(genericDao);
    private static OfficeDao officeDao = new OfficeDaoHibernate(genericDao);
    private static PersonnelDao personnelDao = new PersonnelDaoHibernate(genericDao);
    private static HolidayDao holidayDao = new HolidayDaoHibernate(genericDao);
    private static CustomerDao customerDao = new CustomerDaoHibernate(genericDao);
    private static LoanProductDao loanProductDao = new LoanProductDaoHibernate(genericDao);
    private static SavingsDao savingsDao = new SavingsDaoHibernate(genericDao);
    private static SavingsProductDao savingsProductDao = new SavingsProductDaoHibernate(genericDao);
    private static CollectionSheetDao collectionSheetDao = new CollectionSheetDaoHibernate(savingsDao);
    private static GeneralLedgerDao generalLedgerDao = new GeneralLedgerDaoHibernate(genericDao);

    // translators
    private static CollectionSheetDtoTranslator collectionSheetTranslator = new CollectionSheetDtoTranslatorImpl();

    // helpers
    private static HibernateTransactionHelper hibernateTransactionHelper = new HibernateTransactionHelperForStaticHibernateUtil();

    // validators
    private static InstallmentFormatValidator installmentFormatValidator;
    private static ListOfInstallmentsValidator listOfInstallmentsValidator;
    private static InstallmentRulesValidator installmentRulesValidator;
    private static InstallmentsValidator installmentsValidator;

    public static CollectionSheetService locateCollectionSheetService() {

        if (collectionSheetService == null) {
            collectionSheetService = new CollectionSheetServiceImpl(clientAttendanceDao, loanPersistence,
                    accountPersistence, savingsPersistence, collectionSheetDao);
        }
        return collectionSheetService;
    }

    public static CustomerService locateCustomerService() {

        if (customerService == null) {
            customerService = new CustomerServiceImpl(customerDao, personnelDao, officeDao, holidayDao,
                    hibernateTransactionHelper);
        }
        return customerService;
    }

    public static HolidayService locateHolidayService() {

        if (holidayService == null) {
            holidayService = new HolidayServiceImpl(officeDao, holidayDao, hibernateTransactionHelper);
        }
        return holidayService;
    }

    public static FeeService locateFeeService() {

        if (feeService == null) {
            feeService = new FeeServiceImpl(feeDao, generalLedgerDao, hibernateTransactionHelper);
        }
        return feeService;
    }

    public static CollectionSheetServiceFacade locateCollectionSheetServiceFacade() {

        if (collectionSheetServiceFacade == null) {
            collectionSheetService = DependencyInjectedServiceLocator.locateCollectionSheetService();

            collectionSheetServiceFacade = new CollectionSheetServiceFacadeWebTier(officePersistence,
                    masterPersistence, personnelPersistence, customerPersistence, collectionSheetService,
                    collectionSheetTranslator);
        }
        return collectionSheetServiceFacade;
    }

    public static CustomerServiceFacade locateCustomerServiceFacade() {
        if (customerServiceFacade == null) {

            customerService = DependencyInjectedServiceLocator.locateCustomerService();

            customerServiceFacade = new CustomerServiceFacadeWebTier(customerService, officeDao, personnelDao,
                    customerDao);
        }
        return customerServiceFacade;
    }

    public static ClientDetailsServiceFacade locateClientDetailsServiceFacade() {
        if (clientDetailsServiceFacade == null) {
            clientDetailsServiceFacade = new WebTierClientDetailsServiceFacade(customerDao);
        }
        return clientDetailsServiceFacade;
    }

    public static GroupDetailsServiceFacade locateGroupDetailsServiceFacade() {
        if (groupDetailsServiceFacade == null) {
            groupDetailsServiceFacade = new WebTierGroupDetailsServiceFacade(customerDao);
        }
        return groupDetailsServiceFacade;
    }

    public static CenterDetailsServiceFacade locateCenterDetailsServiceFacade() {
        if (centerDetailsServiceFacade == null) {
            centerDetailsServiceFacade = new WebTierCenterDetailsServiceFacade(customerDao);
        }
        return centerDetailsServiceFacade;
    }

    public static LoanServiceFacade locateLoanServiceFacade() {
        if (loanServiceFacade == null) {
            loanServiceFacade = new LoanServiceFacadeWebTier(loanProductDao, customerDao, personnelDao, fundDao, loanDao, locateInstallmentsValidator(), new ScheduleCalculatorAdaptor());
        }
        return loanServiceFacade;
    }

    public static LegacyLoginServiceFacade locationLoginServiceFacade() {
        if (loginServiceFacade == null) {
            if (personnelService == null) {
                personnelService = new PersonnelServiceImpl(personnelDao);
            }
            loginServiceFacade = new LoginServiceFacadeWebTier(personnelService, personnelDao);
        }
        return loginServiceFacade;
    }

    public static MeetingServiceFacade locateMeetingServiceFacade() {
        if (meetingServiceFacade == null) {
            customerService = locateCustomerService();
            meetingServiceFacade = new MeetingServiceFacadeWebTier(customerService);
        }
        return meetingServiceFacade;
    }

    public static PersonnelServiceFacade locatePersonnelServiceFacade() {
        if (personnelServiceFacade == null) {
            personnelServiceFacade = new PersonnelServiceFacadeWebTier(officeDao, customerDao, personnelDao);
        }
        return personnelServiceFacade;
    }

    public static RolesPermissionServiceFacade locateRolesPermissionServiceFacade() {
        if (rolesPermissionServiceFacade == null) {
            rolesPermissionServiceFacade = new RolesPermissionServiceFacadeWebTier();
        }
        return rolesPermissionServiceFacade;
    }

    public static CheckListServiceFacade locateCheckListServiceFacade() {
        if (checkListServiceFacade == null) {
            checkListServiceFacade = new CheckListServiceFacadeWebTier();
        }
        return checkListServiceFacade;
    }

    public static HolidayServiceFacade locateHolidayServiceFacade() {
        if (holidayServiceFacade == null) {
            holidayService = locateHolidayService();
            holidayServiceFacade = new HolidayServiceFacadeWebTier(holidayService, holidayDao);
        }
        return holidayServiceFacade;
    }

    public static OfficeServiceFacade locateOfficeServiceFacade() {
        if (officeServiceFacade == null) {
            officeServiceFacade = new OfficeServiceFacadeWebTier(officeDao, holidayDao);
        }
        return officeServiceFacade;
    }

    public static LegacyOfficeServiceFacade locateLegacyOfficeServiceFacade() {
        if (legacyOfficeServiceFacade == null) {
            legacyOfficeServiceFacade = new OfficeServiceFacadeWebTier(officeDao, holidayDao);
        }
        return legacyOfficeServiceFacade;
    }

    public static FeeServiceFacade locateFeeServiceFacade() {
        if (feeServiceFacade == null) {
            feeService = locateFeeService();
            feeServiceFacade = new WebTierFeeServiceFacade(feeService, feeDao, generalLedgerDao);
        }
        return feeServiceFacade;
    }

    public static FundServiceFacade locateFundServiceFacade() {
        if (fundServiceFacade == null) {
            fundServiceFacade = new WebTierFundServiceFacade(fundDao);
        }
        return fundServiceFacade;
    }

    public static SavingsServiceFacade locateSavingsServiceFacade() {
        if (savingsServiceFacade == null) {
            savingsServiceFacade = new SavingsServiceFacadeWebTier(savingsDao, personnelDao, customerDao);
        }
        return savingsServiceFacade;
    }

    public static CustomerDao locateCustomerDao() {
        return customerDao;
    }

    public static HolidayDao locateHolidayDao() {
        return holidayDao;
    }

    public static GenericDao locateGenericDao() {
        return genericDao;
    }

    public static PersonnelDao locatePersonnelDao() {
        return personnelDao;
    }

    public static OfficeDao locateOfficeDao() {
        return officeDao;
    }

    public static SavingsDao locateSavingsDao() {
        return savingsDao;
    }

    public static SavingsProductDao locateSavingsProductDao() {
        return savingsProductDao;
    }

    public static FundDao locateFundDao() {
        return fundDao;
    }

    public static InstallmentFormatValidator locateInstallmentFormatValidator() {
        if (installmentFormatValidator == null) {
            installmentFormatValidator = new InstallmentFormatValidatorImpl();
        }
        return installmentFormatValidator;
    }

    public static ListOfInstallmentsValidator locateListOfInstallmentsValidator() {
        if (listOfInstallmentsValidator == null) {
            listOfInstallmentsValidator = new ListOfInstallmentsValidatorImpl();
        }
        return listOfInstallmentsValidator;
    }

    public static InstallmentRulesValidator locateInstallmentRulesValidator() {
        if (installmentRulesValidator == null) {
            installmentRulesValidator = new InstallmentRulesValidatorImpl();
        }
        return installmentRulesValidator;
    }

    public static InstallmentsValidator locateInstallmentsValidator() {
        if (installmentsValidator == null) {
            installmentsValidator = new InstallmentsValidatorImpl(locateInstallmentFormatValidator(), locateListOfInstallmentsValidator(), locateInstallmentRulesValidator());
        }
        return installmentsValidator;
    }
}