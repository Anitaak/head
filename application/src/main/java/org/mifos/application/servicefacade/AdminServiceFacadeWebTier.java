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

import java.util.ArrayList;
import java.util.List;

import org.mifos.accounts.productdefinition.business.LoanOfferingBO;
import org.mifos.accounts.productdefinition.business.ProductTypeEntity;
import org.mifos.accounts.productdefinition.business.service.ProductService;
import org.mifos.accounts.productdefinition.persistence.LoanProductDao;
import org.mifos.accounts.productdefinition.persistence.SavingsProductDao;
import org.mifos.application.NamedQueryConstants;
import org.mifos.application.admin.servicefacade.AdminServiceFacade;
import org.mifos.core.MifosRuntimeException;
import org.mifos.dto.screen.LoanProductDto;
import org.mifos.dto.screen.ProductConfigurationDto;

public class AdminServiceFacadeWebTier implements AdminServiceFacade {

    private ProductService productService;
    private LoanProductDao loanProductDao;
    private SavingsProductDao savingsProductDao;

    public AdminServiceFacadeWebTier(ProductService productService, LoanProductDao loanProductDao,
            SavingsProductDao savingsProductDao) {
        this.productService = productService;
        this.loanProductDao = loanProductDao;
        this.savingsProductDao = savingsProductDao;
    }

    @Override
    public ProductConfigurationDto retrieveProductConfiguration() {
        ProductTypeEntity loanProductConfiguration = this.loanProductDao.findLoanProductConfiguration();
        ProductTypeEntity savingsProductConfiguration = this.savingsProductDao.findSavingsProductConfiguration();

        return new ProductConfigurationDto(loanProductConfiguration.getLatenessDays().intValue(),
                savingsProductConfiguration.getDormancyDays().intValue());
    }

    @Override
    public void updateProductConfiguration(ProductConfigurationDto productConfiguration) {

        ProductTypeEntity loanProductConfiguration = this.loanProductDao.findLoanProductConfiguration();
        ProductTypeEntity savingsProductConfiguration = this.savingsProductDao.findSavingsProductConfiguration();

        this.productService.updateLatenessAndDormancy(loanProductConfiguration, savingsProductConfiguration,
                productConfiguration);
    }

    @Override
    public List<LoanProductDto> retrieveLoanProducts() {

        List<Object[]> queryResult = this.loanProductDao.findAllLoanProducts();
        if (queryResult.size() == 0) {
            return null;
        }

        List<LoanProductDto> loanProducts = new ArrayList<LoanProductDto>();
        Short prdOfferingId;
        String prdOfferingName;
        Short prdOfferingStatusId;
        String prdOfferingStatusName;

        for (Object[] loanRow : queryResult) {
            prdOfferingId = (Short) loanRow[0];
            prdOfferingName = (String) loanRow[1];
            prdOfferingStatusId = (Short) loanRow[2];
            prdOfferingStatusName = (String) loanRow[3];
            LoanProductDto loanProduct = new LoanProductDto(prdOfferingId, prdOfferingName, prdOfferingStatusId,
                    prdOfferingStatusName);
            loanProducts.add(loanProduct);
        }
        return loanProducts;

    }
}