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

package org.mifos.framework;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.mifos.accounts.financial.util.helpers.FinancialInitializer;
import org.mifos.application.admin.system.ShutdownManager;
import org.mifos.config.AccountingRules;
import org.mifos.config.ClientRules;
import org.mifos.config.Localization;
import org.mifos.config.ProcessFlowRules;
import org.mifos.config.business.Configuration;
import org.mifos.config.business.MifosConfiguration;
import org.mifos.config.persistence.ConfigurationPersistence;
import org.mifos.framework.components.audit.util.helpers.AuditConfigurtion;
import org.mifos.framework.components.batchjobs.MifosScheduler;
import org.mifos.framework.components.batchjobs.exceptions.BatchJobException;
import org.mifos.framework.components.batchjobs.helpers.UpdateCustomerFeesHelper;
import org.mifos.framework.components.batchjobs.helpers.UpdateCustomerFeesTask;
import org.mifos.framework.components.logger.LoggerConstants;
import org.mifos.framework.components.logger.MifosLogManager;
import org.mifos.framework.components.logger.MifosLogger;
import org.mifos.framework.exceptions.AppNotConfiguredException;
import org.mifos.framework.exceptions.ApplicationException;
import org.mifos.framework.exceptions.HibernateProcessException;
import org.mifos.framework.exceptions.HibernateStartUpException;
import org.mifos.framework.exceptions.PersistenceException;
import org.mifos.framework.exceptions.SystemException;
import org.mifos.framework.exceptions.XMLReaderException;
import org.mifos.framework.hibernate.helper.StaticHibernateUtil;
import org.mifos.framework.persistence.DatabaseVersionPersistence;
import org.mifos.framework.struts.plugin.helper.EntityMasterData;
import org.mifos.framework.struts.tags.XmlBuilder;
import org.mifos.framework.util.StandardTestingService;
import org.mifos.framework.util.helpers.Money;
import org.mifos.framework.util.helpers.MoneyCompositeUserType;
import org.mifos.security.authorization.AuthorizationManager;
import org.mifos.security.authorization.HierarchyManager;
import org.mifos.security.util.ActivityMapper;

/**
 * This class should prepare all the sub-systems that are required by the app. Cleanup should also happen here when the
 * application is shutdown.
 */
public class ApplicationInitializer implements ServletContextListener, ServletRequestListener, HttpSessionListener {

    private static MifosLogger LOG = null;

    private static class DatabaseError {
        boolean isError = false;
        DatabaseErrorCode errorCode = DatabaseErrorCode.NO_DATABASE_ERROR;
        String errmsg = "";
        Throwable error = null;

        void logError() {
            LOG.fatal(errmsg, false, null, error);
        }
    }

    public static void setDatabaseError(DatabaseErrorCode errcode, String errmsg, Throwable error) {
        databaseError.isError = true;
        databaseError.errorCode = errcode;
        databaseError.error = error;
        databaseError.errmsg = errmsg;
    }

    public static void clearDatabaseError() {
        databaseError.isError = false;
        databaseError.errorCode = DatabaseErrorCode.NO_DATABASE_ERROR;
        databaseError.error = null;
        databaseError.errmsg = null;
    }

    private static String getDatabaseConnectionInfo() {
        StandardTestingService standardTestingService = new StandardTestingService();
        Properties hibernateCfg = new Properties();
        String info = "Using Mifos database connection settings";
        try {
            hibernateCfg = standardTestingService.getDatabaseConnectionSettings();
            info += " from file(s): " + Arrays.toString(standardTestingService.getAllSettingsFilenames());
        } catch (IOException e) {
            /*
             * not sure if we can actually do anything useful with this exception since we're likely running during
             * container initialization
             */
            e.printStackTrace();
        }
        info += " Connection URL=" + hibernateCfg.getProperty("hibernate.connection.url");
        info += ". Username=" + hibernateCfg.getProperty("hibernate.connection.username");
        info += ". Password=********";
        return info;
    }

    private static DatabaseError databaseError = new DatabaseError();

    public void contextInitialized(ServletContextEvent ctx) {
        init(ctx);
    }

