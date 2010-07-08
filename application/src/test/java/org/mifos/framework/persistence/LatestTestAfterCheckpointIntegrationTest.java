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

package org.mifos.framework.persistence;

import static org.mifos.framework.util.helpers.DatabaseSetup.executeScript;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import junit.framework.Assert;

import org.dbunit.Assertion;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mifos.accounts.financial.util.helpers.FinancialInitializer;
import org.mifos.framework.exceptions.SystemException;
import org.mifos.framework.hibernate.helper.StaticHibernateUtil;

/**
 * This class runs tests on database upgrade scripts (both SQL
 * based and java based).  It uses a version of the database referred to as
 * a "checkpoint" as a starting point.  The database checkpoint version that
 * it starts with can be adjusted by updating sql/latest-schema-checkupoint.sql
 * and sql/latest-data-checkupoint.sql with a pair of the corresponding
 * latest-schema.sql and latest-data.sql files for a given database version.
 * The static variable DatabaseVersionPersistence.LATEST_CHECKPOINT_VERSION
 * must then be set to the database version number of the latest-xxx.sql files
 * that have been used to update the latest-xxx-checkpoint.sql files.
 * This test will run upgrade scripts using LATEST_CHECKPOINT_VERSION
 * as a starting point.  In general LATEST_CHECKPOINT_VERSION should be a
 * database version that is at least 3-5 upgrades ago in order to allow for
 * fixes to be made to recent upgrades when necessary.
 */
public class LatestTestAfterCheckpointIntegrationTest {

    private static Connection connection;
    private static DatabaseConnection dbUnitConnection;

    @BeforeClass
    public static void beforeClass() throws Exception {
        StaticHibernateUtil.initialize();
        connection = StaticHibernateUtil.getSessionTL().connection();
        connection.setAutoCommit(false);
        dbUnitConnection = new DatabaseConnection(connection);
    }

    @Before
    public void setUp() throws Exception {
        dropLatestDatabase();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        // Cleaning the database using FK check disabled connection
        // If any one of the test fails or throws error it could lead to
        // multiple failures in other tests during test build
        TestDatabase.createMySQLTestDatabase();
        FinancialInitializer.initialize();
        StaticHibernateUtil.flushAndCloseSession();
    }

    @Test
    public void testSimple() throws Exception {

        loadLatest();
        IDataSet latestDump = dbUnitConnection.createDataSet();
        connection.createStatement().execute("drop table foo");
        connection.commit();
        applyUpgrades();
        IDataSet upgradeDump = dbUnitConnection.createDataSet();
        Assertion.assertEquals(latestDump, upgradeDump);
    }

    @Test
    public void testRealSchemaFromCheckpoint() throws Exception {
        createLatestDatabaseWithLatestData();

        IDataSet latestDataDump = dbUnitConnection.createDataSet();

        // FIXME for some reason the comparison of DatabaseDataSet (IDataSet) doesn't expose the difference at assert in
        // datasets FlatXmlDataSet seems to work here.
        final File latestDumpFile = File.createTempFile("latestDataDump", ".xml");
        FlatXmlDataSet.write(latestDataDump, new FileOutputStream(latestDumpFile));
        latestDataDump = new FlatXmlDataSet(latestDumpFile);
        latestDumpFile.delete();

        final String latestDumpAsString = TestDatabase.getAllTablesStructureDump();
        dropLatestDatabase();

        createLatestCheckPointDatabaseWithLatestData();
        DatabaseMigrator migrator = new DatabaseMigrator(connection);
        migrator.upgrade();

        String upgradeDump = TestDatabase.getAllTablesStructureDump();

        IDataSet upgradeDataDump = dbUnitConnection.createDataSet();

        final File upgradeDumpFile = File.createTempFile("upgradeDataDump", ".xml");
        FlatXmlDataSet.write(upgradeDataDump, new FileOutputStream(upgradeDumpFile));
        upgradeDataDump = new FlatXmlDataSet(upgradeDumpFile);
        upgradeDumpFile.delete();

        Assert.assertEquals(latestDumpAsString, upgradeDump);
        Assertion.assertEquals(latestDataDump, upgradeDataDump);
    }

