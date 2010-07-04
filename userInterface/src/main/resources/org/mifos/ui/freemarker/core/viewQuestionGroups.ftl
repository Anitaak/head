[#ftl]
[#--
 * Copyright (c) 2005-2010 Grameen Foundation USA
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 *  See also http://www.apache.org/licenses/LICENSE-2.0.html for an
 *  explanation of the license and how it is applied.
--]
[#import "spring.ftl" as spring]

<html lang="EN" xmlns:html-el="http://www.w3.org/1999/xhtml" xmlns:html-el="http://www.w3.org/1999/xhtml"
      xmlns:c="http://www.springframework.org/schema/webflow" xmlns:c="http://www.w3.org/1999/XSL/Transform"
      xmlns:fmt="http://jboss.org/xml/ns/javax/validation/mapping" xmlns:fmt="http://java.sun.com/JSP/Page">
<head>
    <title>Mifos</title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
    <link href='pages/framework/css/cssstyle.css' rel="stylesheet" type="text/css">
</head>

<body>
<table width="100%" border="0" cellspacing="0" cellpadding="0">
    <tr>
        <td width="188" rowspan="2"><img src="pages/framework/images/logo.gif" width="188" height="74"></td>
        <td align="right" bgcolor="#FFFFFF" class="fontnormal"><a id="header.link.yoursettings"
                                                                  href="yourSettings.do?method=get">Your settings</a>

            &nbsp;|&nbsp; <a id="logout_link" href="j_spring_security_logout">Logout</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        </td>
    </tr>
    <tr>
        <td align="left" valign="bottom" bgcolor="#FFFFFF">
            <table border="0" cellspacing="1" cellpadding="0">
                <tr>
                    <td class="tablightorange"><a id="header.link.home" href="custSearchAction.do?method=getHomePage">Home</a>
                    </td>
                    <td class="tablightorange"><a id="header.link.clientsAndAccounts"
                                                  href="custSearchAction.do?method=loadMainSearch">Clients &
                        Accounts </a></td>

                    <td class="tablightorange"><a id="header.link.reports"
                                                  href="reportsAction.do?method=load">Reports</a></td>
                    <td class="taborange"><a id="header.link.admin" href="AdminAction.do?method=load"
                                             class="tabfontwhite">Admin</a></td>
                </tr>
            </table>

        </td>
    </tr>
    <tr>
        <td colspan="2" class="bgorange"><img src="pages/framework/images/trans.gif" width="6" height="6"></td>
    </tr>
    <tr>
        <td colspan="2" class="bgwhite"><img src="pages/framework/images/trans.gif" width="100" height="2"></td>
    </tr>
</table>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
    <tr>
        <td width="174" height="500" align="left" valign="top" class="bgorangeleft">
            <table width="100%" border="0" cellspacing="0" cellpadding="0">
                <tr>
                    <td class="leftpanehead" colspan="2">Administrative tasks</td>
                </tr>
                <tr>
                    <td class="leftpanelinks">
                        <form name="custSearchActionForm" method="post"
                              action="/mifos/custSearchAction.do?method=loadAllBranches">
                            <table width="90%" border="0" cellspacing="0" cellpadding="0">
                                <tr>
                                    <td class="paddingbottom03">
                                        <span class="fontnormal8ptbold">Search by client name, system ID or account number</span>
                                    </td>
                                </tr>
                            </table>
                            <table width="90%" border="0" cellpadding="0" cellspacing="0">
                                <tr>
                                    <td width="100%" colspan="2">
                                        <input type="text" name="searchString" maxlength="200" size="20" value="">
                                        <input type="hidden" name="searchNode(search_officeId)" value="0">
                                        <input type="hidden" name="officeName" value="">
                                    </td>
                                </tr>
                            </table>
                            <table width="143" border="0" cellspacing="0" cellpadding="10">
                                <tr>
                                    <td align="right">
                                        <input type="submit" name="searchButton" value="Search" class="buttn">
                                    </td>
                                </tr>
                            </table>
                        </form>
                    </td>
                </tr>
            </table>
        </td>
        <td align="left" valign="top" bgcolor="#FFFFFF" class="paddingleftmain" height="500">
            <span id="page.id" title="view_question_groups"/>
            <table width="95%" border="0" cellpadding="0" cellspacing="0">
                <tr>
                    <td class="bluetablehead05">
                      <span class="fontnormal8pt">
                          <a href="AdminAction.do?method=load">Admin</a> /
                      </span>
                      <span class="fontnormal8ptbold">
                          View Question Groups
                      </span>
                    </td>
                </tr>
            </table>
            <table width="95%" border="0" cellpadding="0" cellspacing="0">
                <tr>
                    <td width="70%" align="left" valign="middle" class="paddingL15T15">
                        <div style="padding:3px" class="headingorange">
                            View Question Groups
                            <br/>
                            <span class="fontnormal">
                                Click on a question group below to view details and make changes or
                                <a href="createQuestionGroup.ftl">
                                    define a new question group
                                </a>
                            </span>
                            <br/>
                            <br/>
                        </div>
                        <table width="90%" border="0" cellspacing="0" cellpadding="0" id="questionGroups.table">
                            [#list questionGroups as questionGroup]
                            <tr class="fontnormal">
                                <td width="1%"><img src="pages/framework/images/bullet_circle.gif" width="9"
                                                    height="11"/></td>
                                <td width="99%">
                                    <a href="viewQuestionGroupDetail.ftl?questionGroupId=${questionGroup.id}"
                                       id="questionGroupId_${questionGroup.id}">
                                        ${questionGroup.title}
                                        <br/>
                                    </a>
                                </td>
                            </tr>
                            [/#list]
                        </table>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
</table>
</body>
</html>
