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

package org.mifos.config.util.helpers;

import junit.framework.Assert;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mifos.application.master.business.CustomFieldDefinitionEntity;
import org.mifos.application.master.business.CustomFieldType;
import org.mifos.application.util.helpers.EntityType;
import org.mifos.application.util.helpers.YesNoFlag;
import org.mifos.config.business.MifosConfiguration;
import org.mifos.config.persistence.ApplicationConfigurationPersistence;
import org.mifos.customers.api.CustomerLevel;
import org.mifos.customers.business.CustomerCustomFieldEntity;
import org.mifos.customers.client.business.ClientBO;
import org.mifos.customers.util.helpers.CustomerStatus;
import org.mifos.framework.MifosIntegrationTestCase;
import org.mifos.framework.hibernate.helper.StaticHibernateUtil;
import org.mifos.framework.util.helpers.TestObjectFactory;

public class CustomFieldsBackfillerIntegrationTest extends MifosIntegrationTestCase {

    // reused by unit tests
    ClientBO client;
    CustomFieldDefinitionEntity customField;
    CustomerCustomFieldEntity ccfe;
    // constants for creating reusable objects
    private static final String FAVORITE_COLOR = "Green";
    private static final String CUSTOM_FIELD_LABEL = "Favorite Color";
    private static final String CUSTOM_FIELD_LABEL2 = "Lucky Number";


    @Before
    public void setUp() throws Exception {
        // create client
        client = TestObjectFactory.createClient("Joe Client", null, CustomerStatus.CLIENT_PARTIAL);
    }

    @After
    public void tearDown() throws Exception {
        Session session = StaticHibernateUtil.getSessionTL();

        // Clean up custom field record and association with client.
        // Don't do this in a test method (like testExistingClientGetsNewField)
        // because it may not be executed... if an assert fails, for instance,
        // we'll jump out of the method before cleanup can occur.
        session.delete(ccfe);
        session.delete(customField);

        // clean up customer/client
        client = null;
    }

    private void createCustomField() throws Exception {
        YesNoFlag mandatory = YesNoFlag.YES;
        customField = new CustomFieldDefinitionEntity(CUSTOM_FIELD_LABEL, CustomerLevel.CLIENT.getValue(),
                CustomFieldType.ALPHA_NUMERIC, EntityType.CLIENT, FAVORITE_COLOR, mandatory);
        Assert.assertNotNull(customField);
        ApplicationConfigurationPersistence persistence = new ApplicationConfigurationPersistence();
        persistence.addCustomField(customField);
        Short englishLocale = new Short("1");
        MifosConfiguration.getInstance().updateLabelKey(customField.getLookUpEntity().getEntityType(),
                CUSTOM_FIELD_LABEL, englishLocale);
    }

    private void createNonMandatoryCustomFieldWithoutDefault() throws Exception {
        YesNoFlag mandatory = YesNoFlag.NO;
        customField = new CustomFieldDefinitionEntity(CUSTOM_FIELD_LABEL2, CustomerLevel.CLIENT.getValue(),
                CustomFieldType.NUMERIC, EntityType.CLIENT, "", mandatory);
        Assert.assertNotNull(customField);
        ApplicationConfigurationPersistence persistence = new ApplicationConfigurationPersistence();
        persistence.addCustomField(customField);
        Short englishLocale = new Short("1");
        MifosConfiguration.getInstance().updateLabelKey(customField.getLookUpEntity().getEntityType(),
                CUSTOM_FIELD_LABEL, englishLocale);
    }

    /**
     * Ensure a newly added field is also added to an existing client.
     */
    @Test
    public void testExistingClientGetsNewField() throws Exception {
        createCustomField();
       Assert.assertEquals(CUSTOM_FIELD_LABEL, customField.getLabel());
        CustomFieldsBackfiller cfb = new CustomFieldsBackfiller();
        // do the actual backfill
        cfb.addCustomFieldsForExistingRecords(EntityType.CLIENT, CustomerLevel.CLIENT.getValue(), customField);

        Session session = StaticHibernateUtil.getSessionTL();

        // make sure record was added that joins the custom field with the
        // customer that existed before the custom field was added
        Query query = session.createQuery("from org.mifos.customers.business.CustomerCustomFieldEntity "
                + "where fieldId=:fieldId and customer=:customerId");
        query.setInteger("fieldId", customField.getFieldId());
        query.setInteger("customerId", client.getCustomerId());
        ccfe = (CustomerCustomFieldEntity) query.list().get(0);
        Assert.assertNotNull(ccfe);
       Assert.assertEquals(FAVORITE_COLOR, ccfe.getFieldValue());
    }

    /**
     * Ensure a non-mandatory newly added field (without a default value) is
     * also added to an existing client.
     */
    @Test
    public void testExistingClientGetsNewNonmandatoryFieldWithoutDefault() throws Exception {
        createNonMandatoryCustomFieldWithoutDefault();
       Assert.assertEquals(CUSTOM_FIELD_LABEL2, customField.getLabel());
        CustomFieldsBackfiller cfb = new CustomFieldsBackfiller();
        // do the actual backfill
        cfb.addCustomFieldsForExistingRecords(EntityType.CLIENT, CustomerLevel.CLIENT.getValue(), customField);

        Session session = StaticHibernateUtil.getSessionTL();

        // make sure record was added that joins the custom field with the
        // customer that existed before the custom field was added
        Query query = session.createQuery("from org.mifos.customers.business.CustomerCustomFieldEntity "
                + "where fieldId=:fieldId and customer=:customerId");
        query.setInteger("fieldId", customField.getFieldId());
        query.setInteger("customerId", client.getCustomerId());
        ccfe = (CustomerCustomFieldEntity) query.list().get(0);
        Assert.assertNotNull(ccfe);
        Assert.assertTrue(StringUtils.isBlank(ccfe.getFieldValue()));
    }

    // TODO: add additional tests for group, center, office, personnel, loan,
    // savings.
}
