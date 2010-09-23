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

package org.mifos.test.acceptance.loanproduct;


import org.mifos.framework.util.DbUnitUtilities;
import org.mifos.test.acceptance.framework.AppLauncher;
import org.mifos.test.acceptance.framework.MifosPage;
import org.mifos.test.acceptance.framework.UiTestCaseBase;
import org.mifos.test.acceptance.framework.admin.AdminPage;
import org.mifos.test.acceptance.framework.loanproduct.*;
import org.mifos.test.acceptance.framework.loanproduct.DefineNewLoanProductPage.SubmitFormParameters;
import org.mifos.test.acceptance.framework.testhelpers.FormParametersHelper;
import org.mifos.test.acceptance.remote.InitializeApplicationRemoteTestingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


@ContextConfiguration(locations={"classpath:ui-test-context.xml"})
@Test(sequential=true, groups={"smoke","loanproduct","acceptance"})
public class DefineNewLoanProductTest extends UiTestCaseBase {

    private AppLauncher appLauncher;

    @Autowired
    private DriverManagerDataSource dataSource;
    @Autowired
    private DbUnitUtilities dbUnitUtilities;
    @Autowired
    private InitializeApplicationRemoteTestingService initRemote;


    @SuppressWarnings("PMD.SignatureDeclareThrowsException") // one of the dependent methods throws Exception
    @BeforeMethod
    @Override
    public void setUp() throws Exception {
        super.setUp();
        appLauncher = new AppLauncher(selenium);
        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, "acceptance_small_003_dbunit.xml.zip", dataSource, selenium);
    }

    @AfterMethod
    public void logOut() {
        (new MifosPage(selenium)).logout();
    }
    
    @SuppressWarnings("PMD.SignatureDeclareThrowsException") // one of the dependent methods throws Exception
    public void createWeeklyLoanProduct()throws Exception {
        SubmitFormParameters formParameters = FormParametersHelper.getWeeklyLoanProductParameters();
        AdminPage adminPage = loginAndNavigateToAdminPage();
        adminPage.verifyPage();
        adminPage.defineLoanProduct(formParameters);

    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException") // one of the dependent methods throws Exception
    public void createMonthlyLoanProduct()throws Exception {
        SubmitFormParameters formParameters = FormParametersHelper.getMonthlyLoanProductParameters();
        AdminPage adminPage = loginAndNavigateToAdminPage();
        adminPage.verifyPage();
        adminPage.defineLoanProduct(formParameters);
    }



    @SuppressWarnings("PMD.SignatureDeclareThrowsException") // one of the dependent methods throws Exception
    public void verifyVariableInstalment()throws Exception {
        verifyVariableInstalmentsInNewLoanProduct();
        verifyVariableInstalmentsInEditLoanProduct();
    }

    private void verifyVariableInstalmentsInNewLoanProduct(){
        AdminPage adminPage = loginAndNavigateToAdminPage();
        adminPage.verifyPage();
        DefineNewLoanProductPage defineLoanProductPage = adminPage.navigateToDefineLoanProduct();
        defineLoanProductPage.verifyVariableInstalmentOptionsDefaults();
        defineLoanProductPage.verifyMinimumAndMaximumInstalmentGapFields();
        defineLoanProductPage.verifyMinimumVariableInstalmentAmountField();
        SubmitFormParameters formParameters = FormParametersHelper.getMonthlyLoanProductParameters();
        defineLoanProductPage.fillLoanParameters(formParameters);
        defineLoanProductPage.fillVariableInstalmentOption("60","1","100");
        DefineNewLoanProductPreviewPage productPreviewPage = defineLoanProductPage.submitAndGotoNewLoanProductPreviewPage();
        productPreviewPage.verifyVariableInstalmentOption("60","1","100");
        DefineNewLoanProductConfirmationPage loanProductConfirmationPage = productPreviewPage.submit();
        loanProductConfirmationPage.verifyVariableInstalmentOption("60","1","100");
    }

    private void verifyVariableInstalmentsInEditLoanProduct() {
        AdminPage adminPage = loginAndNavigateToAdminPage();
        ViewLoanProductsPage viewLoanProductsPage = adminPage.navigateToViewLoanProducts();
        LoanProductDetailsPage detailsPage = viewLoanProductsPage.viewLoanProductDetails("Educational");
        EditLoanProductPage editProductPage = detailsPage.editLoanProduct();
        editProductPage.verifyVariableInstalmentOptionsDefaults();
        editProductPage.verifyMinimumAndMaximumInstalmentGapFields();
        editProductPage.verifyMinimumVariableInstalmentAmountField();
        EditLoanProductPreviewPage editProductPreviewPage = editProductPage.submitVariableInstalmentChange("60", "1", "100");
        editProductPreviewPage.verifyVariableInstalmentOption("60","1","100");
        editProductPreviewPage.submit();
        detailsPage.verifyVariableInstalmentOption("60","1","100");
    }

    private AdminPage loginAndNavigateToAdminPage() {
        return appLauncher
         .launchMifos()
         .loginSuccessfullyUsingDefaultCredentials()
         .navigateToAdminPage();
     }

}

