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

package org.mifos.test.matchers;

import org.mifos.platform.questionnaire.contract.EventSource;
import org.mockito.ArgumentMatcher;

import java.util.List;

public class HasThisKindOfEvent extends ArgumentMatcher<List<EventSource>> {
    private String source;
    private String event;
    private String description;

    public HasThisKindOfEvent(String event, String source, String description) {
        this.event = event;
        this.source = source;
        this.description = description;
    }

    @Override
    public boolean matches(Object o) {
        if (o instanceof List) {
            List<EventSource> eventSources = (List<EventSource>) o;
            for (EventSource eventSource : eventSources) {
                if (eventSource.getSource().equals(source) &&
                        eventSource.getEvent().equals(event) &&
                            eventSource.getDesciption().equals(description))
                    return true;
            }
        }
        return false;
    }
}
