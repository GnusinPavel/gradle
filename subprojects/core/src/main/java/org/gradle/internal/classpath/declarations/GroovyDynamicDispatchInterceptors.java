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

package org.gradle.internal.classpath.declarations;

import groovy.lang.GroovyObject;
import org.codehaus.groovy.runtime.ScriptBytecodeAdapter;
import org.gradle.api.NonNullApi;
import org.gradle.internal.classpath.Instrumented;
import org.gradle.internal.classpath.InstrumentedClosuresHelper;
import org.gradle.internal.classpath.intercept.AbstractInvocation;
import org.gradle.internal.classpath.intercept.CallInterceptor;
import org.gradle.internal.classpath.intercept.InterceptScope;
import org.gradle.internal.instrumentation.api.annotations.CallableKind;
import org.gradle.internal.instrumentation.api.annotations.InterceptJvmCalls;
import org.gradle.internal.instrumentation.api.annotations.ParameterKind.CallerClassName;
import org.gradle.internal.instrumentation.api.annotations.SpecificJvmCallInterceptors;
import org.gradle.internal.instrumentation.api.declarations.InterceptorDeclaration;

import javax.annotation.Nullable;

import static org.gradle.internal.classpath.InstrumentedGroovyCallsHelper.withEntryPoint;
import static org.gradle.internal.classpath.InstrumentedGroovyCallsTracker.CallKind.SET_PROPERTY;

@SuppressWarnings("NewMethodNamingConvention")
@NonNullApi
@SpecificJvmCallInterceptors(generatedClassName = InterceptorDeclaration.JVM_BYTECODE_GENERATED_CLASS_NAME_FOR_CONFIG_CACHE)
public class GroovyDynamicDispatchInterceptors {
    @InterceptJvmCalls
    @CallableKind.StaticMethod(ofClass = ScriptBytecodeAdapter.class)
    public static void intercept_setGroovyObjectProperty(
        Object messageArgument,
        /* Sometimes it's null, we can't rely on it, so we still use @CallerClassName */
        Class<?> senderClass,
        GroovyObject receiver,
        String messageName,
        @CallerClassName String consumer
    ) throws Throwable {
        if (!Instrumented.INTERCEPTOR_RESOLVER.isAwareOfCallSiteName(messageName)) {
            ScriptBytecodeAdapter.setGroovyObjectProperty(messageArgument, senderClass, receiver, messageName);
            return;
        }
        InstrumentedClosuresHelper.INSTANCE.hitInstrumentedDynamicCall();
        withEntryPoint(consumer, messageName, SET_PROPERTY, () -> {
            ScriptBytecodeAdapter.setGroovyObjectProperty(messageArgument, senderClass, receiver, messageName);
            return null;
        });
    }

    @InterceptJvmCalls
    @CallableKind.StaticMethod(ofClass = ScriptBytecodeAdapter.class)
    public static void intercept_setProperty(
        Object messageArgument,
        /* Sometimes it's null, we can't rely on it, so we still use @CallerClassName */
        Class<?> senderClass,
        Object receiver,
        String messageName,
        @CallerClassName String consumer
    ) throws Throwable {
        CallInterceptor interceptor = Instrumented.INTERCEPTOR_RESOLVER.resolveCallInterceptor(InterceptScope.writesOfPropertiesNamed(messageName));
        if (interceptor != null) {
            @NonNullApi
            class SetPropertyInvocationImpl extends AbstractInvocation<Object> {
                public SetPropertyInvocationImpl(Object receiver, Object[] args) {
                    super(receiver, args);
                }

                @Override
                public @Nullable Object callOriginal() throws Throwable {
                    ScriptBytecodeAdapter.setProperty(messageArgument, senderClass, receiver, messageName);
                    return null;
                }
            }
            interceptor.doIntercept(new SetPropertyInvocationImpl(receiver, new Object[]{messageArgument}), consumer);
        } else {
            ScriptBytecodeAdapter.setProperty(messageArgument, senderClass, receiver, messageName);
        }
    }
}
