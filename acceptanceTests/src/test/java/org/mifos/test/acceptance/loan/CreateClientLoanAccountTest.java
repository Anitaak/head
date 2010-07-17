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

package org.mifos.test.acceptance.loan;

import org.mifos.framework.util.DbUnitUtilities;
import org.mifos.test.acceptance.framework.HomePage;
import org.mifos.test.acceptance.framework.MifosPage;
import org.mifos.test.acceptance.framework.UiTestCaseBase;
import org.mifos.test.acceptance.framework.loan.CreateLoanAccountEntryPage;
import org.mifos.test.acceptance.framework.loan.CreateLoanAccountSearchParameters;
import org.mifos.test.acceptance.framework.loan.CreateLoanAccountSubmitParameters;
import org.mifos.test.acceptance.framework.loan.LoanAccountPage;
import org.mifos.test.acceptance.framework.loan.EditLoanAccountInformationParameters;
import org.mifos.test.acceptance.framework.loan.EditPreviewLoanAccountPage;
import org.mifos.test.acceptance.framework.testhelpers.LoanTestHelper;
import org.mifos.test.acceptance.remote.InitializeApplicationRemoteTestingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("PMD")
@ContextConfiguration(locations = { "classpath:ui-test-context.xml" })
@Test(sequential = true, groups = {"loan","acceptance","ui"})
public class CreateClientLoanAccountTest extends UiTestCaseBase {

    private LoanTestHelper loanTestHelper;

    @Autowired
    private DriverManagerDataSource dataSource;
    @Autowired
    private DbUnitUtilities dbUnitUtilities;
    @Autowired
    private InitializeApplicationRemoteTestingService initRemote;

    @Override
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    // one of the dependent methods throws Exception
    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        super.setUp();
        loanTestHelper = new LoanTestHelper(selenium);
    }

    @AfterMethod(alwaysRun = true)
    public void logOut() {
        (new MifosPage(selenium)).logout();
    }

