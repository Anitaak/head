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

package org.mifos.ui.core.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mifos.platform.questionnaire.contract.QuestionDetail;
import org.mifos.platform.questionnaire.contract.QuestionType;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class QuestionDetailFormTest {

    @Test
    public void shouldGetTitleAndType() {
        assertQuestionDetailForm("Question Title1", QuestionType.NUMERIC, "Number");
        assertQuestionDetailForm("Question Title2", QuestionType.FREETEXT, "Free text");
        assertQuestionDetailForm("Question Title2", QuestionType.DATE, "Date");
    }

    private void assertQuestionDetailForm(String shortName, QuestionType questionType, String questionTypeString) {
        QuestionDetail questionDetail = new QuestionDetail(123, "Question Text", shortName, questionType);
        QuestionDetailForm questionDetailForm = new QuestionDetailForm(questionDetail);
        assertThat(questionDetailForm.getTitle(), is(shortName));
        assertThat(questionDetailForm.getType(), is(questionTypeString));
    }
}
