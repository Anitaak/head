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

package org.mifos.application.holiday.persistence;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.mifos.application.admin.servicefacade.HolidayServiceFacade;
import org.mifos.application.holiday.business.HolidayBO;
import org.mifos.application.holiday.business.service.HolidayService;
import org.mifos.application.holiday.util.helpers.RepaymentRuleTypes;
import org.mifos.dto.domain.HolidayDetails;
import org.mifos.dto.domain.OfficeHoliday;

public class HolidayServiceFacadeWebTier implements HolidayServiceFacade {

    private final HolidayService holidayService;
    private final HolidayDao holidayDao;

    public HolidayServiceFacadeWebTier(HolidayService holidayService, HolidayDao holidayDao) {
        this.holidayService = holidayService;
        this.holidayDao = holidayDao;
    }

    @Override
    public void createHoliday(HolidayDetails holidayDetails, List<Short> officeIds) {

        this.holidayService.create(holidayDetails, officeIds);
    }

    @Override
    public Map<String, List<OfficeHoliday>> holidaysByYear() {

        List<HolidayBO> holidays = this.holidayDao.findAllHolidays();

        Map<String, List<OfficeHoliday>> holidaysByYear = new TreeMap<String, List<OfficeHoliday>>();
        for (HolidayBO holiday : holidays) {
            HolidayDetails holidayDetail = new HolidayDetails(holiday.getHolidayName(), holiday.getHolidayFromDate(), holiday
                    .getHolidayThruDate(), holiday.getRepaymentRuleType().getValue());
            holidayDetail.setRepaymentRuleName(holiday.getRepaymentRuleType().getName());

            int year = holiday.getThruDate().getYear();
            List<OfficeHoliday> holidaysInYear = holidaysByYear.get(Integer.toString(year));
            if (holidaysInYear == null) {
                holidaysInYear = new LinkedList<OfficeHoliday>();
            }
            holidaysInYear.add(new OfficeHoliday(holidayDetail, this.holidayDao.applicableOffices(holiday.getId())));
            holidaysByYear.put(Integer.toString(year), holidaysInYear);
        }
        return holidaysByYear;
    }

    @Override
    public OfficeHoliday retrieveHolidayDetailsForPreview(HolidayDetails holidayDetail, List<Short> officeIds) {
        holidayDetail.setRepaymentRuleName(RepaymentRuleTypes.fromInt(holidayDetail.getRepaymentRuleType().intValue()).getName());

        List<String> officeNames = this.holidayDao.retrieveApplicableOfficeNames(officeIds);

        return new OfficeHoliday(holidayDetail, officeNames);
    }
}