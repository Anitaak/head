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

package org.mifos.accounts.savings.interest;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.Days;
import org.mifos.framework.util.helpers.Money;

/**
 * I represent a valid Interest Calculation Period.
 *
 * In mifos, savings interest calculation is to occur every x days/months.
 */
public class InterestCalculationPeriodDetail {

    private final InterestCalculationInterval interval;
    private final List<EndOfDayDetail> dailyDetails;
    private final Money balanceBeforeInterval;

    /**
     * I am responsible for ensuring a {@link InterestCalculationPeriodDetail} is populated with correct
     * {@link EndOfDayDetail}'s applicable to given period and with the running balance of the account before this
     * period.
     */
    public static InterestCalculationPeriodDetail populatePeriodDetailBasedOnInterestCalculationInterval(
            InterestCalculationInterval interval, List<EndOfDayDetail> allEndOfDayDetailsForAccount, Money balanceBeforeInterval) {

        Money balance = balanceBeforeInterval;

        List<EndOfDayDetail> applicableDailyDetailsForPeriod = new ArrayList<EndOfDayDetail>();

        for (EndOfDayDetail endOfDayDetail : allEndOfDayDetailsForAccount) {
            if (interval.contains(endOfDayDetail.getDate())) {
                applicableDailyDetailsForPeriod.add(endOfDayDetail);
            }
        }

        return new InterestCalculationPeriodDetail(interval, applicableDailyDetailsForPeriod, balance);
    }

    public InterestCalculationPeriodDetail(InterestCalculationInterval interval, List<EndOfDayDetail> dailyDetails, Money balanceBeforeInterval) {
        this.dailyDetails = dailyDetails;
        this.balanceBeforeInterval = balanceBeforeInterval;
        this.interval = interval;
    }

    public InterestCalculationInterval getInterval() {
        return this.interval;
    }

    public List<EndOfDayDetail> getDailyDetails() {
        return this.dailyDetails;
    }

    public int getDuration() {
        return Days.daysBetween(interval.getStartDate(), interval.getEndDate()).getDays() + 1;
    }

    public Money getBalanceBeforeInterval() {
        return balanceBeforeInterval;
    }

    public Money zeroAmount() {
        return Money.zero(this.balanceBeforeInterval.getCurrency());
    }

    public Money sumOfPrincipal() {
        Money principalForPeriod = zeroAmount();

        for (EndOfDayDetail daysDetail : this.dailyDetails) {
            principalForPeriod = principalForPeriod.add(daysDetail.getResultantAmountForDay());
        }

        return principalForPeriod;
    }

    public Money sumOfInterest() {
        Money interestForPeriod = zeroAmount();

        for (EndOfDayDetail daysDetail : this.dailyDetails) {
            interestForPeriod = interestForPeriod.add(daysDetail.getInterest());
        }

        return interestForPeriod;
    }
}