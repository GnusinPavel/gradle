/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.problems.interfaces;

import org.gradle.api.Incubating;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Interface for describing structured information about a problem.
 *
 * @since 8.4
 */
@Incubating
public interface Problem {


    ProblemGroup getProblemGroup();

    String getProblemType();

    /**
     * A short description of the problem.
     */
    String getMessage();

    /**
     * A longer string describing the problem.
     */
    @Nullable
    String getDescription();

    /**
     * Problem severity.
     * <p>
     * The severity of a problem is a hint to the user about how important the problem is.
     * ERROR will fail the build, WARNING will not.
     */

    Severity getSeverity();

    @Nullable
    ProblemLocation getWhere();

    /**
     * A link to the documentation for this problem.
     */
    @Nullable
    DocLink getDocumentationLink();


    /**
     * A list of possible solutions the user can try to fix the problem.
     */
    List<String> getSolutions();

    /**
     * The exception that caused the problem.
     */
    @Nullable
    Throwable getCause();

    /**
     * Additional Data about the problem.
     * In a map so it can be somewhat structured.
     */
    Map<String, String> getAdditionalData();
}
