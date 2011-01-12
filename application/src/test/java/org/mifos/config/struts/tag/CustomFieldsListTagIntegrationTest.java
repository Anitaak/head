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

package org.mifos.config.struts.tag;

import static org.mifos.framework.TestUtils.assertWellFormedFragment;
import junit.framework.Assert;
import junitx.framework.StringAssert;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mifos.application.master.business.CustomFieldDefinitionEntity;
import org.mifos.application.master.persistence.MasterPersistence;
import org.mifos.application.servicefacade.ApplicationContextProvider;
import org.mifos.application.util.helpers.EntityType;
import org.mifos.framework.MifosIntegrationTestCase;
import org.mifos.framework.TestUtils;
import org.mifos.framework.struts.tags.XmlBuilder;
import org.mifos.security.util.UserContext;

public class CustomFieldsListTagIntegrationTest extends MifosIntegrationTestCase {

    private UserContext userContext;

    @Before
    public void setUp() throws Exception {
        userContext = TestUtils.makeUser();
    }

    @Test
    public void testGetListRow() throws Exception {
        String categoryName = "Personnel";
        CustomFieldsListTag tag = new CustomFieldsListTag("action", "method", "flow", categoryName, categoryName);
        CustomFieldDefinitionEntity customField = ApplicationContextProvider.getBean(MasterPersistence.class)
                .retrieveCustomFieldsDefinition(EntityType.LOAN).get(0);
        XmlBuilder link = tag.getRow(customField, userContext, 1);
        String sequenceNum = "1";
        String label = "External Loan Id";
        String dataType = "Text";
        String defaultValue = "\u00a0";
        String mandatory = "No";
        String fieldId = "7";

       Assert.assertEquals("<tr>\n" + "<td width=\"11%\" class=\"drawtablerow\">" + sequenceNum + "</td>\n"
                + "<td width=\"22%\" class=\"drawtablerow\">" + label + "</td>\n"
                + "<td width=\"21%\" class=\"drawtablerow\">" + dataType + "</td>\n"
                + "<td width=\"21%\" class=\"drawtablerow\">" + defaultValue + "</td>\n"
                + "<td width=\"17%\" class=\"drawtablerow\">" + mandatory + "</td>\n"
                + "</tr>\n", link.getOutput());
    }

    @Ignore
    @Test
    public void testGetCustomFieldsList() throws Exception {
        String categoryName = "Personnel";
        CustomFieldsListTag tag = new CustomFieldsListTag("action", "method", "flow", categoryName, categoryName);
        String html = tag.getCustomFieldsList(userContext);

        assertWellFormedFragment(html);

        StringAssert.assertContains("External Id", html);
    }

}
