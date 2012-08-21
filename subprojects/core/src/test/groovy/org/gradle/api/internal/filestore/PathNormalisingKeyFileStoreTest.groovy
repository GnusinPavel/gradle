/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.api.internal.filestore

import org.gradle.util.TemporaryFolder
import org.gradle.util.TestFile
import org.junit.Rule
import spock.lang.Specification

class PathNormalisingKeyFileStoreTest extends Specification {

    @Rule TemporaryFolder dir = new TemporaryFolder()
    TestFile fsBase
    PathNormalisingKeyFileStore store

    def pathCounter = 0

    def setup() {
        fsBase = dir.createDir("fs")
        store = new PathNormalisingKeyFileStore(new PathKeyFileStore(fsBase))
    }

    def "can add to filestore"() {
        when:
        store.add("!.zip", file("abc"))
        store.add("  ", file("def"))

        then:
        fsBase.file("_.zip").text == "abc"
        fsBase.file("__").text == "def"
    }

    def "can overwrite entry"() {
        when:
        store.add("!", file("abc"))
        store.add(" ", file("def"))

        then:
        fsBase.file("_").text == "def"
    }

    def "creates intermediary directories"() {
        when:
        store.add("a/!/c", file("abc"))
        store.add("a/ /d", file("abd"))
        store.add("a/c/(", file("aca"))

        then:
        fsBase.file("a/_").directory
        fsBase.file("a/_/c").text == "abc"
        fsBase.file("a/c/_").text == "aca"
    }

    def "can search via globs"() {
        when:
        store.add("a/!/a", file("a"))
        store.add("a/ /b", file("b"))
        store.add("a/b/&", file("c"))

        then:
        store.search("**/a").size() == 1
        store.search("*/ /*").size() == 2
        store.search("a/b/_").size() == 1
    }

    def file(String content, String path = "f${pathCounter++}") {
        dir.createFile(path) << content
    }

}
