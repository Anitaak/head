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

import org.mifos.customers.business.CustomerBO;
import org.mifos.dto.domain.ApplicableAccountFeeDto;
import org.mifos.dto.domain.CustomFieldDto;
import org.mifos.dto.domain.PersonnelDto;

public class GroupFormCreationDto {

    private final CustomerBO parentCustomer;
    private final boolean centerHierarchyExists;
    private final Short groupOfficeId;
    private final List<CustomFieldDto> customFieldDtos;
    private final List<PersonnelDto> personnelList;
    private final List<PersonnelDto> formedByPersonnel;
    private List<ApplicableAccountFeeDto> defaultFees;
    private List<ApplicableAccountFeeDto> additionalFees;

    public GroupFormCreationDto(boolean centerHierarchyExists, CustomerBO parentCustomer,
            Short groupOfficeId, List<CustomFieldDto> customFieldDtos, List<PersonnelDto> personnelList,
            List<PersonnelDto> formedByPersonnel, List<ApplicableAccountFeeDto> defaultFees, List<ApplicableAccountFeeDto> additionalFees) {
        this.centerHierarchyExists = centerHierarchyExists;
        this.parentCustomer = parentCustomer;
        this.groupOfficeId = groupOfficeId;
        this.customFieldDtos = customFieldDtos;
        this.personnelList = personnelList;
        this.formedByPersonnel = formedByPersonnel;
        this.defaultFees = defaultFees;
        this.additionalFees = additionalFees;
    }

    public List<PersonnelDto> getFormedByPersonnel() {
        return this.formedByPersonnel;
    }

    public List<PersonnelDto> getPersonnelList() {
        return this.personnelList;
    }

    public List<CustomFieldDto> getCustomFieldViews() {
        return this.customFieldDtos;
    }

    public boolean isCenterHierarchyExists() {
        return this.centerHierarchyExists;
    }

    public CustomerBO getParentCustomer() {
        return this.parentCustomer;
    }

    public List<ApplicableAccountFeeDto> getDefaultFees() {
        return this.defaultFees;
    }

    public List<ApplicableAccountFeeDto> getAdditionalFees() {
        return this.additionalFees;
    }

    public String getParentOfficeId() {
        String parentOfficeId = groupOfficeId.toString();
        if (this.getParentCustomer() != null) {
            parentOfficeId = this.parentCustomer.getOffice().getOfficeId().toString();
        }
        return parentOfficeId;
    }

    public String getParentPersonnelId() {
        String parentPersonnelId = "";
        if (this.getParentCustomer() != null) {
            parentPersonnelId = this.parentCustomer.getPersonnel().getPersonnelId().toString();
        }
        return parentPersonnelId;
    }
}