    public void init(ServletContextEvent ctx) {
        try {
            synchronized (ApplicationInitializer.class) {

                /*
                 * If we do not call MifosLogManager as first step of initialization
                 * MifosLogManager.loggerRepository will be null.
                 */
                LOG = MifosLogManager.getLogger(LoggerConstants.FRAMEWORKLOGGER);
                LOG.info("Logger has been initialised", false, null);

                initializeHibernate();

                LOG.info(getDatabaseConnectionInfo(), false, null);

                // if a database upgrade loads an instance of Money then MoneyCompositeUserType needs the default currency
                MoneyCompositeUserType.setDefaultCurrency(AccountingRules.getMifosCurrency(new ConfigurationPersistence()));
                AccountingRules.init(); // load the additional currencies
                DatabaseVersionPersistence persistence = new DatabaseVersionPersistence();
                try {
                    /*
                     * This is an easy way to force an actual database query to happen via Hibernate. Simply opening a
                     * Hibernate session may not actually connect to the database.
                     */
                    persistence.isVersioned();
                } catch (Throwable t) {
                    setDatabaseError(DatabaseErrorCode.CONNECTION_FAILURE, "Unable to connect to database.", t);
                }

                if (!databaseError.isError) {
                    try {
                        persistence.upgradeDatabase();
                    } catch (Throwable t) {
                        setDatabaseError(DatabaseErrorCode.UPGRADE_FAILURE, "Failed to upgrade database.", t);
                    }
                }

                if (databaseError.isError) {
                    databaseError.logError();
                } else {
                    // this method is called so that supported locales will be
                    // loaded
                    // from db and stored in cache for later use
                    Localization.getInstance().init();
                    // Check ClientRules configuration in db and config file(s)
                    // for errors. Also caches ClientRules values.
                    ClientRules.init();
                    // Check ProcessFlowRules configuration in db and config
                    // file(s) for errors.
                    ProcessFlowRules.init();
                    initializeSecurity();

                    Money.setDefaultCurrency(AccountingRules.getMifosCurrency(new ConfigurationPersistence()));

                    FinancialInitializer.initialize();
                    EntityMasterData.getInstance().init();
                    initializeEntityMaster();

                    cleanUpRecurringFeesForMifos3712();

                    // FIXME: replace with Spring-managed beans
                    final MifosScheduler mifosScheduler = new MifosScheduler();
                    mifosScheduler.registerTasks();
                    final ShutdownManager shutdownManager = new ShutdownManager();
                    if (null != ctx) {
                        ctx.getServletContext().setAttribute(MifosScheduler.class.getName(), mifosScheduler);
                        ctx.getServletContext().setAttribute(ShutdownManager.class.getName(), shutdownManager);
                    }

                    Configuration.getInstance();
                    MifosConfiguration.getInstance().init();
                    configureAuditLogValues(Localization.getInstance().getMainLocale());
                }
            }
        } catch (Exception e) {
            String errMsgStart = "unable to start Mifos web application";
            if (null == LOG) {
                System.err.println(errMsgStart + " and logger is not available!");
                e.printStackTrace();
            } else {
                LOG.error(errMsgStart, e);
            }
            throw new Error(e);
        }
    }

    /**
     * Force a single run of the UpdateCustomerFeesHelper when Mifos starts up.
     * Add an entry to the configKeyValueInteger store
     */
    private void cleanUpRecurringFeesForMifos3712() throws PersistenceException, BatchJobException {
        String key = "Recurring fees cleanup done for MIFOS-3712";

        ConfigurationPersistence configurationPersistence = new ConfigurationPersistence();
        if (configurationPersistence.getConfigurationKeyValueInteger(key) == null) {
            long dummy = 0;
            new UpdateCustomerFeesHelper(new UpdateCustomerFeesTask()).execute(dummy);
            configurationPersistence.addConfigurationKeyValueInteger(key, 1);
            StaticHibernateUtil.commitTransaction();
        }
    }

    public static void printDatabaseError(XmlBuilder xml, int dbVersion) {
        synchronized (ApplicationInitializer.class) {
            if (databaseError.isError) {
                addDatabaseErrorMessage(xml, dbVersion);
            } else {
                addNoFurtherDetailsMessage(xml);
            }
        }
    }

    private static void addNoFurtherDetailsMessage(XmlBuilder xml) {
        xml.startTag("p");
        xml.text("I don't have any further details, unfortunately.");
        xml.endTag("p");
        xml.text("\n");
    }

    private static void addDatabaseErrorMessage(XmlBuilder xml, int dbVersion) {
        xml.startTag("p", "style", "font-weight: bolder; color: red; font-size: x-large;");

        xml.text(databaseError.errmsg);
        xml.text("\n");

        if (databaseError.errorCode.equals(DatabaseErrorCode.UPGRADE_FAILURE)) {
            addDatabaseVersionMessage(xml, dbVersion);
        }
        xml.endTag("p");
        if (databaseError.errorCode.equals(DatabaseErrorCode.CONNECTION_FAILURE)) {
            addConnectionFailureMessage(xml);
        }
        xml.text("\n");
        xml.startTag("p");
        xml.text("More details:");
        xml.endTag("p");
        xml.text("\n");

        if (null != databaseError.error.getCause()) {
            xml.startTag("p", "style", "font-weight: bolder; color: blue;");
            xml.text(databaseError.error.getCause().toString());
            xml.endTag("p");
            xml.text("\n");
        }

        xml.startTag("p", "style", "font-weight: bolder; color: blue;");
        xml.text(getDatabaseConnectionInfo());
        xml.endTag("p");
        xml.text("\n");
        addStackTraceHtml(xml);
    }

