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

import org.joda.time.LocalDate;

public class InterestCalculationRange {

    private final LocalDate lowerDate;
    private final LocalDate upperDate;

    public InterestCalculationRange(LocalDate lowerDate, LocalDate upperDate) {
        this.lowerDate = lowerDate;
        this.upperDate = upperDate;
    }

    public LocalDate getLowerDate() {
        return this.lowerDate;
    }

    public LocalDate getUpperDate() {
        return this.upperDate;
    }

    public boolean dateFallsWithin(LocalDate date) {
        return ((date.isAfter(this.lowerDate) || date.isEqual(this.lowerDate)) &&
                (date.isBefore(this.upperDate) || date.isEqual(this.upperDate)));
    }

}