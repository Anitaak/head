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

package org.mifos.platform.questionnaire.mappers;

import org.mifos.customers.surveys.business.Question;
import org.mifos.platform.questionnaire.contract.QuestionDefinition;
import org.mifos.platform.questionnaire.contract.QuestionDetail;
import org.mifos.platform.questionnaire.contract.QuestionGroupDefinition;
import org.mifos.platform.questionnaire.contract.QuestionGroupDetail;
import org.mifos.platform.questionnaire.domain.QuestionGroup;

import java.util.List;

public interface QuestionnaireMapper {
    List<QuestionDetail> mapToQuestionDetails(List<Question> questions);

    QuestionDetail mapToQuestionDetail(Question question);

    Question mapToQuestion(QuestionDefinition questionDefinition);

    QuestionGroup mapToQuestionGroup(QuestionGroupDefinition questionGroupDefinition);

    QuestionGroupDetail mapToQuestionGroupDetail(QuestionGroup questionGroup);

    List<QuestionGroupDetail> mapToQuestionGroupDetails(List<QuestionGroup> questionGroups);
}