//    @Test(sequential = true, groups = {"smoke"})
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    // one of the dependent methods throws Exception
    private void newWeeklyClientLoanAccount() throws Exception {

        // FIXME - keithw - do manual test to verify is not broken and rewrite test

        CreateLoanAccountSearchParameters searchParameters = new CreateLoanAccountSearchParameters();
        searchParameters.setSearchString("Client - Veronica Abisya");
        searchParameters.setLoanProduct("Flat Interest Loan Product With Fee");

        CreateLoanAccountSubmitParameters submitAccountParameters = new CreateLoanAccountSubmitParameters();
        submitAccountParameters.setAmount("1012.0");
        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, "acceptance_small_001_dbunit.xml.zip", dataSource, selenium);

        createLoanAndCheckAmount(searchParameters, submitAccountParameters);
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    // one of the dependent methods throws Exception
    public void newWeeklyClientLoanAccountWithModifyErrors() throws Exception {
        CreateLoanAccountSearchParameters searchParameters = new CreateLoanAccountSearchParameters();
        searchParameters.setSearchString("Client - Veronica Abisya");
        searchParameters.setLoanProduct("Flat Interest Loan Product With Fee");

        CreateLoanAccountSubmitParameters submitAccountParameters = new CreateLoanAccountSubmitParameters();
        submitAccountParameters.setAmount("1012.0");
        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, "acceptance_small_001_dbunit.xml.zip", dataSource, selenium);

        String loanId = createLoanAndCheckAmount(searchParameters, submitAccountParameters);
        submitAccountParameters.setAmount("10666.0");
        EditLoanAccountInformationParameters editAccountParameters = new EditLoanAccountInformationParameters();
        editAccountParameters.setGracePeriod("15");
        EditPreviewLoanAccountPage editPreviewLoanAccountPage = tryToEditLoan(loanId, submitAccountParameters, editAccountParameters);
        editPreviewLoanAccountPage.verifyErrorInForm("Please specify valid Amount. Amount should be a value between 1 and 10,000, inclusive");
        editPreviewLoanAccountPage.verifyErrorInForm("Please specify valid Grace period for repayments. Grace period for repayments should be a value less than 12");
    }

    @Test(sequential = true, groups = {"loan","acceptance","ui"})
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void newWeeklyClientLoanAccountWithDateTypeCustomField() throws Exception {
        CreateLoanAccountSearchParameters searchParameters = new CreateLoanAccountSearchParameters();
        searchParameters.setSearchString("Client - Veronica Abisya");
        searchParameters.setLoanProduct("Flat Interest Loan Product With Fee");

        CreateLoanAccountSubmitParameters submitAccountParameters = new CreateLoanAccountSubmitParameters();
        submitAccountParameters.setAmount("1012.0");
        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, "acceptance_small_014_dbunit.xml.zip", dataSource, selenium);

        String loanId = createLoanAndCheckAmount(searchParameters, submitAccountParameters);

        submitAccountParameters.setAmount("1666.0");
        EditLoanAccountInformationParameters editAccountParameters = new EditLoanAccountInformationParameters();
        editAccountParameters.setGracePeriod("5");
        EditPreviewLoanAccountPage editPreviewLoanAccountPage = tryToEditLoan(loanId, submitAccountParameters, editAccountParameters);
        LoanAccountPage loanAccountPage = editPreviewLoanAccountPage.submitAndNavigateToLoanAccountPage();
        loanAccountPage.verifyPage();
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    // one of the dependent methods throws Exception
    public void newMonthlyClientLoanAccountWithMeetingOnSpecificDayOfMonth() throws Exception {
        CreateLoanAccountSearchParameters searchParameters = new CreateLoanAccountSearchParameters();
        searchParameters.setSearchString("Client - Mary Monthly");
        searchParameters.setLoanProduct("MonthlyClientFlatLoan1stOfMonth");

        CreateLoanAccountSubmitParameters submitAccountParameters = new CreateLoanAccountSubmitParameters();
        submitAccountParameters.setAmount("1234.0");
        submitAccountParameters.setGracePeriodTypeNone(true);
        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, "acceptance_small_004_dbunit.xml.zip", dataSource, selenium);

        createLoanAndCheckAmount(searchParameters, submitAccountParameters);
    }

    @Test(sequential = true, groups = {"loan","acceptance","ui"})
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    // one of the dependent methods throws Exception
    public void newMonthlyClientLoanAccountWithMeetingOnSameWeekAndWeekdayOfMonth() throws Exception {
        CreateLoanAccountSearchParameters searchParameters = new CreateLoanAccountSearchParameters();
        searchParameters.setSearchString("Client - Mia Monthly3rdFriday");
        searchParameters.setLoanProduct("MonthlyClientFlatLoanThirdFridayOfMonth");

        CreateLoanAccountSubmitParameters submitAccountParameters = new CreateLoanAccountSubmitParameters();
        submitAccountParameters.setAmount("2765.0");
        submitAccountParameters.setGracePeriodTypeNone(true);
        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, "acceptance_small_004_dbunit.xml.zip", dataSource, selenium);

        createLoanAndCheckAmount(searchParameters, submitAccountParameters);
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    // one of the dependent methods throws Exception
    @Test(sequential = true, groups = {"loan","acceptance","ui"})
    public void newMonthlyClientLoanAccountWithZeroInterestRate() throws Exception {
        CreateLoanAccountSearchParameters searchParameters = new CreateLoanAccountSearchParameters();
        searchParameters.setSearchString("Client - Tesa Mendez");
        searchParameters.setLoanProduct("MIFOS-2636-GKEmergencyLoanWithZeroInterest");

        CreateLoanAccountSubmitParameters submitAccountParameters = new CreateLoanAccountSubmitParameters();
        submitAccountParameters.setAmount("1000");
        submitAccountParameters.setGracePeriodTypeNone(true);
        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, "acceptance_small_010_dbunit.xml.zip", dataSource, selenium);

        createLoanAndCheckAmount(searchParameters, submitAccountParameters);
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    // one of the dependent methods throws Exception
    public void tryClientLoanAccountWithAdditionalFees() throws Exception {
        CreateLoanAccountSearchParameters searchParameters = new CreateLoanAccountSearchParameters();
        searchParameters.setSearchString("Client - Tesa Mendez");
        searchParameters.setLoanProduct("MIFOS-2636-GKEmergencyLoanWithZeroInterest");

        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, "acceptance_small_010_dbunit.xml.zip", dataSource, selenium);

        CreateLoanAccountEntryPage loanAccountEntryPage = loanTestHelper.navigateToCreateLoanAccountEntryPage(searchParameters);

        loanAccountEntryPage.selectAdditionalFees();
        loanAccountEntryPage.clickContinue();

        HomePage homePage = loanAccountEntryPage.navigateToHomePage();
        homePage.verifyPage();
        loanAccountEntryPage = loanTestHelper.navigateToCreateLoanAccountEntryPageWithoutLogout(homePage, searchParameters);
        loanAccountEntryPage.verifyAdditionalFeesAreEmpty();
    }

    private String createLoanAndCheckAmount(CreateLoanAccountSearchParameters searchParameters,
            CreateLoanAccountSubmitParameters submitAccountParameters) {

        LoanAccountPage loanAccountPage = loanTestHelper.createLoanAccount(searchParameters, submitAccountParameters);
        loanAccountPage.verifyPage();
        loanAccountPage.verifyLoanAmount(submitAccountParameters.getAmount());
        return loanAccountPage.getAccountId();
    }

    private EditPreviewLoanAccountPage tryToEditLoan(String loanId, CreateLoanAccountSubmitParameters submitAccountParameters, EditLoanAccountInformationParameters editAccountParameters) {
        return loanTestHelper.changeLoanAccountInformation(loanId, submitAccountParameters, editAccountParameters);
    }
}
