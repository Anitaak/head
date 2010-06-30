/*
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
 */

package org.mifos.platform.questionnaire.validators;

import org.apache.commons.lang.StringUtils;
import org.mifos.framework.exceptions.ApplicationException;
import org.mifos.platform.questionnaire.QuestionnaireConstants;
import org.mifos.platform.questionnaire.contract.QuestionDefinition;
import org.mifos.platform.questionnaire.contract.QuestionGroupDefinition;

import static org.mifos.platform.questionnaire.QuestionnaireConstants.*;
import static org.mifos.platform.questionnaire.contract.QuestionType.INVALID;

public class QuestionnaireValidatorImpl implements QuestionnaireValidator {
    @Override
    public void validate(QuestionDefinition questionDefinition) throws ApplicationException {
        validateQuestionTitle(questionDefinition);
        validateQuestionType(questionDefinition);
    }

    @Override
    public void validate(QuestionGroupDefinition questionGroupDefinition) throws ApplicationException {
        validateQuestionGroupTitle(questionGroupDefinition);
        validateQuestionGroupSections(questionGroupDefinition);
    }

    private void validateQuestionGroupSections(QuestionGroupDefinition questionGroupDefinition) throws ApplicationException {
        if(questionGroupDefinition.getSectionDefinitions()==null || questionGroupDefinition.getSectionDefinitions().size()==0){
            throw new ApplicationException(QuestionnaireConstants.QUESTION_GROUP_SECTION_NOT_PROVIDED);
        }
    }

    private void validateQuestionGroupTitle(QuestionGroupDefinition questionGroupDefinition) throws ApplicationException {
        if (StringUtils.isEmpty(questionGroupDefinition.getTitle()))
            throw new ApplicationException(QUESTION_GROUP_TITLE_NOT_PROVIDED);
    }

    private void validateQuestionType(QuestionDefinition questionDefinition) throws ApplicationException {
        if (INVALID == questionDefinition.getType())
            throw new ApplicationException(QUESTION_TYPE_NOT_PROVIDED);
    }

    private void validateQuestionTitle(QuestionDefinition questionDefinition) throws ApplicationException {
        if (StringUtils.isEmpty(questionDefinition.getTitle()))
            throw new ApplicationException(QUESTION_TITLE_NOT_PROVIDED);
    }
}
