[#ftl]
[#--
* Copyright (c) 2005-2011 Grameen Foundation USA
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

[@layout.basic currentTab="ClientsAndAccounts"]
<h1>test <span class="standout">holla</span></h1>

bread crumbs!<br/>

global account number: ${loanAccountStatusFormBean.loanInformation.globalAccountNum}<br/>
account name: ${loanAccountStatusFormBean.loanInformation.prdOfferingName}<br/>
step: change status (externalize!)<br/>

<form action="${flowExecutionUrl}" method="post" class="two-columns">
    <fieldset>
    <div class="row">
        [@form.label for="status" required=true]Status (i18n):[/@form.label]
		[@form.radioButtons path="loanAccountStatusFormBean.status" options=loanAccountStatusFormBean.accountStatusOptions separator="<br/>" attributes="" /]
    </div>
    <div class="row">
        [@form.label for="note" required=true]Note (i18n):[/@form.label]
		[@spring.formTextarea path="loanAccountStatusFormBean.note" attributes="class='note'" /]
    </div>
    </fieldset>
    <div class="row">
		[@form.submitButton label="widget.form.buttonLabel.preview" webflowEvent="detailsEntered" /]
		[@form.submitButton webflowEvent="cancel" /]
    </div>
</form>

[/@layout.basic]