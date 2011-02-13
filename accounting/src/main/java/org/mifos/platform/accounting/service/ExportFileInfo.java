/*
 * Copyright Grameen Foundation USA
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

package org.mifos.platform.accounting.service;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class ExportFileInfo {

    private final String lastModified;

    private final String startDate;

    private final String fileName;

    private final String endDate;

    private final Boolean isExistInCache;

    public ExportFileInfo(String lastModified, String fileName, String startDate, String endDate, Boolean isExistInCache) {
        super();
        this.lastModified = lastModified;
        this.fileName = fileName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isExistInCache = isExistInCache;
    }


    public final String getLastModified() {
        return lastModified;
    }

    public final String getFileName() {
        return fileName;
    }

    public final String getStartDate() {
        return startDate;
    }

    public final String getEndDate() {
        return endDate;
    }

    public Boolean getIsExistInCache() {
        return isExistInCache;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
