/*
 * Copyright 2019 the original author or authors.
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

package org.gradle.internal.execution.caching.impl

import org.gradle.internal.fingerprint.impl.EmptyCurrentFileCollectionFingerprint
import org.gradle.internal.hash.HashCode
import org.gradle.internal.snapshot.impl.ImplementationSnapshot
import org.gradle.internal.snapshot.impl.IntegerValueSnapshot
import org.gradle.internal.snapshot.impl.StringValueSnapshot
import spock.lang.Specification

class DefaultCachingStateBuilderTest extends Specification {
    def builder = new DefaultCachingStateBuilder()

    def implementation = ImplementationSnapshot.of("org.gradle.WorkType", HashCode.fromInt(1234))
    def additionalImplementation = ImplementationSnapshot.of("org.gradle.AdditionalWorkType", HashCode.fromInt(1234))

    def "caching is enabled with valid inputs"() {
        withValidInputs()

        when:
        def cachingState = builder.build()

        then:
        cachingState.key.present
        cachingState.disabledReasons.empty
        cachingState.inputs.get().implementation == implementation
        cachingState.inputs.get().additionalImplementations == [additionalImplementation]
        cachingState.inputs.get().inputValueFingerprints.keySet().toList() == ["input.number", "input.string"]
        cachingState.inputs.get().inputFileFingerprints.keySet().toList() == ["input.files"]
        cachingState.inputs.get().outputProperties.toList() == ["output"]
    }

    def withValidInputs() {
        builder.withImplementation(
            implementation
        )
        builder.withAdditionalImplementations([
            additionalImplementation
        ])
        builder.withInputValueFingerprints(
            "input.string": new StringValueSnapshot("input"),
            "input.number": new IntegerValueSnapshot(123)
        )
        builder.withInputFilePropertyFingerprints(
            "input.files": new EmptyCurrentFileCollectionFingerprint("test")
        )
        builder.withOutputPropertyNames([
            "output"
        ])
    }
}