    private static void addConnectionFailureMessage(XmlBuilder xml) {
        xml.startTag("p");
        xml.text("Possible causes:");

        xml.startTag("ul");
        xml.startTag("li");
        xml.text("MySQL is not running");
        xml.endTag("li");

        xml.startTag("li");
        xml.text("MySQL is listening on a different port than Mifos is expecting");
        xml.endTag("li");

        xml.startTag("li");
        xml.text("incorrect username or password");
        xml.endTag("li");
        xml.endTag("ul");
        xml.endTag("p");
        xml.text("\n");

        xml.startTag("p");
        xml.startTag("a", "href", "http://www.mifos.org/knowledge/support/deploying-mifos/configuration/guide");
        xml.text("More about configuring your database connection.");
        xml.endTag("a");
        xml.endTag("p");
        xml.text("\n");
    }

    private static void addDatabaseVersionMessage(XmlBuilder xml, int dbVersion) {
        if (dbVersion == -1) {
            xml.text("Database is too old to have a version.\n");
        } else {
            xml.text("Database Version = " + dbVersion + "\n");
        }
        xml.text("Application Version = " + DatabaseVersionPersistence.APPLICATION_VERSION + ".\n");
    }

    private static void addStackTraceHtml(XmlBuilder xml) {
        xml.startTag("p");
        xml.text("Stack trace:");
        xml.endTag("p");
        xml.text("\n");

        xml.startTag("pre");
        StringWriter stackTrace = new StringWriter();
        databaseError.error.printStackTrace(new PrintWriter(stackTrace));
        xml.text("\n" + stackTrace.toString());
        xml.endTag("pre");
        xml.text("\n");
    }

    /**
     * Initializes Hibernate by making it read the hibernate.cfg file and also setting the same with hibernate session
     * factory.
     */
    public static void initializeHibernate() throws AppNotConfiguredException {
        try {
            StaticHibernateUtil.initialize();
        } catch (HibernateStartUpException e) {
            throw new AppNotConfiguredException(e);
        }
    }

    /**
     * This function initialize and bring up the authorization and authentication services
     *
     * @throws AppNotConfiguredException
     *             - IF there is any failures during init
     */
    private void initializeSecurity() throws AppNotConfiguredException {
        try {
            ActivityMapper.getInstance().init();

            AuthorizationManager.getInstance().init();

            HierarchyManager.getInstance().init();

        } catch (XMLReaderException e) {

            throw new AppNotConfiguredException(e);
        } catch (ApplicationException ae) {
            throw new AppNotConfiguredException(ae);
        } catch (SystemException se) {
            throw new AppNotConfiguredException(se);
        }

    }

    private void initializeEntityMaster() throws HibernateProcessException {
        EntityMasterData.getInstance().init();
    }

    private void configureAuditLogValues(Locale locale) throws SystemException {
        AuditConfigurtion.init(locale);
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        ServletContext ctx = servletContextEvent.getServletContext();
        LOG.info("shutting down scheduler");
        final MifosScheduler mifosScheduler = (MifosScheduler) ctx.getAttribute(MifosScheduler.class.getName());
        ctx.removeAttribute(MifosScheduler.class.getName());
        mifosScheduler.shutdown();
    }

    public void requestDestroyed(@SuppressWarnings("unused") ServletRequestEvent event) {
        StaticHibernateUtil.closeSession();
    }

    public void requestInitialized(@SuppressWarnings("unused") ServletRequestEvent event) {

    }

    @Override
    public void sessionCreated(HttpSessionEvent httpSessionEvent) {
        ServletContext ctx = httpSessionEvent.getSession().getServletContext();
        final ShutdownManager shutdownManager = (ShutdownManager) ctx.getAttribute(ShutdownManager.class.getName());
        shutdownManager.sessionCreated(httpSessionEvent);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
        ServletContext ctx = httpSessionEvent.getSession().getServletContext();
        final ShutdownManager shutdownManager = (ShutdownManager) ctx.getAttribute(ShutdownManager.class.getName());
        shutdownManager.sessionDestroyed(httpSessionEvent);
    }

}