    @Test
    public void testDropTables() throws Exception {
        String blankDB = TestDatabase.getAllTablesStructureDump();
        createLatestDatabase();
        dropLatestDatabase();
        String cleanedDB = TestDatabase.getAllTablesStructureDump();
        Assert.assertEquals(blankDB, cleanedDB);
    }

    /**
     * The idea here is to figure out whether we are dropping tables in the
     * right order to deal with foreign keys. I'm not sure we fully succeed,
     * however.
     */
    @Test
    public void testDropTablesWithData() throws Exception {
        String blankDB = TestDatabase.getAllTablesStructureDump();
        createLatestDatabaseWithLatestData();
        dropLatestDatabase();
        String cleanedDB = TestDatabase.getAllTablesStructureDump();
        Assert.assertEquals(blankDB, cleanedDB);
    }

    @Test
    public void testAfterLookupValuesAfterCheckpoint() throws Exception {
        createLatestCheckPointDatabaseWithLatestData();
        int nextLookupId = largestLookupId() + 1;
        connection.createStatement().execute(
                "insert into lookup_value(lookup_id, entity_id, lookup_name) " + "values(" + nextLookupId
                        + ", 19,'TestLookUpName')");
        connection.createStatement().execute(
                "insert into lookup_value_locale(locale_id, lookup_id, lookup_value) " + "values(1," + nextLookupId
                        + ",'Martian')");

        DatabaseMigrator migrator = new DatabaseMigrator(connection);
        migrator.upgrade();
        connection.commit();

        // Assert that custom values have been retained
        ResultSet rs = connection.createStatement().executeQuery(
                "select * from lookup_value where lookup_id=" + nextLookupId);
        rs.next();
        Assert.assertEquals(19, rs.getInt("entity_id"));
        Assert.assertEquals("TestLookUpName", rs.getString("lookup_name"));

        rs = connection.createStatement().executeQuery(
                "select * from lookup_value_locale where lookup_id=" + nextLookupId);
        rs.next();
        Assert.assertEquals(1, rs.getInt("locale_id"));
        Assert.assertEquals("Martian", rs.getString("lookup_value"));
        rs.close();

    }

    private void assertNoHardcodedValues(SqlUpgrade upgrade, int version) throws Exception {
        String[] sqlStatements = SqlExecutor.readFile((InputStream) upgrade.sql().getContent());
        for (String sqlStatement : sqlStatements) {
            Assert.assertTrue("Upgrade " + version + " contains hard-coded lookup values", HardcodedValues
                    .checkLookupValue(sqlStatement));
            Assert.assertTrue("Upgrade " + version + " contains hard-coded lookup value locales", HardcodedValues
                    .checkLookupValueLocale(sqlStatement));
        }
    }

    private int largestLookupId() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet results = statement.executeQuery("select max(lookup_id) from lookup_value");
        if (!results.next()) {
            throw new SystemException(SystemException.DEFAULT_KEY,
                    "Did not find an existing lookup_id in lookup_value table");
        }
        int largestLookupId = results.getInt(1);
        results.close();
        statement.close();
        return largestLookupId;
    }

    private void dropLatestDatabase() throws Exception {
        TestDatabase.dropMySQLDatabase();
    }

    private void applyUpgrades() throws Exception {
        connection.createStatement().execute("create table foo(x integer)");
        connection.createStatement().execute("insert into foo(x) values(5)");
        connection.createStatement().execute("alter table foo add column y integer default 7");
        connection.commit();
    }

    private void loadLatest() throws Exception {
        connection.createStatement().execute("drop table if exists foo");
        connection.commit();
        connection.createStatement().execute("create table foo(x integer, y integer default 7)");
        connection.createStatement().execute("insert into foo(x, y) values(5,7)");
        connection.commit();
    }

    private void createLatestDatabaseWithLatestData() throws Exception {
        executeScript("mifosdroptables.sql", connection);
        executeScript("latest-schema.sql", connection);
        executeScript("latest-data.sql", connection);
        connection.commit();
    }

    private void createLatestDatabase() throws Exception {
        executeScript("latest-schema.sql", connection);
        connection.commit();
    }

    private void createLatestCheckPointDatabaseWithLatestData() throws Exception {
        executeScript("mifosdroptables-checkpoint.sql", connection);
        executeScript("latest-schema-checkpoint.sql", connection);
        executeScript("latest-data-checkpoint.sql", connection);
        connection.commit();
    }
}
